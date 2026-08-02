package com.norvexa.flow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.norvexa.flow.domain.CashFlowPoint
import com.norvexa.flow.domain.formatMoney
import kotlin.math.max

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier, supporting: String? = null, emphasized: Boolean = false) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (!supporting.isNullOrBlank()) Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        action?.invoke()
    }
}

@Composable
fun EmptyState(title: String, description: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CashFlowChart(points: List<CashFlowPoint>, currency: String, modifier: Modifier = Modifier) {
    if (points.size < 2) return
    val lineColor = MaterialTheme.colorScheme.primary
    val zeroColor = MaterialTheme.colorScheme.outlineVariant
    val min = points.minOf { it.balanceMinor }
    val maxValue = points.maxOf { it.balanceMinor }
    val range = max(1L, maxValue - min)
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMoney(points.first().balanceMinor, currency), fontSize = 11.sp)
            Text(formatMoney(points.last().balanceMinor, currency), fontSize = 11.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp)) {
            if (min < 0 && maxValue > 0) {
                val zeroY = size.height - ((0L - min).toFloat() / range.toFloat()) * size.height
                drawLine(zeroColor, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 1.5f)
            }
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = index.toFloat() / (points.size - 1).toFloat() * size.width
                val y = size.height - ((point.balanceMinor - min).toFloat() / range.toFloat()) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
        }
    }
}
