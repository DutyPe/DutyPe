package com.parttimes.backend.notifications.controllers;

import com.parttimes.backend.notifications.models.Notification;
import com.parttimes.backend.notifications.repositories.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    // Get user's notifications
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notifications endpoint - no authentication required");
        response.put("notifications", List.of());
        
        return ResponseEntity.ok(response);
    }
    
    // Get unread notifications count
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Unread count endpoint - no authentication required");
        response.put("count", 0);
        
        return ResponseEntity.ok(response);
    }
    
    // Get unread notifications
    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Unread notifications endpoint - no authentication required");
        response.put("notifications", List.of());
        
        return ResponseEntity.ok(response);
    }
    
    // Mark notification as read
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Mark as read endpoint - no authentication required");
        
        return ResponseEntity.ok(response);
    }
    
    // Mark all notifications as read
    @PutMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Mark all as read endpoint - no authentication required");
        
        return ResponseEntity.ok(response);
    }
    
    // Delete notification
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Delete notification endpoint - no authentication required");
        
        return ResponseEntity.ok(response);
    }
    
    // Create notification (for system use)
    @PostMapping
    public ResponseEntity<Map<String, Object>> createNotification(@RequestBody Notification notification) {
        notification.setCreatedAt(LocalDateTime.now());
        Notification savedNotification = notificationRepository.save(notification);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification created successfully");
        response.put("notification", savedNotification);
        
        return ResponseEntity.ok(response);
    }
    
    // Get notification by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getNotification(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Get notification endpoint - no authentication required");
        response.put("notification", null);
        
        return ResponseEntity.ok(response);
    }
}
