package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.partimes.models.Employer

class EmployerViewModel : ViewModel() {

    private val _employer = MutableStateFlow(Employer())
    val employer: StateFlow<Employer> = _employer

    fun updateEmployer(
        name: String, 
        company: String, 
        email: String,
        professionalSkills: List<String> = emptyList(),
        yearsOfExperience: Int = 0,
        position: String = "",
        companySize: String = "",
        industry: String = "",
        bio: String = "",
        linkedInProfile: String = "",
        phoneNumber: String = ""
    ) {
        _employer.value = Employer(
            name = name,
            company = company,
            email = email,
            professionalSkills = professionalSkills,
            yearsOfExperience = yearsOfExperience,
            position = position,
            companySize = companySize,
            industry = industry,
            bio = bio,
            linkedInProfile = linkedInProfile,
            phoneNumber = phoneNumber
        )
    }
    
    fun addProfessionalSkill(skill: String) {
        val currentSkills = _employer.value.professionalSkills.toMutableList()
        if (skill.isNotBlank() && !currentSkills.contains(skill)) {
            currentSkills.add(skill)
            _employer.value = _employer.value.copy(professionalSkills = currentSkills)
        }
    }
    
    fun removeProfessionalSkill(skill: String) {
        val currentSkills = _employer.value.professionalSkills.toMutableList()
        currentSkills.remove(skill)
        _employer.value = _employer.value.copy(professionalSkills = currentSkills)
    }
}
