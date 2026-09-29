package com.example.data.supabase

import com.example.data.model.AuthSessionResponse
import com.example.data.model.AuthSignInRequest
import com.example.data.model.AuthSignUpRequest
import com.example.data.model.AuthUser
import com.example.data.model.Booking
import com.example.data.model.Category
import com.example.data.model.CreateBookingRequest
import com.example.data.model.CustomerProfile
import com.example.data.model.PaymentRecord
import com.example.data.model.Service
import com.example.data.model.UpdateBookingRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApi {

    // --- AUTHENTICATION ---

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body body: AuthSignUpRequest
    ): Response<AuthSessionResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Body body: AuthSignInRequest
    ): Response<AuthSessionResponse>

    @GET("auth/v1/user")
    suspend fun getUser(): Response<AuthUser>

    @POST("auth/v1/logout")
    suspend fun signOut(): Response<ResponseBody>

    // --- CATEGORIES & SERVICES ---

    @GET("rest/v1/service_categories?select=*&order=name.asc")
    suspend fun getCategories(): Response<List<Category>>

    @GET("rest/v1/services?select=*&order=title.asc")
    suspend fun getServices(
        @Query("category_id") categoryFilter: String? = null,
        @Query("title") titleQuery: String? = null
    ): Response<List<Service>>

    @GET("rest/v1/services")
    suspend fun getServiceById(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Service>>

    // --- BOOKINGS ---

    @GET("rest/v1/bookings")
    suspend fun getBookings(
        @Query("customer_id") customerIdFilter: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<Booking>>

    @GET("rest/v1/bookings")
    suspend fun getBookingById(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Booking>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/bookings")
    suspend fun createBooking(
        @Body booking: CreateBookingRequest
    ): Response<List<Booking>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/bookings")
    suspend fun updateBooking(
        @Query("id") idFilter: String,
        @Body update: UpdateBookingRequest
    ): Response<List<Booking>>

    // --- PAYMENTS ---

    @Headers("Prefer: return=representation")
    @POST("rest/v1/payments")
    suspend fun recordPayment(
        @Body payment: PaymentRecord
    ): Response<List<PaymentRecord>>

    @GET("rest/v1/payments")
    suspend fun getPaymentsForBooking(
        @Query("booking_id") bookingIdFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<PaymentRecord>>

    // --- PROFILES ---

    @GET("rest/v1/customer_profiles")
    suspend fun getCustomerProfile(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<CustomerProfile>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/customer_profiles")
    suspend fun upsertCustomerProfile(
        @Body profile: CustomerProfile
    ): Response<List<CustomerProfile>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/customer_profiles")
    suspend fun updateCustomerProfile(
        @Query("id") idFilter: String,
        @Body profile: CustomerProfile
    ): Response<List<CustomerProfile>>
}
