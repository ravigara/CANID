package com.example.dogregistration.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.dogregistration.camera.NoseDetector
import com.example.dogregistration.camera.NoseRecognizer
import com.example.dogregistration.data.AppDatabase
import com.example.dogregistration.data.DogProfile
import com.example.dogregistration.data.euclideanDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentifyDogScreen(
    database: AppDatabase,
    recognizer: NoseRecognizer,
    noseDetector: NoseDetector,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Helper: Create URI
    fun createTempUri(): Uri? {
        return try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile("scan_${System.currentTimeMillis()}", ".jpg", storageDir)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    // --- Identification Logic ---
    fun identifyImage(uri: Uri) {
        isProcessing = true
        resultText = "Analyzing..."

        coroutineScope.launch(Dispatchers.IO) {
            // 1. Validation: Is it a dog nose?
            // (Using the new logic: 0.0 = Nose, 1.0 = Not Nose)
            val (_, score) = noseDetector.isNoseFromUri(uri)

            // Threshold 0.5: If score > 0.5, it's likely NOT a nose.
            if (score > 0.5) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    val conf = (score * 100).toInt()
                    resultText = "Error: This does not look like a dog nose.\n(Model Score: $conf% 'Not Nose')"
                }
                return@launch
            }

            // 2. Recognition: Get Embedding
            val emb = recognizer.getEmbedding(uri)
            if (emb == null) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    resultText = "Error: Could not generate nose ID."
                }
                return@launch
            }

            // 3. Database Compare
            val profiles = database.dogDao().getAllDogProfiles()
            if (profiles.isEmpty()) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    resultText = "No dogs registered in database."
                }
                return@launch
            }

            // 4. Find Nearest Neighbor
            var best: DogProfile? = null
            var bestDist = Float.MAX_VALUE

            profiles.forEach { p ->
                p.embedding?.let { dbEmb ->
                    val d = euclideanDistance(emb, dbEmb)
                    if (d < bestDist) {
                        bestDist = d
                        best = p
                    }
                }
            }

            // 5. Result
            withContext(Dispatchers.Main) {
                isProcessing = false
                // Distance Threshold: Lower is better.
                // < 0.6 is a strong match. < 0.8 is a weak match. > 1.0 is usually different.
                val matchThreshold = 0.75f

                if (best != null) {
                    if (bestDist < matchThreshold) {
                        resultText = "MATCH FOUND!\n\n" +
                                "Dog Name: ${best?.dogName}\n" +
                                "Breed: ${best?.breed}\n" +
                                "Owner: ${best?.ownerName}\n" +
                                "\nDistance: ${"%.3f".format(bestDist)} (Excellent Match)"
                    } else {
                        resultText = "NO MATCH FOUND.\n\n" +
                                "Closest was: ${best?.dogName}\n" +
                                "Distance: ${"%.3f".format(bestDist)}\n" +
                                "(Too different to be a match)"
                    }
                } else {
                    resultText = "Database has no valid embeddings."
                }
            }
        }
    }

    // --- Launchers ---
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) identifyImage(tempCameraUri!!)
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val uri = createTempUri()
            if (uri != null) { tempCameraUri = uri; takePictureLauncher.launch(uri) }
        } else { Toast.makeText(context, "Camera Permission Required", Toast.LENGTH_SHORT).show() }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) identifyImage(uri)
    }

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identify Dog") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        val uri = createTempUri()
                        if (uri != null) { tempCameraUri = uri; takePictureLauncher.launch(uri) }
                    } else {
                        cameraPermLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF03DAC5),
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.large,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Scan with Camera",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF6200EE)
                ),
                border = BorderStroke(2.dp, Color(0xFF6200EE)),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Select from Gallery",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }

            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("How to identify with CANID", style = MaterialTheme.typography.titleMedium)
                    Text("1. Capture a clear, well-lit photo of the dog's nose (or pick from gallery).", style = MaterialTheme.typography.bodyMedium)
                    Text("2. Ensure the nose fills most of the frame and is in focus (no blur).", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Tap Identify to let CANID compare the nose print with registered dogs.", style = MaterialTheme.typography.bodyMedium)
                    Text("4. If you get 'No match', try another angle or refresh the dog's registration photos.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(24.dp))

            if (isProcessing) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
            }

            resultText?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(it, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}