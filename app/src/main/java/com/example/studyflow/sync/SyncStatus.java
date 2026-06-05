package com.example.studyflow.sync;

public final class SyncStatus {

    public static final String SYNCED = "SYNCED";
    public static final String PENDING_CREATE = "PENDING_CREATE";
    public static final String PENDING_UPDATE = "PENDING_UPDATE";
    public static final String PENDING_DELETE = "PENDING_DELETE";
    public static final String SYNCING = "SYNCING";
    public static final String FAILED = "FAILED";

    private SyncStatus() {
    }
}