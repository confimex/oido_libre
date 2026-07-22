# Oído Libre

Oído Libre es un prototipo de aplicación para ayudar a personas adultas con pérdida auditiva leve o moderada a escuchar mejor mediante el micrófono del teléfono y unos audífonos.

La aplicación permite hacer una prueba sencilla con tonos, ajustar cada oído por separado y guardar la configuración encontrada.

> Importante: Oído Libre está en etapa de desarrollo y pruebas. No sustituye una valoración profesional ni un audífono médico certificado.

## Dónde está el código principal

El archivo principal de la aplicación es:

`index.html`

Ahí se encuentran juntas:

- Las cuatro pantallas.
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
- Apagado del micrófono cuando la aplicación pasa a segundo plano.
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

## Próximas piedritas

1. Subir la versión 1.1 al repositorio conectado con Vercel.
2. Generar el primer APK de prueba desde Android Studio.
3. Probar OL-001 en un teléfono real con audífonos alámbricos y Bluetooth.
4. Revisar qué micrófono utiliza el teléfono.
5. Preparar los recursos visuales para funcionamiento sin Internet.
6. Documentar los modelos de audífonos recomendados.

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
