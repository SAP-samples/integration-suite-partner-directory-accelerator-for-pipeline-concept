package org.example.ui.pages;

import org.example.model.AlternativePartner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Set;

import static org.example.ui.components.LabelTimer.showHttpResponseWithTimer;
import static org.example.utils.SharedData.*;
import static org.example.utils.SharedData.LOGGER;

public class MigrateDeprecatedEntriesPage extends JPanel {
    MigrateDeprecatedEntriesPage() {
        setLayout(new BorderLayout());

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);

        ButtonGroup buttonGroup = new ButtonGroup();

        ItemListener itemListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                JRadioButton selectedRadioButton = (JRadioButton) e.getSource();
                String selectedRadioButtonText = selectedRadioButton.getText();

                if (selectedRadioButtonText.equals(LABEL_MIGRATE_TILDE)) {
                    LOGGER.info("Migrate Tilde Page selected");

                    try {
                        List<String> uniquePidsWithReceiverDetermination = httpRequestHandler.sendGetRequestsPidsWithTilde();

                        MigrateTildePage migrateTildePage = new MigrateTildePage(uniquePidsWithReceiverDetermination);

                        cards.add(migrateTildePage, LABEL_MIGRATE_TILDE);
                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage());
                    }
                } else if (selectedRadioButtonText.equals(LABEL_MERGE_XSLTS)) {
                    LOGGER.info("Merge Page selected");

                    try {
                        String httpResponse = httpRequestHandler.sendGetRequestAlternativePartners(false);
                        Set<String> pidsWithInterfaceDetermination = httpRequestHandler.sendGetRequestBinaryParametersIdsInterfaceDetermination();

                        List<AlternativePartner> alternativePartnersMultipleXslts = currentAlternativePartnersList.stream()
                                .filter(partner -> pidsWithInterfaceDetermination.contains(partner.getPid()))
                                .toList();

                        List<String> pidsMultipleXslts = alternativePartnersMultipleXslts.stream()
                                .map(AlternativePartner::getPid)
                                .toList();

                        Set<String> pidsWithoutCombinedDetermination = httpRequestHandler.sendGetRequestBinaryParametersXsltsReceiverDetermination(pidsMultipleXslts);

                        alternativePartnersMultipleXslts = alternativePartnersMultipleXslts.stream()
                                .filter(partner -> pidsWithoutCombinedDetermination.contains(partner.getPid()))
                                .toList();

                        MergeXsltsPage mergeXsltsPage = new MergeXsltsPage(alternativePartnersMultipleXslts);
                        showHttpResponseWithTimer(httpResponseLabelHeader, httpResponse);

                        cards.add(mergeXsltsPage, LABEL_MERGE_XSLTS);
                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage());
                    }
                }

                cardLayout.show(cards, selectedRadioButtonText);
            }
        };

        JPanel panelRadioButtons = new JPanel();
        String[] labelsRadioButtons = {LABEL_MIGRATE_TILDE, LABEL_MERGE_XSLTS};
        boolean first = true;
        for (String labelRadioButton : labelsRadioButtons) {
            JRadioButton radioButton = new JRadioButton(labelRadioButton);
            buttonGroup.add(radioButton);
            radioButton.addItemListener(itemListener);
            panelRadioButtons.add(radioButton, BorderLayout.NORTH);
            if (first) {
                radioButton.setSelected(true);
                first = false;
            }
        }

        add(panelRadioButtons, BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);
    }
}
