package com.parttimes.backend.worker.repositories;

import com.parttimes.backend.worker.models.ApplicationStatus;
import com.parttimes.backend.worker.models.JobApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {
    
    // Find applications by worker
    List<JobApplication> findByWorkerId(String workerId);
    
    // Find applications by job
    List<JobApplication> findByJobId(String jobId);
    
    // Find applications by status
    List<JobApplication> findByStatus(ApplicationStatus status);
    
    // Find applications by worker and status
    List<JobApplication> findByWorkerIdAndStatus(String workerId, ApplicationStatus status);
    
    // Find applications by job and status
    List<JobApplication> findByJobIdAndStatus(String jobId, ApplicationStatus status);
    
    // Check if worker has already applied to a job
    boolean existsByJobIdAndWorkerId(String jobId, String workerId);
    
    // Find applications by worker email
    List<JobApplication> findByWorkerEmail(String workerEmail);
    
    // Find applications by worker email and status
    List<JobApplication> findByWorkerEmailAndStatus(String workerEmail, ApplicationStatus status);
    
    // Find applications by employer ID (through job postings)
    List<JobApplication> findByJobIdIn(List<String> jobIds);
}
