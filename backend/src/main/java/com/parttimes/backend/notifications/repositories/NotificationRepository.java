package com.parttimes.backend.notifications.repositories;

import com.parttimes.backend.notifications.models.Notification;
import com.parttimes.backend.notifications.models.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    // Find notifications by user
    List<Notification> findByUserId(String userId);
    
    // Find notifications by user with pagination
    Page<Notification> findByUserId(String userId, Pageable pageable);
    
    // Find unread notifications by user
    List<Notification> findByUserIdAndIsReadFalse(String userId);
    
    // Find notifications by type
    List<Notification> findByType(NotificationType type);
    
    // Find notifications by user and type
    List<Notification> findByUserIdAndType(String userId, NotificationType type);
    
    // Count unread notifications by user
    long countByUserIdAndIsReadFalse(String userId);
    
    // Find notifications by job
    List<Notification> findByJobId(String jobId);
    
    // Find notifications by application
    List<Notification> findByApplicationId(String applicationId);
    
    // Find notifications by employer
    List<Notification> findByEmployerId(String employerId);
    
    // Find notifications by worker
    List<Notification> findByWorkerId(String workerId);
}
