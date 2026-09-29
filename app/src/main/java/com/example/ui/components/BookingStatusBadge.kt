package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DoorblyTeal
import com.example.ui.theme.StatusAccepted
import com.example.ui.theme.StatusCancelled
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusOnTheWay
import com.example.ui.theme.StatusPending

@Composable
fun BookingStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        "Pending" -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        "Accepted" -> Color(0xFFDBEAFE) to Color(0xFF1E40AF)
        "Partner Assigned" -> Color(0xFFE0E7FF) to Color(0xFF3730A3)
        "On The Way" -> Color(0xFFEDE9FE) to Color(0xFF5B21B6)
        "Started" -> Color(0xFFCCFBF1) to Color(0xFF115E59)
        "Completed" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "Cancelled" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        else -> Color(0xFFF1F5F9) to Color(0xFF475569)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PaymentStatusBadge(
    paymentStatus: String,
    modifier: Modifier = Modifier
) {
    val isPaid = paymentStatus.equals("Paid", ignoreCase = true)
    val bgColor = if (isPaid) Color(0xFFD1FAE5) else Color(0xFFFFFBEB)
    val textColor = if (isPaid) Color(0xFF065F46) else Color(0xFFB45309)
    val borderColor = if (isPaid) Color(0xFF10B981) else Color(0xFFF59E0B)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.Schedule,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isPaid) "Paid" else "Payment Due",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
