package com.example.studyflow.network;

import com.example.studyflow.network.requests.CreateSubjectRequestDto;
import com.example.studyflow.network.requests.FinishSessionRequestDto;
import com.example.studyflow.network.requests.LoginRequestDto;
import com.example.studyflow.network.requests.RegisterRequestDto;
import com.example.studyflow.network.responses.LoginResponseDto;
import com.example.studyflow.network.responses.SessionResponseDto;
import com.example.studyflow.network.responses.SubjectResponseDto;
import com.example.studyflow.network.requests.SubjectNoteRequestDto;


import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {


    @GET("api/subjects")
    Call<List<SubjectResponseDto>> getSubjects();

    @POST("/api/auth/login")
    Call<LoginResponseDto> login(@Body LoginRequestDto request);

    @POST("/api/auth/register")
    Call<LoginResponseDto> register(@Body RegisterRequestDto request);

    @POST("/api/subjects")
    Call<SubjectResponseDto> createSubject(@Body CreateSubjectRequestDto request);

    @GET("/api/sessions/subject/{subjectId}")
    Call<List<SessionResponseDto>> getSessionsBySubject(@Path("subjectId") long subjectId);

    @POST("/api/sessions")
    Call<SessionResponseDto> finishSession(@Body FinishSessionRequestDto request);

    @DELETE("/api/subjects/{subjectId}")
    Call<Void> deleteSubject(@Path("subjectId") Long subjectId);

    @PATCH("api/subjects/{subjectId}/notes")
    Call<SubjectResponseDto> updateSubjectNotes(
            @Path("subjectId") long subjectId,
            @Body SubjectNoteRequestDto requestDto
    );
}