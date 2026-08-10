package com.example.dutype.components

import com.dutype.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.data.ThemeMode
import com.example.dutype.data.ThemePreferenceStore
import com.example.dutype.ui.theme.LocalThemeMode
import com.example.dutype.ui.theme.WorkerColors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeModeViewModel @Inject constructor(
    private val store: ThemePreferenceStore
) : ViewModel() {
    fun setMode(mode: ThemeMode) {
        viewModelScope.launch { store.setThemeMode(mode) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeModeBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    viewModel: ThemeModeViewModel = hiltViewModel()
) {
    val current = LocalThemeMode.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WorkerColors.CardBackground,
        contentColor = WorkerColors.TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.appearance),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = WorkerColors.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.appearance_subtitle),
                fontSize = 13.sp,
                color = WorkerColors.TextSecondary,
            )
            Spacer(Modifier.height(20.dp))

            ThemeOptionRow(
                title = stringResource(R.string.system_default),
                subtitle = stringResource(R.string.theme_match_phone),
                icon = Icons.Default.PhoneAndroid,
                selected = current == ThemeMode.SYSTEM,
                onClick = {
                    viewModel.setMode(ThemeMode.SYSTEM)
                    onDismiss()
                },
            )
            Spacer(Modifier.height(10.dp))
            ThemeOptionRow(
                title = stringResource(R.string.light),
                subtitle = stringResource(R.string.theme_light_subtitle),
                icon = Icons.Default.LightMode,
                selected = current == ThemeMode.LIGHT,
                onClick = {
                    viewModel.setMode(ThemeMode.LIGHT)
                    onDismiss()
                },
            )
            Spacer(Modifier.height(10.dp))
            ThemeOptionRow(
                title = stringResource(R.string.dark),
                subtitle = stringResource(R.string.theme_dark_subtitle),
                icon = Icons.Default.DarkMode,
                selected = current == ThemeMode.DARK,
                onClick = {
                    viewModel.setMode(ThemeMode.DARK)
                    onDismiss()
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) WorkerColors.BorderFocused else WorkerColors.Border
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WorkerColors.ChipBackground, RoundedCornerShape(14.dp))
            .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(WorkerColors.PrimaryLight, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WorkerColors.Primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WorkerColors.TextPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = WorkerColors.TextSecondary)
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = WorkerColors.Primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
