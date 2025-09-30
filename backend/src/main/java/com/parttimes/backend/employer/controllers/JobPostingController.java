package com.parttimes.backend.employer.controllers;

import com.parttimes.backend.employer.models.JobPosting;
import com.parttimes.backend.employer.repositories.JobPostingRepository;
import com.parttimes.backend.employer.dto.JobListingDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobPostingController {

    @Autowired
    private JobPostingRepository jobRepo;

    // Get all active jobs
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<JobPosting> jobsPage = jobRepo.findByIsActiveTrue(pageable);
        
        // Convert JobPosting objects to frontend-compatible format
        List<Map<String, Object>> convertedJobs = jobsPage.getContent().stream()
                .map(this::convertJobPostingToJobListing)
                .collect(java.util.stream.Collectors.toList());
        
        // Create response in ApiResponse format
        Map<String, Object> data = new HashMap<>();
        data.put("jobs", convertedJobs);
        data.put("currentPage", jobsPage.getNumber());
        data.put("totalItems", jobsPage.getTotalElements());
        data.put("totalPages", jobsPage.getTotalPages());
        data.put("hasNext", jobsPage.hasNext());
        data.put("hasPrevious", jobsPage.hasPrevious());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Jobs retrieved successfully");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }

    // Get job by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getJob(@PathVariable String id) {
        Optional<JobPosting> jobOpt = jobRepo.findById(id);
        
        if (jobOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Job not found");
            return ResponseEntity.notFound().build();
        }
        
        JobPosting job = jobOpt.get();
        
        // Increment view count
        job.setViewCount(job.getViewCount() + 1);
        jobRepo.save(job);
        
        // Create response in ApiResponse format
        Map<String, Object> data = new HashMap<>();
        data.put("job", convertJobPostingToJobListing(job));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Job retrieved successfully");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }

    // Create new job from JobListing (for frontend compatibility)
    @PostMapping
    public ResponseEntity<Map<String, Object>> createJob(@RequestBody Map<String, Object> jobData) {
        
        // Convert JobListing data to JobPosting
        JobPosting job = convertJobListingToJobPosting(jobData);
        
        // Validate required employer ID
        if (job.getEmployerId() == null || job.getEmployerId().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Employer ID is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        JobPosting savedJob = jobRepo.save(job);
        
        // Convert JobPosting to frontend-compatible format
        Map<String, Object> convertedJobData = convertJobPostingToJobListing(savedJob);
        
        // Create response in ApiResponse format
        Map<String, Object> data = new HashMap<>();
        data.put("job", convertedJobData);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Job created successfully");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    // Create new job from JobPosting (for direct backend usage)
    @PostMapping("/direct")
    public ResponseEntity<Map<String, Object>> createJobDirect(@RequestBody JobPosting job) {
        
        // Validate required employer ID
        if (job.getEmployerId() == null || job.getEmployerId().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Employer ID is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        JobPosting savedJob = jobRepo.save(job);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Job created successfully");
        response.put("job", savedJob);
        
        return ResponseEntity.ok(response);
    }

    // Update job
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateJob(
            @PathVariable String id, 
            @RequestBody JobPosting jobUpdate) {
        
        Optional<JobPosting> jobOpt = jobRepo.findById(id);
        
        if (jobOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Job not found");
            return ResponseEntity.notFound().build();
        }
        
        JobPosting job = jobOpt.get();
        
        // Update fields
        if (jobUpdate.getTitle() != null) job.setTitle(jobUpdate.getTitle());
        if (jobUpdate.getPayAmount() != null) job.setPayAmount(jobUpdate.getPayAmount());
        if (jobUpdate.getPayType() != null) job.setPayType(jobUpdate.getPayType());
        // PayPeriod field removed as per user request
        if (jobUpdate.getLocation() != null) job.setLocation(jobUpdate.getLocation());
        if (jobUpdate.getArea() != null) job.setArea(jobUpdate.getArea());
        if (jobUpdate.getCity() != null) job.setCity(jobUpdate.getCity());
        if (jobUpdate.getDescription() != null) job.setDescription(jobUpdate.getDescription());
        if (jobUpdate.getContactNumber() != null) job.setContactNumber(jobUpdate.getContactNumber());
        if (jobUpdate.getCategory() != null) job.setCategory(jobUpdate.getCategory());
        if (jobUpdate.getShiftTiming() != null) job.setShiftTiming(jobUpdate.getShiftTiming());
        if (jobUpdate.getUrgency() != null) job.setUrgency(jobUpdate.getUrgency());
        // Perks field removed as per user request
        if (jobUpdate.getRequirements() != null) job.setRequirements(jobUpdate.getRequirements());
        if (jobUpdate.getBenefits() != null) job.setBenefits(jobUpdate.getBenefits());
        if (jobUpdate.getVacancies() > 0) job.setVacancies(jobUpdate.getVacancies());
        if (jobUpdate.getWorkingHours() != null) job.setWorkingHours(jobUpdate.getWorkingHours());
        if (jobUpdate.getExperienceRequired() != null) job.setExperienceRequired(jobUpdate.getExperienceRequired());
        if (jobUpdate.getAgeRange() != null) job.setAgeRange(jobUpdate.getAgeRange());
        if (jobUpdate.getGender() != null) job.setGender(jobUpdate.getGender());
        if (jobUpdate.getJobType() != null) job.setJobType(jobUpdate.getJobType());
        if (jobUpdate.getApplicationDeadline() != null) job.setApplicationDeadline(jobUpdate.getApplicationDeadline());
        if (jobUpdate.getCompanySize() != null) job.setCompanySize(jobUpdate.getCompanySize());
        if (jobUpdate.getIndustry() != null) job.setIndustry(jobUpdate.getIndustry());
        if (jobUpdate.getImageUrl() != null) job.setImageUrl(jobUpdate.getImageUrl());
        
        JobPosting updatedJob = jobRepo.save(job);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Job updated successfully");
        response.put("job", updatedJob);
        
        return ResponseEntity.ok(response);
    }

    // Delete job
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable String id) {
        
        Optional<JobPosting> jobOpt = jobRepo.findById(id);
        
        if (jobOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Job not found");
            return ResponseEntity.notFound().build();
        }
        
        JobPosting job = jobOpt.get();
        
        jobRepo.delete(job);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Job deleted successfully");
        
        return ResponseEntity.ok(response);
    }

    // Search jobs
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchJobs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<JobPosting> jobs = jobRepo.searchJobs(query);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("jobs", jobs);
        response.put("totalResults", jobs.size());
        
        return ResponseEntity.ok(response);
    }

    // Filter jobs
    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> filterJobs(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String payType,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String urgency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<JobPosting> jobs;
        
        if (city != null && payType != null && jobType != null) {
            jobs = jobRepo.findJobsByFilters(city, payType, jobType);
        } else if (city != null) {
            jobs = jobRepo.findByCityContainingIgnoreCase(city);
        } else if (payType != null) {
            jobs = jobRepo.findByPayType(payType);
        } else if (jobType != null) {
            jobs = jobRepo.findByJobType(jobType);
        } else if (category != null) {
            jobs = jobRepo.findByCategory(category);
        } else if (urgency != null) {
            jobs = jobRepo.findByUrgency(urgency);
        } else {
            jobs = jobRepo.findByIsActiveTrue();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("jobs", jobs);
        response.put("totalResults", jobs.size());
        
        return ResponseEntity.ok(response);
    }

    // Get jobs by employer
    @GetMapping("/employer/{employerId}")
    public ResponseEntity<Map<String, Object>> getJobsByEmployer(@PathVariable String employerId) {
        List<JobPosting> jobs = jobRepo.findByEmployerId(employerId);
        
        // Convert JobPosting objects to frontend-compatible format
        List<Map<String, Object>> convertedJobs = jobs.stream()
                .map(this::convertJobPostingToJobListing)
                .collect(java.util.stream.Collectors.toList());
        
        // Create response in ApiResponse format
        Map<String, Object> data = new HashMap<>();
        data.put("jobs", convertedJobs);
        data.put("totalJobs", jobs.size());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Employer jobs retrieved successfully");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }

    // Get verified jobs
    @GetMapping("/verified")
    public ResponseEntity<Map<String, Object>> getVerifiedJobs() {
        List<JobPosting> jobs = jobRepo.findByIsVerifiedTrue();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("jobs", jobs);
        response.put("totalJobs", jobs.size());
        
        return ResponseEntity.ok(response);
    }
    
    // Helper method to convert JobListingDto to JobPosting
    private JobPosting convertJobListingDtoToJobPosting(JobListingDto dto) {
        JobPosting job = new JobPosting();
        
        // Basic fields
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setLocation(dto.getLocation());
        job.setCategory(dto.getCategory());
        job.setJobType(dto.getJobType() != null ? dto.getJobType() : "Part-time");
        
        // Company/Employer information
        String companyName = dto.getCompany() != null ? dto.getCompany() : dto.getCompanyName();
        job.setEmployerName(companyName != null ? companyName : "");
        
        // Contact information
        String contactNumber = dto.getContactNumber() != null ? dto.getContactNumber() : 
                              dto.getContactInfo() != null ? dto.getContactInfo() : 
                              dto.getPhoneNumber();
        job.setContactNumber(contactNumber != null ? contactNumber : "");
        
        // Pay information
        if (dto.getSalary() != null && !dto.getSalary().isEmpty()) {
            // Parse salary string like "500/daily" or "500/hourly"
            String[] salaryParts = dto.getSalary().split("/");
            if (salaryParts.length >= 2) {
                job.setPayAmount(salaryParts[0]);
                job.setPayType(salaryParts[1].toUpperCase());
                // PayPeriod field removed as per user request
            }
        } else if (dto.getPayRate() != null) {
            job.setPayAmount(dto.getPayRate().toString());
        }
        
        // Additional fields
        job.setWorkingHours(dto.getWorkingHours());
        job.setExperienceRequired(dto.getExperienceRequired() != null ? dto.getExperienceRequired() : dto.getExperienceLevel());
        job.setAgeRange(dto.getAgeRange());
        job.setGender(dto.getGender());
        job.setApplicationDeadline(dto.getApplicationDeadline());
        job.setCompanySize(dto.getCompanySize());
        job.setIndustry(dto.getIndustry());
        job.setImageUrl(dto.getImageUrl());
        
        // Lists
        // Perks field removed as per user request
        job.setBenefits(dto.getBenefits());
        job.setRequirements(dto.getRequirements());
        
        // Numeric fields
        job.setVacancies(dto.getVacancies());
        job.setViewCount(dto.getViewCount() != null ? dto.getViewCount().intValue() : 0);
        job.setApplicationsReceived(dto.getApplicationCount() != null ? dto.getApplicationCount().intValue() : 0);
        
        // Boolean fields
        job.setActive(dto.getIsActive());
        job.setVerified(dto.getIsVerified());
        // isBookmarked and isApplied are worker-specific and handled separately
        
        // Urgency
        if (dto.getIsUrgent() != null && dto.getIsUrgent()) {
            job.setUrgency("URGENT");
        } else if (dto.getUrgency() != null) {
            job.setUrgency(dto.getUrgency().toUpperCase());
        }
        
        // Shift timing
        if (dto.getShiftTiming() != null) {
            job.setShiftTiming(dto.getShiftTiming());
        } else if (dto.getTiming() != null) {
            job.setShiftTiming(dto.getTiming());
        }
        
        // Location details
        job.setArea(dto.getArea());
        job.setCity(dto.getCity());
        
        // Set posted time
        if (dto.getPostedAt() != null) {
            job.setPostedTime(LocalDateTime.ofEpochSecond(dto.getPostedAt() / 1000, 0, java.time.ZoneOffset.UTC));
        } else {
            job.setPostedTime(LocalDateTime.now());
        }
        
        return job;
    }
    
    /**
     * Convert JobListing data (Map<String, Object>) to JobPosting entity
     */
    private JobPosting convertJobListingToJobPosting(Map<String, Object> jobData) {
        JobPosting job = new JobPosting();
        
        // Basic fields
        job.setTitle(getStringValue(jobData, "title"));
        job.setDescription(getStringValue(jobData, "description"));
        job.setLocation(getStringValue(jobData, "location"));
        job.setCategory(getStringValue(jobData, "category"));
        job.setJobType(getStringValue(jobData, "jobType"));
        
        // Company/Employer information
        String companyName = getStringValue(jobData, "company");
        if (companyName == null || companyName.isEmpty()) {
            companyName = getStringValue(jobData, "companyName");
        }
        job.setEmployerName(companyName);
        
        // Contact information
        String contactNumber = getStringValue(jobData, "contactNumber");
        if (contactNumber == null || contactNumber.isEmpty()) {
            contactNumber = getStringValue(jobData, "contactInfo");
        }
        if (contactNumber == null || contactNumber.isEmpty()) {
            contactNumber = getStringValue(jobData, "phoneNumber");
        }
        job.setContactNumber(contactNumber);
        
        // Pay information
        String wage = getStringValue(jobData, "wage");
        if (wage != null && !wage.isEmpty()) {
            String[] salaryParts = wage.split("/");
            if (salaryParts.length >= 2) {
                job.setPayAmount(salaryParts[0]);
                job.setPayType(salaryParts[1].toUpperCase());
            }
        } else {
            Double payRate = getDoubleValue(jobData, "payRate");
            if (payRate != null) {
                job.setPayAmount(payRate.toString());
            }
        }
        
        // Additional fields
        job.setWorkingHours(getStringValue(jobData, "workingHours"));
        job.setExperienceRequired(getStringValue(jobData, "experienceLevel"));
        job.setAgeRange(getStringValue(jobData, "ageRange"));
        job.setGender(getStringValue(jobData, "gender"));
        job.setApplicationDeadline(getStringValue(jobData, "applicationDeadline"));
        job.setCompanySize(getStringValue(jobData, "companySize"));
        job.setIndustry(getStringValue(jobData, "industry"));
        job.setImageUrl(getStringValue(jobData, "imageUrl"));
        
        // Lists
        job.setBenefits(getStringListValue(jobData, "benefits"));
        job.setRequirements(getStringListValue(jobData, "requirements"));
        
        // Numeric fields
        job.setVacancies(getIntValue(jobData, "vacancies"));
        job.setViewCount(getIntValue(jobData, "viewCount"));
        job.setApplicationsReceived(getIntValue(jobData, "applicationCount"));
        
        // Boolean fields
        job.setActive(getBooleanValue(jobData, "isActive", true));
        job.setVerified(getBooleanValue(jobData, "isVerified", false));
        // Note: isBookmarked and isApplied are worker-specific and handled separately
        
        // Urgency
        if (getBooleanValue(jobData, "isUrgent", false)) {
            job.setUrgency("URGENT");
        } else {
            job.setUrgency("NORMAL");
        }
        
        // Shift timing
        String shiftTiming = getStringValue(jobData, "shiftTiming");
        if (shiftTiming == null || shiftTiming.isEmpty()) {
            shiftTiming = getStringValue(jobData, "timing");
        }
        job.setShiftTiming(shiftTiming);
        
        // Location details
        job.setArea(getStringValue(jobData, "area"));
        job.setCity(getStringValue(jobData, "city"));
        
        // Set posted time
        Long postedAt = getLongValue(jobData, "postedAt");
        if (postedAt != null && postedAt > 0) {
            job.setPostedTime(LocalDateTime.ofEpochSecond(postedAt / 1000, 0, java.time.ZoneOffset.UTC));
        } else {
            job.setPostedTime(LocalDateTime.now());
        }
        
        // Set employer ID - this is required
        String employerId = getStringValue(jobData, "employerId");
        if (employerId == null || employerId.isEmpty()) {
            // Generate a default employer ID if not provided
            employerId = "emp_" + System.currentTimeMillis();
        }
        job.setEmployerId(employerId);
        
        return job;
    }
    
    // Helper methods for type-safe extraction from Map<String, Object>
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getStringListValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        } else if (value instanceof String) {
            String stringValue = (String) value;
            if (stringValue.trim().isEmpty()) {
                return new ArrayList<>();
            }
            // Split by comma and trim each element
            List<String> result = new ArrayList<>();
            for (String item : stringValue.split(",")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }
    
    /**
     * Convert JobPosting to frontend-compatible JobListing format
     */
    private Map<String, Object> convertJobPostingToJobListing(JobPosting job) {
        Map<String, Object> jobListing = new HashMap<>();
        
        // Basic fields
        jobListing.put("id", job.getJobId());
        jobListing.put("jobId", job.getJobId());
        jobListing.put("employerId", job.getEmployerId());
        jobListing.put("title", job.getTitle());
        jobListing.put("companyName", job.getEmployerName());
        jobListing.put("company", job.getEmployerName());
        jobListing.put("location", job.getLocation());
        jobListing.put("specificLocation", job.getLocation());
        jobListing.put("locationNearby", job.getLocation());
        jobListing.put("area", job.getArea());
        jobListing.put("city", job.getCity());
        
        // Pay information
        jobListing.put("payAmount", job.getPayAmount());
        jobListing.put("payType", job.getPayType());
        jobListing.put("payRate", 0.0); // Default value
        
        // Timing
        jobListing.put("timing", job.getShiftTiming());
        jobListing.put("shiftTiming", job.getShiftTiming());
        
        // Description and details
        jobListing.put("description", job.getDescription());
        jobListing.put("requirements", job.getRequirements() != null ? job.getRequirements() : new ArrayList<>());
        jobListing.put("benefits", job.getBenefits() != null ? job.getBenefits() : new ArrayList<>());
        jobListing.put("skills", new ArrayList<>()); // Default empty list
        
        // Job details
        jobListing.put("vacancies", job.getVacancies());
        jobListing.put("isActive", job.isActive());
        jobListing.put("isTrending", false);
        jobListing.put("isRemote", false);
        jobListing.put("isVerified", job.isVerified());
        
        // Dates - convert LocalDateTime to String
        if (job.getPostedTime() != null) {
            jobListing.put("postedAt", job.getPostedTime().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli());
            jobListing.put("postedTime", job.getPostedTime().toString());
            jobListing.put("postedDate", job.getPostedTime().toString());
        } else {
            jobListing.put("postedAt", System.currentTimeMillis());
            jobListing.put("postedTime", "");
            jobListing.put("postedDate", "");
        }
        
        // Contact information
        jobListing.put("phoneNumber", job.getContactNumber());
        jobListing.put("contactNumber", job.getContactNumber());
        jobListing.put("contactInfo", job.getContactNumber());
        
        // Additional fields
        jobListing.put("category", job.getCategory());
        jobListing.put("jobType", job.getJobType());
        jobListing.put("workingHours", job.getWorkingHours());
        jobListing.put("experienceRequired", job.getExperienceRequired());
        jobListing.put("ageRange", job.getAgeRange());
        jobListing.put("gender", job.getGender());
        jobListing.put("applicationDeadline", job.getApplicationDeadline());
        jobListing.put("companySize", job.getCompanySize());
        jobListing.put("industry", job.getIndustry());
        jobListing.put("imageUrl", job.getImageUrl());
        
        // Urgency
        jobListing.put("urgency", job.getUrgency());
        
        // Counts
        jobListing.put("viewCount", job.getViewCount());
        jobListing.put("applicationCount", job.getApplicationsReceived());
        
        return jobListing;
    }
}
