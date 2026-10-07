package com.example.dutype.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.dutype.app.R
import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.fg
import com.example.dutype.utils.findActivity
import com.truecaller.android.sdk.oAuth.CodeVerifierUtil
import com.truecaller.android.sdk.oAuth.TcOAuthCallback
import com.truecaller.android.sdk.oAuth.TcOAuthData
import com.truecaller.android.sdk.oAuth.TcOAuthError
import com.truecaller.android.sdk.oAuth.TcSdk
import com.truecaller.android.sdk.oAuth.TcSdkOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID

/**
 * Truecaller one-tap login (Truecaller OAuth SDK 3.2.x). Free: the number is verified by
 * Truecaller, so no SMS is sent.
 *
 * The app only gets an authorization code here; the server (`truecallerSignIn`) exchanges it with
 * the PKCE code verifier for the verified number, name and email, and returns a Firebase sign-in
 * token. Only shown when the Truecaller app is installed and signed in, and when
 * `truecaller_client_id` is set (res/values/truecaller.xml); otherwise the screens look as before.
 *
 * The SDK needs a FragmentActivity, so the consent sheet runs from the small transparent
 * [TruecallerActivity] instead of MainActivity.
 */
object TruecallerAuth {
    sealed interface Event {
        data class Authorized(val code: String, val state: String?) : Event
        data class Failed(val errorCode: Int, val message: String?, val cancelled: Boolean = false) : Event
    }

    /** One login attempt in progress: the verifier the server needs and the state to match. */
    data class Pending(val codeVerifier: String, val state: String)

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val _usable = MutableStateFlow(false)
    /** True when the SDK started and Truecaller is installed and signed in on this phone. */
    val usable: StateFlow<Boolean> = _usable.asStateFlow()

    @Volatile private var started = false

    private val callback = object : TcOAuthCallback {
        override fun onSuccess(tcOAuthData: TcOAuthData) {
            val code = tcOAuthData.authorizationCode.orEmpty()
            _events.tryEmit(if (code.isNotBlank()) Event.Authorized(code, tcOAuthData.state) else Event.Failed(-1, "empty code"))
        }

        override fun onFailure(tcOAuthError: TcOAuthError) {
            // Closing the sheet or picking "use another method" is the user's choice, not an error.
            val cancelled = tcOAuthError is TcOAuthError.UserDeniedError ||
                tcOAuthError is TcOAuthError.UserDeniedByPressingFooterError ||
                tcOAuthError is TcOAuthError.UserDeniedWhileLoadingError ||
                tcOAuthError is TcOAuthError.TruecallerClosedError
            _events.tryEmit(Event.Failed(tcOAuthError.errorCode, tcOAuthError.errorMessage, cancelled))
        }

        override fun onVerificationRequired(tcOAuthError: TcOAuthError?) {
            // Only Truecaller users are verified here (OPTION_VERIFY_ONLY_TC_USERS); others use OTP.
            _events.tryEmit(Event.Failed(tcOAuthError?.errorCode ?: -1, tcOAuthError?.errorMessage))
        }
    }

    /** Starts the SDK once; harmless to call again. Does nothing without a client ID. */
    fun init(context: Context) {
        if (started) return
        val clientId = context.getString(R.string.truecaller_client_id)
        if (clientId.isBlank()) {
            Timber.w("Truecaller client ID is blank; button will remain hidden")
            return
        }
        started = true
        runCatching {
            val options = TcSdkOptions.Builder(context.applicationContext, callback)
                .buttonColor(android.graphics.Color.parseColor("#0F0F0F"))
                .buttonTextColor(android.graphics.Color.WHITE)
                .loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_GET_STARTED)
                .ctaText(TcSdkOptions.CTA_TEXT_CONTINUE)
                .buttonShapeOptions(TcSdkOptions.BUTTON_SHAPE_ROUNDED)
                .footerType(TcSdkOptions.FOOTER_TYPE_ANOTHER_METHOD)
                .consentHeadingOption(TcSdkOptions.SDK_CONSENT_HEADING_LOG_IN_TO)
                .sdkOptions(TcSdkOptions.OPTION_VERIFY_ONLY_TC_USERS)
                .build()
            TcSdk.init(options)
            val usable = TcSdk.getInstance().isOAuthFlowUsable
            _usable.value = usable
            Timber.i("Truecaller SDK initialized. clientId=$clientId, isOAuthFlowUsable=$usable")
        }.onFailure {
            started = false
            _usable.value = false
            Timber.w(it, "Truecaller SDK init failed")
        }
    }

    /** Prepares a login (PKCE challenge, state, scopes). Null when Truecaller cannot be used now. */
    fun begin(): Pending? {
        if (!_usable.value) return null
        return runCatching {
            val sdk = TcSdk.getInstance()
            val verifier = CodeVerifierUtil.generateRandomCodeVerifier()
            val challenge = CodeVerifierUtil.getCodeChallenge(verifier) ?: return null
            val state = UUID.randomUUID().toString()
            sdk.setOAuthState(state)
            sdk.setOAuthScopes(arrayOf("openid", "profile", "phone", "email"))
            sdk.setCodeChallenge(challenge)
            Pending(verifier, state)
        }.onFailure { Timber.w(it, "Truecaller begin failed") }.getOrNull()
    }

    internal fun failed(message: String) {
        _events.tryEmit(Event.Failed(-1, message))
    }
}

/**
 * Transparent screen that shows Truecaller's consent sheet (the SDK requires a FragmentActivity)
 * and closes itself; the result reaches the login screen through [TruecallerAuth.events].
 */
class TruecallerActivity : FragmentActivity() {
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        runCatching { TcSdk.getInstance().onActivityResultObtained(this, result.resultCode, result.data) }
            .onFailure {
                Timber.w(it, "Truecaller result failed")
                TruecallerAuth.failed(it.message ?: "result failed")
            }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return // recreated while the sheet is open; the launcher still delivers
        runCatching { TcSdk.getInstance().getAuthorizationCode(this, launcher) }
            .onFailure {
                Timber.w(it, "Truecaller start failed")
                TruecallerAuth.failed(it.message ?: "start failed")
                finish()
            }
    }
}

/**
 * "Continue with Truecaller" button. Renders nothing unless Truecaller can be used on this phone.
 * [onAuthorized] gets the authorization code and the PKCE verifier for the server exchange.
 */
@Composable
fun TruecallerLoginButton(
    busy: Boolean,
    onAuthorized: (code: String, codeVerifier: String) -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { TruecallerAuth.init(context) }
    val usable by TruecallerAuth.usable.collectAsState()
    var pending by remember { mutableStateOf<TruecallerAuth.Pending?>(null) }
    val authorized by rememberUpdatedState(onAuthorized)
    val failed by rememberUpdatedState(onFailed)

    LaunchedEffect(Unit) {
        TruecallerAuth.events.collect { event ->
            val current = pending ?: return@collect
            when (event) {
                is TruecallerAuth.Event.Authorized -> {
                    pending = null
                    if (event.state == null || event.state == current.state) {
                        authorized(event.code, current.codeVerifier)
                    } else {
                        Timber.w("Truecaller state mismatch")
                        failed()
                    }
                }
                is TruecallerAuth.Event.Failed -> {
                    pending = null
                    Timber.i("Truecaller not completed: ${event.errorCode} ${event.message}")
                    if (!event.cancelled) failed()
                }
            }
        }
    }

    val clientId = remember(context) { context.getString(R.string.truecaller_client_id) }
    if (clientId.isBlank()) return

    OutlinedButton(
        onClick = {
            if (!usable) {
                val isInstalled = runCatching {
                    context.packageManager.getPackageInfo("com.truecaller", 0) != null
                }.getOrDefault(false)
                if (!isInstalled) {
                    Toast.makeText(context, "Truecaller app is not installed on this device. Please install Truecaller or use Mobile OTP.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Please ensure you are signed in to the Truecaller app, or use Mobile OTP.", Toast.LENGTH_LONG).show()
                }
                return@OutlinedButton
            }
            val started = TruecallerAuth.begin()
            pending = started
            if (started == null) {
                failed()
            } else {
                val activity = context.findActivity()
                val intent = Intent(context, TruecallerActivity::class.java)
                if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                (activity ?: context).startActivity(intent)
            }
        },
        enabled = enabled && !busy,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF0087FF).bd())
    ) {
        if (busy) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp), color = Color(0xFF0087FF))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                androidx.compose.ui.res.painterResource(id = R.drawable.ic_truecaller).let { iconPainter ->
                    Icon(
                        painter = iconPainter,
                        contentDescription = "Truecaller",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.auth_continue_with_truecaller),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0087FF).fg()
                    )
                )
            }
        }
    }
}
