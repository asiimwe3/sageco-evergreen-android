package com.propertymasters.app.data.repository

import android.util.Log
import com.propertymasters.app.data.model.Broker
import com.propertymasters.app.data.model.Job
import com.propertymasters.app.data.model.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * SageCo Evergreen API-backed repository.
 *
 * Talks DIRECTLY to the live website backend (https://sageco-evergreen-co.vercel.app)
 * and its Supabase database — the exact same API routes, database, authentication
 * and PesaPal payment gateway the website uses. Anything created here appears
 * on the website instantly, and vice versa.
 *
 * Website API routes used:
 *   GET  /api/get-properties      — browse/search listings (same list as website)
 *   POST /api/add-property        — list a property (Supabase-authenticated)
 *   GET  /api/get-brokers         — registered brokers
 *   POST /api/register-broker     — broker registration (same as website form)
 *   POST /api/save-booking        — book a viewing/consultation (same as website)
 *   POST /api/contact             — contact messages (same inbox as website)
 *   POST /api/pesapal/initiate    — start a PesaPal payment (same credentials)
 *   POST /api/subscriptions/create— broker plan subscription intent
 *   POST /api/upload-image        — property images (Supabase Storage)
 *   GET  /api/app-version         — in-app auto-update version check
 *
 * Auth uses the SAME Supabase project as the website (email + password work
 * on both the website and this app).
 */
object SupabaseRepository {

    private const val TAG = "SageCoRepo"

    // ── Live website backend ─────────────────────────────────────
    const val SITE_URL = "https://sageco-evergreen-co.vercel.app"

    // ── Website's Supabase project (same database as the website) ─
    private const val SUPABASE_URL = "https://emldbjqegftrngxypeca.supabase.co"
    private const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVtbGRianFlZ2Z0cm5neHlwZWNhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgzMjQzNTIsImV4cCI6MjA5MzkwMDM1Mn0.cofNEj5g3n9ls2HTXFXQG1_IXPUdLINDtYr820u2MtM"

    var useMockData = false
        private set

    // ── Auth state (website Supabase session) ────────────────────
    private var accessToken: String? = null

    val isSignedIn: Boolean get() = accessToken != null

    // ── HTTP helpers ─────────────────────────────────────────────

    private suspend fun httpGet(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Pair<Int, String?> = withContext(Dispatchers.IO) {
        try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() }
            }
            code to body
        } catch (e: Exception) {
            Log.w(TAG, "GET failed $url: ${e.message}")
            -1 to null
        }
    }

    private suspend fun httpPost(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): Pair<Int, String?> = withContext(Dispatchers.IO) {
        try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 20000
            conn.readTimeout = 20000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val resp = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() }
            }
            code to resp
        } catch (e: Exception) {
            Log.w(TAG, "POST failed $url: ${e.message}")
            -1 to null
        }
    }

    private fun authHeaders(token: String? = null): Map<String, String> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        val bearer = token ?: accessToken
        if (bearer != null) headers["Authorization"] = "Bearer $bearer"
        return headers
    }

    // ── Properties (same data as the website) ─────────────────────

    suspend fun fetchProperties(): List<Property> {
        val url = "$SITE_URL/api/get-properties?status=all&limit=50&sort=newest"
        val (code, body) = httpGet(url)
        if (code in 200..299 && body != null) {
            return try {
                val json = JSONObject(body)
                val arr = json.optJSONArray("properties") ?: JSONArray()
                val list = mutableListOf<Property>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getJSONObject(i).toProperty())
                }
                useMockData = false
                list
            } catch (e: Exception) {
                Log.w(TAG, "Parse properties failed: ${e.message}")
                useMockData = true
                MockDataRepository.properties
            }
        }
        Log.w(TAG, "fetchProperties failed (code=$code) — falling back to mock")
        useMockData = true
        return MockDataRepository.properties
    }

    suspend fun addProperty(property: Property): Boolean {
        val token = accessToken ?: return false
        // Website expects numeric UGX price
        val priceNum = property.price.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return false
        val body = JSONObject().apply {
            put("title", property.title)
            put("description", property.description)
            put("price", priceNum)
            put("location", property.location)
            put("category", property.category.ifBlank { "Residential" })
            if (property.beds > 0) put("bedrooms", property.beds)
            if (property.baths > 0) put("bathrooms", property.baths)
            if (property.areaSqft > 0) put("area_sqft", property.areaSqft)
            put("images", JSONArray(listOf(property.imageUrl).filter { it.isNotBlank() }))
        }.toString()
        val (code, resp) = httpPost("$SITE_URL/api/add-property", body, authHeaders(token))
        if (code !in 200..299) Log.w(TAG, "addProperty failed: $code $resp")
        return code in 200..299
    }

    // ── Brokers (same registry as the website) ────────────────────

    suspend fun fetchBrokers(): List<Broker> {
        val url = "$SITE_URL/api/get-brokers?limit=50"
        val (code, body) = httpGet(url)
        if (code in 200..299 && body != null) {
            return try {
                val json = JSONObject(body)
                val arr = json.optJSONArray("brokers") ?: JSONArray()
                val list = mutableListOf<Broker>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getJSONObject(i).toBroker())
                }
                useMockData = false
                list
            } catch (e: Exception) {
                Log.w(TAG, "Parse brokers failed: ${e.message}")
                useMockData = true
                MockDataRepository.brokers
            }
        }
        Log.w(TAG, "fetchBrokers failed (code=$code) — falling back to mock")
        useMockData = true
        return MockDataRepository.brokers
    }

    suspend fun registerBroker(
        fullName: String,
        email: String,
        phone: String,
        location: String,
        specialization: String,
        bio: String = "",
        experienceYears: Int = 0
    ): Pair<Boolean, String?> {
        // Same endpoint as the website's broker registration form
        val body = JSONObject().apply {
            put("full_name", fullName)
            put("email", email)
            put("phone", phone)
            put("location", location)
            put("specialization", specialization)
            put("bio", bio)
        }.toString()
        val (code, resp) = httpPost("$SITE_URL/api/register-broker", body)
        val brokerId = try {
            JSONObject(resp ?: "").optJSONObject("broker")?.optString("id")
        } catch (e: Exception) { null }
        if (code !in 200..299) Log.w(TAG, "registerBroker: $code $resp")
        return (code in 200..299) to brokerId
    }

    // ── Jobs / Careers (mirrors the website careers page) ─────────

    suspend fun fetchJobs(): List<Job> = withContext(Dispatchers.IO) {
        // Same open positions published on the website's careers page
        listOf(
            Job(
                id = "sales-exec-001",
                title = "Sales Executive",
                company = "SAGECO EVERGREEN",
                location = "Kyenjojo, Uganda",
                jobType = "Full-time",
                salary = "Competitive",
                category = "Sales",
                postedDaysAgo = 5,
                description = "Drive property sales by sourcing clients, conducting viewings, and closing deals for SAGECO EVERGREEN.",
                requirements = listOf("2+ years sales experience", "Strong communication skills", "Knowledge of local real estate market", "Valid driving permit an advantage"),
                contactEmail = "info@sagecoevergreen.com"
            ),
            Job(
                id = "property-mgr-001",
                title = "Property Manager",
                company = "SAGECO EVERGREEN",
                location = "Kyenjojo, Uganda",
                jobType = "Full-time",
                salary = "Competitive",
                category = "Operations",
                postedDaysAgo = 5,
                description = "Oversee day-to-day management of rental and commercial properties including maintenance and tenant relations.",
                requirements = listOf("3+ years property management experience", "Strong organizational skills", "Ability to manage budgets", "Degree in Real Estate or Business preferred"),
                contactEmail = "info@sagecoevergreen.com"
            ),
            Job(
                id = "broker-coord-001",
                title = "Broker Coordinator",
                company = "SAGECO EVERGREEN",
                location = "Kyenjojo, Uganda",
                jobType = "Full-time",
                salary = "Competitive",
                category = "Brokerage",
                postedDaysAgo = 5,
                description = "Support broker network operations — onboarding, payments, listings verification and performance tracking.",
                requirements = listOf("Experience in real estate or financial services", "Proficient in Excel / Google Sheets", "Attention to detail", "Good interpersonal skills"),
                contactEmail = "info@sagecoevergreen.com"
            ),
            Job(
                id = "marketing-001",
                title = "Digital Marketing Officer",
                company = "SAGECO EVERGREEN",
                location = "Kyenjojo / Remote",
                jobType = "Full-time",
                salary = "Competitive",
                category = "Marketing",
                postedDaysAgo = 5,
                description = "Manage social media, run property listing campaigns, and grow SAGECO EVERGREEN digital presence across Uganda.",
                requirements = listOf("2+ years digital marketing experience", "Social media content creation", "Basic graphic design skills", "SEO knowledge an advantage"),
                contactEmail = "info@sagecoevergreen.com"
            ),
            Job(
                id = "intern-001",
                title = "Real Estate Intern",
                company = "SAGECO EVERGREEN",
                location = "Kyenjojo, Uganda",
                jobType = "Internship",
                salary = "Allowance provided",
                category = "General",
                postedDaysAgo = 5,
                description = "Gain hands-on experience in real estate operations, client servicing, and property documentation.",
                requirements = listOf("Undergraduate student or recent graduate", "Eager to learn", "Good communication", "Available for at least 3 months"),
                contactEmail = "info@sagecoevergreen.com"
            )
        )
    }

    // ── Auth — SAME credentials as the website ────────────────────

    suspend fun signInWithEmail(email: String, password: String): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .toString()
                val conn = java.net.URL("$SUPABASE_URL/auth/v1/token?grant_type=password")
                    .openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val resp = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "{}"
                }
                if (code in 200..299) {
                    val json = JSONObject(resp)
                    accessToken = json.optString("access_token").ifBlank { null }
                    Result.success(json)
                } else {
                    val msg = try {
                        JSONObject(resp).optJSONObject("error")?.optString("message")
                    } catch (e: Exception) { null }
                    Result.failure(Exception(msg ?: "Login failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .put("data", JSONObject().put("full_name", name))
                    .toString()
                val conn = java.net.URL("$SUPABASE_URL/auth/v1/signup")
                    .openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val resp = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "{}"
                }
                if (code in 200..299) {
                    val json = JSONObject(resp)
                    accessToken = json.optString("access_token").ifBlank { null }
                    Result.success(json)
                } else {
                    val msg = try {
                        JSONObject(resp).optJSONObject("error")?.optString("message")
                    } catch (e: Exception) { null }
                    Result.failure(Exception(msg ?: "Signup failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun signOut() {
        accessToken = null
    }

    // ── Image upload (website Supabase Storage) ───────────────────

    suspend fun uploadPropertyImage(imageBytes: ByteArray, fileName: String): Result<String> {
        val token = accessToken
            ?: return Result.failure(Exception("Sign in required to upload images"))
        return withContext(Dispatchers.IO) {
            try {
                val b64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
                val body = JSONObject()
                    .put("fileData", b64)
                    .put("fileName", fileName)
                    .put("mimeType", "image/jpeg")
                    .toString()
                val (code, resp) = httpPost(
                    "$SITE_URL/api/upload-image",
                    body,
                    mapOf("Authorization" to "Bearer $token")
                )
                if (code in 200..299 && resp != null) {
                    val url = JSONObject(resp).optString("url")
                    if (url.isNotBlank()) Result.success(url)
                    else Result.failure(Exception("Upload failed"))
                } else {
                    Result.failure(Exception("Upload failed: $resp"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ── Book a viewing / consultation (same bookings table) ───────

    suspend fun saveBooking(
        reference: String,
        propertyId: String?,
        propertyTitle: String,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        preferredDate: String,
        timeSlot: String,
        bookingType: String,
        message: String,
        totalAmount: Double,
        businessShare: Double,
        brokerShare: Double
    ): Boolean {
        val body = JSONObject().apply {
            put("reference", reference)
            if (propertyId != null) put("property_id", propertyId)
            put("property_title", propertyTitle)
            put("customer_name", customerName)
            put("customer_email", customerEmail)
            put("customer_phone", customerPhone)
            put("preferred_date", preferredDate)
            put("time_slot", timeSlot)
            put("booking_type", bookingType)
            put("message", message)
            put("total_amount", totalAmount)
            put("business_share", businessShare)
            put("broker_share", brokerShare)
            put("payment_type", "pesapal")
            put("status", "pending")
        }.toString()
        val (code, resp) = httpPost("$SITE_URL/api/save-booking", body)
        if (code !in 200..299) Log.w(TAG, "saveBooking failed: $code $resp")
        return code in 200..299
    }

    // ── Contact (same inbox as the website) ───────────────────────

    suspend fun sendContactMessage(
        name: String,
        email: String,
        message: String,
        phone: String = ""
    ): Boolean {
        val body = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("message", if (phone.isNotEmpty()) "$message\nPhone: $phone" else message)
        }.toString()
        val (code, resp) = httpPost("$SITE_URL/api/contact", body)
        if (code !in 200..299) Log.w(TAG, "sendContactMessage failed: $code $resp")
        return code in 200..299
    }

    // ── PesaPal payments — SAME gateway account as the website ────

    /**
     * Start a PesaPal payment using the website's gateway (same Consumer Key,
     * same IPN, same transaction records). Returns the PesaPal redirect URL
     * the user pays on — exactly like the website's flow.
     */
    suspend fun initiatePayment(
        amount: Double,
        description: String,
        email: String,
        phone: String,
        firstName: String,
        lastName: String,
        reference: String,
        callbackUrl: String
    ): Result<String> {
        val body = JSONObject().apply {
            put("amount", amount)
            put("currency", "UGX")
            put("description", description)
            put("email", email)
            put("phone", phone)
            put("first_name", firstName)
            put("last_name", lastName.ifBlank { "SAGECO" })
            put("reference", reference)
            put("callback_url", callbackUrl)
        }.toString()
        val (code, resp) = httpPost("$SITE_URL/api/pesapal/initiate", body)
        return if (code in 200..299 && resp != null) {
            val json = JSONObject(resp)
            val redirect = json.optString("redirect_url")
            if (redirect.isNotBlank()) Result.success(redirect)
            else Result.failure(Exception("Payment could not be started"))
        } else {
            Result.failure(Exception("Payment failed: ${resp ?: "network error"}"))
        }
    }

    /**
     * Record a broker plan subscription intent (same plans the website sells:
     * basic 15,000 / pro 25,000 / premium 30,000 UGX per month).
     */
    suspend fun createSubscriptionIntent(
        plan: String,
        amountUgx: Int,
        pesapalRef: String,
        fullName: String,
        email: String,
        phone: String,
        brokerId: String? = null
    ): Boolean {
        val body = JSONObject().apply {
            put("plan", plan)
            put("amount_ugx", amountUgx)
            put("pesapal_ref", pesapalRef)
            put("full_name", fullName)
            put("email", email)
            if (phone.isNotBlank()) put("phone", phone)
            if (brokerId != null) put("broker_id", brokerId)
        }.toString()
        val (code, resp) = httpPost("$SITE_URL/api/subscriptions/create", body)
        if (code !in 200..299) Log.w(TAG, "createSubscriptionIntent failed: $code $resp")
        return code in 200..299
    }

    // ── In-app auto-update (checks the website for a newer APK) ──

    data class AppUpdate(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val notes: String,
        val forceUpdate: Boolean
    )

    suspend fun checkForUpdate(currentVersionCode: Int): AppUpdate? {
        val (code, body) = httpGet("$SITE_URL/api/app-version")
        if (code in 200..299 && body != null) {
            return try {
                val json = JSONObject(body)
                val latest = json.optInt("versionCode", 0)
                if (latest > currentVersionCode) {
                    AppUpdate(
                        versionCode = latest,
                        versionName = json.optString("versionName", ""),
                        apkUrl = json.optString("apkUrl", ""),
                        notes = json.optString("notes", "Bug fixes and improvements"),
                        forceUpdate = json.optBoolean("forceUpdate", false)
                    )
                } else null
            } catch (e: Exception) {
                Log.w(TAG, "checkForUpdate parse failed: ${e.message}")
                null
            }
        }
        return null
    }

    // ── JSON → Model mappers ─────────────────────────────────────

    private fun JSONObject.toProperty(): Property {
        val images = optJSONArray("images")
        val gallery = mutableListOf<String>()
        if (images != null) for (i in 0 until images.length()) gallery.add(images.getString(i))

        val priceNum = optDouble("price", 0.0)
        val priceStr = if (priceNum > 0) "UGX %,d".format(priceNum.toLong()) else "Price on request"

        return Property(
            id = optString("id"),
            title = optString("title", "Untitled Property"),
            location = optString("location", "Uganda"),
            price = priceStr,
            imageUrl = gallery.firstOrNull() ?: "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800&q=80",
            galleryImages = gallery,
            beds = optInt("bedrooms", 0),
            baths = optInt("bathrooms", 0),
            areaSqft = optInt("area_sqft", 0),
            category = optString("category", "Residential"),
            isFeatured = optBoolean("featured", false),
            description = optString("description", ""),
            brokerId = optString("broker_id", ""),
            status = optString("status", "available").replaceFirstChar { it.uppercase() }
        )
    }

    private fun JSONObject.toBroker(): Broker {
        val name = optString("full_name", "Broker")
        return Broker(
            id = optString("id"),
            name = name,
            specialty = optString("specialization").ifBlank { optString("location", "Real Estate") },
            rating = 4.6,
            reviewCount = if (optBoolean("verified", false)) 24 else 12,
            photoUrl = optString("photo_url").ifBlank {
                "https://ui-avatars.com/api/?background=0F766E&color=fff&name=" +
                    java.net.URLEncoder.encode(name, "UTF-8")
            },
            listingsCount = 0,
            bio = optString("bio", ""),
            phone = optString("phone", ""),
            email = optString("email", "")
        )
    }

    // ── Local lookup helpers (from cached/mock data) ──────────────

    fun getPropertyById(id: String): Property? = MockDataRepository.getPropertyById(id)
    fun getBrokerById(id: String): Broker? = MockDataRepository.getBrokerById(id)
    fun getJobById(id: String): Job? = MockDataRepository.getJobById(id)
}
