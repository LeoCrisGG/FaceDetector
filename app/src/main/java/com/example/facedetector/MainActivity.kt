package com.example.facedetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.facedetector.navigation.NavigationGraph
import com.example.facedetector.ui.theme.FacedetectorTheme

/**
 * MainActivity - Actividad principal de la aplicación
 *
 * Esta es la única Activity de la aplicación. Toda la app usa Jetpack Compose
 * para la UI, por lo que no necesitamos múltiples Activities.
 *
 * ComponentActivity: Actividad base compatible con Jetpack Compose
 *
 * La actividad se encarga de:
 * - Configurar el tema de la aplicación
 * - Inicializar el grafo de navegación
 * - Habilitar diseño edge-to-edge (pantalla completa)
 */
class MainActivity : ComponentActivity() {
    /**
     * onCreate se llama cuando se crea la actividad
     * Es el punto de entrada de la aplicación
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * enableEdgeToEdge() permite que la UI se extienda detrás de las barras
         * del sistema (status bar y navigation bar) para un diseño moderno
         */
        enableEdgeToEdge()

        /**
         * setContent { } es la función de Compose que define el contenido de la UI
         * Reemplaza al tradicional setContentView(R.layout.xxx)
         */
        setContent {
            /**
             * FacedetectorTheme aplica el tema personalizado de la app
             * Define colores, tipografías y estilos globales
             */
            FacedetectorTheme {
                /**
                 * Surface es un contenedor Material Design que aplica
                 * el color de fondo del tema
                 */
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    /**
                     * NavigationGraph es el componente raíz que contiene
                     * todas las pantallas y maneja la navegación entre ellas
                     */
                    NavigationGraph()
                }
            }
        }
    }
}
