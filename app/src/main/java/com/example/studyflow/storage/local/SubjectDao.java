package com.example.studyflow.storage.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SubjectDao {

    @Insert
    long insert(SubjectEntity subject);

    @Update
    void update(SubjectEntity subject);

    @Query("SELECT * FROM subjects ORDER BY updatedAtMillis DESC")
    List<SubjectEntity> getAllSubjects();

    @Query("SELECT * FROM subjects WHERE syncStatus != 'SYNCED'")
    List<SubjectEntity> getPendingSubjects();

    @Query("SELECT * FROM subjects WHERE syncStatus = 'PENDING_CREATE'")
    List<SubjectEntity> getPendingCreateSubjects();

    @Query("SELECT * FROM subjects WHERE syncStatus = 'PENDING_UPDATE'")
    List<SubjectEntity> getPendingUpdateSubjects();

    @Query("SELECT * FROM subjects WHERE syncStatus = 'PENDING_DELETE'")
    List<SubjectEntity> getPendingDeleteSubjects();

    @Query("UPDATE subjects SET syncStatus = 'SYNCING', updatedAtMillis = :updatedAtMillis WHERE localId = :localId AND syncStatus = 'PENDING_CREATE'")
    int markSubjectSyncing(long localId, long updatedAtMillis);

    @Query("UPDATE subjects SET serverId = :serverId, syncStatus = 'SYNCED', updatedAtMillis = :updatedAtMillis WHERE localId = :localId")
    void markSubjectSynced(long localId, Long serverId, long updatedAtMillis);

    @Query("UPDATE subjects SET syncStatus = 'FAILED', updatedAtMillis = :updatedAtMillis WHERE localId = :localId")
    void markSubjectSyncFailed(long localId, long updatedAtMillis);

    @Query("UPDATE subjects SET syncStatus = 'PENDING_CREATE', updatedAtMillis = :updatedAtMillis WHERE localId = :localId")
    void markSubjectPendingCreate(long localId, long updatedAtMillis);

    @Query("UPDATE subjects SET syncStatus = 'PENDING_UPDATE', updatedAtMillis = :updatedAtMillis WHERE localId = :localId")
    void markSubjectPendingUpdate(long localId, long updatedAtMillis);

    @Query("UPDATE subjects SET syncStatus = 'PENDING_DELETE', updatedAtMillis = :updatedAtMillis WHERE localId = :localId")
    void markSubjectPendingDelete(long localId, long updatedAtMillis);

    @Query("DELETE FROM subjects")
    void deleteAll();

    @Delete
    void delete(SubjectEntity subject);

    @Query("SELECT * FROM subjects WHERE serverId = :serverId LIMIT 1")
    SubjectEntity findByServerId(Long serverId);

    @Query("SELECT * FROM subjects WHERE localId = :localId LIMIT 1")
    SubjectEntity findByLocalId(long localId);

    @Query("DELETE FROM subjects WHERE syncStatus = 'SYNCED' AND serverId IS NOT NULL")
    void deleteAllSyncedServerSubjects();

    @Query("DELETE FROM subjects WHERE syncStatus = 'SYNCED' AND serverId IS NOT NULL AND serverId NOT IN (:serverIds)")
    void deleteSyncedSubjectsNotInServerIds(List<Long> serverIds);

    @Query("DELETE FROM subjects WHERE serverId = :serverId")
    void deleteByServerId(Long serverId);

    @Query("SELECT * FROM subjects WHERE serverId = :serverId LIMIT 1")
    SubjectEntity getSubjectByServerId(long serverId);

    @Query("UPDATE subjects SET note = :note WHERE serverId = :subjectId")
    void updateSubjectNote(Long subjectId, String note);

    @Query("UPDATE subjects SET notes = :note WHERE serverId = :serverId")
    void updateSubjectNoteByServerId(long serverId, String note);

    @Query("UPDATE subjects SET title = :title, description = :description, syncStatus = 'SYNCED', updatedAtMillis = :updatedAtMillis WHERE serverId = :serverId")
    void updateSubjectFromServer(long serverId,
                                 String title,
                                 String description,
                                 long updatedAtMillis);
}