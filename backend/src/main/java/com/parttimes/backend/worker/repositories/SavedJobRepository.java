package com.parttimes.backend.worker.repositories;

import com.parttimes.backend.worker.models.SavedJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedJobRepository extends MongoRepository<SavedJob, String> {
    
    List<SavedJob> findByWorkerIdAndIsActiveTrueOrderBySavedAtDesc(String workerId);
    
    Optional<SavedJob> findByWorkerIdAndJobId(String workerId, String jobId);
    
    boolean existsByWorkerIdAndJobId(String workerId, String jobId);
    
    long countByWorkerIdAndIsActiveTrue(String workerId);
    
    List<SavedJob> findByJobIdAndIsActiveTrue(String jobId);
}
