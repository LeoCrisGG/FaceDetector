package com.example.facedetector.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * FaceDao - Data Access Object (DAO) para operaciones de base de datos
 *
 * Esta interfaz define todas las operaciones CRUD (Create, Read, Update, Delete)
 * que se pueden realizar sobre la tabla "faces" en la base de datos SQLite.
 * Room genera automáticamente la implementación de estos métodos.
 *
 * @Dao: Anotación que marca esta interfaz como un DAO de Room
 */
@Dao
interface FaceDao {

    /**
     * Inserta un nuevo registro facial en la base de datos
     *
     * @Insert: Anotación que genera el código SQL INSERT
     * OnConflictStrategy.ABORT: Si hay conflicto (DNI duplicado), lanza una excepción
     *
     * @param face La entidad FaceEntity a insertar
     * @return El ID generado para el nuevo registro
     * @throws SQLiteConstraintException si el DNI ya existe (por el índice único)
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFace(face: FaceEntity): Long

    /**
     * Busca un registro facial por DNI
     *
     * @Query: Anotación que permite escribir consultas SQL personalizadas
     * LIMIT 1: Optimización para retornar solo un resultado
     *
     * @param dni El DNI a buscar
     * @return La entidad FaceEntity si existe, null si no se encuentra
     */
    @Query("SELECT * FROM faces WHERE dni = :dni LIMIT 1")
    suspend fun getFaceByDni(dni: String): FaceEntity?

    /**
     * Obtiene todos los registros faciales como un Flow
     *
     * Flow: Stream reactivo que emite automáticamente nuevos valores cuando cambia la BD
     * Útil para observar cambios en tiempo real en la UI (lista que se actualiza sola)
     *
     * @return Flow que emite la lista completa cada vez que hay cambios en la tabla
     */
    @Query("SELECT * FROM faces")
    fun getAllFaces(): Flow<List<FaceEntity>>

    /**
     * Obtiene todos los registros faciales como una lista estática
     *
     * A diferencia de getAllFaces(), este método retorna la lista una sola vez
     * sin observar cambios futuros. Útil para operaciones puntuales.
     *
     * @return Lista con todos los registros actuales
     */
    @Query("SELECT * FROM faces")
    suspend fun getAllFacesList(): List<FaceEntity>

    /**
     * Elimina un registro facial específico
     *
     * @Delete: Genera el SQL DELETE basándose en la primary key del objeto
     *
     * @param face La entidad a eliminar
     */
    @Delete
    suspend fun deleteFace(face: FaceEntity)

    /**
     * Elimina un registro facial por su DNI
     *
     * Útil cuando solo conocemos el DNI y no tenemos el objeto completo
     *
     * @param dni El DNI del registro a eliminar
     */
    @Query("DELETE FROM faces WHERE dni = :dni")
    suspend fun deleteFaceByDni(dni: String)

    /**
     * Verifica si existe un registro con el DNI especificado
     *
     * COUNT(*): Cuenta cuántos registros coinciden con el DNI
     *
     * @param dni El DNI a verificar
     * @return Cantidad de registros con ese DNI (0 = no existe, 1 = existe)
     */
    @Query("SELECT COUNT(*) FROM faces WHERE dni = :dni")
    suspend fun existsDni(dni: String): Int
}
