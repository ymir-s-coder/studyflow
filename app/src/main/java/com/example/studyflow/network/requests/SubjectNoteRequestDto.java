package com.example.studyflow.network.requests;

public class SubjectNoteRequestDto {

    private String notes;

    public SubjectNoteRequestDto(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
