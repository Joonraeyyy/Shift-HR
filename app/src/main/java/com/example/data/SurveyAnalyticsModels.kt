package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CategoryScore(
    val categoryName: String,
    val averageScore: Float, // 1.0 to 5.0
    val totalResponses: Int,
    val statusColorHex: String // "#34D399" (Green), "#F59E0B" (Amber), "#EF4444" (Red)
)

@JsonClass(generateAdapter = true)
data class QuarterTrend(
    val quarterLabel: String, // e.g. "Q1 2026", "Q2 2026", "Q3 2026"
    val overallScore: Float
)

@JsonClass(generateAdapter = true)
data class KeywordInsight(
    val word: String,
    val count: Int,
    val sentiment: String // "NEGATIVE", "POSITIVE", "NEUTRAL"
)

@JsonClass(generateAdapter = true)
data class SurveyAnalyticsData(
    val surveyId: String,
    val surveyTitle: String,
    val completionRatePercentage: Int,
    val totalParticipants: Int,
    val anonymousRatioPercentage: Int,
    val overallAverageScore: Float,
    val categoryScores: List<CategoryScore>,
    val quarterlyTrends: List<QuarterTrend>,
    val topKeywords: List<KeywordInsight>
)

fun sampleSurveyAnalyticsData(): SurveyAnalyticsData {
    return SurveyAnalyticsData(
        surveyId = "SURVEY-2026-Q3",
        surveyTitle = "Q3 2026 Employee Engagement & Wellness Pulse",
        completionRatePercentage = 88,
        totalParticipants = 142,
        anonymousRatioPercentage = 76,
        overallAverageScore = 3.8f,
        categoryScores = listOf(
            CategoryScore("Management Support", 4.2f, 142, "#34D399"),
            CategoryScore("Workplace Tools & Tech", 2.8f, 138, "#EF4444"),
            CategoryScore("Work-Life Balance", 3.1f, 140, "#F59E0B"),
            CategoryScore("Compensation & Benefits", 3.9f, 142, "#34D399"),
            CategoryScore("Shift Scheduling Clarity", 2.6f, 135, "#EF4444")
        ),
        quarterlyTrends = listOf(
            QuarterTrend("Q4 2025", 3.2f),
            QuarterTrend("Q1 2026", 3.5f),
            QuarterTrend("Q2 2026", 3.6f),
            QuarterTrend("Q3 2026", 3.8f)
        ),
        topKeywords = listOf(
            KeywordInsight("Shift Overtime", 42, "NEGATIVE"),
            KeywordInsight("Great Managers", 38, "POSITIVE"),
            KeywordInsight("Tool Blockers", 29, "NEGATIVE"),
            KeywordInsight("Remote Days", 21, "NEUTRAL")
        )
    )
}
