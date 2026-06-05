package com.example.studyflow.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.studyflow.R;
import com.example.studyflow.activities.MainActivity;
import com.example.studyflow.models.MicroCheckpoint;
import com.example.studyflow.utils.CircleTimerView;
import com.example.studyflow.utils.DialogUtils;
import com.example.studyflow.viewmodel.StudyViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudyFragment extends Fragment {

    private long subjectId = -1L;
    private String subjectName;

    private TextView textSubjectName;
    private TextView textTime;
    private CircleTimerView circleTimer;

    private long totalSeconds;
    private long remainingSeconds;

    private long sessionStartTimeMillis;
    private long accumulatedStudiedMillis = 0L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean isRunning = false;
    private boolean isTimerStarted = false;

    private Button buttonPause;
    private Button buttonFinish;
    private StudyViewModel studyViewModel;
    private long currentSessionLocalId = -1L;
    private final List<MicroCheckpoint> microCheckpoints = new ArrayList<>();

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                long currentStudiedSeconds = getCurrentStudiedSeconds();

                remainingSeconds = totalSeconds - currentStudiedSeconds;

                if (remainingSeconds <= 0) {
                    remainingSeconds = 0;
                    finishStudySession();
                    return;
                }

                updateUI();
                handler.postDelayed(this, 1000);
            }
        }
    };

    public StudyFragment() {
        super(R.layout.fragment_study);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textSubjectName = view.findViewById(R.id.textSubjectName);
        textTime = view.findViewById(R.id.textTime);
        circleTimer = view.findViewById(R.id.circleTimer);
        buttonPause = view.findViewById(R.id.buttonPause);
        buttonFinish = view.findViewById(R.id.buttonFinish);

        readArguments();

        studyViewModel = new ViewModelProvider(requireActivity()).get(StudyViewModel.class);

        studyViewModel.getCurrentSessionLocalId().observe(getViewLifecycleOwner(), sessionLocalId -> {
            if (sessionLocalId != null) {
                currentSessionLocalId = sessionLocalId;
            }
        });

        if (subjectId == 0) {
            Toast.makeText(requireContext(), "Ошибка: subjectId не передан", Toast.LENGTH_SHORT).show();
            ((MainActivity) requireActivity()).returnToSubjects();
            return;
        }

        if (totalSeconds <= 0) {
            totalSeconds = 3600L;
        }

        remainingSeconds = totalSeconds;

        textSubjectName.setText(
                subjectName != null ? subjectName + " Study" : "Study"
        );

        updateUI();

        buttonPause.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();

                DialogUtils.showPauseSurveyDialog(requireContext(), checkpoint -> {
                    microCheckpoints.add(checkpoint);

                    if (currentSessionLocalId > 0) {
                        studyViewModel.saveMicroCheckpoint(
                                currentSessionLocalId,
                                checkpoint.getDistractionCountRange(),
                                checkpoint.getMood(),
                                checkpoint.getBreakReason(),
                                checkpoint.getConcentrationLevel()
                        );
                    }

                    Toast.makeText(
                            requireContext(),
                            "Опрос сохранён",
                            Toast.LENGTH_SHORT
                    ).show();
                });

            } else {
                resumeTimer();
            }
        });

        buttonFinish.setOnClickListener(v -> finishStudySession());

        studyViewModel.startSession(subjectId, "DEFAULT");
        startTimer();
    }

    private void readArguments() {
        if (getArguments() != null) {
            subjectId = getArguments().getLong("subjectId", -1L);
            subjectName = getArguments().getString("subjectName");
            totalSeconds = getArguments().getLong("plannedSeconds", 3600L);
        }
    }

    private void startTimer() {
        if (isTimerStarted) {
            return;
        }

        isTimerStarted = true;
        isRunning = true;
        sessionStartTimeMillis = System.currentTimeMillis();

        buttonPause.setText("Pause");

        handler.post(timerRunnable);
    }

    private void pauseTimer() {
        if (!isRunning) {
            return;
        }

        accumulatedStudiedMillis += System.currentTimeMillis() - sessionStartTimeMillis;

        isRunning = false;
        buttonPause.setText("Resume");

        handler.removeCallbacks(timerRunnable);
    }

    private void resumeTimer() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        sessionStartTimeMillis = System.currentTimeMillis();

        buttonPause.setText("Pause");

        handler.post(timerRunnable);
    }

    private long getCurrentStudiedSeconds() {
        long studiedMillis = accumulatedStudiedMillis;

        if (isRunning) {
            studiedMillis += System.currentTimeMillis() - sessionStartTimeMillis;
        }

        return studiedMillis / 1000;
    }

    private void updateUI() {
        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;
        long seconds = remainingSeconds % 60;

        String time = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
        );

        textTime.setText(time);

        float progress = 1f - ((float) remainingSeconds / totalSeconds);
        circleTimer.setProgress(progress);
    }

    private void finishStudySession() {
        if (subjectId == 0) {
            Toast.makeText(requireContext(), "Ошибка: subjectId = " + subjectId, Toast.LENGTH_SHORT).show();
            return;
        }

        long studiedSeconds = getCurrentStudiedSeconds();

        if (studiedSeconds > totalSeconds) {
            studiedSeconds = totalSeconds;
        }

        if (studiedSeconds <= 0) {
            studiedSeconds = 1L;
        }

        isRunning = false;
        isTimerStarted = false;
        handler.removeCallbacks(timerRunnable);

        ((MainActivity) requireActivity()).openResultFragment(
                subjectId,
                subjectName,
                studiedSeconds,
                totalSeconds,
                new ArrayList<>(microCheckpoints),
                currentSessionLocalId
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        handler.removeCallbacks(timerRunnable);
        isRunning = false;
        isTimerStarted = false;
    }
}