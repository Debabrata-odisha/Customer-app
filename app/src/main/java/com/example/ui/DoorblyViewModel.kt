package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.location.DeviceLocation
import com.example.data.location.LocationHelper
import com.example.data.model.Booking
import com.example.data.model.Category
import com.example.data.model.CreateBookingRequest
import com.example.data.model.CustomerProfile
import com.example.data.model.PaymentRecord
import com.example.data.model.Service
import com.example.data.repository.SupabaseRepository
import com.example.data.supabase.SupabaseClientProvider
import com.example.data.supabase.SupabaseConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class DoorblyUiState(
    val isConfigured: Boolean = false,
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val isAuthenticated: Boolean = false,
    val customerProfile: CustomerProfile? = null,
    val deviceLocation: DeviceLocation? = null,
    val locationPermissionGranted: Boolean = false,
    val isLocating: Boolean = false,
    val categories: List<Category> = emptyList(),
    val services: List<Service> = emptyList(),
    val selectedCategory: Category? = null,
    val searchQuery: String = "",
    val myBookings: List<Booking> = emptyList(),
    val activeBooking: Booking? = null,
    val selectedBooking: Booking? = null,
    val selectedService: Service? = null,
    val isLoading: Boolean = false,
    val isAuthLoading: Boolean = false,
    val isBookingLoading: Boolean = false,
    val isPaymentLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class DoorblyViewModel(application: Application) : AndroidViewModel(application) {

    val config = SupabaseConfig(application)
    val provider = SupabaseClientProvider(config)
    val repository = SupabaseRepository(config, provider)
    val locationHelper = LocationHelper(application)

    private val _uiState = MutableStateFlow(
        DoorblyUiState(
            isConfigured = config.isConfigured,
            supabaseUrl = config.projectUrl,
            supabaseAnonKey = config.anonKey,
            isAuthenticated = config.isAuthenticated,
            customerProfile = if (config.isAuthenticated && config.userId != null) {
                CustomerProfile(
                    id = config.userId!!,
                    email = config.userEmail,
                    fullName = config.userName ?: "Doorbly Customer",
                    phoneNumber = config.userPhone
                )
            } else null
        )
    )
    val uiState: StateFlow<DoorblyUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        checkLocationPermissionAndFetch()
        if (config.isConfigured) {
            provider.realtimeClient.connect()
            observeRealtimeUpdates()
            startPeriodicStatusPolling()
            loadCategories()
            loadServices()
            if (config.isAuthenticated) {
                loadProfile()
                loadBookings()
            }
        }
    }

    fun checkLocationPermissionAndFetch() {
        val hasPerm = locationHelper.hasLocationPermission()
        _uiState.update { it.copy(locationPermissionGranted = hasPerm) }
        if (hasPerm) {
            refreshLocation()
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true) }
            val loc = locationHelper.getCurrentLocation()
            _uiState.update {
                it.copy(
                    deviceLocation = loc,
                    isLocating = false
                )
            }
        }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(locationPermissionGranted = granted) }
        if (granted) {
            refreshLocation()
        }
    }

    fun saveSupabaseConfig(url: String, key: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            config.projectUrl = url.trim()
            config.anonKey = key.trim()
            provider.resetClient()

            val testResult = repository.testConnection()
            if (testResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        isConfigured = true,
                        supabaseUrl = config.projectUrl,
                        supabaseAnonKey = config.anonKey,
                        isLoading = false,
                        infoMessage = "Connected to Supabase successfully!"
                    )
                }
                provider.realtimeClient.connect()
                observeRealtimeUpdates()
                startPeriodicStatusPolling()
                loadCategories()
                loadServices()
                if (config.isAuthenticated) {
                    loadBookings()
                }
                onComplete?.invoke(true, "Connected successfully!")
            } else {
                val err = testResult.exceptionOrNull()?.message ?: "Connection failed"
                _uiState.update {
                    it.copy(
                        isConfigured = config.isConfigured,
                        isLoading = false,
                        errorMessage = err
                    )
                }
                onComplete?.invoke(false, err)
            }
        }
    }

    fun testSupabaseConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.testConnection()
            if (res.isSuccess) {
                onResult(true, res.getOrNull().orEmpty())
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Connection test failed")
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            val res = repository.getCategories()
            if (res.isSuccess) {
                _uiState.update { it.copy(categories = res.getOrDefault(emptyList())) }
            } else {
                // If table doesn't exist or network error
                _uiState.update {
                    it.copy(errorMessage = res.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun loadServices(categoryId: String? = _uiState.value.selectedCategory?.id, search: String? = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.getServices(categoryId, search)
            _uiState.update {
                it.copy(
                    services = res.getOrDefault(emptyList()),
                    isLoading = false,
                    errorMessage = if (res.isFailure) res.exceptionOrNull()?.message else it.errorMessage
                )
            }
        }
    }

    fun selectCategory(category: Category?) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadServices(categoryId = category?.id, search = _uiState.value.searchQuery)
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadServices(categoryId = _uiState.value.selectedCategory?.id, search = query)
    }

    fun selectService(service: Service?) {
        _uiState.update { it.copy(selectedService = service) }
    }

    fun selectBooking(booking: Booking?) {
        _uiState.update { it.copy(selectedBooking = booking) }
    }

    fun loadBookings() {
        val userId = config.userId ?: return
        viewModelScope.launch {
            val res = repository.getBookings(userId)
            if (res.isSuccess) {
                val list = res.getOrDefault(emptyList())
                val active = list.firstOrNull { b ->
                    b.status in listOf("Pending", "Accepted", "Partner Assigned", "On The Way", "Started")
                }
                _uiState.update {
                    it.copy(
                        myBookings = list,
                        activeBooking = active,
                        selectedBooking = list.find { b -> b.id == it.selectedBooking?.id } ?: it.selectedBooking
                    )
                }
            }
        }
    }

    fun loadProfile() {
        val userId = config.userId ?: return
        viewModelScope.launch {
            val res = repository.getCustomerProfile(userId)
            if (res.isSuccess) {
                _uiState.update { it.copy(customerProfile = res.getOrNull()) }
            }
        }
    }

    fun updateProfile(fullName: String, phone: String, defaultAddress: String?, onResult: (Boolean, String) -> Unit) {
        val userId = config.userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val updated = CustomerProfile(
                id = userId,
                email = config.userEmail,
                fullName = fullName.trim(),
                phoneNumber = phone.trim(),
                defaultAddress = defaultAddress?.trim(),
                defaultLat = _uiState.value.deviceLocation?.latitude,
                defaultLng = _uiState.value.deviceLocation?.longitude
            )
            val res = repository.updateCustomerProfile(updated)
            _uiState.update { it.copy(isLoading = false) }
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        customerProfile = res.getOrNull(),
                        infoMessage = "Profile updated successfully"
                    )
                }
                onResult(true, "Profile updated")
            } else {
                val msg = res.exceptionOrNull()?.message ?: "Failed to update profile"
                _uiState.update { it.copy(errorMessage = msg) }
                onResult(false, msg)
            }
        }
    }

    fun createBooking(
        service: Service,
        scheduledAt: String,
        address: String,
        latitude: Double?,
        longitude: Double?,
        additionalInstructions: String?,
        onSuccess: (Booking) -> Unit,
        onError: (String) -> Unit
    ) {
        val customerId = config.userId
        if (customerId == null) {
            onError("Please sign in to confirm your booking.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBookingLoading = true, errorMessage = null) }

            val req = CreateBookingRequest(
                customerId = customerId,
                customerName = _uiState.value.customerProfile?.fullName ?: config.userName ?: "Customer",
                customerEmail = _uiState.value.customerProfile?.email ?: config.userEmail,
                customerPhone = _uiState.value.customerProfile?.phoneNumber ?: config.userPhone,
                serviceId = service.id,
                serviceTitle = service.title,
                status = "Pending",
                scheduledAt = scheduledAt,
                address = address,
                latitude = latitude,
                longitude = longitude,
                additionalInstructions = additionalInstructions,
                totalAmount = service.price,
                paymentStatus = "Pending"
            )

            val res = repository.createBooking(req)
            _uiState.update { it.copy(isBookingLoading = false) }

            if (res.isSuccess) {
                val booking = res.getOrNull()!!
                loadBookings()
                _uiState.update {
                    it.copy(
                        selectedBooking = booking,
                        infoMessage = "Booking confirmed! A partner will be assigned shortly."
                    )
                }
                onSuccess(booking)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Failed to create booking"
                _uiState.update { it.copy(errorMessage = err) }
                onError(err)
            }
        }
    }

    fun cancelBooking(bookingId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.updateBooking(bookingId, status = "Cancelled")
            _uiState.update { it.copy(isLoading = false) }
            if (res.isSuccess) {
                loadBookings()
                onResult(true, "Booking cancelled")
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed to cancel")
            }
        }
    }

    fun processPayment(
        booking: Booking,
        paymentMethod: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val customerId = config.userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPaymentLoading = true) }

            val payment = PaymentRecord(
                bookingId = booking.id,
                customerId = customerId,
                amount = booking.totalAmount,
                paymentMethod = paymentMethod,
                status = "Paid",
                transactionRef = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"
            )

            val res = repository.recordPayment(payment)
            _uiState.update { it.copy(isPaymentLoading = false) }

            if (res.isSuccess) {
                loadBookings()
                // Update selected booking
                _uiState.update { state ->
                    val updated = state.selectedBooking?.copy(
                        paymentStatus = "Paid",
                        paymentMethod = paymentMethod
                    )
                    state.copy(
                        selectedBooking = updated,
                        infoMessage = "Payment successful! Thank you for using Doorbly."
                    )
                }
                onSuccess()
            } else {
                val err = res.exceptionOrNull()?.message ?: "Payment processing failed"
                onError(err)
            }
        }
    }

    fun signIn(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, errorMessage = null) }
            val res = repository.signIn(email, pass)
            _uiState.update { it.copy(isAuthLoading = false) }
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        customerProfile = CustomerProfile(
                            id = config.userId!!,
                            email = config.userEmail,
                            fullName = config.userName ?: "Doorbly Customer",
                            phoneNumber = config.userPhone
                        ),
                        infoMessage = "Welcome back, ${config.userName ?: "Customer"}!"
                    )
                }
                loadProfile()
                loadBookings()
                onResult(true, "Signed in successfully")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Authentication failed"
                _uiState.update { it.copy(errorMessage = err) }
                onResult(false, err)
            }
        }
    }

    fun signUp(email: String, pass: String, fullName: String, phone: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, errorMessage = null) }
            val res = repository.signUp(email, pass, fullName, phone)
            _uiState.update { it.copy(isAuthLoading = false) }
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isAuthenticated = config.isAuthenticated,
                        customerProfile = CustomerProfile(
                            id = config.userId ?: "",
                            email = email,
                            fullName = fullName,
                            phoneNumber = phone
                        ),
                        infoMessage = "Account created successfully!"
                    )
                }
                if (config.isAuthenticated) {
                    loadBookings()
                }
                onResult(true, "Registration successful")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Sign up failed"
                _uiState.update { it.copy(errorMessage = err) }
                onResult(false, err)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    customerProfile = null,
                    myBookings = emptyList(),
                    activeBooking = null,
                    selectedBooking = null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    private fun observeRealtimeUpdates() {
        viewModelScope.launch {
            repository.realtimeUpdates.collect { update ->
                // When any booking changes in Supabase realtime, refresh bookings
                loadBookings()
                if (_uiState.value.selectedBooking?.id == update.bookingId) {
                    val bookingId = update.bookingId ?: return@collect
                    val freshRes = repository.getBookingById(bookingId)
                    if (freshRes.isSuccess) {
                        _uiState.update { it.copy(selectedBooking = freshRes.getOrNull()) }
                    }
                }
            }
        }
    }

    private fun startPeriodicStatusPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(12000)
                if (config.isAuthenticated && _uiState.value.isConfigured) {
                    loadBookings()
                    val selected = _uiState.value.selectedBooking
                    if (selected != null && selected.status != "Completed" && selected.status != "Cancelled") {
                        val fresh = repository.getBookingById(selected.id)
                        if (fresh.isSuccess) {
                            _uiState.update { it.copy(selectedBooking = fresh.getOrNull()) }
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        provider.realtimeClient.disconnect()
        pollJob?.cancel()
    }
}
