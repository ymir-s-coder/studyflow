package com.example.studyflow.fragments;

import static com.google.android.material.internal.ViewUtils.showKeyboard;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.NumberPicker;


import androidx.appcompat.app.AlertDialog;
import android.graphics.Color;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studyflow.R;
import com.example.studyflow.activities.MainActivity;
import com.example.studyflow.adapters.SessionAdapter;
import com.example.studyflow.network.responses.SessionResponseDto;
import com.example.studyflow.repository.SubjectRepository;
import com.example.studyflow.viewmodel.SessionViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubjectDetailFragment extends Fragment {

    private TextView textSubjectTitle;
    private TextView textSubjectSubtitle;
    private TextView textGoalValue;
    private TextView textFrequencyValue;
    private TextView textProgressValue;
    private ProgressBar progressSubject;
    private Button buttonStartStudy;
    private Button buttonDeleteSubject;

    private long subjectId = -1L;
    private String subjectName;
    private String subjectDescription;
    private Integer plannedTotalMinutes;
    private Integer goalMinutesPerSession;
    private String learningType;
    private String notes;

    private RecyclerView recyclerSessionHistory;
    private SessionAdapter sessionAdapter;
    private EditText editNotes;
    private Button btnSaveNote;
    private Button btnDeleteNote;
    private final List<SessionResponseDto> sessionList = new ArrayList<>();

    private SessionViewModel sessionViewModel;
    private SubjectRepository subjectRepository;

    private boolean isNoteEditing = true;

    public SubjectDetailFragment() {
        super(R.layout.fragment_subject_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textSubjectTitle = view.findViewById(R.id.textSubjectTitle);
        textSubjectSubtitle = view.findViewById(R.id.textSubjectSubtitle);
        textGoalValue = view.findViewById(R.id.textGoalValue);
        textFrequencyValue = view.findViewById(R.id.textFrequencyValue);
        textProgressValue = view.findViewById(R.id.textProgressValue);
        editNotes = view.findViewById(R.id.editNotes);
        btnSaveNote = view.findViewById(R.id.btnSaveNote);
        btnDeleteNote = view.findViewById(R.id.btnDeleteNote);
        progressSubject = view.findViewById(R.id.progressSubject);
        buttonStartStudy = view.findViewById(R.id.buttonStartStudy);
        buttonDeleteSubject = view.findViewById(R.id.buttonDeleteSubject);
        recyclerSessionHistory = view.findViewById(R.id.recyclerSessionHistory);

        subjectRepository = new SubjectRepository(requireContext());

        readArguments();

        if (subjectId == -1L || subjectId == 0L) {
            Toast.makeText(requireContext(), "Ошибка: subjectId не передан", Toast.LENGTH_SHORT).show();
            ((MainActivity) requireActivity()).returnToSubjects();
            return;
        }
        if (notes != null && !notes.trim().isEmpty()) {
            editNotes.setText(notes);
        } else {
            editNotes.setText("");
        }

        setNoteEditMode(notes == null || notes.trim().isEmpty());

        bindSubjectInfo();
        setupRecyclerView();

        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        observeViewModel();
        sessionViewModel.loadSessionsIfNeeded(subjectId);

        buttonStartStudy.setOnClickListener(v -> showHoursDialog());
        buttonDeleteSubject.setOnClickListener(v -> showDeleteSubjectDialog());
        btnSaveNote.setOnClickListener(v -> {
            android.util.Log.d("NOTE_DEBUG", "Button clicked. isNoteEditing=" + isNoteEditing);

            if (!isNoteEditing) {
                setNoteEditMode(true);
                editNotes.requestFocus();
                editNotes.setSelection(editNotes.getText().length());
                showKeyboard();
                return;
            }

            String note = editNotes.getText().toString().trim();

            android.util.Log.d("NOTE_DEBUG", "Trying to save note. subjectId=" + subjectId + ", note=" + note);

            btnSaveNote.setEnabled(false);

            subjectRepository.updateSubjectNote(subjectId, note, new SubjectRepository.UpdateNoteCallback() {
                @Override
                public void onSuccess(String savedNote) {
                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        notes = savedNote;
                        editNotes.setText(savedNote);

                        setNoteEditMode(false);

                        btnSaveNote.setEnabled(true);

                        Toast.makeText(requireContext(), "Note saved", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        btnSaveNote.setEnabled(true);

                        Toast.makeText(
                                requireContext(),
                                "Note was not saved: " + message,
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                }
            });
        });

        btnDeleteNote.setOnClickListener(v -> {
            btnDeleteNote.setEnabled(false);

            subjectRepository.updateSubjectNote(subjectId, "", new SubjectRepository.UpdateNoteCallback() {
                @Override
                public void onSuccess(String savedNote) {
                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        notes = "";
                        editNotes.setText("");

                        setNoteEditMode(true);

                        btnDeleteNote.setEnabled(true);

                        Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        btnDeleteNote.setEnabled(true);

                        Toast.makeText(
                                requireContext(),
                                "Note was not deleted: " + message,
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                }
            });
        });
    }

    private void showKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(editNotes, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void setNoteEditMode(boolean editing) {
        isNoteEditing = editing;

        editNotes.setFocusable(editing);
        editNotes.setFocusableInTouchMode(editing);
        editNotes.setCursorVisible(editing);
        editNotes.setClickable(editing);
        editNotes.setLongClickable(editing);

        if (editing) {
            btnSaveNote.setText("Save note");

            editNotes.setBackgroundResource(android.R.drawable.edit_text);
            editNotes.setTextColor(Color.BLACK);
            editNotes.setHint("Write your note for this subject...");
        } else {
            btnSaveNote.setText("Edit note");

            editNotes.clearFocus();
            editNotes.setBackgroundColor(Color.TRANSPARENT);
            editNotes.setTextColor(Color.BLACK);
            editNotes.setHint("");
        }
    }

    private void readArguments() {
        Bundle args = getArguments();

        if (args == null) {
            return;
        }

        subjectId = args.getLong("subjectId", -1L);
        subjectName = args.getString("subjectName");
        subjectDescription = args.getString("subjectDescription");

        plannedTotalMinutes = args.containsKey("plannedTotalMinutes")
                ? args.getInt("plannedTotalMinutes")
                : null;

        goalMinutesPerSession = args.containsKey("goalMinutesPerSession")
                ? args.getInt("goalMinutesPerSession")
                : null;

        learningType = args.getString("learningType");
        notes = args.getString("notes");
    }

    private void bindSubjectInfo() {
        textSubjectTitle.setText(isEmpty(subjectName) ? "Class" : subjectName);

        String description = isEmpty(subjectDescription) ? "No description" : subjectDescription;
        textSubjectSubtitle.setText(description);

        String goalText = "Total plan: " + formatMinutes(plannedTotalMinutes)
                + " / One session: " + formatMinutes(goalMinutesPerSession);
        textGoalValue.setText(goalText);

        textFrequencyValue.setText(isEmpty(learningType) ? "Learning type not set" : learningType);
        //textNotesValue.setText(isEmpty(notes) ? description : notes);

        progressSubject.setProgress(0);
        textProgressValue.setText("Studied: 0 min / Left: " + formatMinutes(plannedTotalMinutes) + " / 0%");

        android.util.Log.d("SUBJECT_DETAIL_DEBUG",
                "subjectId=" + subjectId
                        + ", plannedTotalMinutes=" + plannedTotalMinutes
                        + ", goalMinutesPerSession=" + goalMinutesPerSession
                        + ", learningType=" + learningType
        );
    }

    private void setupRecyclerView() {
        sessionAdapter = new SessionAdapter(sessionList);
        recyclerSessionHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerSessionHistory.setAdapter(sessionAdapter);
    }

    private void observeViewModel() {
        sessionViewModel.getSessionsLiveData().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null) {
                sessionAdapter.setSessions(sessions);
                bindSessionStats(sessions);
            }
        });

        sessionViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindSessionStats(List<SessionResponseDto> sessions) {
        long studiedSeconds = 0L;

        if (sessions != null) {
            for (SessionResponseDto session : sessions) {
                if (session == null || session.getDurationSeconds() == null) {
                    continue;
                }

                if (session.getDurationSeconds() > 0) {
                    studiedSeconds += session.getDurationSeconds();
                }
            }
        }

        long plannedSeconds = plannedTotalMinutes == null ? 0L : plannedTotalMinutes * 60L;
        long leftSeconds = Math.max(0L, plannedSeconds - studiedSeconds);

        int progress = 0;
        if (plannedSeconds > 0) {
            progress = (int) Math.round((studiedSeconds * 100.0) / plannedSeconds);
            progress = Math.min(progress, 100);
        }

        progressSubject.setProgress(progress);
        textProgressValue.setText(String.format(
                Locale.getDefault(),
                "Studied: %s / Left: %s / %d%%",
                formatSeconds(studiedSeconds),
                formatSeconds(leftSeconds),
                progress
        ));

        String goalText = "Total plan: " + formatMinutes(plannedTotalMinutes)
                + " / One session: " + formatMinutes(goalMinutesPerSession);

        textGoalValue.setText(goalText);

        if (sessions == null || sessions.isEmpty()) {
            textFrequencyValue.setText(isEmpty(learningType) ? "No sessions yet" : learningType);
        } else {
            textFrequencyValue.setText(isEmpty(learningType) ? "Learning type not set" : learningType);
        }
    }

    private String formatMinutes(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return "not set";
        }

        return formatSeconds(minutes * 60L);
    }

    private String formatSeconds(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours > 0 && minutes > 0) {
            return hours + " h " + minutes + " min";
        }

        if (hours > 0) {
            return hours + " h";
        }

        if (minutes > 0) {
            return minutes + " min";
        }

        return "0 min";
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void showDeleteSubjectDialog() {
        String title = subjectName != null ? subjectName : "этот урок";

        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить урок?")
                .setMessage("Урок \"" + title + "\" будет удалён. Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> deleteSubject())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteSubject() {
        if (subjectId == -1L || subjectId == 0L) {
            Toast.makeText(requireContext(), "Ошибка: subjectId = " + subjectId, Toast.LENGTH_SHORT).show();
            return;
        }

        buttonDeleteSubject.setEnabled(false);

        if (subjectId > 0) {
            subjectRepository.deleteSubject(subjectId, new SubjectRepository.DeleteSubjectCallback() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Урок удалён", Toast.LENGTH_SHORT).show();
                        ((MainActivity) requireActivity()).returnToSubjects();
                    });
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        buttonDeleteSubject.setEnabled(true);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            long localId = Math.abs(subjectId);

            subjectRepository.getLocalSubjectById(localId, subject -> {
                if (subject == null) {
                    if (!isAdded()) return;

                    requireActivity().runOnUiThread(() -> {
                        buttonDeleteSubject.setEnabled(true);
                        Toast.makeText(requireContext(), "Локальный урок не найден", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                subjectRepository.deleteSubjectOfflineFirst(subject, new SubjectRepository.DeleteSubjectCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;

                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Урок удалён", Toast.LENGTH_SHORT).show();
                            ((MainActivity) requireActivity()).returnToSubjects();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;

                        requireActivity().runOnUiThread(() -> {
                            buttonDeleteSubject.setEnabled(true);
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            });
        }
    }

    private void showHoursDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_study_time, null);

        NumberPicker inputHours = dialogView.findViewById(R.id.inputHours);
        NumberPicker inputMinutes = dialogView.findViewById(R.id.inputMinutes);
        NumberPicker inputSeconds = dialogView.findViewById(R.id.inputSeconds);

        inputHours.setMinValue(0);
        inputHours.setMaxValue(12);
        inputHours.setValue(0);
        inputHours.setWrapSelectorWheel(true);

        inputMinutes.setMinValue(0);
        inputMinutes.setMaxValue(59);
        inputMinutes.setValue(25);
        inputMinutes.setWrapSelectorWheel(true);

        inputSeconds.setMinValue(0);
        inputSeconds.setMaxValue(59);
        inputSeconds.setValue(0);
        inputSeconds.setWrapSelectorWheel(true);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Начать", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            button.setOnClickListener(v -> {
                int hours = inputHours.getValue();
                int minutes = inputMinutes.getValue();
                int seconds = inputSeconds.getValue();

                long plannedSeconds = hours * 3600L + minutes * 60L + seconds;

                if (plannedSeconds <= 0) {
                    Toast.makeText(
                            requireContext(),
                            "Выбери время больше 0",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                dialog.dismiss();
                openStudyFragment(plannedSeconds);
            });
        });

        dialog.show();
    }

    private int parseTimeValue(EditText editText) {
        String value = editText.getText().toString().trim();

        if (value.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void openStudyFragment(long plannedSeconds) {
        if (subjectId == -1L || subjectId == 0L) {
            Toast.makeText(requireContext(), "Ошибка: subjectId = " + subjectId, Toast.LENGTH_SHORT).show();
            return;
        }

        ((MainActivity) requireActivity()).openStudyFragment(
                subjectId,
                subjectName,
                plannedSeconds
        );
    }
}
