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

/**
 * 3D Fanning Stacked Carousel Card Component for HR Bulletin
 * Matching the exact visual aesthetic with arched 3D cards, pill tags, and detailed announcement view.
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

    // Detail modal dialog
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

    // HR Create Bulletin Dialog
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
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Section: Eyebrow + Display Headline + HR Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAILY ENERGY & HR BULLETIN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE2B774), // Warm golden editorial tone
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Feel Better Every Day",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
            }

            // Post Button for HR / Management
            if (isHrOrManager) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp).testTag("btn_post_hr_bulletin")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Post", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Post", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (announcements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No announcements published yet.", color = Color.White.copy(alpha = 0.6f))
            }
        } else {
            // 3D Stacked Fanning Carousel
            val pagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { announcements.size }
            )

            val density = LocalDensity.current.density

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 72.dp),
                    pageSpacing = (-28).dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(370.dp)
                        .testTag("hr_bulletin_3d_pager")
                ) { page ->
                    val bulletin = announcements[page]
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                        .coerceIn(-3f, 3f)

                    // 3D Arched Transformation calculations
                    val scale = lerp(0.85f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
                    val rotationZ = (pageOffset * 6.5f) // Fans along the arc
                    val rotationY = (pageOffset * -16f) // 3D inward perspective
                    val translationY = (pageOffset.absoluteValue * 26.dp.value * density) // Downward arch curve
                    val translationX = (pageOffset * 6.dp.value * density)
                    val alpha = lerp(0.55f, 1f, 1f - (pageOffset.absoluteValue * 0.35f).coerceIn(0f, 0.45f))

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
                        HRBulletinCard(
                            bulletin = bulletin,
                            onClick = { selectedBulletin = bulletin }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Carousel Dot Indicators
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(announcements.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateFloatAsState(
                        targetValue = if (isSelected) 22f else 6f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dot_width"
                    )

                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(dotWidth.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Single 3D Bulletin Card Item
 */
@Composable
fun HRBulletinCard(
    bulletin: Announcement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .width(235.dp)
            .height(345.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = Color.Black.copy(alpha = 0.6f),
                ambientColor = Color.Black.copy(alpha = 0.4f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("bulletin_card_${bulletin.id}"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Photo Background Cover
            when {
                bulletin.drawableResName.isNotEmpty() -> {
                    val resId = when (bulletin.drawableResName) {
                        "img_hr_mind_reset" -> R.drawable.img_hr_mind_reset
                        "img_hr_morning_flow" -> R.drawable.img_hr_morning_flow
                        "img_hr_team_summit" -> R.drawable.img_hr_team_summit
                        else -> R.drawable.img_hr_mind_reset
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
                    // Aesthetic fallback ambient gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
                                )
                            )
                    )
                }
            }

            // 2. Dark Cinematic Overlay Gradient for crisp text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // 3. Top Content: Title + Subtitle on Left, Glass Pill Badge on Right
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = bulletin.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 24.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (bulletin.subtitle.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = bulletin.subtitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Glassmorphic Category Tag Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = bulletin.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                // Bottom Content: Tap hint with subtle blur bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to view update ↗",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (bulletin.likesCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(12.dp)
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
                                    Icons.Default.ChatBubble,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(12.dp)
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
 * Rich Detail Modal for Bulletin Announcement
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
    
    // Multi-photo slider pager if gallery exists
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
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .shadow(24.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Hero Image / Multi-Photo Carousel Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    if (allPhotos.isNotEmpty()) {
                        HorizontalPager(
                            state = galleryPagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val photoUrl = allPhotos[page]
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Gallery photo ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else if (bulletin.drawableResName.isNotEmpty()) {
                        val resId = when (bulletin.drawableResName) {
                            "img_hr_mind_reset" -> R.drawable.img_hr_mind_reset
                            "img_hr_morning_flow" -> R.drawable.img_hr_morning_flow
                            "img_hr_team_summit" -> R.drawable.img_hr_team_summit
                            else -> R.drawable.img_hr_mind_reset
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
                                .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))))
                        )
                    }

                    // Gradient overlay on photo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                    )

                    // Close Button + Category Tag
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = bulletin.category.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Multi-photo indicator dots if > 1
                    if (allPhotos.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(allPhotos.size) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(if (galleryPagerState.currentPage == idx) 8.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(if (galleryPagerState.currentPage == idx) Color.White else Color.White.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }

                // 2. Main Announcement Body & Details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = bulletin.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 30.sp
                    )

                    if (bulletin.subtitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bulletin.subtitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Author & Date metadata chip row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bulletin.author,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = bulletin.date,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Full Content Body
                    Text(
                        text = bulletin.content,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Reaction & Engagement Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like Button
                        Button(
                            onClick = onLikeClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bulletin.isLiked) Color(0xFFF43F5E).copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (bulletin.isLiked) Color(0xFFF43F5E) else Color.White.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_like_bulletin")
                        ) {
                            Icon(
                                if (bulletin.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (bulletin.isLiked) Color(0xFFF43F5E) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Like (${bulletin.likesCount})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Celebrate Button
                        Button(
                            onClick = onCelebrateClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bulletin.isCelebrated) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (bulletin.isCelebrated) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_celebrate_bulletin")
                        ) {
                            Icon(
                                Icons.Default.Celebration,
                                contentDescription = "Celebrate",
                                tint = if (bulletin.isCelebrated) Color(0xFFF59E0B) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Celebrate (${bulletin.celebrateCount})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Delete / Archive if HR
                        if (onDeleteClick != null) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFF43F5E).copy(alpha = 0.7f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Employee Comments & Acknowledgments Thread
                    Text(
                        text = "ACKNOWLEDGMENTS & COMMENTS (${bulletin.comments.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (bulletin.comments.isEmpty()) {
                        Text(
                            text = "Be the first team member to acknowledge or share thoughts!",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bulletin.comments.forEach { comment ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${comment.authorName} • ${comment.authorRole}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                        Text(
                                            text = comment.timestamp,
                                            fontSize = 10.sp,
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

                    // Comment input box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            placeholder = { Text("Write comment or note...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) },
                            modifier = Modifier.weight(1f).testTag("bulletin_comment_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        IconButton(
                            onClick = {
                                if (commentInput.isNotBlank()) {
                                    onAddComment(commentInput.trim())
                                    commentInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF38BDF8))
                                .testTag("btn_send_bulletin_comment")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * HR Creator Dialog to Post new Bulletin Cards with Photos
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
    var selectedCategory by remember { mutableStateOf("Wellness") }
    var selectedVisualPreset by remember { mutableStateOf("img_hr_mind_reset") }
    var customImageUrl by remember { mutableStateOf("") }
    var additionalPhotosCsv by remember { mutableStateOf("") }

    val categories = listOf("Calm", "Ritual", "Sleep", "Motion", "Health", "Fitness", "Milestone", "Culture", "Event", "Policy")
    val visualPresets = listOf(
        "img_hr_mind_reset" to "Mind Reset (Warm Editorial)",
        "img_hr_morning_flow" to "Morning Flow (Sunlight Glow)",
        "img_hr_team_summit" to "Team Summit (Executive Chic)"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .shadow(24.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PUBLISH HR BULLETIN POST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE2B774),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Create Visual Announcement",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Title Input
                Text("Post Headline / Title", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Mid-Day Wellness Circle", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth().testTag("input_bulletin_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Subtitle Input
                Text("Subtitle / Tagline", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    placeholder = { Text("e.g. Breathe, recharge, and refocus together", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth().testTag("input_bulletin_subtitle"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Selection
                Text("Category Pill Badge", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
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
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f))
                                .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Choose Cover Photo Visual Preset
                Text("Select Cover Photography", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    visualPresets.forEach { (resKey, label) ->
                        val isSelected = selectedVisualPreset == resKey && customImageUrl.isBlank()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
                                .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedVisualPreset = resKey
                                    customImageUrl = ""
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedVisualPreset = resKey
                                    customImageUrl = ""
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8), unselectedColor = Color.White.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }

                // Or Custom Photo URL
                Text("Or Custom Photo URL / Cloud Image", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                OutlinedTextField(
                    value = customImageUrl,
                    onValueChange = { customImageUrl = it },
                    placeholder = { Text("https://example.com/photo.jpg", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth().testTag("input_bulletin_custom_url"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Additional Gallery Photos
                Text("Additional Gallery Photo URLs (comma-separated)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                OutlinedTextField(
                    value = additionalPhotosCsv,
                    onValueChange = { additionalPhotosCsv = it },
                    placeholder = { Text("https://photo1.jpg, https://photo2.jpg", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Detailed Announcement Content
                Text("Full Announcement Body", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Write full update, schedule, directives or details...", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("input_bulletin_content"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (title.isBlank() || content.isBlank()) {
                                Toast.makeText(context, "Please provide a title and announcement content.", Toast.LENGTH_SHORT).show()
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_publish_bulletin")
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Publish Bulletin", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
