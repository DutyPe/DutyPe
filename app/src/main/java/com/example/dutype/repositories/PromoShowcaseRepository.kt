package com.example.dutype.repositories

import com.example.dutype.models.PromoShowcase
import com.example.dutype.models.PromoShowcaseButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromoShowcaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getShowcasesFlow(): Flow<List<PromoShowcase>> = callbackFlow {
        val listener = firestore.collection("promo_showcases")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.w(error, "promo_showcases listener failed")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    parseShowcase(doc)
                }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getShowcase(showcaseId: String): PromoShowcase? {
        return runCatching {
            val targetId = showcaseId.trim()
            if (targetId.isBlank() || targetId.equals("active_campaign", ignoreCase = true) || targetId.equals("dynamic_campaign", ignoreCase = true)) {
                val doc = firestore.collection("promo_showcases").document("active_campaign").get().await()
                if (doc.exists()) {
                    parseShowcase(doc)
                } else {
                    val list = firestore.collection("promo_showcases").whereEqualTo("active", true).limit(1).get().await()
                    val fallbackDoc = list.documents.firstOrNull()
                    if (fallbackDoc != null) parseShowcase(fallbackDoc) else null
                }
            } else {
                val doc = firestore.collection("promo_showcases").document(targetId).get().await()
                if (doc.exists()) {
                    parseShowcase(doc)
                } else {
                    val list = firestore.collection("promo_showcases").whereEqualTo("active", true).limit(1).get().await()
                    val fallbackDoc = list.documents.firstOrNull()
                    if (fallbackDoc != null) parseShowcase(fallbackDoc) else null
                }
            }
        }.getOrNull()
    }

    private fun parseShowcase(doc: DocumentSnapshot): PromoShowcase? {
        if (!doc.exists()) return null
        val data = doc.data.orEmpty()
        @Suppress("UNCHECKED_CAST")
        val rawButtons = (data["actionButtons"] as? List<*>)?.mapNotNull { b ->
            val bm = b as? Map<*, *> ?: return@mapNotNull null
            PromoShowcaseButton(
                label = (bm["label"] as? String) ?: "Contact",
                icon = (bm["icon"] as? String) ?: "call",
                actionUrl = (bm["actionUrl"] as? String) ?: "",
                isPrimary = bm["isPrimary"] == true
            )
        } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val rawHighlights = (data["highlights"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val rawGallery = (data["galleryImages"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

        return PromoShowcase(
            id = doc.id,
            title = (data["title"] as? String).orEmpty(),
            tagline = (data["tagline"] as? String).orEmpty(),
            categoryBadge = (data["categoryBadge"] as? String).orEmpty(),
            rating = (data["rating"] as? String) ?: "4.8",
            heroImageUrl = (data["heroImageUrl"] as? String).orEmpty(),
            videoUrl = (data["videoUrl"] as? String).orEmpty(),
            aboutDescription = (data["aboutDescription"] as? String).orEmpty(),
            address = (data["address"] as? String).orEmpty(),
            mapLink = (data["mapLink"] as? String).orEmpty(),
            discountOfferTitle = (data["discountOfferTitle"] as? String).orEmpty(),
            discountOfferCode = (data["discountOfferCode"] as? String).orEmpty(),
            discountOfferTerms = (data["discountOfferTerms"] as? String).orEmpty(),
            highlights = rawHighlights,
            galleryImages = rawGallery,
            actionButtons = rawButtons,
            district = (data["district"] as? String).orEmpty(),
            scope = (data["scope"] as? String) ?: "DISTRICT",
            active = (data["active"] as? Boolean) ?: true
        )
    }
}
