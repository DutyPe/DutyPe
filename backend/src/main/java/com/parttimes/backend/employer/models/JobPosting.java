package com.parttimes.backend.employer.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Document(collection = "jobPostings")
public class JobPosting {

    @Id
    private String jobId = UUID.randomUUID().toString();

    private String title;
    private String payAmount;
    private String payType;  // DAILY, HOURLY, MONTHLY, PER_TASK
    private String location;
    private String area;
    private String city;
    private String description;
    private String contactNumber;
    private String category;
    private String shiftTiming = "FLEXIBLE";
    private String urgency = "NORMAL"; // IMMEDIATE, URGENT, NORMAL
    private List<String> requirements;
    private List<String> benefits;
    private int vacancies = 1;
    private LocalDateTime postedTime = LocalDateTime.now();
    private boolean isVerified = false;
    private String employerId;
    private String employerName = "";
    private boolean isActive = true;
    private int applicationsReceived = 0;
    private int applicationCount = 0; // Alias for applicationsReceived
    private int viewCount = 0;
    
    // Additional fields to match Android JobCardModel
    private String imageUrl;
    private String workingHours;
    private String experienceRequired;
    private String ageRange;
    private String gender = "Any";
    private String jobType = "Part-time"; // Full-time, Part-time, Contract
    private String applicationDeadline;
    private String companySize;
    private String industry;
    // Note: isBookmarked and isApplied are jobseeker-specific and should be handled separately

    public JobPosting() {}

    // Getters and Setters
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(String payAmount) {
        this.payAmount = payAmount;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }


    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getShiftTiming() {
        return shiftTiming;
    }

    public void setShiftTiming(String shiftTiming) {
        this.shiftTiming = shiftTiming;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public List<String> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<String> requirements) {
        this.requirements = requirements;
    }

    public List<String> getBenefits() {
        return benefits;
    }

    public void setBenefits(List<String> benefits) {
        this.benefits = benefits;
    }

    public int getVacancies() {
        return vacancies;
    }

    public void setVacancies(int vacancies) {
        this.vacancies = vacancies;
    }

    public LocalDateTime getPostedTime() {
        return postedTime;
    }

    public void setPostedTime(LocalDateTime postedTime) {
        this.postedTime = postedTime;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getEmployerId() {
        return employerId;
    }

    public void setEmployerId(String employerId) {
        this.employerId = employerId;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getApplicationsReceived() {
        return applicationsReceived;
    }

    public void setApplicationsReceived(int applicationsReceived) {
        this.applicationsReceived = applicationsReceived;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(String workingHours) {
        this.workingHours = workingHours;
    }

    public String getExperienceRequired() {
        return experienceRequired;
    }

    public void setExperienceRequired(String experienceRequired) {
        this.experienceRequired = experienceRequired;
    }

    public String getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(String ageRange) {
        this.ageRange = ageRange;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(String applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public String getCompanySize() {
        return companySize;
    }

    public void setCompanySize(String companySize) {
        this.companySize = companySize;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    // Distance field removed as per user request


    // Helper methods
    public boolean isReadyToPublish() {
        return title != null && !title.isBlank() &&
                payAmount != null && !payAmount.isBlank() &&
                location != null && !location.isBlank() &&
                description != null && !description.isBlank() &&
                contactNumber != null && !contactNumber.isBlank();
    }

    public int getCompletionPercentage() {
        int completed = 0;
        int total = 7;

        if (title != null && !title.isBlank()) completed++;
        if (payAmount != null && !payAmount.isBlank()) completed++;
        if (location != null && !location.isBlank()) completed++;
        if (description != null && !description.isBlank()) completed++;
        if (contactNumber != null && !contactNumber.isBlank()) completed++;
        // Perks field removed as per user request
        if (vacancies > 0) completed++;

        return (completed * 100) / total;
    }

    public String getTimeAgo() {
        LocalDateTime now = LocalDateTime.now();
        long diffInSeconds = java.time.Duration.between(postedTime, now).getSeconds();
        long diffInMinutes = diffInSeconds / 60;
        long diffInHours = diffInMinutes / 60;
        long diffInDays = diffInHours / 24;
        long diffInWeeks = diffInDays / 7;

        if (diffInSeconds < 60) return "Just now";
        else if (diffInMinutes < 60) return diffInMinutes + "m ago";
        else if (diffInHours < 24) return diffInHours + "h ago";
        else if (diffInDays < 7) return diffInDays + "d ago";
        else return diffInWeeks + "w ago";
    }

    public String getPayDisplayText() {
        return "₹" + payAmount + "/" + payType.toLowerCase();
    }

    public String getLocationDisplayText() {
        if (area != null && city != null) {
            return area + ", " + city;
        }
        return location;
    }
}
