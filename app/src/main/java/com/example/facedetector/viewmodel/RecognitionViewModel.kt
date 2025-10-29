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

/**
 * RecognitionViewModel - ViewModel para la pantalla de reconocimiento facial
 *
 * Este ViewModel gestiona la lógica de negocio para reconocer un rostro capturado
 * comparándolo con todos los rostros registrados en la base de datos.
 *
 * Responsabilidades:
 * - Capturar imagen del rostro a reconocer
 * - Detectar el rostro en la imagen
 * - Extraer características faciales
 * - Comparar con todos los rostros registrados
 * - Encontrar la mejor coincidencia usando un algoritmo de similitud
 * - Determinar si la similitud supera el umbral mínimo
 * - Gestionar estados de UI (loading, found, not found, error)
 */
class RecognitionViewModel(application: Application) : AndroidViewModel(application) {

    // Repository para acceder a la base de datos
    private val repository: FaceRepository

    // Servicio de detección y comparación facial usando ML Kit
    private val faceDetectionService = FaceDetectionService()

    /**
     * Estado de la UI observable por la pantalla
     * Puede ser: Initial, Loading, Found, NotFound, o Error
     */
    private val _uiState = MutableStateFlow<RecognitionUiState>(RecognitionUiState.Initial)
    val uiState: StateFlow<RecognitionUiState> = _uiState.asStateFlow()

    /**
     * Almacena la imagen capturada para reconocimiento
     * Se muestra en preview en la UI
     */
    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    /**
     * Umbral de similitud mínimo para considerar una coincidencia válida
     *
     * 65% significa que las características faciales deben coincidir al menos
     * en un 65% para considerar que es la misma persona. Este valor es ajustable:
     * - Valores más altos (ej: 80%): Más estricto, menos falsos positivos
     * - Valores más bajos (ej: 50%): Más permisivo, más falsos positivos
     */
    private val SIMILARITY_THRESHOLD = 65.0f

    /**
     * Inicializa el repository con el DAO de la base de datos
     */
    init {
        val faceDao = FaceDatabase.getDatabase(application).faceDao()
        repository = FaceRepository(faceDao)
    }

    /**
     * Guarda la imagen capturada en el estado
     * Llamado cuando el usuario captura una foto para reconocer
     *
     * @param bitmap La imagen capturada
     */
    fun setCapturedImage(bitmap: Bitmap) {
        _capturedBitmap.value = bitmap
    }

    /**
     * Reconoce el rostro en la imagen capturada
     *
     * Proceso de reconocimiento:
     * 1. Detecta el rostro en la imagen capturada
     * 2. Verifica que haya exactamente un rostro
     * 3. Extrae características faciales del rostro
     * 4. Obtiene todos los rostros registrados de la BD
     * 5. Compara el rostro capturado con cada rostro registrado
     * 6. Encuentra el rostro con mayor similitud
     * 7. Si la similitud supera el umbral, es una coincidencia válida
     * 8. Si no, el rostro no está registrado
     *
     * @param bitmap La imagen facial a reconocer
     */
    fun recognizeFace(bitmap: Bitmap) {
        viewModelScope.launch {
            // Cambiar estado a Loading para mostrar indicador de progreso
            _uiState.value = RecognitionUiState.Loading

            try {
                // Detectar rostro en la imagen capturada usando ML Kit
                val faces = faceDetectionService.detectFaces(bitmap)

                // Verificar que se detectó al menos un rostro
                if (faces.isEmpty()) {
                    _uiState.value = RecognitionUiState.NotFound("No se detectó ningún rostro en la imagen")
                    return@launch
                }

                // Verificar que solo haya un rostro
                if (faces.size > 1) {
                    _uiState.value = RecognitionUiState.NotFound("Se detectaron múltiples rostros. Intenta capturar solo un rostro")
                    return@launch
                }

                // Extraer características del rostro capturado
                // Estas características son landmarks (puntos clave) del rostro
                val capturedFeatures = faceDetectionService.extractFaceFeatures(faces[0])

                // Obtener todos los rostros registrados en la base de datos
                val registeredFaces = repository.getAllFacesList()

                // Verificar que haya rostros registrados para comparar
                if (registeredFaces.isEmpty()) {
                    _uiState.value = RecognitionUiState.NotFound("No hay rostros registrados en la base de datos")
                    return@launch
                }

                // Variables para almacenar la mejor coincidencia encontrada
                var bestMatch: FaceEntity? = null
                var bestSimilarity = 0f

                /**
                 * Comparar el rostro capturado con cada rostro registrado
                 *
                 * Para cada rostro en la BD:
                 * 1. Compara las características faciales
                 * 2. Calcula un porcentaje de similitud
                 * 3. Si es mayor que la mejor similitud hasta ahora, actualiza
                 */
                registeredFaces.forEach { registeredFace ->
                    // Comparar características usando el algoritmo de similitud
                    val similarity = faceDetectionService.compareFaces(
                        capturedFeatures,
                        registeredFace.faceFeatures
                    )

                    // Si esta similitud es la más alta, actualizar la mejor coincidencia
                    if (similarity > bestSimilarity) {
                        bestSimilarity = similarity
                        bestMatch = registeredFace
                    }
                }

                /**
                 * Verificar si la mejor coincidencia supera el umbral mínimo
                 *
                 * Si bestSimilarity >= SIMILARITY_THRESHOLD (65%):
                 * - Es una coincidencia válida, se reconoció a la persona
                 *
                 * Si bestSimilarity < SIMILARITY_THRESHOLD:
                 * - No hay coincidencia suficiente, rostro no reconocido
                 */
                if (bestMatch != null && bestSimilarity >= SIMILARITY_THRESHOLD) {
                    _uiState.value = RecognitionUiState.Found(
                        face = bestMatch!!,
                        similarity = bestSimilarity
                    )
                } else {
                    _uiState.value = RecognitionUiState.NotFound(
                        "No se encontró una coincidencia (Similitud máxima: ${String.format("%.1f", bestSimilarity)}%)"
                    )
                }

            } catch (e: Exception) {
                // Capturar cualquier error inesperado
                _uiState.value = RecognitionUiState.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Reinicia el estado del ViewModel
     * Llamado cuando el usuario quiere hacer un nuevo reconocimiento
     */
    fun resetState() {
        _uiState.value = RecognitionUiState.Initial
        _capturedBitmap.value = null
    }

    /**
     * Libera recursos cuando el ViewModel se destruye
     * Importante para evitar memory leaks
     */
    override fun onCleared() {
        super.onCleared()
        faceDetectionService.release()
    }
}

/**
 * RecognitionUiState - Estados posibles de la UI de reconocimiento
 *
 * Estados:
 * - Initial: Estado inicial, sin operación en curso
 * - Loading: Procesando el reconocimiento
 * - Found: Rostro reconocido exitosamente (incluye la persona y % similitud)
 * - NotFound: Rostro no reconocido o similitud insuficiente
 * - Error: Error en el proceso
 */
sealed class RecognitionUiState {
    object Initial : RecognitionUiState()
    object Loading : RecognitionUiState()
    data class Found(val face: FaceEntity, val similarity: Float) : RecognitionUiState()
    data class NotFound(val message: String) : RecognitionUiState()
    data class Error(val message: String) : RecognitionUiState()
}
