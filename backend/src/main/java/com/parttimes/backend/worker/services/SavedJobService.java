package com.parttimes.backend.worker.services;

import com.parttimes.backend.employer.models.JobPosting;
import com.parttimes.backend.employer.repositories.JobPostingRepository;
import com.parttimes.backend.worker.models.SavedJob;
import com.parttimes.backend.worker.repositories.SavedJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SavedJobService {

    @Autowired
    private SavedJobRepository savedJobRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    public SavedJob saveJob(String workerId, String jobId, String notes) {
        // Check if job exists
        if (!jobPostingRepository.existsById(jobId)) {
            throw new RuntimeException("Job not found");
        }

        // Check if already saved
        Optional<SavedJob> existingSavedJob = savedJobRepository.findByWorkerIdAndJobId(workerId, jobId);
        if (existingSavedJob.isPresent()) {
            SavedJob savedJob = existingSavedJob.get();
            if (savedJob.isActive()) {
                throw new RuntimeException("Job already saved");
            } else {
                // Reactivate the saved job
                savedJob.setActive(true);
                savedJob.setNotes(notes);
                return savedJobRepository.save(savedJob);
            }
        }

        // Create new saved job
        SavedJob savedJob = new SavedJob(workerId, jobId, notes);
        return savedJobRepository.save(savedJob);
    }

    public void unsaveJob(String workerId, String jobId) {
        Optional<SavedJob> savedJob = savedJobRepository.findByWorkerIdAndJobId(workerId, jobId);
        if (savedJob.isPresent()) {
            SavedJob job = savedJob.get();
            job.setActive(false);
            savedJobRepository.save(job);
        } else {
            throw new RuntimeException("Saved job not found");
        }
    }

    public List<JobPosting> getSavedJobs(String workerId) {
        List<SavedJob> savedJobs = savedJobRepository.findByWorkerIdAndIsActiveTrueOrderBySavedAtDesc(workerId);
        
        return savedJobs.stream()
            .map(savedJob -> jobPostingRepository.findById(savedJob.getJobId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(JobPosting::isActive) // Only return active jobs
            .collect(Collectors.toList());
    }

    public List<SavedJob> getSavedJobDetails(String workerId) {
        return savedJobRepository.findByWorkerIdAndIsActiveTrueOrderBySavedAtDesc(workerId);
    }

    public boolean isJobSaved(String workerId, String jobId) {
        return savedJobRepository.existsByWorkerIdAndJobId(workerId, jobId);
    }

    public long getSavedJobCount(String workerId) {
        return savedJobRepository.countByWorkerIdAndIsActiveTrue(workerId);
    }

    public SavedJob updateSavedJobNotes(String workerId, String jobId, String notes) {
        Optional<SavedJob> savedJob = savedJobRepository.findByWorkerIdAndJobId(workerId, jobId);
        if (savedJob.isPresent()) {
            SavedJob job = savedJob.get();
            job.setNotes(notes);
            return savedJobRepository.save(job);
        } else {
            throw new RuntimeException("Saved job not found");
        }
    }

    public List<SavedJob> getSavedJobsByJobId(String jobId) {
        return savedJobRepository.findByJobIdAndIsActiveTrue(jobId);
    }
}
