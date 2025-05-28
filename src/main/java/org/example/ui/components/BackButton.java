package org.example.ui.components;

import org.example.ui.pages.AlternativePartnersPage;

import javax.swing.*;

import static org.example.ui.components.LabelTimer.showHttpResponseWithTimer;
import static org.example.utils.SharedData.*;

public class BackButton extends JButton {
    public BackButton(JFrame parentFrame) {
        super(LABEL_BACK);

        addActionListener(e -> {
            try {
                String httpResponse = httpRequestHandler.sendGetRequestAlternativePartners();
                showHttpResponseWithTimer(httpResponseLabelHeader, httpResponse);

                panelContainer.removeAll();
                panelContainer.add(new AlternativePartnersPage(parentFrame));
                panelContainer.revalidate();
                panelContainer.repaint();
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
        });
    }
}