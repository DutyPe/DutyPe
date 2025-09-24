package com.parttimes.backend.jobseeker.services;

import com.parttimes.backend.employer.models.JobPosting;
import com.parttimes.backend.employer.repositories.JobPostingRepository;
import com.parttimes.backend.jobseeker.models.SavedJob;
import com.parttimes.backend.jobseeker.repositories.SavedJobRepository;
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

    public SavedJob saveJob(String jobseekerId, String jobId, String notes) {
        // Check if job exists
        if (!jobPostingRepository.existsById(jobId)) {
            throw new RuntimeException("Job not found");
        }

        // Check if already saved
        Optional<SavedJob> existingSavedJob = savedJobRepository.findByJobseekerIdAndJobId(jobseekerId, jobId);
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
        SavedJob savedJob = new SavedJob(jobseekerId, jobId, notes);
        return savedJobRepository.save(savedJob);
    }

    public void unsaveJob(String jobseekerId, String jobId) {
        Optional<SavedJob> savedJob = savedJobRepository.findByJobseekerIdAndJobId(jobseekerId, jobId);
        if (savedJob.isPresent()) {
            SavedJob job = savedJob.get();
            job.setActive(false);
            savedJobRepository.save(job);
        } else {
            throw new RuntimeException("Saved job not found");
        }
    }

    public List<JobPosting> getSavedJobs(String jobseekerId) {
        List<SavedJob> savedJobs = savedJobRepository.findByJobseekerIdAndIsActiveTrueOrderBySavedAtDesc(jobseekerId);
        
        return savedJobs.stream()
            .map(savedJob -> jobPostingRepository.findById(savedJob.getJobId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(JobPosting::isActive) // Only return active jobs
            .collect(Collectors.toList());
    }

    public List<SavedJob> getSavedJobDetails(String jobseekerId) {
        return savedJobRepository.findByJobseekerIdAndIsActiveTrueOrderBySavedAtDesc(jobseekerId);
    }

    public boolean isJobSaved(String jobseekerId, String jobId) {
        return savedJobRepository.existsByJobseekerIdAndJobId(jobseekerId, jobId);
    }

    public long getSavedJobCount(String jobseekerId) {
        return savedJobRepository.countByJobseekerIdAndIsActiveTrue(jobseekerId);
    }

    public SavedJob updateSavedJobNotes(String jobseekerId, String jobId, String notes) {
        Optional<SavedJob> savedJob = savedJobRepository.findByJobseekerIdAndJobId(jobseekerId, jobId);
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
