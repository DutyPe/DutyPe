package com.example.dutype.models

import androidx.annotation.Keep

@Keep
data class PromoShowcaseButton(
    val label: String = "Contact",
    val icon: String = "call", // call, whatsapp, maps, url
    val actionUrl: String = "", // tel:+91..., https://wa.me/..., https://maps.google.com/..., https://...
    val isPrimary: Boolean = false
)

@Keep
data class PromoShowcase(
    val id: String = "",
    val title: String = "",
    val tagline: String = "",
    val categoryBadge: String = "",
    val rating: String = "4.8",
    val heroImageUrl: String = "",
    val videoUrl: String = "",
    val aboutDescription: String = "",
    val address: String = "",
    val mapLink: String = "",
    val discountOfferTitle: String = "",
    val discountOfferCode: String = "",
    val discountOfferTerms: String = "",
    val highlights: List<String> = emptyList(),
    val galleryImages: List<String> = emptyList(),
    val actionButtons: List<PromoShowcaseButton> = emptyList(),
    val district: String = "",
    val scope: String = "DISTRICT",
    val active: Boolean = true
)
