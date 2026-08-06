# Oído Libre

Oído Libre es un prototipo de aplicación para ayudar a personas adultas con pérdida auditiva leve o moderada a escuchar mejor mediante el micrófono del teléfono y unos audífonos.

La aplicación permite hacer una prueba sencilla con tonos, ajustar cada oído por separado y guardar la configuración encontrada.

> Importante: Oído Libre está en etapa de desarrollo y pruebas. No sustituye una valoración profesional ni un audífono médico certificado.

## Dónde está el código principal

El archivo principal de la aplicación es:

`index.html`

Ahí se encuentran juntas:

- Las tres secciones principales.
- El diseño y los colores.
- El control del micrófono.
- La prueba con tonos.
- Los perfiles Casa y Calle.
- El guardado de la configuración.

Para hacer una corrección sencilla, normalmente se modifica únicamente `index.html`.

## Cómo reconocer la carpeta correcta

La carpeta completa del proyecto debe contener, como mínimo:

- `index.html`
- `README.md`
- `package.json`
- `capacitor.config.json`
- La carpeta `android`
- La carpeta `src`
- La carpeta `public`

No debe trabajarse solamente con el archivo que está dentro de `android/app/src/main/assets/public`, porque esa es una copia generada para Android.

## Cómo ver la aplicación en la computadora

1. Abrir una terminal dentro de la carpeta `oidolibre`.
2. La primera vez, ejecutar:

```bash
npm install
```

3. Para abrir la aplicación durante el desarrollo, ejecutar:

```bash
npm run dev
```

4. Abrir en el navegador la dirección que aparezca en la terminal.

## Cómo pasar los cambios a Android

Después de modificar `index.html`, ejecutar:

```bash
npm run build
npx cap sync android
```

El primer comando prepara la versión final. El segundo copia esa versión al proyecto Android.

Si no se ejecutan ambos comandos, Android puede conservar una pantalla anterior aunque `index.html` ya esté corregido.

## Cómo abrir Android

Después de compilar y sincronizar:

```bash
npx cap open android
```

Esto abre el proyecto en Android Studio, desde donde se podrá probar en un teléfono o generar un APK.

## Piedrita 1 — Arranque controlado

La primera corrección incluye:

- Arranque del micrófono en silencio.
- Aumento gradual durante 1.5 segundos.
- Limitador para reducir golpes repentinos.
- Límite interno de amplificación.
- Guardado de los niveles de cada oído.
- Recuperación de la última configuración al volver a abrir.
- En la versión web, apagado del micrófono cuando la página pasa a segundo plano.
- Sincronización entre la versión principal y Android.
- Uso de la palabra **nivel** en lugar de **dB**, porque todavía no existe una calibración física en decibeles reales.

## Reglas de seguridad actuales

- Comenzar siempre con el volumen físico del teléfono bajo.
- Utilizar audífonos para evitar eco o chillidos.
- Por ahora se recomiendan audífonos alámbricos por su menor retraso y mayor estabilidad.
- No activar la amplificación si el sonido está saliendo por la bocina del teléfono.
- Si aparece un sonido incómodo o demasiado fuerte, desactivar inmediatamente.
- Los niveles mostrados por la aplicación no representan todavía decibeles reales.

## Piedrita 2 — OL-001 Audífonos conectados

La versión 1.1 incorpora un componente nativo de Android para:

- Detectar si existen audífonos conectados.
- Mostrar qué salida de audio está utilizando el teléfono.
- Impedir que la amplificación se active por la bocina.
- Apagarla si los audífonos se desconectan.
- Diferenciar entre audífonos alámbricos y Bluetooth.

Esta protección funciona dentro de la aplicación Android. Cuando se abre `index.html` directamente en un navegador, aparece el aviso **Modo navegador** y la detección automática no está disponible.

## Piedrita 3 — Prueba ampliada hasta nivel 45

La prueba auditiva ahora contiene los niveles:

`5, 10, 15, 20, 25, 30, 35, 40 y 45`

Los controles izquierdo y derecho también permiten ajustar hasta el nivel 45. Los niveles 40 y 45 amplían el margen de prueba, pero continúan pasando por el limitador protector. Estos números siguen siendo niveles internos y no decibeles físicamente calibrados.

## Piedrita 4 — Micrófono y canales Android

La versión Android 1.2 incorpora:

- El permiso `MODIFY_AUDIO_SETTINGS` requerido por Capacitor para entregar el micrófono al WebView.
- La orientación normal de los canales: izquierdo sale por el oído izquierdo y derecho por el oído derecho.
- Un mensaje de error propio de la aplicación que muestra el nombre técnico del fallo cuando Android rechaza el micrófono.

La prueba auditiva realizada con la versión 1.1 debe repetirse, porque los canales estaban intercambiados y sus resultados por oído no son válidos para ajustar el perfil definitivo.

## Piedrita 5 — Separación estéreo Android 1.3

La separación mediante `ChannelMerger` no fue consistente dentro del WebView de Android: un canal podía aparecer intercambiado o quedar sin sonido. La versión 1.3 utiliza `StereoPanner` tanto en el Control Principal como en la prueba:

- Rama izquierda fijada explícitamente a la posición `-1`.
- Rama derecha fijada explícitamente a la posición `+1`.
- Controles de ganancia independientes antes del limitador.
- Tonos de prueba dirigidos mediante el mismo sistema estéreo.

La prueba debe repetirse con audífonos alámbricos y después con Bluetooth. Si un modelo convierte internamente la señal a mono, deberá registrarse como no compatible con la separación por oído.

## Piedrita 6 — Escala progresiva y perfiles Android 1.4

Los niveles del Control Principal ahora utilizan una escala progresiva entre 0 y 45:

- Nivel 0 silencia el canal.
- Los niveles bajos producen cambios pequeños.
- Los niveles altos aumentan progresivamente sin rebasar el límite interno.
- Casa aplica niveles 36 izquierdo y 34 derecho.
- Calle aplica niveles 20 izquierdo y 18 derecho.

Antes de esta corrección, Casa y Calle producían prácticamente el mismo volumen porque ambos perfiles alcanzaban inmediatamente el límite máximo. La calidad, potencia y respuesta de los audífonos Bluetooth sigue dependiendo del modelo; no se aumenta automáticamente el máximo porque los audífonos alámbricos pueden entregar una salida considerablemente mayor.

## Piedrita 7 — Audio continuo Android 1.5 (OL-002)

La amplificación de Android ya no depende de que la pantalla de la aplicación permanezca visible. La versión 1.5 incorpora un servicio nativo que:

- Continúa activo al abrir otra aplicación o apagar la pantalla.
- Muestra la notificación permanente **Oído Libre está activo**.
- Incluye la acción **Detener** dentro de la notificación.
- Conserva los niveles izquierdo y derecho mientras funciona.
- Mantiene despierto únicamente el procesamiento de audio, no la pantalla.
- Se detiene por seguridad si se desconectan los audífonos.

El servicio debe iniciarse mientras Oído Libre está visible. Android no permite que una aplicación inicie silenciosamente el micrófono desde el fondo.

## Piedrita 8 — Uso automático y accesible Android 1.6

La versión 1.6 facilita el uso cotidiano para personas mayores:

- Al guardar los audífonos en su cargador, la amplificación entra en pausa sin apagarse.
- Al volver a conectar los audífonos, el sonido se reanuda automáticamente con los niveles guardados.
- La notificación cambia entre **Oído Libre está activo** y **Oído Libre espera tus audífonos**.
- El botón **Detener** apaga completamente el servicio cuando la persona así lo decide.
- Los perfiles Casa y Calle aparecen también en el Control Principal.
- Las pantallas admiten desplazamiento y dejan espacio suficiente sobre el menú inferior.
- Los controles se acomodan en una sola columna en teléfonos, incluso con la fuente de Android al máximo.
- Al tocar **ACTIVAR**, Android solicita automáticamente los permisos de micrófono y notificaciones cuando hagan falta.

## Piedrita 9 — Interfaz accesible Android 1.6.1

La versión 1.6.1 reorganiza OídoLibre para que pueda usarse con el tamaño de fuente y visualización de Android al máximo:

- Menú inferior reducido a **Escuchar**, **Prueba** y **Más**.
- Encabezado compacto para dejar más espacio al contenido.
- Controles izquierdo y derecho colocados uno debajo del otro.
- Botones grandes **−** y **+** para ajustar cada oído.
- Casa y Calle visibles en la pantalla principal y con selección claramente marcada.
- Guardado automático de niveles, sin depender de un botón adicional.
- Tarjetas de altura flexible y desplazamiento hasta el final de cada pantalla.
- Instrucciones de la prueba más cortas y ordenadas.
- Mensaje de permiso con acceso directo a los ajustes de OídoLibre.
- Recursos visuales principales incluidos en la aplicación para no depender de Internet.

## Piedrita 10 — Baja latencia Android 1.7

La versión 1.7 reduce el retraso entre lo que capta el micrófono y lo que llega a los audífonos sin cambiar los niveles ni los perfiles ya probados:

- El audio deja de procesarse obligatoriamente en bloques de 1024 muestras y utiliza bloques pequeños adaptados al teléfono.
- Se usa la frecuencia de salida nativa informada por Android para evitar conversiones innecesarias.
- En Android 8 o superior se solicita explícitamente el modo de reproducción de baja latencia.
- El hilo de procesamiento recibe prioridad de audio para disminuir pausas provocadas por otras tareas del teléfono.
- El búfer de reproducción se ajusta a dos ráfagas como punto de partida entre respuesta rápida y estabilidad.
- Se conservan el funcionamiento en segundo plano, la pausa/reanudación al conectar audífonos, los canales izquierdo/derecho y los perfiles Casa/Calle.

Bluetooth todavía puede añadir retraso propio del enlace inalámbrico. Para medir la mejora de OídoLibre, primero se recomienda comparar 1.6.1 contra 1.7 con los mismos audífonos alámbricos y el mismo teléfono.

## Próximas piedritas

1. Comparar el retraso de Android 1.7 contra 1.6.1 con audífonos alámbricos.
2. Repetir la comparación con Bluetooth.
3. Probar una conversación de dos o tres personas y comprobar que las voces se enciman menos.
4. Confirmar que la pausa/reanudación automática y el botón **Detener** siguen funcionando.
5. Si aún existe un retraso perceptible con cable, evaluar el siguiente salto del motor de audio a Oboe/AAudio nativo.

## Respaldo antes de modificar

Antes de hacer cambios importantes:

1. Cerrar la aplicación y Android Studio.
2. Comprimir la carpeta completa `oidolibre` en un archivo ZIP.
3. Nombrarlo con la piedrita o la fecha, por ejemplo:

`oidolibre-piedrita-1.zip`

No enviar solamente una fotografía de la pantalla ni copiar fragmentos aislados. El ZIP completo conserva la estructura necesaria para continuar.

## Estado actual

- Proyecto web: disponible.
- Proyecto Android con Capacitor: disponible.
- Compilación: comprobada.
- APK instalable para pruebas: listo para generarse desde Android Studio.
- Detección nativa de audífonos: incorporada; pendiente de prueba física.
- Publicación comercial: no iniciada.

---

Método de trabajo: **piedrita por piedrita**.
