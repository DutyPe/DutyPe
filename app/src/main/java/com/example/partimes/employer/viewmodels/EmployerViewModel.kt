package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.partimes.models.Employer

class EmployerViewModel : ViewModel() {

    private val _employer = MutableStateFlow(Employer())
    val employer: StateFlow<Employer> = _employer

    fun updateEmployer(name: String, company: String, email: String) {
        _employer.value = Employer(name, company, email)
    }
}
