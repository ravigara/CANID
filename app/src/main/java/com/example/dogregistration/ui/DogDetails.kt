package com.example.dogregistration.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dogregistration.data.DogProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DogDetailsScreen(
    dog: DogProfile,
    onBack: () -> Unit
) {
    // If a single persisted imageUri was saved with the profile, parse it here
    val profileImageUri = remember(dog.imageUri) {
        dog.imageUri?.let {
            runCatching { Uri.parse(it) }.getOrNull()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dog.dogName.ifBlank { "Dog details" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- Profile Image Section ---
            if (profileImageUri != null) {
                Text("Dog nose profile", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = "Primary dog image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                    onError = {
                        // Error loading profile image - will show placeholder
                    }
                )
            } else {
                Card(
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Column {
                            Text("No profile photo available.", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "A nose/profile photo is not saved for this dog. " +
                                        "If you have previously registered a photo, it may have been removed or the profile wasn't saved correctly.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            // Use HorizontalDivider per Material3 deprecation
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // --- Details Section ---
            DetailRow("Owner", dog.ownerName ?: "N/A")
            DetailRow("Breed", dog.breed.ifBlank { "Unknown" })
            DetailRow("Dog Type", dog.dogType.displayName)
            DetailRow("Age", "${dog.ageInMonths} months")
            DetailRow("Gender", dog.gender.name)
            DetailRow("Primary Color", dog.primaryColor.displayName)
            DetailRow("Secondary Color", dog.secondaryColor.displayName)
            DetailRow("Adoption Date", dog.adoptionDate ?: "N/A")
            DetailRow("Microchip Number", dog.microchipNumber ?: "N/A")

            if (dog.vaccinations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Vaccination History", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                dog.vaccinations.forEach { record ->
                    Text("• ${record.date}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
