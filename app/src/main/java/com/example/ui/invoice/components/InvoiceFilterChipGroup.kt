package com.example.ui.invoice.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.OverdueRedDark
import com.example.ui.theme.OverdueRedLight
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PaidGreenDark
import com.example.ui.theme.PaidGreenLight
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberDark
import com.example.ui.theme.PendingAmberLight

data class StatusFilterOption(
    val key: String,
    val title: String,
    val count: Int,
    val icon: ImageVector,
    val testTag: String
)

/**
 * Material 3 Filter Chip Group for filtering invoices by status:
 * All, Paid, Pending, or Overdue.
 */
@Composable
fun InvoiceFilterChipGroup(
    selectedFilter: String,
    allCount: Int,
    paidCount: Int,
    pendingCount: Int,
    overdueCount: Int,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        StatusFilterOption(
            key = "ALL",
            title = "All",
            count = allCount,
            icon = Icons.Default.Layers,
            testTag = "filter_chip_all"
        ),
        StatusFilterOption(
            key = "PAID",
            title = "Paid",
            count = paidCount,
            icon = Icons.Default.CheckCircle,
            testTag = "filter_chip_paid"
        ),
        StatusFilterOption(
            key = "PENDING",
            title = "Pending",
            count = pendingCount,
            icon = Icons.Default.HourglassTop,
            testTag = "filter_chip_pending"
        ),
        StatusFilterOption(
            key = "OVERDUE",
            title = "Overdue",
            count = overdueCount,
            icon = Icons.Default.ErrorOutline,
            testTag = "filter_chip_overdue"
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("invoice_filter_chip_group"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { option ->
            val isSelected = selectedFilter.equals(option.key, ignoreCase = true)

            val (selectedContainer, selectedLabel, iconTint) = when (option.key) {
                "PAID" -> Triple(PaidGreenLight, PaidGreenDark, if (isSelected) PaidGreenDark else PaidGreen)
                "PENDING" -> Triple(PendingAmberLight, PendingAmberDark, if (isSelected) PendingAmberDark else PendingAmber)
                "OVERDUE" -> Triple(OverdueRedLight, OverdueRedDark, if (isSelected) OverdueRedDark else OverdueRed)
                else -> Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
            }

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(option.key) },
                label = {
                    Text(
                        text = "${option.title} (${option.count})",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = selectedContainer,
                    selectedLabelColor = selectedLabel,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .testTag(option.testTag)
                    .testTag("filter_chip_${option.key}")
            )
        }
    }
}
