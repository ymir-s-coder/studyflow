package com.example.studyflow.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studyflow.R;
import com.example.studyflow.network.responses.MicroCheckpointResponseDto;
import com.example.studyflow.network.responses.SessionResponseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private final List<SessionResponseDto> sessionList;

    public SessionAdapter(List<SessionResponseDto> sessionList) {
        this.sessionList = sessionList != null ? sessionList : new ArrayList<>();
    }

    public void setSessions(List<SessionResponseDto> newSessions) {
        sessionList.clear();

        if (newSessions != null) {
            sessionList.addAll(newSessions);
        }

        notifyDataSetChanged();
    }

    public void addSessionToTop(SessionResponseDto session) {
        if (session == null) {
            return;
        }

        sessionList.add(0, session);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);

        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        SessionResponseDto session = sessionList.get(position);

        long durationSeconds = session.getDurationSeconds() != null
                ? session.getDurationSeconds()
                : 0L;

        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;

        String durationText = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
        );

        holder.textSessionDuration.setText("Duration: " + durationText);

        Integer productivity = session.getProductivity();
        Integer fatigue = session.getFatigue();

        holder.textSessionProductivity.setText(
                "Productivity: " + (productivity != null ? productivity : 0)
        );

        holder.textSessionFatigue.setText(
                "Fatigue: " + (fatigue != null ? fatigue : 0)
        );

        String notes = session.getNotes();

        if (notes != null && !notes.trim().isEmpty()) {
            holder.textSessionNotes.setText("Notes: " + notes);
            holder.textSessionNotes.setVisibility(View.VISIBLE);
        } else {
            holder.textSessionNotes.setVisibility(View.GONE);
        }

        bindMicroCheckpoints(holder, session);

        String createdAt = session.getCreatedAt();

        if (createdAt != null && !createdAt.trim().isEmpty()) {
            holder.textSessionDate.setText(createdAt);
            holder.textSessionDate.setVisibility(View.VISIBLE);
        } else {
            holder.textSessionDate.setVisibility(View.GONE);
        }
    }

    private void bindMicroCheckpoints(SessionViewHolder holder, SessionResponseDto session) {
        List<MicroCheckpointResponseDto> checkpoints = session.getMicroCheckpoints();

        if (checkpoints == null || checkpoints.isEmpty()) {
            holder.textPauseSurvey.setVisibility(View.GONE);
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Pause survey:");

        for (int i = 0; i < checkpoints.size(); i++) {
            MicroCheckpointResponseDto checkpoint = checkpoints.get(i);

            builder.append("\n\nPause ").append(i + 1);

            String distractionCountRange = checkpoint.getDistractionCountRange();
            String mood = checkpoint.getMood();
            String breakReason = checkpoint.getBreakReason();
            Integer concentrationLevel = checkpoint.getConcentrationLevel();

            if (distractionCountRange != null && !distractionCountRange.trim().isEmpty()) {
                builder.append("\nDistractions: ").append(distractionCountRange);
            }

            if (mood != null && !mood.trim().isEmpty()) {
                builder.append("\nMood: ").append(mood);
            }

            if (breakReason != null && !breakReason.trim().isEmpty()) {
                builder.append("\nReason: ").append(breakReason);
            }

            if (concentrationLevel != null) {
                builder.append("\nConcentration: ").append(concentrationLevel);
            }
        }

        holder.textPauseSurvey.setText(builder.toString());
        holder.textPauseSurvey.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {

        TextView textSessionDuration;
        TextView textSessionProductivity;
        TextView textSessionFatigue;
        TextView textSessionNotes;
        TextView textPauseSurvey;
        TextView textSessionDate;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);

            textSessionDuration = itemView.findViewById(R.id.textSessionDuration);
            textSessionProductivity = itemView.findViewById(R.id.textSessionProductivity);
            textSessionFatigue = itemView.findViewById(R.id.textSessionFatigue);
            textSessionNotes = itemView.findViewById(R.id.textSessionNotes);
            textPauseSurvey = itemView.findViewById(R.id.textPauseSurvey);
            textSessionDate = itemView.findViewById(R.id.textSessionDate);
        }
    }
}