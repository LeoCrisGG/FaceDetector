package com.example.facedetector.navigation

/**
 * Screen - Clase sellada que define todas las rutas de navegación de la app
 *
 * Una clase sellada (sealed class) es perfecta para representar un conjunto
 * cerrado de opciones. Aquí define todas las pantallas posibles de la aplicación.
 *
 * Cada objeto dentro de Screen representa una pantalla y su ruta de navegación.
 * Las rutas son strings que identifican de manera única cada destino.
 *
 * @param route String que identifica la ruta de navegación
 */
sealed class Screen(val route: String) {
    /**
     * Home - Pantalla principal con opciones para:
     * - Registrar un nuevo rostro
     * - Reconocer un rostro
     * - Ver lista de registrados
     */
    object Home : Screen("home")

    /**
     * Register - Pantalla para capturar imagen facial y registrar
     * datos de una nueva persona (DNI y nombre)
     */
    object Register : Screen("register")

    /**
     * Recognition - Pantalla para capturar imagen y reconocer
     * a qué persona registrada pertenece el rostro
     */
    object Recognition : Screen("recognition")

    /**
     * List - Pantalla que muestra la lista completa de todas
     * las personas registradas en la base de datos
     */
    object List : Screen("list")

    /**
     * Detail - Pantalla de detalle de una persona específica
     *
     * Esta ruta usa un parámetro dinámico {dni} que se pasa en la URL.
     * Por ejemplo: "detail/12345678" muestra el detalle del DNI 12345678
     *
     * createRoute() es una función helper para construir la ruta completa
     * con el DNI específico de forma segura.
     */
    object Detail : Screen("detail/{dni}") {
        /**
         * Crea una ruta completa con el DNI especificado
         *
         * @param dni El DNI de la persona a mostrar
         * @return String con la ruta completa, ej: "detail/12345678"
         */
        fun createRoute(dni: String) = "detail/$dni"
    }
}
