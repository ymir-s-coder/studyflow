package com.example.studyflow.network.requests;

public class CreateSubjectRequestDto {

    private String title;
    private String description;
    private Integer plannedTotalMinutes;
    private Integer goalMinutesPerSession;
    private String learningType;
    private String studyFrequency;
    private String notes;

    public CreateSubjectRequestDto(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public CreateSubjectRequestDto(
            String title,
            String description,
            Integer plannedTotalMinutes,
            Integer goalMinutesPerSession,
            String learningType,
            String notes
    ) {
        this.title = title;
        this.description = description;
        this.plannedTotalMinutes = plannedTotalMinutes;
        this.goalMinutesPerSession = goalMinutesPerSession;
        this.learningType = learningType;
        this.studyFrequency = learningType;
        this.notes = notes;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getPlannedTotalMinutes() {
        return plannedTotalMinutes;
    }

    public Integer getGoalMinutesPerSession() {
        return goalMinutesPerSession;
    }

    public String getLearningType() {
        return learningType;
    }

    public String getStudyFrequency() {
        return studyFrequency;
    }

    public String getNotes() {
        return notes;
    }

    public void setPlannedTotalMinutes(Integer plannedTotalMinutes) {
        this.plannedTotalMinutes = plannedTotalMinutes;
    }

    public void setGoalMinutesPerSession(Integer goalMinutesPerSession) {
        this.goalMinutesPerSession = goalMinutesPerSession;
    }

    public void setLearningType(String learningType) {
        this.learningType = learningType;
    }

    public void setStudyFrequency(String studyFrequency) {
        this.studyFrequency = studyFrequency;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
