import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

// ✅ Shared Dummy Jobs List

@Composable
fun JobHomeScreen(rootNavController: NavHostController, onScroll: (Boolean) -> Unit) {
    val context = LocalContext.current
//    LaunchedEffect(Unit) {
//        RewardedAdManager.loadAd(context)
//    }

//    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedShopName by remember { mutableStateOf("") }
    val searchQuery = remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var previousOffset by remember { mutableStateOf(0) }

    LaunchedEffect(listState.firstVisibleItemScrollOffset) {
        val currentOffset = listState.firstVisibleItemScrollOffset
        onScroll(currentOffset > previousOffset)
        previousOffset = currentOffset
    }
    Column(modifier = Modifier.fillMaxSize()) {
        // Title
        Text(
            text = "Local Jobs",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 3.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery.value,
            onValueChange = { searchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 43.dp),
            label = { Text("Search by Job Title") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Expandable Map
//        GoogleMapsCard(
//            context = context,
//            selectedLocation = selectedLocation,
//            selectedShopName = selectedShopName,
////            jobTitle = dummyJobs.find { it.actualLocation == selectedLocation }?.shopName ?: "",
//            vacancies = dummyJobs.find { it.actualLocation == selectedLocation }?.vacancies?.toString() ?: "",
//            modifier = Modifier.fillMaxWidth() // ✅ No side padding
//        )


//        BannerAdView()

        // Job List
//        LazyColumn(state = listState) {
//            itemsIndexed(dummyJobs.filter {
//                it.title.contains(searchQuery.value, ignoreCase = true)
//            }) { index, job ->
//                val isFirst = index == 0
//
//                // Side-effect to auto-select the first job's location
//                LaunchedEffect(key1 = isFirst) {
//                    if (isFirst) {
////                        selectedLocation = job.actualLocation
//                        selectedShopName = job.shopName
//                    }
//                }
//
//                CustomJobCard(
//                    job = job,
//                    index = index,
//                    isFirstCard = isFirst,
////                    onJobSelected = {
////                        selectedLocation = it
////                        selectedShopName = job.shopName
////                    },
//                    onCardClick = {
//                        rootNavController.navigate("jobDetails/${job.title}")
//                    }
//                )
//            }
//        }

    }
}

