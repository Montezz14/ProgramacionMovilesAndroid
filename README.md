# Flappy Bird - Práctica Tema 12

Práctica del módulo **Programación Multimedia y Dispositivos Móviles**.
Implementación de un minijuego 2D estilo *Flappy Bird* siguiendo la **Opción B** del enunciado: arquitectura SurfaceView con Game Loop manual, sin motores externos.

## 📋 Requisitos del enunciado cumplidos

| # | Requisito | Implementación |
|---|-----------|----------------|
| 1 | Arquitectura **SurfaceView** | `GameView extends SurfaceView implements Runnable` |
| 2 | **Game Loop** en hilo separado | `Thread` propio con `while(corriendo)` en `run()` |
| 3 | **Bitmap / Rect** para gráficos | Dibujo con primitivas + uso de `Rect` para colisiones |
| 4 | **Colisiones Bounding Box** | `Rect.intersects()` entre pájaro y tuberías |
| 5 | **Ciclo de vida** correcto | `surfaceCreated` arranca hilo, `surfaceDestroyed` lo detiene con `join()` |
| Extra | **Persistencia de récord** | `SharedPreferences` guarda la puntuación máxima |

## 🛠 Instalación

### Requisitos previos

- **Android Studio** Hedgehog 2023.1.1 o superior
- **JDK 8** o superior
- **Android SDK** con plataforma API 34 instalada
- Un emulador o dispositivo físico con **Android 5.0 (API 21)** mínimo

### Pasos

1. Clona o descarga este repositorio:
   ```bash
   git clone https://github.com/<tu-usuario>/FlappyBirdGame.git
   ```
2. Abre Android Studio → **File** → **Open** → selecciona la carpeta `FlappyBirdGame`.
3. Espera a que termine la sincronización de **Gradle**.
4. Conecta un dispositivo Android o arranca un emulador.
5. Pulsa **Run ▶** (o `Shift + F10`).

### Alternativa: descargar el .zip desde GitHub

En la página del repositorio: **Code → Download ZIP**, descomprimir y abrir con Android Studio.

## 🎮 Cómo jugar

- **Toca la pantalla** para que el pájaro salte.
- Esquiva las tuberías verdes que vienen desde la derecha.
- Cada tubería superada suma **1 punto**.
- Si tocas una tubería, el suelo o el techo: **Game Over**.
- Tu mejor puntuación se guarda automáticamente.

## 📁 Estructura del proyecto

```
FlappyBirdGame/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/practica/flappybird/
│       │   ├── MainActivity.java     ← Activity principal
│       │   ├── GameView.java         ← SurfaceView + Game Loop
│       │   └── Tuberia.java          ← Modelo de obstáculo
│       └── res/
│           └── values/
│               ├── strings.xml
│               └── themes.xml
├── build.gradle
├── settings.gradle
└── README.md
```

## 👤 Autor

Práctica académica - Tema 12.
