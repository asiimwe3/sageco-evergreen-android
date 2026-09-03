package com.sagecoevergreen.app.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagecoevergreen.app.data.Agent
import com.sagecoevergreen.app.data.AgentDashboard
import com.sagecoevergreen.app.data.ApiClient
import com.sagecoevergreen.app.ui.components.*
import com.sagecoevergreen.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AgentsScreen(
    savedAgentId: String?,
    onSaveAgentId: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var agents by remember { mutableStateOf<List<Agent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var dashboard by remember { mutableStateOf<AgentDashboard?>(null) }
    var dashboardLoading by remember { mutableStateOf(false) }
    var showRegister by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }
    var agentIdInput by remember { mutableStateOf(savedAgentId ?: "") }

    fun loadAgents() {
        scope.launch {
            loading = true; error = null
            try { agents = ApiClient.getAgents() }
            catch (e: Exception) { error = e.message }
            loading = false
        }
    }

    fun loadDashboard(id: String) {
        scope.launch {
            dashboardLoading = true
            try { dashboard = ApiClient.getAgentDashboard(id) }
            catch (_: Exception) { dashboard = null }
            dashboardLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadAgents()
        if (savedAgentId != null) loadDashboard(savedAgentId)
    }

    if (showRegister) {
        AgentRegisterSheet(
            onDismiss = { showRegister = false },
            onRegistered = { id ->
                agentIdInput = id
                onSaveAgentId(id)
                showRegister = false
                loadDashboard(id)
                loadAgents()
            }
        )
        return
    }

    if (showWithdraw && dashboard != null) {
        WithdrawSheet(
            dashboard = dashboard!!,
            onDismiss = { showWithdraw = false },
            onSuccess = { loadDashboard(dashboard!!.agent.id) }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(OffWhite)) {
        // Header
        Surface(color = SagecoGreen, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Agents Network", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("MLM earnings, downline & withdrawals", fontSize = 13.sp, color = Color(0xFFCBD5E1))
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Agent ID input or dashboard
            if (dashboard == null) {
                item {
                    Surface(shape = RoundedCornerShape(14.dp), color = White, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Enter Your Agent ID", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gray800)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = agentIdInput,
                                onValueChange = { agentIdInput = it },
                                placeholder = { Text("Agent ID", fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onSaveAgentId(agentIdInput.trim())
                                        loadDashboard(agentIdInput.trim())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SagecoGreen),
                                    modifier = Modifier.weight(1f)
                                ) { Text("View Dashboard", fontSize = 13.sp) }
                                OutlinedButton(
                                    onClick = { showRegister = true },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Register", fontSize = 13.sp) }
                            }
                        }
                    }
                }
            } else {
                val d = dashboard!!
                item {
                    // Wallet card
                    Surface(shape = RoundedCornerShape(14.dp), color = SagecoGreen, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Wallet Balance", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                    Text("UGX ${d.wallet_balance}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = White)
                                }
                                Surface(shape = CircleShape, color = White.copy(alpha = 0.2f), modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.AccountBalanceWallet, null, tint = White, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoBox("Total Earned", "UGX ${d.total_earned}", Modifier.weight(1f))
                                InfoBox("Withdrawn", "UGX ${d.total_withdrawn}", Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showWithdraw = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Withdraw Funds", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Agent profile
                item {
                    Surface(shape = RoundedCornerShape(14.dp), color = White, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = SagecoGreen.copy(alpha = 0.15f), modifier = Modifier.size(48.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(d.agent.full_name.take(2).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SagecoGreen)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(d.agent.full_name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gray800)
                                Text("Level ${d.agent.level}  •  ${d.downline.size} downline", fontSize = 12.sp, color = Gray600)
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = if (d.agent.registration_status == "active") GreenBg else AmberBg) {
                                Text(
                                    d.agent.registration_status ?: "pending",
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    color = if (d.agent.registration_status == "active") SagecoGreen else Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Downline
                if (d.downline.isNotEmpty()) {
                    item { Text("Downline Agents", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gray800, modifier = Modifier.padding(top = 4.dp)) }
                    items(d.downline) { agent ->
                        DownlineRow(agent)
                    }
                }

                // Commissions
                if (d.commissions.isNotEmpty()) {
                    item { Text("Recent Commissions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gray800, modifier = Modifier.padding(top = 4.dp)) }
                    items(d.commissions.take(10)) { comm ->
                        Surface(shape = RoundedCornerShape(10.dp), color = White, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(comm.type ?: "Commission", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray800)
                                    Text(comm.created_at?.take(10) ?: "", fontSize = 11.sp, color = Gray400)
                                }
                                Text("UGX ${comm.amount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SagecoGreen)
                            }
                        }
                    }
                }
            }

            // All agents list
            item {
                Text("All Agents (${agents.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gray800, modifier = Modifier.padding(top = 8.dp))
            }

            if (loading) {
                item { LoadingIndicator() }
            } else if (error != null) {
                item { ErrorView(error!!) { loadAgents() } }
            } else {
                items(agents.take(20)) { agent ->
                    Surface(shape = RoundedCornerShape(10.dp), color = White, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = SagecoGreen.copy(alpha = 0.1f), modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(agent.full_name.take(1).uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SagecoGreen)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(agent.full_name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray800, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("L${agent.level}  •  ${agent.location ?: "Uganda"}", fontSize = 11.sp, color = Gray400)
                            }
                            Text("UGX ${agent.total_earnings}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SagecoGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(8.dp), color = White.copy(alpha = 0.15f), modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, fontSize = 10.sp, color = Color(0xFFCBD5E1))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = White)
        }
    }
}

@Composable
private fun DownlineRow(agent: Agent) {
    Surface(shape = RoundedCornerShape(10.dp), color = White, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = SagecoGreen.copy(alpha = 0.1f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(agent.full_name.take(1).uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SagecoGreen)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(agent.full_name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray800)
                Text("Level ${agent.level}  •  ${agent.downline_count} downline", fontSize = 11.sp, color = Gray400)
            }
            Surface(shape = RoundedCornerShape(6.dp), color = if (agent.registration_status == "active") GreenBg else AmberBg) {
                Text(
                    agent.registration_status ?: "pending",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = if (agent.registration_status == "active") SagecoGreen else Color(0xFF92400E),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AgentRegisterSheet(
    onDismiss: () -> Unit,
    onRegistered: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var sponsorId by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(OffWhite)) {
        Surface(color = SagecoGreen, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, null, tint = White) }
                Text("Register as Agent", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White)
            }
        }
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Registration Fee: UGX 30,000", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gold)
            OutlinedTextField(fullName, { fullName = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(phone, { phone = it }, label = { Text("Phone *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(bio, { bio = it }, label = { Text("Bio / Experience") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            OutlinedTextField(sponsorId, { sponsorId = it }, label = { Text("Sponsor Agent ID (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            errorMsg?.let { Text(it, color = Red, fontSize = 13.sp) }
            Button(
                onClick = {
                    scope.launch {
                        submitting = true; errorMsg = null
                        try {
                            val res = ApiClient.registerAgent(fullName, phone, email.ifBlank { null }, location.ifBlank { null }, bio.ifBlank { null }, sponsorId.ifBlank { null })
                            val id = res.optString("id")
                            if (id.isNotEmpty()) onRegistered(id)
                            else errorMsg = res.optString("error", "Registration failed")
                        } catch (e: Exception) { errorMsg = e.message }
                        submitting = false
                    }
                },
                enabled = fullName.isNotBlank() && phone.isNotBlank() && !submitting,
                colors = ButtonDefaults.buttonColors(containerColor = SagecoGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (submitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White)
                else Text("Register Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WithdrawSheet(
    dashboard: AgentDashboard,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("mobile_money") }
    var phone by remember { mutableStateOf(dashboard.agent.phone ?: "") }
    var accountName by remember { mutableStateOf(dashboard.agent.full_name) }
    var submitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(OffWhite)) {
        Surface(color = SagecoGreen, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, null, tint = White) }
                Text("Withdraw Funds", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White)
            }
        }
        if (success) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = SagecoGreen, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Withdrawal Requested!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gray800)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Your request is being processed", fontSize = 14.sp, color = Gray600)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SagecoGreen)) {
                        Text("Done")
                    }
                }
            }
            return
        }
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(10.dp), color = SagecoGreen.copy(alpha = 0.1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Available Balance", fontSize = 12.sp, color = Gray600)
                    Text("UGX ${dashboard.wallet_balance}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = SagecoGreen)
                }
            }
            OutlinedTextField(amount, { amount = it }, label = { Text("Amount (UGX) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("Withdrawal Method", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray800)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("mobile_money" to "Mobile Money", "bank" to "Bank", "cash" to "Cash").forEach { (val_, label) ->
                    FilterChip(
                        selected = method == val_,
                        onClick = { method = val_ },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SagecoGreen,
                            selectedLabelColor = White
                        )
                    )
                }
            }
            if (method == "mobile_money") {
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            if (method == "bank") {
                OutlinedTextField(accountName, { accountName = it }, label = { Text("Account Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            errorMsg?.let { Text(it, color = Red, fontSize = 13.sp) }
            Button(
                onClick = {
                    scope.launch {
                        submitting = true; errorMsg = null
                        try {
                            val res = ApiClient.requestWithdrawal(
                                dashboard.agent.id,
                                amount.toLongOrNull() ?: 0,
                                method,
                                phone.ifBlank { null },
                                accountName.ifBlank { null }
                            )
                            if (res.has("error")) errorMsg = res.optString("error")
                            else success = true
                        } catch (e: Exception) { errorMsg = e.message }
                        submitting = false
                    }
                },
                enabled = amount.isNotBlank() && !submitting,
                colors = ButtonDefaults.buttonColors(containerColor = SagecoGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (submitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White)
                else Text("Request Withdrawal", fontWeight = FontWeight.Bold)
            }
        }
    }
}
