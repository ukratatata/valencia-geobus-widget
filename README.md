# València GeoBus Widget 🚌
### Widget Nativo para Samsung Galaxy S24 Ultra (Android 16 · API 36 · One UI 8.5)

Aplicación nativa de Android en Kotlin y widget interactivo en **Jetpack Glance** para consultar las llegadas en tiempo real de los autobuses de la **EMT de València** según la proximidad geográfica del usuario a sus paradas configuradas.

Diseñado específicamente con la estética de **Samsung One UI 8.5**, curvatura `26.dp`, paleta oscura profunda (`#17171A`), acentos en rojo oficial EMT (`#E30613`), y verde menta (`#30D158`) para llegadas inminentes.

---

## 🚀 Características Principales

1. **Catálogo Oficial de 1.155 Paradas (OpenData València / Geoportal):**
   * Descarga y cacheo local en **Room Database** en menos de 0,3 segundos mediante GeoJSON WGS84.
   * **Actualización Dinámica de Paradas:** Botón integrado en la app para refrescar el catálogo completo oficial y sincronizar automáticamente las coordenadas de tus paradas guardadas por si han cambiado de sitio o trazado.

2. **Detección de Desvíos e Incidencias:**
   * Parser en tiempo real con detección inteligente de avisos de desvío, obras o líneas temporalmente alteradas (`⚠ Desvío`).

3. **Cero Polling en Segundo Plano (100% Ahorro de Batería):**
   * El widget **NO** realiza actualizaciones periódicas automáticas en segundo plano (`updatePeriodMillis="0"`). Cero consumo de CPU y batería mientras no interactúas con él.

4. **Botón de Refresco Bajo Demanda (ActionCallback):**
   * Al pulsar el botón circular de refresco en la cabecera:
     1. Obtiene la ubicación GPS instantánea mediante `FusedLocationProviderClient` con alta precisión.
     2. Mide la distancia en metros entre el usuario y todas sus paradas guardadas.
     3. **Filtro de Zona ($\le 500\text{ m}$):** Si estás a menos de 500 metros, consulta en paralelo los tiempos de llegada y muestra los **2 próximos autobuses por línea** (ej. `C3 -> 2 min • 11 min`).
     4. **Modo Reposo / Inactivo:** Si estás a $> 500\text{ m}$ de todas tus paradas, pasa a estado inactivo sin realizar peticiones HTTP a la API de la EMT, informando además de la distancia a tu parada más cercana.

---

## 📱 Configuración de Permisos en Android 16 (One UI 8.5)

Para que el widget pueda leer el GPS directamente desde la pantalla de inicio al pulsar el botón sin tener que abrir la aplicación:
1. Abre **Ajustes** en tu Samsung Galaxy S24 Ultra.
2. Ve a **Aplicaciones** > **València GeoBus** > **Permisos** > **Ubicación**.
3. Selecciona **"Permitir todo el tiempo"** (necesario para widgets y lectura en segundo plano).
4. (Opcional en Samsung): En Batería, selecciona "Sin restricciones" para evitar que One UI suspenda los broadcasts del widget.

*(La propia aplicación incluye accesos directos desde la pantalla principal a estos menús de ajustes).*

---

## 🛠️ Tecnologías y Arquitectura

* **Lenguaje:** Kotlin 2.1+ con Compose Compiler nativo.
* **UI:** Jetpack Compose + Material 3 (One UI 8.5 Dark Theme).
* **Widget:** Jetpack Glance 1.1+ (`GlanceAppWidget`, `PreferencesGlanceStateDefinition`).
* **Base de Datos:** Room 2.6+ con KSP y serialización JSON.
* **Red:** OkHttp 4.12 con tiempos de espera de 10s y resiliencia ante fallos.
* **Geolocalización:** Google Play Services Location (`FusedLocationProviderClient`).
* **Gestión de Versiones:** Gradle Version Catalogs (`gradle/libs.versions.toml`).

---

## 📦 Compilación y Ejecución

Abre el proyecto directamente en **Android Studio** (Koala / Ladybug o superior) o compila desde terminal:

```bash
# Compilar APK de depuración
./gradlew assembleDebug

# Instalar en el dispositivo conectado (ej. S24 Ultra con depuración USB/WiFi)
./gradlew installDebug
```

---

Desarrollado para la comunidad de València y usuarios de la EMT.
