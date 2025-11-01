package abualqasim.dr3.usul.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraController
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import abualqasim.dr3.usul.ui.theme.UsulTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestIntent = intent
        val startType = requestIntent.extractStartType()
        val title = requestIntent.extractTitle()
        val initialNear = requestIntent.extractNear()
        val initialFar = requestIntent.extractFar()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            UsulTheme {
                CameraCaptureScreen(
                    title = title,
                    startType = startType,
                    initialNear = initialNear,
                    initialFar = initialFar,
                    onResult = { near, far ->
                        setResult(Activity.RESULT_OK, Intent().putCameraResult(near, far))
                        finish()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraCaptureScreen(
    title: String,
    startType: PhotoType,
    initialNear: String?,
    initialFar: String?,
    onResult: (String?, String?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (!granted) {
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!permissionGranted) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Camera permission required",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    DisposableEffect(controller, lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }

    var currentType by remember { mutableStateOf(startType) }
    var nearPath by remember { mutableStateOf(initialNear) }
    var farPath by remember { mutableStateOf(initialFar) }
    var capturedPath by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    val executor = remember { ContextCompat.getMainExecutor(context) }

    fun sanitizeTitle(raw: String): String {
        val fallback = if (raw.isBlank()) "Photo" else raw
        return fallback.replace(Regex("[^A-Za-z0-9_\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF ]"), " ")
            .trim()
            .ifBlank { "Photo" }
    }

    fun createTargetFile(type: PhotoType): File {
        val safeTitle = sanitizeTitle(title)
        val stamp = SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault()).format(Date())
        val suffix = when (type) {
            PhotoType.NEAR -> "near"
            PhotoType.FAR -> "far"
        }
        return File(context.cacheDir, "$safeTitle $stamp $suffix.jpg")
    }

    fun discard(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    fun takePhoto() {
        val file = createTargetFile(currentType)
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        isCapturing = true
        controller.takePicture(
            options,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    discard(file.absolutePath)
                    isCapturing = false
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    capturedPath = file.absolutePath
                    isCapturing = false
                }
            }
        )
    }

    fun finishWithResult() {
        val path = capturedPath
        if (path != null) {
            when (currentType) {
                PhotoType.NEAR -> nearPath = path
                PhotoType.FAR -> farPath = path
            }
        }
        onResult(nearPath, farPath)
    }

    fun proceedToFar() {
        val path = capturedPath ?: return
        nearPath = path
        capturedPath = null
        currentType = PhotoType.FAR
    }

    BackHandler {
        discard(capturedPath)
        onCancel()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.controller = controller
                }
            }
        )

        if (capturedPath != null) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = ImageRequest.Builder(context)
                    .data(File(capturedPath!!))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    discard(capturedPath)
                    onCancel()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
                Text(
                    text = when (currentType) {
                        PhotoType.NEAR -> "Near photo"
                        PhotoType.FAR -> "Far photo"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (capturedPath == null) {
                    Button(
                        onClick = ::takePhoto,
                        enabled = !isCapturing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = CircleShape,
                        modifier = Modifier.size(96.dp)
                    ) {}
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    discard(capturedPath)
                                    onCancel()
                                }
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { finishWithResult() }
                            ) {
                                Text("Accept")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (currentType == PhotoType.NEAR) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = { proceedToFar() }
                                ) {
                                    Text("Next")
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    discard(capturedPath)
                                    capturedPath = null
                                }
                            ) {
                                Text("Redo")
                            }
                        }
                    }
                }
            }
        }
    }
}
