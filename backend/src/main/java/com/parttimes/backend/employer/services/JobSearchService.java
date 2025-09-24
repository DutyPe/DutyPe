package com.parttimes.backend.employer.services;

import com.parttimes.backend.employer.models.JobPosting;
import com.parttimes.backend.employer.repositories.JobPostingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JobSearchService {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    public Page<JobPosting> advancedSearch(Map<String, Object> searchCriteria, Pageable pageable) {
        String query = (String) searchCriteria.get("query");
        String location = (String) searchCriteria.get("location");
        String category = (String) searchCriteria.get("category");
        String payType = (String) searchCriteria.get("payType");
        String jobType = (String) searchCriteria.get("jobType");
        String urgency = (String) searchCriteria.get("urgency");
        String experienceLevel = (String) searchCriteria.get("experienceLevel");
        String gender = (String) searchCriteria.get("gender");
        String ageRange = (String) searchCriteria.get("ageRange");
        String companySize = (String) searchCriteria.get("companySize");
        String industry = (String) searchCriteria.get("industry");
        Boolean isVerified = (Boolean) searchCriteria.get("isVerified");
        Boolean isRemote = (Boolean) searchCriteria.get("isRemote");
        Double minSalary = (Double) searchCriteria.get("minSalary");
        Double maxSalary = (Double) searchCriteria.get("maxSalary");

        // Build dynamic query based on criteria
        if (query != null && !query.isEmpty()) {
            return jobPostingRepository.searchJobs(query, pageable);
        }

        // Use repository methods for filtering
        if (location != null && !location.isEmpty()) {
            return jobPostingRepository.findByCityContainingIgnoreCase(location, pageable);
        }

        if (category != null && !category.isEmpty()) {
            return jobPostingRepository.findByCategory(category, pageable);
        }

        if (payType != null && !payType.isEmpty()) {
            return jobPostingRepository.findByPayType(payType, pageable);
        }

        if (jobType != null && !jobType.isEmpty()) {
            return jobPostingRepository.findByJobType(jobType, pageable);
        }

        if (urgency != null && !urgency.isEmpty()) {
            return jobPostingRepository.findByUrgency(urgency, pageable);
        }

        if (isVerified != null && isVerified) {
            return jobPostingRepository.findByIsVerifiedTrue(pageable);
        }

        // Default: return all active jobs
        return jobPostingRepository.findByIsActiveTrue(pageable);
    }

    public List<JobPosting> getRecommendedJobs(String userId, int limit) {
        // This would implement recommendation logic based on user preferences
        // For now, return recent active jobs
        return jobPostingRepository.findByIsActiveTrueOrderByPostedTimeDesc(
            org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }

    public List<String> getSearchSuggestions(String query) {
        // Return job title suggestions based on query
        return jobPostingRepository.findDistinctTitlesContainingIgnoreCase(query);
    }

    public Map<String, Long> getSearchFilters() {
        // Return counts for filter options
        return Map.of(
            "totalJobs", jobPostingRepository.countByIsActiveTrue(),
            "verifiedJobs", jobPostingRepository.countByIsVerifiedTrue(),
            "urgentJobs", jobPostingRepository.countByUrgency("URGENT")
        );
    }
}
