package com.parttimes.backend.worker.controllers;

import com.parttimes.backend.employer.models.JobPosting;
import com.parttimes.backend.worker.models.SavedJob;
import com.parttimes.backend.worker.services.SavedJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/saved-jobs")
@CrossOrigin(origins = "*")
public class SavedJobController {

    @Autowired
    private SavedJobService savedJobService;

    @PostMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> saveJob(
            @PathVariable String jobId,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String workerId = authentication.getName();
            SavedJob savedJob = savedJobService.saveJob(workerId, jobId, notes);
            
            response.put("success", true);
            response.put("message", "Job saved successfully");
            response.put("savedJob", savedJob);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> unsaveJob(
            @PathVariable String jobId,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String workerId = authentication.getName();
            savedJobService.unsaveJob(workerId, jobId);
            
            response.put("success", true);
            response.put("message", "Job removed from saved list");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSavedJobs(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String workerId = authentication.getName();
            List<JobPosting> savedJobs = savedJobService.getSavedJobs(workerId);
            long savedJobCount = savedJobService.getSavedJobCount(workerId);
            
            response.put("success", true);
            response.put("savedJobs", savedJobs);
            response.put("count", savedJobCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to fetch saved jobs: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getSavedJobDetails(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String workerId = authentication.getName();
            List<SavedJob> savedJobDetails = savedJobService.getSavedJobDetails(workerId);
            
            response.put("success", true);
            response.put("savedJobDetails", savedJobDetails);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to fetch saved job details: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<Map<String, Object>> isJobSaved(
            @PathVariable String jobId,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String workerId = authentication.getName();
            boolean isSaved = savedJobService.isJobSaved(workerId, jobId);
            
            response.put("success", true);
            response.put("isSaved", isSaved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to check saved status: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PutMapping("/{jobId}/notes")
    public ResponseEntity<Map<String, Object>> updateSavedJobNotes(
            @PathVariable String jobId,
            @RequestParam String notes,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String workerId = authentication.getName();
            SavedJob updatedSavedJob = savedJobService.updateSavedJobNotes(workerId, jobId, notes);
            
            response.put("success", true);
            response.put("message", "Notes updated successfully");
            response.put("savedJob", updatedSavedJob);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getSavedJobCount(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String workerId = authentication.getName();
            long count = savedJobService.getSavedJobCount(workerId);
            
            response.put("success", true);
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get saved job count: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
