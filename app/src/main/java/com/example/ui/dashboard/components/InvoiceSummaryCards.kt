package com.example.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.PrimaryBlue
import com.example.util.CurrencyFormatter

/**
 * Material 3 Summary Cards displaying statistical totals for
 * 'Paid', 'Pending', and 'Overdue' invoices with visual polish,
 * cashflow collection ratio, and fast actionable follow-ups.
 */
@Composable
fun InvoiceSummaryCards(
    paidTotal: Double,
    paidCount: Int,
    pendingTotal: Double,
    pendingCount: Int,
    overdueTotal: Double,
    overdueCount: Int,
    currencySymbol: String,
    onOverdueActionClick: () -> Unit,
    onCardClick: ((statusFilter: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val grandTotal = paidTotal + pendingTotal + overdueTotal
    val paidPercent = if (grandTotal > 0) ((paidTotal / grandTotal) * 100).toInt() else 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ==========================================
        // 1. HERO MATERIAL 3 PAID SUMMARY CARD
        // ==========================================
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("summary_card_paid")
                .then(
                    if (onCardClick != null) Modifier.clickable { onCardClick("PAID") } else Modifier
                ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PaidGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Paid Invoices",
                                tint = PaidGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Paid & Collected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$paidCount invoice${if (paidCount != 1) "s" else ""} settled",
                                fontSize = 12.sp,
                                color = PaidGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // % Collection Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PaidGreenLight
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = PaidGreenDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$paidPercent% collected",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PaidGreenDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = CurrencyFormatter.format(paidTotal, currencySymbol),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("summary_stat_paid_amount")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Smooth linear progress indicator of collected funds
                LinearProgressIndicator(
                    progress = { if (grandTotal > 0) (paidTotal / grandTotal).toFloat() else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PaidGreen,
                    trackColor = Color(0xFFE2E8F0)
                )
            }
        }

        // ==========================================
        // 2. SIDE-BY-SIDE PENDING & OVERDUE M3 CARDS
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pending Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("summary_card_pending")
                    .then(
                        if (onCardClick != null) Modifier.clickable { onCardClick("PENDING") } else Modifier
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PendingAmberLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = "Pending Invoices",
                                tint = PendingAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "$pendingCount inv",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Pending",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = CurrencyFormatter.format(pendingTotal, currencySymbol),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("summary_stat_pending_amount")
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Awaiting due date",
                        fontSize = 11.sp,
                        color = PendingAmberDark
                    )
                }
            }

            // Overdue Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("summary_card_overdue")
                    .then(
                        if (onCardClick != null) Modifier.clickable { onCardClick("OVERDUE") } else Modifier
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (overdueCount > 0) OverdueRedLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (overdueCount > 0) OverdueRed else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Overdue Invoices",
                                tint = if (overdueCount > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (overdueCount > 0) OverdueRedLight else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "$overdueCount inv",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (overdueCount > 0) OverdueRedDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Overdue",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (overdueCount > 0) OverdueRedDark else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = CurrencyFormatter.format(overdueTotal, currencySymbol),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (overdueCount > 0) OverdueRed else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("summary_stat_overdue_amount")
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (overdueCount > 0) "Needs follow-up ⚠️" else "Zero late invoices",
                        fontSize = 11.sp,
                        fontWeight = if (overdueCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (overdueCount > 0) OverdueRedDark else PaidGreen
                    )
                }
            }
        }

        // ==========================================
        // 3. SEGMENTED CASHFLOW BREAKDOWN CARD
        // ==========================================
        if (grandTotal > 0) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.testTag("cashflow_breakdown_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Total Invoiced Volume",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = CurrencyFormatter.format(grandTotal, currencySymbol),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Multi-color segmented cashflow distribution bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        if (paidTotal > 0) {
                            Box(
                                modifier = Modifier
                                    .weight((paidTotal / grandTotal).toFloat())
                                    .fillMaxSize()
                                    .background(PaidGreen)
                            )
                        }
                        if (pendingTotal > 0) {
                            Box(
                                modifier = Modifier
                                    .weight((pendingTotal / grandTotal).toFloat())
                                    .fillMaxSize()
                                    .background(PendingAmber)
                            )
                        }
                        if (overdueTotal > 0) {
                            Box(
                                modifier = Modifier
                                    .weight((overdueTotal / grandTotal).toFloat())
                                    .fillMaxSize()
                                    .background(OverdueRed)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend with counts and percentages
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Paid legend
                        LegendDot(
                            label = "Paid (${paidPercent}%)",
                            color = PaidGreen
                        )
                        // Pending legend
                        val pendingPercent = ((pendingTotal / grandTotal) * 100).toInt()
                        LegendDot(
                            label = "Pending (${pendingPercent}%)",
                            color = PendingAmber
                        )
                        // Overdue legend
                        val overduePercent = ((overdueTotal / grandTotal) * 100).toInt()
                        LegendDot(
                            label = "Overdue (${overduePercent}%)",
                            color = OverdueRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
