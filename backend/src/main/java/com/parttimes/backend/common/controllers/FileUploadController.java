package com.parttimes.backend.common.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    @PostMapping("/document")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String type) { // resume, profile, document
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check file size (10MB limit)
            if (file.getSize() > 10 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "File size exceeds 10MB limit");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && 
                !contentType.equals("application/pdf") && 
                !contentType.equals("application/msword") &&
                !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
                response.put("success", false);
                response.put("message", "Invalid file type. Only images, PDF, and Word documents are allowed");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Generate file URL
            String fileUrl = "/uploads/" + uniqueFilename;
            
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("filename", uniqueFilename);
            response.put("originalFilename", originalFilename);
            response.put("fileUrl", fileUrl);
            response.put("fileSize", file.getSize());
            response.put("contentType", contentType);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/profile-image")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check file size (5MB limit for images)
            if (file.getSize() > 5 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "File size exceeds 5MB limit");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if it's an image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "Invalid file type. Only images are allowed");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir + "/profile-images");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Generate file URL
            String fileUrl = "/uploads/profile-images/" + uniqueFilename;
            
            response.put("success", true);
            response.put("message", "Profile image uploaded successfully");
            response.put("filename", uniqueFilename);
            response.put("originalFilename", originalFilename);
            response.put("fileUrl", fileUrl);
            response.put("fileSize", file.getSize());
            response.put("contentType", contentType);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to upload profile image: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @DeleteMapping("/{filename}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String filename) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            
            if (!Files.exists(filePath)) {
                response.put("success", false);
                response.put("message", "File not found");
                return ResponseEntity.notFound().build();
            }
            
            Files.delete(filePath);
            
            response.put("success", true);
            response.put("message", "File deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to delete file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
