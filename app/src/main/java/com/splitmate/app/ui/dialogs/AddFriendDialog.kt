package com.splitmate.app.ui.dialogs

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.splitmate.app.model.Friend
import com.splitmate.app.ui.theme.*
import com.splitmate.app.util.ContactsUtils
import com.splitmate.app.util.DeviceContact

@Composable
fun AddFriendDialog(
    isDemoMode: Boolean = false,
    existingFriends: List<Friend> = emptyList(),
    onDismiss: () -> Unit,
    onAddFriend: (name: String, email: String, phone: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showDemoWarning by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showContactsPicker by remember { mutableStateOf(false) }
    var availableContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var selectedContactIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val contacts = ContactsUtils.queryDeviceContacts(context)
            val filtered = ContactsUtils.filterDuplicates(contacts, existingFriends)
            availableContacts = filtered
            showContactsPicker = true
        } else {
            showPermissionDeniedDialog = true
        }
    }

    // Demo warning dialog
    if (showDemoWarning) {
        AlertDialog(
            onDismissRequest = { showDemoWarning = false },
            title = { Text("Switch to Live Mode", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Importing real device contacts is available in Live mode. Please switch to Live mode first.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { showDemoWarning = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SplitIndigo)
                ) {
                    Text("Got It")
                }
            },
            containerColor = SurfaceCard
        )
    }

    // Permission denied / Settings prompt dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Contacts Permission Needed", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "SplitMate requires Contacts permission to import friends from your phonebook. Your contact data remains strictly local on your device and is never uploaded.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDeniedDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SplitEmerald, contentColor = Color.Black)
                ) {
                    Text("Open Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard
        )
    }

    // Contacts picker sheet/dialog
    if (showContactsPicker) {
        Dialog(
            onDismissRequest = { showContactsPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Import Contacts",
                            style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { showContactsPicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Text(
                        text = "${availableContacts.size} contacts found (${selectedContactIds.size} selected)",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (availableContacts.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No new contacts available to import.", color = TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableContacts) { contact ->
                                val isSelected = selectedContactIds.contains(contact.id)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SplitEmerald else SurfaceBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedContactIds = if (isSelected) selectedContactIds - contact.id else selectedContactIds + contact.id
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedContactIds = if (checked == true) selectedContactIds + contact.id else selectedContactIds - contact.id
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = SplitEmerald)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(contact.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(contact.phoneNumbers.joinToString(", "), color = TextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val selectedContacts = availableContacts.filter { selectedContactIds.contains(it.id) }
                            selectedContacts.forEach { contact ->
                                val firstPhone = contact.phoneNumbers.firstOrNull() ?: ""
                                val emailPlaceholder = "${contact.name.lowercase().replace(" ", ".")}@contacts.local"
                                onAddFriend(contact.name, emailPlaceholder, firstPhone)
                            }
                            showContactsPicker = false
                            onDismiss()
                        },
                        enabled = selectedContactIds.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SplitEmerald, contentColor = Color.Black)
                    ) {
                        Text("Import Selected (${selectedContactIds.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                .testTag("add_friend_dialog"),
            color = SurfaceDark,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add a Friend",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_add_friend_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Import from Contacts Button
                OutlinedButton(
                    onClick = {
                        if (isDemoMode) {
                            showDemoWarning = true
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                val contacts = ContactsUtils.queryDeviceContacts(context)
                                val filtered = ContactsUtils.filterDuplicates(contacts, existingFriends)
                                availableContacts = filtered
                                showContactsPicker = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SplitIndigoLight)
                ) {
                    Icon(imageVector = Icons.Default.Contacts, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import from Contacts", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceBorder)
                    Text("  OR ADD MANUALLY  ", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SurfaceBorder)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Friend's Full Name") },
                    placeholder = { Text("e.g. Rachel Green") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = SplitIndigoLight)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SplitEmerald,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = SplitEmerald
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    placeholder = { Text("e.g. rachel@splitmate.app") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = SplitIndigoLight)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_email_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SplitIndigo,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") },
                    placeholder = { Text("e.g. +1 555-234-5678") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = SplitIndigoLight)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SplitIndigo,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = SplitRose,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMessage = "Please enter your friend's name."
                        } else {
                            onAddFriend(name.trim(), email.trim(), phone.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_add_friend_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SplitEmerald,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Friend", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
