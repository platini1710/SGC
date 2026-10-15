package com.example.schedule.config;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Genera el logo del colegio como data URI PNG (Base64) en tiempo de ejecución.
 * Se genera en memoria con java.awt para garantizar un PNG válido (evita el
 * riesgo de corromper un Base64 pegado a mano). Escudo "LS" azul.
 */
final class ColegioLogo {

    private ColegioLogo() {}

    static String generarDataUri() {
        int w = 140, h = 140;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Escudo (polígono) con gradiente azul.
        Polygon escudo = new Polygon(
                new int[]{70, 128, 128, 70, 12, 12},
                new int[]{8, 30, 72, 132, 72, 30},
                6);
        g.setPaint(new GradientPaint(0, 0, new Color(21, 101, 192), 0, h, new Color(13, 71, 161)));
        g.fillPolygon(escudo);

        // Texto "LS" centrado en blanco.
        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD, 48));
        String txt = "LS";
        var fm = g.getFontMetrics();
        int tx = (w - fm.stringWidth(txt)) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(txt, tx, ty);
        g.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            String b64 = Base64.getEncoder().encodeToString(out.toByteArray());
            return "data:image/png;base64," + b64;
        } catch (Exception e) {
            return null;
        }
    }
}
