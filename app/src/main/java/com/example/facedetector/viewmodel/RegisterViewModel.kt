package com.example.facedetector.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.facedetector.data.FaceDatabase
import com.example.facedetector.data.FaceEntity
import com.example.facedetector.data.FaceRepository
import com.example.facedetector.ml.FaceDetectionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * RegisterViewModel - ViewModel para la pantalla de registro facial
 *
 * Este ViewModel gestiona toda la lógica de negocio para registrar un nuevo rostro.
 * Implementa el patrón MVVM (Model-View-ViewModel) separando la lógica de la UI.
 *
 * AndroidViewModel: Extiende AndroidViewModel porque necesita acceso al Application context
 * para obtener la instancia de la base de datos.
 *
 * Responsabilidades:
 * - Capturar y almacenar la imagen facial
 * - Validar DNI (8 dígitos) y nombre
 * - Detectar rostro usando ML Kit
 * - Extraer características faciales
 * - Guardar en la base de datos
 * - Gestionar estados de UI (loading, success, error)
 */
class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    // Repository para acceder a la base de datos
    private val repository: FaceRepository

    // Servicio de detección facial usando ML Kit
    private val faceDetectionService = FaceDetectionService()

    /**
     * _uiState es un MutableStateFlow privado que solo el ViewModel puede modificar
     * uiState es un StateFlow público que la UI puede observar pero no modificar
     *
     * StateFlow es un holder de estado observable que emite el valor actual
     * y todas las actualizaciones futuras. La UI se recompone automáticamente
     * cuando el estado cambia.
     */
    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Initial)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /**
     * Almacena la imagen capturada por la cámara
     * Se expone como StateFlow para que la UI pueda mostrarla en preview
     */
    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    /**
     * init se ejecuta cuando se crea el ViewModel
     * Inicializa el repository con el DAO de la base de datos
     */
    init {
        val faceDao = FaceDatabase.getDatabase(application).faceDao()
        repository = FaceRepository(faceDao)
    }

    /**
     * Guarda la imagen capturada en el estado
     * Llamado cuando el usuario captura una foto con la cámara
     *
     * @param bitmap La imagen capturada
     */
    fun setCapturedImage(bitmap: Bitmap) {
        _capturedBitmap.value = bitmap
    }

    /**
     * Registra un nuevo rostro en la base de datos
     *
     * Este es el método principal que realiza todo el flujo de registro:
     * 1. Valida DNI (8 dígitos numéricos)
     * 2. Valida que el nombre no esté vacío
     * 3. Verifica que el DNI no esté ya registrado
     * 4. Detecta el rostro en la imagen
     * 5. Verifica que haya exactamente un rostro
     * 6. Extrae características faciales (landmarks)
     * 7. Convierte la imagen a ByteArray
     * 8. Guarda en la base de datos
     *
     * viewModelScope.launch: Ejecuta el código en una coroutine dentro del
     * scope del ViewModel. Se cancela automáticamente cuando se destruye el ViewModel.
     *
     * @param dni DNI de la persona (debe ser 8 dígitos)
     * @param nombre Nombre de la persona
     * @param bitmap Imagen facial capturada
     */
    fun registerFace(dni: String, nombre: String, bitmap: Bitmap) {
        viewModelScope.launch {
            // Cambiar estado a Loading para mostrar indicador de progreso
            _uiState.value = RegisterUiState.Loading

            try {
                // Validar DNI: debe tener exactamente 8 dígitos
                if (!isValidDni(dni)) {
                    _uiState.value = RegisterUiState.Error("El DNI debe tener exactamente 8 dígitos")
                    return@launch
                }

                // Validar que el nombre no esté vacío
                if (nombre.isBlank()) {
                    _uiState.value = RegisterUiState.Error("El nombre no puede estar vacío")
                    return@launch
                }

                // Verificar si el DNI ya existe en la base de datos
                if (repository.isDniRegistered(dni)) {
                    _uiState.value = RegisterUiState.Error("El DNI $dni ya está registrado")
                    return@launch
                }

                // Detectar rostros en la imagen usando ML Kit
                val faces = faceDetectionService.detectFaces(bitmap)

                // Verificar que se detectó al menos un rostro
                if (faces.isEmpty()) {
                    _uiState.value = RegisterUiState.Error("No se detectó ningún rostro en la imagen")
                    return@launch
                }

                // Verificar que solo haya un rostro (evitar confusión)
                if (faces.size > 1) {
                    _uiState.value = RegisterUiState.Error("Se detectaron múltiples rostros. Asegúrate de capturar solo un rostro")
                    return@launch
                }

                // Extraer características faciales (landmarks) del rostro detectado
                // Estas características se usarán posteriormente para reconocimiento
                val faceFeatures = faceDetectionService.extractFaceFeatures(faces[0])

                // Convertir bitmap a ByteArray para almacenar en la BD
                val imageBytes = bitmapToByteArray(bitmap)

                // Crear la entidad con todos los datos
                val faceEntity = FaceEntity(
                    dni = dni,
                    nombre = nombre,
                    imagenBytes = imageBytes,
                    faceFeatures = faceFeatures
                )

                // Intentar insertar en la base de datos
                val result = repository.insertFace(faceEntity)

                // Manejar el resultado usando fold (success o failure)
                result.fold(
                    onSuccess = {
                        _uiState.value = RegisterUiState.Success("Rostro registrado exitosamente")
                    },
                    onFailure = { error ->
                        _uiState.value = RegisterUiState.Error(error.message ?: "Error al registrar")
                    }
                )

            } catch (e: Exception) {
                // Capturar cualquier excepción inesperada
                _uiState.value = RegisterUiState.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Valida que el DNI tenga exactamente 8 dígitos numéricos
     *
     * Regex: ^\\d{8}$
     * ^ = inicio de string
     * \\d{8} = exactamente 8 dígitos
     * $ = fin de string
     *
     * @param dni El DNI a validar
     * @return true si es válido, false si no
     */
    private fun isValidDni(dni: String): Boolean {
        return dni.matches(Regex("^\\d{8}$"))
    }

    /**
     * Convierte un Bitmap a ByteArray para almacenar en la base de datos
     *
     * Comprime la imagen en formato JPEG con 80% de calidad para
     * reducir el tamaño del archivo sin perder mucha calidad visual.
     *
     * @param bitmap La imagen a convertir
     * @return ByteArray con la imagen comprimida
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    /**
     * Reinicia el estado del ViewModel
     * Llamado cuando el usuario quiere hacer un nuevo registro
     * o después de un registro exitoso
     */
    fun resetState() {
        _uiState.value = RegisterUiState.Initial
        _capturedBitmap.value = null
    }

    /**
     * onCleared se llama cuando el ViewModel está a punto de ser destruido
     * Es importante liberar recursos aquí para evitar memory leaks
     *
     * Libera los recursos del servicio de detección facial de ML Kit
     */
    override fun onCleared() {
        super.onCleared()
        faceDetectionService.release()
    }
}

/**
 * RegisterUiState - Clase sellada que representa todos los posibles estados de la UI
 *
 * Sealed class permite definir un conjunto cerrado de estados.
 * Esto hace que el manejo de estados en la UI sea exhaustivo y type-safe.
 *
 * Estados posibles:
 * - Initial: Estado inicial, sin operación en curso
 * - Loading: Procesando el registro (mostrar indicador de progreso)
 * - Success: Registro exitoso (mostrar mensaje de éxito)
 * - Error: Error en el proceso (mostrar mensaje de error)
 */
sealed class RegisterUiState {
    object Initial : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val message: String) : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}
