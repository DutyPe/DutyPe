package com.example.dutype.employer.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.example.dutype.employer.viewmodels.VoiceJobPostingViewModel
import com.example.dutype.employer.viewmodels.VoiceStep
import com.example.dutype.utils.VoiceJobPostingHelper
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceJobPostingScreen(
    navController: NavController,
    viewModel: VoiceJobPostingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    
    val uiState by viewModel.uiState.collectAsState()
    val jobData by viewModel.jobData.collectAsState()
    
    var voiceHelper by remember { mutableStateOf<VoiceJobPostingHelper?>(null) }
    var isInitialized by remember { mutableStateOf(false) }
    
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = voiceHelper?.extractText(result.data)
            if (!text.isNullOrBlank()) {
                Timber.d("🎤 Voice input: $text")
                viewModel.processVoiceInput(text)
            }
        }
        viewModel.stopListening()
    }
    
    DisposableEffect(Unit) {
        voiceHelper = VoiceJobPostingHelper(activity) { initialized ->
            isInitialized = initialized
            if (initialized) {
                viewModel.startVoicePosting()
            }
        }
        onDispose {
            voiceHelper?.cleanup()
        }
    }
    
    LaunchedEffect(uiState.isSpeaking, uiState.currentStep) {
        if (uiState.isSpeaking && isInitialized) {
            val question = getQuestionForStep(uiState.currentStep, jobData)
            voiceHelper?.speak(question)
            kotlinx.coroutines.delay(question.length * 50L + 1000)
            viewModel.finishSpeaking()
            
            kotlinx.coroutines.delay(500)
            viewModel.startListening()
            voiceHelper?.startVoiceRecognition(
                launcher = voiceLauncher,
                prompt = "Speak your answer..."
            )
        }
    }
    
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
    
    if (uiState.successMessage != null) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            navController.popBackStack()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Premium gradient background
        PremiumGradientBackground(
            isListening = uiState.isListening,
            isSpeaking = uiState.isSpeaking
        )
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Voice Assistant",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    !isInitialized -> ModernLoadingState()
                    uiState.currentStep == VoiceStep.COMPLETED -> ModernSuccessState(uiState.successMessage ?: "Job posted!")
                    else -> PremiumConversationUI(
                        uiState = uiState,
                        jobData = jobData,
                        onMicClick = {
                            voiceHelper?.stopSpeaking()
                            viewModel.startListening()
                            voiceHelper?.startVoiceRecognition(
                                launcher = voiceLauncher,
                                prompt = "Speak your answer..."
                            )
                        },
                        onBackClick = { viewModel.goToPreviousStep() }
                    )
                }
                
                if (uiState.isPosting) {
                    ModernPostingOverlay()
                }
            }
        }
    }
}

@Composable
private fun PremiumGradientBackground(
    isListening: Boolean,
    isSpeaking: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    
    val colors = when {
        isListening -> listOf(
            Color(0xFFFF3B5C),
            Color(0xFFFF6B88),
            Color(0xFFFF8FA3)
        )
        isSpeaking -> listOf(
            Color(0xFF5B86E5),
            Color(0xFF36D1DC),
            Color(0xFF667EEA)
        )
        else -> listOf(
            Color(0xFF667EEA),
            Color(0xFF764BA2),
            Color(0xFFF093FB)
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = colors,
                    start = androidx.compose.ui.geometry.Offset(0f, offset),
                    end = androidx.compose.ui.geometry.Offset(1000f, offset + 1000f)
                )
            )
    )
}

@Composable
private fun ModernLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            "Initializing Voice Assistant...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = Color.White,
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun ModernSuccessState(message: String) {
    val scale = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        scope.launch {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(scale.value),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            // Main circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    tint = Color(0xFF4CAF50)
                )
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Success!",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun ModernPostingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "posting")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing)
                    ),
                    label = "rotation"
                )
                
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 6.dp,
                        color = Color(0xFF667EEA)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "Posting Your Job",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Please wait a moment...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun PremiumConversationUI(
    uiState: com.example.dutype.employer.viewmodels.VoiceJobPostingState,
    jobData: com.example.dutype.employer.viewmodels.VoiceJobData,
    onMicClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Sleek Progress
        SleekProgressIndicator(currentStep = uiState.currentStep)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Premium Voice Orb
        PremiumVoiceOrb(
            isListening = uiState.isListening,
            isSpeaking = uiState.isSpeaking,
            onMicClick = onMicClick
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Premium Question Card
            PremiumQuestionCard(
                question = getQuestionForStep(uiState.currentStep, jobData),
                step = uiState.currentStep
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Answer Preview
            SleekAnswerPreview(
                step = uiState.currentStep,
                jobData = jobData
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Job Summary for confirmation
            if (uiState.currentStep == VoiceStep.CONFIRMATION) {
                PremiumJobSummary(jobData = jobData)
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Error Banner
            uiState.errorMessage?.let { error ->
                SleekErrorBanner(message = error)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Modern Action Buttons
        PremiumActionButtons(
            currentStep = uiState.currentStep,
            isListening = uiState.isListening,
            isSpeaking = uiState.isSpeaking,
            isPosting = uiState.isPosting,
            onBackClick = onBackClick
        )
    }
}


@Composable
private fun SleekProgressIndicator(currentStep: VoiceStep) {
    val steps = listOf(
        VoiceStep.JOB_TITLE, VoiceStep.LOCATION, VoiceStep.SALARY,
        VoiceStep.DESCRIPTION, VoiceStep.CONTACT, VoiceStep.CONFIRMATION
    )
    
    val currentIndex = steps.indexOf(currentStep)
    val progress = if (currentIndex >= 0) (currentIndex + 1).toFloat() / steps.size else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step ${currentIndex + 1} of ${steps.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.25f)
            ) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun PremiumVoiceOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    
    // Pulsing animation
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Outer rings
        if (isListening || isSpeaking) {
            repeat(3) { index ->
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.4f + (index * 0.2f),
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 1500 + (index * 300),
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ring$index"
                )
                
                Box(
                    modifier = Modifier
                        .size((240 + index * 40).dp)
                        .scale(scale)
                        .alpha(0.15f - (index * 0.03f))
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
        
        // Main orb with clickable area
        Box(
            modifier = Modifier
                .size(200.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!isListening && !isSpeaking) {
                        onMicClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(if (isListening || isSpeaking) pulse else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // Main circle
            Surface(
                modifier = Modifier
                    .size(160.dp)
                    .scale(if (isListening || isSpeaking) pulse else 1f),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 24.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isListening -> Icons.Default.Mic
                            isSpeaking -> Icons.Default.VolumeUp
                            else -> Icons.Default.MicNone
                        },
                        contentDescription = null,
                        modifier = Modifier.size(70.dp),
                        tint = when {
                            isListening -> Color(0xFFFF3B5C)
                            isSpeaking -> Color(0xFF5B86E5)
                            else -> Color(0xFF667EEA)
                        }
                    )
                }
            }
        }
        
        // Status badge
        if (isListening || isSpeaking) {
            Box(
                modifier = Modifier
                    .offset(y = 130.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Color(0xFFFF3B5C) else Color(0xFF5B86E5)
                            )
                            .scale(pulse)
                    )
                    Text(
                        text = if (isListening) "Listening..." else "Speaking...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) Color(0xFFFF3B5C) else Color(0xFF5B86E5)
                    )
                }
            }
        }
    }
}


@Composable
private fun PremiumQuestionCard(question: String, step: VoiceStep) {
    val icon = when (step) {
        VoiceStep.JOB_TITLE -> Icons.Default.Work
        VoiceStep.LOCATION -> Icons.Default.LocationOn
        VoiceStep.SALARY -> Icons.Default.AttachMoney
        VoiceStep.DESCRIPTION -> Icons.Default.Description
        VoiceStep.CONTACT -> Icons.Default.Phone
        VoiceStep.CONFIRMATION -> Icons.Default.CheckCircle
        VoiceStep.COMPLETED -> Icons.Default.Done
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF667EEA),
                                    Color(0xFF764BA2)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
                Text(
                    text = "Question",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = question,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 28.sp,
                color = Color(0xFF4B5563),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SleekAnswerPreview(
    step: VoiceStep,
    jobData: com.example.dutype.employer.viewmodels.VoiceJobData
) {
    val answer = when (step) {
        VoiceStep.JOB_TITLE -> if (jobData.jobTitle.isNotBlank()) jobData.jobTitle else null
        VoiceStep.LOCATION -> if (jobData.location.isNotBlank()) jobData.location else null
        VoiceStep.SALARY -> if (jobData.payAmount.isNotBlank()) "₹${jobData.payAmount} ${jobData.payType.displayName}" else null
        VoiceStep.DESCRIPTION -> if (jobData.description.isNotBlank()) jobData.description else null
        VoiceStep.CONTACT -> if (jobData.contactPhone.isNotBlank()) jobData.contactPhone else null
        else -> null
    }
    
    AnimatedVisibility(
        visible = answer != null,
        enter = fadeIn() + slideInVertically() + expandVertically(),
        exit = fadeOut() + slideOutVertically() + shrinkVertically()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Answer",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = answer ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumJobSummary(jobData: com.example.dutype.employer.viewmodels.VoiceJobData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF10B981),
                                    Color(0xFF059669)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Summarize,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Job Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PremiumSummaryItem(Icons.Default.Work, "Job Title", jobData.jobTitle)
            PremiumSummaryItem(Icons.Default.LocationOn, "Location", jobData.location)
            PremiumSummaryItem(Icons.Default.AttachMoney, "Salary", "₹${jobData.payAmount} ${jobData.payType.displayName}")
            PremiumSummaryItem(Icons.Default.Description, "Description", jobData.description)
            PremiumSummaryItem(Icons.Default.Phone, "Contact", jobData.contactPhone, isLast = true)
        }
    }
}

@Composable
private fun PremiumSummaryItem(
    icon: ImageVector,
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF667EEA).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color(0xFF667EEA)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            }
        }
        if (!isLast) {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SleekErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun PremiumActionButtons(
    currentStep: VoiceStep,
    isListening: Boolean,
    isSpeaking: Boolean,
    isPosting: Boolean,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status indicator
        if (isListening || isSpeaking) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Color(0xFFFF3B5C) else Color(0xFF5B86E5)
                            )
                    )
                    
                    Text(
                        text = if (isListening) "Listening to your voice..." else "Speaking question...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) Color(0xFFFF3B5C) else Color(0xFF5B86E5)
                    )
                }
            }
        }
        
        // Back Button
        if (currentStep != VoiceStep.JOB_TITLE) {
            OutlinedButton(
                onClick = onBackClick,
                enabled = !isListening && !isSpeaking && !isPosting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(2.dp, Color.White),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Go Back",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Helper Text
        if (!isListening && !isSpeaking) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "Tap the microphone to speak",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun getQuestionForStep(
    step: VoiceStep,
    jobData: com.example.dutype.employer.viewmodels.VoiceJobData
): String {
    return when (step) {
        VoiceStep.JOB_TITLE -> "Hello! Let's post your job. What position are you hiring for? For example, you can say cook, delivery person, cleaner, waiter, or any other job."
        VoiceStep.LOCATION -> "Great! Now, where is this job located? Please tell me the area, locality, or city name."
        VoiceStep.SALARY -> "Perfect! What salary are you offering? Please mention the amount and say if it's per day, per month, or per hour."
        VoiceStep.DESCRIPTION -> "Excellent! Now tell me about the job. What are the main duties or requirements?"
        VoiceStep.CONTACT -> "Almost done! What is your contact number? Please say the 10-digit mobile number clearly."
        VoiceStep.CONFIRMATION -> buildString {
            append("Perfect! Let me read back your job details. ")
            append("Job title: ${jobData.jobTitle}. ")
            append("Location: ${jobData.location}. ")
            append("Salary: ${jobData.payAmount} rupees ${jobData.payType.displayName}. ")
            append("Description: ${jobData.description}. ")
            append("Contact number: ${jobData.contactPhone}. ")
            append("If everything is correct, please say YES to post this job. If you want to cancel, say NO.")
        }
        VoiceStep.COMPLETED -> "Wonderful! Your job has been posted successfully. Thank you!"
    }
}
