package com.tdfpoule.service;

import com.tdfpoule.entity.Stage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses a stage's GPX track (lat/lon/ele trackpoints) into a compact, chart-ready elevation
 * profile: cumulative distance via the haversine formula between consecutive points, paired
 * with elevation, downsampled to a fixed point count -- plus the route's true total distance
 * and total climbing, which replace the hand-typed approximations in stages.json once a GPX
 * file exists for that stage.
 */
@Service
public class GpxRouteParser {

    private static final int TARGET_POINTS = 180;
    private static final double EARTH_RADIUS_KM = 6371.0088;

    public record ElevationPoint(double km, double elev) {}
    public record GpxProfile(double totalKm, double totalAscentM, List<ElevationPoint> points) {
        public boolean isEmpty() { return points.isEmpty(); }
    }

    private static final GpxProfile EMPTY = new GpxProfile(0, 0, List.of());

    /**
     * Overwrites km/elev/elevationProfile on the given stage from its bundled GPX, if one
     * exists -- the GPX-derived figures replace the hand-typed seed approximations. Shared by
     * both new-poule creation and the startup backfill for poules created before this existed.
     */
    public void applyToStage(Stage stage) {
        GpxProfile profile = parseForStage(stage.getStageNumber());
        if (profile.isEmpty()) return;
        stage.setKm((int) Math.round(profile.totalKm()));
        stage.setElev((int) Math.round(profile.totalAscentM()));
        stage.setElevationProfile(profile.points().stream()
                .map(p -> new com.tdfpoule.entity.ElevationPoint(p.km(), p.elev()))
                .collect(Collectors.toList()));
    }

    /** Empty if no bundled GPX exists for this stage number (e.g. a custom extra stage). */
    public GpxProfile parseForStage(int stageNumber) {
        ClassPathResource resource = new ClassPathResource("gpx/stage-" + stageNumber + ".gpx");
        if (!resource.exists()) return EMPTY;
        try (InputStream in = resource.getInputStream()) {
            return parse(in);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse stage-" + stageNumber + ".gpx", e);
        }
    }

    public GpxProfile parse(InputStream gpxStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = factory.newDocumentBuilder().parse(gpxStream);

        NodeList trkpts = doc.getElementsByTagName("trkpt");
        int total = trkpts.getLength();
        double[] lats = new double[total];
        double[] lons = new double[total];
        double[] eles = new double[total];
        for (int i = 0; i < total; i++) {
            Element pt = (Element) trkpts.item(i);
            lats[i] = Double.parseDouble(pt.getAttribute("lat"));
            lons[i] = Double.parseDouble(pt.getAttribute("lon"));
            NodeList eleNodes = pt.getElementsByTagName("ele");
            eles[i] = eleNodes.getLength() > 0 ? Double.parseDouble(eleNodes.item(0).getTextContent().trim()) : 0;
        }
        if (total < 2) return EMPTY;

        double[] cumKm = new double[total];
        for (int i = 1; i < total; i++) {
            cumKm[i] = cumKm[i - 1] + haversineKm(lats[i - 1], lons[i - 1], lats[i], lons[i]);
        }
        double totalKm = cumKm[total - 1];
        if (totalKm <= 0) return EMPTY;

        // Light smoothing before measuring climb so GPS/DEM noise doesn't inflate "total ascent" --
        // a raw sum of every positive delta tends to wildly overcount on noisy elevation data.
        double[] smoothed = movingAverage(eles, 5);
        double ascent = 0;
        for (int i = 1; i < total; i++) {
            double delta = smoothed[i] - smoothed[i - 1];
            if (delta > 0) ascent += delta;
        }

        return new GpxProfile(totalKm, ascent, resample(cumKm, eles, totalKm));
    }

    /** Resamples at evenly spaced distance intervals (not raw point index) so charts look
     *  consistent regardless of how densely a particular GPX file was recorded. */
    private List<ElevationPoint> resample(double[] cumKm, double[] eles, double totalKm) {
        List<ElevationPoint> out = new ArrayList<>(TARGET_POINTS + 1);
        int srcIdx = 0;
        for (int i = 0; i <= TARGET_POINTS; i++) {
            double targetKm = totalKm * i / TARGET_POINTS;
            while (srcIdx < cumKm.length - 2 && cumKm[srcIdx + 1] < targetKm) srcIdx++;
            int nextIdx = Math.min(srcIdx + 1, cumKm.length - 1);
            double kmA = cumKm[srcIdx], kmB = cumKm[nextIdx];
            double eleA = eles[srcIdx], eleB = eles[nextIdx];
            double frac = kmB > kmA ? (targetKm - kmA) / (kmB - kmA) : 0;
            out.add(new ElevationPoint(round1(targetKm), round1(eleA + (eleB - eleA) * frac)));
        }
        return out;
    }

    private double[] movingAverage(double[] values, int window) {
        double[] out = new double[values.length];
        int half = window / 2;
        for (int i = 0; i < values.length; i++) {
            int from = Math.max(0, i - half), to = Math.min(values.length - 1, i + half);
            double sum = 0;
            for (int j = from; j <= to; j++) sum += values[j];
            out[i] = sum / (to - from + 1);
        }
        return out;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double round1(double v) { return Math.round(v * 10) / 10.0; }
}
