package com.tdfpoule.entity;

/** One swap a player made: which rider went out, which came in, and from which stage it counts. */
public class SwapLogEntry {

    private String outId;
    private String inId;
    private int effectiveFromStage;
    private long ts;

    public SwapLogEntry() {}

    public SwapLogEntry(String outId, String inId, int effectiveFromStage, long ts) {
        this.outId = outId;
        this.inId = inId;
        this.effectiveFromStage = effectiveFromStage;
        this.ts = ts;
    }

    public String getOutId() { return outId; }
    public void setOutId(String outId) { this.outId = outId; }

    public String getInId() { return inId; }
    public void setInId(String inId) { this.inId = inId; }

    public int getEffectiveFromStage() { return effectiveFromStage; }
    public void setEffectiveFromStage(int effectiveFromStage) { this.effectiveFromStage = effectiveFromStage; }

    public long getTs() { return ts; }
    public void setTs(long ts) { this.ts = ts; }
}
