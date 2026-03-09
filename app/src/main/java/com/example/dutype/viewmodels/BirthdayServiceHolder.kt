package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import com.example.dutype.services.BirthdayService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel wrapper to inject BirthdayService into Compose screens
 * This prevents inline service instantiation which causes performance issues
 */
@HiltViewModel
class BirthdayServiceHolder @Inject constructor(
    val service: BirthdayService
) : ViewModel()
