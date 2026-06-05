package com.example.studyflow.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studyflow.R;
import com.example.studyflow.listeners.OnSubjectClickListener;
import com.example.studyflow.network.responses.SubjectResponseDto;

import java.util.ArrayList;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.ViewHolder> {

    private final List<SubjectResponseDto> subjects = new ArrayList<>();
    private final OnSubjectClickListener listener;

    public SubjectAdapter(OnSubjectClickListener listener) {
        this.listener = listener;
    }

    public void setSubjects(List<SubjectResponseDto> subjects) {
        this.subjects.clear();

        if (subjects != null) {
            this.subjects.addAll(subjects);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubjectAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);
        return new SubjectAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectAdapter.ViewHolder holder, int position) {
        SubjectResponseDto subject = subjects.get(position);

        holder.textName.setText(subject.getTitle() != null ? subject.getTitle() : "Untitled subject");

        String goalText = buildGoalText(subject);
        holder.textGoal.setText(goalText);

        int progress = calculateProgressPercent(subject);
        holder.textProgress.setText("Progress: " + progress + "%");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSubjectClick(subject);
            }
        });
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    private int calculateProgressPercent(SubjectResponseDto subject) {
        if (subject == null) {
            return 0;
        }

        Integer plannedTotalMinutes = subject.getPlannedTotalMinutes();
        Long studiedSeconds = subject.getStudiedSeconds();

        if (plannedTotalMinutes == null || plannedTotalMinutes <= 0) {
            return 0;
        }

        if (studiedSeconds == null || studiedSeconds <= 0) {
            return 0;
        }

        long plannedSeconds = plannedTotalMinutes * 60L;

        int progress = (int) Math.round((studiedSeconds * 100.0) / plannedSeconds);

        return Math.min(progress, 100);
    }

    private String buildGoalText(SubjectResponseDto subject) {
        if (subject == null) {
            return "";
        }

        Integer plannedTotalMinutes = subject.getPlannedTotalMinutes();
        Integer goalMinutesPerSession = subject.getGoalMinutesPerSession();

        if (plannedTotalMinutes == null || plannedTotalMinutes <= 0) {
            return subject.getDescription() != null ? subject.getDescription() : "";
        }

        String totalPlan = formatMinutes(plannedTotalMinutes);

        if (goalMinutesPerSession == null || goalMinutesPerSession <= 0) {
            return "Total plan: " + totalPlan;
        }

        return "Total plan: " + totalPlan
                + " / Session: " + formatMinutes(goalMinutesPerSession);
    }

    private String formatMinutes(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return "not set";
        }

        int hours = minutes / 60;
        int leftMinutes = minutes % 60;

        if (hours > 0 && leftMinutes > 0) {
            return hours + " h " + leftMinutes + " min";
        }

        if (hours > 0) {
            return hours + " h";
        }

        return minutes + " min";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textName;
        TextView textGoal;
        TextView textProgress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textName = itemView.findViewById(R.id.textSubjectName);
            textGoal = itemView.findViewById(R.id.textSubjectGoal);
            textProgress = itemView.findViewById(R.id.textSubjectProgress);
        }
    }
}