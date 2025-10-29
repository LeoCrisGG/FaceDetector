# Face Detector - Sistema de Reconocimiento Facial

Aplicación Android para registro y reconocimiento facial usando ML Kit y Jetpack Compose.

## Características Principales

✅ **Registro de Rostros**
- Captura de imagen facial con cámara frontal
- Validación de DNI (8 dígitos)
- Prevención de DNIs duplicados
- Almacenamiento en base de datos SQLite con Room
- Detección automática de rostros con ML Kit

✅ **Reconocimiento Facial**
- Comparación de características faciales (landmarks)
- Búsqueda en base de datos de rostros registrados
- Umbral de similitud del 65%
- Resultados con porcentaje de coincidencia

✅ **Validaciones Implementadas**
- DNI debe tener exactamente 8 dígitos
- DNI único (no se puede registrar dos veces)
- Solo se permite un rostro por captura
- Validación de nombre no vacío

## Tecnologías Utilizadas

- **Jetpack Compose** - UI moderna y declarativa
- **Room Database** - Persistencia de datos con SQLite
- **CameraX** - Captura de imágenes
- **ML Kit Face Detection** - Detección y análisis de rostros
- **Kotlin Coroutines** - Programación asíncrona
- **Navigation Compose** - Navegación entre pantallas
- **Material Design 3** - Diseño moderno

## Estructura del Proyecto

```
app/src/main/java/com/example/facedetector/
├── data/
│   ├── FaceEntity.kt          # Entidad de Room
│   ├── FaceDao.kt             # Data Access Object
│   ├── FaceDatabase.kt        # Base de datos Room
│   └── FaceRepository.kt      # Repositorio de datos
├── ml/
│   └── FaceDetectionService.kt # Servicio de ML Kit
├── viewmodel/
│   ├── RegisterViewModel.kt    # ViewModel de registro
│   └── RecognitionViewModel.kt # ViewModel de reconocimiento
├── ui/
│   ├── camera/
│   │   └── CameraPreview.kt   # Componente de cámara
│   ├── screens/
│   │   ├── HomeScreen.kt      # Pantalla principal
│   │   ├── RegisterScreen.kt  # Pantalla de registro
│   │   └── RecognitionScreen.kt # Pantalla de reconocimiento
│   └── theme/                 # Tema de la app
├── navigation/
│   ├── Screen.kt              # Rutas de navegación
│   └── NavigationGraph.kt     # Grafo de navegación
└── MainActivity.kt            # Actividad principal
```

## Flujo de la Aplicación

### 1. Registro de Rostro
1. Usuario selecciona "Registrar Rostro"
2. Ingresa DNI (8 dígitos) y nombre completo
3. Captura foto con cámara frontal
4. Sistema detecta el rostro y extrae características
5. Valida que el DNI no exista en la BD
6. Guarda en SQLite con imagen y características faciales

### 2. Reconocimiento de Rostro
1. Usuario selecciona "Reconocer Rostro"
2. Captura foto con cámara frontal
3. Sistema detecta el rostro y extrae características
4. Compara con todos los rostros registrados
5. Calcula similitud usando distancia euclidiana de landmarks
6. Si similitud >= 65%, muestra datos de la persona
7. Si no, indica que no se encontró coincidencia

## Algoritmo de Comparación Facial

El sistema utiliza un algoritmo basado en landmarks faciales:

1. **Extracción de características:**
   - Posiciones de ojos, nariz, boca, mejillas, orejas
   - Ángulos de rotación de la cabeza
   - Dimensiones del bounding box
   - Probabilidades de sonrisa y ojos abiertos

2. **Comparación:**
   - Calcula distancia euclidiana entre landmarks correspondientes
   - Normaliza las distancias
   - Convierte a porcentaje de similitud (0-100%)
   - Umbral: 65% para considerar coincidencia

## Requisitos

- Android Studio (última versión)
- Kotlin 1.9.20+
- minSdk: 24 (Android 7.0)
- targetSdk: 36

## Permisos Necesarios

- `CAMERA` - Para captura de fotos
- `WRITE_EXTERNAL_STORAGE` - Para dispositivos con Android <= 9

## Base de Datos

**Tabla: faces**
- `id` (Long) - PRIMARY KEY
- `dni` (String) - UNIQUE, 8 dígitos
- `nombre` (String) - Nombre completo
- `imagenBytes` (ByteArray) - Imagen en formato JPEG
- `faceFeatures` (String) - JSON con características faciales
- `timestamp` (Long) - Fecha de registro

## Compilación y Ejecución

1. Abrir proyecto en Android Studio
2. Sincronizar Gradle
3. Conectar dispositivo Android o iniciar emulador
4. Ejecutar la aplicación

**Nota:** Se recomienda usar un dispositivo físico para mejor rendimiento de la cámara.

## Configuración de ML Kit

ML Kit descargará automáticamente los modelos de detección facial en el primer uso. Asegúrate de tener conexión a internet la primera vez que uses la app.

## Notas Técnicas

- La cámara usa el modo frontal para facilitar el selfie
- Las imágenes se almacenan en formato JPEG con compresión 80%
- El umbral de similitud puede ajustarse en `RecognitionViewModel`
- El sistema es compatible con rotación y diferentes ángulos faciales

## Mejoras Futuras Posibles

- Implementar FaceNet para embeddings más precisos
- Agregar autenticación biométrica
- Exportar/importar base de datos
- Historial de reconocimientos
- Modo batch para registrar múltiples personas
- Soporte para cámara trasera
- Filtros y ajustes de imagen

## Autor

Proyecto de práctica - Sistema de Reconocimiento Facial Android

