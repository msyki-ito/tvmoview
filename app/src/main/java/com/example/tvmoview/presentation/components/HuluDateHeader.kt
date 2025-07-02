package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tvmoview.presentation.theme.HuluColors

@Composable
fun HuluDateHeader(
    dateText: String,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleSmall,
            color = HuluColors.TextPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${itemCount}件",
            style = MaterialTheme.typography.bodySmall,
            color = HuluColors.TextTertiary
        )
    }
}

