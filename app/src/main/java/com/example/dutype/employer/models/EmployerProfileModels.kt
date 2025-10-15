package com.example.dutype.employer.models

/**
 * Data classes for employer profile setup
 */

data class CompanyInfo(
    val companyName: String = "",
    val industry: String = "",
    val companySize: String = "",
    val website: String = "",
    val description: String = ""
)

data class BusinessDetails(
    val contactPersonName: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val businessAddress: String = "",
    val yearsInBusiness: String = ""
)

data class VerificationDetails(
    val businessRegistrationNumber: String = "",
    val gstNumber: String = "",
    val panNumber: String = "",
    val businessType: String = "",
    val additionalNotes: String = ""
)

data class EmployerProfileSetupUiState(
    val companyInfo: CompanyInfo = CompanyInfo(),
    val businessDetails: BusinessDetails = BusinessDetails(),
    val verificationDetails: VerificationDetails = VerificationDetails(),
    val currentStep: Int = 0,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null
)
