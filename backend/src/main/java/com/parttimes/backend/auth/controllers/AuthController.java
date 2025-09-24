package com.parttimes.backend.auth.controllers;

import com.parttimes.backend.auth.models.User;
import com.parttimes.backend.auth.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/verify-phone")
    public ResponseEntity<Map<String, Object>> verifyPhone(@RequestBody Map<String, String> request) {
        String firebaseToken = request.get("firebaseToken");
        String phoneNumber = request.get("phoneNumber");
        
        Map<String, Object> response = authService.verifyPhoneAndCreateUser(firebaseToken, phoneNumber);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/create-user")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String fullName = request.get("fullName");
        String email = request.get("email");
        
        Map<String, Object> response = authService.createUserWithoutAuth(phoneNumber, fullName, email);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestParam(required = false) String userId) {
        if (userId == null || userId.isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "User ID required"
            );
            return ResponseEntity.status(400).body(errorResponse);
        }
        
        Map<String, Object> response = authService.getProfile(userId);
        
        if ((Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(404).body(response);
        }
    }
    
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        if (userId == null || userId.isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "User ID required"
            );
            return ResponseEntity.status(400).body(errorResponse);
        }
        
        // Create User object from request data
        User user = new User();
        if (request.get("fullName") != null) user.setFullName((String) request.get("fullName"));
        if (request.get("phoneNumber") != null) user.setPhoneNumber((String) request.get("phoneNumber"));
        if (request.get("email") != null) user.setEmail((String) request.get("email"));
        if (request.get("bio") != null) user.setBio((String) request.get("bio"));
        if (request.get("location") != null) user.setLocation((String) request.get("location"));
        if (request.get("dateOfBirth") != null) user.setDateOfBirth((String) request.get("dateOfBirth"));
        if (request.get("gender") != null) user.setGender((String) request.get("gender"));
        if (request.get("experience") != null) user.setExperience((String) request.get("experience"));
        if (request.get("education") != null) user.setEducation((String) request.get("education"));
        if (request.get("coverLetter") != null) user.setCoverLetter((String) request.get("coverLetter"));
        if (request.get("companyName") != null) user.setCompanyName((String) request.get("companyName"));
        if (request.get("companyDescription") != null) user.setCompanyDescription((String) request.get("companyDescription"));
        if (request.get("companyWebsite") != null) user.setCompanyWebsite((String) request.get("companyWebsite"));
        if (request.get("industry") != null) user.setIndustry((String) request.get("industry"));
        if (request.get("companySize") != null) user.setCompanySize((String) request.get("companySize"));
        
        Map<String, Object> response = authService.updateProfile(userId, user);
        
        if ((Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(404).body(response);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        // JWT is stateless, so logout is handled on client side
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
