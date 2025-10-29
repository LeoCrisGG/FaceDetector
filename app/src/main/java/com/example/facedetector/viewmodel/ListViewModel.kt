package com.example.facedetector.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.facedetector.data.FaceDatabase
import com.example.facedetector.data.FaceEntity
import com.example.facedetector.data.FaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ListViewModel - ViewModel para la pantalla de lista de personas registradas
 *
 * Este ViewModel gestiona la lógica para mostrar la lista completa de todas
 * las personas registradas en la base de datos.
 *
 * Es un ViewModel simple que solo se encarga de:
 * - Cargar la lista de personas registradas
 * - Observar cambios en tiempo real (cuando se agrega/elimina alguien)
 * - Exponer la lista a la UI mediante StateFlow
 *
 * La UI se actualiza automáticamente cuando cambia la base de datos
 * gracias al Flow reactivo.
 */
class ListViewModel(application: Application) : AndroidViewModel(application) {

    // Repository para acceder a la base de datos
    private val repository: FaceRepository

    /**
     * Lista de personas registradas observable por la UI
     *
     * _faces es privado (solo el ViewModel puede modificar)
     * faces es público (la UI puede observar pero no modificar)
     *
     * Inicia con una lista vacía y se actualiza automáticamente
     * cuando hay cambios en la base de datos.
     */
    private val _faces = MutableStateFlow<List<FaceEntity>>(emptyList())
    val faces: StateFlow<List<FaceEntity>> = _faces.asStateFlow()

    /**
     * Inicialización del ViewModel
     *
     * 1. Obtiene el DAO de la base de datos
     * 2. Crea el repository
     * 3. Carga la lista de personas
     */
    init {
        val faceDao = FaceDatabase.getDatabase(application).faceDao()
        repository = FaceRepository(faceDao)
        loadFaces()
    }

    /**
     * Carga la lista de personas registradas
     *
     * Usa repository.getAllFaces() que retorna un Flow.
     * El Flow emite automáticamente una nueva lista cada vez que
     * cambia la base de datos (insert, delete, update).
     *
     * .collect { } observa continuamente el Flow y actualiza _faces
     * cada vez que hay un nuevo valor.
     *
     * Esta coroutine se ejecuta mientras el ViewModel esté vivo.
     * Cuando el ViewModel se destruye (onCleared), la coroutine se cancela
     * automáticamente porque usa viewModelScope.
     */
    private fun loadFaces() {
        viewModelScope.launch {
            // Observar cambios en la base de datos
            repository.getAllFaces().collect { faceList ->
                // Actualizar el estado con la nueva lista
                _faces.value = faceList
            }
        }
    }
}
