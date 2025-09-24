package com.parttimes.backend.common.controllers;

import com.parttimes.backend.common.services.LocationService;
import com.parttimes.backend.employer.models.JobPosting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/location")
@CrossOrigin(origins = "*")
public class LocationController {

    @Autowired
    private LocationService locationService;

    @GetMapping("/jobs/nearby")
    public ResponseEntity<Map<String, Object>> getJobsNearLocation(
            @RequestParam String city,
            @RequestParam(defaultValue = "25") double radiusKm) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<JobPosting> jobs = locationService.getJobsNearLocation(city, radiusKm);
            
            response.put("success", true);
            response.put("jobs", jobs);
            response.put("count", jobs.size());
            response.put("city", city);
            response.put("radiusKm", radiusKm);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/jobs/distance")
    public ResponseEntity<Map<String, Object>> getJobsByDistance(
            @RequestParam String userCity,
            @RequestParam double userLat,
            @RequestParam double userLon,
            @RequestParam(defaultValue = "50") double maxDistanceKm) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<JobPosting> jobs = locationService.getJobsByDistance(userCity, userLat, userLon, maxDistanceKm);
            Map<String, Double> distances = locationService.calculateDistancesToJobs(userCity, jobs);
            
            response.put("success", true);
            response.put("jobs", jobs);
            response.put("distances", distances);
            response.put("count", jobs.size());
            response.put("maxDistanceKm", maxDistanceKm);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/stats/{city}")
    public ResponseEntity<Map<String, Object>> getLocationBasedJobStats(@PathVariable String city) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> stats = locationService.getLocationBasedJobStats(city);
            
            response.put("success", true);
            response.put("stats", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/nearby-cities")
    public ResponseEntity<Map<String, Object>> getNearbyCities(
            @RequestParam String city,
            @RequestParam(defaultValue = "100") double radiusKm) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> nearbyCities = locationService.getNearbyCities(city, radiusKm);
            
            response.put("success", true);
            response.put("nearbyCities", nearbyCities);
            response.put("count", nearbyCities.size());
            response.put("radiusKm", radiusKm);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/jobs/multiple-cities")
    public ResponseEntity<Map<String, Object>> getJobsInMultipleCities(
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<String> cities = (List<String>) request.get("cities");
            double radiusKm = ((Number) request.getOrDefault("radiusKm", 25)).doubleValue();
            
            List<JobPosting> jobs = locationService.getJobsInMultipleCities(cities, radiusKm);
            
            response.put("success", true);
            response.put("jobs", jobs);
            response.put("count", jobs.size());
            response.put("cities", cities);
            response.put("radiusKm", radiusKm);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/popular-cities")
    public ResponseEntity<Map<String, Object>> getPopularCities() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> popularCities = locationService.getPopularCities();
            
            response.put("success", true);
            response.put("popularCities", popularCities);
            response.put("count", popularCities.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/insights")
    public ResponseEntity<Map<String, Object>> getLocationInsights() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> insights = locationService.getLocationInsights();
            
            response.put("success", true);
            response.put("insights", insights);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
