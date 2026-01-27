package com.example.dutype.employer.validation

import com.example.dutype.core.validation.*

/**
 * Job Posting Validation Rules
 * 
 * Centralized validation for job posting forms.
 * Used across PostJobScreen and AIJobPostingScreen.
 * 
 * @author DutyPe Engineering Team
 * @since 3.0.0
 */
object JobPostingValidation {
    
    /**
     * Validation rules for job posting
     */
    fun getJobPostingRules() = validationRules {
        field("title") {
            required("Job title is required")
            length(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
        }
        
        field("description") {
            required("Job description is required")
            length(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
        }
        
        field("category") {
            required("Job category is required")
        }
        
        field("location") {
            required("Job location is required")
            length(min = 3, max = 200, message = "Location must be between 3 and 200 characters")
        }
        
        field("salary") {
            required("Salary is required")
            range(min = 0, max = 1000000, message = "Salary must be between 0 and 10,00,000")
        }
        
        field("salaryPeriod") {
            required("Salary period is required")
            custom(message = "Invalid salary period") { value ->
                value in listOf("HOURLY", "DAILY", "WEEKLY", "MONTHLY", "YEARLY", "ONE_TIME")
            }
        }
        
        field("vacancies") {
            required("Number of vacancies is required")
            range(min = 1, max = 1000, message = "Vacancies must be between 1 and 1000")
        }
        
        field("workingHours") {
            length(max = 100, message = "Working hours must be less than 100 characters")
        }
        
        field("contactPhone") {
            phone("Invalid phone number format")
        }
        
        field("contactEmail") {
            email("Invalid email address")
        }
        
        field("companyName") {
            required("Company name is required")
            length(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
        }
    }
    
    /**
     * Validation rules for quick job posting (minimal fields)
     */
    fun getQuickJobPostingRules() = validationRules {
        field("title") {
            required("Job title is required")
            length(min = 5, max = 100)
        }
        
        field("category") {
            required("Job category is required")
        }
        
        field("location") {
            required("Job location is required")
        }
        
        field("salary") {
            required("Salary is required")
            range(min = 0, max = 1000000)
        }
    }
}
