package com.example.facedetector.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
 * DetailViewModel - ViewModel para la pantalla de detalle de una persona
 *
 * Este ViewModel gestiona la lógica de negocio para:
 * - Mostrar los detalles de una persona específica (DNI, nombre, foto)
 * - Actualizar la foto facial (con validación de similitud)
 * - Eliminar el registro de la persona
 *
 * La actualización de foto tiene una validación importante:
 * La nueva foto debe ser similar (al menos 70%) a la foto actual para
 * asegurar que se está actualizando la foto de la misma persona.
 *
 * Este ViewModel recibe el DNI como parámetro en el constructor,
 * por lo que necesita un Factory personalizado para crearse.
 *
 * @param application Contexto de la aplicación
 * @param dni DNI de la persona a mostrar
 */
class DetailViewModel(
    application: Application,
    private val dni: String
) : AndroidViewModel(application) {

    // Repository para acceder a la base de datos
    private val repository: FaceRepository

    // Servicio de detección facial para validar similitud en actualización de foto
    private val faceDetectionService = FaceDetectionService()

    /**
     * Estado de la UI observable
     * Puede ser: Initial, Loading, Success, Error, Deleted
     */
    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Initial)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    /**
     * Datos de la persona actual (del DNI especificado)
     * null si no se encuentra
     */
    private val _currentFace = MutableStateFlow<FaceEntity?>(null)
    val currentFace: StateFlow<FaceEntity?> = _currentFace.asStateFlow()

    /**
     * Nueva foto capturada para actualizar
     * null si no hay foto pendiente de actualización
     */
    private val _newCapturedBitmap = MutableStateFlow<Bitmap?>(null)
    val newCapturedBitmap: StateFlow<Bitmap?> = _newCapturedBitmap.asStateFlow()

    /**
     * Umbral de similitud para actualizar foto
     *
     * 70% significa que la nueva foto debe parecerse al menos en un 70%
     * a la foto actual. Esto previene que se actualice la foto con una
     * imagen de otra persona.
     */
    private val SIMILARITY_THRESHOLD = 70.0f

    /**
     * Inicialización del ViewModel
     * Carga los datos de la persona con el DNI especificado
     */
    init {
        val faceDao = FaceDatabase.getDatabase(application).faceDao()
        repository = FaceRepository(faceDao)
        loadFace()
    }

    /**
     * Carga los datos de la persona desde la base de datos
     * usando el DNI proporcionado en el constructor
     */
    private fun loadFace() {
        viewModelScope.launch {
            val face = repository.getFaceByDni(dni)
            _currentFace.value = face
        }
    }

    /**
     * Guarda la nueva imagen capturada para actualización
     *
     * @param bitmap La nueva foto capturada
     */
    fun setCapturedImage(bitmap: Bitmap) {
        _newCapturedBitmap.value = bitmap
        _uiState.value = DetailUiState.Initial
    }

    /**
     * Cancela la actualización de foto pendiente
     * Descarta la nueva imagen capturada
     */
    fun cancelPhotoUpdate() {
        _newCapturedBitmap.value = null
        _uiState.value = DetailUiState.Initial
    }

    /**
     * Actualiza la foto de la persona con validación de similitud
     *
     * Proceso de actualización:
     * 1. Verifica que existan los datos necesarios (foto actual y nueva foto)
     * 2. Detecta el rostro en la nueva imagen
     * 3. Verifica que haya exactamente un rostro
     * 4. Extrae características faciales de la nueva foto
     * 5. Compara con las características de la foto actual
     * 6. Si la similitud >= 70%, permite la actualización
     * 7. Si la similitud < 70%, rechaza la actualización (no es la misma persona)
     * 8. Actualiza el registro en la base de datos
     *
     * Este mecanismo evita que alguien cambie la foto por la de otra persona.
     */
    fun updatePhoto() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading

            try {
                val currentFaceEntity = _currentFace.value
                val newBitmap = _newCapturedBitmap.value

                // Validar que existan los datos necesarios
                if (currentFaceEntity == null || newBitmap == null) {
                    _uiState.value = DetailUiState.Error("Error: No se encontró la información necesaria")
                    return@launch
                }

                // Detectar rostro en la nueva imagen
                val faces = faceDetectionService.detectFaces(newBitmap)

                // Verificar que se detectó al menos un rostro
                if (faces.isEmpty()) {
                    _uiState.value = DetailUiState.Error("No se detectó ningún rostro en la nueva imagen")
                    return@launch
                }

                // Verificar que solo haya un rostro
                if (faces.size > 1) {
                    _uiState.value = DetailUiState.Error("Se detectaron múltiples rostros. Captura solo un rostro")
                    return@launch
                }

                // Extraer características de la nueva foto
                val newFeatures = faceDetectionService.extractFaceFeatures(faces[0])

                /**
                 * Comparar con la foto actual
                 *
                 * Esta es la validación clave: comparamos las características faciales
                 * de la foto actual con las de la nueva foto.
                 *
                 * Si son similares (>= 70%), es la misma persona y se permite actualizar.
                 * Si no son similares (< 70%), probablemente sea otra persona y se rechaza.
                 */
                val similarity = faceDetectionService.compareFaces(
                    currentFaceEntity.faceFeatures,
                    newFeatures
                )

                // Verificar si la similitud supera el umbral
                if (similarity < SIMILARITY_THRESHOLD) {
                    _uiState.value = DetailUiState.Error(
                        "La nueva foto no es suficientemente similar a la registrada. " +
                        "Similitud: ${String.format("%.1f", similarity)}% (mínimo requerido: ${SIMILARITY_THRESHOLD.toInt()}%)"
                    )
                    return@launch
                }

                // Convertir el nuevo bitmap a ByteArray para almacenar
                val newImageBytes = bitmapToByteArray(newBitmap)

                /**
                 * Actualizar la entidad con la nueva foto y características
                 *
                 * Se crea una copia de la entidad actual usando copy()
                 * actualizando solo la imagen, características y timestamp.
                 * El DNI y nombre permanecen igual.
                 */
                val updatedFace = currentFaceEntity.copy(
                    imagenBytes = newImageBytes,
                    faceFeatures = newFeatures,
                    timestamp = System.currentTimeMillis()
                )

                /**
                 * Estrategia de actualización:
                 * 1. Eliminar el registro anterior
                 * 2. Insertar el nuevo registro
                 *
                 * Esto es necesario porque Room no tiene un método update directo
                 * y el índice único del DNI podría causar problemas.
                 */
                repository.deleteFaceByDni(dni)
                val result = repository.insertFace(updatedFace)

                // Manejar el resultado de la inserción
                result.fold(
                    onSuccess = {
                        _currentFace.value = updatedFace
                        _newCapturedBitmap.value = null
                        _uiState.value = DetailUiState.Success(
                            "Foto actualizada exitosamente. Similitud: ${String.format("%.1f", similarity)}%"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = DetailUiState.Error(error.message ?: "Error al actualizar")
                    }
                )

            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Elimina el registro de la persona de la base de datos
     *
     * Una vez eliminado, el estado cambia a Deleted para que
     * la UI pueda navegar de vuelta a la pantalla anterior.
     */
    fun deleteFace() {
        viewModelScope.launch {
            try {
                repository.deleteFaceByDni(dni)
                _uiState.value = DetailUiState.Deleted
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error("Error al eliminar: ${e.message}")
            }
        }
    }

    /**
     * Convierte un Bitmap a ByteArray para almacenar en la BD
     * Comprime en JPEG con 80% de calidad
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    /**
     * Libera recursos al destruir el ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        faceDetectionService.release()
    }

    /**
     * Factory - Clase Factory para crear DetailViewModel con parámetros
     *
     * ViewModels normalmente se crean sin parámetros, pero DetailViewModel
     * necesita recibir el DNI. El Factory permite crear ViewModels con
     * parámetros personalizados.
     *
     * El Factory se usa en la UI así:
     * val viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory(dni))
     *
     * @param dni El DNI de la persona a mostrar
     */
    class Factory(private val dni: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
                /**
                 * Obtiene el Application context usando reflection
                 * Esto es necesario porque el Factory no tiene acceso directo
                 * al Application context
                 */
                val application = try {
                    Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication")
                        .invoke(null) as Application
                } catch (e: Exception) {
                    throw IllegalStateException("Cannot get Application context")
                }
                return DetailViewModel(application, dni) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * DetailUiState - Estados posibles de la pantalla de detalle
 *
 * Estados:
 * - Initial: Estado inicial, sin operación en curso
 * - Loading: Procesando una operación (actualizar o eliminar)
 * - Success: Operación exitosa (muestra mensaje)
 * - Error: Error en la operación (muestra mensaje de error)
 * - Deleted: Registro eliminado (la UI debe volver atrás)
 */
sealed class DetailUiState {
    object Initial : DetailUiState()
    object Loading : DetailUiState()
    data class Success(val message: String) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
    object Deleted : DetailUiState()
}
