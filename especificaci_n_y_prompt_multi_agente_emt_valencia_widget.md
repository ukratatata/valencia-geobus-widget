# Especificación del Proyecto: GeoBus Widget Valencia (Android 16 / One UI 8.5)

Este documento define la arquitectura definitiva basada en tus decisiones de diseño, junto con el **Script de Descubrimiento de API (Fase 0)** y el **Prompt Maestro de Orquestación Multi-Agente** listo para lanzar en **Antigravity IDE**.

---

## 1. Decisiones de Arquitectura Confirmadas

1. **Geolocalización centrada en la Parada ($d \le 500\text{ m}$):**
   * No tendrás que introducir coordenadas manualmente. La app descargará el catálogo oficial de paradas de la EMT Valencia (del portal OpenData del Ayuntamiento de Valencia: `valencia.opendatasoft.com`, dataset `emt` / GTFS) y guardará en local (`Room`) el ID de parada (ej. `1694`), su nombre (`Primado Reig`), sus coordenadas exactas ($\text{lat}, \text{lon}$) y las líneas que paran en ella (ej. `C3`, `98`, etc.).
   * En la pantalla de configuración solo tendrás un buscador: escribes `"1694"` o `"Primado Reig"`, seleccionas la parada y marcas con un check qué línea(s) quieres monitorizar en esa parada.
   * Los $500\text{ m}$ se calculan directamente entre la ubicación GPS de tu S24 Ultra y las coordenadas oficiales de cada parada configurada.

2. **Permisos en Android 16 (One UI 8.5) y Cero Drenaje de Batería:**
   * Se declarará `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` y `ACCESS_BACKGROUND_LOCATION` en el `AndroidManifest.xml`.
   * Concederás manualmente el permiso *"Permitir todo el tiempo"* en Ajustes para que el widget pueda leer el GPS instantáneamente desde la pantalla de inicio al tocar el botón **Actualizar**, sin necesidad de abrir la aplicación ni mantener servicios persistentes consumiendo batería.

3. **Solapamiento y Formato de Tiempos (2 Autobuses por Línea):**
   * Si estás a $\le 500\text{ m}$ de una o varias paradas configuradas, el widget mostrará todas las paradas/líneas activas en una lista (`LazyColumn` de Jetpack Glance).
   * Para cada línea elegida, mostrará los **2 próximos tiempos de llegada** (ejemplo: **`1694 · Línea C3 -> 3 min | 14 min`**).
   * Si al pulsar el botón de refresco estás a $> 500\text{ m}$ de todas tus paradas configuradas, el widget pasará a estado **Inactivo/Reposo** sin realizar llamadas a la API de tiempos reales.

---

## 2. Script de Descubrimiento y Verificación de Endpoints (Fase 0)

El Sub-Agente de Fase 0 ejecutará este script en Python en el terminal de Antigravity IDE para verificar la estructura JSON exacta del catálogo de paradas de OpenData Valencia y del endpoint de llegadas en tiempo real para la parada `1694` (Línea `C3`):

```python
import urllib.request
import json
import re

def test_opendata_stops(stop_id="1694"):
    print(f"--- 1. Probando OpenData Valencia (Catálogo de Paradas y Coordenadas para ID {stop_id}) ---")
    # API v2.1 de OpenDataSoft Ayuntamiento de Valencia
    url = f"https://valencia.opendatasoft.com/api/explore/v2.1/catalog/datasets/emt/records?where=id_parada%3D{stop_id}&limit=5"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=10) as response:
            data = json.loads(response.read().decode('utf-8'))
            print("Respuesta OpenData recibida correctamente:")
            print(json.dumps(data, indent=2, ensure_ascii=False)[:1000])
            return data
    except Exception as e:
        print(f"Error en OpenData endpoint primario: {e}")

def test_emt_realtime(stop_id="1694", line="C3"):
    print(f"\n--- 2. Probando Endpoints de Tiempo Real EMT Valencia (Parada {stop_id}) ---")
    # Endpoints habituales de consulta de tiempos de paso de EMT Valencia / Geoportal
    endpoints = [
        f"https://www.emtvalencia.es/ciudadano/modules/mod_tiempo/busca_parada.php?parada={stop_id}&adaptados=0&idioma=es",
        f"https://geoportal.valencia.es/geoportal-services/api/v1/salidas-parada?parada={stop_id}"
    ]
    for url in endpoints:
        print(f"Consultando: {url}")
        try:
            req = urllib.request.Request(url, headers={
                'User-Agent': 'Mozilla/5.0 (Linux; Android 16; SM-S928B) AppleWebKit/537.36',
                'Referer': 'https://www.emtvalencia.es/'
            })
            with urllib.request.urlopen(req, timeout=10) as resp:
                body = resp.read().decode('utf-8', errors='ignore')
                print(f"Status: {resp.status} | Longitud: {len(body)} chars")
                print("Muestra de respuesta:\n", body[:600])
                print("-" * 50)
        except Exception as e:
            print(f"Fallo en {url}: {e}")

if __name__ == "__main__":
    test_opendata_stops("1694")
    test_emt_realtime("1694", "C3")
```

---

## 3. Prompt Maestro para Antigravity IDE (Copiar y Pegar)

Copia y pega el siguiente prompt en el chat principal de **Antigravity IDE** para que tu agente principal (Orquestador) tome el control y coordine a los subagentes:

```markdown
# ROLE: Lead Android Architect & Multi-Agent Orchestrator

Actúa como el Agente Orquestador Principal (Tier-1 Model) para desarrollar desde cero una aplicación nativa de Android en Kotlin llamada "València GeoBus Widget", diseñada específicamente para un Samsung Galaxy S24 Ultra (Android 16, API 36, One UI 8.5).

## 1. ESTRATEGIA DE ORQUESTACIÓN Y AHORRO DE TOKENS
Tu misión es dirigir la arquitectura, tomar decisiones críticas, gestionar las interfaces entre módulos y coordinar subagentes de menor coste para no desperdiciar tokens del modelo principal:

- **[Sub-Agente Tier-3 / Modelo Ligero y Económico (ej. Haiku / Flash)]**: Asígnale la ejecución del script de exploración en Python, la creación de archivos Gradle (`libs.versions.toml`, `build.gradle.kts`), `AndroidManifest.xml`, entidades de base de datos `Room`, DTOs/Parsers y componentes de UI de configuración básicos.
- **[Sub-Agente Tier-2 / Modelo Intermedio]**: Asígnale la lógica de negocio: sincronización del catálogo oficial de paradas de EMT Valencia, parser de tiempos de llegada (extrayendo los 2 próximos autobuses por línea), cálculo de distancias con `FusedLocationProviderClient` y la interfaz del widget en `Jetpack Glance`.
- **[Sub-Agente QA / Bug Hunter]**: Al finalizar cada fase, lanza una revisión estática y de integración para cazar errores de compilación, problemas de permisos en segundo plano en Android 16, fugas de memoria o fallos de actualización de estado en `GlanceAppWidget` (`PreferencesGlanceStateDefinition` / DataStore).

*Regla de Contexto*: Mantén en tu contexto de Orquestador únicamente los contratos/interfaces de cada módulo y los resúmenes de estado. Pide a los subagentes que escriban directamente en los archivos del proyecto.

---

## 2. ESPECIFICACIONES FUNCIONALES CERRADAS

1. **Catálogo Oficial de Paradas y Configuración Simplificada:**
   - La app descarga y cachea en local (`Room`) el listado oficial de paradas de la EMT Valencia desde OpenData Valencia (`valencia.opendatasoft.com` dataset `emt`), incluyendo: `stopId` (ej. `1694`), `stopName` (ej. `Primado Reig`), coordenadas oficiales (`latitude`, `longitude`) y las líneas que dan servicio a esa parada (ej. `C3`).
   - En la pantalla principal de la app (Jetpack Compose), el usuario solo tiene que buscar la parada por número o nombre, seleccionarla y elegir de una lista qué línea(s) de esa parada desea monitorizar.
   - El radio de activación ($500\text{ metros}$ por defecto, configurable) se mide directamente desde las coordenadas oficiales de la parada guardada.

2. **Comportamiento del Widget (Jetpack Glance estilo One UI 8.5):**
   - **Cero Polling en Segundo Plano:** El widget NO se actualiza solo periódicamente para ahorrar un 100% de batería.
   - **Botón de Refresco Bajo Demanda:** Incluye un botón de "Actualizar" en la cabecera del widget (`ActionCallback`). Al pulsarlo:
     1. Cambia el estado visual del widget a `Cargando...`.
     2. Obtiene la ubicación GPS actual del usuario mediante `FusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, ...)`. (El usuario concederá `ACCESS_BACKGROUND_LOCATION` = "Permitir todo el tiempo" en Ajustes para permitir lecturas inmediatas desde el widget sin abrir Activity).
     3. Calcula la distancia en metros (`Location.distanceBetween`) entre la posición actual del usuario y todas las paradas configuradas en `Room`.
     4. **Evaluación de Zonas y Solapamiento:**
        - Filtra todas las paradas configuradas cuya distancia sea $\le 500\text{ m}$.
        - Si hay 1 o varias paradas dentro del radio de $500\text{ m}$ (solapamiento de zonas), consulta en paralelo el tiempo de llegada para cada parada activa y muestra una lista (`LazyColumn` de Glance) con todas las coincidencias.
        - **2 Autobuses por Línea:** Cada fila debe mostrar el código/nombre de parada, la línea objetivo y el tiempo de llegada de los **2 próximos autobuses** (ej. `1694 Primado Reig | C3 -> 2 min • 11 min`).
        - **Modo Inactivo / Reposo:** Si ninguna parada configurada está a $\le 500\text{ m}$, el widget pasa inmediatamente a estado `INACTIVE` mostrando un diseño limpio en reposo ("Fuera de tus zonas de paradas (>500m). Pulsa actualizar al acercarte") sin realizar peticiones HTTP a la API de tiempos de la EMT.

---

## 3. PLAN DE EJECUCIÓN POR FASES (CON GATES DE QA)

### FASE 0: Descubrimiento y Validación de Endpoints (Python Script)
- Ordena al Sub-Agente Tier-3 que cree y ejecute en terminal un script `scripts/test_emt_api.py` para consultar:
  1. La API v2.1 de OpenData Valencia (`https://valencia.opendatasoft.com/api/explore/v2.1/catalog/datasets/emt/records`) buscando la parada `1694` para confirmar los nombres exactos de los campos JSON (`id_parada`, `denominacion`, `geo_point_2d`, `lineas`).
  2. El endpoint de tiempos de paso en tiempo real de EMT Valencia para la parada `1694` y línea `C3`.
- Con la respuesta real en consola, congela los DTOs/parsers necesarios para Kotlin.

### FASE 1: Estructura del Proyecto, Gradle y Base de Datos (Room)
- Sub-Agente Tier-3 genera la estructura del proyecto Android (Kotlin 2.0+, Compose, Glance 1.1+, Room, Retrofit/OkHttp o Ktor, Coroutines, WorkManager).
- Define en `Room` dos entidades:
  - `EmtStopCatalogEntity` (catálogo descargado con coordenadas y líneas disponibles).
  - `UserMonitoredStopEntity` (paradas y líneas seleccionadas por el usuario + radio en metros).
- Ejecuta Sub-Agente QA para validar configuración de Gradle y permisos de Android 16 en `AndroidManifest.xml`.

### FASE 2: Sincronizador de Catálogo, Cliente Tiempo Real y Motor de Ubicación
- Sub-Agente Tier-2 implementa:
  - `EmtCatalogSyncRepository`: descarga las paradas de OpenData Valencia y las inserta en `Room`.
  - `EmtRealTimeService`: obtiene los 2 próximos tiempos de llegada para un par `(stopId, lineId)` usando el formato verificado en la Fase 0 (incluyendo fallback o modo prueba si no hay buses nocturnos en el momento del test).
  - `ProximityEvaluator`: obtiene ubicación GPS actual y devuelve las `UserMonitoredStopEntity` que están a $\le 500\text{ m}$ junto con la distancia exacta en metros (ej. `a 180m`).
- Ejecuta Sub-Agente QA para verificar timeouts de red, manejo de excepciones y cancelación de corrutinas.

### FASE 3: Widget en Jetpack Glance (UI One UI 8.5 + ActionCallback)
- Sub-Agente Tier-2 implementa `GeoBusGlanceWidget`, `GeoBusWidgetReceiver` y `RefreshBusTimesAction` (`ActionCallback`).
- Diseño con esquinas redondeadas estilo Samsung One UI 8.5, colores dinámicos/oscuros legibles, cabecera con hora de última actualización y botón de refresco, cuerpo con `LazyColumn` para soportar múltiples paradas solapadas mostrando los 2 próximos buses, y vista de Estado Inactivo cuando el usuario está fuera de rango.
- Ejecuta Sub-Agente QA para revisar la persistencia del estado del widget (`GlanceStateDefinition`) y asegurar que la UI se redibuja de inmediato tras pulsar el botón.

### FASE 4: UI de Configuración en Jetpack Compose y Guía de Permisos
- Sub-Agente Tier-3 crea una `MainActivity` en Jetpack Compose con:
  1. Tarjeta de estado de permisos (con botón directo a los Ajustes de la App para activar el permiso "Permitir todo el tiempo" de ubicación y excluir optimización de batería en Samsung).
  2. Buscador de paradas del catálogo local (por código como `1694` o nombre como `Primado Reig`).
  3. Selector de líneas de esa parada (ej. chips seleccionables para marcar `C3`) y lista de paradas monitorizadas actualmente con opción de borrar o probar refresco.
- Ejecuta Sub-Agente QA final con una compilación de prueba (`./gradlew assembleDebug`) y corrige cualquier warning o error.

Comienza ahora mismo ejecutando la **FASE 0** mediante el Sub-Agente correspondiente y muéstrame los payloads reales devueltos por los servidores de Valencia antes de pasar a la Fase 1.
```