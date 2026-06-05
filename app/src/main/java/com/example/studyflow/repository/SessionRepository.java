package com.example.studyflow.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.studyflow.network.ApiClient;
import com.example.studyflow.network.ApiService;
import com.example.studyflow.network.requests.FinishSessionRequestDto;
import com.example.studyflow.network.requests.MicroCheckpointRequestDto;
import com.example.studyflow.network.responses.SessionResponseDto;
import com.example.studyflow.storage.local.StudyflowDatabase;
import com.example.studyflow.storage.local.SubjectEntity;
import com.example.studyflow.storage.local.session.LocalMicroCheckpointEntity;
import com.example.studyflow.storage.local.session.LocalStudySessionDao;
import com.example.studyflow.storage.local.session.LocalStudySessionEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionRepository {

    private final ApiService apiService;
    private final LocalStudySessionDao sessionDao;
    private final ExecutorService executorService;

    public SessionRepository(Context context) {
        apiService = ApiClient.getApiService(context);
        sessionDao = StudyflowDatabase.getInstance(context).localStudySessionDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void startLocalSession(
            Long subjectId,
            String learningType,
            StartSessionCallback callback
    ) {
        executorService.execute(() -> {
            LocalStudySessionEntity session = new LocalStudySessionEntity();

            if (subjectId != null && subjectId > 0) {
                session.subjectServerId = subjectId;
            } else if (subjectId != null) {
                session.subjectLocalId = Math.abs(subjectId);
            }

            long now = System.currentTimeMillis();

            session.learningType = learningType;
            session.startedAtMillis = now;
            session.createdAtMillis = now;
            session.updatedAtMillis = now;
            session.synced = false;
            session.syncStatus = "PENDING";

            long localId = sessionDao.insertSession(session);
            session.localId = localId;

            if (callback != null) {
                callback.onStarted(session);
            }
        });
    }

    public void saveMicroCheckpoint(
            long sessionLocalId,
            String distractionCountRange,
            String mood,
            String breakReason,
            Integer concentrationLevel,
            SaveCallback callback
    ) {
        executorService.execute(() -> {
            LocalMicroCheckpointEntity checkpoint = new LocalMicroCheckpointEntity(
                    sessionLocalId,
                    distractionCountRange,
                    mood,
                    breakReason,
                    concentrationLevel
            );

            sessionDao.insertMicroCheckpoint(checkpoint);

            LocalStudySessionEntity session = sessionDao.getSessionByLocalId(sessionLocalId);

            if (session != null) {
                session.hadBreak = true;
                session.breakCount = session.breakCount + 1;
                session.updatedAtMillis = System.currentTimeMillis();
                session.synced = false;
                session.syncStatus = "PENDING";
                sessionDao.updateSession(session);
            }

            if (callback != null) {
                callback.onSaved();
            }
        });
    }

    public void finishLocalSession(
            long sessionLocalId,
            long endedAtMillis,
            long durationSeconds,
            long plannedSeconds,
            String studyPlace,
            String studyEnvironment,
            String helpfulFactors,
            String disturbingFactors,
            Integer productivityLevel,
            Integer fatigue,
            String notes,
            String difficultyLevel,
            Boolean needReview,
            String fatigueLevel,
            Integer understandingLevel,
            SaveCallback callback
    ) {
        executorService.execute(() -> {
            LocalStudySessionEntity session = sessionDao.getSessionByLocalId(sessionLocalId);

            if (session == null) {
                if (callback != null) {
                    callback.onSaved();
                }
                return;
            }

            session.endedAtMillis = endedAtMillis;
            session.durationSeconds = durationSeconds;
            session.plannedSeconds = plannedSeconds;

            session.studyPlace = studyPlace;
            session.studyEnvironment = studyEnvironment;
            session.helpfulFactors = helpfulFactors;
            session.disturbingFactors = disturbingFactors;
            session.productivityLevel = productivityLevel;
            session.fatigue = fatigue;
            session.notes = notes;
            session.difficultyLevel = difficultyLevel;
            session.needReview = needReview;
            session.fatigueLevel = fatigueLevel;
            session.understandingLevel = understandingLevel;

            session.synced = false;
            session.syncStatus = "PENDING";
            session.updatedAtMillis = System.currentTimeMillis();

            sessionDao.updateSession(session);

            if (callback != null) {
                callback.onSaved();
            }
        });
    }

    public void getLocalSessionsBySubjectId(
            long subjectId,
            LocalSessionsCallback callback
    ) {
        executorService.execute(() -> {
            long lookupId = subjectId < 0 ? Math.abs(subjectId) : subjectId;

            List<LocalStudySessionEntity> sessions =
                    sessionDao.getSessionsBySubjectId(lookupId);

            if (callback != null) {
                callback.onResult(sessions);
            }
        });
    }

    public void getPendingSessions(PendingSessionsCallback callback) {
        executorService.execute(() -> {
            List<LocalStudySessionEntity> sessions = sessionDao.getPendingSessions();

            if (callback != null) {
                callback.onResult(sessions);
            }
        });
    }

    public void syncPendingSessions(SyncSessionsCallback callback) {
        executorService.execute(() -> {
            List<LocalStudySessionEntity> pendingSessions = sessionDao.getPendingSessions();

            if (pendingSessions == null || pendingSessions.isEmpty()) {
                if (callback != null) {
                    callback.onFinished();
                }
                return;
            }

            syncPendingSessionAtIndex(pendingSessions, 0, callback);
        });
    }

    private void syncPendingSessionAtIndex(
            List<LocalStudySessionEntity> pendingSessions,
            int index,
            SyncSessionsCallback callback
    ) {
        if (index >= pendingSessions.size()) {
            if (callback != null) {
                callback.onFinished();
            }
            return;
        }

        LocalStudySessionEntity session = pendingSessions.get(index);

        if (session.subjectServerId == null || session.subjectServerId <= 0) {
            sessionDao.markSessionSyncFailed(
                    session.localId,
                    System.currentTimeMillis()
            );

            syncPendingSessionAtIndex(pendingSessions, index + 1, callback);
            return;
        }

        List<LocalMicroCheckpointEntity> checkpoints =
                sessionDao.getMicroCheckpointsBySessionLocalId(session.localId);

        FinishSessionRequestDto request = new FinishSessionRequestDto(
                session.subjectServerId,
                session.durationSeconds,
                session.plannedSeconds,
                session.productivityLevel,
                session.fatigue,
                session.notes,
                session.studyPlace,
                session.studyEnvironment,
                splitTextToList(session.helpfulFactors),
                splitTextToList(session.disturbingFactors),
                session.difficultyLevel,
                session.needReview,
                session.fatigueLevel,
                session.understandingLevel,
                mapLocalCheckpointsToDto(checkpoints)
        );

        apiService.finishSession(request).enqueue(new Callback<SessionResponseDto>() {
            @Override
            public void onResponse(
                    @NonNull Call<SessionResponseDto> call,
                    @NonNull Response<SessionResponseDto> response
            ) {
                executorService.execute(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        SessionResponseDto savedSession = response.body();

                        sessionDao.markSessionSynced(
                                session.localId,
                                savedSession.getId(),
                                System.currentTimeMillis()
                        );
                    } else {
                        sessionDao.markSessionSyncFailed(
                                session.localId,
                                System.currentTimeMillis()
                        );
                    }

                    syncPendingSessionAtIndex(pendingSessions, index + 1, callback);
                });
            }

            @Override
            public void onFailure(
                    @NonNull Call<SessionResponseDto> call,
                    @NonNull Throwable t
            ) {
                executorService.execute(() -> {
                    sessionDao.markSessionPending(
                            session.localId,
                            System.currentTimeMillis()
                    );

                    syncPendingSessionAtIndex(pendingSessions, index + 1, callback);
                });
            }
        });
    }

    private ArrayList<MicroCheckpointRequestDto> mapLocalCheckpointsToDto(
            List<LocalMicroCheckpointEntity> checkpoints
    ) {
        ArrayList<MicroCheckpointRequestDto> result = new ArrayList<>();

        if (checkpoints == null) {
            return result;
        }

        for (LocalMicroCheckpointEntity checkpoint : checkpoints) {
            result.add(new MicroCheckpointRequestDto(
                    checkpoint.distractionCountRange,
                    checkpoint.mood,
                    checkpoint.breakReason,
                    checkpoint.concentrationLevel,
                    checkpoint.createdAtMillis
            ));
        }

        return result;
    }

    private List<String> splitTextToList(String text) {
        if (text == null || text.trim().isEmpty() || text.equals("Не выбрано")) {
            return new ArrayList<>();
        }

        return new ArrayList<>(Arrays.asList(text.split("\\s*,\\s*")));
    }

    public interface StartSessionCallback {
        void onStarted(LocalStudySessionEntity session);
    }

    public interface SaveCallback {
        void onSaved();
    }

    public interface LocalSessionsCallback {
        void onResult(List<LocalStudySessionEntity> sessions);
    }

    public interface PendingSessionsCallback {
        void onResult(List<LocalStudySessionEntity> sessions);
    }

    public interface SyncSessionsCallback {
        void onFinished();
    }
}