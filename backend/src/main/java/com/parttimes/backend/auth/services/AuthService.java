package com.parttimes.backend.auth.services;

import com.parttimes.backend.auth.models.User;
import com.parttimes.backend.auth.models.UserRole;
import com.parttimes.backend.auth.repositories.UserRepository;
import com.parttimes.backend.auth.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    public Map<String, Object> verifyPhoneAndCreateUser(String firebaseToken, String phoneNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Verify Firebase token (in a real implementation, you would verify with Firebase Admin SDK)
            // For now, we'll assume the token is valid if it's not null
            if (firebaseToken == null || firebaseToken.isEmpty()) {
                response.put("success", false);
                response.put("message", "Invalid Firebase token");
                return response;
            }
            
            // Check if user already exists by phone number
            Optional<User> existingUser = userRepository.findByPhoneNumber(phoneNumber);
            User user;
            
            if (existingUser.isPresent()) {
                // User exists, update last login
                user = existingUser.get();
                user.setUpdatedAt(LocalDateTime.now());
                user = userRepository.save(user);
            } else {
                // Create new user
                user = new User();
                user.setPhoneNumber(phoneNumber);
                user.setEmail(""); // Empty email for phone-only auth
                user.setFullName("User"); // Default name, can be updated later
                user.setRole(UserRole.WORKER); // Default role
                user.setVerified(true); // Phone verified
                user.setActive(true);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                user = userRepository.save(user);
            }
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user);
            
            response.put("success", true);
            response.put("message", "Phone authentication successful");
            response.put("token", token);
            response.put("user", createUserResponse(user));
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Authentication failed: " + e.getMessage());
        }
        
        return response;
    }
    
    public Map<String, Object> getProfile(String userId) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found");
            return response;
        }
        
        User user = userOpt.get();
        response.put("success", true);
        response.put("user", createUserResponse(user));
        
        return response;
    }
    
    public Map<String, Object> updateProfile(String userId, User updatedUser) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found");
            return response;
        }
        
        User user = userOpt.get();
        
        // Update fields
        if (updatedUser.getFullName() != null) user.setFullName(updatedUser.getFullName());
        if (updatedUser.getPhoneNumber() != null) user.setPhoneNumber(updatedUser.getPhoneNumber());
        if (updatedUser.getBio() != null) user.setBio(updatedUser.getBio());
        if (updatedUser.getLocation() != null) user.setLocation(updatedUser.getLocation());
        if (updatedUser.getDateOfBirth() != null) user.setDateOfBirth(updatedUser.getDateOfBirth());
        if (updatedUser.getGender() != null) user.setGender(updatedUser.getGender());
        if (updatedUser.getSkills() != null) user.setSkills(updatedUser.getSkills());
        if (updatedUser.getExperience() != null) user.setExperience(updatedUser.getExperience());
        if (updatedUser.getEducation() != null) user.setEducation(updatedUser.getEducation());
        if (updatedUser.getCoverLetter() != null) user.setCoverLetter(updatedUser.getCoverLetter());
        if (updatedUser.getCompanyName() != null) user.setCompanyName(updatedUser.getCompanyName());
        if (updatedUser.getCompanyDescription() != null) user.setCompanyDescription(updatedUser.getCompanyDescription());
        if (updatedUser.getCompanyWebsite() != null) user.setCompanyWebsite(updatedUser.getCompanyWebsite());
        if (updatedUser.getIndustry() != null) user.setIndustry(updatedUser.getIndustry());
        if (updatedUser.getCompanySize() != null) user.setCompanySize(updatedUser.getCompanySize());
        
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        response.put("success", true);
        response.put("message", "Profile updated successfully");
        response.put("user", createUserResponse(savedUser));
        
        return response;
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
    
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    public Map<String, Object> createUserWithoutAuth(String phoneNumber, String fullName, String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check if user already exists by phone number
            Optional<User> existingUser = userRepository.findByPhoneNumber(phoneNumber);
            User user;
            
            if (existingUser.isPresent()) {
                // User exists, return existing user
                user = existingUser.get();
                response.put("success", true);
                response.put("message", "User already exists");
                response.put("user", createUserResponse(user));
                return response;
            }
            
            // Create new user without authentication
            user = new User();
            user.setPhoneNumber(phoneNumber);
            user.setEmail(email != null ? email : "");
            user.setFullName(fullName != null ? fullName : "User");
            user.setRole(UserRole.WORKER); // Default role
            user.setVerified(true); // Skip verification for now
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            user = userRepository.save(user);
            
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("user", createUserResponse(user));
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "User creation failed: " + e.getMessage());
        }
        
        return response;
    }
}
