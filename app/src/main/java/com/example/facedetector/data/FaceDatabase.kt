package com.example.facedetector.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * FaceDatabase - Clase que representa la base de datos Room de la aplicación
 *
 * Esta clase abstracta define la configuración de la base de datos SQLite
 * y proporciona acceso a los DAOs. Room genera automáticamente la implementación.
 *
 * @Database: Anotación que marca esta clase como una base de datos Room
 * entities: Lista de entidades (tablas) que contiene la base de datos
 * version: Número de versión de la BD (incrementar cuando cambie el esquema)
 * exportSchema: false para no exportar el esquema de la BD a un archivo
 */
@Database(
    entities = [FaceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FaceDatabase : RoomDatabase() {

    /**
     * Método abstracto que proporciona acceso al DAO
     * Room genera automáticamente la implementación
     */
    abstract fun faceDao(): FaceDao

    companion object {
        /**
         * @Volatile asegura que los cambios a INSTANCE sean visibles inmediatamente
         * para todos los threads. Previene problemas de concurrencia.
         */
        @Volatile
        private var INSTANCE: FaceDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos (patrón Singleton)
         *
         * Este método implementa el patrón Singleton thread-safe usando
         * double-checked locking para garantizar que solo exista una instancia
         * de la base de datos en toda la aplicación.
         *
         * synchronized(this): Bloquea el objeto para que solo un thread pueda
         * crear la instancia a la vez, evitando crear múltiples instancias.
         *
         * @param context Contexto de la aplicación para crear la BD
         * @return La única instancia de FaceDatabase
         */
        fun getDatabase(context: Context): FaceDatabase {
            // Si INSTANCE ya existe, la retorna (primer check, sin lock)
            return INSTANCE ?: synchronized(this) {
                // Segundo check dentro del bloque sincronizado
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FaceDatabase::class.java,
                    "face_database" // Nombre del archivo de la base de datos
                )
                    // Si hay cambios en el esquema, destruye y recrea la BD
                    // En producción se deberían usar migraciones
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
