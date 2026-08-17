package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.viewmodel.Announcement
import com.example.ui.viewmodel.BulletinComment
import com.example.ui.viewmodel.TimeTrackerViewModel
import kotlin.math.absoluteValue

// Corporate Color Palette
private val CorpEmerald = Color(0xFF10B981)
private val CorpCyan = Color(0xFF06B6D4)
private val CorpDarkNavy = Color(0xFF0B1120)
private val CorpSlateCard = Color(0xFF131E32)
private val CorpBorder = Color(0xFF1E293B)
private val CorpGold = Color(0xFFF59E0B)

/**
 * Corporate Style 3D Fanning HR Bulletin & Executive Directive Desk
 */
@Composable
fun HRBulletinSection(
    viewModel: TimeTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val announcements by viewModel.announcements
    val currentUserRole by viewModel.currentUserRole
    val isHrOrManager = currentUserRole == "ADMIN_HR" || currentUserRole == "MANAGER" || currentUserRole == "SUPERVISOR"
    
    var selectedBulletin by remember { mutableStateOf<Announcement?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Corporate Detail Modal dialog
    if (selectedBulletin != null) {
        val currentPost = announcements.find { it.id == selectedBulletin!!.id } ?: selectedBulletin!!
        HRBulletinDetailDialog(
            bulletin = currentPost,
            onDismiss = { selectedBulletin = null },
            onLikeClick = { viewModel.toggleLikeBulletin(currentPost.id) },
            onCelebrateClick = { viewModel.toggleCelebrateBulletin(currentPost.id) },
            onAddComment = { comment -> viewModel.addBulletinComment(currentPost.id, comment) },
            onDeleteClick = if (isHrOrManager) {
                {
                    viewModel.deleteBulletinPost(currentPost.id)
                    selectedBulletin = null
                }
            } else null
        )
    }

    // HR Publish Corporate Directive Dialog
    if (showCreateDialog) {
        HRCreateBulletinDialog(
            onDismiss = { showCreateDialog = false },
            onPublish = { title, subtitle, content, category, imageUrl, drawableResName, gallery ->
                viewModel.createBulletinPost(
                    title = title,
                    subtitle = subtitle,
                    content = content,
                    category = category,
                    imageUrl = imageUrl,
                    drawableResName = drawableResName,
                    galleryImages = gallery
                )
                showCreateDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Executive Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(CorpEmerald)
                    )
                    Text(
                        text = "CORPORATE DIRECTIVES & HR BULLETIN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = CorpEmerald,
                        letterSpacing = 1.8.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Executive Operations Desk",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.4).sp
                )
            }

            // Post Button for Corporate / HR Admins
            if (isHrOrManager) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CorpEmerald.copy(alpha = 0.18f),
                        contentColor = CorpEmerald
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CorpEmerald.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_post_hr_bulletin")
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = "Dispatch Memo", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Dispatch Memo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (announcements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(CorpSlateCard.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .border(1.dp, CorpBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No corporate directives published yet.", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        } else {
            // 3D Stacked Corporate Pager
            val pagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { announcements.size }
            )

            val density = LocalDensity.current.density

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(375.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 68.dp),
                    pageSpacing = (-24).dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(365.dp)
                        .testTag("hr_bulletin_3d_pager")
                ) { page ->
                    val bulletin = announcements[page]
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        .coerceIn(-3f, 3f)

                    // 3D Arched Transformations
                    val scale = lerp(0.86f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
                    val rotationZ = (pageOffset * 5.5f)
                    val rotationY = (pageOffset * -15f)
                    val translationY = (pageOffset.absoluteValue * 22.dp.value * density)
                    val translationX = (pageOffset * 5.dp.value * density)
                    val alpha = lerp(0.6f, 1f, 1f - (pageOffset.absoluteValue * 0.3f).coerceIn(0f, 0.4f))

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                this.scaleX = scale
                                this.scaleY = scale
                                this.rotationZ = rotationZ
                                this.rotationY = rotationY
                                this.translationY = translationY
                                this.translationX = translationX
                                this.alpha = alpha
                                this.cameraDistance = 14f * density
                            }
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        HRCorporateCard(
                            bulletin = bulletin,
                            onClick = { selectedBulletin = bulletin }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Corporate Pager Indicator Dots
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(announcements.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateFloatAsState(
                        targetValue = if (isSelected) 24f else 6f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "corp_dot_width"
                    )

                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(dotWidth.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isSelected) CorpEmerald else Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Single Corporate Directive Card
 */
@Composable
fun HRCorporateCard(
    bulletin: Announcement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Category Corporate Pill Colors
    val (pillBg, pillBorder, pillText) = when (bulletin.category.uppercase()) {
        "POLICY" -> Triple(CorpEmerald.copy(alpha = 0.2f), CorpEmerald.copy(alpha = 0.6f), CorpEmerald)
        "STRATEGY" -> Triple(CorpCyan.copy(alpha = 0.2f), CorpCyan.copy(alpha = 0.6f), CorpCyan)
        "COMPLIANCE" -> Triple(Color(0xFFE11D48).copy(alpha = 0.2f), Color(0xFFE11D48).copy(alpha = 0.6f), Color(0xFFFF6482))
        "TOWN HALL" -> Triple(CorpGold.copy(alpha = 0.2f), CorpGold.copy(alpha = 0.6f), CorpGold)
        "BENEFITS" -> Triple(Color(0xFF8B5CF6).copy(alpha = 0.2f), Color(0xFF8B5CF6).copy(alpha = 0.6f), Color(0xFFA78BFA))
        else -> Triple(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.35f), Color.White)
    }

    Card(
        modifier = modifier
            .width(240.dp)
            .height(340.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.7f),
                ambientColor = Color.Black.copy(alpha = 0.5f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("bulletin_card_${bulletin.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CorpDarkNavy),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.8f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Corporate Imagery Background
            when {
                bulletin.drawableResName.isNotEmpty() -> {
                    val resId = when (bulletin.drawableResName) {
                        "corp_global_synergy" -> R.drawable.corp_global_synergy
                        "corp_executive_boardroom" -> R.drawable.corp_executive_boardroom
                        else -> R.drawable.corp_executive_boardroom
                    }
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = bulletin.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                bulletin.imageUrl.isNotEmpty() -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(bulletin.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = bulletin.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                )
                            )
                    )
                }
            }

            // 2. High-contrast Slate Executive Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0B1120).copy(alpha = 0.85f),
                                Color(0xFF0B1120).copy(alpha = 0.25f),
                                Color(0xFF0B1120).copy(alpha = 0.75f),
                                Color(0xFF0B1120).copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            // 3. Card Details & Meta
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag Bar: Official Classification & Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Official Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = CorpEmerald, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "OFFICIAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Category Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(pillBg)
                            .border(1.dp, pillBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = bulletin.category.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = pillText,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Middle Content: Directive Title & Department
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = bulletin.title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 23.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (bulletin.subtitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = bulletin.subtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CorpCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Issued by: ${bulletin.author}",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom Action & Engagement Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF334155).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Review Memo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CorpEmerald
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = CorpEmerald, modifier = Modifier.size(12.dp))
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (bulletin.likesCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = CorpCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${bulletin.likesCount}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        if (bulletin.comments.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.QuestionAnswer,
                                    contentDescription = null,
                                    tint = CorpGold,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${bulletin.comments.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formal Corporate Memorandum Detail Dialog
 */
@Composable
fun HRBulletinDetailDialog(
    bulletin: Announcement,
    onDismiss: () -> Unit,
    onLikeClick: () -> Unit,
    onCelebrateClick: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var commentInput by remember { mutableStateOf("") }
    
    val allPhotos = remember(bulletin) {
        val list = mutableListOf<String>()
        if (bulletin.imageUrl.isNotEmpty()) list.add(bulletin.imageUrl)
        list.addAll(bulletin.galleryImages)
        list
    }
    val galleryPagerState = rememberPagerState(initialPage = 0, pageCount = { if (allPhotos.isNotEmpty()) allPhotos.size else 1 })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .shadow(28.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1322)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Executive Memo Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF070C16))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CorpEmerald.copy(alpha = 0.15f))
                                .border(1.dp, CorpEmerald.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CorporateFare, contentDescription = null, tint = CorpEmerald, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ENTERPRISE DIRECTIVE & MEMORANDUM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = CorpEmerald,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "REF: CORP-${bulletin.id.take(8).uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // 2. Photo / Executive Graphic Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (allPhotos.isNotEmpty()) {
                        HorizontalPager(
                            state = galleryPagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(allPhotos[page])
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Memo photo ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else if (bulletin.drawableResName.isNotEmpty()) {
                        val resId = when (bulletin.drawableResName) {
                            "corp_global_synergy" -> R.drawable.corp_global_synergy
                            "corp_executive_boardroom" -> R.drawable.corp_executive_boardroom
                            else -> R.drawable.corp_executive_boardroom
                        }
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = bulletin.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
                        )
                    }

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF0B1322).copy(alpha = 0.9f),
                                        Color(0xFF0B1322)
                                    )
                                )
                            )
                    )

                    // Pill Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 18.dp, bottom = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CorpDarkNavy)
                            .border(1.dp, CorpEmerald.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "CATEGORY: ${bulletin.category.uppercase()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CorpEmerald,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // 3. Corporate Directive Metadata Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    Text(
                        text = bulletin.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 28.sp
                    )

                    if (bulletin.subtitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = bulletin.subtitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CorpCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Formal Memo Data Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111C30), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MemoDataRow(label = "ISSUED BY", value = bulletin.author, icon = Icons.Default.AccountBalance)
                        MemoDataRow(label = "DATE OF ISSUE", value = bulletin.date, icon = Icons.Default.CalendarToday)
                        MemoDataRow(label = "GOVERNANCE", value = "Enterprise Active Mandate", icon = Icons.Default.Gavel)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Directive Content Body
                    Text(
                        text = "DIRECTIVE DETAILS & OPERATING PROCEDURES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = bulletin.content,
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Corporate Action Bar: Endorse, Celebrate & Archive
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111C30), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Endorse / Like Button
                        Button(
                            onClick = onLikeClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bulletin.isLiked) CorpCyan.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (bulletin.isLiked) CorpCyan else Color.White.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_like_bulletin")
                        ) {
                            Icon(
                                if (bulletin.isLiked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Endorse",
                                tint = if (bulletin.isLiked) CorpCyan else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Endorse (${bulletin.likesCount})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Celebrate Milestone Button
                        Button(
                            onClick = onCelebrateClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bulletin.isCelebrated) CorpGold.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (bulletin.isCelebrated) CorpGold else Color.White.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_celebrate_bulletin")
                        ) {
                            Icon(
                                Icons.Default.Celebration,
                                contentDescription = "Celebrate",
                                tint = if (bulletin.isCelebrated) CorpGold else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Acknowledge (${bulletin.celebrateCount})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (onDeleteClick != null) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFF43F5E).copy(alpha = 0.8f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 5. Official Department Feedback Thread
                    Text(
                        text = "DEPARTMENT INQUIRIES & ACKNOWLEDGMENTS (${bulletin.comments.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (bulletin.comments.isEmpty()) {
                        Text(
                            text = "No employee questions or acknowledgments logged yet.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bulletin.comments.forEach { comment ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF111C30), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = comment.authorName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(CorpCyan.copy(alpha = 0.2f))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = comment.authorRole,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CorpCyan
                                                )
                                            }
                                        }
                                        Text(
                                            text = comment.timestamp,
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.4f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comment.text,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Row for Remarks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            placeholder = { Text("Log official response or note...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)) },
                            modifier = Modifier.weight(1f).testTag("bulletin_comment_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CorpEmerald,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        IconButton(
                            onClick = {
                                if (commentInput.isNotBlank()) {
                                    onAddComment(commentInput.trim())
                                    commentInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CorpEmerald)
                                .testTag("btn_send_bulletin_comment")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MemoDataRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = CorpEmerald, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
        }
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

/**
 * HR Creator Dialog to Dispatch Official Corporate Directives
 */
@Composable
fun HRCreateBulletinDialog(
    onDismiss: () -> Unit,
    onPublish: (title: String, subtitle: String, content: String, category: String, imageUrl: String, drawableResName: String, gallery: List<String>) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("POLICY") }
    var selectedVisualPreset by remember { mutableStateOf("corp_executive_boardroom") }
    var customImageUrl by remember { mutableStateOf("") }
    var additionalPhotosCsv by remember { mutableStateOf("") }

    val categories = listOf("POLICY", "STRATEGY", "COMPLIANCE", "TOWN HALL", "BENEFITS", "OPERATIONS", "SECURITY", "TALENT")
    val visualPresets = listOf(
        "corp_executive_boardroom" to "Executive Boardroom (Glass Skyline)",
        "corp_global_synergy" to "Global Strategy (Enterprise Sync)",
        "img_hr_team_summit" to "Leadership Summit (Summit Hall)"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .shadow(24.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1322)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DISPATCH CORPORATE DIRECTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = CorpEmerald,
                            letterSpacing = 1.4.sp
                        )
                        Text(
                            text = "Executive Memo Authoring",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                // Title Input
                Text("Memo Subject / Directive Title", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Q3 Regional Synergy & Compliance Directive", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("input_bulletin_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CorpEmerald,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Subtitle Input
                Text("Executive Summary / Tagline", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    placeholder = { Text("e.g. Enterprise Hybrid Protocol & Roster Alignment", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("input_bulletin_subtitle"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CorpEmerald,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Category Selection
                Text("Directive Classification", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CorpEmerald else Color(0xFF111C30))
                                .border(1.dp, if (isSelected) CorpEmerald else Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Choose Cover Photo Visual Preset
                Text("Select Corporate Visual Theme", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    visualPresets.forEach { (resKey, label) ->
                        val isSelected = selectedVisualPreset == resKey && customImageUrl.isBlank()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF162540) else Color(0xFF111C30))
                                .border(1.dp, if (isSelected) CorpEmerald else Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedVisualPreset = resKey
                                    customImageUrl = ""
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedVisualPreset = resKey
                                    customImageUrl = ""
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = CorpEmerald, unselectedColor = Color.White.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = label, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }

                // Custom Photo URL
                Text("Or Custom Enterprise Graphic URL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                OutlinedTextField(
                    value = customImageUrl,
                    onValueChange = { customImageUrl = it },
                    placeholder = { Text("https://company.com/memo-banner.jpg", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("input_bulletin_custom_url"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CorpEmerald,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Additional Photos
                Text("Additional Document & Gallery URLs (comma-separated)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                OutlinedTextField(
                    value = additionalPhotosCsv,
                    onValueChange = { additionalPhotosCsv = it },
                    placeholder = { Text("https://img1.jpg, https://img2.jpg", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CorpEmerald,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Detailed Announcement Content
                Text("Official Directive Memorandum Body", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Enter formal policy directives, memorandum reference, scope, timelines, and instructions...", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("input_bulletin_content"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CorpEmerald,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (title.isBlank() || content.isBlank()) {
                                Toast.makeText(context, "Please provide directive title and content.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val galleryList = additionalPhotosCsv.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }

                            onPublish(
                                title.trim(),
                                subtitle.trim(),
                                content.trim(),
                                selectedCategory,
                                customImageUrl.trim(),
                                if (customImageUrl.isBlank()) selectedVisualPreset else "",
                                galleryList
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CorpEmerald, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_publish_bulletin")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dispatch Directive", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
