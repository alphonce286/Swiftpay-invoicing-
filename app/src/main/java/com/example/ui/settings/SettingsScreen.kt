package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isSavedSuccess by viewModel.isSavedSuccess.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var businessName by remember { mutableStateOf("") }
    var businessEmail by remember { mutableStateOf("") }
    var businessPhone by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var currencySymbol by remember { mutableStateOf("$") }
    var mobileMoneyProvider by remember { mutableStateOf("") }
    var mobileMoneyNumber by remember { mutableStateOf("") }
    var onlinePaymentUrl by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankAccountNumber by remember { mutableStateOf("") }
    var bankAccountName by remember { mutableStateOf("") }
    var defaultTerms by remember { mutableStateOf("") }
    var isProTier by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        businessName = settings.businessName
        businessEmail = settings.businessEmail
        businessPhone = settings.businessPhone
        businessAddress = settings.businessAddress
        currencySymbol = settings.currencySymbol
        mobileMoneyProvider = settings.mobileMoneyProvider
        mobileMoneyNumber = settings.mobileMoneyNumber
        onlinePaymentUrl = settings.onlinePaymentUrl
        bankName = settings.bankName
        bankAccountNumber = settings.bankAccountNumber
        bankAccountName = settings.bankAccountName
        defaultTerms = settings.defaultPaymentTerms
        isProTier = settings.isProTier
    }

    LaunchedEffect(isSavedSuccess) {
        if (isSavedSuccess) {
            Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetSaveSuccess()
        }
    }

    Scaffold(
        modifier = modifier.testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Business & Payment Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveSettings(
                                settings.copy(
                                    businessName = businessName,
                                    businessEmail = businessEmail,
                                    businessPhone = businessPhone,
                                    businessAddress = businessAddress,
                                    currencySymbol = currencySymbol,
                                    mobileMoneyProvider = mobileMoneyProvider,
                                    mobileMoneyNumber = mobileMoneyNumber,
                                    onlinePaymentUrl = onlinePaymentUrl,
                                    bankName = bankName,
                                    bankAccountNumber = bankAccountNumber,
                                    bankAccountName = bankAccountName,
                                    defaultPaymentTerms = defaultTerms,
                                    isProTier = isProTier
                                )
                            )
                        },
                        modifier = Modifier.testTag("save_settings_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pro Plan Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProTier) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isProTier) Color(0xFFF59E0B) else Color(0xFF475569)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isProTier) "PRO PLAN ($8/month)" else "FREE TIER",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isProTier) "Unlimited invoices & auto-reminders" else "3 invoices / month limit",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Switch(
                                checked = isProTier,
                                onCheckedChange = {
                                    isProTier = it
                                    viewModel.toggleProTier(it)
                                    Toast.makeText(
                                        context,
                                        if (it) "Upgraded to Pro Plan!" else "Switched to Free Tier",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.testTag("pro_tier_switch")
                            )
                        }
                    }
                }
            }

            // Currency Selector
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Currency Symbol", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        val currencies = listOf("$", "€", "£", "KSh", "₦", "ZAR", "₹", "C$", "A$")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(currencies) { curr ->
                                FilterChip(
                                    selected = currencySymbol == curr,
                                    onClick = { currencySymbol = curr },
                                    label = { Text(curr, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }
            }

            // Business Profile
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Business Profile (Appears on Invoices)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business / Freelancer Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("settings_business_name")
                        )

                        OutlinedTextField(
                            value = businessEmail,
                            onValueChange = { businessEmail = it },
                            label = { Text("Billing Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = businessPhone,
                            onValueChange = { businessPhone = it },
                            label = { Text("Phone / WhatsApp") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = businessAddress,
                            onValueChange = { businessAddress = it },
                            label = { Text("Business Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Payment Links (Card / Mobile Money / Bank Transfer)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Online Payment & Mobile Money Links", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Text(
                            text = "These details will be attached directly to generated invoices and WhatsApp reminders.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = onlinePaymentUrl,
                            onValueChange = { onlinePaymentUrl = it },
                            label = { Text("Online Payment Link (Stripe, Paystack, Flutterwave)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = mobileMoneyProvider,
                                onValueChange = { mobileMoneyProvider = it },
                                label = { Text("Provider (e.g. M-Pesa)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = mobileMoneyNumber,
                                onValueChange = { mobileMoneyNumber = it },
                                label = { Text("Till / Paybill / Number") },
                                singleLine = true,
                                modifier = Modifier.weight(1.3f)
                            )
                        }

                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = bankAccountNumber,
                                onValueChange = { bankAccountNumber = it },
                                label = { Text("Account Number") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = bankAccountName,
                                onValueChange = { bankAccountName = it },
                                label = { Text("Account Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = defaultTerms,
                            onValueChange = { defaultTerms = it },
                            label = { Text("Default Payment Terms") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        viewModel.saveSettings(
                            settings.copy(
                                businessName = businessName,
                                businessEmail = businessEmail,
                                businessPhone = businessPhone,
                                businessAddress = businessAddress,
                                currencySymbol = currencySymbol,
                                mobileMoneyProvider = mobileMoneyProvider,
                                mobileMoneyNumber = mobileMoneyNumber,
                                onlinePaymentUrl = onlinePaymentUrl,
                                bankName = bankName,
                                bankAccountNumber = bankAccountNumber,
                                bankAccountName = bankAccountName,
                                defaultPaymentTerms = defaultTerms,
                                isProTier = isProTier
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_settings_bottom_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save All Settings", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
