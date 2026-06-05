package com.example.studyflow.network.responses;

public class SubjectResponseDto {

    private Long id;
    private String title;
    private String description;

    private Integer plannedTotalMinutes;
    private Integer goalMinutesPerSession;
    private String learningType;
    private String studyFrequency;
    private String notes;

    public SubjectResponseDto() {
    }

    public SubjectResponseDto(Long id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    private Long studiedSeconds;

    private String note;

    public String getTitle() {
        return title;
    }

    public String getName() {
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

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Long getStudiedSeconds() {
        return studiedSeconds;
    }

    public void setStudiedSeconds(Long studiedSeconds) {
        this.studiedSeconds = studiedSeconds;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

}
