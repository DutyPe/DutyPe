package com.example.dutype.di

import android.content.Context
import com.example.dutype.repositories.LocationRepository
import com.example.dutype.services.ReferralService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Firebase access for call sites that can't use constructor injection
 * (Composables, static helpers, legacy singletons).
 *
 * This is NOT a shortcut for services — services must use constructor @Inject.
 * This accessor exists so that Composables and similar call sites resolve the
 * Hilt-configured singletons (with settings, App Check, etc. applied by
 * [AppModule]) instead of calling FirebaseX.getInstance() directly, which can
 * race provider initialization and bypass configured settings like
 * offline persistence.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FirebaseEntryPoint {
    fun firestore(): FirebaseFirestore
    fun auth(): FirebaseAuth
    fun storage(): FirebaseStorage
    fun functions(): FirebaseFunctions
    fun referralService(): ReferralService
    fun locationRepository(): LocationRepository
}

/** Resolve the Hilt-provided [FirebaseFirestore] singleton from a Compose context. */
fun firestoreFromHilt(context: Context): FirebaseFirestore =
    EntryPointAccessors
        .fromApplication(context.applicationContext, FirebaseEntryPoint::class.java)
        .firestore()

/** Resolve the Hilt-provided [FirebaseAuth] singleton from a Compose context. */
fun authFromHilt(context: Context): FirebaseAuth =
    EntryPointAccessors
        .fromApplication(context.applicationContext, FirebaseEntryPoint::class.java)
        .auth()

/** Resolve the Hilt-provided [ReferralService] singleton from a Compose context. */
fun referralServiceFromHilt(context: Context): ReferralService =
    EntryPointAccessors
        .fromApplication(context.applicationContext, FirebaseEntryPoint::class.java)
        .referralService()

/** Resolve the Hilt-provided [LocationRepository] singleton from a Compose context. */
fun locationRepositoryFromHilt(context: Context): LocationRepository =
    EntryPointAccessors
        .fromApplication(context.applicationContext, FirebaseEntryPoint::class.java)
        .locationRepository()
