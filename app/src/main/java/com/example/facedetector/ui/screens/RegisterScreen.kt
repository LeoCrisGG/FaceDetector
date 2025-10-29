package com.example.facedetector.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.facedetector.ui.camera.CameraPreview
import com.example.facedetector.viewmodel.RegisterUiState
import com.example.facedetector.viewmodel.RegisterViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()

    var dni by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var showCamera by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(uiState) {
        if (uiState is RegisterUiState.Success) {
            kotlinx.coroutines.delay(2000)
            viewModel.resetState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Rostro") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetState()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
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
                        // Mostrar error
                        showCamera = false
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mostrar imagen capturada
                    if (capturedBitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Foto capturada",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Campo DNI
                    OutlinedTextField(
                        value = dni,
                        onValueChange = { if (it.length <= 8) dni = it },
                        label = { Text("DNI (8 dígitos)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = dni.isNotEmpty() && dni.length != 8
                    )

                    if (dni.isNotEmpty() && dni.length != 8) {
                        Text(
                            text = "El DNI debe tener exactamente 8 dígitos",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo Nombre
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón capturar/recapturar
                    Button(
                        onClick = {
                            if (cameraPermissionState.status.isGranted) {
                                showCamera = true
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (capturedBitmap == null) "Capturar Foto" else "Recapturar Foto")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón registrar
                    Button(
                        onClick = {
                            capturedBitmap?.let { bitmap ->
                                viewModel.registerFace(dni, nombre, bitmap)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = capturedBitmap != null &&
                                dni.length == 8 &&
                                nombre.isNotBlank() &&
                                uiState !is RegisterUiState.Loading
                    ) {
                        if (uiState is RegisterUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Registrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mostrar mensajes de estado
                    when (uiState) {
                        is RegisterUiState.Success -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = (uiState as RegisterUiState.Success).message,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        is RegisterUiState.Error -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = (uiState as RegisterUiState.Error).message,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

