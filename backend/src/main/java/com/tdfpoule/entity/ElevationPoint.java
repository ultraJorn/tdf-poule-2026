package com.tdfpoule.entity;

/** One point of a stage's downsampled elevation profile, parsed from its GPX file. */
public class ElevationPoint {

    private double km;
    private double elev;

    public ElevationPoint() {}

    public ElevationPoint(double km, double elev) {
        this.km = km;
        this.elev = elev;
    }

    public double getKm() { return km; }
    public void setKm(double km) { this.km = km; }

    public double getElev() { return elev; }
    public void setElev(double elev) { this.elev = elev; }
}
