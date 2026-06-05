package com.example.studyflow.storage.local.session;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocalStudySessionDao {

    @Insert
    long insertSession(LocalStudySessionEntity session);

    @Update
    void updateSession(LocalStudySessionEntity session);

    @Insert
    long insertMicroCheckpoint(LocalMicroCheckpointEntity checkpoint);

    @Query("SELECT * FROM study_sessions WHERE localId = :localId LIMIT 1")
    LocalStudySessionEntity getSessionByLocalId(long localId);

    @Query("SELECT * FROM study_sessions WHERE synced = 0 ORDER BY createdAtMillis ASC")
    List<LocalStudySessionEntity> getPendingSessions();

    @Query("SELECT * FROM micro_checkpoints WHERE sessionLocalId = :sessionLocalId ORDER BY createdAtMillis ASC")
    List<LocalMicroCheckpointEntity> getMicroCheckpointsBySessionLocalId(long sessionLocalId);

    @Query("UPDATE study_sessions SET synced = 1, syncStatus = 'SYNCED', serverId = :serverId, updatedAtMillis = :updatedAtMillis WHERE localId = :localId")
    void markSessionSynced(long localId, Long serverId, long updatedAtMillis);

    @Query("UPDATE study_sessions SET syncStatus = 'FAILED', updatedAtMillis = :updatedAtMillis WHERE localId = :localId")
    void markSessionSyncFailed(long localId, long updatedAtMillis);

    @Query("UPDATE study_sessions SET syncStatus = 'PENDING', updatedAtMillis = :updatedAtMillis WHERE localId = :localId")
    void markSessionPending(long localId, long updatedAtMillis);

    @Query("SELECT * FROM study_sessions WHERE subjectServerId = :subjectId OR subjectLocalId = :subjectId ORDER BY createdAtMillis DESC")
    List<LocalStudySessionEntity> getSessionsBySubjectId(long subjectId);

    @Query("UPDATE study_sessions SET subjectServerId = :subjectServerId, updatedAtMillis = :updatedAtMillis WHERE subjectLocalId = :subjectLocalId AND subjectServerId IS NULL")
    void updateSubjectServerIdForPendingSessions(
            long subjectLocalId,
            Long subjectServerId,
            long updatedAtMillis
    );

    @Query("SELECT IFNULL(SUM(durationSeconds), 0) FROM study_sessions " +
            "WHERE subjectServerId = :subjectId OR subjectLocalId = :subjectId")
    Long getTotalStudiedSecondsBySubjectId(long subjectId);

    @Query("SELECT * FROM study_sessions " +
            "WHERE subjectServerId = :subjectId OR subjectLocalId = :subjectId " +
            "ORDER BY createdAtMillis DESC")
    List<LocalStudySessionEntity> getSessionsBySubjectIdSync(long subjectId);
}