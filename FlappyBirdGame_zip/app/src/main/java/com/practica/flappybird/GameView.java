package com.practica.flappybird;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Clase principal del juego.
 *
 * REQUISITOS DEL ENUNCIADO QUE CUMPLE ESTA CLASE:
 *   1. Extiende SurfaceView e implementa Runnable -> arquitectura SurfaceView.
 *   2. Game Loop manual mediante un Thread propio, separando lógica y
 *      renderizado del hilo principal de la UI.
 *   3. Uso de Bitmap/Canvas (en este caso dibujamos con primitivas geométricas
 *      directamente sobre el Canvas, que es lo que pidió el alumno).
 *   4. Detección de colisiones mediante Bounding Box con Rect.intersect().
 *   5. Gestión correcta del ciclo de vida con surfaceCreated y surfaceDestroyed
 *      para arrancar y parar el hilo sin causar fugas de memoria.
 *
 * EXTRA implementado:
 *   - Persistencia de la puntuación máxima con SharedPreferences.
 */
public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    // ----------------------------------------------------------------------
    // CONSTANTES DE CONFIGURACIÓN DEL JUEGO
    // ----------------------------------------------------------------------

    /** FPS objetivo del juego. Sirve para calcular el delay entre frames. */
    private static final int FPS_OBJETIVO = 60;

    /** Tiempo (ms) que debería durar cada frame para mantener 60 FPS. */
    private static final long TIEMPO_FRAME_MS = 1000 / FPS_OBJETIVO;

    /** Gravedad aplicada al pájaro en cada frame (píxeles/frame²). */
    private static final float GRAVEDAD = 0.8f;

    /** Velocidad vertical aplicada al pájaro cuando el jugador toca pantalla. */
    private static final float FUERZA_SALTO = -15f;

    /** Velocidad horizontal a la que se desplazan las tuberías hacia la izquierda. */
    private static final float VELOCIDAD_TUBERIAS = 8f;

    /** Distancia horizontal entre pares de tuberías. */
    private static final int DISTANCIA_ENTRE_TUBERIAS = 500;

    /** Hueco vertical entre la tubería superior y la inferior. */
    private static final int HUECO_TUBERIAS = 450;

    /** Ancho de las tuberías en píxeles. */
    private static final int ANCHO_TUBERIA = 150;

    /** Radio del pájaro (dibujado como círculo). */
    private static final int RADIO_PAJARO = 40;

    /** Nombre del fichero de SharedPreferences para guardar el récord. */
    private static final String PREFS_NAME = "FlappyBirdPrefs";
    private static final String KEY_RECORD = "puntuacionMaxima";

    // ----------------------------------------------------------------------
    // HILO Y CONTROL DEL GAME LOOP
    // ----------------------------------------------------------------------

    /** Hilo dedicado a la lógica y al renderizado del juego. */
    private Thread hiloJuego;

    /** Flag que controla el bucle while del game loop. volatile para visibilidad entre hilos. */
    private volatile boolean corriendo = false;

    /** Holder de la SurfaceView, necesario para bloquear el Canvas. */
    private final SurfaceHolder holder;

    // ----------------------------------------------------------------------
    // OBJETOS DEL JUEGO
    // ----------------------------------------------------------------------

    /** Posición vertical del pájaro. */
    private float pajaroY;
    /** Posición horizontal del pájaro (fija en el eje X). */
    private float pajaroX;
    /** Velocidad vertical actual del pájaro. */
    private float velocidadPajaro;

    /** Lista dinámica de tuberías activas en pantalla. */
    private ArrayList<Tuberia> tuberias;

    /** Rectángulo reutilizable para la "bounding box" del pájaro (evita crear objetos cada frame). */
    private final Rect rectPajaro = new Rect();

    // ----------------------------------------------------------------------
    // ESTADO Y PUNTUACIÓN
    // ----------------------------------------------------------------------

    /** Estados posibles del juego. */
    private enum Estado { INICIO, JUGANDO, FIN }
    private Estado estado = Estado.INICIO;

    private int puntuacion = 0;
    private int puntuacionMaxima = 0;

    private int anchoPantalla;
    private int altoPantalla;

    /** Paints precreados (NUNCA crear Paint dentro del game loop por rendimiento). */
    private final Paint paintCielo = new Paint();
    private final Paint paintTuberia = new Paint();
    private final Paint paintBordeTuberia = new Paint();
    private final Paint paintPajaro = new Paint();
    private final Paint paintOjo = new Paint();
    private final Paint paintPico = new Paint();
    private final Paint paintSuelo = new Paint();
    private final Paint paintTexto = new Paint();
    private final Paint paintTextoGrande = new Paint();

    private final SharedPreferences prefs;

    // ----------------------------------------------------------------------
    // CONSTRUCTOR
    // ----------------------------------------------------------------------

    public GameView(Context contexto) {
        super(contexto);
        // Registramos esta clase como callback del holder para recibir
        // los eventos surfaceCreated, surfaceChanged y surfaceDestroyed.
        holder = getHolder();
        holder.addCallback(this);

        // Cargamos la puntuación máxima guardada en sesiones anteriores
        prefs = contexto.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        puntuacionMaxima = prefs.getInt(KEY_RECORD, 0);

        configurarPaints();
    }

    /**
     * Configura los objetos Paint una sola vez al inicio. Crearlos dentro
     * del game loop es uno de los errores típicos que provocan tirones.
     */
    private void configurarPaints() {
        paintCielo.setColor(Color.rgb(135, 206, 235));   // Azul cielo

        paintTuberia.setColor(Color.rgb(76, 175, 80));   // Verde tubería
        paintBordeTuberia.setColor(Color.rgb(46, 125, 50));
        paintBordeTuberia.setStyle(Paint.Style.STROKE);
        paintBordeTuberia.setStrokeWidth(6);

        paintPajaro.setColor(Color.rgb(255, 235, 59));   // Amarillo
        paintPajaro.setAntiAlias(true);

        paintOjo.setColor(Color.BLACK);
        paintOjo.setAntiAlias(true);

        paintPico.setColor(Color.rgb(255, 152, 0));      // Naranja
        paintPico.setAntiAlias(true);

        paintSuelo.setColor(Color.rgb(222, 184, 135));   // Marrón claro

        paintTexto.setColor(Color.WHITE);
        paintTexto.setTextSize(60);
        paintTexto.setAntiAlias(true);
        paintTexto.setShadowLayer(4, 2, 2, Color.BLACK);
        paintTexto.setTextAlign(Paint.Align.CENTER);

        paintTextoGrande.setColor(Color.WHITE);
        paintTextoGrande.setTextSize(100);
        paintTextoGrande.setAntiAlias(true);
        paintTextoGrande.setShadowLayer(6, 3, 3, Color.BLACK);
        paintTextoGrande.setTextAlign(Paint.Align.CENTER);
    }

    // ----------------------------------------------------------------------
    // CALLBACKS DE LA SURFACE (requisito 5: ciclo de vida)
    // ----------------------------------------------------------------------

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // En el momento en que la superficie está lista, conocemos sus
        // dimensiones reales y podemos inicializar la posición del juego.
        anchoPantalla = getWidth();
        altoPantalla = getHeight();
        reiniciarJuego();
        iniciarHilo();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        anchoPantalla = width;
        altoPantalla = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Si la superficie se destruye, paramos el hilo de forma segura
        // antes de soltar la referencia. Sin esto tendríamos fuga de memoria
        // y posibles NullPointerExceptions al intentar dibujar.
        detenerHilo();
    }

    // ----------------------------------------------------------------------
    // GESTIÓN DEL HILO
    // ----------------------------------------------------------------------

    private void iniciarHilo() {
        if (hiloJuego == null || !hiloJuego.isAlive()) {
            corriendo = true;
            hiloJuego = new Thread(this);
            hiloJuego.start();
        }
    }

    private void detenerHilo() {
        corriendo = false;
        boolean detenido = false;
        while (!detenido) {
            try {
                if (hiloJuego != null) {
                    hiloJuego.join();   // Espera a que el hilo termine limpiamente
                }
                detenido = true;
            } catch (InterruptedException e) {
                // Reintentamos hasta lograr el join
            }
        }
        hiloJuego = null;
    }

    /** Llamado desde MainActivity.onPause(). */
    public void pausar() {
        detenerHilo();
    }

    /** Llamado desde MainActivity.onResume(). */
    public void reanudar() {
        iniciarHilo();
    }

    // ----------------------------------------------------------------------
    // GAME LOOP (requisito 2)
    // ----------------------------------------------------------------------

    /**
     * Bucle principal del juego. Se ejecuta en su propio hilo, totalmente
     * independiente del hilo de la UI. En cada iteración:
     *   1. Mide el tiempo de inicio del frame.
     *   2. Actualiza la lógica (física, colisiones, puntuación).
     *   3. Bloquea el Canvas y dibuja la escena.
     *   4. Duerme el tiempo restante para mantener ~60 FPS estables.
     */
    @Override
    public void run() {
        while (corriendo) {
            long inicioFrame = System.currentTimeMillis();

            if (!holder.getSurface().isValid()) continue;

            // 1) ACTUALIZAR LÓGICA
            actualizar();

            // 2) DIBUJAR (bloqueamos y liberamos el Canvas correctamente)
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    synchronized (holder) {
                        dibujar(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            // 3) CONTROL DE FPS: dormir el tiempo restante hasta los 16ms aprox.
            long tiempoFrame = System.currentTimeMillis() - inicioFrame;
            long espera = TIEMPO_FRAME_MS - tiempoFrame;
            if (espera > 0) {
                try {
                    Thread.sleep(espera);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // LÓGICA DE ACTUALIZACIÓN (FÍSICA Y COLISIONES)
    // ----------------------------------------------------------------------

    private void actualizar() {
        if (estado != Estado.JUGANDO) return;

        // FÍSICA DEL PÁJARO: aplicamos gravedad incrementando la velocidad vertical
        velocidadPajaro += GRAVEDAD;
        pajaroY += velocidadPajaro;

        // Construimos la bounding box del pájaro para las colisiones
        rectPajaro.set(
                (int) (pajaroX - RADIO_PAJARO),
                (int) (pajaroY - RADIO_PAJARO),
                (int) (pajaroX + RADIO_PAJARO),
                (int) (pajaroY + RADIO_PAJARO)
        );

        // COLISIÓN CON SUELO O TECHO
        if (pajaroY + RADIO_PAJARO >= altoPantalla - 100   // tocar suelo
                || pajaroY - RADIO_PAJARO <= 0) {           // tocar techo
            finalizarPartida();
            return;
        }

        // MOVIMIENTO Y GESTIÓN DE TUBERÍAS
        Iterator<Tuberia> iterador = tuberias.iterator();
        while (iterador.hasNext()) {
            Tuberia tub = iterador.next();
            tub.x -= VELOCIDAD_TUBERIAS;

            // COLISIONES (requisito 4: Rect.intersect)
            Rect superior = tub.getRectSuperior();
            Rect inferior = tub.getRectInferior();
            if (Rect.intersects(rectPajaro, superior)
                    || Rect.intersects(rectPajaro, inferior)) {
                finalizarPartida();
                return;
            }

            // PUNTUACIÓN: si el pájaro ha rebasado el centro de la tubería
            // y aún no la habíamos contado, sumamos un punto.
            if (!tub.puntuada && tub.x + ANCHO_TUBERIA < pajaroX) {
                tub.puntuada = true;
                puntuacion++;
            }

            // Eliminamos las tuberías que ya han salido por la izquierda
            if (tub.x + ANCHO_TUBERIA < 0) {
                iterador.remove();
            }
        }

        // GENERACIÓN DE NUEVAS TUBERÍAS: cuando la última no está demasiado cerca del borde
        if (tuberias.isEmpty()
                || tuberias.get(tuberias.size() - 1).x < anchoPantalla - DISTANCIA_ENTRE_TUBERIAS) {
            crearNuevaTuberia();
        }
    }

    /**
     * Crea un nuevo par de tuberías (superior+inferior) con un hueco en
     * posición vertical aleatoria pero respetando márgenes para que el
     * jugador siempre tenga espacio para pasar.
     */
    private void crearNuevaTuberia() {
        int margen = 150;
        int minY = margen;
        int maxY = altoPantalla - 100 - HUECO_TUBERIAS - margen;
        int huecoY = minY + (int) (Math.random() * (maxY - minY));
        tuberias.add(new Tuberia(anchoPantalla, huecoY, HUECO_TUBERIAS, ANCHO_TUBERIA, altoPantalla - 100));
    }

    /**
     * Maneja el final de una partida: actualiza el récord si procede
     * y cambia el estado del juego.
     */
    private void finalizarPartida() {
        estado = Estado.FIN;
        if (puntuacion > puntuacionMaxima) {
            puntuacionMaxima = puntuacion;
            // Guardamos el récord en SharedPreferences (extra del enunciado)
            prefs.edit().putInt(KEY_RECORD, puntuacionMaxima).apply();
        }
    }

    private void reiniciarJuego() {
        pajaroX = anchoPantalla / 4f;
        pajaroY = altoPantalla / 2f;
        velocidadPajaro = 0;
        tuberias = new ArrayList<>();
        puntuacion = 0;
        estado = Estado.INICIO;
    }

    // ----------------------------------------------------------------------
    // RENDERIZADO (DIBUJO)
    // ----------------------------------------------------------------------

    private void dibujar(Canvas canvas) {
        // Fondo (cielo)
        canvas.drawRect(0, 0, anchoPantalla, altoPantalla, paintCielo);

        // Tuberías
        for (Tuberia tub : tuberias) {
            // Superior
            canvas.drawRect(tub.x, 0, tub.x + ANCHO_TUBERIA, tub.huecoY, paintTuberia);
            canvas.drawRect(tub.x, 0, tub.x + ANCHO_TUBERIA, tub.huecoY, paintBordeTuberia);
            // Tapa decorativa de la tubería superior
            canvas.drawRect(tub.x - 10, tub.huecoY - 30, tub.x + ANCHO_TUBERIA + 10,
                    tub.huecoY, paintTuberia);

            // Inferior
            float yInferior = tub.huecoY + HUECO_TUBERIAS;
            canvas.drawRect(tub.x, yInferior, tub.x + ANCHO_TUBERIA, altoPantalla - 100, paintTuberia);
            canvas.drawRect(tub.x, yInferior, tub.x + ANCHO_TUBERIA, altoPantalla - 100, paintBordeTuberia);
            // Tapa decorativa de la tubería inferior
            canvas.drawRect(tub.x - 10, yInferior, tub.x + ANCHO_TUBERIA + 10,
                    yInferior + 30, paintTuberia);
        }

        // Suelo
        canvas.drawRect(0, altoPantalla - 100, anchoPantalla, altoPantalla, paintSuelo);

        // Pájaro (cuerpo + pico + ojo) usando primitivas geométricas
        canvas.drawCircle(pajaroX, pajaroY, RADIO_PAJARO, paintPajaro);
        canvas.drawCircle(pajaroX + 15, pajaroY - 10, 8, paintOjo);
        canvas.drawRect(pajaroX + RADIO_PAJARO - 5, pajaroY - 5,
                pajaroX + RADIO_PAJARO + 20, pajaroY + 10, paintPico);

        // HUD
        canvas.drawText("Puntos: " + puntuacion, anchoPantalla / 2f, 120, paintTexto);
        canvas.drawText("Récord: " + puntuacionMaxima, anchoPantalla / 2f, 200, paintTexto);

        // Mensajes según estado
        if (estado == Estado.INICIO) {
            canvas.drawText("TOCA PARA EMPEZAR", anchoPantalla / 2f,
                    altoPantalla / 2f - 100, paintTextoGrande);
        } else if (estado == Estado.FIN) {
            canvas.drawText("GAME OVER", anchoPantalla / 2f,
                    altoPantalla / 2f - 100, paintTextoGrande);
            canvas.drawText("Toca para reintentar", anchoPantalla / 2f,
                    altoPantalla / 2f, paintTexto);
        }
    }

    // ----------------------------------------------------------------------
    // EVENTOS TÁCTILES
    // ----------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            switch (estado) {
                case INICIO:
                    // Primer toque: empieza la partida
                    estado = Estado.JUGANDO;
                    velocidadPajaro = FUERZA_SALTO;
                    break;
                case JUGANDO:
                    // Cada toque hace que el pájaro "salte"
                    velocidadPajaro = FUERZA_SALTO;
                    break;
                case FIN:
                    // Reiniciamos para una nueva partida
                    reiniciarJuego();
                    break;
            }
        }
        return true;
    }
}
