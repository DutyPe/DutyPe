package com.parttimes.backend.employer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class JobListingDto {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("jobId")
    private String jobId;
    
    @JsonProperty("employerId")
    private String employerId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("companyName")
    private String companyName;
    
    @JsonProperty("company")
    private String company;
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("specificLocation")
    private String specificLocation;
    
    @JsonProperty("locationNearby")
    private String locationNearby;
    
    @JsonProperty("area")
    private String area;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("payRate")
    private Double payRate;
    
    @JsonProperty("wage")
    private String wage;
    
    @JsonProperty("payType")
    private String payType;
    
    @JsonProperty("payPeriod")
    private String payPeriod;
    
    @JsonProperty("timing")
    private String timing;
    
    @JsonProperty("shiftTiming")
    private String shiftTiming;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("preferences")
    private List<String> preferences;
    
    @JsonProperty("perks")
    private List<String> perks;
    
    @JsonProperty("benefits")
    private List<String> benefits;
    
    @JsonProperty("requirements")
    private List<String> requirements;
    
    @JsonProperty("skills")
    private List<String> skills;
    
    @JsonProperty("vacancies")
    private Integer vacancies;
    
    @JsonProperty("isActive")
    private Boolean isActive;
    
    @JsonProperty("isTrending")
    private Boolean isTrending;
    
    @JsonProperty("isRemote")
    private Boolean isRemote;
    
    @JsonProperty("isVerified")
    private Boolean isVerified;
    
    @JsonProperty("isUrgent")
    private Boolean isUrgent;
    
    @JsonProperty("isBookmarked")
    private Boolean isBookmarked;
    
    @JsonProperty("isApplied")
    private Boolean isApplied;
    
    @JsonProperty("postedAt")
    private Long postedAt;
    
    @JsonProperty("postedTime")
    private String postedTime;
    
    @JsonProperty("postedDate")
    private String postedDate;
    
    @JsonProperty("imageUrl")
    private String imageUrl;
    
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @JsonProperty("contactNumber")
    private String contactNumber;
    
    @JsonProperty("contactInfo")
    private String contactInfo;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("jobType")
    private String jobType;
    
    @JsonProperty("experienceLevel")
    private String experienceLevel;
    
    @JsonProperty("experienceRequired")
    private String experienceRequired;
    
    @JsonProperty("workingHours")
    private String workingHours;
    
    @JsonProperty("applicationDeadline")
    private String applicationDeadline;
    
    @JsonProperty("ageRange")
    private String ageRange;
    
    @JsonProperty("gender")
    private String gender;
    
    @JsonProperty("companySize")
    private String companySize;
    
    @JsonProperty("industry")
    private String industry;
    
    @JsonProperty("viewCount")
    private Long viewCount;
    
    @JsonProperty("applicationCount")
    private Long applicationCount;
    
    @JsonProperty("distance")
    private Double distance;
    
    @JsonProperty("salary")
    private String salary;
    
    @JsonProperty("urgency")
    private String urgency;

    // Default constructor
    public JobListingDto() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getEmployerId() { return employerId; }
    public void setEmployerId(String employerId) { this.employerId = employerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSpecificLocation() { return specificLocation; }
    public void setSpecificLocation(String specificLocation) { this.specificLocation = specificLocation; }

    public String getLocationNearby() { return locationNearby; }
    public void setLocationNearby(String locationNearby) { this.locationNearby = locationNearby; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Double getPayRate() { return payRate; }
    public void setPayRate(Double payRate) { this.payRate = payRate; }

    public String getWage() { return wage; }
    public void setWage(String wage) { this.wage = wage; }

    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }

    public String getPayPeriod() { return payPeriod; }
    public void setPayPeriod(String payPeriod) { this.payPeriod = payPeriod; }

    public String getTiming() { return timing; }
    public void setTiming(String timing) { this.timing = timing; }

    public String getShiftTiming() { return shiftTiming; }
    public void setShiftTiming(String shiftTiming) { this.shiftTiming = shiftTiming; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getPreferences() { return preferences; }
    public void setPreferences(List<String> preferences) { this.preferences = preferences; }

    public List<String> getPerks() { return perks; }
    public void setPerks(List<String> perks) { this.perks = perks; }

    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }

    public List<String> getRequirements() { return requirements; }
    public void setRequirements(List<String> requirements) { this.requirements = requirements; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public Integer getVacancies() { return vacancies; }
    public void setVacancies(Integer vacancies) { this.vacancies = vacancies; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsTrending() { return isTrending; }
    public void setIsTrending(Boolean isTrending) { this.isTrending = isTrending; }

    public Boolean getIsRemote() { return isRemote; }
    public void setIsRemote(Boolean isRemote) { this.isRemote = isRemote; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public Boolean getIsUrgent() { return isUrgent; }
    public void setIsUrgent(Boolean isUrgent) { this.isUrgent = isUrgent; }

    public Boolean getIsBookmarked() { return isBookmarked; }
    public void setIsBookmarked(Boolean isBookmarked) { this.isBookmarked = isBookmarked; }

    public Boolean getIsApplied() { return isApplied; }
    public void setIsApplied(Boolean isApplied) { this.isApplied = isApplied; }

    public Long getPostedAt() { return postedAt; }
    public void setPostedAt(Long postedAt) { this.postedAt = postedAt; }

    public String getPostedTime() { return postedTime; }
    public void setPostedTime(String postedTime) { this.postedTime = postedTime; }

    public String getPostedDate() { return postedDate; }
    public void setPostedDate(String postedDate) { this.postedDate = postedDate; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }

    public String getExperienceRequired() { return experienceRequired; }
    public void setExperienceRequired(String experienceRequired) { this.experienceRequired = experienceRequired; }

    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }

    public String getApplicationDeadline() { return applicationDeadline; }
    public void setApplicationDeadline(String applicationDeadline) { this.applicationDeadline = applicationDeadline; }

    public String getAgeRange() { return ageRange; }
    public void setAgeRange(String ageRange) { this.ageRange = ageRange; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getCompanySize() { return companySize; }
    public void setCompanySize(String companySize) { this.companySize = companySize; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public Long getApplicationCount() { return applicationCount; }
    public void setApplicationCount(Long applicationCount) { this.applicationCount = applicationCount; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
}
