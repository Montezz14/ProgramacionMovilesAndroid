package com.practica.flappybird;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

/**
 * Activity principal del juego.
 *
 * Su única responsabilidad es lanzar el GameView (que contiene SurfaceView)
 * a pantalla completa y sin barra de título, tal como exige el enunciado:
 * "No se debe usar un layout XML complejo para el juego".
 *
 * También delega los eventos del ciclo de vida (onPause / onResume) a la
 * vista del juego para que el hilo se detenga y reanude correctamente,
 * evitando así fugas de memoria (requisito 5 del enunciado).
 */
public class MainActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Quitamos la barra de título para tener toda la pantalla disponible
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Modo pantalla completa (sin barra de estado)
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Creamos la vista del juego y la asignamos como contenido principal.
        // No usamos setContentView con un XML, sino que pasamos directamente
        // nuestra SurfaceView personalizada.
        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Detenemos el hilo del juego cuando la app pasa a segundo plano
        gameView.pausar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reanudamos el hilo cuando la app vuelve al primer plano
        gameView.reanudar();
    }
}
