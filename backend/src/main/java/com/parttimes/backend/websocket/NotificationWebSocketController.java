package com.parttimes.backend.websocket;

import com.parttimes.backend.notifications.models.Notification;
import com.parttimes.backend.notifications.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class NotificationWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    @MessageMapping("/notifications.send")
    public void sendNotification(@Payload Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        String title = (String) payload.get("title");
        String message = (String) payload.get("message");
        String type = (String) payload.get("type");

        // Create notification
        // Save notification
        Notification savedNotification = notificationService.createNotification(
            userId, 
            title, 
            message, 
            com.parttimes.backend.notifications.models.NotificationType.valueOf(type)
        );

        // Send to specific user
        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/notifications",
            savedNotification
        );
    }

    @MessageMapping("/notifications.mark-read")
    public void markNotificationAsRead(@Payload Map<String, Object> payload) {
        String notificationId = (String) payload.get("notificationId");
        String userId = (String) payload.get("userId");

        try {
            notificationService.markAsRead(notificationId, userId);
            
            // Send confirmation back to user
            messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                Map.of("type", "MARKED_READ", "notificationId", notificationId)
            );
        } catch (Exception e) {
            // Send error back to user
            messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                Map.of("type", "ERROR", "message", e.getMessage())
            );
        }
    }
}
