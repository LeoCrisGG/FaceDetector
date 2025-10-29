package com.example.facedetector.data

import kotlinx.coroutines.flow.Flow

/**
 * FaceRepository - Repositorio que gestiona el acceso a los datos
 *
 * Esta clase actúa como intermediario entre los ViewModels y la base de datos.
 * Implementa el patrón Repository para abstraer la fuente de datos y proporcionar
 * una API limpia para las operaciones de datos. Aquí se implementa la lógica de
 * negocio relacionada con los datos (ej: validaciones antes de insertar).
 *
 * Ventajas del patrón Repository:
 * - Separa la lógica de acceso a datos de la lógica de negocio
 * - Facilita el testing (se puede mockear fácilmente)
 * - Si cambias de base de datos, solo modificas el Repository
 *
 * @param faceDao El DAO que realiza las operaciones reales en la BD
 */
class FaceRepository(private val faceDao: FaceDao) {

    /**
     * Obtiene todos los registros faciales como un Flow reactivo
     *
     * Este método simplemente delega al DAO. El Flow permite que la UI
     * observe cambios en tiempo real: cuando se agrega/elimina un registro,
     * la UI se actualiza automáticamente.
     *
     * @return Flow que emite la lista actualizada cada vez que cambia la BD
     */
    fun getAllFaces(): Flow<List<FaceEntity>> = faceDao.getAllFaces()

    /**
     * Obtiene todos los registros como una lista estática
     *
     * Útil para operaciones que no necesitan observar cambios continuos,
     * como en el proceso de reconocimiento facial donde solo necesitamos
     * la lista una vez para comparar.
     *
     * @return Lista con todos los registros en el momento de la llamada
     */
    suspend fun getAllFacesList(): List<FaceEntity> = faceDao.getAllFacesList()

    /**
     * Inserta un nuevo registro facial con validación de DNI duplicado
     *
     * Este método implementa la lógica de negocio antes de insertar:
     * 1. Verifica que el DNI no esté ya registrado
     * 2. Si no existe, inserta el registro
     * 3. Retorna Result<Long> para manejar éxito o error de forma segura
     *
     * Result<T> es una clase de Kotlin que encapsula un valor exitoso o un error,
     * permitiendo manejar errores sin excepciones.
     *
     * @param face La entidad a insertar
     * @return Result.success(id) si se insertó correctamente
     *         Result.failure(Exception) si el DNI ya existe o hay otro error
     */
    suspend fun insertFace(face: FaceEntity): Result<Long> {
        return try {
            // Verificar que el DNI no exista (validación de negocio)
            if (faceDao.existsDni(face.dni) > 0) {
                Result.failure(Exception("El DNI ${face.dni} ya está registrado"))
            } else {
                // Insertar y retornar el ID generado
                val id = faceDao.insertFace(face)
                Result.success(id)
            }
        } catch (e: Exception) {
            // Capturar cualquier error de BD y retornarlo como Result.failure
            Result.failure(e)
        }
    }

    /**
     * Busca un registro por DNI
     *
     * @param dni El DNI a buscar
     * @return La entidad si existe, null si no se encuentra
     */
    suspend fun getFaceByDni(dni: String): FaceEntity? = faceDao.getFaceByDni(dni)

    /**
     * Elimina un registro por su DNI
     *
     * @param dni El DNI del registro a eliminar
     */
    suspend fun deleteFaceByDni(dni: String) = faceDao.deleteFaceByDni(dni)

    /**
     * Verifica si un DNI está registrado
     *
     * Útil para validaciones antes de intentar registrar un nuevo usuario
     *
     * @param dni El DNI a verificar
     * @return true si el DNI existe, false si no está registrado
     */
    suspend fun isDniRegistered(dni: String): Boolean = faceDao.existsDni(dni) > 0
}
