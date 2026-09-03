package com.sagecoevergreen.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.net.HttpURLConnection

object ApiClient {
    private const val BASE_URL = "https://sageco-evergreen-co.vercel.app"

    // ── Properties ──────────────────────────────────────────────
    suspend fun getProperties(
        category: String? = null,
        search: String? = null,
        sort: String = "newest",
        limit: Int = 20,
        offset: Int = 0,
        featured: Boolean? = null
    ): Pair<List<Property>, Int> = withContext(Dispatchers.IO) {
        val params = mutableListOf("limit=$limit", "offset=$offset", "sort=${enc(sort)}")
        category?.let { if (it != "All") params.add("category=${enc(it)}") }
        search?.let { if (it.isNotBlank()) params.add("search=${enc(it)}") }
        featured?.let { if (it) params.add("featured=true") }

        val url = "$BASE_URL/api/get-properties?${params.joinToString("&")}"
        val json = fetchJson(url)
        val arr = json.optJSONArray("properties") ?: JSONArray()
        val total = json.optInt("total", 0)
        val list = mutableListOf<Property>()
        for (i in 0 until arr.length()) {
            list.add(parseProperty(arr.getJSONObject(i)))
        }
        Pair(list, total)
    }

    suspend fun getProperty(id: String): Property? = withContext(Dispatchers.IO) {
        val json = fetchJson("$BASE_URL/api/get-property?id=${enc(id)}")
        if (json.has("error")) null else parseProperty(json)
    }

    // ── Brokers ─────────────────────────────────────────────────
    suspend fun getBrokers(search: String? = null): List<Broker> = withContext(Dispatchers.IO) {
        val url = if (search != null && search.isNotBlank())
            "$BASE_URL/api/get-brokers?search=${enc(search)}&limit=50"
        else
            "$BASE_URL/api/get-brokers?limit=50"
        val json = fetchJson(url)
        val arr = json.optJSONArray("brokers") ?: JSONArray()
        val list = mutableListOf<Broker>()
        for (i in 0 until arr.length()) {
            list.add(parseBroker(arr.getJSONObject(i)))
        }
        list
    }

    // ── Agents / MLM ────────────────────────────────────────────
    suspend fun getAgents(): List<Agent> = withContext(Dispatchers.IO) {
        val json = fetchJson("$BASE_URL/api/agents/list")
        val arr = json.optJSONArray("agents") ?: JSONArray()
        val list = mutableListOf<Agent>()
        for (i in 0 until arr.length()) {
            list.add(parseAgent(arr.getJSONObject(i)))
        }
        list
    }

    suspend fun getAgentDashboard(agentId: String): AgentDashboard? = withContext(Dispatchers.IO) {
        val json = fetchJson("$BASE_URL/api/agents/dashboard?agent_id=${enc(agentId)}")
        if (json.has("error")) return@withContext null

        val agent = parseAgent(json.getJSONObject("agent"))
        val downlineArr = json.optJSONArray("downline") ?: JSONArray()
        val downline = mutableListOf<Agent>()
        for (i in 0 until downlineArr.length()) {
            downline.add(parseAgent(downlineArr.getJSONObject(i)))
        }

        val commArr = json.optJSONArray("commissions") ?: JSONArray()
        val commissions = mutableListOf<Commission>()
        for (i in 0 until commArr.length()) {
            val c = commArr.getJSONObject(i)
            commissions.add(Commission(
                id = c.optString("id"),
                amount = c.optLong("amount"),
                type = optNullableString(c, "type"),
                status = optNullableString(c, "status"),
                created_at = optNullableString(c, "created_at")
            ))
        }

        AgentDashboard(
            agent = agent,
            downline = downline,
            commissions = commissions,
            wallet_balance = json.optLong("wallet_balance", 0),
            total_earned = json.optLong("total_earned", 0),
            total_withdrawn = json.optLong("total_withdrawn", 0)
        )
    }

    suspend fun registerAgent(
        fullName: String, phone: String, email: String?,
        location: String?, bio: String?, sponsorId: String?
    ): JSONObject = withContext(Dispatchers.IO) {
        postJson("$BASE_URL/api/agents/register", JSONObject().apply {
            put("full_name", fullName)
            put("phone", phone)
            email?.let { put("email", it) }
            location?.let { put("location", it) }
            bio?.let { put("bio", it) }
            sponsorId?.let { put("sponsor_id", it) }
        })
    }

    suspend fun requestWithdrawal(
        agentId: String, amount: Long, method: String,
        phoneNumber: String?, accountName: String?
    ): JSONObject = withContext(Dispatchers.IO) {
        postJson("$BASE_URL/api/agents/withdraw", JSONObject().apply {
            put("agent_id", agentId)
            put("amount", amount)
            put("method", method)
            phoneNumber?.let { put("phone_number", it) }
            accountName?.let { put("account_name", it) }
        })
    }

    suspend fun getWithdrawals(agentId: String): Pair<List<Withdrawal>, Long?> = withContext(Dispatchers.IO) {
        val json = fetchJson("$BASE_URL/api/agents/withdraw?agent_id=${enc(agentId)}")
        if (json.has("error")) return@withContext Pair(emptyList(), null)
        val arr = json.optJSONArray("withdrawals") ?: JSONArray()
        val list = mutableListOf<Withdrawal>()
        for (i in 0 until arr.length()) {
            val w = arr.getJSONObject(i)
            list.add(Withdrawal(
                id = w.optString("id"),
                amount = w.optLong("amount"),
                method = optNullableString(w, "method") ?: "mobile_money",
                status = optNullableString(w, "status") ?: "pending",
                created_at = optNullableString(w, "created_at")
            ))
        }
        val balance = if (json.has("balance")) json.optLong("balance") else null
        Pair(list, balance)
    }

    // ── Chatbot ─────────────────────────────────────────────────
    suspend fun sendChat(message: String): String = withContext(Dispatchers.IO) {
        val json = postJson("$BASE_URL/api/chat", JSONObject().apply {
            put("message", message)
        })
        optNullableString(json, "reply") ?: optNullableString(json, "error") ?: "Sorry, I couldn't process that."
    }

    // ── App version ─────────────────────────────────────────────
    suspend fun checkVersion(): AppVersion? = withContext(Dispatchers.IO) {
        try {
            val json = fetchJson("$BASE_URL/api/app-version")
            AppVersion(
                versionCode = json.optInt("versionCode"),
                versionName = json.optString("versionName"),
                apkUrl = optNullableString(json, "apkUrl"),
                forceUpdate = json.optBoolean("forceUpdate", false),
                notes = optNullableString(json, "notes")
            )
        } catch (_: Exception) { null }
    }

    // ── Helpers ─────────────────────────────────────────────────

    /**
     * Safely get a nullable String from JSONObject.
     * Android's optString(key, null) returns the literal string "null"
     * when the JSON value is null, not Java null. This helper checks
     * isNull() first to return a proper null.
     */
    private fun optNullableString(j: JSONObject, key: String): String? {
        if (!j.has(key)) return null
        if (j.isNull(key)) return null
        val s = j.optString(key)
        return s.ifBlank { null }
    }

    private fun optNullableString(arr: JSONArray, index: Int): String? {
        if (arr.isNull(index)) return null
        return arr.optString(index).ifBlank { null }
    }

    private fun fetchJson(url: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 20000
        conn.setRequestProperty("User-Agent", "SagecoApp/4.0 Android")
        conn.setRequestProperty("Accept", "application/json")
        try {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun postJson(url: String, body: JSONObject): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 20000
        conn.readTimeout = 20000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "SagecoApp/4.0 Android")
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(response)
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun parseProperty(j: JSONObject): Property {
        val imagesArr = j.optJSONArray("images")
        val images = mutableListOf<String>()
        if (imagesArr != null) {
            for (i in 0 until imagesArr.length()) {
                val img = optNullableString(imagesArr, i)
                if (img != null && img.startsWith("http")) {
                    images.add(img)
                }
            }
        }
        if (images.isEmpty()) {
            optNullableString(j, "image_url")?.let { if (it.startsWith("http")) images.add(it) }
        }
        return Property(
            id = j.optString("id"),
            title = j.optString("title"),
            description = optNullableString(j, "description"),
            price = j.optLong("price"),
            location = optNullableString(j, "location"),
            category = optNullableString(j, "category"),
            bedrooms = j.optInt("bedrooms", 0).takeIf { it > 0 },
            bathrooms = j.optInt("bathrooms", 0).takeIf { it > 0 },
            area_sqft = j.optDouble("area_sqft", 0.0).takeIf { it > 0 },
            images = images.ifEmpty { null },
            status = optNullableString(j, "status"),
            featured = j.optBoolean("featured", false),
            broker_name = optNullableString(j, "broker_name"),
            land_acres = j.optDouble("land_acres", 0.0).takeIf { it > 0 },
            plot_feet = optNullableString(j, "plot_feet"),
            water_available = if (j.has("water_available") && !j.isNull("water_available")) j.optBoolean("water_available") else null,
            electricity_available = if (j.has("electricity_available") && !j.isNull("electricity_available")) j.optBoolean("electricity_available") else null,
            road_distance_km = j.optDouble("road_distance_km", 0.0).takeIf { it > 0 },
            fence = optNullableString(j, "fence"),
            title_deed = optNullableString(j, "title_deed"),
            is_negotiable = j.optBoolean("is_negotiable", false),
            contact_name = optNullableString(j, "contact_name"),
            contact_phone = optNullableString(j, "contact_phone"),
            latitude = j.optDouble("latitude", 0.0).takeIf { it != 0.0 },
            longitude = j.optDouble("longitude", 0.0).takeIf { it != 0.0 },
            views = j.optInt("views", 0),
            eco_score = j.optInt("eco_score", 0).takeIf { it > 0 },
            valuation_estimate = j.optLong("valuation_estimate", 0).takeIf { it > 0 },
            title_number = optNullableString(j, "title_number"),
            tenure_type = optNullableString(j, "tenure_type"),
            created_at = optNullableString(j, "created_at")
        )
    }

    private fun parseBroker(j: JSONObject): Broker = Broker(
        id = j.optString("id"),
        full_name = optNullableString(j, "full_name") ?: "Unknown",
        email = optNullableString(j, "email"),
        phone = optNullableString(j, "phone"),
        photo_url = optNullableString(j, "photo_url"),
        bio = optNullableString(j, "bio"),
        location = optNullableString(j, "location"),
        specialization = optNullableString(j, "specialization"),
        registration_status = optNullableString(j, "registration_status"),
        verified = if (j.has("verified") && !j.isNull("verified")) j.optBoolean("verified") else null,
        plan = optNullableString(j, "plan")
    )

    private fun parseAgent(j: JSONObject): Agent = Agent(
        id = j.optString("id"),
        full_name = optNullableString(j, "full_name") ?: "Unknown",
        phone = optNullableString(j, "phone"),
        email = optNullableString(j, "email"),
        location = optNullableString(j, "location"),
        level = j.optInt("level", 1),
        group_name = optNullableString(j, "group_name"),
        registration_status = optNullableString(j, "registration_status"),
        downline_count = j.optInt("downline_count", 0),
        total_earnings = j.optLong("total_earnings", 0),
        created_at = optNullableString(j, "created_at")
    )
}
