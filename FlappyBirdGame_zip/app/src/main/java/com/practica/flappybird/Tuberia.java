package com.practica.flappybird;

import android.graphics.Rect;

/**
 * Modelo de datos de un par de tuberías (superior + inferior).
 *
 * Se ha extraído a una clase aparte para mantener GameView más legible
 * y porque encapsula la lógica de su bounding box para colisiones
 * (requisito 4 del enunciado: detección mediante Rect.intersect()).
 */
public class Tuberia {

    /** Posición horizontal (cambia en cada frame del game loop). */
    public float x;

    /** Posición Y donde empieza el hueco entre la tubería superior y la inferior. */
    public final int huecoY;

    /** Tamaño vertical del hueco por el que debe pasar el pájaro. */
    public final int alturaHueco;

    /** Ancho de la tubería. */
    public final int ancho;

    /** Posición Y del suelo (necesaria para calcular la altura de la tubería inferior). */
    public final int ySuelo;

    /** Flag que indica si esta tubería ya contó como punto. */
    public boolean puntuada = false;

    /** Rectángulos reutilizables (evitamos crear objetos cada frame). */
    private final Rect rectSuperior = new Rect();
    private final Rect rectInferior = new Rect();

    public Tuberia(int xInicial, int huecoY, int alturaHueco, int ancho, int ySuelo) {
        this.x = xInicial;
        this.huecoY = huecoY;
        this.alturaHueco = alturaHueco;
        this.ancho = ancho;
        this.ySuelo = ySuelo;
    }

    /**
     * Devuelve la "caja envolvente" (bounding box) de la parte superior.
     * Va desde la parte alta de la pantalla (y=0) hasta donde empieza el hueco.
     */
    public Rect getRectSuperior() {
        rectSuperior.set((int) x, 0, (int) x + ancho, huecoY);
        return rectSuperior;
    }

    /**
     * Devuelve la "caja envolvente" de la parte inferior.
     * Va desde donde acaba el hueco hasta el suelo.
     */
    public Rect getRectInferior() {
        rectInferior.set((int) x, huecoY + alturaHueco, (int) x + ancho, ySuelo);
        return rectInferior;
    }
}
