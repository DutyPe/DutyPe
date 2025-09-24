package com.parttimes.backend.notifications.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailNotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@parttimes.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public void sendJobApplicationNotification(String toEmail, String jobTitle, String companyName) {
        String subject = "Application Received - " + jobTitle;
        String body = String.format(
            "Dear Applicant,\n\n" +
            "Thank you for your interest in the %s position at %s.\n\n" +
            "We have received your application and our team will review it shortly. " +
            "You will be notified of the next steps within 2-3 business days.\n\n" +
            "Best regards,\n" +
            "The %s Team",
            jobTitle, companyName, companyName
        );

        sendSimpleEmail(toEmail, subject, body);
    }

    public void sendApplicationStatusUpdate(String toEmail, String jobTitle, String status, String companyName) {
        String subject = "Application Update - " + jobTitle;
        String body = String.format(
            "Dear Applicant,\n\n" +
            "We have an update regarding your application for the %s position at %s.\n\n" +
            "Status: %s\n\n" +
            "Thank you for your interest in our company.\n\n" +
            "Best regards,\n" +
            "The %s Team",
            jobTitle, companyName, status, companyName
        );

        sendSimpleEmail(toEmail, subject, body);
    }

    public void sendJobAlert(String toEmail, String jobTitle, String companyName, String location, String payAmount) {
        String subject = "New Job Alert - " + jobTitle;
        String body = String.format(
            "Hello,\n\n" +
            "We found a new job that matches your preferences:\n\n" +
            "Job Title: %s\n" +
            "Company: %s\n" +
            "Location: %s\n" +
            "Pay: %s\n\n" +
            "Apply now at: %s\n\n" +
            "Best regards,\n" +
            "ParTimes Team",
            jobTitle, companyName, location, payAmount, baseUrl
        );

        sendSimpleEmail(toEmail, subject, body);
    }

    public void sendWelcomeEmail(String toEmail, String fullName, String role) {
        String subject = "Welcome to ParTimes!";
        String body = String.format(
            "Dear %s,\n\n" +
            "Welcome to ParTimes! We're excited to have you join our community as a %s.\n\n" +
            "Here's what you can do next:\n" +
            "- Complete your profile to get better job matches\n" +
            "- Browse available opportunities\n" +
            "- Set up job alerts for your preferred positions\n\n" +
            "If you have any questions, feel free to contact our support team.\n\n" +
            "Best regards,\n" +
            "The ParTimes Team",
            fullName, role
        );

        sendSimpleEmail(toEmail, subject, body);
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String subject = "Password Reset Request";
        String resetUrl = baseUrl + "/reset-password?token=" + resetToken;
        String body = String.format(
            "Hello,\n\n" +
            "You have requested to reset your password. Click the link below to reset your password:\n\n" +
            "%s\n\n" +
            "This link will expire in 24 hours.\n\n" +
            "If you did not request this password reset, please ignore this email.\n\n" +
            "Best regards,\n" +
            "The ParTimes Team",
            resetUrl
        );

        sendSimpleEmail(toEmail, subject, body);
    }

    public void sendJobPostedConfirmation(String toEmail, String jobTitle, String companyName) {
        String subject = "Job Posted Successfully - " + jobTitle;
        String body = String.format(
            "Dear %s,\n\n" +
            "Your job posting '%s' has been successfully posted and is now live on our platform.\n\n" +
            "You can manage your job posting and view applications through your employer dashboard.\n\n" +
            "Best regards,\n" +
            "The ParTimes Team",
            companyName, jobTitle
        );

        sendSimpleEmail(toEmail, subject, body);
    }

    public void sendNewApplicationNotification(String toEmail, String jobTitle, String applicantName) {
        String subject = "New Application Received - " + jobTitle;
        String body = String.format(
            "Dear Employer,\n\n" +
            "You have received a new application for the '%s' position.\n\n" +
            "Applicant: %s\n\n" +
            "Please review the application in your employer dashboard.\n\n" +
            "Best regards,\n" +
            "The ParTimes Team",
            jobTitle, applicantName
        );

        sendSimpleEmail(toEmail, subject, body);
    }

    public void sendNewsletter(String toEmail, String content) {
        String subject = "ParTimes Weekly Newsletter";
        sendSimpleEmail(toEmail, subject, content);
    }

    public void sendCustomEmail(String toEmail, String subject, String body) {
        sendSimpleEmail(toEmail, subject, body);
    }

    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        if (mailSender == null) {
            System.out.println("Email service not configured. Skipping HTML email to: " + toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    public void sendBulkEmails(String[] toEmails, String subject, String body) {
        for (String email : toEmails) {
            sendSimpleEmail(email, subject, body);
        }
    }

    private void sendSimpleEmail(String toEmail, String subject, String body) {
        if (mailSender == null) {
            System.out.println("Email service not configured. Skipping email to: " + toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public String generateJobAlertHtml(String jobTitle, String companyName, String location, String payAmount, String jobUrl) {
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head><title>Job Alert</title></head>" +
            "<body style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
            "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 8px;'>" +
            "<h2 style='color: #333;'>New Job Alert</h2>" +
            "<div style='background-color: white; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
            "<h3 style='color: #007bff; margin-top: 0;'>%s</h3>" +
            "<p><strong>Company:</strong> %s</p>" +
            "<p><strong>Location:</strong> %s</p>" +
            "<p><strong>Pay:</strong> %s</p>" +
            "<a href='%s' style='background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; display: inline-block; margin-top: 15px;'>Apply Now</a>" +
            "</div>" +
            "<p style='color: #666; font-size: 14px;'>Best regards,<br>The ParTimes Team</p>" +
            "</div>" +
            "</body>" +
            "</html>",
            jobTitle, companyName, location, payAmount, jobUrl
        );
    }
}
