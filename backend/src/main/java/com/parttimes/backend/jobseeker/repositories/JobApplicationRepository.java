package com.parttimes.backend.jobseeker.repositories;

import com.parttimes.backend.jobseeker.models.ApplicationStatus;
import com.parttimes.backend.jobseeker.models.JobApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {
    
    // Find applications by jobseeker
    List<JobApplication> findByJobseekerId(String jobseekerId);
    
    // Find applications by job
    List<JobApplication> findByJobId(String jobId);
    
    // Find applications by status
    List<JobApplication> findByStatus(ApplicationStatus status);
    
    // Find applications by jobseeker and status
    List<JobApplication> findByJobseekerIdAndStatus(String jobseekerId, ApplicationStatus status);
    
    // Find applications by job and status
    List<JobApplication> findByJobIdAndStatus(String jobId, ApplicationStatus status);
    
    // Check if jobseeker has already applied to a job
    boolean existsByJobIdAndJobseekerId(String jobId, String jobseekerId);
    
    // Find applications by jobseeker email
    List<JobApplication> findByJobseekerEmail(String jobseekerEmail);
    
    // Find applications by jobseeker email and status
    List<JobApplication> findByJobseekerEmailAndStatus(String jobseekerEmail, ApplicationStatus status);
    
    // Find applications by employer ID (through job postings)
    List<JobApplication> findByJobIdIn(List<String> jobIds);
}
