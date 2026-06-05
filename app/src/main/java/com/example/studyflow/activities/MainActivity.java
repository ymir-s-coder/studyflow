package com.example.studyflow.activities;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.studyflow.R;
import com.example.studyflow.fragments.CreateSubjectFragment;
import com.example.studyflow.fragments.ResultFragment;
import com.example.studyflow.fragments.StatisticsFragment;
import com.example.studyflow.fragments.StudyFragment;
import com.example.studyflow.fragments.SubjectDetailFragment;
import com.example.studyflow.fragments.SubjectsFragment;
import com.example.studyflow.models.MicroCheckpoint;
import com.example.studyflow.repository.SubjectRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar topAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        topAppBar = findViewById(R.id.topAppBar);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        setupBottomNavigation();

        SubjectRepository subjectRepository = new SubjectRepository(this);
        subjectRepository.syncSubjectsFromServer();

        if (savedInstanceState == null) {
            loadFragment(new SubjectsFragment(), false);
        }

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragmentContainer);

            updateBottomNavigationVisibility(currentFragment);
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_subjects) {
                loadFragment(new SubjectsFragment(), false);
                return true;
            } else if (id == R.id.nav_statistics) {
                loadFragment(new StatisticsFragment(), false);
                return true;
            }

            return false;
        });
    }

    private void updateBottomNavigationVisibility(Fragment fragment) {
        boolean shouldShowBottomNavigation =
                fragment instanceof SubjectsFragment ||
                        fragment instanceof StatisticsFragment;

        setBottomNavigationVisible(shouldShowBottomNavigation);
    }

    private void setBottomNavigationVisible(boolean visible) {
        if (bottomNavigationView == null) {
            return;
        }

        bottomNavigationView.setVisibility(visible ? View.VISIBLE : View.GONE);

        View fragmentContainer = findViewById(R.id.fragmentContainer);

        if (fragmentContainer == null) {
            return;
        }

        if (visible) {
            bottomNavigationView.post(() -> {
                int bottomPadding = bottomNavigationView.getHeight();
                fragmentContainer.setPadding(0, 0, 0, bottomPadding);
            });
        } else {
            fragmentContainer.setPadding(0, 0, 0, 0);
        }
    }

    private void loadFragment(@NonNull Fragment fragment, boolean addToBackStack) {
        updateBottomNavigationVisibility(fragment);

        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    public void openSubjectDetail(
            Long subjectId,
            String subjectName,
            String subjectDescription,
            Integer plannedTotalMinutes,
            Integer goalMinutesPerSession,
            String learningType,
            String studyFrequency,
            String notes
    ) {
        if (subjectId == null || subjectId == 0L) {
            return;
        }

        SubjectDetailFragment fragment = new SubjectDetailFragment();

        Bundle bundle = new Bundle();
        bundle.putLong("subjectId", subjectId);
        bundle.putString("subjectName", subjectName);
        bundle.putString("subjectDescription", subjectDescription);

        if (plannedTotalMinutes != null) {
            bundle.putInt("plannedTotalMinutes", plannedTotalMinutes);
        }

        if (goalMinutesPerSession != null) {
            bundle.putInt("goalMinutesPerSession", goalMinutesPerSession);
        }

        bundle.putString("learningType", learningType);
        bundle.putString("studyFrequency", studyFrequency);
        bundle.putString("notes", notes);

        fragment.setArguments(bundle);

        loadFragment(fragment, true);
    }

    public void openStudyFragment(Long subjectId, String subjectName) {
        if (subjectId == null || subjectId == 0L) {
            return;
        }

        StudyFragment fragment = new StudyFragment();

        Bundle bundle = new Bundle();
        bundle.putLong("subjectId", subjectId);
        bundle.putString("subjectName", subjectName);

        fragment.setArguments(bundle);

        loadFragment(fragment, true);
    }

    public void openStudyFragment(Long subjectId, String subjectName, long plannedSeconds) {
        if (subjectId == null || subjectId == 0L) {
            return;
        }

        StudyFragment fragment = new StudyFragment();

        Bundle bundle = new Bundle();
        bundle.putLong("subjectId", subjectId);
        bundle.putString("subjectName", subjectName);
        bundle.putLong("plannedSeconds", plannedSeconds);

        fragment.setArguments(bundle);

        loadFragment(fragment, true);
    }

    public void openCreateSubjectFragment() {
        loadFragment(new CreateSubjectFragment(), true);
    }

    public void openResultFragment(
            Long subjectId,
            String subjectName,
            long studiedSeconds,
            long plannedSeconds,
            ArrayList<MicroCheckpoint> microCheckpoints,
            long sessionLocalId
    ) {
        if (subjectId == null || subjectId == 0L) {
            return;
        }

        ResultFragment fragment = new ResultFragment();

        Bundle bundle = new Bundle();
        bundle.putLong("subjectId", subjectId);
        bundle.putString("subjectName", subjectName);
        bundle.putLong("studiedSeconds", studiedSeconds);
        bundle.putLong("plannedSeconds", plannedSeconds);
        bundle.putSerializable("microCheckpoints", microCheckpoints);
        bundle.putLong("sessionLocalId", sessionLocalId);

        fragment.setArguments(bundle);

        loadFragment(fragment, true);
    }

    public void returnToSubjects() {
        loadFragment(new SubjectsFragment(), false);
        bottomNavigationView.setSelectedItemId(R.id.nav_subjects);
    }

    public void returnToSubjectDetail(Long subjectId, String subjectName) {
        if (subjectId == null || subjectId == 0L) {
            returnToSubjects();
            return;
        }

        SubjectDetailFragment fragment = new SubjectDetailFragment();

        Bundle bundle = new Bundle();
        bundle.putLong("subjectId", subjectId);
        bundle.putString("subjectName", subjectName);

        fragment.setArguments(bundle);

        loadFragment(fragment, false);
    }
}