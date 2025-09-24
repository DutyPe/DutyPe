package com.parttimes.backend.common.controllers;

import com.parttimes.backend.common.services.JobSharingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
@CrossOrigin(origins = "*")
public class JobSharingController {

    @Autowired
    private JobSharingService jobSharingService;

    @GetMapping("/job/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobShareData(@PathVariable String jobId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> shareData = jobSharingService.generateJobShareData(jobId);
            
            response.put("success", true);
            response.put("shareData", shareData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/collection")
    public ResponseEntity<Map<String, Object>> getJobCollectionShareData(
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String[] jobIds = ((java.util.List<String>) request.get("jobIds")).toArray(new String[0]);
            String collectionName = (String) request.get("collectionName");
            
            Map<String, Object> shareData = jobSharingService.generateJobCollectionShareData(jobIds, collectionName);
            
            response.put("success", true);
            response.put("shareData", shareData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/whatsapp/{jobId}")
    public ResponseEntity<Map<String, Object>> getWhatsAppShareData(@PathVariable String jobId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> shareData = jobSharingService.generateJobShareData(jobId);
            String whatsappMessage = (String) shareData.get("whatsappMessage");
            String shareUrl = (String) shareData.get("shareUrl");
            
            // Generate WhatsApp share URL
            String whatsappUrl = "https://wa.me/?text=" + java.net.URLEncoder.encode(whatsappMessage, "UTF-8");
            
            response.put("success", true);
            response.put("whatsappUrl", whatsappUrl);
            response.put("message", whatsappMessage);
            response.put("shareUrl", shareUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/facebook/{jobId}")
    public ResponseEntity<Map<String, Object>> getFacebookShareData(@PathVariable String jobId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> shareData = jobSharingService.generateJobShareData(jobId);
            String facebookMessage = (String) shareData.get("facebookMessage");
            String shareUrl = (String) shareData.get("shareUrl");
            
            // Generate Facebook share URL
            String facebookUrl = "https://www.facebook.com/sharer/sharer.php?u=" + 
                                java.net.URLEncoder.encode(shareUrl, "UTF-8");
            
            response.put("success", true);
            response.put("facebookUrl", facebookUrl);
            response.put("message", facebookMessage);
            response.put("shareUrl", shareUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/twitter/{jobId}")
    public ResponseEntity<Map<String, Object>> getTwitterShareData(@PathVariable String jobId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> shareData = jobSharingService.generateJobShareData(jobId);
            String twitterMessage = (String) shareData.get("twitterMessage");
            String shareUrl = (String) shareData.get("shareUrl");
            
            // Generate Twitter share URL
            String twitterUrl = "https://twitter.com/intent/tweet?text=" + 
                               java.net.URLEncoder.encode(twitterMessage, "UTF-8");
            
            response.put("success", true);
            response.put("twitterUrl", twitterUrl);
            response.put("message", twitterMessage);
            response.put("shareUrl", shareUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/linkedin/{jobId}")
    public ResponseEntity<Map<String, Object>> getLinkedInShareData(@PathVariable String jobId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> shareData = jobSharingService.generateJobShareData(jobId);
            String linkedinMessage = (String) shareData.get("linkedinMessage");
            String shareUrl = (String) shareData.get("shareUrl");
            
            // Generate LinkedIn share URL
            String linkedinUrl = "https://www.linkedin.com/sharing/share-offsite/?url=" + 
                                java.net.URLEncoder.encode(shareUrl, "UTF-8");
            
            response.put("success", true);
            response.put("linkedinUrl", linkedinUrl);
            response.put("message", linkedinMessage);
            response.put("shareUrl", shareUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/email/{jobId}")
    public ResponseEntity<Map<String, Object>> getEmailShareData(@PathVariable String jobId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> shareData = jobSharingService.generateJobShareData(jobId);
            String emailSubject = (String) shareData.get("emailSubject");
            String emailBody = (String) shareData.get("emailBody");
            String shareUrl = (String) shareData.get("shareUrl");
            
            // Generate email share URL
            String emailUrl = "mailto:?subject=" + java.net.URLEncoder.encode(emailSubject, "UTF-8") +
                             "&body=" + java.net.URLEncoder.encode(emailBody, "UTF-8");
            
            response.put("success", true);
            response.put("emailUrl", emailUrl);
            response.put("subject", emailSubject);
            response.put("body", emailBody);
            response.put("shareUrl", shareUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
