package com.example.andrio_teacher.ui.screens.market

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FilterBar(
    selectedAcademicStage: String?,
    selectedSubjects: Set<String>,
    onAcademicStageChange: (String) -> Unit,
    onSubjectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 小学
            FilterChipItem(
                label = "小学",
                isSelected = selectedAcademicStage == "primary",
                onClick = { onAcademicStageChange("primary") }
            )
            
            // 初中
            FilterChipItem(
                label = "初中",
                isSelected = selectedAcademicStage == "junior_high",
                onClick = { onAcademicStageChange("junior_high") }
            )
            
            // 高中
            FilterChipItem(
                label = "高中",
                isSelected = selectedAcademicStage == "senior_high",
                onClick = { onAcademicStageChange("senior_high") }
            )
            
            // 科目
            FilterChipItem(
                label = if (selectedSubjects.isEmpty()) {
                    "科目"
                } else {
                    "科目(${selectedSubjects.size})"
                },
                isSelected = selectedSubjects.isNotEmpty(),
                onClick = onSubjectClick
            )
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color(0xFFF5F5F5)
        }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) {
                Color.White
            } else {
                Color(0xFF666666)
            }
        )
    }
}

