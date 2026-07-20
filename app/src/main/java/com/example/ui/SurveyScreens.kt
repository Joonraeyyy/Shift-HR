package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.CompanySurvey
import com.example.ui.viewmodel.QuestionType
import com.example.ui.viewmodel.SurveyQuestion
import com.example.ui.viewmodel.SurveyResponse
import com.example.ui.viewmodel.TimeTrackerViewModel

@Composable
fun SurveyHubScreen(viewModel: TimeTrackerViewModel) {
    val userRole by viewModel.currentUserRole
    val isAdmin = userRole == "ADMIN_HR" || userRole == "MANAGER"
    
    var selectedTab by remember { mutableStateOf(if (isAdmin) 0 else 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Module Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Poll,
                contentDescription = "Survey Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "COMPANY SURVEY HUB",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Quarterly Engagement & Burnout Pulse",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Two-Sided Tab Layout for HR Admin or Employee Experience
        if (isAdmin) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("HR BUILDER & ANALYTICS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("SURVEYS FEED", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        // Screen Routing
        if (isAdmin && selectedTab == 0) {
            AdminSurveyDashboard(viewModel = viewModel)
        } else {
            EmployeeSurveyFeed(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSurveyDashboard(viewModel: TimeTrackerViewModel) {
    val surveys by viewModel.surveys
    val surveyResponses by viewModel.surveyResponses
    val context = LocalContext.current

    // Form Dynamic States
    var surveyTitle by remember { mutableStateOf("Q3 2026 Daily Work & Burnout Pulse") }
    var surveyDesc by remember { mutableStateOf("Evaluating friction points in daily toolkits and team alignment.") }
    var isMandatory by remember { mutableStateOf(true) }

    // Dynamic Questionnaire Array
    val questionsList = remember {
        mutableStateListOf(
            SurveyQuestion("1", "How clear are your day-to-day deliverables?", QuestionType.RATING_1_TO_5),
            SurveyQuestion("2", "Detail any technical blockers you encountered this week.", QuestionType.TEXT)
        )
    }
    var newQuestionText by remember { mutableStateOf("") }
    var newQuestionType by remember { mutableStateOf(QuestionType.RATING_1_TO_5) }

    var expandedSurveyId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: CREATE SURVEY
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_survey_builder_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "BUILD SURVEY CAMPAIGN",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = surveyTitle,
                        onValueChange = { surveyTitle = it },
                        label = { Text("Survey Title") },
                        modifier = Modifier.fillMaxWidth().testTag("survey_title_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = surveyDesc,
                        onValueChange = { surveyDesc = it },
                        label = { Text("Instructions/Description for Staff") },
                        modifier = Modifier.fillMaxWidth().testTag("survey_desc_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mandatory Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Enforce as Mandatory Action",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Blocks app usage until employee completes it",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = isMandatory,
                            onCheckedChange = { isMandatory = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("survey_mandatory_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic Questions List Inside Form
                    Text(
                        text = "Dynamic Questionnaire (${questionsList.size} Questions)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    questionsList.forEachIndexed { index, question ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${index + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = question.text,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { questionsList.removeAt(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Question",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Question Creator Block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .padding(10.dp)
                    ) {
                        Text(
                            "Add Custom Daily Work Question",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = newQuestionText,
                            onValueChange = { newQuestionText = it },
                            label = { Text("Question text (e.g. 'How is team synergy?')") },
                            modifier = Modifier.fillMaxWidth().testTag("new_question_text"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Question Type: ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FilterChip(
                                selected = newQuestionType == QuestionType.RATING_1_TO_5,
                                onClick = { newQuestionType = QuestionType.RATING_1_TO_5 },
                                label = { Text("1-5 Star Rating", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = newQuestionType == QuestionType.TEXT,
                                onClick = { newQuestionType = QuestionType.TEXT },
                                label = { Text("Free Text response", fontSize = 10.sp) }
                            )
                        }

                        Button(
                            onClick = {
                                if (newQuestionText.isNotBlank()) {
                                    questionsList.add(
                                        SurveyQuestion(
                                            id = java.util.UUID.randomUUID().toString(),
                                            text = newQuestionText,
                                            type = newQuestionType
                                        )
                                    )
                                    newQuestionText = ""
                                } else {
                                    Toast.makeText(context, "Please enter a question prompt", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Append Question", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Broadcast Trigger Button
                    Button(
                        onClick = {
                            if (surveyTitle.isBlank()) {
                                Toast.makeText(context, "Please configure a Survey Campaign Title", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (questionsList.isEmpty()) {
                                Toast.makeText(context, "Please append at least one question", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val survey = CompanySurvey(
                                id = java.util.UUID.randomUUID().toString(),
                                title = surveyTitle,
                                description = surveyDesc,
                                isMandatory = isMandatory,
                                questions = questionsList.toList()
                            )
                            viewModel.postSurvey(survey)
                            Toast.makeText(context, "Broadcasted and Notified Employee Terminal successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("broadcast_survey_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publish & Notify Staff", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section: EXISTING SURVEYS & ANALYTICS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SURVEY METRICS & HR ANALYTICS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (surveys.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No company surveys have been published yet.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(surveys) { survey ->
                val isExpanded = expandedSurveyId == survey.id
                val responsesForThisSurvey = surveyResponses.filter { it.surveyId == survey.id }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedSurveyId = if (isExpanded) null else survey.id },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (survey.isMandatory) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (survey.isMandatory) {
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "MANDATORY",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = "${survey.questions.size} Questions",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = survey.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "${responsesForThisSurvey.size} Responded",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSurvey(survey.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Expanded Analytics Details
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "Analytics Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            survey.questions.forEachIndexed { qIdx, question ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Q${qIdx + 1}: ${question.text}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Render depending on type
                                        if (question.type == QuestionType.RATING_1_TO_5) {
                                            // Calculate average rating
                                            val ratings = responsesForThisSurvey.mapNotNull { it.answers[question.id]?.toIntOrNull() }
                                            if (ratings.isEmpty()) {
                                                Text("No ratings submitted yet.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                            } else {
                                                val avgRating = ratings.average()
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = Color(0xFFF59E0B),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Average: ${String.format("%.1f", avgRating)} / 5.0",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "(${ratings.size} total votes)",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                // Mini bar visualizer
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                                ) {
                                                    val fraction = (avgRating / 5.0).toFloat()
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .fillMaxWidth(fraction)
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                }
                                            }
                                        } else {
                                            // TEXT Responses list
                                            val textAnswers = responsesForThisSurvey.mapNotNull { it.answers[question.id] }.filter { it.isNotBlank() }
                                            if (textAnswers.isEmpty()) {
                                                Text("No feedback responses submitted yet.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    textAnswers.forEach { ans ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                                .padding(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.FormatQuote,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = ans,
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeSurveyFeed(viewModel: TimeTrackerViewModel) {
    val surveys by viewModel.surveys
    val completedIds by viewModel.completedSurveyIds
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Active Survey campaigns",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        if (surveys.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No survey campaigns found. You're fully up-to-date!",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(surveys) { survey ->
                val isCompleted = completedIds.contains(survey.id)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (!isCompleted && survey.isMandatory) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else if (survey.isMandatory) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Assignment,
                                contentDescription = null,
                                tint = if (isCompleted) MaterialTheme.colorScheme.primary
                                else if (survey.isMandatory) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (survey.isMandatory) {
                                    Text(
                                        "REQUIRED",
                                        color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier
                                            .background(
                                                if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                                RoundedCornerShape(3.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isCompleted) "Completed" else "Pending Response",
                                    fontSize = 10.sp,
                                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = survey.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = survey.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                if (isCompleted) {
                                    Toast.makeText(context, "You've already submitted responses for this survey!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.activeCompletingSurvey.value = survey
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (isCompleted) "Submitted" else "Respond",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BouncySurveyNotificationPopup(
    visible: Boolean,
    survey: CompanySurvey?,
    viewModel: TimeTrackerViewModel,
    onDismiss: () -> Unit,
    onTakeSurvey: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    val alertBg = if (isDark) Color(0xE61E293B) else Color(0xF2FFFFFF)
    val alertBorder = if (isDark) Color(0x3334D399) else Color(0x331D4ED8)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) { -it },
        exit = slideOutVertically(
            animationSpec = spring(stiffness = Spring.StiffnessHigh)
        ) { -it }
    ) {
        survey?.let { activeSurvey ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(alertBg)
                    .border(BorderStroke(2.dp, alertBorder), RoundedCornerShape(20.dp))
                    .padding(16.dp)
                    .testTag("bouncy_survey_popup")
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (activeSurvey.isMandatory) Color(0x26DC2626) else Color(0x2634D399), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationImportant,
                            contentDescription = null,
                            tint = if (activeSurvey.isMandatory) Color(0xFFEF4444) else Color(0xFF34D399)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (activeSurvey.isMandatory) "REQUIRED ACTION" else "NEW COMPLIANCE SURVEY",
                                color = if (activeSurvey.isMandatory) Color(0xFFEF4444) else Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(text = activeSurvey.title, color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(text = activeSurvey.description, color = Color.Gray, fontSize = 12.sp, maxLines = 2)
                        
                        // Visual Progress Indicator for surveys with more than 1 question
                        if (activeSurvey.questions.size > 1) {
                            val totalQuestions = activeSurvey.questions.size
                            val answeredQuestions = activeSurvey.questions.count { q ->
                                viewModel.activeDraftAnswers[q.id]?.isNotBlank() == true
                            }
                            val progress = if (totalQuestions > 0) answeredQuestions.toFloat() / totalQuestions else 0f
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Draft Survey Progress",
                                        color = textPrimary.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$answeredQuestions of $totalQuestions completed (${(progress * 100).toInt()}%)",
                                        color = if (activeSurvey.isMandatory) Color(0xFFEF4444) else Color(0xFF34D399),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = if (activeSurvey.isMandatory) Color(0xFFEF4444) else Color(0xFF34D399),
                                    trackColor = if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onTakeSurvey,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeSurvey.isMandatory) Color(0xFFDC2626) else Color(0xFF1D4ED8)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Respond Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            if (!activeSurvey.isMandatory) {
                                TextButton(onClick = onDismiss, modifier = Modifier.height(36.dp)) {
                                    Text("Dismiss", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyCompletionTerminal(
    survey: CompanySurvey,
    viewModel: TimeTrackerViewModel,
    onDismiss: () -> Unit
) {
    val answers = viewModel.activeDraftAnswers
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // Glassmorphic styling colors
    val cardBg = if (isDark) Color(0xEE0F172A) else Color(0xEEFFFFFF)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
    val primaryAccent = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = {
            if (!survey.isMandatory) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !survey.isMandatory,
            dismissOnClickOutside = !survey.isMandatory
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(
                    listOf(
                        cardBg.copy(alpha = 0.9f),
                        cardBg.copy(alpha = 0.98f)
                    )
                ))
                .padding(24.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("survey_completion_terminal")
            ) {
                // Header of Terminal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = primaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SURVEY COMPLIANCE TERMINAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent,
                            letterSpacing = 1.sp
                        )
                    }

                    if (!survey.isMandatory) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        // Display security indicator
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "COMPLIANCE LOCKED",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title and description of survey
                Text(
                    text = survey.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = survey.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = cardBorder)

                // Questions Lazy List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(survey.questions) { question ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = question.text,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // Dynamic answer inputs depending on type
                                if (question.type == QuestionType.RATING_1_TO_5) {
                                    val selectedRating = answers[question.id]?.toIntOrNull() ?: 0
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val ratings = listOf(
                                            1 to "💀",
                                            2 to "😞",
                                            3 to "😐",
                                            4 to "😊",
                                            5 to "🔥"
                                        )
                                        ratings.forEach { (score, emoji) ->
                                            val isSelected = selectedRating == score
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) primaryAccent
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                    .clickable { answers[question.id] = score.toString() }
                                                    .padding(6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(emoji, fontSize = 16.sp)
                                                    Text(
                                                        text = score.toString(),
                                                        fontSize = 9.sp,
                                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val currentText = answers[question.id] ?: ""
                                    OutlinedTextField(
                                        value = currentText,
                                        onValueChange = { answers[question.id] = it },
                                        placeholder = { Text("Write your honest workplace feedback here...", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = primaryAccent,
                                            focusedLabelColor = primaryAccent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Submit Button
                Button(
                    onClick = {
                        // Check if all questions are filled out
                        val allAnswered = survey.questions.all { answers[it.id]?.isNotBlank() == true }
                        if (!allAnswered) {
                            Toast.makeText(context, "Please provide responses for all survey questions", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.submitSurveyResponse(survey.id, answers.toMap())
                        onDismiss()
                        Toast.makeText(context, "Responses recorded. Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_survey_response_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Securely Submit Response", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
