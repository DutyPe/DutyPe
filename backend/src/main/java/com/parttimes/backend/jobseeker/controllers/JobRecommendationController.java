package com.parttimes.backend.jobseeker.controllers;

import com.parttimes.backend.employer.models.JobPosting;
import com.parttimes.backend.jobseeker.services.JobRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class JobRecommendationController {

    @Autowired
    private JobRecommendationService jobRecommendationService;

    @GetMapping("/personalized")
    public ResponseEntity<Map<String, Object>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("success", true);
        response.put("message", "Personalized recommendations endpoint - no authentication required");
        response.put("recommendations", List.of());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/similar/{jobId}")
    public ResponseEntity<Map<String, Object>> getSimilarJobs(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "5") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<JobPosting> similarJobs = jobRecommendationService.getSimilarJobs(jobId, limit);
            
            response.put("success", true);
            response.put("similarJobs", similarJobs);
            response.put("count", similarJobs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get similar jobs: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularJobs(
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<JobPosting> popularJobs = jobRecommendationService.getPopularJobs(limit);
            
            response.put("success", true);
            response.put("popularJobs", popularJobs);
            response.put("count", popularJobs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get popular jobs: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/trending")
    public ResponseEntity<Map<String, Object>> getTrendingJobs(
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<JobPosting> trendingJobs = jobRecommendationService.getTrendingJobs(limit);
            
            response.put("success", true);
            response.put("trendingJobs", trendingJobs);
            response.put("count", trendingJobs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get trending jobs: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/location-based")
    public ResponseEntity<Map<String, Object>> getLocationBasedRecommendations(
            @RequestParam String location,
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("success", true);
        response.put("message", "Location-based recommendations endpoint - no authentication required");
        response.put("recommendations", List.of());
        
        return ResponseEntity.ok(response);
    }
}
