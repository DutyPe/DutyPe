package com.parttimes.backend.worker.controllers;

import com.parttimes.backend.worker.models.ApplicationStatus;
import com.parttimes.backend.worker.models.JobApplication;
import com.parttimes.backend.worker.repositories.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "*")
public class JobApplicationController {
    
    @Autowired
    private JobApplicationRepository applicationRepository;
    
    // Submit job application
    @PostMapping
    public ResponseEntity<Map<String, Object>> submitApplication(@RequestBody JobApplication application) {
        
        // Validate required fields
        if (application.getWorkerId() == null || application.getWorkerId().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Worker ID is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        if (application.getWorkerEmail() == null || application.getWorkerEmail().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Worker email is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        application.setAppliedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        
        JobApplication savedApplication = applicationRepository.save(application);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Application submitted successfully");
        response.put("application", savedApplication);
        
        return ResponseEntity.ok(response);
    }
    
    // Get applications by worker
    @GetMapping("/my-applications")
    public ResponseEntity<Map<String, Object>> getMyApplications() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "My applications endpoint - no authentication required");
        response.put("applications", List.of());
        
        return ResponseEntity.ok(response);
    }
    
    // Get application by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getApplication(@PathVariable String id) {
        
        Optional<JobApplication> applicationOpt = applicationRepository.findById(id);
        
        if (applicationOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Application not found");
            return ResponseEntity.notFound().build();
        }
        
        JobApplication application = applicationOpt.get();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("application", application);
        
        return ResponseEntity.ok(response);
    }
    
    // Update application status (for employers)
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateApplicationStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> statusUpdate) {
        
        Optional<JobApplication> applicationOpt = applicationRepository.findById(id);
        
        if (applicationOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Application not found");
            return ResponseEntity.notFound().build();
        }
        
        JobApplication application = applicationOpt.get();
        
        // Update status
        String newStatus = statusUpdate.get("status");
        String notes = statusUpdate.get("notes");
        String rejectionReason = statusUpdate.get("rejectionReason");
        
        try {
            application.setStatus(ApplicationStatus.valueOf(newStatus.toUpperCase()));
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid status");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        if (notes != null) application.setNotes(notes);
        if (rejectionReason != null) application.setRejectionReason(rejectionReason);
        
        application.setUpdatedAt(LocalDateTime.now());
        
        JobApplication updatedApplication = applicationRepository.save(application);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Application status updated successfully");
        response.put("application", updatedApplication);
        
        return ResponseEntity.ok(response);
    }
    
    // Get applications for a specific job (for employers)
    @GetMapping("/job/{jobId}")
    public ResponseEntity<Map<String, Object>> getApplicationsForJob(@PathVariable String jobId) {
        List<JobApplication> applications = applicationRepository.findByJobId(jobId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("applications", applications);
        response.put("totalApplications", applications.size());
        
        return ResponseEntity.ok(response);
    }
    
    // Withdraw application
    @PutMapping("/{id}/withdraw")
    public ResponseEntity<Map<String, Object>> withdrawApplication(@PathVariable String id) {
        
        Optional<JobApplication> applicationOpt = applicationRepository.findById(id);
        
        if (applicationOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Application not found");
            return ResponseEntity.notFound().build();
        }
        
        JobApplication application = applicationOpt.get();
        
        // Check if application can be withdrawn
        if (application.getStatus() == ApplicationStatus.SELECTED || 
            application.getStatus() == ApplicationStatus.REJECTED ||
            application.getStatus() == ApplicationStatus.WITHDRAWN) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Cannot withdraw application in current status");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setUpdatedAt(LocalDateTime.now());
        
        JobApplication updatedApplication = applicationRepository.save(application);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Application withdrawn successfully");
        response.put("application", updatedApplication);
        
        return ResponseEntity.ok(response);
    }
    
    // Get applications by status
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getApplicationsByStatus(@PathVariable String status) {
        
        try {
            ApplicationStatus applicationStatus = ApplicationStatus.valueOf(status.toUpperCase());
            
            List<JobApplication> applications = applicationRepository.findByStatus(applicationStatus);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", applications);
            response.put("totalApplications", applications.size());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid status");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
