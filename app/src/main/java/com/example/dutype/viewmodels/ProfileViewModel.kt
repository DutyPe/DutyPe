package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.profile.CurrentProfileStore
import com.example.dutype.profile.WorkerProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** The signed-in worker's own profile, live from the shared [CurrentProfileStore] (no extra reads). */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileStore: CurrentProfileStore
) : ViewModel() {

    val worker: StateFlow<WorkerProfile?> = profileStore.worker

    fun loadProfile() = profileStore.start(Values.Role.WORKER)
}
