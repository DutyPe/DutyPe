package com.example.dutype

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage

class LocalStagingFirebaseProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val app = FirebaseApp.initializeApp(requireNotNull(context)) ?: error("Local Firebase configuration is missing")
        check(app.options.projectId == "demo-dutype-android-fixes") { "Staging must not use a real Firebase project" }
        app.setDataCollectionDefaultEnabled(false)
        FirebaseAuth.getInstance(app).useEmulator("10.0.2.2", 9199)
        FirebaseFirestore.getInstance(app).apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            useEmulator("10.0.2.2", 8185)
        }
        FirebaseFunctions.getInstance(app).useEmulator("10.0.2.2", 5101)
        FirebaseStorage.getInstance(app).useEmulator("10.0.2.2", 9299)
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}