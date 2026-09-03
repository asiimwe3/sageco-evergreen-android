package com.propertymasters.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class PlansViewModel : ViewModel() {
    var loading by mutableStateOf<String?>(null) // plan name being paid
        private set

    // Same details the website's subscribe page collects
    var fullName by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")

    fun detailsValid(): Boolean =
        fullName.isNotBlank() && email.contains("@") && phone.isNotBlank()

    /**
     * Subscribe to a broker plan — identical flow to the website:
     * PesaPal payment + subscription intent recorded on the website.
     */
    fun subscribe(plan: String, price: Int, name: String, email: String, phone: String, onPayUrl: (String) -> Unit) {
        if (loading != null) return
        loading = plan
        viewModelScope.launch {
            val reference = "SUB-" + plan.uppercase() + "-" + System.currentTimeMillis()
            val pay = com.propertymasters.app.data.repository.SupabaseRepository.initiatePayment(
                amount = price.toDouble(),
                description = "SAGECO " + plan.replaceFirstChar { it.uppercase() } + " Plan — Monthly",
                email = email,
                phone = phone,
                firstName = name.substringBefore(" "),
                lastName = name.substringAfter(" ", ""),
                reference = reference,
                callbackUrl = com.propertymasters.app.data.repository.SupabaseRepository.SITE_URL +
                    "/subscription-success?ref=" + reference + "&plan=" + plan
            )
            com.propertymasters.app.data.repository.SupabaseRepository.createSubscriptionIntent(
                plan = plan.lowercase(),
                amountUgx = price,
                pesapalRef = reference,
                fullName = name,
                email = email,
                phone = phone
            )
            loading = null
            pay.onSuccess(onPayUrl)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(onBack: () -> Unit) {
    val vm: PlansViewModel = viewModel()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val sagecoTeal = Color(0xFF0F766E)
    val sagecoLight = Color(0xFFCCFBF1)

    val plans = listOf(
        PlanData("Free", 0, "No expiry", listOf("List up to 3 properties", "Basic broker profile", "Email support")),
        PlanData("Basic", 15000, "1 month", listOf("List up to 10 properties", "Standard broker profile", "Email support")),
        PlanData("Pro", 25000, "1 month", listOf("List up to 50 properties", "Featured broker profile", "Priority placement", "WhatsApp badge"), true),
        PlanData("Premium", 30000, "1 month", listOf("Unlimited listings", "Top placement", "Verified badge", "Priority support", "Analytics dashboard")),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Broker Plans", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = sagecoTeal, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("Choose Your Plan", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = sagecoTeal)
            Text("Premium broker features for listing properties across Uganda.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = sagecoLight)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Your Details", fontWeight = FontWeight.Bold, color = sagecoTeal, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = vm.fullName,
                        onValueChange = { vm.fullName = it },
                        label = { Text("Full name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = vm.email,
                        onValueChange = { vm.email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = vm.phone,
                        onValueChange = { vm.phone = it },
                        label = { Text("Phone (MoMo number)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            plans.forEach { plan ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (plan.popular) sagecoLight else Color.White),
                    border = androidx.compose.foundation.BorderStroke(2.dp, sagecoTeal)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(plan.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = sagecoTeal)
                                Text(plan.duration, fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(
                                if (plan.price == 0) "FREE" else "UGX ${"%,.0f".format(plan.price.toDouble())}",
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sagecoTeal
                            )
                        }
                        if (plan.popular) {
                            Spacer(Modifier.height(4.dp))
                            AssistChip(onClick = {}, label = { Text("Most Popular", fontSize = 10.sp) }, colors = AssistChipDefaults.assistChipColors(containerColor = sagecoTeal, labelColor = Color.White))
                        }
                        if (plan.price > 0) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    vm.subscribe(plan.name.lowercase(), plan.price,
                                        name = vm.fullName, email = vm.email, phone = vm.phone) { url ->
                                        ctx.startActivity(
                                            android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(url)
                                            )
                                        )
                                    }
                                },
                                enabled = vm.detailsValid() && vm.loading == null,
                                colors = ButtonDefaults.buttonColors(containerColor = sagecoTeal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (vm.loading == plan.name.lowercase()) "Starting payment…" else "Subscribe — UGX " + "%,.0f".format(plan.price.toDouble()))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        plan.features.forEach { feature ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("✓", color = sagecoTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(feature, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Payments via MTN MoMo, Airtel Money, or Card through PesaPal.", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

private data class PlanData(
    val name: String,
    val price: Int,
    val duration: String,
    val features: List<String>,
    val popular: Boolean = false
)
