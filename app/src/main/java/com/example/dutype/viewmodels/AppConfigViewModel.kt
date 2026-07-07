package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import com.example.dutype.repositories.AppConfigRepository
import com.example.dutype.repositories.AppUpdateConfig
import com.example.dutype.repositories.ReferralConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Lightweight ViewModel that exposes the admin-editable referral config
 * to screens that don't need the full ReferralViewModel (e.g. login,
 * register, onboarding bottom sheets).
 */
@HiltViewModel
class AppConfigViewModel @Inject constructor(
    appConfigRepository: AppConfigRepository
) : ViewModel() {
    val referralConfig: StateFlow<ReferralConfig> = appConfigRepository.referralConfig
    val appUpdateConfig: StateFlow<AppUpdateConfig> = appConfigRepository.appUpdateConfig
    val dynamicFeaturesConfig: StateFlow<com.example.dutype.repositories.DynamicFeaturesConfig> = appConfigRepository.dynamicFeaturesConfig
}
