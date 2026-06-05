package com.example.studyflow.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.studyflow.repository.SessionRepository;
import com.example.studyflow.repository.SubjectRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StudyflowSyncWorker extends Worker {

    private final SubjectRepository subjectRepository;
    private final SessionRepository sessionRepository;

    public StudyflowSyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);

        subjectRepository = new SubjectRepository(context);
        sessionRepository = new SessionRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            boolean subjectsSynced = syncSubjectsBlocking();

            if (!subjectsSynced) {
                return Result.retry();
            }

            boolean sessionsSynced = syncSessionsBlocking();

            if (!sessionsSynced) {
                return Result.retry();
            }

            return Result.success();

        } catch (Exception e) {
            return Result.retry();
        }
    }

    private boolean syncSubjectsBlocking() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] finished = {false};

        subjectRepository.syncPendingSubjects(() -> {
            finished[0] = true;
            latch.countDown();
        });

        boolean completedInTime = latch.await(30, TimeUnit.SECONDS);

        return completedInTime && finished[0];
    }

    private boolean syncSessionsBlocking() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] finished = {false};

        sessionRepository.syncPendingSessions(() -> {
            finished[0] = true;
            latch.countDown();
        });

        boolean completedInTime = latch.await(60, TimeUnit.SECONDS);

        return completedInTime && finished[0];
    }
}