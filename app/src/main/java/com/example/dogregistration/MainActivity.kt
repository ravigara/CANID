package com.example.dogregistration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.dogregistration.camera.NoseDetector
import com.example.dogregistration.camera.NoseRecognizer
import com.example.dogregistration.data.AppDatabase
import com.example.dogregistration.data.DogProfile
import com.example.dogregistration.ui.*

// Helper composable to manage NoseDetector lifecycle
@Composable
fun IdentifyDogScreenWithDetector(
    database: AppDatabase,
    recognizer: NoseRecognizer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val noseDetector = remember { NoseDetector(context, "dog_nose_classifier.tflite") }
    DisposableEffect(Unit) {
        onDispose { noseDetector.close() }
    }
    
    IdentifyDogScreen(
        database = database,
        recognizer = recognizer,
        noseDetector = noseDetector,
        onBack = onBack
    )
}

// Navigation state
sealed class Screen {
    object Welcome : Screen()
    object Register : Screen()
    object Identify : Screen()
    object RegisteredDogs : Screen()
    data class DogDetails(val dog: DogProfile) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainAppContent()
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val database = AppDatabase.getInstance(context)
    
    // Navigation state
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Welcome) }

    // Initialize NoseRecognizer
    val noseRecognizer = remember {
        try {
            NoseRecognizer(context)
        } catch (e: Exception) {
            null
        }
    }

    // Navigate based on current screen
    when (val screen = currentScreen) {
        is Screen.Welcome -> {
            WelcomeScreen(
                onRegisterClick = { currentScreen = Screen.Register },
                onScanClick = { currentScreen = Screen.Identify },
                onViewRegistered = { currentScreen = Screen.RegisteredDogs }
            )
        }
        is Screen.Register -> {
            if (noseRecognizer != null) {
                RegisterDogScreen(
                    database = database,
                    recognizer = noseRecognizer,
                    onComplete = { currentScreen = Screen.Welcome },
                    onBack = { currentScreen = Screen.Welcome }
                )
            } else {
                // Fallback if NoseRecognizer failed to initialize
                WelcomeScreen(
                    onRegisterClick = { currentScreen = Screen.Register },
                    onScanClick = { currentScreen = Screen.Identify },
                    onViewRegistered = { currentScreen = Screen.RegisteredDogs }
                )
            }
        }
        is Screen.Identify -> {
            if (noseRecognizer != null) {
                IdentifyDogScreenWithDetector(
                    database = database,
                    recognizer = noseRecognizer,
                    onBack = { currentScreen = Screen.Welcome }
                )
            } else {
                // Fallback if NoseRecognizer failed to initialize
                WelcomeScreen(
                    onRegisterClick = { currentScreen = Screen.Register },
                    onScanClick = { currentScreen = Screen.Identify },
                    onViewRegistered = { currentScreen = Screen.RegisteredDogs }
                )
            }
        }
        is Screen.RegisteredDogs -> {
            RegisteredDogsScreen(
                database = database,
                onBack = { currentScreen = Screen.Welcome },
                onSelectDog = { dog ->
                    currentScreen = Screen.DogDetails(dog)
                }
            )
        }
        is Screen.DogDetails -> {
            DogDetailsScreen(
                dog = screen.dog,
                onBack = { currentScreen = Screen.RegisteredDogs }
            )
        }
    }
}
