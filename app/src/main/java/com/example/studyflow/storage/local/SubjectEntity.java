package com.example.studyflow.storage.local;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.studyflow.sync.SyncStatus;

@Entity(tableName = "subjects")
public class SubjectEntity {

    @PrimaryKey(autoGenerate = true)
    public long localId;

    public String note;

    public Long serverId;

    public String title;
    public String description;

    public int plannedTotalMinutes;
    public int goalMinutesPerSession;
    public String learningType;
    public String notes;

    // Compatibility field for old code that still reads/writes studyFrequency.
    // It mirrors learningType so older repository/viewmodel code can compile.
    public String studyFrequency;

    public String syncStatus;

    public long createdAtMillis;
    public long updatedAtMillis;

    // Room uses this constructor.
    public SubjectEntity() {
    }

    @Ignore
    public SubjectEntity(String title, String description) {
        this(
                title,
                description,
                0,
                0,
                "",
                description
        );
    }

    @Ignore
    public SubjectEntity(
            String title,
            String description,
            int plannedTotalMinutes,
            int goalMinutesPerSession,
            String learningType,
            String notes
    ) {
        this.title = title;
        this.description = description;
        this.plannedTotalMinutes = plannedTotalMinutes;
        this.goalMinutesPerSession = goalMinutesPerSession;
        this.learningType = learningType;
        this.notes = notes;
        this.studyFrequency = learningType;
        this.serverId = null;
        this.syncStatus = SyncStatus.PENDING_CREATE;
        this.createdAtMillis = System.currentTimeMillis();
        this.updatedAtMillis = System.currentTimeMillis();
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
