package com.example.facedetector.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.facedetector.ui.camera.CameraPreview
import com.example.facedetector.viewmodel.DetailUiState
import com.example.facedetector.viewmodel.DetailViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DetailScreen(
    dni: String,
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory(dni))
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentFace by viewModel.currentFace.collectAsState()
    val newCapturedBitmap by viewModel.newCapturedBitmap.collectAsState()

    var showCamera by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(uiState) {
        if (uiState is DetailUiState.Deleted) {
            kotlinx.coroutines.delay(1500)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Registro") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showCamera && cameraPermissionState.status.isGranted) {
                CameraPreview(
                    onImageCaptured = { bitmap ->
                        viewModel.setCapturedImage(bitmap)
                        showCamera = false
                    },
                    onError = { error ->
                        showCamera = false
                    }
                )
            } else {
                currentFace?.let { face ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Información del Registro",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Foto actual
                        Card(
                            modifier = Modifier
                                .size(250.dp)
                        ) {
                            val bitmap = BitmapFactory.decodeByteArray(
                                face.imagenBytes,
                                0,
                                face.imagenBytes.size
                            )
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Foto actual",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Información
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Nombre",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = face.nombre,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "DNI",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = face.dni,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Fecha de Registro",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = java.text.SimpleDateFormat(
                                        "dd/MM/yyyy HH:mm",
                                        java.util.Locale.getDefault()
                                    ).format(java.util.Date(face.timestamp)),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Mostrar nueva foto capturada
                        if (newCapturedBitmap != null) {
                            Text(
                                text = "Nueva foto capturada:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.size(150.dp)
                            ) {
                                Image(
                                    bitmap = newCapturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Nueva foto",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Botón para actualizar foto
                        Button(
                            onClick = {
                                if (newCapturedBitmap == null) {
                                    if (cameraPermissionState.status.isGranted) {
                                        showCamera = true
                                    } else {
                                        cameraPermissionState.launchPermissionRequest()
                                    }
                                } else {
                                    viewModel.updatePhoto()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState !is DetailUiState.Loading
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            if (uiState is DetailUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(if (newCapturedBitmap == null) "Cambiar Foto" else "Actualizar Foto")
                            }
                        }

                        if (newCapturedBitmap != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.cancelPhotoUpdate() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancelar")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mostrar mensajes
                        when (val state = uiState) {
                            is DetailUiState.Success -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.message,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            is DetailUiState.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.message,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            is DetailUiState.Deleted -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Registro eliminado",
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }

            // Diálogo de confirmación para eliminar
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Eliminar Registro") },
                    text = { Text("¿Estás seguro de que deseas eliminar este registro? Esta acción no se puede deshacer.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteFace()
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Eliminar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

