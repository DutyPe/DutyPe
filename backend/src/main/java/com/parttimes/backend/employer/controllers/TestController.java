package com.parttimes.backend.employer.controllers;

import com.parttimes.backend.employer.models.JobPosting;
import com.parttimes.backend.employer.repositories.JobPostingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @Autowired
    private JobPostingRepository jobRepo;

    @GetMapping("/connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Try a simple count operation
            long count = jobRepo.count();
            
            response.put("success", true);
            response.put("message", "Database connection successful");
            response.put("totalJobs", count);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Database connection failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/simple-job")
    public ResponseEntity<Map<String, Object>> createSimpleJob() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create a simple job for testing
            JobPosting job = new JobPosting();
            job.setTitle("Test Job");
            job.setDescription("This is a test job");
            job.setEmployerId("test-employer-123");
            job.setPayAmount("100");
            job.setPayType("HOURLY");
            job.setLocation("Test Location");
            job.setContactNumber("1234567890");
            job.setCategory("Test");
            
            JobPosting savedJob = jobRepo.save(job);
            
            response.put("success", true);
            response.put("message", "Test job created successfully");
            response.put("jobId", savedJob.getJobId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create test job: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
