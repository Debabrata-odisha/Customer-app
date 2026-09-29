package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Category(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "icon_name") val iconName: String? = "build"
)

@JsonClass(generateAdapter = true)
data class Service(
    @Json(name = "id") val id: String,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "price") val price: Double,
    @Json(name = "duration_minutes") val durationMinutes: Int? = 60,
    @Json(name = "is_available") val isAvailable: Boolean = true,
    @Json(name = "image_url") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class Booking(
    @Json(name = "id") val id: String,
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "customer_email") val customerEmail: String? = null,
    @Json(name = "customer_phone") val customerPhone: String? = null,
    @Json(name = "service_id") val serviceId: String,
    @Json(name = "service_title") val serviceTitle: String,
    @Json(name = "status") val status: String = "Pending", // Pending, Accepted, Partner Assigned, On The Way, Started, Completed, Cancelled
    @Json(name = "scheduled_at") val scheduledAt: String,
    @Json(name = "address") val address: String,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "additional_instructions") val additionalInstructions: String? = null,
    @Json(name = "partner_name") val partnerName: String? = null,
    @Json(name = "partner_phone") val partnerPhone: String? = null,
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "payment_status") val paymentStatus: String = "Pending", // Pending, Paid, Failed
    @Json(name = "payment_method") val paymentMethod: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateBookingRequest(
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "customer_email") val customerEmail: String?,
    @Json(name = "customer_phone") val customerPhone: String?,
    @Json(name = "service_id") val serviceId: String,
    @Json(name = "service_title") val serviceTitle: String,
    @Json(name = "status") val status: String = "Pending",
    @Json(name = "scheduled_at") val scheduledAt: String,
    @Json(name = "address") val address: String,
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "additional_instructions") val additionalInstructions: String?,
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "payment_status") val paymentStatus: String = "Pending"
)

@JsonClass(generateAdapter = true)
data class UpdateBookingRequest(
    @Json(name = "status") val status: String? = null,
    @Json(name = "payment_status") val paymentStatus: String? = null,
    @Json(name = "payment_method") val paymentMethod: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentRecord(
    @Json(name = "id") val id: String? = null,
    @Json(name = "booking_id") val bookingId: String,
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "status") val status: String = "Paid",
    @Json(name = "transaction_ref") val transactionRef: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CustomerProfile(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "phone_number") val phoneNumber: String? = null,
    @Json(name = "default_address") val defaultAddress: String? = null,
    @Json(name = "default_lat") val defaultLat: Double? = null,
    @Json(name = "default_lng") val defaultLng: Double? = null
)

// Supabase Auth models
@JsonClass(generateAdapter = true)
data class AuthSignUpRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "data") val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class AuthSignInRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class AuthSessionResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "user") val user: AuthUser? = null
)

@JsonClass(generateAdapter = true)
data class AuthUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null
)
