package com.example.studyflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.studyflow.R;
import com.example.studyflow.activities.MainActivity;
import com.example.studyflow.models.MicroCheckpoint;
import com.example.studyflow.network.ApiClient;
import com.example.studyflow.network.ApiService;
import com.example.studyflow.network.requests.FinishSessionRequestDto;
import com.example.studyflow.network.requests.MicroCheckpointRequestDto;
import com.example.studyflow.network.responses.SessionResponseDto;
import com.example.studyflow.utils.AuthErrorHandler;
import com.example.studyflow.viewmodel.SessionViewModel;
import com.example.studyflow.viewmodel.StudyViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultFragment extends Fragment {

    private LinearLayout sessionReviewStepOneLayout;
    private LinearLayout sessionReviewStepTwoLayout;

    private ProgressBar sessionReviewProgressBar;

    private Button buttonSessionReviewNext;
    private Button buttonSessionReviewBack;
    private Button buttonSaveSessionReview;

    private Button buttonNeedReviewYes;
    private Button buttonNeedReviewNo;

    private RatingBar ratingProductivity;

    private ChipGroup chipGroupStudyPlace;
    private ChipGroup chipGroupStudyEnvironment;
    private ChipGroup chipGroupHelpfulFactors;
    private ChipGroup chipGroupDisturbingFactors;
    private ChipGroup chipGroupDifficulty;
    private ChipGroup chipGroupFatigue;
    private ChipGroup chipGroupUnderstanding;

    private Boolean needReview = null;

    private long subjectId = -1L;
    private String subjectName;
    private long studiedSeconds;
    private long plannedSeconds;

    private ArrayList<MicroCheckpoint> microCheckpoints = new ArrayList<>();

    private SessionViewModel sessionViewModel;
    private StudyViewModel studyViewModel;
    private long sessionLocalId = -1L;

    public ResultFragment() {
        super(R.layout.fragment_result);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        sessionViewModel = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        studyViewModel = new ViewModelProvider(requireActivity()).get(StudyViewModel.class);

        readArguments();
        initViews(view);
        setupDefaultValues();
        setupListeners();

        if (subjectId == 0) {
            Toast.makeText(requireContext(), "Ошибка: subjectId не передан", Toast.LENGTH_SHORT).show();
            ((MainActivity) requireActivity()).returnToSubjects();
        }
    }

    private void readArguments() {
        Bundle args = getArguments();

        if (args == null) {
            return;
        }

        sessionLocalId = args.getLong("sessionLocalId", -1L);
        subjectId = args.getLong("subjectId", -1L);
        subjectName = args.getString("subjectName");
        studiedSeconds = args.getLong("studiedSeconds", 0L);
        plannedSeconds = args.getLong("plannedSeconds", 0L);

        Object data = args.getSerializable("microCheckpoints");

        if (data instanceof ArrayList<?>) {
            microCheckpoints = (ArrayList<MicroCheckpoint>) data;
        }
    }

    private void initViews(View view) {
        sessionReviewStepOneLayout = view.findViewById(R.id.sessionReviewStepOneLayout);
        sessionReviewStepTwoLayout = view.findViewById(R.id.sessionReviewStepTwoLayout);

        sessionReviewProgressBar = view.findViewById(R.id.sessionReviewProgressBar);

        buttonSessionReviewNext = view.findViewById(R.id.buttonSessionReviewNext);
        buttonSessionReviewBack = view.findViewById(R.id.buttonSessionReviewBack);
        buttonSaveSessionReview = view.findViewById(R.id.buttonSaveSessionReview);

        buttonNeedReviewYes = view.findViewById(R.id.buttonNeedReviewYes);
        buttonNeedReviewNo = view.findViewById(R.id.buttonNeedReviewNo);

        ratingProductivity = view.findViewById(R.id.ratingProductivity);

        chipGroupStudyPlace = view.findViewById(R.id.chipGroupStudyPlace);
        chipGroupStudyEnvironment = view.findViewById(R.id.chipGroupStudyEnvironment);
        chipGroupHelpfulFactors = view.findViewById(R.id.chipGroupHelpfulFactors);
        chipGroupDisturbingFactors = view.findViewById(R.id.chipGroupDisturbingFactors);
        chipGroupDifficulty = view.findViewById(R.id.chipGroupDifficulty);
        chipGroupFatigue = view.findViewById(R.id.chipGroupFatigue);
        chipGroupUnderstanding = view.findViewById(R.id.chipGroupUnderstanding);
    }

    private void setupDefaultValues() {
        sessionReviewStepOneLayout.setVisibility(View.VISIBLE);
        sessionReviewStepTwoLayout.setVisibility(View.GONE);
        sessionReviewProgressBar.setProgress(1);

        ratingProductivity.setRating(3);

        checkChipIfExists(chipGroupStudyPlace, R.id.chipHome);
        checkChipIfExists(chipGroupStudyEnvironment, R.id.chipSilence);
        checkChipIfExists(chipGroupDifficulty, R.id.chipMedium);
        checkChipIfExists(chipGroupFatigue, R.id.chipNormalTired);
        checkChipIfExists(chipGroupUnderstanding, R.id.chipUnderstoodPartly);

        setNeedReview(null);
    }

    private void setupListeners() {
        buttonSessionReviewNext.setOnClickListener(v -> {
            if (!validateStepOne()) {
                return;
            }

            showStepTwo();
        });

        buttonSessionReviewBack.setOnClickListener(v -> showStepOne());

        buttonNeedReviewYes.setOnClickListener(v -> setNeedReview(true));
        buttonNeedReviewNo.setOnClickListener(v -> setNeedReview(false));

        buttonSaveSessionReview.setOnClickListener(v -> {
            if (!validateStepTwo()) {
                return;
            }

            saveResult();
        });

        setupDisturbingNothingLogic();
    }

    private void setupDisturbingNothingLogic() {
        Chip chipNothing = chipGroupDisturbingFactors.findViewById(R.id.chipDisturbNothing);

        if (chipNothing == null) {
            return;
        }

        chipNothing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                return;
            }

            for (int i = 0; i < chipGroupDisturbingFactors.getChildCount(); i++) {
                View child = chipGroupDisturbingFactors.getChildAt(i);

                if (child instanceof Chip && child.getId() != R.id.chipDisturbNothing) {
                    ((Chip) child).setChecked(false);
                }
            }
        });

        for (int i = 0; i < chipGroupDisturbingFactors.getChildCount(); i++) {
            View child = chipGroupDisturbingFactors.getChildAt(i);

            if (child instanceof Chip && child.getId() != R.id.chipDisturbNothing) {
                ((Chip) child).setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        chipNothing.setChecked(false);
                    }
                });
            }
        }
    }

    private void showStepOne() {
        sessionReviewStepOneLayout.setVisibility(View.VISIBLE);
        sessionReviewStepTwoLayout.setVisibility(View.GONE);
        sessionReviewProgressBar.setProgress(1);
    }

    private void showStepTwo() {
        sessionReviewStepOneLayout.setVisibility(View.GONE);
        sessionReviewStepTwoLayout.setVisibility(View.VISIBLE);
        sessionReviewProgressBar.setProgress(2);
    }

    private void setNeedReview(Boolean value) {
        needReview = value;

        if (value == null) {
            buttonNeedReviewYes.setAlpha(0.65f);
            buttonNeedReviewNo.setAlpha(0.65f);
            return;
        }

        if (value) {
            buttonNeedReviewYes.setAlpha(1.0f);
            buttonNeedReviewNo.setAlpha(0.45f);
        } else {
            buttonNeedReviewYes.setAlpha(0.45f);
            buttonNeedReviewNo.setAlpha(1.0f);
        }
    }

    private boolean validateStepOne() {
        if (getSelectedChipText(chipGroupStudyPlace) == null) {
            Toast.makeText(requireContext(), "Выбери, где обучался", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (getSelectedChipText(chipGroupStudyEnvironment) == null) {
            Toast.makeText(requireContext(), "Выбери фон во время обучения", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean validateStepTwo() {
        if (getSelectedChipText(chipGroupDifficulty) == null) {
            Toast.makeText(requireContext(), "Выбери сложность", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (needReview == null) {
            Toast.makeText(requireContext(), "Выбери, нужно ли повторить тему", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (getSelectedChipText(chipGroupFatigue) == null) {
            Toast.makeText(requireContext(), "Выбери уровень усталости", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (getSelectedChipText(chipGroupUnderstanding) == null) {
            Toast.makeText(requireContext(), "Выбери, понял ли ты материал", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (studiedSeconds <= 0) {
            Toast.makeText(requireContext(), "Ошибка: время занятия равно 0", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveResult() {
        int productivity = (int) ratingProductivity.getRating();

        String studyPlace = getSelectedChipText(chipGroupStudyPlace);
        String studyEnvironment = getSelectedChipText(chipGroupStudyEnvironment);
        List<String> helpfulFactors = getCheckedChipTexts(chipGroupHelpfulFactors);
        List<String> disturbingFactors = getCheckedChipTexts(chipGroupDisturbingFactors);
        String difficulty = getSelectedChipText(chipGroupDifficulty);
        String fatigueLevel = getSelectedChipText(chipGroupFatigue);
        String understanding = getSelectedChipText(chipGroupUnderstanding);

        int fatigue = mapFatigueToNumber(fatigueLevel);
        int understandingLevel = mapUnderstandingToNumber(understanding);

        String notes = buildSessionReviewNotes();

        if (sessionLocalId <= 0) {
            Toast.makeText(requireContext(), "Ошибка: sessionLocalId = " + sessionLocalId, Toast.LENGTH_SHORT).show();
            return;
        }

        if (subjectId == 0) {
            Toast.makeText(requireContext(), "Ошибка: subjectId = " + subjectId, Toast.LENGTH_SHORT).show();
            return;
        }

        if (studiedSeconds <= 0) {
            Toast.makeText(requireContext(), "Ошибка: время занятия равно 0", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSaveSessionReview.setEnabled(false);
        buttonSaveSessionReview.setText("Сохранение...");

        studyViewModel.finishSession(
                sessionLocalId,
                System.currentTimeMillis(),
                studiedSeconds,
                plannedSeconds,
                studyPlace,
                studyEnvironment,
                joinList(helpfulFactors),
                joinList(disturbingFactors),
                productivity,
                fatigue,
                notes,
                difficulty,
                needReview,
                fatigueLevel,
                understandingLevel
        );

        Toast.makeText(
                requireContext(),
                "Сессия сохранена на телефоне",
                Toast.LENGTH_SHORT
        ).show();

        ((MainActivity) requireActivity()).returnToSubjectDetail(
                subjectId,
                subjectName
        );
    }

    private ArrayList<MicroCheckpointRequestDto> mapMicroCheckpointsToRequestDto() {
        ArrayList<MicroCheckpointRequestDto> requestList = new ArrayList<>();

        if (microCheckpoints == null) {
            return requestList;
        }

        for (MicroCheckpoint checkpoint : microCheckpoints) {
            if (checkpoint == null) {
                continue;
            }

            MicroCheckpointRequestDto dto = new MicroCheckpointRequestDto(
                    checkpoint.getDistractionCountRange(),
                    checkpoint.getMood(),
                    checkpoint.getBreakReason(),
                    checkpoint.getConcentrationLevel(),
                    checkpoint.getCreatedAtMillis()
            );

            requestList.add(dto);
        }

        return requestList;
    }

    private String buildSessionReviewNotes() {
        String studyPlace = getSelectedChipText(chipGroupStudyPlace);
        String studyEnvironment = getSelectedChipText(chipGroupStudyEnvironment);
        List<String> helpfulFactors = getCheckedChipTexts(chipGroupHelpfulFactors);
        List<String> disturbingFactors = getCheckedChipTexts(chipGroupDisturbingFactors);
        String difficulty = getSelectedChipText(chipGroupDifficulty);
        String fatigueLevel = getSelectedChipText(chipGroupFatigue);
        String understanding = getSelectedChipText(chipGroupUnderstanding);

        String needReviewText = needReview != null && needReview ? "Да" : "Нет";

        return "Session Review\n"
                + "Где обучался: " + safeText(studyPlace) + "\n"
                + "Фон: " + safeText(studyEnvironment) + "\n"
                + "Что помогло: " + joinList(helpfulFactors) + "\n"
                + "Что помешало: " + joinList(disturbingFactors) + "\n"
                + "Сложность: " + safeText(difficulty) + "\n"
                + "Нужно повторить: " + needReviewText + "\n"
                + "Усталость: " + safeText(fatigueLevel) + "\n"
                + "Понимание: " + safeText(understanding);
    }

    private int mapFatigueToNumber(String fatigueText) {
        if (fatigueText == null) {
            return 3;
        }

        switch (fatigueText) {
            case "Не устал":
                return 1;
            case "Средне":
                return 3;
            case "Устал":
                return 5;
            default:
                return 3;
        }
    }

    private int mapUnderstandingToNumber(String understandingText) {
        if (understandingText == null) {
            return 3;
        }

        switch (understandingText) {
            case "Понял":
            case "Хорошо понял":
                return 5;

            case "Частично":
            case "Понял частично":
                return 3;

            case "Не понял":
            case "Плохо понял":
                return 1;

            default:
                return 3;
        }
    }

    private String getSelectedChipText(ChipGroup chipGroup) {
        if (chipGroup == null) {
            return null;
        }

        int checkedChipId = chipGroup.getCheckedChipId();

        if (checkedChipId == View.NO_ID) {
            return null;
        }

        Chip chip = chipGroup.findViewById(checkedChipId);

        if (chip == null) {
            return null;
        }

        return cleanChipText(chip.getText().toString());
    }

    private List<String> getCheckedChipTexts(ChipGroup chipGroup) {
        List<String> result = new ArrayList<>();

        if (chipGroup == null) {
            return result;
        }

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);

            if (child instanceof Chip) {
                Chip chip = (Chip) child;

                if (chip.isChecked()) {
                    result.add(cleanChipText(chip.getText().toString()));
                }
            }
        }

        return result;
    }

    private String cleanChipText(String text) {
        if (text == null) {
            return null;
        }

        return text
                .replace("🏠", "")
                .replace("☕", "")
                .replace("📚", "")
                .replace("🏫", "")
                .replace("🎵", "")
                .replace("🎬", "")
                .replace("🔇", "")
                .replace("🔊", "")
                .replace("📝", "")
                .replace("💻", "")
                .replace("⏰", "")
                .replace("✅", "")
                .replace("📱", "")
                .replace("😴", "")
                .replace("🧩", "")
                .replace("😐", "")
                .replace("🙂", "")
                .replace("😣", "")
                .replace("🔁", "")
                .replace("😊", "")
                .replace("🟡", "")
                .replace("❌", "")
                .replace("😵", "")
                .replace("😌", "")
                .trim();
    }

    private void checkChipIfExists(ChipGroup chipGroup, int chipId) {
        if (chipGroup == null) {
            return;
        }

        Chip chip = chipGroup.findViewById(chipId);

        if (chip != null) {
            chip.setChecked(true);
        }
    }

    private String joinList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "Не выбрано";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            builder.append(list.get(i));

            if (i < list.size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    private String safeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Не выбрано";
        }

        return text;
    }
}