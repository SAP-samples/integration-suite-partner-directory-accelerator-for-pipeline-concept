package org.example.ui.components;

import javax.swing.*;

import static org.example.utils.SharedData.*;

public class LabelTimer {
    public static void showHttpResponseWithTimer(JLabel jLabel, String httpResponse) {
        jLabel.setText(httpResponse);
        jLabel.setVisible(true);
        try {
            if (httpResponse.startsWith(LABEL_HTTP_ERROR)) {
                jLabel.setForeground(RED);
            } else if (httpResponse.startsWith(LABEL_HTTP_SUCCESS)) {
                jLabel.setForeground(GREEN);
            } else if (httpResponse.startsWith(LABEL_HTTP_WARNING)) {
                jLabel.setForeground(ORANGE);
            }
        } catch (Exception e) {
            LOGGER.warn(e);
        }

        Timer timer = new Timer(3000, e -> jLabel.setVisible(false));
        timer.setRepeats(false);
        timer.start();
    }
}
