---
name: oneui-glance-design
description: Reglas estrictas de diseño visual One UI 8.5 para widgets en Jetpack Glance y pantallas Material 3 en Samsung Galaxy S24 Ultra. Activar siempre que se cree o modifique UI o el Widget.
---

# Directrices Visuales: One UI 8.5 + EMT Valencia (Jetpack Glance)

## 1. Estética del Widget (GlanceAppWidget)
- **Contenedor Raíz (`Box` / `Column`):**
  - `cornerRadius(26.dp)` (curvatura nativa de widgets de Samsung One UI 8.5).
  - Padding interno: `16.dp`.
  - Fondo (`background`): `#17171A` (modo oscuro profundo de Samsung) o `GlanceTheme.colors.surface` con ligera transparencia.
- **Cabecera del Widget:**
  - Fila superior (`Row` con `Alignment.CenterVertically`) que ocupa todo el ancho (`fillMaxWidth()`).
  - Izquierda: Icono o indicador de estado + título pequeño `"EMT València"` en `#8E8E93` (`12.sp`).
  - Derecha: Botón de refresco circular discreto (`32.dp`) con icono `ic_refresh`.
- **Filas de Paradas y Buses (Máximo impacto visual):**
  - Cada línea activa va en una tarjeta interior (`Row`) con fondo `#242429`, `cornerRadius(16.dp)` y `padding(horizontal = 12.dp, vertical = 10.dp)`.
  - **Badge de la Línea (ej. "C3"):** Caja redondeada (`cornerRadius(8.dp)`) con color rojo oficial EMT Valencia (`#E30613`), texto blanco en negrita (`14.sp`).
  - **Nombre de Parada:** Texto principal blanco (`#FFFFFF`, `14.sp`, Bold, `maxLines = 1`), seguido debajo del código de parada en gris (`#9E9EA6`, `11.sp`, ej. `"Parada 1694"`).
  - **Tiempos de llegada (2 buses):** Alineados a la derecha.
    - Primer bus (Próximo): Texto grande y destacado (`16.sp`, Bold). Si es `<= 3 min`, usar color verde menta/esmeralda (`#30D158`); si es `> 3 min`, usar blanco o ámbar suave (`#FFD60A`).
    - Segundo bus (Siguiente): Texto secundario debajo o al lado (`12.sp`, `#8E8E93`, ej. `"• 11 min"`).

## 2. Restricciones Técnicas Críticas de Jetpack Glance
- **PROHIBIDO** importar `androidx.compose.material3.*` o `androidx.compose.foundation.*` dentro de archivos del widget.
- **OBLIGATORIO** usar exclusivamente `androidx.glance.*`, `androidx.glance.appwidget.*`, `androidx.glance.layout.*` y `androidx.glance.text.*`.