package com.tdfpoule.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "stage", uniqueConstraints = @UniqueConstraint(columnNames = {"poule_id", "stage_number"}))
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poule_id", nullable = false, length = 8)
    private String pouleId;

    @Column(name = "stage_number", nullable = false)
    private int stageNumber;

    @Column(nullable = false, length = 160)
    private String label;

    @Column(name = "stage_tag", nullable = false, length = 20)
    private String stageTag;

    private Integer km;
    private Integer elev;

    @Column(name = "note_en", columnDefinition = "text")
    private String noteEn;

    @Column(name = "note_nl", columnDefinition = "text")
    private String noteNl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> favorites = new ArrayList<>();

    /** Downsampled distance/elevation profile parsed from this stage's GPX file (empty if none). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "elevation_profile", nullable = false, columnDefinition = "jsonb")
    private List<ElevationPoint> elevationProfile = new ArrayList<>();

    @Column(nullable = false)
    private boolean locked = false;

    /** Ordered finish order, up to 15 places. Null entries mean "not entered yet". */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_rider_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> topRiderIds = new ArrayList<>();

    /** Ordered GC standing after this stage, up to 10 places. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gc_rider_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> gcRiderIds = new ArrayList<>();

    /** yellow/green/polka/white -> riderId */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> jerseys = new LinkedHashMap<>();

    /** riderId -> points scored in this stage (finish points + jersey bonuses) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "points_by_rider", nullable = false, columnDefinition = "jsonb")
    private Map<String, Integer> pointsByRider = new LinkedHashMap<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPouleId() { return pouleId; }
    public void setPouleId(String pouleId) { this.pouleId = pouleId; }

    public int getStageNumber() { return stageNumber; }
    public void setStageNumber(int stageNumber) { this.stageNumber = stageNumber; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getStageTag() { return stageTag; }
    public void setStageTag(String stageTag) { this.stageTag = stageTag; }

    public Integer getKm() { return km; }
    public void setKm(Integer km) { this.km = km; }

    public Integer getElev() { return elev; }
    public void setElev(Integer elev) { this.elev = elev; }

    public String getNoteEn() { return noteEn; }
    public void setNoteEn(String noteEn) { this.noteEn = noteEn; }

    public String getNoteNl() { return noteNl; }
    public void setNoteNl(String noteNl) { this.noteNl = noteNl; }

    public List<String> getFavorites() { return favorites; }
    public void setFavorites(List<String> favorites) { this.favorites = favorites; }

    public List<ElevationPoint> getElevationProfile() { return elevationProfile; }
    public void setElevationProfile(List<ElevationPoint> elevationProfile) { this.elevationProfile = elevationProfile; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public List<String> getTopRiderIds() { return topRiderIds; }
    public void setTopRiderIds(List<String> topRiderIds) { this.topRiderIds = topRiderIds; }

    public List<String> getGcRiderIds() { return gcRiderIds; }
    public void setGcRiderIds(List<String> gcRiderIds) { this.gcRiderIds = gcRiderIds; }

    public Map<String, String> getJerseys() { return jerseys; }
    public void setJerseys(Map<String, String> jerseys) { this.jerseys = jerseys; }

    public Map<String, Integer> getPointsByRider() { return pointsByRider; }
    public void setPointsByRider(Map<String, Integer> pointsByRider) { this.pointsByRider = pointsByRider; }
}
