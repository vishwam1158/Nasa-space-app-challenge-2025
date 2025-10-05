package com.example.farmnavigator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.farmnavigator.viewmodel.ChartDataPoint

@Composable
fun DataVisualization(chartData: List<ChartDataPoint>) {
    if (chartData.isEmpty()) {
        Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Awaiting satellite data for visualization...", color = Color.Gray, modifier = Modifier.padding(16.dp))
        }
        return
    }

    val minVal = 0.0
    val maxVal = 1.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val gridLineColor = Color.Gray.copy(alpha = 0.5f)
            val strokeWidthPx = 1.dp.toPx()
            drawLine(color = gridLineColor, start = Offset(0f, 0f), end = Offset(width, 0f), strokeWidth = strokeWidthPx * 2)
            drawLine(color = gridLineColor, start = Offset(0f, height / 2), end = Offset(width, height / 2), strokeWidth = strokeWidthPx)
            drawLine(color = gridLineColor, start = Offset(0f, height), end = Offset(width, height), strokeWidth = strokeWidthPx * 2)

            val dataPointsCount = chartData.size
            if (dataPointsCount > 1) {
                val path = Path()
                val successColor = Color(0xFF4CAF50)
                val criticalColor = Color(0xFF1E88E5)

                chartData.forEachIndexed { index, point ->
                    val x = (index.toFloat() / (dataPointsCount - 1)) * width
                    val y = height - ((point.value - minVal) / (maxVal - minVal)).toFloat() * height
                    val currentOffset = Offset(x, y)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }

                    drawCircle(
                        color = if (index == dataPointsCount - 1) criticalColor else successColor,
                        center = currentOffset,
                        radius = 8f
                    )
                }

                drawPath(path = path, color = successColor, style = Stroke(width = 5f))

                val finalPoint = chartData.last()
                val finalX = width
                val finalY = height - ((finalPoint.value - minVal) / (maxVal - minVal)).toFloat() * height
                drawCircle(color = criticalColor, center = Offset(finalX, finalY), radius = 12f)
            }
        }
        Text(
            text = "Day ${chartData.size}\n(${String.format("%.2f", chartData.last().value)})",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            lineHeight = 14.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 5.dp)
        )
        Text(
            text = "1.0 (Optimal)",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Text(
            text = "0.0 (Critical)",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

