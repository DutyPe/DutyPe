package com.example.partimes.models



data class Employer(
    val name: String = "",
    val company: String = "",
    val email: String = "",
    val professionalSkills: List<String> = emptyList(),
    val yearsOfExperience: Int = 0,
    val position: String = "",
    val companySize: String = "",
    val industry: String = "",
    val bio: String = "",
    val linkedInProfile: String = "",
    val phoneNumber: String = ""
)
