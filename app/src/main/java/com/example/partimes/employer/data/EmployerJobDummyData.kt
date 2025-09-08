package com.example.partimes.employer.data

import com.example.partimes.employer.models.JobPostingModel
import com.example.partimes.employer.models.enums.*

object EmployerJobDummyData {
    fun getMyPostedJobs(): List<JobPostingModel> {
        return listOf(
            JobPostingModel(
                title = "Cook for Restaurant",
                payAmount = "400",
                payType = PayType.DAILY,
                location = "Kukatpally, Hyderabad, Near Metro Station",
                description = "Looking for experienced cook for South Indian restaurant. Must know traditional recipes and be able to handle rush hours.",
                contactNumber = "+91 9876543210",
                category = JobCategory.COOK,
                shiftTiming = ShiftTiming.MORNING,
                urgency = JobUrgency.URGENT,
                perks = listOf(JobPerk.MEALS, JobPerk.OVERTIME_PAY, JobPerk.BONUS),
                vacancies = 2,
                postedTime = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000), // 2 days ago
                isVerified = true,
                employerId = "emp_001",
                employerName = "Sri Sai Tiffins"
            ),
            JobPostingModel(
                title = "House Cleaning",
                payAmount = "300",
                payType = PayType.TASK,
                location = "Banjara Hills, Hyderabad, Residential Area",
                description = "Need reliable house cleaning service for residential property. Must be thorough and trustworthy.",
                contactNumber = "+91 9876543211",
                category = JobCategory.MAID,
                shiftTiming = ShiftTiming.FLEXIBLE,
                urgency = JobUrgency.FLEXIBLE,
                perks = listOf(JobPerk.TRANSPORT),
                vacancies = 1,
                postedTime = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000), // 5 days ago
                isVerified = false,
                employerId = "emp_002",
                employerName = "Residential Owner"
            ),
            JobPostingModel(
                title = "Delivery Executive",
                payAmount = "50",
                payType = PayType.HOURLY,
                location = "Gachibowli, Hyderabad, Tech Hub Area",
                description = "Food delivery executive needed for busy restaurant. Must have own vehicle and be familiar with area.",
                contactNumber = "+91 9876543212",
                category = JobCategory.DELIVERY,
                shiftTiming = ShiftTiming.EVENING,
                urgency = JobUrgency.IMMEDIATE,
                perks = listOf(JobPerk.BONUS, JobPerk.TRANSPORT),
                vacancies = 5,
                postedTime = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000), // 1 day ago
                isVerified = true,
                employerId = "emp_003",
                employerName = "Food Express"
            ),
            JobPostingModel(
                title = "Security Guard",
                payAmount = "12000",
                payType = PayType.MONTHLY,
                location = "Jubilee Hills, Hyderabad, Commercial Complex",
                description = "Looking for experienced security guard for night shift. Must be alert and responsible.",
                contactNumber = "+91 9876543213",
                category = JobCategory.SECURITY,
                shiftTiming = ShiftTiming.NIGHT,
                urgency = JobUrgency.NORMAL,
                perks = listOf(JobPerk.ACCOMMODATION, JobPerk.MEALS),
                vacancies = 1,
                postedTime = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                isVerified = true,
                employerId = "emp_004",
                employerName = "Phoenix Mall"
            ),
            JobPostingModel(
                title = "Garden Maintenance",
                payAmount = "8000",
                payType = PayType.MONTHLY,
                location = "Kondapur, Hyderabad, Residential Society",
                description = "Need gardener for residential society maintenance. Must know plant care and landscaping basics.",
                contactNumber = "+91 9876543214",
                category = JobCategory.GARDENER,
                shiftTiming = ShiftTiming.MORNING,
                urgency = JobUrgency.FLEXIBLE,
                perks = listOf(JobPerk.PAID_LEAVES),
                vacancies = 1,
                postedTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000), // 1 week ago
                isVerified = false,
                employerId = "emp_005",
                employerName = "Green Valley Society"
            ),
            JobPostingModel(
                title = "Car Driver",
                payAmount = "15000",
                payType = PayType.MONTHLY,
                location = "Hi-Tech City, Hyderabad, Corporate Office",
                description = "Professional driver needed for corporate executive. Must have clean driving record and good communication skills.",
                contactNumber = "+91 9876543215",
                category = JobCategory.DRIVER,
                shiftTiming = ShiftTiming.FULL_DAY,
                urgency = JobUrgency.URGENT,
                perks = listOf(JobPerk.MEDICAL, JobPerk.BONUS, JobPerk.PAID_LEAVES),
                vacancies = 1,
                postedTime = System.currentTimeMillis() - (4 * 24 * 60 * 60 * 1000), // 4 days ago
                isVerified = true,
                employerId = "emp_006",
                employerName = "Tech Solutions Pvt Ltd"
            )
        )
    }

    // Helper function to get jobs by employer
    fun getJobsByEmployer(employerId: String): List<JobPostingModel> {
        return getMyPostedJobs().filter { it.employerId == employerId }
    }

    // Helper function to get jobs by category
    fun getJobsByCategory(category: JobCategory): List<JobPostingModel> {
        return getMyPostedJobs().filter { it.category == category }
    }

    // Helper function to get recent jobs
    fun getRecentJobs(days: Int = 7): List<JobPostingModel> {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        return getMyPostedJobs().filter { it.postedTime >= cutoffTime }
    }

    // Helper function to get verified jobs
    fun getVerifiedJobs(): List<JobPostingModel> {
        return getMyPostedJobs().filter { it.isVerified }
    }
}
