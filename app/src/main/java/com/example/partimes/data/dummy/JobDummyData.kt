package com.example.partimes.data.dummy

import com.example.partimes.models.JobListing
import com.example.partimes.models.AppliedJob
import com.example.partimes.models.ApplicationStatus
import com.example.partimes.R

val jobListings = listOf(
    // Hourly
    JobListing(
        jobId = "job1",
        employerId = "employer1",
        title = "Delivery Partner (2 hrs)",
        company = "Zomato",
        locationNearby = "2 km away",
        specificLocation = "SR Nagar",
        wage = "₹130/hour",
        timing = "2pm - 4pm",
        description = "Deliver food in your area. Bike required.",
        preferences = listOf("Immediate", "Evening shift", "Female preferred", "Experience preferred"),
        isTrending = true,
        imageUrl = "", // Will dynamically load category image based on title
        vacancies = 5,
        phoneNumber = "9390693988",
        isActive = true,
        postedAt = System.currentTimeMillis()
    ),

    // Daily
    JobListing(
        jobId = "job2",
        employerId = "employer2",
        title = "Community Engagement Specialist & Dentist Assist",
        company = "UrbanCo",
        locationNearby = "3 km away",
        specificLocation = "KPHB Colony",
        wage = "₹600/day",
        timing = "4pm - 5pm",
        description = "Assist dentist in local clinics. Great for freshers looking to explore.",
        preferences = listOf("Only 2 spots left", "Women Only"),
        isTrending = false,
        imageUrl = "",
        vacancies = 2,
        phoneNumber = "9611998185",
        isActive = true,
        postedAt = System.currentTimeMillis()
    ),

    // Poster Pasting
    JobListing(
        jobId = "job3",
        employerId = "employer3",
        title = "Poster Pasting",
        company = "Local Agency",
        locationNearby = "4 km away",
        specificLocation = "Ameerpet",
        wage = "₹25/poster",
        timing = "Flexible",
        description = "Paste posters at colleges or street sides. Proof required.",
        preferences = listOf("Urgent", "No experience"),
        isTrending = false,
        imageUrl = "",
        vacancies = 10,
        phoneNumber = "9390693988",
        isActive = true,
        postedAt = System.currentTimeMillis()
    ),

    // Part Time
    JobListing(
        jobId = "job4",
        employerId = "employer4",
        title = "Retail Associate (Part-time)",
        company = "Reliance Trends",
        locationNearby = "2.5 km away",
        specificLocation = "Kukatpally",
        wage = "₹2500/weekend",
        timing = "Sat-Sun",
        description = "Assist customers in selecting clothes and handling the cash register.",
        preferences = listOf("Student friendly", "No uniform"),
        isTrending = true,
        imageUrl = "",
        vacancies = 3,
        phoneNumber = "9390693988",
        isActive = true,
        postedAt = System.currentTimeMillis()
    ),

    // Per Task
    JobListing(
        jobId = "job5",
        employerId = "employer5",
        title = "Leaflet Distributor (Per Task)",
        company = "Local Campaign",
        locationNearby = "Nearby",
        specificLocation = "Metro Stations",
        wage = "₹5/leaflet",
        timing = "Any time (Flexible)",
        description = "Distribute flyers at metro stations. Flexible timing. Submit proof online.",
        preferences = listOf("Immediate start", "Part-time possible"),
        isTrending = false,
        imageUrl = "",
        vacancies = 15,
        phoneNumber = "9390693988",
        isActive = true,
        postedAt = System.currentTimeMillis()
    ),

    // Monthly Full-Time
    JobListing(
        jobId = "job6",
        employerId = "employer6",
        title = "Store Assistant",
        company = "Dmart",
        locationNearby = "1.8 km away",
        specificLocation = "Moosapet",
        wage = "₹12,000/month",
        timing = "9am - 6pm",
        description = "Manage inventory and assist customers at the counter.",
        preferences = listOf("Experience preferred", "Male only"),
        isTrending = false,
        imageUrl = "",
        vacancies = 4,
        phoneNumber = "9390693988",
        isActive = true,
        postedAt = System.currentTimeMillis()
    )
)

// Applied Jobs Dummy Data
val dummyAppliedJobs = listOf(
    AppliedJob(
        jobListing = JobListing(
            jobId = "applied_job1",
            employerId = "employer_retail",
            title = "Retail Associate (Part-time)",
            company = "Reliance Trends",
            locationNearby = "Kukatpally",
            specificLocation = "Forum Mall",
            wage = "₹2500/weekend",
            timing = "Weekends, 10 AM - 6 PM",
            isTrending = true,
            vacancies = 5,
            imageUrl = R.drawable.delivery.toString(),
            description = "Assist customers, manage inventory, and ensure smooth billing operations.",
            preferences = listOf("Good communication", "Punctuality"),
            phoneNumber = "9390693988"
        ),
        status = ApplicationStatus.SELECTED,
        lastUpdated = System.currentTimeMillis() - 2 * 60 * 60 * 1000L,
        employerMessage = "Congratulations! You've been selected. Please report on Monday at 10 AM."
    ),
    AppliedJob(
        jobListing = JobListing(
            jobId = "applied_job2",
            employerId = "employer_delivery",
            title = "Food Delivery Partner",
            company = "Swiggy",
            locationNearby = "Madhapur",
            specificLocation = "HITEC City",
            wage = "₹25/delivery",
            timing = "Flexible hours, 11 AM - 10 PM",
            isTrending = false,
            vacancies = 20,
            imageUrl = R.drawable.delivery.toString(),
            description = "Deliver food orders to customers in your area.",
            preferences = listOf("Own vehicle", "Smartphone"),
            phoneNumber = "9876543210"
        ),
        status = ApplicationStatus.INTERVIEWING,
        lastUpdated = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L,
        employerMessage = "Your interview is scheduled for tomorrow at 2 PM. Please bring your documents."
    ),
    AppliedJob(
        jobListing = JobListing(
            jobId = "applied_job3",
            employerId = "employer_support",
            title = "Customer Support Executive",
            company = "Amazon",
            locationNearby = "Gachibowli",
            specificLocation = "Waverock Building",
            wage = "₹3500/weekend",
            timing = "Weekends only, 9 AM - 5 PM",
            isTrending = true,
            vacancies = 8,
            imageUrl = R.drawable.delivery.toString(),
            description = "Handle customer queries via phone and chat support.",
            preferences = listOf("Good English", "Problem solving"),
            phoneNumber = "9123456789"
        ),
        status = ApplicationStatus.PENDING,
        lastUpdated = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L,
        employerMessage = null
    ),
    AppliedJob(
        jobListing = JobListing(
            jobId = "applied_job4",
            employerId = "employer_data",
            title = "Data Entry Operator",
            company = "TCS",
            locationNearby = "Kondapur",
            specificLocation = "DLF Cyber City",
            wage = "₹2000/weekend",
            timing = "Saturday & Sunday, 10 AM - 6 PM",
            isTrending = false,
            vacancies = 3,
            imageUrl = R.drawable.delivery.toString(),
            description = "Enter and manage data in company systems.",
            preferences = listOf("Computer skills", "Attention to detail"),
            phoneNumber = "9988776655"
        ),
        status = ApplicationStatus.REJECTED,
        lastUpdated = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L,
        employerMessage = "Thank you for your interest. We have selected other candidates for this position."
    ),
    AppliedJob(
        jobListing = JobListing(
            jobId = "applied_job5",
            employerId = "employer_intern",
            title = "Marketing Intern",
            company = "Flipkart",
            locationNearby = "Kondapur",
            specificLocation = "Salarpuria Sattva",
            wage = "₹1500/weekend",
            timing = "Saturdays, 10 AM - 4 PM",
            isTrending = true,
            vacancies = 2,
            imageUrl = R.drawable.delivery.toString(),
            description = "Assist marketing team with campaigns and social media.",
            preferences = listOf("Creative thinking", "Social media savvy"),
            phoneNumber = "9876501234"
        ),
        status = ApplicationStatus.SHORTLISTED,
        lastUpdated = System.currentTimeMillis() - 1 * 60 * 60 * 1000L,
        employerMessage = "You've been shortlisted! We'll contact you soon for the next round."
    ),
    AppliedJob(
        jobListing = JobListing(
            jobId = "applied_job6",
            employerId = "employer_tutor",
            title = "Home Tutor",
            company = "EduCare Services",
            locationNearby = "Miyapur",
            specificLocation = "Miyapur X Roads",
            wage = "₹500/hour",
            timing = "Evenings, 6 PM - 8 PM",
            isTrending = false,
            vacancies = 1,
            imageUrl = R.drawable.delivery.toString(),
            description = "Teach mathematics to 10th grade students.",
            preferences = listOf("Good at math", "Patient with kids"),
            phoneNumber = "9123450987"
        ),
        status = ApplicationStatus.INTERVIEW_SCHEDULED,
        lastUpdated = System.currentTimeMillis() - 4 * 60 * 60 * 1000L,
        employerMessage = "Interview scheduled for this Saturday at 3 PM. Please bring your certificates."
    ),
    AppliedJob(
        jobListing = JobListing(
            jobId = "applied_job7",
            employerId = "employer_office",
            title = "Office Assistant",
            company = "Tech Solutions Ltd",
            locationNearby = "Begumpet",
            specificLocation = "Greenlands",
            wage = "₹2200/weekend",
            timing = "Saturday & Sunday, 9 AM - 5 PM",
            isTrending = false,
            vacancies = 4,
            imageUrl = R.drawable.delivery.toString(),
            description = "General office work including filing, data entry, and phone handling.",
            preferences = listOf("Basic computer skills", "Good communication"),
            phoneNumber = "9988123456"
        ),
        status = ApplicationStatus.DOCUMENTS_PENDING,
        lastUpdated = System.currentTimeMillis() - 6 * 60 * 60 * 1000L,
        employerMessage = "Please submit your ID proof and address proof to complete your application."
    ),
    AppliedJob(
        jobListing = JobListing(
            jobId = "applied_job8",
            employerId = "employer_event",
            title = "Event Staff",
            company = "EventPro",
            locationNearby = "Banjara Hills",
            specificLocation = "Road No. 12",
            wage = "₹1000/event",
            timing = "Weekend events, 4-8 hours",
            isTrending = true,
            vacancies = 0,
            imageUrl = R.drawable.delivery.toString(),
            description = "Help with event setup, guest management, and coordination.",
            preferences = listOf("Presentable", "Good communication"),
            phoneNumber = "9876509876"
        ),
        status = ApplicationStatus.VACANCY_FILLED,
        lastUpdated = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L,
        employerMessage = "Thank you for your interest. All positions have been filled for this event."
    )
)
