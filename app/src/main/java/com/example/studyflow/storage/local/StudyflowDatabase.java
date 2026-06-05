package com.example.studyflow.storage.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.studyflow.storage.local.session.LocalMicroCheckpointEntity;
import com.example.studyflow.storage.local.session.LocalStudySessionDao;
import com.example.studyflow.storage.local.session.LocalStudySessionEntity;

@Database(
        entities = {
                SubjectEntity.class,
                LocalStudySessionEntity.class,
                LocalMicroCheckpointEntity.class
        },
        version = 5,
        exportSchema = false
)
public abstract class StudyflowDatabase extends RoomDatabase {

    private static volatile StudyflowDatabase INSTANCE;

    public abstract SubjectDao subjectDao();

    public abstract LocalStudySessionDao localStudySessionDao();

    public static StudyflowDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (StudyflowDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    StudyflowDatabase.class,
                                    "studyflow_database"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}