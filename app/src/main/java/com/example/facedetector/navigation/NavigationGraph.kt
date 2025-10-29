package com.example.facedetector.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.facedetector.ui.screens.DetailScreen
import com.example.facedetector.ui.screens.HomeScreen
import com.example.facedetector.ui.screens.ListScreen
import com.example.facedetector.ui.screens.RecognitionScreen
import com.example.facedetector.ui.screens.RegisterScreen

/**
 * NavigationGraph - Componente que define el grafo de navegación de la aplicación
 *
 * Este es el componente principal de navegación usando Jetpack Compose Navigation.
 * Define todas las pantallas de la app y cómo navegar entre ellas.
 *
 * El grafo de navegación funciona como un mapa que indica:
 * - Qué pantallas existen
 * - Cuál es la pantalla inicial
 * - Cómo moverse de una pantalla a otra
 *
 * @Composable: Marca esta función como un componente de UI de Compose
 */
@Composable
fun NavigationGraph() {
    /**
     * rememberNavController() crea y recuerda el controlador de navegación
     * Este controlador maneja el stack de navegación (pila de pantallas)
     * y proporciona métodos para navegar (navigate, popBackStack, etc.)
     */
    val navController = rememberNavController()

    /**
     * NavHost es el contenedor del grafo de navegación
     * Define el punto de inicio y todas las rutas disponibles
     *
     * startDestination: La primera pantalla que se muestra al abrir la app
     */
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        /**
         * Composable para la pantalla Home (principal)
         *
         * Los parámetros on* son callbacks (funciones) que se pasan a HomeScreen
         * para que pueda notificar cuando el usuario quiere navegar a otra pantalla.
         *
         * navController.navigate(ruta) agrega una nueva pantalla al stack
         */
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToRecognition = {
                    navController.navigate(Screen.Recognition.route)
                },
                onNavigateToList = {
                    navController.navigate(Screen.List.route)
                }
            )
        }

        /**
         * Composable para la pantalla Register (registro facial)
         *
         * onNavigateBack usa popBackStack() que remueve la pantalla actual
         * del stack y vuelve a la anterior (en este caso, Home)
         */
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        /**
         * Composable para la pantalla Recognition (reconocimiento facial)
         *
         * Similar a Register, permite volver atrás
         */
        composable(Screen.Recognition.route) {
            RecognitionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        /**
         * Composable para la pantalla List (lista de registrados)
         *
         * Además de volver atrás, puede navegar a Detail pasando un DNI
         * onNavigateToDetail recibe el DNI y crea la ruta dinámica
         */
        composable(Screen.List.route) {
            ListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { dni ->
                    // Navega a detail/{dni} con el DNI específico
                    navController.navigate(Screen.Detail.createRoute(dni))
                }
            )
        }

        /**
         * Composable para la pantalla Detail (detalle de persona)
         *
         * Esta ruta tiene un parámetro dinámico {dni} en la URL
         * backStackEntry.arguments contiene los parámetros pasados en la ruta
         * Extrae el DNI de los argumentos y lo pasa a DetailScreen
         *
         * Ejemplo: Si navegamos a "detail/12345678", dni será "12345678"
         */
        composable(Screen.Detail.route) { backStackEntry ->
            // Extrae el parámetro "dni" de la URL, usa "" si no existe
            val dni = backStackEntry.arguments?.getString("dni") ?: ""
            DetailScreen(
                dni = dni,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
