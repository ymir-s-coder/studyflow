package com.example.studyflow.network.responses;

public class MicroCheckpointResponseDto {

    private Long id;
    private String distractionCountRange;
    private String mood;
    private String breakReason;
    private Integer concentrationLevel;
    private Long createdAtMillis;
    private String createdAt;

    public MicroCheckpointResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public String getDistractionCountRange() {
        return distractionCountRange;
    }

    public String getMood() {
        return mood;
    }

    public String getBreakReason() {
        return breakReason;
    }

    public Integer getConcentrationLevel() {
        return concentrationLevel;
    }

    public Long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDistractionCountRange(String distractionCountRange) {
        this.distractionCountRange = distractionCountRange;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public void setBreakReason(String breakReason) {
        this.breakReason = breakReason;
    }

    public void setConcentrationLevel(Integer concentrationLevel) {
        this.concentrationLevel = concentrationLevel;
    }

    public void setCreatedAtMillis(Long createdAtMillis) {
        this.createdAtMillis = createdAtMillis;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}