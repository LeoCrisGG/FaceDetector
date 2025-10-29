package com.example.facedetector.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * FaceDetectionService - Servicio que gestiona toda la detección y comparación facial
 *
 * Esta clase encapsula toda la lógica de Machine Learning usando Google ML Kit.
 * Es responsable de:
 * - Detectar rostros en imágenes
 * - Extraer características faciales (landmarks, ángulos, probabilidades)
 * - Comparar dos rostros y calcular su similitud
 *
 * ML Kit Face Detection detecta rostros y extrae puntos clave (ojos, nariz, boca, etc.)
 * pero NO hace reconocimiento facial directamente. Nosotros implementamos el reconocimiento
 * comparando los landmarks extraídos.
 */
class FaceDetectionService {

    /**
     * Opciones de configuración del detector facial de ML Kit
     *
     * FaceDetectorOptions configura cómo debe funcionar el detector:
     * - PERFORMANCE_MODE_ACCURATE: Modo preciso (más lento pero más exacto)
     * - LANDMARK_MODE_ALL: Detectar todos los landmarks (ojos, nariz, boca, etc.)
     * - CLASSIFICATION_MODE_ALL: Clasificar sonrisa y ojos abiertos
     * - MinFaceSize: Tamaño mínimo del rostro a detectar (15% de la imagen)
     */
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .build()

    /**
     * Cliente del detector facial de ML Kit
     * Se crea una sola vez y se reutiliza para todas las detecciones
     */
    private val detector = FaceDetection.getClient(options)

    /**
     * Detecta rostros en una imagen
     *
     * Proceso:
     * 1. Convierte el Bitmap a InputImage (formato que entiende ML Kit)
     * 2. Procesa la imagen con el detector
     * 3. Retorna lista de rostros detectados (cada Face tiene landmarks y datos)
     *
     * suspend: Esta función es asíncrona (usa coroutines)
     * .await(): Convierte la Task de ML Kit en una coroutine suspendible
     *
     * @param bitmap La imagen a analizar
     * @return Lista de rostros detectados (vacía si no encuentra ninguno)
     */
    suspend fun detectFaces(bitmap: Bitmap): List<Face> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            detector.process(image).await()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Extrae características faciales de un rostro detectado y las serializa a JSON
     *
     * Características extraídas:
     * 1. Bounding Box: Rectángulo que encierra el rostro
     * 2. Ángulos de Euler: Orientación de la cabeza (X, Y, Z)
     * 3. Landmarks: Puntos clave del rostro (ojos, nariz, boca, mejillas, orejas)
     * 4. Probabilidades: Sonrisa, ojos abiertos
     *
     * Estos datos se guardan como JSON string en la base de datos para
     * poder compararlos posteriormente con otros rostros.
     *
     * @param face El rostro detectado por ML Kit
     * @return String JSON con todas las características faciales
     */
    fun extractFaceFeatures(face: Face): String {
        val features = JSONObject()

        /**
         * Bounding Box: Coordenadas del rectángulo que contiene el rostro
         * Útil para saber la posición y tamaño del rostro en la imagen
         */
        features.put("boundingBox", JSONObject().apply {
            put("left", face.boundingBox.left)
            put("top", face.boundingBox.top)
            put("right", face.boundingBox.right)
            put("bottom", face.boundingBox.bottom)
        })

        /**
         * Ángulos de Euler: Rotación de la cabeza en 3 ejes
         * - X (pitch): Cabeza arriba/abajo
         * - Y (yaw): Cabeza izquierda/derecha
         * - Z (roll): Cabeza inclinada
         */
        features.put("headEulerAngleX", face.headEulerAngleX)
        features.put("headEulerAngleY", face.headEulerAngleY)
        features.put("headEulerAngleZ", face.headEulerAngleZ)

        /**
         * Landmarks: Puntos clave del rostro
         *
         * Cada landmark tiene:
         * - type: Tipo de punto (ojo izquierdo, nariz, etc.)
         * - x, y: Coordenadas del punto en la imagen
         *
         * Estos son los puntos más importantes para comparar rostros.
         * La posición relativa de estos puntos es única para cada persona.
         */
        val landmarksArray = JSONArray()
        val landmarkTypes = listOf(
            FaceLandmark.LEFT_EYE,      // Ojo izquierdo
            FaceLandmark.RIGHT_EYE,     // Ojo derecho
            FaceLandmark.NOSE_BASE,     // Base de la nariz
            FaceLandmark.LEFT_CHEEK,    // Mejilla izquierda
            FaceLandmark.RIGHT_CHEEK,   // Mejilla derecha
            FaceLandmark.MOUTH_LEFT,    // Lado izquierdo de la boca
            FaceLandmark.MOUTH_RIGHT,   // Lado derecho de la boca
            FaceLandmark.MOUTH_BOTTOM,  // Parte inferior de la boca
            FaceLandmark.LEFT_EAR,      // Oreja izquierda
            FaceLandmark.RIGHT_EAR      // Oreja derecha
        )

        // Extraer cada landmark si está disponible
        landmarkTypes.forEach { type ->
            face.getLandmark(type)?.let { landmark ->
                landmarksArray.put(JSONObject().apply {
                    put("type", type)
                    put("x", landmark.position.x)
                    put("y", landmark.position.y)
                })
            }
        }
        features.put("landmarks", landmarksArray)

        /**
         * Probabilidades de clasificación
         * ML Kit puede clasificar:
         * - Probabilidad de sonrisa (0.0 = no sonríe, 1.0 = sonríe mucho)
         * - Probabilidad de ojo izquierdo abierto
         * - Probabilidad de ojo derecho abierto
         */
        face.smilingProbability?.let { features.put("smilingProbability", it) }
        face.leftEyeOpenProbability?.let { features.put("leftEyeOpenProbability", it) }
        face.rightEyeOpenProbability?.let { features.put("rightEyeOpenProbability", it) }

        return features.toString()
    }

    /**
     * Compara dos conjuntos de características faciales y calcula su similitud
     *
     * Algoritmo de comparación:
     * 1. Parsea los JSON de características de ambos rostros
     * 2. Extrae los landmarks de cada uno
     * 3. Para cada landmark del rostro 1, busca el mismo tipo en rostro 2
     * 4. Calcula la distancia euclidiana entre las posiciones de cada par de landmarks
     * 5. Promedia todas las distancias
     * 6. Convierte la distancia a un porcentaje de similitud (0-100%)
     *
     * Distancia Euclidiana: sqrt((x1-x2)² + (y1-y2)²)
     * Es la distancia en línea recta entre dos puntos.
     *
     * Normalización: Cuanto menor es la distancia, mayor es la similitud.
     * La fórmula 1/(1+(dist/100)) * 100 convierte distancia a porcentaje.
     *
     * @param features1 JSON string con características del primer rostro
     * @param features2 JSON string con características del segundo rostro
     * @return Porcentaje de similitud (0-100), donde 100 = idénticos
     */
    fun compareFaces(features1: String, features2: String): Float {
        return try {
            // Parsear los JSON
            val json1 = JSONObject(features1)
            val json2 = JSONObject(features2)

            // Extraer arrays de landmarks
            val landmarks1 = json1.getJSONArray("landmarks")
            val landmarks2 = json2.getJSONArray("landmarks")

            // Verificar que ambos tengan landmarks
            if (landmarks1.length() == 0 || landmarks2.length() == 0) {
                return 0f
            }

            // Variables para acumular distancias
            var totalDistance = 0.0
            var countMatches = 0

            /**
             * Comparar cada landmark del rostro 1 con el correspondiente del rostro 2
             *
             * Para cada landmark en face1:
             * 1. Obtener su tipo (ej: ojo izquierdo)
             * 2. Buscar el mismo tipo en face2
             * 3. Calcular distancia euclidiana entre sus posiciones
             * 4. Acumular la distancia
             */
            for (i in 0 until landmarks1.length()) {
                val lm1 = landmarks1.getJSONObject(i)
                val type1 = lm1.getInt("type")

                // Buscar el mismo tipo de landmark en features2
                for (j in 0 until landmarks2.length()) {
                    val lm2 = landmarks2.getJSONObject(j)
                    if (lm2.getInt("type") == type1) {
                        // Extraer coordenadas
                        val x1 = lm1.getDouble("x")
                        val y1 = lm1.getDouble("y")
                        val x2 = lm2.getDouble("x")
                        val y2 = lm2.getDouble("y")

                        // Calcular distancia euclidiana
                        val distance = sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
                        totalDistance += distance
                        countMatches++
                        break
                    }
                }
            }

            // Si no hubo coincidencias, similitud = 0
            if (countMatches == 0) return 0f

            /**
             * Normalizar la distancia a porcentaje de similitud (0-100)
             *
             * avgDistance: Distancia promedio entre landmarks
             * similarity: Fórmula que convierte distancia a similitud
             * - Distancia pequeña → similitud alta (cerca de 100%)
             * - Distancia grande → similitud baja (cerca de 0%)
             */
            val avgDistance = totalDistance / countMatches
            val similarity = (1.0 / (1.0 + (avgDistance / 100.0))) * 100.0

            return similarity.toFloat()
        } catch (e: Exception) {
            // Si hay error parseando JSON o calculando, retornar 0
            0f
        }
    }

    /**
     * Libera los recursos del detector
     *
     * Importante llamar cuando ya no se necesita el servicio
     * para evitar memory leaks. Los ViewModels llaman a esto
     * en su método onCleared().
     */
    fun release() {
        detector.close()
    }
}
