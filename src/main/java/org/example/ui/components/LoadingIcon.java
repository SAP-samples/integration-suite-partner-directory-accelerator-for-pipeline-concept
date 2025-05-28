package org.example.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;

public class LoadingIcon extends JPanel {

    private double angle;
    private double extent;

    private final double angleDelta = -1;
    private final double extentDelta = -5;

    private boolean flip = false;

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(40, 40);
    }

    public void startTimer() {
        Timer timer = new Timer(10, e -> {
            angle += angleDelta;
            extent += extentDelta;
            if (Math.abs(extent) % 360.0 == 0) {
                angle = angle - extent;
                flip = !flip;
                if (flip) {
                    extent = 360.0;
                } else {
                    extent = 0.0;
                }
            }
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Arc2D.Double arc = new Arc2D.Double(10, 10, 20, 20, angle, extent, Arc2D.OPEN);
        BasicStroke stroke = new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
        g2d.setStroke(stroke);
        g2d.setColor(Color.BLACK);
        g2d.draw(arc);
        g2d.dispose();
    }
}
