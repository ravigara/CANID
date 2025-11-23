package com.example.dogregistration.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import coil.compose.rememberAsyncImagePainter
import com.example.dogregistration.camera.NoseDetector
import com.example.dogregistration.camera.NoseRecognizer
import com.example.dogregistration.data.*
import com.example.dogregistration.util.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDogScreen(
    database: AppDatabase,
    recognizer: NoseRecognizer,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Classifier
    val noseDetector = remember { NoseDetector(context, "dog_nose_classifier.tflite") }
    DisposableEffect(Unit) { onDispose { noseDetector.close() } }

    // Form State
    val dogName = remember { mutableStateOf("") }
    val breed = remember { mutableStateOf("") }
    val ageInMonths = remember { mutableStateOf("") }
    val ownerName = remember { mutableStateOf("") }
    val gender = remember { mutableStateOf(DogGender.None) }
    val primaryColor = remember { mutableStateOf(DogColor.NONE) }
    val secondaryColor = remember { mutableStateOf(DogColor.NONE) }
    val dogType = remember { mutableStateOf(DogType.PET) }

    val capturedImages = remember { mutableStateListOf<Uri>() }
    val selectedFolderUri = remember { mutableStateOf<Uri?>(null) }

    // UI State
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Validation State
    var showValidationReportDialog by remember { mutableStateOf(false) }
    var validationResults by remember { mutableStateOf<List<Triple<Uri, Float, Boolean>>>(emptyList()) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // --- Helpers & Launchers (Same as before) ---
    fun createTempUri(): Uri? {
        return try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir != null && !storageDir.exists()) storageDir.mkdirs()
            val file = File.createTempFile("nose_${System.currentTimeMillis()}", ".jpg", storageDir)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) capturedImages.add(tempCameraUri!!)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val uri = createTempUri()
            if (uri != null) { tempCameraUri = uri; takePictureLauncher.launch(uri) }
        } else { Toast.makeText(context, "Permission required", Toast.LENGTH_SHORT).show() }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { uri ->
            try {
                val flags = (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
        }
        capturedImages.addAll(uris)
    }

    val pickFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        treeUri?.let { uri ->
            try {
                val flags = (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                context.contentResolver.takePersistableUriPermission(uri, flags)
                selectedFolderUri.value = uri
            } catch (e: Exception) { Toast.makeText(context, "Permission error", Toast.LENGTH_SHORT).show() }
        }
    }

    // --- Validation Check (0.0 = Nose) ---
    fun isNose(score: Float): Boolean {
        return score <= 0.5f
    }

    // --- Validate (Force Dialog) ---
    fun validateImages() {
        if (capturedImages.isEmpty()) {
            Toast.makeText(context, "No images selected.", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessing = true
        coroutineScope.launch(Dispatchers.IO) {
            val evaluation = evaluateCapturedImages(noseDetector, capturedImages.toList(), ::isNose)
            withContext(Dispatchers.Main) {
                isProcessing = false
                validationResults = evaluation.entries.map { Triple(it.uri, it.score, it.passed) }
                showValidationReportDialog = true // Show dialog regardless of result
            }
        }
    }

    // --- Submit ---
    fun submitRegistration(ignoreValidation: Boolean = false) {
        if (capturedImages.size < 2) { Toast.makeText(context, "Upload 2+ images", Toast.LENGTH_LONG).show(); return }
        if (selectedFolderUri.value == null) { Toast.makeText(context, "Select folder", Toast.LENGTH_LONG).show(); return }
        if (dogName.value.isBlank()) { Toast.makeText(context, "Name required", Toast.LENGTH_SHORT).show(); return }

        isProcessing = true
        coroutineScope.launch(Dispatchers.IO) {
            val evaluation = evaluateCapturedImages(noseDetector, capturedImages.toList(), ::isNose)
            val hasFailure = evaluation.entries.any { !it.passed }
            if (hasFailure && !ignoreValidation) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    validationResults = evaluation.entries.map { Triple(it.uri, it.score, it.passed) }
                    showValidationReportDialog = true
                }
                return@launch
            }

            val embeddingSource = (evaluation.bestPassing ?: evaluation.bestOverall)?.uri
            if (embeddingSource == null) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    Toast.makeText(context, "Unable to pick a valid image for embedding.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            // 2. Save
            try {
                val embedding = recognizer.getEmbedding(embeddingSource)
                if (embedding == null) {
                    withContext(Dispatchers.Main) { isProcessing = false; Toast.makeText(context, "Embedding failed", Toast.LENGTH_LONG).show() }
                    return@launch
                }

                val folderUri = selectedFolderUri.value!!
                // Ensure we have persistable permission on the parent tree first
                val persistFlags = (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                StorageUtils.takePersistablePermission(context.contentResolver, folderUri, persistFlags)
                
                val tree = DocumentFile.fromTreeUri(context, folderUri)
                if (tree == null || !tree.canWrite()) {
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        Toast.makeText(context, "Unable to access the selected folder. Please re-select it.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                val subName = (dogName.value.ifBlank { "dog" }) + "_" + sdf.format(Date())
                val subFolder = tree.createDirectory(subName)
                if (subFolder == null || !subFolder.exists()) {
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        Toast.makeText(context, "Failed to create dog folder. Please try again.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                // Persist permission on the subfolder as well - use document URI directly
                StorageUtils.takePersistablePermission(context.contentResolver, subFolder.uri, persistFlags)
                
                // Store the subfolder's document URI - this is more reliable than tree URI for subfolders
                // The document URI can be resolved directly without needing to navigate from parent
                val storageUriToSave = subFolder.uri

                var firstImageUri: String? = null
                capturedImages.forEachIndexed { index, uri ->
                    val dest = subFolder.createFile("image/jpeg", "nose_${index}_${System.currentTimeMillis()}.jpg")
                    if (dest != null) {
                        copyUriToUri(context, uri, dest.uri)
                        // Also persist permission on each image file
                        StorageUtils.takePersistablePermission(context.contentResolver, dest.uri, persistFlags)
                        // Use the first image as the profile image
                        if (index == 0) {
                            firstImageUri = dest.uri.toString()
                        }
                    }
                }

                val profile = DogProfile(
                    dogName = dogName.value,
                    breed = breed.value,
                    ageInMonths = ageInMonths.value.toIntOrNull() ?: 0,
                    gender = gender.value,
                    primaryColor = primaryColor.value,
                    secondaryColor = secondaryColor.value,
                    dogType = dogType.value,
                    ownerName = ownerName.value,
                    embedding = embedding,
                    createdAt = System.currentTimeMillis(),
                    imageUri = firstImageUri,
                    storageUri = storageUriToSave.toString()
                )
                database.dogDao().insertDog(profile)

                withContext(Dispatchers.Main) {
                    isProcessing = false
                    Toast.makeText(context, "Success!", Toast.LENGTH_LONG).show()
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- UI Layout (Same structure) ---
    Scaffold(
        topBar = { TopAppBar(title = { Text("Register New Dog") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            // ... (Existing UI elements for Images, Button, Form Fields are unchanged) ...
            // Keeping it concise here, reuse the layout from previous responses.
            Text("Dog Nose Images (Min 2)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Card(modifier = Modifier.size(120.dp).clickable { showImageSourceDialog = true }, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, "Add", modifier = Modifier.size(40.dp)) }
                    }
                }
                items(capturedImages) { uri ->
                    Box(modifier = Modifier.size(120.dp)) {
                        Image(painter = rememberAsyncImagePainter(uri), contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        IconButton(onClick = { capturedImages.remove(uri) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)) { Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp)) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showImageSourceDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE),
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Select .jpeg Image", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))

            // CONFIRM BUTTON
            OutlinedButton(
                onClick = { validateImages() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isProcessing && capturedImages.isNotEmpty(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF03DAC5)
                ),
                border = BorderStroke(2.dp, Color(0xFF03DAC5)),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm Images", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp)); Divider(); Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = dogName.value, onValueChange = { dogName.value = it }, label = { Text("Dog Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = breed.value, onValueChange = { breed.value = it }, label = { Text("Breed") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = ageInMonths.value, onValueChange = { if (it.all { c -> c.isDigit() }) ageInMonths.value = it }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            GenderDropdown(gender.value) { gender.value = it }
            Spacer(modifier = Modifier.height(8.dp))
            ColorDropdown("Primary Color", primaryColor.value) { primaryColor.value = it }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = ownerName.value, onValueChange = { ownerName.value = it }, label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (selectedFolderUri.value != null) "Folder Selected" else "No Folder", color = if (selectedFolderUri.value == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Button(
                    onClick = { pickFolderLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EE),
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.small,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Text("Select Folder", fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { submitRegistration(ignoreValidation = false) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE),
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.large,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text(
                        "Register Dog",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    }

    if (showImageSourceDialog) {
        Dialog(onDismissRequest = { showImageSourceDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Add Photo", style = MaterialTheme.typography.titleLarge); Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                            showImageSourceDialog = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                val uri = createTempUri()
                                if (uri != null) { tempCameraUri = uri; takePictureLauncher.launch(uri) }
                            } else { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                        }) { Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary); Text("Camera") }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                            showImageSourceDialog = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary); Text("Gallery") }
                    }
                    Spacer(modifier = Modifier.height(16.dp)); TextButton(onClick = { showImageSourceDialog = false }) { Text("Cancel") }
                }
            }
        }
    }

    // --- DETAILED REPORT DIALOG ---
    if (showValidationReportDialog) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(if (validationResults.any { !it.third }) Icons.Default.Warning else Icons.Default.CheckCircle, null) },
            title = { Text(if (validationResults.any { !it.third }) "Validation Issues" else "Validation Report") },
            text = {
                Column {
                    Text("Raw Scores", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    validationResults.forEachIndexed { i, (_, score, passed) ->
                        val color = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        val label = if (passed) "PASS" else "FAIL"
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text("Image ${i + 1}: ${"%.4f".format(score)} ($label)", color = color)
                            if (!passed) {
                                Text(
                                    text = "The image uploaded may be blurred or is not a dog nose image.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    if (validationResults.any { !it.third }) {
                        Spacer(Modifier.height(16.dp))
                        Text("Do you want to proceed anyway?")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showValidationReportDialog = false
                    // If triggered by submit (fields valid), proceed.
                    if (dogName.value.isNotBlank() && selectedFolderUri.value != null) {
                        submitRegistration(ignoreValidation = true)
                    }
                }) { Text(if (validationResults.any { !it.third }) "Proceed Anyway" else "OK") }
            },
            dismissButton = {
                TextButton(onClick = { showValidationReportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun copyUriToUri(context: Context, srcUri: Uri, dstUri: Uri) {
    val resolver = context.contentResolver
    resolver.openInputStream(srcUri)?.use { input -> resolver.openOutputStream(dstUri)?.use { output -> input.copyTo(output) } }
}

private data class ImageValidationEntry(val uri: Uri, val score: Float, val passed: Boolean)

private data class ImageEvaluation(
    val entries: List<ImageValidationEntry>,
    val bestPassing: ImageValidationEntry?,
    val bestOverall: ImageValidationEntry?
)

private fun evaluateCapturedImages(
    detector: NoseDetector,
    images: List<Uri>,
    classifier: (Float) -> Boolean
): ImageEvaluation {
    val entries = mutableListOf<ImageValidationEntry>()
    var bestPassing: ImageValidationEntry? = null
    var bestOverall: ImageValidationEntry? = null

    images.forEach { uri ->
        val (_, score) = detector.isNoseFromUri(uri)
        val passed = classifier(score)
        val entry = ImageValidationEntry(uri, score, passed)
        entries += entry

        if (passed && (bestPassing == null || score < bestPassing!!.score)) {
            bestPassing = entry
        }
        if (bestOverall == null || score < bestOverall!!.score) {
            bestOverall = entry
        }
    }

    return ImageEvaluation(entries, bestPassing, bestOverall)
}