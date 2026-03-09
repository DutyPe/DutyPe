package com.example.dutype.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Extension function to get Activity from Context
 * Useful for Compose screens that need Activity reference
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
