package com.sagecoevergreen.app.data

data class Property(
    val id: String,
    val title: String,
    val description: String? = null,
    val price: Long,
    val location: String? = null,
    val category: String? = null,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val area_sqft: Double? = null,
    val images: List<String>? = null,
    val status: String? = null,
    val featured: Boolean = false,
    val broker_name: String? = null,
    val land_acres: Double? = null,
    val plot_feet: String? = null,
    val water_available: Boolean? = null,
    val electricity_available: Boolean? = null,
    val road_distance_km: Double? = null,
    val fence: String? = null,
    val title_deed: String? = null,
    val is_negotiable: Boolean? = null,
    val contact_name: String? = null,
    val contact_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val views: Int? = null,
    val eco_score: Int? = null,
    val valuation_estimate: Long? = null,
    val title_number: String? = null,
    val tenure_type: String? = null,
    val created_at: String? = null
)

data class Broker(
    val id: String,
    val full_name: String,
    val email: String? = null,
    val phone: String? = null,
    val photo_url: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val specialization: String? = null,
    val registration_status: String? = null,
    val verified: Boolean? = null,
    val plan: String? = null
)

data class Agent(
    val id: String,
    val full_name: String,
    val phone: String? = null,
    val email: String? = null,
    val location: String? = null,
    val level: Int = 1,
    val group_name: String? = null,
    val registration_status: String? = null,
    val downline_count: Int = 0,
    val total_earnings: Long = 0,
    val created_at: String? = null
)

data class AgentDashboard(
    val agent: Agent,
    val downline: List<Agent> = emptyList(),
    val commissions: List<Commission> = emptyList(),
    val wallet_balance: Long = 0,
    val total_earned: Long = 0,
    val total_withdrawn: Long = 0
)

data class Commission(
    val id: String,
    val amount: Long,
    val type: String? = null,
    val status: String? = null,
    val created_at: String? = null
)

data class Withdrawal(
    val id: String,
    val amount: Long,
    val method: String,
    val status: String,
    val created_at: String? = null
)

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AppVersion(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String? = null,
    val forceUpdate: Boolean = false,
    val notes: String? = null
)
