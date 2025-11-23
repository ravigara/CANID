package com.example.dogregistration.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class NoseRecognizer(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val loadLock = ReentrantLock()
    private val runLock = ReentrantLock()

    // Must match your dog_nose_model.tflite input
    private val MODEL_NAME = "dog_nose_model.tflite"
    private val INPUT_IMAGE_WIDTH = 224
    private val INPUT_IMAGE_HEIGHT = 224
    private val EMBEDDING_SIZE = 128

    private val imageProcessor: ImageProcessor by lazy {
        ImageProcessor.Builder()
            // 1. Resize (Stretch to fit) - Better than Crop for embeddings
            .add(ResizeOp(INPUT_IMAGE_HEIGHT, INPUT_IMAGE_WIDTH, ResizeOp.ResizeMethod.BILINEAR))

            // 2. Normalize to [-1, 1]
            // This is the standard for MobileNet-based models used for embeddings
            // Formula: (Pixel - 127.5) / 127.5
            .add(NormalizeOp(127.5f, 127.5f))
            .build()
    }

    init {
        loadModel()
    }

    private fun loadModel() {
        loadLock.withLock {
            try {
                val modelByteBuffer: ByteBuffer = FileUtil.loadMappedFile(context, MODEL_NAME)
                val options = Interpreter.Options().apply { setNumThreads(2) }
                interpreter = Interpreter(modelByteBuffer, options)
                Log.i("NoseRecognizer", "TFLite model loaded.")
            } catch (e: Exception) {
                Log.e("NoseRecognizer", "Error loading model", e)
                interpreter = null
            }
        }
    }

    fun isReady(): Boolean = interpreter != null

    fun close() {
        runLock.withLock {
            interpreter?.close()
            interpreter = null
        }
    }

    // Generate Embedding Vector (Float Array)
    fun getEmbedding(imageUri: Uri): FloatArray? {
        val localInterpreter = interpreter ?: return null

        val bitmap = loadBitmapFromUri(imageUri) ?: return null
        val tensorImage = TensorImage.fromBitmap(bitmap)

        // Process: Resize -> Normalize [-1, 1]
        val processed = imageProcessor.process(tensorImage)

        val outputBuffer = Array(1) { FloatArray(EMBEDDING_SIZE) }

        runLock.withLock {
            return try {
                localInterpreter.run(processed.buffer, outputBuffer)

                // L2 Normalization (Important for Euclidean Distance)
                // Makes the vector length = 1.0, so distance calculations are stable
                val emb = outputBuffer[0]
                var norm = 0f
                for (v in emb) norm += v * v
                norm = kotlin.math.sqrt(norm).coerceAtLeast(1e-12f)
                for (i in emb.indices) emb[i] = emb[i] / norm

                emb
            } catch (e: Exception) {
                Log.e("NoseRecognizer", "Inference error", e)
                null
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val src = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: IOException) {
            Log.e("NoseRecognizer", "Failed to decode bitmap", e)
            null
        }
    }
}