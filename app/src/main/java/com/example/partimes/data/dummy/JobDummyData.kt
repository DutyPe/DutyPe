package com.example.partimes.data.dummy

import com.example.partimes.models.JobListing

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
