package com.example.notes.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun OcrScannerDialog(
    onDismiss: () -> Unit,
    onTextScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            
                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            
                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                                
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("CameraX", "Binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Extracting Text...", color = MaterialTheme.colorScheme.onBackground)
                    } else {
                        Button(
                            onClick = {
                                val currentCapture = imageCapture ?: return@Button
                                isProcessing = true
                                
                                val photoFile = File(context.cacheDir, "ocr_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                
                                currentCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            val savedUri = Uri.fromFile(photoFile)
                                            processImageWithMlKit(context, savedUri) { text ->
                                                isProcessing = false
                                                if (text.isNotBlank()) {
                                                    onTextScanned(text)
                                                    onDismiss()
                                                } else {
                                                    Log.d("OCR", "No text detected")
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isProcessing = false
                                            Log.e("CameraX", "Photo capture failed", exception)
                                        }
                                    }
                                )
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Text("SCAN")
                        }
                    }
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

private fun processImageWithMlKit(context: Context, imageUri: Uri, onComplete: (String) -> Unit) {
    try {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val filteredText = filterKeyboardNoise(visionText.text)
                onComplete(filteredText)
            }
            .addOnFailureListener { e ->
                Log.e("MLKit", "Text recognition failed", e)
                onComplete("")
            }
    } catch (e: Exception) {
        Log.e("MLKit", "Failed to load image", e)
        onComplete("")
    }
}

private fun filterKeyboardNoise(text: String): String {
    val keysRegex = Regex(
        "(?i)\\b(ctrl|crtl|crv|esc|del|delete|delote|caps\\s*lock|alt|shift|tab|enter|insert|home|end|page\\s*up|page\\s*down|pgup|pgdn|fn|f[1-9]|f1[0-2])\\b"
    )
    val combinationsRegex = Regex("(?i)\\b(ctrl|crtl|alt|shift|ot)\\s*[+\\-_]\\s*[a-z0-9_]+\\b")
    
    var cleaned = text.replace(combinationsRegex, "")
    cleaned = cleaned.replace(keysRegex, "")
    
    return cleaned.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n")
}
