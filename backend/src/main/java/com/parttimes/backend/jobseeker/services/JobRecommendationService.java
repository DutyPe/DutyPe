package com.parttimes.backend.jobseeker.services;

import com.parttimes.backend.auth.models.User;
import com.parttimes.backend.auth.repositories.UserRepository;
import com.parttimes.backend.employer.models.JobPosting;
import com.parttimes.backend.employer.repositories.JobPostingRepository;
import com.parttimes.backend.jobseeker.models.JobApplication;
import com.parttimes.backend.jobseeker.repositories.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobRecommendationService {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    public List<JobPosting> getRecommendedJobs(String userId, int limit) {
        User user = userRepository.findByEmail(userId).orElse(null);
        if (user == null) {
            return getPopularJobs(limit);
        }

        List<JobPosting> allActiveJobs = jobPostingRepository.findByIsActiveTrue();
        List<JobApplication> userApplications = jobApplicationRepository.findByJobseekerId(userId);

        // Get applied job IDs to exclude them
        Set<String> appliedJobIds = userApplications.stream()
            .map(JobApplication::getJobId)
            .collect(Collectors.toSet());

        // Filter out applied jobs
        List<JobPosting> availableJobs = allActiveJobs.stream()
            .filter(job -> !appliedJobIds.contains(job.getJobId()))
            .collect(Collectors.toList());

        // Calculate recommendation scores
        Map<JobPosting, Double> jobScores = new HashMap<>();
        for (JobPosting job : availableJobs) {
            double score = calculateRecommendationScore(user, job);
            jobScores.put(job, score);
        }

        // Sort by score and return top recommendations
        return jobScores.entrySet().stream()
            .sorted(Map.Entry.<JobPosting, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public List<JobPosting> getSimilarJobs(String jobId, int limit) {
        JobPosting targetJob = jobPostingRepository.findById(jobId).orElse(null);
        if (targetJob == null) {
            return getPopularJobs(limit);
        }

        List<JobPosting> allActiveJobs = jobPostingRepository.findByIsActiveTrue();
        
        // Filter out the target job itself
        List<JobPosting> otherJobs = allActiveJobs.stream()
            .filter(job -> !job.getJobId().equals(jobId))
            .collect(Collectors.toList());

        // Calculate similarity scores
        Map<JobPosting, Double> similarityScores = new HashMap<>();
        for (JobPosting job : otherJobs) {
            double similarity = calculateJobSimilarity(targetJob, job);
            similarityScores.put(job, similarity);
        }

        // Sort by similarity and return top similar jobs
        return similarityScores.entrySet().stream()
            .sorted(Map.Entry.<JobPosting, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public List<JobPosting> getPopularJobs(int limit) {
        return jobPostingRepository.findByIsActiveTrueOrderByPostedTimeDesc(
            org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }

    public List<JobPosting> getTrendingJobs(int limit) {
        // Jobs with high view count and recent applications
        List<JobPosting> allActiveJobs = jobPostingRepository.findByIsActiveTrue();
        
        Map<JobPosting, Double> trendingScores = new HashMap<>();
        for (JobPosting job : allActiveJobs) {
            double score = calculateTrendingScore(job);
            trendingScores.put(job, score);
        }

        return trendingScores.entrySet().stream()
            .sorted(Map.Entry.<JobPosting, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public List<JobPosting> getLocationBasedRecommendations(String userId, String location, int limit) {
        User user = userRepository.findByEmail(userId).orElse(null);
        if (user == null) {
            return getPopularJobs(limit);
        }

        List<JobPosting> locationJobs = jobPostingRepository.findByCityContainingIgnoreCase(location);
        List<JobApplication> userApplications = jobApplicationRepository.findByJobseekerId(userId);

        // Get applied job IDs to exclude them
        Set<String> appliedJobIds = userApplications.stream()
            .map(JobApplication::getJobId)
            .collect(Collectors.toSet());

        // Filter out applied jobs
        List<JobPosting> availableJobs = locationJobs.stream()
            .filter(job -> !appliedJobIds.contains(job.getJobId()))
            .collect(Collectors.toList());

        // Calculate recommendation scores
        Map<JobPosting, Double> jobScores = new HashMap<>();
        for (JobPosting job : availableJobs) {
            double score = calculateRecommendationScore(user, job);
            jobScores.put(job, score);
        }

        // Sort by score and return top recommendations
        return jobScores.entrySet().stream()
            .sorted(Map.Entry.<JobPosting, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private double calculateRecommendationScore(User user, JobPosting job) {
        double score = 0.0;

        // Skill matching (40% weight) - using benefits instead of perks
        if (user.getSkills() != null && job.getBenefits() != null) {
            double skillMatch = calculateSkillMatch(user.getSkills(), job.getBenefits());
            score += skillMatch * 0.4;
        }

        // Location preference (25% weight)
        if (user.getLocation() != null && job.getCity() != null) {
            double locationMatch = calculateLocationMatch(user.getLocation(), job.getCity());
            score += locationMatch * 0.25;
        }

        // Experience level matching (20% weight)
        if (user.getExperience() != null && job.getExperienceRequired() != null) {
            double experienceMatch = calculateExperienceMatch(user.getExperience(), job.getExperienceRequired());
            score += experienceMatch * 0.2;
        }

        // Job popularity (10% weight)
        double popularityScore = Math.min(job.getViewCount() / 100.0, 1.0);
        score += popularityScore * 0.1;

        // Recent posting bonus (5% weight)
        long daysSincePosted = java.time.temporal.ChronoUnit.DAYS.between(job.getPostedTime(), java.time.LocalDateTime.now());
        double recencyScore = Math.max(0, 1.0 - (daysSincePosted / 30.0));
        score += recencyScore * 0.05;

        return Math.min(score, 1.0);
    }

    private double calculateJobSimilarity(JobPosting job1, JobPosting job2) {
        double similarity = 0.0;

        // Category similarity (30% weight)
        if (job1.getCategory() != null && job2.getCategory() != null) {
            if (job1.getCategory().equals(job2.getCategory())) {
                similarity += 0.3;
            }
        }

        // Location similarity (25% weight)
        if (job1.getCity() != null && job2.getCity() != null) {
            if (job1.getCity().equals(job2.getCity())) {
                similarity += 0.25;
            }
        }

        // Pay type similarity (20% weight)
        if (job1.getPayType() != null && job2.getPayType() != null) {
            if (job1.getPayType().equals(job2.getPayType())) {
                similarity += 0.2;
            }
        }

        // Job type similarity (15% weight)
        if (job1.getJobType() != null && job2.getJobType() != null) {
            if (job1.getJobType().equals(job2.getJobType())) {
                similarity += 0.15;
            }
        }

        // Experience level similarity (10% weight)
        if (job1.getExperienceRequired() != null && job2.getExperienceRequired() != null) {
            if (job1.getExperienceRequired().equals(job2.getExperienceRequired())) {
                similarity += 0.1;
            }
        }

        return similarity;
    }

    private double calculateTrendingScore(JobPosting job) {
        double score = 0.0;

        // View count (40% weight)
        score += Math.min(job.getViewCount() / 100.0, 1.0) * 0.4;

        // Application count (30% weight)
        int applicationCount = jobApplicationRepository.findByJobId(job.getJobId()).size();
        score += Math.min(applicationCount / 20.0, 1.0) * 0.3;

        // Recency (30% weight)
        long daysSincePosted = java.time.temporal.ChronoUnit.DAYS.between(job.getPostedTime(), java.time.LocalDateTime.now());
        double recencyScore = Math.max(0, 1.0 - (daysSincePosted / 7.0));
        score += recencyScore * 0.3;

        return score;
    }

    private double calculateSkillMatch(List<String> userSkills, List<String> jobSkills) {
        if (userSkills == null || jobSkills == null || userSkills.isEmpty() || jobSkills.isEmpty()) {
            return 0.0;
        }

        Set<String> userSkillSet = new HashSet<>(userSkills);
        Set<String> jobSkillSet = new HashSet<>(jobSkills);

        int matchingSkills = 0;
        for (String skill : userSkillSet) {
            if (jobSkillSet.contains(skill)) {
                matchingSkills++;
            }
        }

        return (double) matchingSkills / Math.max(userSkillSet.size(), jobSkillSet.size());
    }

    private double calculateLocationMatch(String userLocation, String jobLocation) {
        if (userLocation == null || jobLocation == null) {
            return 0.0;
        }

        // Simple string matching - could be enhanced with distance calculation
        if (userLocation.toLowerCase().contains(jobLocation.toLowerCase()) ||
            jobLocation.toLowerCase().contains(userLocation.toLowerCase())) {
            return 1.0;
        }

        return 0.0;
    }

    private double calculateExperienceMatch(String userExperience, String jobExperience) {
        if (userExperience == null || jobExperience == null) {
            return 0.0;
        }

        // Simple matching - could be enhanced with experience level mapping
        if (userExperience.equalsIgnoreCase(jobExperience)) {
            return 1.0;
        }

        return 0.5; // Partial match
    }
}
