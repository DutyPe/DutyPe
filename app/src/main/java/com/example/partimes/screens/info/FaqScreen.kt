package com.example.partimes.screens.info

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.components.BackNavigationTopBar

@Composable
fun FaqScreen(navController: NavController) {
    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "FAQs", navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            FaqItem("Do I need a resume?", "No! You can apply with a simple profile, voice intro, and your skills.")
            FaqItem("Are the jobs verified?", "We try to verify all job posts, but always contact the employer to confirm details.")
            FaqItem("Is this app free?", "Yes, using the app and applying to jobs is 100% free for job seekers.")
            FaqItem("How do I contact employers?", "You can chat or send voice messages directly in the app after applying.")
            FaqItem("What kind of jobs are available?", "You’ll find part-time work like cooking, cleaning, delivery, shop help, and more.")
        }
    }
}


@Composable
fun FaqItem(question: String, answer: String) {
    Text(
        text = "Q: $question",
        fontSize = 16.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp)
    )
    Text(
        text = "A: $answer",
        fontSize = 15.sp,
        lineHeight = 22.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
