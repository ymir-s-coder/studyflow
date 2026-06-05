package com.example.studyflow.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.studyflow.R;
import com.example.studyflow.activities.MainActivity;
import com.example.studyflow.viewmodel.SubjectViewModel;

public class CreateSubjectFragment extends Fragment {

    private EditText editSubjectName;
    private EditText editPlannedTotalMinutes;
    private EditText editGoalMinutesPerSession;
    private EditText editDescription;
    private RadioGroup radioLearningTypeGroup;
    private RadioButton radioLecture;
    private RadioButton radioCoding;
    private Button buttonCreateSubject;

    private SubjectViewModel viewModel;

    public CreateSubjectFragment() {
        super(R.layout.fragment_create_subject);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editSubjectName = view.findViewById(R.id.editSubjectName);
        editPlannedTotalMinutes = view.findViewById(R.id.editPlannedTotalMinutes);
        editGoalMinutesPerSession = view.findViewById(R.id.editGoalMinutesPerSession);
        editDescription = view.findViewById(R.id.editDescription);
        radioLearningTypeGroup = view.findViewById(R.id.radioLearningTypeGroup);
        radioLecture = view.findViewById(R.id.radioLecture);
        radioCoding = view.findViewById(R.id.radioCoding);
        buttonCreateSubject = view.findViewById(R.id.buttonCreateSubject);

        editPlannedTotalMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        editGoalMinutesPerSession.setInputType(InputType.TYPE_CLASS_NUMBER);

        viewModel = new ViewModelProvider(requireActivity()).get(SubjectViewModel.class);

        observeViewModel();

        buttonCreateSubject.setOnClickListener(v -> createSubject());
    }

    private void observeViewModel() {
        viewModel.getSubjectCreated().observe(getViewLifecycleOwner(), created -> {
            if (created != null && created) {
                Toast.makeText(requireContext(), "Class saved", Toast.LENGTH_SHORT).show();
                viewModel.resetSubjectCreated();
                ((MainActivity) requireActivity()).returnToSubjects();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.trim().isEmpty()) {
                buttonCreateSubject.setEnabled(true);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            buttonCreateSubject.setEnabled(!Boolean.TRUE.equals(isLoading));
        });
    }

    private void createSubject() {
        String title = editSubjectName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            editSubjectName.setError("Enter class name");
            editSubjectName.requestFocus();
            return;
        }

        Integer plannedTotalMinutes = readPositiveInt(
                editPlannedTotalMinutes,
                "Enter total planned study time"
        );
        if (plannedTotalMinutes == null) {
            return;
        }

        Integer goalMinutesPerSession = readPositiveInt(
                editGoalMinutesPerSession,
                "Enter planned time for one session"
        );
        if (goalMinutesPerSession == null) {
            return;
        }

        String learningType = getSelectedLearningType();

        buttonCreateSubject.setEnabled(false);

        viewModel.createSubject(
                title,
                description,
                plannedTotalMinutes,
                goalMinutesPerSession,
                learningType,
                description
        );
    }

    private Integer readPositiveInt(EditText editText, String errorMessage) {
        String value = editText.getText().toString().trim();

        if (TextUtils.isEmpty(value)) {
            editText.setError(errorMessage);
            editText.requestFocus();
            return null;
        }

        try {
            int number = Integer.parseInt(value);

            if (number <= 0) {
                editText.setError("Value must be greater than 0");
                editText.requestFocus();
                return null;
            }

            return number;
        } catch (NumberFormatException e) {
            editText.setError("Only numbers are allowed");
            editText.requestFocus();
            return null;
        }
    }

    private String getSelectedLearningType() {
        int checkedId = radioLearningTypeGroup.getCheckedRadioButtonId();

        if (checkedId == R.id.radioCoding) {
            return "Writing code";
        }

        return "Watching lectures";
    }
}
