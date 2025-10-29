# 📱 Arquitectura de la Aplicación Face Detector

## 🎯 Descripción General

Esta es una aplicación de **reconocimiento facial** que permite:
- ✅ Registrar personas con su foto, DNI (8 dígitos) y nombre
- 🔍 Reconocer personas a partir de una foto
- 📋 Ver lista de todas las personas registradas
- ✏️ Actualizar fotos (con validación de similitud)
- 🗑️ Eliminar registros

## 🏗️ Arquitectura

La aplicación sigue el patrón **MVVM (Model-View-ViewModel)** con **Clean Architecture**:

```
┌─────────────────────────────────────────────────────────┐
│                      UI (Compose)                       │
│  HomeScreen, RegisterScreen, RecognitionScreen, etc.    │
└─────────────────┬───────────────────────────────────────┘
                  │ observa StateFlow
┌─────────────────▼───────────────────────────────────────┐
│                    ViewModels                           │
│  RegisterVM, RecognitionVM, ListVM, DetailVM            │
└─────────────────┬───────────────────────────────────────┘
                  │ usa
┌─────────────────▼───────────────────────────────────────┐
│              Repository (FaceRepository)                │
│         Lógica de negocio y validaciones                │
└─────────────────┬───────────────────────────────────────┘
                  │ accede
┌─────────────────▼───────────────────────────────────────┐
│        Database (Room SQLite + ML Kit)                  │
│  FaceDao, FaceEntity, FaceDetectionService              │
└─────────────────────────────────────────────────────────┘
```

## 📂 Estructura de Carpetas

```
app/src/main/java/com/example/facedetector/
├── 📁 data/                    # Capa de Datos
│   ├── FaceEntity.kt          # Entidad de tabla (persona registrada)
│   ├── FaceDao.kt             # Operaciones de BD (CRUD)
│   ├── FaceDatabase.kt        # Configuración de Room
│   └── FaceRepository.kt      # Intermediario con lógica de negocio
│
├── 📁 ml/                      # Machine Learning
│   └── FaceDetectionService.kt # Detección y comparación facial (ML Kit)
│
├── 📁 viewmodel/               # Capa de Lógica de Negocio
│   ├── RegisterViewModel.kt   # Lógica de registro
│   ├── RecognitionViewModel.kt # Lógica de reconocimiento
│   ├── ListViewModel.kt       # Lógica de lista
│   └── DetailViewModel.kt     # Lógica de detalle/edición
│
├── 📁 ui/                      # Capa de Presentación
│   ├── screens/               # Pantallas de la app
│   │   ├── HomeScreen.kt      # Menú principal
│   │   ├── RegisterScreen.kt  # Registrar persona
│   │   ├── RecognitionScreen.kt # Reconocer persona
│   │   ├── ListScreen.kt      # Lista de registrados
│   │   └── DetailScreen.kt    # Detalle y edición
│   └── theme/                 # Tema de la app
│
├── 📁 navigation/              # Navegación
│   ├── Screen.kt              # Definición de rutas
│   └── NavigationGraph.kt     # Grafo de navegación
│
└── MainActivity.kt             # Punto de entrada de la app
```

## 🔄 Flujo de Datos

### 1️⃣ Registro de Persona
```
Usuario captura foto → RegisterScreen → RegisterViewModel
                                           ↓
                                 Valida DNI (8 dígitos)
                                 Valida nombre (no vacío)
                                 Verifica DNI no duplicado
                                           ↓
                              FaceDetectionService.detectFaces()
                                (Detecta rostro con ML Kit)
                                           ↓
                              Verifica que hay 1 solo rostro
                                           ↓
                         FaceDetectionService.extractFaceFeatures()
                         (Extrae landmarks: ojos, nariz, boca, etc.)
                                           ↓
                              Repository.insertFace()
                                           ↓
                                   Room SQLite
                                 (Guarda en BD)
```

### 2️⃣ Reconocimiento de Persona
```
Usuario captura foto → RecognitionScreen → RecognitionViewModel
                                              ↓
                               FaceDetectionService.detectFaces()
                                              ↓
                          Extrae características del rostro
                                              ↓
                          Repository.getAllFacesList()
                          (Obtiene todos los registrados)
                                              ↓
                    Para cada persona en BD:
                    FaceDetectionService.compareFaces()
                    (Calcula similitud con distancia euclidiana)
                                              ↓
                    Encuentra la mejor coincidencia
                                              ↓
                    ¿Similitud >= 65%?
                    ├─ SÍ → Persona reconocida ✅
                    └─ NO → No encontrado ❌
```

### 3️⃣ Actualización de Foto
```
Usuario captura nueva foto → DetailScreen → DetailViewModel
                                               ↓
                               Detecta rostro en nueva foto
                                               ↓
                               Extrae características
                                               ↓
                       Compara con foto actual (similitud)
                                               ↓
                       ¿Similitud >= 70%?
                       ├─ SÍ → Actualiza foto ✅
                       └─ NO → Rechaza (no es la misma persona) ❌
```

## 🗄️ Base de Datos (Room SQLite)

### Tabla: `faces`
| Campo          | Tipo       | Descripción                              |
|----------------|------------|------------------------------------------|
| id             | Long (PK)  | ID autogenerado                          |
| dni            | String     | DNI de 8 dígitos (índice único)          |
| nombre         | String     | Nombre de la persona                     |
| imagenBytes    | ByteArray  | Foto comprimida en JPEG (80% calidad)    |
| faceFeatures   | String     | JSON con landmarks faciales              |
| timestamp      | Long       | Fecha/hora de creación                   |

**Índice único en DNI:** Previene registros duplicados del mismo DNI.

## 🤖 Machine Learning (ML Kit)

### Configuración del Detector
```kotlin
PERFORMANCE_MODE_ACCURATE  // Precisión > velocidad
LANDMARK_MODE_ALL          // Detecta todos los puntos clave
CLASSIFICATION_MODE_ALL    // Clasifica sonrisa y ojos
MinFaceSize = 15%          // Tamaño mínimo del rostro
```

### Características Extraídas
1. **Bounding Box**: Rectángulo del rostro (left, top, right, bottom)
2. **Ángulos de Euler**: Rotación de cabeza (X, Y, Z)
3. **Landmarks** (10 puntos):
   - Ojo izquierdo/derecho
   - Nariz
   - Mejillas
   - Boca (izquierda/derecha/inferior)
   - Orejas
4. **Probabilidades**:
   - Sonrisa
   - Ojos abiertos

### Algoritmo de Comparación
```
1. Extraer landmarks de ambos rostros
2. Para cada landmark:
   - Calcular distancia euclidiana: sqrt((x1-x2)² + (y1-y2)²)
3. Promediar todas las distancias
4. Normalizar a porcentaje: similarity = (1 / (1 + dist/100)) * 100
```

**Umbrales de Similitud:**
- Reconocimiento: **65%** (más permisivo)
- Actualización de foto: **70%** (más estricto)

## 🎨 UI (Jetpack Compose)

Toda la interfaz está construida con **Jetpack Compose** (UI declarativa).

### Pantallas
1. **HomeScreen**: Menú con 3 opciones (Registrar, Reconocer, Lista)
2. **RegisterScreen**: Cámara + campos DNI y nombre
3. **RecognitionScreen**: Cámara + resultado de reconocimiento
4. **ListScreen**: Lista reactiva de personas (LazyColumn)
5. **DetailScreen**: Detalle + actualizar foto + eliminar

### Estados de UI (Sealed Classes)
```kotlin
sealed class RegisterUiState {
    object Initial      // Estado inicial
    object Loading      // Procesando
    data class Success  // Éxito con mensaje
    data class Error    // Error con mensaje
}
```

Cada ViewModel tiene su propio UiState que la pantalla observa para reaccionar.

## 🔐 Validaciones Implementadas

✅ **DNI**:
- Exactamente 8 dígitos numéricos
- No puede repetirse (índice único en BD)

✅ **Nombre**:
- No puede estar vacío

✅ **Rostro**:
- Debe detectarse exactamente 1 rostro
- No se permiten múltiples rostros

✅ **Actualización de Foto**:
- Nueva foto debe ser >= 70% similar a la actual
- Previene cambiar foto por otra persona

## 📊 Tecnologías Utilizadas

| Tecnología           | Propósito                          |
|----------------------|------------------------------------|
| Kotlin               | Lenguaje de programación           |
| Jetpack Compose      | UI declarativa                     |
| Room                 | Base de datos SQLite               |
| ML Kit Face Detection| Detección facial                   |
| Coroutines + Flow    | Programación asíncrona             |
| Navigation Compose   | Navegación entre pantallas         |
| CameraX              | Acceso a cámara                    |
| ViewModel            | Gestión de estado                  |

## 🔄 Flujo de Estados (StateFlow)

```kotlin
ViewModel mantiene:
private val _state = MutableStateFlow(InitialState)
val state: StateFlow = _state.asStateFlow()

UI observa:
val state by viewModel.state.collectAsState()

Cuando state cambia → UI se recompone automáticamente
```

## 🎯 Características Especiales

### 1. Lista Reactiva
La lista de personas se actualiza automáticamente cuando se agrega/elimina alguien gracias a `Flow<List<FaceEntity>>`.

### 2. Actualización Segura de Foto
Al actualizar una foto, el sistema verifica que la nueva foto sea similar (70%) a la actual, evitando suplantaciones.

### 3. Factory Pattern para DetailViewModel
Como DetailViewModel necesita recibir el DNI como parámetro, usa un Factory personalizado:
```kotlin
val viewModel: DetailViewModel = viewModel(
    factory = DetailViewModel.Factory(dni)
)
```

### 4. Limpieza de Recursos
Todos los ViewModels liberan el `FaceDetectionService` en `onCleared()` para evitar memory leaks.

## 📝 Resumen de Responsabilidades

| Capa          | Responsabilidad                                    |
|---------------|-----------------------------------------------------|
| **UI**        | Mostrar datos, capturar entrada del usuario        |
| **ViewModel** | Lógica de negocio, validaciones, gestión de estado |
| **Repository**| Abstracción de fuente de datos, validaciones de BD |
| **DAO**       | Operaciones CRUD en SQLite                          |
| **ML Service**| Detección y comparación facial con ML Kit           |

## 🚀 Puntos de Mejora Futuros

1. ✨ Implementar migraciones de BD (en vez de destructive)
2. 🔒 Agregar encriptación de imágenes
3. 📊 Mejorar algoritmo de comparación (usar embeddings más sofisticados)
4. 🎨 Agregar animaciones en transiciones
5. 📱 Soportar modo oscuro
6. 🧪 Agregar tests unitarios e instrumentados
7. 🌐 Sincronización con servidor remoto

---

**¡La aplicación está completamente documentada! 🎉**

Todos los archivos tienen comentarios detallados explicando cada concepto.

