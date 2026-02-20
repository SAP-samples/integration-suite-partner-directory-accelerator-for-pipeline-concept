package org.example.ui.dialogs;

import org.example.model.AlternativePartner;
import org.example.model.AlternativePartnerMigrateTilde;
import org.example.ui.components.LoadingIcon;
import org.json.JSONObject;

import javax.swing.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.example.utils.SharedData.*;

public class MigrateTildeDialog extends JDialog {

    private final LoadingIcon loadingIcon = new LoadingIcon();

    public MigrateTildeDialog(List<AlternativePartnerMigrateTilde> listAlternativePartnerMigrateTilde, int counterSelected) {
        super(mainFrame, LABEL_MIGRATE_TILDE, true);
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel deleteLabel = new JLabel(colon(LABEL_DELETE_OLD_ENTRIES));
        centerPanel.add(deleteLabel, gbc);
        gbc.gridx = 1;
        JCheckBox shouldDeleteEntriesCheckBox = new JCheckBox();
        centerPanel.add(shouldDeleteEntriesCheckBox, gbc);

        add(centerPanel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton cancelButton = new JButton(LABEL_CANCEL);
        cancelButton.addActionListener(e -> dispose());
        southPanel.add(cancelButton);

        JButton migrateButton = new JButton(LABEL_CONFIRM_MIGRATION_1 + counterSelected + LABEL_CONFIRM_MIGRATION_2);
        migrateButton.addActionListener(e -> {
            loadingIcon.startTimer();

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    HashMap<String, String> oldAndNewPids = new HashMap<>();
                    List<AlternativePartner> alternativePartnersToMigrate = new ArrayList<>();

                    boolean shouldDeleteOldEntries = shouldDeleteEntriesCheckBox.isSelected();
                    List<String> migrationErrors = new ArrayList<>();

                    for (AlternativePartnerMigrateTilde alternativePartnerMigrateTilde : listAlternativePartnerMigrateTilde) {
                        String newPid = alternativePartnerMigrateTilde.getNewPid();
                        if (newPid != null && !newPid.isEmpty()) {
                            String oldPid = alternativePartnerMigrateTilde.getOldPid();
                            oldAndNewPids.put(oldPid, newPid);

                            alternativePartnersToMigrate.add(new AlternativePartner(alternativePartnerMigrateTilde.getAgency(), SCHEME_SENDER_INTERFACE, alternativePartnerMigrateTilde.getId(), newPid));
                        }
                    }

                    httpRequestHandler.transportAlternativePartners(alternativePartnersToMigrate, false, migrationErrors);

                    List<String> oldPids = new ArrayList<>(oldAndNewPids.keySet());

                    // Binary Parameters
                    JSONObject jsonBinaryParametersToTransport = httpRequestHandler.getBinaryParametersToTransport(oldPids);
                    if (jsonBinaryParametersToTransport != null) {
                        httpRequestHandler.transportBinaryParameters(jsonBinaryParametersToTransport, false, migrationErrors, shouldDeleteOldEntries, oldAndNewPids);
                    }

                    // String Parameters
                    JSONObject jsonStringParametersToTransport = httpRequestHandler.getStringParametersToTransport(oldPids);
                    if (jsonStringParametersToTransport != null) {
                        httpRequestHandler.transportStringParameters(jsonStringParametersToTransport, false, migrationErrors, shouldDeleteOldEntries, oldAndNewPids);
                    }

                    dispose();

                    LOGGER.info(LOGGER_INFO_ALTERNATIVE_PARTNERS_PAGE);
                    mainFrame.getAndShowLatestAlternativePartners();

                    if (migrationErrors.isEmpty()) {
                        String logTransport = LABEL_TRANSPORT_FINISHED + LABEL_TRANSPORT_SUCCESSFUL;
                        JOptionPane.showMessageDialog(mainFrame, logTransport, LABEL_SUCCESS, JOptionPane.INFORMATION_MESSAGE);
                        LOGGER.info(logTransport);
                    } else {
                        String logTransport = LABEL_TRANSPORT_FINISHED + LABEL_TRANSPORT_FAILED_1 + migrationErrors.size() + LABEL_TRANSPORT_FAILED_2;
                        JOptionPane.showMessageDialog(mainFrame, logTransport, LABEL_WARNING, JOptionPane.WARNING_MESSAGE);
                        LOGGER.warn(logTransport);
                    }

                    return null;
                }
            };
            worker.execute();

        });
        southPanel.add(migrateButton);

        southPanel.add(loadingIcon);

        add(southPanel, BorderLayout.SOUTH);

        setSize(UI_DIALOG_WIDTH, UI_DIALOG_HEIGHT);
        setLocationRelativeTo(mainFrame);
        setVisible(true);
    }
}
