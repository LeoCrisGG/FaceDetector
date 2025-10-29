package com.example.facedetector.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * FaceEntity - Entidad de Room que representa un registro facial en la base de datos SQLite
 *
 * Esta clase define la estructura de la tabla "faces" en la base de datos.
 * Cada registro almacena la información de una persona junto con su imagen facial y características.
 *
 * @Entity: Anotación de Room que marca esta clase como una tabla de base de datos
 * tableName: Nombre de la tabla en la base de datos
 * indices: Define índices para optimizar búsquedas. El DNI tiene un índice único para evitar duplicados
 */
@Entity(
    tableName = "faces",
    indices = [Index(value = ["dni"], unique = true)]
)
data class FaceEntity(
    /**
     * ID único autogenerado para cada registro
     * @PrimaryKey con autoGenerate = true genera automáticamente IDs incrementales
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * DNI de la persona (Documento Nacional de Identidad)
     * Debe tener exactamente 8 dígitos según las validaciones de la app
     * Tiene un índice único que previene registros duplicados
     */
    val dni: String,

    /**
     * Nombre completo de la persona registrada
     */
    val nombre: String,

    /**
     * Imagen facial almacenada como array de bytes (ByteArray)
     * La imagen se convierte de Bitmap a JPEG comprimido para ahorrar espacio
     */
    val imagenBytes: ByteArray,

    /**
     * Características faciales extraídas del rostro, almacenadas como JSON
     * Contiene los landmarks (puntos clave) del rostro para comparación posterior
     * Usado en el reconocimiento facial para comparar rostros
     */
    val faceFeatures: String,

    /**
     * Timestamp de cuándo se creó o actualizó el registro
     * Se genera automáticamente con la hora actual del sistema
     */
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Override de equals para comparar correctamente ByteArrays
     * Necesario porque ByteArray.equals() no compara contenido, solo referencias
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceEntity

        if (id != other.id) return false
        if (dni != other.dni) return false
        if (nombre != other.nombre) return false
        if (!imagenBytes.contentEquals(other.imagenBytes)) return false
        if (faceFeatures != other.faceFeatures) return false

        return true
    }

    /**
     * Override de hashCode consistente con equals
     * Necesario para usar FaceEntity en colecciones como HashSet o HashMap
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + dni.hashCode()
        result = 31 * result + nombre.hashCode()
        result = 31 * result + imagenBytes.contentHashCode()
        result = 31 * result + faceFeatures.hashCode()
        return result
    }
}
