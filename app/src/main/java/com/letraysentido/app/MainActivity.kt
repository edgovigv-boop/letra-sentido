package com.letraysentido.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkOverlayPermission()
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, AudioCaptureService::class.java)
            serviceIntent.putExtra("resultCode", result.resultCode)
            serviceIntent.putExtra("data", result.data)
            startForegroundService(serviceIntent)
            startOverlayService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onStartCapture = { startCaptureFlow() },
                        onStopCapture = { stopServices() }
                    )
                }
            }
        }
    }

    private fun startCaptureFlow() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            !Settings.canDrawOverlays(this) -> {
                checkOverlayPermission()
            }
            else -> {
                requestMediaProjection()
            }
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            requestMediaProjection()
        }
    }

    private fun requestMediaProjection() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun startOverlayService() {
        val intent = Intent(this, FloatingOverlayService::class.java)
        startService(intent)
    }

    private fun stopServices() {
        stopService(Intent(this, AudioCaptureService::class.java))
        stopService(Intent(this, FloatingOverlayService::class.java))
    }
}

@Composable
fun MainScreen(onStartCapture: () -> Unit, onStopCapture: () -> Unit) {
    var isCapturing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Letra y Sentido",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Captura audio del sistema y muestra letras flotantes",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (isCapturing) {
                    onStopCapture()
                } else {
                    onStartCapture()
                }
                isCapturing = !isCapturing
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isCapturing) "Detener Captura" else "Iniciar Captura")
        }
    }
}