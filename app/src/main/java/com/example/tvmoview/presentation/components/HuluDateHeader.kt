package com.example.tvmoview.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.example.tvmoview.presentation.theme.HuluColors

@Composable
fun HuluDateHeader(
    dateText: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = dateText,
        style = MaterialTheme.typography.titleMedium,
        color = HuluColors.TextPrimary,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

