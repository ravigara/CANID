package com.example.dogregistration.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dogregistration.data.AppDatabase
import com.example.dogregistration.data.DogProfile
import com.example.dogregistration.util.FolderImageResult
import com.example.dogregistration.util.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisteredDogsScreen(
    database: AppDatabase,
    onBack: () -> Unit,
    onSelectDog: (DogProfile) -> Unit
) {
    val profilesFlow = remember { database.dogDao().getAllDogProfilesFlow() }
    val profiles by profilesFlow.collectAsState(initial = emptyList())

    // Sort by newest first
    val sorted = remember(profiles) { profiles.sortedByDescending { it.createdAt } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registered Dogs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (sorted.isEmpty()) {
                Text(
                    text = "No registered dogs found.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sorted) { dog ->
                        DogListItem(dog = dog, onClick = { onSelectDog(dog) })
                    }
                }
            }
        }
    }
}

@Composable
fun DogListItem(dog: DogProfile, onClick: () -> Unit) {
    val context = LocalContext.current
    val preview by produceState(initialValue = FolderImageResult(emptyList(), null), dog.storageUri) {
        if (dog.storageUri.isNullOrBlank()) {
            value = FolderImageResult(emptyList(), null)
        } else {
            value = withContext(Dispatchers.IO) {
                StorageUtils.loadFolderImages(context, dog.storageUri, limit = 1)
            }
        }
    }
    val previewUri = preview.images.firstOrNull()

    val formattedTime = remember(dog.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(dog.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (previewUri != null) {
                AsyncImage(
                    model = previewUri,
                    contentDescription = "Dog preview",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = "Dog Icon",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (dog.dogName.isNotBlank()) dog.dogName else "Unnamed Dog",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Breed: ${dog.breed.ifBlank { "Unknown" }}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Registered: $formattedTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}