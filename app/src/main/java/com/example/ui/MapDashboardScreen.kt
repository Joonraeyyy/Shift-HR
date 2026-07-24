package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LiquidThemeRegistry
import com.example.ui.viewmodel.TimeTrackerViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

enum class MapViewMode {
    MAP_3D,
    SATELLITE,
    NIGHT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDashboardScreen(
    viewModel: TimeTrackerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeTheme = LiquidThemeRegistry.getThemeByName(viewModel.selectedTheme.value)
    val isLightTheme = MaterialTheme.colorScheme.onBackground != Color(0xFFFFFFFF)
    
    // Theme colors adaptively mapped
    val cardBackground = activeTheme.cardSurface
    val cardBorderColor = activeTheme.cardBorder
    val primaryColor = activeTheme.primaryAccent
    val textPrimary = activeTheme.textPrimary
    val textSecondary = activeTheme.textSecondary

    // Local state for view modes
    var viewMode by remember { mutableStateOf(MapViewMode.MAP_3D) }

    // Check Maps API Key
    val mapsApiKey = try {
        com.example.BuildConfig.MAPS_API_KEY
    } catch (e: Exception) {
        ""
    }
    val isDummyMapsKey = mapsApiKey.isEmpty() || mapsApiKey.contains("DUMMY") || mapsApiKey == "your_api_key_here"
    var forceSimulatedMap by remember { mutableStateOf(isDummyMapsKey) }

    // Geofence & Location stats
    val geofenceRadius by viewModel.geofenceRadius
    val simulatedDistance by viewModel.simulatedDistance
    val officeLat by viewModel.officeLatitude
    val officeLng by viewModel.officeLongitude
    val officeName by viewModel.officeName

    val userRole by viewModel.currentUserRole
    val userName by viewModel.currentUserName

    val isHrAdmin = userRole == "ADMIN_HR"
    val isManager = userRole == "MANAGER" || userRole == "SUPERVISOR"
    val canViewOtherEmployees = isHrAdmin || isManager

    val coroutineScope = rememberCoroutineScope()

    // Calculated employee simulated position
    val distanceRatio = simulatedDistance.toDouble() / 111111.0
    val userLat = officeLat + distanceRatio * cos(Math.toRadians(45.0))
    val userLng = officeLng + (distanceRatio / cos(Math.toRadians(officeLat))) * sin(Math.toRadians(45.0))
    val userLocation = LatLng(userLat, userLng)
    val officeLocation = LatLng(officeLat, officeLng)

    // Load custom cyber night map style JSON for dark compliance theme aesthetics
    val customStyle = remember(context) {
        try {
            com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(context, com.example.R.raw.cyber_night_map)
        } catch (e: Exception) {
            null
        }
    }

    // Setup map properties based on View Mode selection
    val mapProperties = remember(viewMode, customStyle) {
        when (viewMode) {
            MapViewMode.SATELLITE -> MapProperties(
                isMyLocationEnabled = false,
                mapType = MapType.HYBRID,
                mapStyleOptions = null
            )
            MapViewMode.NIGHT -> MapProperties(
                isMyLocationEnabled = false,
                mapType = MapType.NORMAL,
                mapStyleOptions = customStyle
            )
            MapViewMode.MAP_3D -> MapProperties(
                isMyLocationEnabled = false,
                mapType = MapType.NORMAL,
                mapStyleOptions = customStyle,
                isBuildingEnabled = true
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(officeLocation, 17f)
    }

    // When office location updates, adjust camera position dynamically
    LaunchedEffect(officeLat, officeLng, viewMode) {
        val zoom = if (viewMode == MapViewMode.MAP_3D) 17.5f else 17f
        val tilt = if (viewMode == MapViewMode.MAP_3D) 60f else 0f
        val bearing = if (viewMode == MapViewMode.MAP_3D) 45f else 0f
        cameraPositionState.position = CameraPosition.builder()
            .target(officeLocation)
            .zoom(zoom)
            .tilt(tilt)
            .bearing(bearing)
            .build()
    }

    // Simulated other active employees surrounding the office
    val otherEmployees = remember(officeLat, officeLng) {
        listOf(
            Triple("Marcus Aurelius", "HR Intern", LatLng(officeLat + 0.0003, officeLng - 0.0002)),
            Triple("Robert Chen", "QA Tech", LatLng(officeLat - 0.0002, officeLng + 0.0003)),
            Triple("Anjali Sharma", "Dev Lead", LatLng(officeLat + 0.0001, officeLng + 0.0001)),
            Triple("Aditya Joshi", "Director", LatLng(officeLat - 0.0004, officeLng - 0.0003))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(activeTheme.bgGradientStart, activeTheme.bgGradientEnd)
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Geofence Intelligence",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Role: $userRole | Location: $officeName",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("map_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary
                            )
                        }
                    },
                    actions = {
                        // Toggle simulated map view
                        TextButton(
                            onClick = { forceSimulatedMap = !forceSimulatedMap }
                        ) {
                            Text(
                                text = if (forceSimulatedMap) "USE GOOGLE MAP" else "USE CYBER RADAR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Compact Operational Status Indicator
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = cardBackground.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OPERATIONAL NODE: ${officeName.uppercase()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = if (isHrAdmin) "HR/ADMIN REROUTING ACTIVE" else if (isManager) "MANAGER APPROVAL ACTIVE" else "VIEW ONLY NODE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }

            // Map Controller (3D Map, Satellite View, Night Mode Selection)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(textSecondary.copy(alpha = 0.05f))
                    .border(BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(
                    MapViewMode.MAP_3D to "3D Perspective",
                    MapViewMode.SATELLITE to "Satellite Overlay",
                    MapViewMode.NIGHT to "Blueprint Night"
                ).forEach { (mode, label) ->
                    val isSelected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) primaryColor.copy(alpha = 0.4f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewMode = mode }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.uppercase(),
                            color = if (isSelected) textPrimary else textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // MAIN MAP CARD CONTAINER (Expanded Hero Element ~60% Height)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, cardBorderColor),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!forceSimulatedMap && !isDummyMapsKey) {
                        // REAL GOOGLE MAPS IMPLEMENTATION
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = mapProperties,
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                compassEnabled = true,
                                myLocationButtonEnabled = false,
                                mapToolbarEnabled = false
                            ),
                            onMapClick = { latLng ->
                                if (isHrAdmin) {
                                    viewModel.pendingRerouteLatitude.value = latLng.latitude
                                    viewModel.pendingRerouteLongitude.value = latLng.longitude
                                    viewModel.pendingRerouteName.value = "Pinned Location (${String.format("%.4f", latLng.latitude)}, ${String.format("%.4f", latLng.longitude)})"
                                    viewModel.pendingRerouteRequestedBy.value = userName
                                    viewModel.addAuditLog(userName, "PROPOSED pinpoint tap reroute to: (${String.format("%.4f", latLng.latitude)}, ${String.format("%.4f", latLng.longitude)})")
                                    Toast.makeText(context, "New pinpoint location proposed! Awaiting Manager permission to update.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "🔒 Pinpoint location editing is restricted to HR/Admin with Manager approval.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            // Geofence circle
                            Circle(
                                center = officeLocation,
                                radius = geofenceRadius.toDouble(),
                                fillColor = primaryColor.copy(alpha = if (viewMode == MapViewMode.NIGHT) 0.1f else 0.2f),
                                strokeColor = primaryColor,
                                strokeWidth = 4f
                            )

                            // Central office pin
                            Marker(
                                state = MarkerState(position = officeLocation),
                                title = officeName,
                                snippet = "Secure Center (Geofence: ${geofenceRadius.toInt()}m)"
                            )

                            // Current User position marker with user photo avatar
                            val userProfile = viewModel.employeeProfiles.value.find { it.name.equals(userName, ignoreCase = true) }
                            MarkerComposable(
                                keys = arrayOf(userLocation, userProfile?.picture ?: ""),
                                state = MarkerState(position = userLocation),
                                title = "$userName (YOU)",
                                snippet = "Distance: ${simulatedDistance.toInt()}m from center"
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor)
                                        .border(2.5.dp, Color.White, CircleShape)
                                        .padding(2.5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!userProfile?.picture.isNullOrEmpty()) {
                                        coil.compose.AsyncImage(
                                            model = userProfile?.picture,
                                            contentDescription = userName,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(primaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = userName.take(2).uppercase(),
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Plot other employees ONLY if manager/HR role
                            if (canViewOtherEmployees) {
                                otherEmployees.forEach { (empName, role, loc) ->
                                    Marker(
                                        state = MarkerState(position = loc),
                                        title = empName,
                                        snippet = role
                                    )
                                }
                            }
                        }
                    } else {
                        // CYBER GRID / RADAR FALLBACK BEAUTIFUL SIMULATED MAP
                        // This serves as an amazing fallback showing high-fidelity graphics
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (viewMode == MapViewMode.NIGHT) Color(0xFF0F172A)
                                    else if (viewMode == MapViewMode.SATELLITE) Color(0xFF0F251E)
                                    else Color(0xFF1E293B)
                                )
                        ) {
                            // Ambient grid and radar pulse background
                            val infiniteTransition = rememberInfiniteTransition(label = "RadarRadar")
                            val pulseRadius by infiniteTransition.animateFloat(
                                initialValue = 0.1f,
                                targetValue = 1.8f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(4000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "PulseRadius"
                            )
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 0.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(4000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "PulseAlpha"
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2, size.height / 2)
                                val step = 40.dp.toPx()

                                // Grid Lines
                                val gridColor = when (viewMode) {
                                    MapViewMode.NIGHT -> Color(0xFF334155).copy(alpha = 0.25f)
                                    MapViewMode.SATELLITE -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    else -> Color(0xFFE2E8F0).copy(alpha = 0.15f)
                                }

                                var y = 0f
                                while (y < size.height) {
                                    drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                                    y += step
                                }
                                var x = 0f
                                while (x < size.width) {
                                    drawLine(gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                                    x += step
                                }

                                // Satellite-view styled contours
                                if (viewMode == MapViewMode.SATELLITE) {
                                    drawCircle(
                                        color = Color(0xFF064E3B).copy(alpha = 0.2f),
                                        radius = 200.dp.toPx(),
                                        center = center
                                    )
                                    drawCircle(
                                        color = Color(0xFF065F46).copy(alpha = 0.1f),
                                        radius = 120.dp.toPx(),
                                        center = center + Offset(80f, -100f)
                                    )
                                }

                                // Dynamic Geofence radius circle visualization
                                val scaleFactor = (geofenceRadius / 1000f) * 150.dp.toPx() + 40.dp.toPx()
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.12f),
                                    radius = scaleFactor,
                                    center = center
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = scaleFactor,
                                    center = center,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                )

                                // Animated Radar Pulse
                                drawCircle(
                                    color = primaryColor.copy(alpha = pulseAlpha),
                                    radius = scaleFactor * pulseRadius,
                                    center = center,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                )
                            }

                            // Center Hub Pin Point Icon (HQ Location)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(y = (-12).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "HQ PIN",
                                    tint = primaryColor,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .graphicsLayer {
                                            if (viewMode == MapViewMode.MAP_3D) {
                                                rotationX = 30f
                                                scaleY = 1.1f
                                            }
                                        }
                                )
                                Badge(
                                    containerColor = Color.Black,
                                    contentColor = Color.White,
                                    modifier = Modifier.offset(y = (-25).dp)
                                ) {
                                    Text(
                                        text = "PIN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }

                            // Plot current employee (Sarah Jenkins / YOU) using circular avatar or photo marker
                            val userProfile = viewModel.employeeProfiles.value.find { it.name.equals(userName, ignoreCase = true) }
                            userProfile?.let { profile ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        // Offset visually based on simulated distance
                                        .offset(
                                            x = 65.dp,
                                            y = (-45).dp
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor)
                                            .border(2.dp, Color.White, CircleShape)
                                            .padding(2.dp)
                                    ) {
                                        if (profile.picture.isNotEmpty()) {
                                            coil.compose.AsyncImage(
                                                model = profile.picture,
                                                contentDescription = "User Avatar",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Gray),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = profile.name.take(2).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                    // Label
                                    Badge(
                                        containerColor = primaryColor,
                                        contentColor = Color.Black,
                                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = 12.dp)
                                    ) {
                                        Text(
                                            text = "YOU",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(horizontal = 3.dp)
                                        )
                                    }
                                }
                            }

                            // Plot other employees surrounding the office with their photo avatars ONLY if HR/Manager
                            if (canViewOtherEmployees) {
                                val sampleAvatars = listOf(
                                    Triple("Marcus", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=100&q=80", Offset(-80f, 60f)),
                                    Triple("Robert", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=100&q=80", Offset(120f, 80f)),
                                    Triple("Anjali", "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=100&q=80", Offset(-110f, -70f)),
                                    Triple("Aditya", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=100&q=80", Offset(-40f, 130f))
                                )

                                sampleAvatars.forEach { avatar ->
                                    val name = avatar.first
                                    val imgUrl = avatar.second
                                    val offset = avatar.third

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .offset(x = offset.x.dp, y = offset.y.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF334155))
                                                .border(1.5.dp, primaryColor.copy(alpha = 0.8f), CircleShape)
                                                .padding(1.5.dp)
                                        ) {
                                            coil.compose.AsyncImage(
                                                model = imgUrl,
                                                contentDescription = name,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Badge(
                                            containerColor = Color(0xFF1E293B),
                                            contentColor = textPrimary,
                                            modifier = Modifier.align(Alignment.BottomCenter).offset(y = 10.dp)
                                        ) {
                                            Text(
                                                text = name,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Map Compass visualizer
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Compass",
                                tint = primaryColor.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .size(32.dp)
                                    .rotate(35f)
                            )

                            // Status Indicator Footer inside the visual canvas
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CYBER-RADAR SIMULATION ACTIVE",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Floating LOCATE ME & CURVY ZOOM CONTROLS COLUMN
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Locate Me / Center on Me Tool Button
                        FloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(userLocation, 18.5f)),
                                        durationMs = 800
                                    )
                                }
                                Toast.makeText(context, "Directing & zooming in to $userName's location!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("locate_me_button"),
                            shape = CircleShape,
                            containerColor = primaryColor,
                            contentColor = Color.Black
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Zoom into my location",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 2. Curvy Styled Zoom In & Zoom Out Buttons Group
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                            shadowElevation = 6.dp
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            cameraPositionState.animate(CameraUpdateFactory.zoomIn(), durationMs = 300)
                                        }
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Zoom In",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.width(24.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    thickness = 1.dp
                                )

                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            cameraPositionState.animate(CameraUpdateFactory.zoomOut(), durationMs = 300)
                                        }
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Zoom Out",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // GEOFENCE RANGE METRIC & SLIDER CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, cardBorderColor),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GEOFENCE RANGE: ${geofenceRadius.toInt()}m",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (isHrAdmin) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = primaryColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Edit Range",
                                    color = primaryColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = textSecondary.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "HR/Admin Only",
                                    color = textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = geofenceRadius,
                        onValueChange = { if (isHrAdmin) viewModel.geofenceRadius.value = it },
                        valueRange = 1f..1000f,
                        enabled = isHrAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("geofence_range_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = if (isHrAdmin) primaryColor else textSecondary.copy(alpha = 0.3f),
                            activeTrackColor = if (isHrAdmin) primaryColor else textSecondary.copy(alpha = 0.2f),
                            inactiveTrackColor = textSecondary.copy(alpha = 0.2f),
                            disabledThumbColor = textSecondary.copy(alpha = 0.3f),
                            disabledActiveTrackColor = textSecondary.copy(alpha = 0.2f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Min: 1m", fontSize = 10.sp, color = textSecondary)
                        Text("Current: ${geofenceRadius.toInt()}m", fontSize = 10.sp, color = if (isHrAdmin) textPrimary else textSecondary, fontWeight = FontWeight.SemiBold)
                        Text("Max: 1000m", fontSize = 10.sp, color = textSecondary)
                    }

                    if (!isHrAdmin) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "🔒 Geofence boundary radius can only be edited by HR/Admin.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF43F5E)
                        )
                    }
                }
            }

            // REROUTE PINPOINT CONTROLLER CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, cardBorderColor),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HUB PINPOINT REROUTING CONTROL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Change corporate core coordinate center and geofencing validation origin.",
                        fontSize = 11.sp,
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val pendingLat by viewModel.pendingRerouteLatitude
                    val pendingLng by viewModel.pendingRerouteLongitude
                    val pendingName by viewModel.pendingRerouteName

                    if (pendingLat != null && pendingLng != null) {
                        // PENDING APPROVAL ALERT
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFD97706).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFD97706).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = "Pending Approval",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "REROUTE APPROVAL PENDING",
                                        color = Color(0xFFD97706),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Proposed New Hub: $pendingName",
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Coordinates: (${String.format("%.4f", pendingLat)}, ${String.format("%.4f", pendingLng)})",
                                    color = textSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Requested by HR: ${viewModel.pendingRerouteRequestedBy.value ?: "Sarah Jenkins"}",
                                    color = textSecondary,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Approver Role Actions
                                if (userRole == "MANAGER" || userRole == "SUPERVISOR") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                // APPROVE REQUEST
                                                viewModel.officeLatitude.value = pendingLat!!
                                                viewModel.officeLongitude.value = pendingLng!!
                                                viewModel.officeName.value = pendingName ?: "Manually Rerouted Hub"
                                                
                                                viewModel.addAuditLog(userName, "APPROVED pinpoint reroute to: ${pendingName ?: "Manually Rerouted Hub"}")
                                                Toast.makeText(context, "Pinpoint Reroute Approved & Updated!", Toast.LENGTH_LONG).show()

                                                viewModel.pendingRerouteLatitude.value = null
                                                viewModel.pendingRerouteLongitude.value = null
                                                viewModel.pendingRerouteName.value = null
                                                viewModel.pendingRerouteRequestedBy.value = null
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("APPROVE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                // REJECT REQUEST
                                                viewModel.addAuditLog(userName, "REJECTED proposed pinpoint reroute to: $pendingName")
                                                Toast.makeText(context, "Proposed Reroute Rejected.", Toast.LENGTH_SHORT).show()

                                                viewModel.pendingRerouteLatitude.value = null
                                                viewModel.pendingRerouteLongitude.value = null
                                                viewModel.pendingRerouteName.value = null
                                                viewModel.pendingRerouteRequestedBy.value = null
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("REJECT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "⚠️ Waiting for supervisor/manager role authorization. Please switch roles to Manager to approve or reject.",
                                        color = Color(0xFFD97706),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 13.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // NO PENDING PROPOSAL -> REROUTE PROPOSE BUTTONS
                        if (userRole == "ADMIN_HR") {
                            Text(
                                text = "Propose hub center update (Select Target Headquarters):",
                                fontSize = 12.sp,
                                color = textSecondary,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            val presets = listOf(
                                Triple("Silicon Valley Hub", 37.4220, -122.0841),
                                Triple("Indore Innovation HQ", 22.7196, 75.8577),
                                Triple("Manila Core Center", 14.5995, 120.9842),
                                Triple("London Business Annex", 51.5074, -0.1278)
                            )

                            presets.forEach { (name, lat, lng) ->
                                val isCurrent = officeLat == lat && officeLng == lng
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) primaryColor.copy(alpha = 0.08f) else textSecondary.copy(alpha = 0.03f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isCurrent) primaryColor.copy(alpha = 0.4f) else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            viewModel.pendingRerouteLatitude.value = lat
                                            viewModel.pendingRerouteLongitude.value = lng
                                            viewModel.pendingRerouteName.value = name
                                            viewModel.pendingRerouteRequestedBy.value = userName
                                            
                                            viewModel.addAuditLog(userName, "PROPOSED pinpoint reroute to: $name")
                                            Toast.makeText(context, "Proposed $name reroute. Request sent to Manager!", Toast.LENGTH_LONG).show()
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                                        Text("Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}", fontSize = 10.sp, color = textSecondary)
                                    }
                                    if (isCurrent) {
                                        Badge(containerColor = primaryColor, contentColor = Color.Black) {
                                            Text("ACTIVE", fontWeight = FontWeight.Bold, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                        }
                                    } else {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Propose", tint = textSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        } else {
                            // Non HR View
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(textSecondary.copy(alpha = 0.04f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Only HR/Admin accounts (ADMIN_HR role) can initiate office center reroutes. To test the rerouting authorization pipeline, please change your active profile/role to HR/Admin.",
                                    color = textSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
