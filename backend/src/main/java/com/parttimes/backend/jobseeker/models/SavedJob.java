package com.parttimes.backend.jobseeker.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "savedJobs")
public class SavedJob {

    @Id
    private String id = UUID.randomUUID().toString();
    private String jobseekerId;
    private String jobId;
    private LocalDateTime savedAt = LocalDateTime.now();
    private String notes; // Optional notes from user
    private boolean isActive = true;

    public SavedJob() {}

    public SavedJob(String jobseekerId, String jobId) {
        this.jobseekerId = jobseekerId;
        this.jobId = jobId;
    }

    public SavedJob(String jobseekerId, String jobId, String notes) {
        this.jobseekerId = jobseekerId;
        this.jobId = jobId;
        this.notes = notes;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobseekerId() {
        return jobseekerId;
    }

    public void setJobseekerId(String jobseekerId) {
        this.jobseekerId = jobseekerId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
