package com.example.studyflow.repository;

import android.content.Context;

import com.example.studyflow.network.ApiClient;
import com.example.studyflow.network.ApiService;
import com.example.studyflow.network.requests.CreateSubjectRequestDto;
import com.example.studyflow.network.requests.SubjectNoteRequestDto;
import com.example.studyflow.network.responses.SubjectResponseDto;
import com.example.studyflow.storage.local.StudyflowDatabase;
import com.example.studyflow.storage.local.SubjectDao;
import com.example.studyflow.storage.local.SubjectEntity;
import com.example.studyflow.storage.local.session.LocalStudySessionDao;
import com.example.studyflow.sync.SyncStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubjectRepository {

    private final ApiService apiService;
    private final SubjectDao subjectDao;
    private final LocalStudySessionDao sessionDao;
    private final ExecutorService executorService;

    public SubjectRepository(Context context) {
        apiService = ApiClient.getApiService(context);

        StudyflowDatabase database = StudyflowDatabase.getInstance(context);
        subjectDao = database.subjectDao();
        sessionDao = database.localStudySessionDao();

        executorService = Executors.newSingleThreadExecutor();
    }

    public void syncSubjectsFromServer() {
        apiService.getSubjects().enqueue(new Callback<List<SubjectResponseDto>>() {
            @Override
            public void onResponse(Call<List<SubjectResponseDto>> call,
                                   Response<List<SubjectResponseDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                saveServerSubjectsToLocal(response.body(), null);
            }

            @Override
            public void onFailure(Call<List<SubjectResponseDto>> call, Throwable t) {
                // Приложение продолжает работать из Room.
            }
        });
    }

    // Получить предметы с сервера
    public Call<List<SubjectResponseDto>> getSubjects() {
        return apiService.getSubjects();
    }

    // Получить предметы из Room
    public void getLocalSubjects(LocalSubjectsCallback callback) {
        executorService.execute(() -> {
            List<SubjectEntity> subjects = subjectDao.getAllSubjects();

            if (callback != null) {
                callback.onResult(subjects);
            }
        });
    }

    // Создать предмет offline-first
    public void createSubjectOfflineFirst(
            String title,
            String description,
            Integer plannedTotalMinutes,
            Integer goalMinutesPerSession,
            String learningType,
            String notes,
            CreateSubjectCallback callback
    ) {
        executorService.execute(() -> {
            SubjectEntity localSubject = new SubjectEntity(
                    title,
                    description,
                    plannedTotalMinutes,
                    goalMinutesPerSession,
                    learningType,
                    notes
            );

            long localId = subjectDao.insert(localSubject);
            localSubject.localId = localId;

            if (callback != null) {
                callback.onLocalSaved(localSubject);
            }

            // Здесь не вызываем createSubject().
            // Отправка на сервер должна идти только через syncPendingSubjects().
        });
    }

    public void saveServerSubjectsToLocal(
            List<SubjectResponseDto> serverSubjects,
            Runnable callback
    ) {
        executorService.execute(() -> {
            if (serverSubjects == null) {
                if (callback != null) {
                    callback.run();
                }
                return;
            }

            if (serverSubjects.isEmpty()) {
                subjectDao.deleteAllSyncedServerSubjects();

                if (callback != null) {
                    callback.run();
                }

                return;
            }

            List<Long> serverIds = new ArrayList<>();

            for (SubjectResponseDto dto : serverSubjects) {
                if (dto == null || dto.getId() == null) {
                    continue;
                }

                serverIds.add(dto.getId());

                SubjectEntity existing = subjectDao.findByServerId(dto.getId());

                String title = safeString(dto.getTitle());
                String description = safeString(dto.getDescription());

                int plannedTotalMinutes = safeInt(dto.getPlannedTotalMinutes());
                int goalMinutesPerSession = safeInt(dto.getGoalMinutesPerSession());

                String learningType = dto.getLearningType() != null
                        ? dto.getLearningType()
                        : dto.getStudyFrequency();

                learningType = safeString(learningType);

                String notes = safeString(dto.getNotes());

                if (existing == null) {
                    SubjectEntity entity = new SubjectEntity(
                            title,
                            description,
                            plannedTotalMinutes,
                            goalMinutesPerSession,
                            learningType,
                            notes
                    );

                    entity.serverId = dto.getId();
                    entity.learningType = learningType;
                    entity.studyFrequency = learningType;
                    entity.notes = notes;
                    entity.syncStatus = SyncStatus.SYNCED;
                    entity.createdAtMillis = System.currentTimeMillis();
                    entity.updatedAtMillis = System.currentTimeMillis();

                    subjectDao.insert(entity);
                } else {
                    existing.title = title;
                    existing.description = description;

                    existing.plannedTotalMinutes = plannedTotalMinutes;
                    existing.goalMinutesPerSession = goalMinutesPerSession;
                    existing.learningType = learningType;
                    existing.studyFrequency = learningType;
                    existing.notes = notes;

                    existing.syncStatus = SyncStatus.SYNCED;
                    existing.updatedAtMillis = System.currentTimeMillis();

                    subjectDao.update(existing);
                }
            }

            subjectDao.deleteSyncedSubjectsNotInServerIds(serverIds);

            if (callback != null) {
                callback.run();
            }
        });
    }

    public Long getTotalStudiedSecondsBySubjectId(long subjectId) {
        return sessionDao.getTotalStudiedSecondsBySubjectId(subjectId);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    public void syncPendingSubjects(SyncCallback callback) {
        executorService.execute(() -> {
            List<SubjectEntity> pendingSubjects = subjectDao.getPendingCreateSubjects();

            if (pendingSubjects == null || pendingSubjects.isEmpty()) {
                if (callback != null) {
                    callback.onFinished();
                }
                return;
            }

            syncPendingSubjectAtIndex(pendingSubjects, 0, callback);
        });
    }

    private void syncPendingSubjectAtIndex(
            List<SubjectEntity> pendingSubjects,
            int index,
            SyncCallback callback
    ) {
        if (index >= pendingSubjects.size()) {
            if (callback != null) {
                callback.onFinished();
            }
            return;
        }

        SubjectEntity subject = pendingSubjects.get(index);

        if (subject.serverId != null) {
            executorService.execute(() ->
                    syncPendingSubjectAtIndex(pendingSubjects, index + 1, callback)
            );
            return;
        }

        executorService.execute(() -> {
            int changedRows = subjectDao.markSubjectSyncing(
                    subject.localId,
                    System.currentTimeMillis()
            );

            if (changedRows == 0) {
                syncPendingSubjectAtIndex(pendingSubjects, index + 1, callback);
                return;
            }

            subject.updatedAtMillis = System.currentTimeMillis();

            CreateSubjectRequestDto requestDto = new CreateSubjectRequestDto(
                    subject.title,
                    subject.description,
                    subject.plannedTotalMinutes,
                    subject.goalMinutesPerSession,
                    subject.learningType,
                    subject.notes
            );

            apiService.createSubject(requestDto).enqueue(new Callback<SubjectResponseDto>() {
                @Override
                public void onResponse(
                        Call<SubjectResponseDto> call,
                        Response<SubjectResponseDto> response
                ) {
                    executorService.execute(() -> {
                        if (response.isSuccessful() && response.body() != null) {
                            SubjectResponseDto serverSubject = response.body();

                            subjectDao.markSubjectSynced(
                                    subject.localId,
                                    serverSubject.getId(),
                                    System.currentTimeMillis()
                            );

                            sessionDao.updateSubjectServerIdForPendingSessions(
                                    subject.localId,
                                    serverSubject.getId(),
                                    System.currentTimeMillis()
                            );

                            subject.serverId = serverSubject.getId();
                            subject.syncStatus = SyncStatus.SYNCED;
                            subject.updatedAtMillis = System.currentTimeMillis();
                        } else {
                            subjectDao.markSubjectPendingCreate(
                                    subject.localId,
                                    System.currentTimeMillis()
                            );

                            subject.syncStatus = SyncStatus.PENDING_CREATE;
                            subject.updatedAtMillis = System.currentTimeMillis();
                        }

                        syncPendingSubjectAtIndex(pendingSubjects, index + 1, callback);
                    });
                }

                @Override
                public void onFailure(Call<SubjectResponseDto> call, Throwable t) {
                    executorService.execute(() -> {
                        subjectDao.markSubjectPendingCreate(
                                subject.localId,
                                System.currentTimeMillis()
                        );

                        subject.syncStatus = SyncStatus.PENDING_CREATE;
                        subject.updatedAtMillis = System.currentTimeMillis();

                        syncPendingSubjectAtIndex(pendingSubjects, index + 1, callback);
                    });
                }
            });
        });
    }

    // Старый метод. Можно оставить, но лучше больше не использовать для создания subject.
    public Call<SubjectResponseDto> createSubject(String title, String description) {
        return apiService.createSubject(new CreateSubjectRequestDto(title, description));
    }

    public void deleteSubject(Long subjectId, DeleteSubjectCallback callback) {
        apiService.deleteSubject(subjectId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    executorService.execute(() -> {
                        subjectDao.deleteByServerId(subjectId);

                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                } else {
                    if (callback != null) {
                        callback.onError("Не удалось удалить урок");
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (callback != null) {
                    callback.onError(t.getMessage());
                }
            }
        });
    }

    public void deleteSubjectOfflineFirst(
            SubjectEntity subject,
            DeleteSubjectCallback callback
    ) {
        executorService.execute(() -> {
            if (subject.serverId == null) {
                subjectDao.delete(subject);

                if (callback != null) {
                    callback.onSuccess();
                }

                return;
            }

            subject.syncStatus = SyncStatus.PENDING_DELETE;
            subject.updatedAtMillis = System.currentTimeMillis();

            subjectDao.update(subject);

            apiService.deleteSubject(subject.serverId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    executorService.execute(() -> {
                        if (response.isSuccessful()) {
                            subjectDao.deleteByServerId(subject.serverId);

                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            if (callback != null) {
                                callback.onError("Deleted locally, but failed to delete on server");
                            }
                        }
                    });
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    if (callback != null) {
                        callback.onError("Subject marked for delete. Server sync will retry later.");
                    }
                }
            });
        });
    }

    public void getLocalSubjectById(long localId, LocalSubjectCallback callback) {
        executorService.execute(() -> {
            SubjectEntity subject = subjectDao.findByLocalId(localId);

            if (callback != null) {
                callback.onResult(subject);
            }
        });
    }

    public interface LocalSubjectsCallback {
        void onResult(List<SubjectEntity> subjects);
    }

    public void updateSubjectNote(long subjectId, String note, UpdateNoteCallback callback) {
        android.util.Log.d("NOTE_DEBUG", "updateSubjectNote called. subjectId=" + subjectId + ", note=" + note);

        if (subjectId <= 0) {
            executorService.execute(() -> {
                long localId = Math.abs(subjectId);
                subjectDao.updateSubjectNote(localId, note);

                if (callback != null) {
                    callback.onSuccess(note);
                }
            });
            return;
        }

        SubjectNoteRequestDto requestDto = new SubjectNoteRequestDto(note);

        android.util.Log.d("NOTE_DEBUG", "Sending PATCH /subjects/" + subjectId + "/notes");

        apiService.updateSubjectNotes(subjectId, requestDto).enqueue(new Callback<SubjectResponseDto>() {
            @Override
            public void onResponse(Call<SubjectResponseDto> call, Response<SubjectResponseDto> response) {
                android.util.Log.d("NOTE_DEBUG", "PATCH response code=" + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    SubjectResponseDto updatedSubject = response.body();
                    String savedNote = updatedSubject.getNotes();

                    android.util.Log.d("NOTE_DEBUG", "Server saved note=" + savedNote);

                    executorService.execute(() -> {
                        subjectDao.updateSubjectNoteByServerId(subjectId, savedNote);

                        if (callback != null) {
                            callback.onSuccess(savedNote);
                        }
                    });
                } else {
                    String message = "Failed to update note. Code=" + response.code();
                    android.util.Log.e("NOTE_DEBUG", message);

                    if (callback != null) {
                        callback.onError(message);
                    }
                }
            }

            @Override
            public void onFailure(Call<SubjectResponseDto> call, Throwable t) {
                String message = t.getMessage() == null ? "Network error" : t.getMessage();

                android.util.Log.e("NOTE_DEBUG", "Note update error: " + message, t);

                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }
    public interface CreateSubjectCallback {
        void onLocalSaved(SubjectEntity subject);

        void onSynced(SubjectEntity subject);

        void onSyncFailed(SubjectEntity subject);
    }

    public interface DeleteSubjectCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface SyncCallback {
        void onFinished();
    }

    public interface LocalSubjectCallback {
        void onResult(SubjectEntity subject);
    }
    public interface UpdateNoteCallback {
        void onSuccess(String savedNote);

        void onError(String message);
    }
}