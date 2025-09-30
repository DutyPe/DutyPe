package com.parttimes.backend.notifications.services;

import com.parttimes.backend.notifications.models.Notification;
import com.parttimes.backend.notifications.models.NotificationType;
import com.parttimes.backend.notifications.models.NotificationPriority;
import com.parttimes.backend.notifications.repositories.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    public Notification createNotification(String userId, String title, String message, NotificationType type) {
        Notification notification = new Notification(userId, title, message, type);
        return notificationRepository.save(notification);
    }
    
    public Notification createNotification(String userId, String title, String message, NotificationType type, NotificationPriority priority) {
        Notification notification = new Notification(userId, title, message, type);
        notification.setPriority(priority);
        return notificationRepository.save(notification);
    }
    
    public Notification createJobApplicationNotification(String workerId, String employerId, String jobId, String applicationId) {
        Notification notification = new Notification(
            employerId,
            "New Job Application",
            "You have received a new job application",
            NotificationType.JOB_APPLICATION
        );
        notification.setWorkerId(workerId);
        notification.setJobId(jobId);
        notification.setApplicationId(applicationId);
        notification.setPriority(NotificationPriority.HIGH);
        return notificationRepository.save(notification);
    }
    
    public Notification createApplicationStatusNotification(String workerId, String applicationId, String status) {
        String title = "Application Status Update";
        String message = "Your job application status has been updated to: " + status;
        
        Notification notification = new Notification(
            workerId,
            title,
            message,
            NotificationType.APPLICATION_STATUS_UPDATE
        );
        notification.setApplicationId(applicationId);
        notification.setPriority(NotificationPriority.HIGH);
        return notificationRepository.save(notification);
    }
    
    public Notification createInterviewScheduledNotification(String workerId, String jobId, String interviewDate) {
        Notification notification = new Notification(
            workerId,
            "Interview Scheduled",
            "Your interview has been scheduled for " + interviewDate,
            NotificationType.INTERVIEW_SCHEDULED
        );
        notification.setJobId(jobId);
        notification.setPriority(NotificationPriority.HIGH);
        return notificationRepository.save(notification);
    }
    
    public Notification createJobOfferNotification(String workerId, String jobId, String companyName) {
        Notification notification = new Notification(
            workerId,
            "Job Offer Received",
            "Congratulations! You have received a job offer from " + companyName,
            NotificationType.JOB_OFFER
        );
        notification.setJobId(jobId);
        notification.setPriority(NotificationPriority.URGENT);
        return notificationRepository.save(notification);
    }
    
    public Notification createJobRejectionNotification(String workerId, String jobId, String companyName) {
        Notification notification = new Notification(
            workerId,
            "Application Update",
            "Your application for the position at " + companyName + " was not selected",
            NotificationType.JOB_REJECTION
        );
        notification.setJobId(jobId);
        notification.setPriority(NotificationPriority.NORMAL);
        return notificationRepository.save(notification);
    }
    
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserId(userId);
    }
    
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }
    
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    public Notification markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getUserId().equals(userId)) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            return notificationRepository.save(notification);
        }
        return null;
    }
    
    public void markAllAsRead(String userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(unreadNotifications);
    }
    
    public void deleteNotification(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getUserId().equals(userId)) {
            notificationRepository.delete(notification);
        }
    }
    
    public void deleteAllNotifications(String userId) {
        List<Notification> userNotifications = notificationRepository.findByUserId(userId);
        notificationRepository.deleteAll(userNotifications);
    }
}
