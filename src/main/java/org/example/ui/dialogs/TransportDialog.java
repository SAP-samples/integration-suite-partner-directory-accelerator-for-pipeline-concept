package org.example.ui.dialogs;

import org.example.api.HttpRequestHandler;
import org.example.model.AlternativePartner;
import org.example.ui.MainFrame;
import org.example.ui.components.LoadingIcon;
import org.example.utils.TenantCredentials;
import org.json.JSONObject;

import javax.swing.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.example.utils.SharedData.*;

public class TransportDialog extends JDialog {

    private final LoadingIcon loadingIcon = new LoadingIcon();

    public TransportDialog(JFrame parent, JTable table, int counterSelected) {
        super(parent, LABEL_TRANSPORT_ALTERNATIVE_PARTNERS, true);
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

        JLabel selectTenantLabel = new JLabel(colonAsterisk(LABEL_SELECT_TENANT_TO_TRANSPORT));
        centerPanel.add(selectTenantLabel);

        DefaultComboBoxModel<String> dropdownModel = new DefaultComboBoxModel<>();
        JComboBox<String> tenantDropdown = new JComboBox<>(dropdownModel);
        for (TenantCredentials tenant : tenantCredentialsList) {
            if (!Objects.equals(tenant.getName(), currentTenantName)) {
                tenantDropdown.addItem(tenant.getName());
            }
        }

        gbc.gridx = 0;
        gbc.gridy = 0;
        centerPanel.add(selectTenantLabel, gbc);

        gbc.gridx = 1;
        centerPanel.add(tenantDropdown, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel overwriteLabel = new JLabel(colon(LABEL_OVERWRITE_EXISTING_ENTRIES));
        centerPanel.add(overwriteLabel, gbc);
        gbc.gridx = 1;
        JCheckBox overwriteCheckBox = new JCheckBox();
        centerPanel.add(overwriteCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel landscapeLabel = new JLabel(colon("Include " + STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE));
        centerPanel.add(landscapeLabel, gbc);
        gbc.gridx = 1;
        JCheckBox landscapeCheckBox = new JCheckBox();
        centerPanel.add(landscapeCheckBox, gbc);

        add(centerPanel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton cancelButton = new JButton(LABEL_CANCEL);
        cancelButton.addActionListener(e -> dispose());
        southPanel.add(cancelButton);

        JButton transportButton = new JButton(LABEL_TRANSPORT_1 + counterSelected + LABEL_TRANSPORT_2);
        transportButton.addActionListener(e -> {
            loadingIcon.startTimer();

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    List<AlternativePartner> alternativePartnersToTransport = new ArrayList<>();
                    for (int i = 0; i < table.getRowCount(); i++) {
                        Boolean isChecked = (Boolean) table.getValueAt(i, 0);
                        if (isChecked) {
                            String agency = (String) table.getValueAt(i, 1);
                            String scheme = (String) table.getValueAt(i, 2);
                            String id = (String) table.getValueAt(i, 3);
                            String pid = (String) table.getValueAt(i, 4);

                            currentAlternativePartnersList.stream()
                                    .filter(obj -> obj.getAgency().equals(agency)
                                            && obj.getScheme().equals(scheme)
                                            && obj.getId().equals(id)
                                            && obj.getPid().equals(pid))
                                    .findFirst()
                                    .ifPresent(alternativePartnersToTransport::add);
                        }
                    }

                    List<String> uniquePidsToTransport = new ArrayList<>(alternativePartnersToTransport.stream()
                            .map(AlternativePartner::getPid)
                            .distinct()
                            .toList());

                    String selectedTenantName = (String) tenantDropdown.getSelectedItem();

                    boolean overwrite = overwriteCheckBox.isSelected();
                    boolean includeLLandscape = landscapeCheckBox.isSelected();

                    List<String> transportErrors = new ArrayList<>();

                    LOGGER.info(LABEL_TRANSPORT_START_1 + "{}" + LABEL_TRANSPORT_START_2 + "\"{}\".", counterSelected, selectedTenantName);

                    try {
                        TenantCredentials selectedTenant = tenantCredentialsList.stream()
                                .filter(tenant -> tenant.getName().equalsIgnoreCase(selectedTenantName))
                                .findFirst()
                                .orElse(null);

                        if (selectedTenant == null) {
                            JOptionPane.showMessageDialog(parent, LABEL_ERROR_SELECTED_TENANT_NOT_FOUND, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                        }

                        HttpRequestHandler httpTransportHandler = new HttpRequestHandler(selectedTenant);

                        httpTransportHandler.transportAlternativePartners(alternativePartnersToTransport, overwrite, transportErrors);

                        JSONObject jsonBinaryParametersToTransport = httpRequestHandler.getBinaryParametersToTransport(uniquePidsToTransport);
                        if (jsonBinaryParametersToTransport != null) {
                            httpTransportHandler.transportBinaryParameters(jsonBinaryParametersToTransport, overwrite, transportErrors);
                        }

                        if (includeLLandscape) {
                            uniquePidsToTransport.add(STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE);
                        }
                        JSONObject jsonStringParametersToTransport = httpRequestHandler.getStringParametersToTransport(uniquePidsToTransport);
                        if (jsonStringParametersToTransport != null) {
                            httpTransportHandler.transportStringParameters(jsonStringParametersToTransport, overwrite, transportErrors);
                        }

                        ((MainFrame) parent).setSelectedTenant(selectedTenantName);
                        dispose();

                        if (transportErrors.isEmpty()) {
                            String logTransport = LABEL_TRANSPORT_FINISHED + LABEL_TRANSPORT_SUCCESSFUL;
                            JOptionPane.showMessageDialog(parent, logTransport, LABEL_SUCCESS, JOptionPane.INFORMATION_MESSAGE);
                            LOGGER.info(logTransport);
                        } else {
                            String logTransport = LABEL_TRANSPORT_FINISHED + LABEL_TRANSPORT_FAILED_1 + transportErrors.size() + LABEL_TRANSPORT_FAILED_2;
                            JOptionPane.showMessageDialog(parent, logTransport, LABEL_WARNING, JOptionPane.WARNING_MESSAGE);
                            LOGGER.warn(logTransport);
                        }
                    } catch (Exception ex) {
                        LOGGER.error(ex);
                        JOptionPane.showMessageDialog(parent, LABEL_ERROR_TRANSPORT_TRY_AGAIN, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                    }
                    return null;
                }
            };
            worker.execute();

        });
        southPanel.add(transportButton);

        southPanel.add(loadingIcon);

        add(southPanel, BorderLayout.SOUTH);

        setSize(800, 500);
        setLocationRelativeTo(parent);
        setVisible(true);
    }
}
