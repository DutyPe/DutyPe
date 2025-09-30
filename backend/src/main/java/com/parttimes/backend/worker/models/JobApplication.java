package com.parttimes.backend.worker.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Document(collection = "jobApplications")
public class JobApplication {
    
    @Id
    private String applicationId = UUID.randomUUID().toString();
    
    private String jobId;
    private String workerId;
    private String workerEmail;
    private String workerName;
    private String workerPhone;
    
    // Application status
    private ApplicationStatus status = ApplicationStatus.PENDING;
    
    // Personal information
    private String fullName;
    private String email;
    private String phoneNumber;
    private String dateOfBirth;
    private String gender;
    private String location;
    
    // Experience and skills
    private String experience;
    private List<String> skills;
    private String education;
    private String resumeUrl;
    private String coverLetter;
    
    // Additional documents
    private List<String> documentUrls;
    
    // Application metadata
    private LocalDateTime appliedAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private String notes; // Notes from employer
    private String rejectionReason;
    
    // Interview details
    private LocalDateTime interviewScheduledAt;
    private String interviewLocation;
    private String interviewNotes;
    
    public JobApplication() {}
    
    public JobApplication(String jobId, String workerId, String workerEmail) {
        this.jobId = jobId;
        this.workerId = workerId;
        this.workerEmail = workerEmail;
    }
    
    // Getters and Setters
    public String getApplicationId() {
        return applicationId;
    }
    
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    
    public String getWorkerId() {
        return workerId;
    }
    
    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
    
    public String getWorkerEmail() {
        return workerEmail;
    }
    
    public void setWorkerEmail(String workerEmail) {
        this.workerEmail = workerEmail;
    }
    
    public String getWorkerName() {
        return workerName;
    }
    
    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }
    
    public String getWorkerPhone() {
        return workerPhone;
    }
    
    public void setWorkerPhone(String workerPhone) {
        this.workerPhone = workerPhone;
    }
    
    public ApplicationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getExperience() {
        return experience;
    }
    
    public void setExperience(String experience) {
        this.experience = experience;
    }
    
    public List<String> getSkills() {
        return skills;
    }
    
    public void setSkills(List<String> skills) {
        this.skills = skills;
    }
    
    public String getEducation() {
        return education;
    }
    
    public void setEducation(String education) {
        this.education = education;
    }
    
    public String getResumeUrl() {
        return resumeUrl;
    }
    
    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }
    
    public String getCoverLetter() {
        return coverLetter;
    }
    
    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }
    
    public List<String> getDocumentUrls() {
        return documentUrls;
    }
    
    public void setDocumentUrls(List<String> documentUrls) {
        this.documentUrls = documentUrls;
    }
    
    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }
    
    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public LocalDateTime getInterviewScheduledAt() {
        return interviewScheduledAt;
    }
    
    public void setInterviewScheduledAt(LocalDateTime interviewScheduledAt) {
        this.interviewScheduledAt = interviewScheduledAt;
    }
    
    public String getInterviewLocation() {
        return interviewLocation;
    }
    
    public void setInterviewLocation(String interviewLocation) {
        this.interviewLocation = interviewLocation;
    }
    
    public String getInterviewNotes() {
        return interviewNotes;
    }
    
    public void setInterviewNotes(String interviewNotes) {
        this.interviewNotes = interviewNotes;
    }
}
