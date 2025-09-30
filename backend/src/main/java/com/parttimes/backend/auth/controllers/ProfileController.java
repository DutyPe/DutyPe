package com.parttimes.backend.auth.controllers;

import com.parttimes.backend.auth.models.User;
import com.parttimes.backend.auth.services.AuthService;
import com.parttimes.backend.common.services.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private AuthService authService;

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("success", false);
        response.put("message", "Profile endpoint - no authentication required");
        
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody User userData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create a new user with the provided data
            User user = new User();
            
            // Set basic fields from the incoming User object
            user.setFullName(userData.getFullName());
            user.setEmail(userData.getEmail());
            user.setPhoneNumber(userData.getPhoneNumber());
            user.setLocation(userData.getLocation());
            user.setDateOfBirth(userData.getDateOfBirth());
            user.setGender(userData.getGender());
            user.setBio(userData.getBio());
            
            // Set worker specific fields
            user.setSkills(userData.getSkills());
            user.setExperience(userData.getExperience());
            user.setEducation(userData.getEducation());
            user.setResumeUrl(userData.getResumeUrl());
            user.setCoverLetter(userData.getCoverLetter());
            
            // Set role (default to WORKER if not provided)
            if (userData.getRole() != null) {
                user.setRole(userData.getRole());
            } else {
                user.setRole(com.parttimes.backend.auth.models.UserRole.WORKER);
            }
            
            // Set verification status
            user.setVerified(true);
            user.setActive(true);
            
            // Save user
            User savedUser = authService.saveUser(user);
            
            // Create response
            Map<String, Object> data = new HashMap<>();
            data.put("user", createUserResponse(savedUser));
            
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update profile: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Upload file without authentication
            String fileUrl = fileUploadService.uploadFile(file, "profile-images");
            
            response.put("success", true);
            response.put("message", "Image uploaded successfully");
            response.put("fileUrl", fileUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/image")
    public ResponseEntity<Map<String, Object>> deleteProfileImage() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("success", true);
        response.put("message", "Profile image deletion - no authentication required");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/completion")
    public ResponseEntity<Map<String, Object>> getProfileCompletion() {
        Map<String, Object> response = new HashMap<>();
        
        // Return a default completion percentage without authentication
        Map<String, Object> data = new HashMap<>();
        data.put("completionPercentage", 0);
        data.put("missingFields", new String[]{"Profile not created yet"});
        
        response.put("success", true);
        response.put("message", "Profile completion - no authentication required");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }

    private int calculateProfileCompletion(Map<String, Object> user) {
        int totalFields = 10;
        int completedFields = 0;
        
        if (user.get("fullName") != null && !user.get("fullName").toString().isEmpty()) completedFields++;
        if (user.get("email") != null && !user.get("email").toString().isEmpty()) completedFields++;
        if (user.get("phoneNumber") != null && !user.get("phoneNumber").toString().isEmpty()) completedFields++;
        if (user.get("profileImageUrl") != null && !user.get("profileImageUrl").toString().isEmpty()) completedFields++;
        if (user.get("bio") != null && !user.get("bio").toString().isEmpty()) completedFields++;
        if (user.get("location") != null && !user.get("location").toString().isEmpty()) completedFields++;
        if (user.get("dateOfBirth") != null && !user.get("dateOfBirth").toString().isEmpty()) completedFields++;
        if (user.get("gender") != null && !user.get("gender").toString().isEmpty()) completedFields++;
        if (user.get("skills") != null && !user.get("skills").toString().isEmpty()) completedFields++;
        if (user.get("experience") != null && !user.get("experience").toString().isEmpty()) completedFields++;
        
        return (completedFields * 100) / totalFields;
    }

    private String[] getMissingFields(Map<String, Object> user) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        
        if (user.get("fullName") == null || user.get("fullName").toString().isEmpty()) missing.add("Full Name");
        if (user.get("phoneNumber") == null || user.get("phoneNumber").toString().isEmpty()) missing.add("Phone Number");
        if (user.get("profileImageUrl") == null || user.get("profileImageUrl").toString().isEmpty()) missing.add("Profile Image");
        if (user.get("bio") == null || user.get("bio").toString().isEmpty()) missing.add("Bio");
        if (user.get("location") == null || user.get("location").toString().isEmpty()) missing.add("Location");
        if (user.get("dateOfBirth") == null || user.get("dateOfBirth").toString().isEmpty()) missing.add("Date of Birth");
        if (user.get("gender") == null || user.get("gender").toString().isEmpty()) missing.add("Gender");
        if (user.get("skills") == null || user.get("skills").toString().isEmpty()) missing.add("Skills");
        if (user.get("experience") == null || user.get("experience").toString().isEmpty()) missing.add("Experience");
        
        return missing.toArray(new String[0]);
    }
    
    // Helper methods for type-safe extraction from Map<String, Object>
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getStringListValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return new ArrayList<>();
    }
    
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("email", user.getEmail());
        userResponse.put("fullName", user.getFullName());
        userResponse.put("phoneNumber", user.getPhoneNumber());
        userResponse.put("role", user.getRole());
        userResponse.put("isVerified", user.isVerified());
        userResponse.put("isActive", user.isActive());
        userResponse.put("profileImageUrl", user.getProfileImageUrl());
        userResponse.put("bio", user.getBio());
        userResponse.put("location", user.getLocation());
        userResponse.put("dateOfBirth", user.getDateOfBirth());
        userResponse.put("gender", user.getGender());
        userResponse.put("skills", user.getSkills());
        userResponse.put("experience", user.getExperience());
        userResponse.put("education", user.getEducation());
        userResponse.put("resumeUrl", user.getResumeUrl());
        userResponse.put("coverLetter", user.getCoverLetter());
        userResponse.put("companyName", user.getCompanyName());
        userResponse.put("companyDescription", user.getCompanyDescription());
        userResponse.put("companyWebsite", user.getCompanyWebsite());
        userResponse.put("companyLogoUrl", user.getCompanyLogoUrl());
        userResponse.put("industry", user.getIndustry());
        userResponse.put("companySize", user.getCompanySize());
        // Convert LocalDateTime to String for JSON serialization
        userResponse.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        userResponse.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
        
        return userResponse;
    }
}
