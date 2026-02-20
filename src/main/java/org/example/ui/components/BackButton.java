package org.example.ui.components;

import org.example.ui.pages.AlternativePartnersPage;

import javax.swing.*;

import static org.example.ui.components.LabelTimer.showHttpResponseWithTimer;
import static org.example.utils.SharedData.*;

public class BackButton extends JButton {
    public BackButton() {
        super(LABEL_BACK);

        addActionListener(e -> {
            try {
                LOGGER.info(LOGGER_INFO_ALTERNATIVE_PARTNERS_PAGE);
                String httpResponse = httpRequestHandler.sendGetRequestAlternativePartners(true);
                showHttpResponseWithTimer(httpResponseLabelHeader, httpResponse);

                panelContainer.removeAll();
                panelContainer.add(new AlternativePartnersPage());
                panelContainer.revalidate();
                panelContainer.repaint();
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
        });
    }
}