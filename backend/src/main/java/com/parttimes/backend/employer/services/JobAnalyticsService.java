package com.parttimes.backend.employer.services;

import com.parttimes.backend.employer.models.JobPosting;
import com.parttimes.backend.employer.repositories.JobPostingRepository;
import com.parttimes.backend.jobseeker.models.JobApplication;
import com.parttimes.backend.jobseeker.repositories.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobAnalyticsService {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    public Map<String, Object> getJobAnalytics(String employerId) {
        Map<String, Object> analytics = new HashMap<>();
        
        // Get all jobs by employer
        List<JobPosting> jobs = jobPostingRepository.findByEmployerId(employerId);
        
        // Basic statistics
        analytics.put("totalJobs", jobs.size());
        analytics.put("activeJobs", jobs.stream().filter(JobPosting::isActive).count());
        analytics.put("totalViews", jobs.stream().mapToInt(JobPosting::getViewCount).sum());
        analytics.put("totalApplications", getTotalApplications(employerId));
        
        // Job performance metrics
        analytics.put("averageViewsPerJob", calculateAverageViews(jobs));
        analytics.put("averageApplicationsPerJob", calculateAverageApplications(jobs));
        analytics.put("conversionRate", calculateConversionRate(jobs, employerId));
        
        // Recent activity
        analytics.put("jobsPostedThisWeek", getJobsPostedThisWeek(jobs));
        analytics.put("applicationsThisWeek", getApplicationsThisWeek(employerId));
        
        // Top performing jobs
        analytics.put("topPerformingJobs", getTopPerformingJobs(jobs, 5));
        
        // Application status breakdown
        analytics.put("applicationStatusBreakdown", getApplicationStatusBreakdown(employerId));
        
        return analytics;
    }

    public Map<String, Object> getJobPerformance(String jobId) {
        Map<String, Object> performance = new HashMap<>();
        
        JobPosting job = jobPostingRepository.findById(jobId).orElse(null);
        if (job == null) {
            return performance;
        }
        
        List<JobApplication> applications = jobApplicationRepository.findByJobId(jobId);
        
        performance.put("jobId", jobId);
        performance.put("title", job.getTitle());
        performance.put("views", job.getViewCount());
        performance.put("applications", applications.size());
        performance.put("conversionRate", calculateJobConversionRate(job, applications));
        performance.put("daysActive", ChronoUnit.DAYS.between(job.getPostedTime(), LocalDateTime.now()));
        performance.put("applicationStatusBreakdown", getJobApplicationStatusBreakdown(applications));
        performance.put("averageApplicationTime", calculateAverageApplicationTime(applications));
        
        return performance;
    }

    public Map<String, Object> getEmployerDashboard(String employerId) {
        Map<String, Object> dashboard = new HashMap<>();
        
        List<JobPosting> jobs = jobPostingRepository.findByEmployerId(employerId);
        List<String> jobIds = jobs.stream().map(JobPosting::getJobId).toList();
        List<JobApplication> allApplications = jobApplicationRepository.findByJobIdIn(jobIds);
        
        // Quick stats
        dashboard.put("totalJobs", jobs.size());
        dashboard.put("activeJobs", jobs.stream().filter(JobPosting::isActive).count());
        dashboard.put("totalApplications", allApplications.size());
        dashboard.put("pendingApplications", allApplications.stream()
            .filter(app -> "PENDING".equals(app.getStatus().name())).count());
        
        // Recent activity
        dashboard.put("recentApplications", getRecentApplications(employerId, 10));
        dashboard.put("recentJobs", getRecentJobs(jobs, 5));
        
        // Performance trends
        dashboard.put("weeklyTrends", getWeeklyTrends(employerId));
        
        return dashboard;
    }

    private int getTotalApplications(String employerId) {
        List<JobPosting> jobs = jobPostingRepository.findByEmployerId(employerId);
        List<String> jobIds = jobs.stream().map(JobPosting::getJobId).toList();
        return jobApplicationRepository.findByJobIdIn(jobIds).size();
    }

    private double calculateAverageViews(List<JobPosting> jobs) {
        if (jobs.isEmpty()) return 0;
        return jobs.stream().mapToInt(JobPosting::getViewCount).average().orElse(0);
    }

    private double calculateAverageApplications(List<JobPosting> jobs) {
        if (jobs.isEmpty()) return 0;
        return jobs.stream()
            .mapToInt(job -> jobApplicationRepository.findByJobId(job.getJobId()).size())
            .average()
            .orElse(0);
    }

    private double calculateConversionRate(List<JobPosting> jobs, String employerId) {
        int totalViews = jobs.stream().mapToInt(JobPosting::getViewCount).sum();
        int totalApplications = getTotalApplications(employerId);
        
        if (totalViews == 0) return 0;
        return (double) totalApplications / totalViews * 100;
    }

    private double calculateJobConversionRate(JobPosting job, List<JobApplication> applications) {
        if (job.getViewCount() == 0) return 0;
        return (double) applications.size() / job.getViewCount() * 100;
    }

    private long getJobsPostedThisWeek(List<JobPosting> jobs) {
        LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        return jobs.stream()
            .filter(job -> job.getPostedTime().isAfter(weekAgo))
            .count();
    }

    private long getApplicationsThisWeek(String employerId) {
        LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        List<JobPosting> jobs = jobPostingRepository.findByEmployerId(employerId);
        List<String> jobIds = jobs.stream().map(JobPosting::getJobId).toList();
        return jobApplicationRepository.findByJobIdIn(jobIds).stream()
            .filter(app -> app.getAppliedAt() != null && app.getAppliedAt().isAfter(weekAgo))
            .count();
    }

    private List<Map<String, Object>> getTopPerformingJobs(List<JobPosting> jobs, int limit) {
        return jobs.stream()
            .map(job -> {
                Map<String, Object> jobData = new HashMap<>();
                jobData.put("jobId", job.getJobId());
                jobData.put("title", job.getTitle());
                jobData.put("views", job.getViewCount());
                jobData.put("applications", jobApplicationRepository.findByJobId(job.getJobId()).size());
                return jobData;
            })
            .sorted((a, b) -> Integer.compare(
                (Integer) b.get("views") + (Integer) b.get("applications"),
                (Integer) a.get("views") + (Integer) a.get("applications")
            ))
            .limit(limit)
            .toList();
    }

    private Map<String, Long> getApplicationStatusBreakdown(String employerId) {
        List<JobPosting> jobs = jobPostingRepository.findByEmployerId(employerId);
        List<String> jobIds = jobs.stream().map(JobPosting::getJobId).toList();
        List<JobApplication> applications = jobApplicationRepository.findByJobIdIn(jobIds);
        Map<String, Long> breakdown = new HashMap<>();
        
        applications.forEach(app -> {
            String status = app.getStatus().name();
            breakdown.put(status, breakdown.getOrDefault(status, 0L) + 1);
        });
        
        return breakdown;
    }

    private Map<String, Long> getJobApplicationStatusBreakdown(List<JobApplication> applications) {
        Map<String, Long> breakdown = new HashMap<>();
        
        applications.forEach(app -> {
            String status = app.getStatus().name();
            breakdown.put(status, breakdown.getOrDefault(status, 0L) + 1);
        });
        
        return breakdown;
    }

    private double calculateAverageApplicationTime(List<JobApplication> applications) {
        if (applications.isEmpty()) return 0;
        
        return applications.stream()
            .filter(app -> app.getAppliedAt() != null)
            .mapToLong(app -> {
                try {
                    LocalDateTime appDate = app.getAppliedAt();
                    return ChronoUnit.HOURS.between(appDate, LocalDateTime.now());
                } catch (Exception e) {
                    return 0;
                }
            })
            .average()
            .orElse(0);
    }

    private List<Map<String, Object>> getRecentApplications(String employerId, int limit) {
        List<JobPosting> jobs = jobPostingRepository.findByEmployerId(employerId);
        List<String> jobIds = jobs.stream().map(JobPosting::getJobId).toList();
        return jobApplicationRepository.findByJobIdIn(jobIds).stream()
            .sorted((a, b) -> b.getAppliedAt().compareTo(a.getAppliedAt()))
            .limit(limit)
            .map(app -> {
                Map<String, Object> appData = new HashMap<>();
                appData.put("applicationId", app.getApplicationId());
                appData.put("jobId", app.getJobId());
                appData.put("status", app.getStatus().name());
                appData.put("applicationDate", app.getAppliedAt());
                return appData;
            })
            .toList();
    }

    private List<Map<String, Object>> getRecentJobs(List<JobPosting> jobs, int limit) {
        return jobs.stream()
            .sorted((a, b) -> b.getPostedTime().compareTo(a.getPostedTime()))
            .limit(limit)
            .map(job -> {
                Map<String, Object> jobData = new HashMap<>();
                jobData.put("jobId", job.getJobId());
                jobData.put("title", job.getTitle());
                jobData.put("postedTime", job.getPostedTime());
                jobData.put("views", job.getViewCount());
                jobData.put("applications", jobApplicationRepository.findByJobId(job.getJobId()).size());
                return jobData;
            })
            .toList();
    }

    private Map<String, Object> getWeeklyTrends(String employerId) {
        Map<String, Object> trends = new HashMap<>();
        
        // This would typically involve more complex date calculations
        // For now, return basic trend data
        trends.put("jobsPosted", getJobsPostedThisWeek(jobPostingRepository.findByEmployerId(employerId)));
        trends.put("applicationsReceived", getApplicationsThisWeek(employerId));
        
        return trends;
    }
}
