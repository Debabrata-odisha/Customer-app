package com.example.data.repository

import android.util.Log
import com.example.data.model.AuthSessionResponse
import com.example.data.model.AuthSignInRequest
import com.example.data.model.AuthSignUpRequest
import com.example.data.model.Booking
import com.example.data.model.Category
import com.example.data.model.CreateBookingRequest
import com.example.data.model.CustomerProfile
import com.example.data.model.PaymentRecord
import com.example.data.model.Service
import com.example.data.model.UpdateBookingRequest
import com.example.data.supabase.SupabaseClientProvider
import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabaseRealtimeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext

class SupabaseRepository(
    val config: SupabaseConfig,
    val provider: SupabaseClientProvider
) {
    private val tag = "DoorblyRepository"

    val realtimeUpdates: SharedFlow<SupabaseRealtimeClient.RealtimeBookingUpdate>
        get() = provider.realtimeClient.realtimeEvents

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!config.isConfigured) {
                return@withContext Result.failure(
                    IllegalStateException("Supabase URL or Anon Key is missing. Please set them in .env or configure them in app.")
                )
            }
            val response = provider.getApi().getCategories()
            if (response.isSuccessful) {
                val count = response.body()?.size ?: 0
                Result.success("Connected successfully to Supabase! Found $count categories.")
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(Exception("Supabase HTTP ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Connection test failed", e)
            Result.failure(e)
        }
    }

    suspend fun getCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            if (!config.isConfigured) {
                return@withContext Result.failure(IllegalStateException("Supabase credentials not configured."))
            }
            val response = provider.getApi().getCategories()
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("Failed to fetch categories: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get categories", e)
            Result.failure(e)
        }
    }

    suspend fun getServices(categoryId: String? = null, searchQuery: String? = null): Result<List<Service>> =
        withContext(Dispatchers.IO) {
            try {
                if (!config.isConfigured) {
                    return@withContext Result.failure(IllegalStateException("Supabase credentials not configured."))
                }
                val catFilter = if (!categoryId.isNullOrEmpty()) "eq.$categoryId" else null
                val searchFilter = if (!searchQuery.isNullOrBlank()) "ilike.*${searchQuery.trim()}*" else null
                val response = provider.getApi().getServices(catFilter, searchFilter)
                if (response.isSuccessful) {
                    Result.success(response.body().orEmpty())
                } else {
                    Result.failure(Exception("Failed to fetch services: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to get services", e)
                Result.failure(e)
            }
        }

    suspend fun getServiceById(id: String): Result<Service> = withContext(Dispatchers.IO) {
        try {
            val response = provider.getApi().getServiceById(idFilter = "eq.$id")
            if (response.isSuccessful) {
                val item = response.body()?.firstOrNull()
                if (item != null) Result.success(item)
                else Result.failure(NoSuchElementException("Service with ID $id not found"))
            } else {
                Result.failure(Exception("HTTP ${response.code()} loading service"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookings(customerId: String): Result<List<Booking>> = withContext(Dispatchers.IO) {
        try {
            if (!config.isConfigured) {
                return@withContext Result.failure(IllegalStateException("Supabase credentials not configured."))
            }
            val response = provider.getApi().getBookings(customerIdFilter = "eq.$customerId")
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("Failed to fetch bookings: ${response.code()} ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get bookings", e)
            Result.failure(e)
        }
    }

    suspend fun getBookingById(bookingId: String): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val response = provider.getApi().getBookingById(idFilter = "eq.$bookingId")
            if (response.isSuccessful) {
                val item = response.body()?.firstOrNull()
                if (item != null) Result.success(item)
                else Result.failure(NoSuchElementException("Booking not found"))
            } else {
                Result.failure(Exception("HTTP ${response.code()} loading booking"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBooking(request: CreateBookingRequest): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val response = provider.getApi().createBooking(request)
            if (response.isSuccessful) {
                val created = response.body()?.firstOrNull()
                if (created != null) Result.success(created)
                else Result.failure(Exception("Booking created but empty response returned"))
            } else {
                val err = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("Failed to book service: $err"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error creating booking", e)
            Result.failure(e)
        }
    }

    suspend fun updateBooking(
        bookingId: String,
        status: String? = null,
        paymentStatus: String? = null,
        paymentMethod: String? = null
    ): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val updateReq = UpdateBookingRequest(
                status = status,
                paymentStatus = paymentStatus,
                paymentMethod = paymentMethod
            )
            val response = provider.getApi().updateBooking("eq.$bookingId", updateReq)
            if (response.isSuccessful) {
                val updated = response.body()?.firstOrNull()
                if (updated != null) Result.success(updated)
                else Result.failure(Exception("Booking updated but empty result returned"))
            } else {
                Result.failure(Exception("Failed to update booking: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordPayment(payment: PaymentRecord): Result<PaymentRecord> = withContext(Dispatchers.IO) {
        try {
            val response = provider.getApi().recordPayment(payment)
            if (response.isSuccessful) {
                val record = response.body()?.firstOrNull() ?: payment
                // Also update booking payment status
                updateBooking(
                    bookingId = payment.bookingId,
                    paymentStatus = "Paid",
                    paymentMethod = payment.paymentMethod
                )
                Result.success(record)
            } else {
                Result.failure(Exception("Failed to record payment: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerProfile(userId: String): Result<CustomerProfile> = withContext(Dispatchers.IO) {
        try {
            val response = provider.getApi().getCustomerProfile("eq.$userId")
            if (response.isSuccessful) {
                val profile = response.body()?.firstOrNull()
                if (profile != null) {
                    config.userName = profile.fullName
                    config.userPhone = profile.phoneNumber
                    Result.success(profile)
                } else {
                    // Profile not yet created in table, return fallback profile from local session
                    val fallback = CustomerProfile(
                        id = userId,
                        email = config.userEmail,
                        fullName = config.userName ?: "Doorbly Customer",
                        phoneNumber = config.userPhone
                    )
                    Result.success(fallback)
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()} reading profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomerProfile(profile: CustomerProfile): Result<CustomerProfile> =
        withContext(Dispatchers.IO) {
            try {
                val response = provider.getApi().upsertCustomerProfile(profile)
                if (response.isSuccessful) {
                    val updated = response.body()?.firstOrNull() ?: profile
                    config.userName = updated.fullName
                    config.userPhone = updated.phoneNumber
                    Result.success(updated)
                } else {
                    Result.failure(Exception("Failed to save profile: ${response.errorBody()?.string()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun signIn(email: String, password: String): Result<AuthSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = provider.getApi().signIn(AuthSignInRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val session = response.body()
                    if (session?.accessToken != null && session.user != null) {
                        config.accessToken = session.accessToken
                        config.userId = session.user.id
                        config.userEmail = session.user.email
                        val metaName = session.user.userMetadata?.get("full_name") as? String
                        val metaPhone = (session.user.userMetadata?.get("phone_number") as? String)
                            ?: session.user.phone
                        if (!metaName.isNullOrEmpty()) config.userName = metaName
                        if (!metaPhone.isNullOrEmpty()) config.userPhone = metaPhone
                        provider.resetClient()
                        Result.success(session)
                    } else {
                        Result.failure(Exception("Sign in returned empty session"))
                    }
                } else {
                    val err = response.errorBody()?.string().orEmpty()
                    Result.failure(Exception("Sign in failed: $err"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        phone: String?
    ): Result<AuthSessionResponse> = withContext(Dispatchers.IO) {
        try {
            val meta = mutableMapOf<String, String>()
            meta["full_name"] = fullName.trim()
            if (!phone.isNullOrBlank()) meta["phone_number"] = phone.trim()

            val response = provider.getApi().signUp(
                AuthSignUpRequest(
                    email = email.trim(),
                    password = password,
                    data = meta
                )
            )
            if (response.isSuccessful) {
                val session = response.body()
                if (session?.accessToken != null && session.user != null) {
                    config.accessToken = session.accessToken
                    config.userId = session.user.id
                    config.userEmail = session.user.email
                    config.userName = fullName.trim()
                    config.userPhone = phone?.trim()
                    provider.resetClient()

                    // Also initialize customer_profiles row
                    try {
                        provider.getApi().upsertCustomerProfile(
                            CustomerProfile(
                                id = session.user.id,
                                email = session.user.email,
                                fullName = fullName.trim(),
                                phoneNumber = phone?.trim()
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(tag, "Profile upsert upon signup error: ${e.message}")
                    }

                    Result.success(session)
                } else if (session?.user != null) {
                    // Email confirmation may be enabled in Supabase
                    Result.success(session)
                } else {
                    Result.failure(Exception("Sign up completed but empty session"))
                }
            } else {
                val err = response.errorBody()?.string().orEmpty()
                Result.failure(Exception("Sign up failed: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            provider.getApi().signOut()
        } catch (e: Exception) {
            // ignore remote logout errors
        } finally {
            config.clearSession()
            provider.resetClient()
        }
    }
}
