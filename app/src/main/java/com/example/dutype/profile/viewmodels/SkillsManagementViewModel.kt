package com.example.dutype.profile.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.profile.models.Skill
import com.example.dutype.profile.services.SkillsManagementService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillsManagementViewModel @Inject constructor(
    private val skillsService: SkillsManagementService
) : ViewModel() {
    
    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        // Load skills safely - errors are handled in loadSkills()
        try {
            loadSkills()
        } catch (e: Exception) {
            // Handle any initialization errors gracefully
            _error.value = "Failed to initialize skills: ${e.message}"
            _isLoading.value = false
        }
    }
    
    fun loadSkills() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val userSkills = skillsService.getUserSkills("current_user_id").first()
                _skills.value = userSkills
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load skills"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addSkill(skill: Skill) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                skillsService.addSkill("current_user_id", skill)
                loadSkills() // Refresh the list
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add skill"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun editSkill(skill: Skill) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                skillsService.updateSkill("current_user_id", skill)
                loadSkills() // Refresh the list
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update skill"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteSkill(skillId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                skillsService.removeSkill("current_user_id", skillId)
                loadSkills() // Refresh the list
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete skill"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
