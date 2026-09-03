package com.sagecoevergreen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagecoevergreen.app.data.ApiClient
import com.sagecoevergreen.app.data.ChatMessage
import com.sagecoevergreen.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatScreen() {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf(listOf(ChatMessage("bot", "Hello! I'm SAGECO's virtual assistant. Ask me about properties, brokers, bookings, or pricing."))) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize().background(OffWhite)) {
        // Header
        Surface(color = SagecoGreen, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = White.copy(alpha = 0.2f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.SmartToy, null, tint = White, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("SAGECO Assistant", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                    Text("Online  •  Local AI", fontSize = 11.sp, color = SagecoGreenBright)
                }
            }
        }

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (sending) {
                item {
                    Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(16.dp), color = White) {
                            Text("Typing...", fontSize = 13.sp, color = Gray400, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                }
            }
        }

        // Quick suggestions
        if (messages.size <= 2) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Show properties", "How to register", "Pricing").forEach { q ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SagecoGreen.copy(alpha = 0.1f),
                        modifier = Modifier.clickable {
                            input = q
                        }
                    ) {
                        Text(q, fontSize = 12.sp, color = SagecoGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Input bar
        Surface(color = White, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Type a message...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gray200,
                        unfocusedBorderColor = Gray200
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SagecoGreen,
                    modifier = Modifier.size(44.dp).clickable {
                        if (input.isNotBlank() && !sending) {
                            val msg = input.trim()
                            input = ""
                            messages = messages + ChatMessage("user", msg)
                            sending = true
                            scope.launch {
                                val reply = ApiClient.sendChat(msg)
                                messages = messages + ChatMessage("bot", reply)
                                sending = false
                            }
                        }
                    }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) SagecoGreen else White,
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Text(
                msg.content,
                fontSize = 14.sp,
                color = if (isUser) White else Gray800,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}
