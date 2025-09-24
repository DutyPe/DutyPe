package com.parttimes.backend.common.services;

import com.parttimes.backend.employer.models.JobPosting;
import com.parttimes.backend.employer.repositories.JobPostingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JobSharingService {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public Map<String, Object> generateJobShareData(String jobId) {
        Optional<JobPosting> jobOpt = jobPostingRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            throw new RuntimeException("Job not found");
        }

        JobPosting job = jobOpt.get();
        Map<String, Object> shareData = new HashMap<>();

        // Generate shareable URL
        String shareUrl = baseUrl + "/jobs/" + jobId;
        shareData.put("shareUrl", shareUrl);

        // Generate social media content
        shareData.put("title", job.getTitle());
        shareData.put("description", generateJobDescription(job));
        shareData.put("location", job.getCity());
        shareData.put("payAmount", job.getPayAmount());
        shareData.put("payType", job.getPayType());
        shareData.put("company", job.getEmployerName());

        // Generate platform-specific content
        shareData.put("whatsappMessage", generateWhatsAppMessage(job, shareUrl));
        shareData.put("facebookMessage", generateFacebookMessage(job, shareUrl));
        shareData.put("twitterMessage", generateTwitterMessage(job, shareUrl));
        shareData.put("linkedinMessage", generateLinkedInMessage(job, shareUrl));
        shareData.put("emailSubject", generateEmailSubject(job));
        shareData.put("emailBody", generateEmailBody(job, shareUrl));

        // Generate QR code data
        shareData.put("qrCodeData", shareUrl);

        return shareData;
    }

    public Map<String, Object> generateJobCollectionShareData(String[] jobIds, String collectionName) {
        Map<String, Object> shareData = new HashMap<>();
        
        // Generate collection share URL
        String shareUrl = baseUrl + "/collections/" + String.join(",", jobIds);
        shareData.put("shareUrl", shareUrl);
        shareData.put("collectionName", collectionName);
        shareData.put("jobCount", jobIds.length);

        // Generate collection description
        shareData.put("description", "Check out this collection of " + jobIds.length + " amazing job opportunities!");

        // Generate platform-specific content
        shareData.put("whatsappMessage", generateCollectionWhatsAppMessage(collectionName, jobIds.length, shareUrl));
        shareData.put("facebookMessage", generateCollectionFacebookMessage(collectionName, jobIds.length, shareUrl));
        shareData.put("twitterMessage", generateCollectionTwitterMessage(collectionName, jobIds.length, shareUrl));

        return shareData;
    }

    private String generateJobDescription(JobPosting job) {
        StringBuilder description = new StringBuilder();
        description.append("🚀 ").append(job.getTitle()).append("\n\n");
        description.append("📍 Location: ").append(job.getCity()).append("\n");
        description.append("💰 Pay: ").append(job.getPayAmount()).append("/").append(job.getPayType().toLowerCase()).append("\n");
        description.append("🏢 Company: ").append(job.getEmployerName()).append("\n\n");
        
        if (job.getDescription() != null && !job.getDescription().isEmpty()) {
            String shortDescription = job.getDescription().length() > 200 
                ? job.getDescription().substring(0, 200) + "..." 
                : job.getDescription();
            description.append(shortDescription).append("\n\n");
        }
        
        description.append("Apply now and find your dream job! 💼");
        return description.toString();
    }

    private String generateWhatsAppMessage(JobPosting job, String shareUrl) {
        return "🚀 *" + job.getTitle() + "*\n\n" +
               "📍 " + job.getCity() + "\n" +
               "💰 " + job.getPayAmount() + "/" + job.getPayType().toLowerCase() + "\n" +
               "🏢 " + job.getEmployerName() + "\n\n" +
               "Check out this amazing job opportunity!\n" +
               shareUrl;
    }

    private String generateFacebookMessage(JobPosting job, String shareUrl) {
        return "🚀 " + job.getTitle() + "\n\n" +
               "📍 Location: " + job.getCity() + "\n" +
               "💰 Pay: " + job.getPayAmount() + "/" + job.getPayType().toLowerCase() + "\n" +
               "🏢 Company: " + job.getEmployerName() + "\n\n" +
               "Looking for a new opportunity? Check out this job posting!\n" +
               shareUrl;
    }

    private String generateTwitterMessage(JobPosting job, String shareUrl) {
        String message = "🚀 " + job.getTitle() + " in " + job.getCity() + 
                        " - " + job.getPayAmount() + "/" + job.getPayType().toLowerCase() + 
                        " at " + job.getEmployerName() + "\n" + shareUrl;
        
        // Twitter character limit
        if (message.length() > 280) {
            message = "🚀 " + job.getTitle() + " in " + job.getCity() + 
                     " - " + job.getPayAmount() + "/" + job.getPayType().toLowerCase() + "\n" + shareUrl;
        }
        
        return message;
    }

    private String generateLinkedInMessage(JobPosting job, String shareUrl) {
        return "🚀 Exciting Job Opportunity: " + job.getTitle() + "\n\n" +
               "📍 Location: " + job.getCity() + "\n" +
               "💰 Compensation: " + job.getPayAmount() + "/" + job.getPayType().toLowerCase() + "\n" +
               "🏢 Company: " + job.getEmployerName() + "\n\n" +
               "If you're looking for your next career move, this could be the perfect opportunity for you!\n\n" +
               "Apply here: " + shareUrl + "\n\n" +
               "#Jobs #Career #Opportunity #" + job.getCity().replace(" ", "") + 
               " #" + job.getCategory().replace(" ", "");
    }

    private String generateEmailSubject(JobPosting job) {
        return "Job Opportunity: " + job.getTitle() + " in " + job.getCity();
    }

    private String generateEmailBody(JobPosting job, String shareUrl) {
        return "Hi there!\n\n" +
               "I found this amazing job opportunity that might interest you:\n\n" +
               "🚀 " + job.getTitle() + "\n" +
               "📍 Location: " + job.getCity() + "\n" +
               "💰 Pay: " + job.getPayAmount() + "/" + job.getPayType().toLowerCase() + "\n" +
               "🏢 Company: " + job.getEmployerName() + "\n\n" +
               "Check it out and apply if you're interested:\n" +
               shareUrl + "\n\n" +
               "Good luck with your job search!\n\n" +
               "Best regards";
    }

    private String generateCollectionWhatsAppMessage(String collectionName, int jobCount, String shareUrl) {
        return "📋 *" + collectionName + "*\n\n" +
               "I've curated " + jobCount + " amazing job opportunities for you!\n\n" +
               "Check them out: " + shareUrl + "\n\n" +
               "Good luck with your job search! 🚀";
    }

    private String generateCollectionFacebookMessage(String collectionName, int jobCount, String shareUrl) {
        return "📋 " + collectionName + "\n\n" +
               "I've found " + jobCount + " great job opportunities that might interest you!\n\n" +
               "Take a look: " + shareUrl + "\n\n" +
               "Share with anyone who might be looking for work! 🚀";
    }

    private String generateCollectionTwitterMessage(String collectionName, int jobCount, String shareUrl) {
        String message = "📋 " + collectionName + " - " + jobCount + " job opportunities!\n" + shareUrl;
        
        if (message.length() > 280) {
            message = "📋 " + jobCount + " job opportunities in " + collectionName + "!\n" + shareUrl;
        }
        
        return message;
    }
}
