package com.parttimes.backend.jobseeker.repositories;

import com.parttimes.backend.jobseeker.models.SavedJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedJobRepository extends MongoRepository<SavedJob, String> {
    
    List<SavedJob> findByJobseekerIdAndIsActiveTrueOrderBySavedAtDesc(String jobseekerId);
    
    Optional<SavedJob> findByJobseekerIdAndJobId(String jobseekerId, String jobId);
    
    boolean existsByJobseekerIdAndJobId(String jobseekerId, String jobId);
    
    long countByJobseekerIdAndIsActiveTrue(String jobseekerId);
    
    List<SavedJob> findByJobIdAndIsActiveTrue(String jobId);
}
