1.why to show the phone number availble from the user phone as bottom sheet cant you show directly in the keyboard --do in that way
2.:27:17.139 23044-23044 AndroidUtils            usap64                               E  mismatch between getCallingPackage com.dutype.app and getLaunchedFromPackage com.google.android.gms for activity com.google.android.gms.auth.api.credentials.assistedsignin.ui.PhoneNumberHintActivity [CONTEXT service_id=259 ]
2026-04-23 14:27:17.266 16815-16815 InsetsController        com.dutype.app                       D  show(ime(), fromIme=true)
2026-04-23 14:27:17.267 16815-16815 ImeTracker              com.dutype.app                       I  com.dutype.app:7ecd34b4: onCancelled at PHASE_CLIENT_APPLY_ANIMATION
2026-04-23 14:27:17.293 16815-16815 InsetsController        com.dutype.app                       D  show(ime(), fromIme=true)
2026-04-23 14:27:17.294 16815-16815 ImeTracker              com.dutype.app                       I  com.dutype.app:a5ee1c3e: onCancelled at PHASE_CLIENT_APPLY_ANIMATION
2026-04-23 14:27:17.345 16815-16815 WindowOnBackDispatcher  com.dutype.app                       W  sendCancelIfRunning: isInProgress=false callback=ImeCallback=ImeOnBackInvokedCallback@104973066 Callback=android.window.IOnBackInvokedCallback$Stub$Proxy@7e449d7
2026-04-23 14:27:17.347 16815-16815 InsetsController        com.dutype.app                       D  hide(ime(), fromIme=true)
2026-04-23 14:27:17.371 16815-16815 InsetsController        com.dutype.app                       D  hide(ime(), fromIme=true)
2026-04-23 14:27:17.372 16815-16815 ImeTracker              com.dutype.app                       I  com.google.android.gms:fa995400: onCancelled at PHASE_CLIENT_APPLY_ANIMATION
2026-04-23 14:27:17.388 16815-16815 ImeTracker              com.dutype.app                       I  com.google.android.inputmethod.latin:5f13bcd4: onCancelled at PHASE_CLIENT_ANIMATION_CANCEL
2026-04-23 14:27:17.391 16815-16815 ImeTracker              com.dutype.app                       I  com.dutype.app:8b672a92: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT_ON_ANIMATION_STATE_CHANGED fromUser false
2026-04-23 14:27:17.391 16815-16815 ImeTracker              com.dutype.app                       I  com.dutype.app:8b672a92: onFailed at PHASE_CLIENT_VIEW_SERVED
2026-04-23 14:27:18.150 16815-16815 MainActivity            com.dutype.app                       D  📱 MainActivity.onResume()
2026-04-23 14:27:18.155 16815-16815 InAppUpdateManager      com.dutype.app                       D  ℹ️ IN-APP UPDATE: Skipped (debug build)
2026-04-23 14:27:18.155 16815-16815 MainActivity$onResume   com.dutype.app                       D  ✅ App is up to date
2026-04-23 14:27:18.155 16815-16815 PlayCore                com.dutype.app                       I  UID: [10932]  PID: [16815] AppUpdateService : requestUpdateInfo(com.dutype.app)
2026-04-23 14:27:18.156 16815-26020 PlayCore                com.dutype.app                       I  UID: [10932]  PID: [16815] AppUpdateService : Initiate binding to the service.
2026-04-23 14:27:18.192 16815-16815 PlayCore                com.dutype.app                       I  UID: [10932]  PID: [16815] AppUpdateService : ServiceConnectionImpl.onServiceConnected(ComponentInfo{com.android.vending/com.google.android.finsky.installservice.DevTriggeredUpdateService})
2026-04-23 14:27:18.196 16815-26020 PlayCore                com.dutype.app                       I  UID: [10932]  PID: [16815] AppUpdateService : linkToDeath
2026-04-23 14:27:18.198 16815-16815 ImeFocusController      com.dutype.app                       V  onWindowFocus: androidx.compose.ui.platform.AndroidComposeView{3b6961f VFED..... .F...... 0,0-1080,2246 aid=1073741825} softInputMode=STATE_UNSPECIFIED|ADJUST_PAN
2026-04-23 14:27:18.213 16815-26020 PlayCore                com.dutype.app                       I  UID: [10932]  PID: [16815] AppUpdateService : Unbind from service.
2026-04-23 14:27:18.213 16815-25896 PlayCore                com.dutype.app                       I  UID: [10932]  PID: [16815] OnRequestInstallCallback : onRequestInfo
2026-04-23 14:27:18.970 16815-25886 com.dutype.app          com.dutype.app                       I  Compiler allocated 10MB to compile void com.example.dutype.auth.RegisterScreenKt.RegisterInputSection(java.lang.String, kotlin.jvm.functions.Function1, java.lang.String, kotlin.jvm.functions.Function1, java.lang.String, com.example.dutype.viewmodels.OtpState, boolean, boolean, kotlin.jvm.functions.Function1, com.example.dutype.viewmodels.ProfileCompletionViewModel, kotlin.jvm.functions.Function0, kotlin.jvm.functions.Function0, kotlin.jvm.functions.Function0, androidx.compose.runtime.Composer, int, int)
2026-04-23 14:27:21.058 16815-16815 RegisterScreenKt        com.dutype.app                       D  📱 Register - Continue clicked, name=VAMSI BANOTH 
2026-04-23 14:27:21.483 16815-26438 FirebaseContextProvider com.dutype.app                       W  Error getting App Check token. Error: com.google.firebase.FirebaseException: Error returned from API. code: 403 body: App attestation failed.
2026-04-23 14:27:21.485 16815-26438 CompatChangeReporter    com.dutype.app                       D  Compat change id reported: 270674727; UID 10932; state: ENABLED
2026-04-23 14:27:21.749 16815-16815 FirestoreUtils          com.dutype.app                       W  Callable lookupPhoneRole unavailable (Fix with AI)
                                                                                                    com.google.firebase.functions.FirebaseFunctionsException: NOT_FOUND
                                                                                                    	at com.google.firebase.functions.FirebaseFunctionsException$Companion.fromResponse$com_google_firebase_firebase_functions(FirebaseFunctionsException.kt:234)
                                                                                                    	at com.google.firebase.functions.FirebaseFunctions$call$5.onResponse(FirebaseFunctions.kt:273)
                                                                                                    	at okhttp3.internal.connection.RealCall$AsyncCall.run(RealCall.kt:519)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652)
                                                                                                    	at java.lang.Thread.run(Thread.java:1563)
2026-04-23 14:27:21.764 16815-26445 FirebaseContextProvider com.dutype.app                       W  Error getting App Check token. Error: com.google.firebase.FirebaseException: Too many attempts.
2026-04-23 14:27:21.831 16815-16815 FirestoreUtils          com.dutype.app                       W  Callable checkPhoneExists unavailable (Fix with AI)
                                                                                                    com.google.firebase.functions.FirebaseFunctionsException: NOT_FOUND
                                                                                                    	at com.google.firebase.functions.FirebaseFunctionsException$Companion.fromResponse$com_google_firebase_firebase_functions(FirebaseFunctionsException.kt:234)
                                                                                                    	at com.google.firebase.functions.FirebaseFunctions$call$5.onResponse(FirebaseFunctions.kt:273)
                                                                                                    	at okhttp3.internal.connection.RealCall$AsyncCall.run(RealCall.kt:519)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154)
                                                                                                    	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652)
                                                                                                    	at java.lang.Thread.run(Thread.java:1563)
2026-04-23 14:27:21.832 16815-16815 FirestoreUtils          com.dutype.app                       D  Phone existence fallback skipped for guest user (users query requires auth)
2026-04-23 14:27:21.849 16815-16815 RegisterSc...terContent com.dutype.app                       W  📱 REGISTER blocked - phone pre-check UNKNOWN: +919390693988
2026-04-23 14:27:46.660 16815-16815 ScrollIdentify          com.dutype.ap

when im registering or login please from the login screen and login bottom sheet 
it says above error and in the phne the toast message showing as the : taost message as the could not cerify this number right now and please trya again
3. sysatem bar colr top in the worker homescreen shoiuld matvch the dynamic header color whatever the color we cchanges -so now change the blue color of the dynamic heder color  to thegradinet purple color ,also update the worker homescren onlyto apply the purple matching color
4. apply for  a job screen should not show the section like the your anme , profle image, skills other things are sharing like these 
  val visibleItems = listOf(
                "Your name and profile photo",
                "Phone number (for contact)",
                "Skills and experience",
                "Cover letter (if provided)",
                "Your location"
            )
            remove it anyways we arre showing the profieldata sharing text under submit applcaition buttonn

5.Button(
            onClick = onReturnHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1F2937)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Return to Home",
                style = AppTypography.buttonMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
            not returnig saely to the worker homescreen when cliking the bitton

            6.package com.example.dutype.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated splash overlay shown on top of the app on cold start.
 *
 * Design:
 *  - Pure black background (matches the system splash so there is no flash
 *    between the system splash and this Compose splash).
 *  - "DutyPe" rendered letter-by-letter in white. Each letter fades in and
 *    slides up with a small stagger — a clean, premium fade-up reveal
 *    similar to Linear / Notion / Vercel.
 *  - System status-bar icons (battery, network, time) are forced to light /
 *    white while the splash is visible so they remain readable on the black
 *    background, then reverted on dispose.
 *  - The whole splash fades out after a short hold so the underlying app
 *    appears beneath it.
 */
@Composable
fun AnimatedSplashScreen(
    onAnimationEnd: () -> Unit
) {
    val word = "DutyPe"
    val letterAnims = remember { List(word.length) { Animatable(0f) } }
    var visible by remember { mutableStateOf(true) }

    // Force light (white) status- and nav-bar icons while the black splash is
    // showing, then restore the app's normal dark-on-white setup on dispose.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatus = controller?.isAppearanceLightStatusBars
        val previousLightNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            previousLightStatus?.let { controller.isAppearanceLightStatusBars = it }
            previousLightNav?.let { controller.isAppearanceLightNavigationBars = it }
        }
    }

    LaunchedEffect(Unit) {
        // Smooth premium reveal \u2014 slightly relaxed easing so it reads as
        // deliberate instead of "instant snap".
        val staggerMs = 55L
        val perLetterMs = 260
        letterAnims.forEachIndexed { index, anim ->
            launch {
                delay(index * staggerMs)
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = perLetterMs,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
        // Wait for the FULL word to finish revealing before holding/fading.
        val fullRevealMs = (letterAnims.lastIndex * staggerMs) + perLetterMs
        delay(fullRevealMs)
        // Very brief hold so the user perceives the complete word, then fade.
        delay(350L)
        visible = false
        // Match the AnimatedVisibility fade-out below before signalling completion.
        delay(180L)
        onAnimationEnd()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(0)),
        exit = fadeOut(animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                word.forEachIndexed { index, char ->
                    val progress = letterAnims[index].value
                    Text(
                        text = char.toString(),
                        color = Color.White,
                        fontSize = 47.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .alpha(progress)
                            .graphicsLayer {
                                // Pure fade-up: characters slide up into place, no scale.
                                translationY = (1f - progress) * 24.dp.toPx()
                            }
                    )
                }
            }
        }
    }
} splash screen should match exact this