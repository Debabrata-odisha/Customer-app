package com.example.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MiscellaneousServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Booking
import com.example.data.model.Service
import com.example.ui.screens.AuthDialog
import com.example.ui.screens.BookingDetailScreen
import com.example.ui.screens.BookingScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyBookingsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ServiceDetailScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.SupabaseSetupDialog
import com.example.ui.theme.DoorblyTeal

enum class AppDestination(val route: String, val label: String) {
    HOME("home", "Home"),
    SERVICES("services", "Services"),
    BOOKINGS("bookings", "My Bookings"),
    PROFILE("profile", "Profile")
}

@Composable
fun DoorblyApp(viewModel: DoorblyViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    var currentSubScreen by remember { mutableStateOf<String?>(null) } // "service_detail", "create_booking", "booking_detail"

    var showAuthDialog by remember { mutableStateOf(false) }
    var showSetupDialog by remember { mutableStateOf(false) }

    // Android Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.setLocationPermissionGranted(granted)
        if (granted) {
            Toast.makeText(context, "Location permission enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location access denied. Please enter address manually.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentSubScreen == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    AppDestination.values().forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                currentDestination = destination
                                currentSubScreen = null
                            },
                            icon = {
                                Icon(
                                    imageVector = when (destination) {
                                        AppDestination.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                                        AppDestination.SERVICES -> if (isSelected) Icons.Filled.MiscellaneousServices else Icons.Outlined.MiscellaneousServices
                                        AppDestination.BOOKINGS -> if (isSelected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
                                        AppDestination.PROFILE -> if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
                                    },
                                    contentDescription = destination.label
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DoorblyTeal,
                                selectedTextColor = DoorblyTeal,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_${destination.route}")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentSubScreen) {
                "service_detail" -> {
                    val service = uiState.selectedService
                    if (service != null) {
                        ServiceDetailScreen(
                            service = service,
                            onBack = { currentSubScreen = null },
                            onBookNow = {
                                currentSubScreen = "create_booking"
                            }
                        )
                    } else {
                        currentSubScreen = null
                    }
                }

                "create_booking" -> {
                    val service = uiState.selectedService
                    if (service != null) {
                        BookingScreen(
                            service = service,
                            state = uiState,
                            onBack = { currentSubScreen = "service_detail" },
                            onRequestAuth = { showAuthDialog = true },
                            onRequestLocation = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            onConfirmBooking = { s, scheduledAt, address, lat, lng, instructions, onSuccess ->
                                viewModel.createBooking(
                                    service = s,
                                    scheduledAt = scheduledAt,
                                    address = address,
                                    latitude = lat,
                                    longitude = lng,
                                    additionalInstructions = instructions,
                                    onSuccess = onSuccess,
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            onBookingSuccess = { createdBooking ->
                                viewModel.selectBooking(createdBooking)
                                currentSubScreen = "booking_detail"
                            }
                        )
                    } else {
                        currentSubScreen = null
                    }
                }

                "booking_detail" -> {
                    val booking = uiState.selectedBooking
                    if (booking != null) {
                        BookingDetailScreen(
                            booking = booking,
                            isPaymentLoading = uiState.isPaymentLoading,
                            onBack = { currentSubScreen = null },
                            onCancelBooking = { bookingId ->
                                viewModel.cancelBooking(bookingId) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onProcessPayment = { b, method ->
                                viewModel.processPayment(
                                    booking = b,
                                    paymentMethod = method,
                                    onSuccess = {
                                        Toast.makeText(context, "Payment successful!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        )
                    } else {
                        currentSubScreen = null
                    }
                }

                else -> {
                    // Main Tabs
                    when (currentDestination) {
                        AppDestination.HOME -> {
                            HomeScreen(
                                state = uiState,
                                onLocationClick = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onSetupClick = { showSetupDialog = true },
                                onSearchChange = { query -> viewModel.setSearchQuery(query) },
                                onSelectCategory = { cat -> viewModel.selectCategory(cat) },
                                onSelectService = { service ->
                                    viewModel.selectService(service)
                                    currentSubScreen = "service_detail"
                                },
                                onSelectBooking = { booking ->
                                    viewModel.selectBooking(booking)
                                    currentSubScreen = "booking_detail"
                                },
                                onViewAllServices = {
                                    currentDestination = AppDestination.SERVICES
                                },
                                onRequestLocationPermission = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            )
                        }

                        AppDestination.SERVICES -> {
                            ServicesScreen(
                                state = uiState,
                                onSearchChange = { query -> viewModel.setSearchQuery(query) },
                                onSelectCategory = { cat -> viewModel.selectCategory(cat) },
                                onSelectService = { service ->
                                    viewModel.selectService(service)
                                    currentSubScreen = "service_detail"
                                }
                            )
                        }

                        AppDestination.BOOKINGS -> {
                            MyBookingsScreen(
                                state = uiState,
                                onSelectBooking = { booking ->
                                    viewModel.selectBooking(booking)
                                    currentSubScreen = "booking_detail"
                                },
                                onRefresh = { viewModel.loadBookings() },
                                onRequestAuth = { showAuthDialog = true },
                                onExploreServices = { currentDestination = AppDestination.SERVICES }
                            )
                        }

                        AppDestination.PROFILE -> {
                            ProfileScreen(
                                state = uiState,
                                onRequestAuth = { showAuthDialog = true },
                                onSignOut = { viewModel.signOut() },
                                onOpenSetup = { showSetupDialog = true },
                                onUpdateProfile = { name, phone, address ->
                                    viewModel.updateProfile(name, phone, address) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Auth Dialog
    AuthDialog(
        isOpen = showAuthDialog,
        isLoading = uiState.isAuthLoading,
        errorMessage = uiState.errorMessage,
        onDismiss = { showAuthDialog = false },
        onSignIn = { email, pass ->
            viewModel.signIn(email, pass) { success, msg ->
                if (success) showAuthDialog = false
            }
        },
        onSignUp = { email, pass, name, phone ->
            viewModel.signUp(email, pass, name, phone) { success, msg ->
                if (success) showAuthDialog = false
            }
        }
    )

    // Supabase Setup Dialog
    SupabaseSetupDialog(
        isOpen = showSetupDialog,
        currentUrl = uiState.supabaseUrl,
        currentKey = uiState.supabaseAnonKey,
        onDismiss = { showSetupDialog = false },
        onSave = { url, key ->
            viewModel.saveSupabaseConfig(url, key) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        },
        onTest = { callback ->
            viewModel.testSupabaseConnection(callback)
        }
    )
}
