package org.example.ui.dialogs;

import org.example.api.HttpRequestHandler;
import org.example.model.AlternativePartner;
import org.example.model.BinaryParameter;
import org.example.ui.components.LoadingIcon;
import org.example.utils.TenantCredentials;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;

import java.awt.*;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.example.utils.SharedData.*;

public class TransportDialog extends JDialog {

    private final LoadingIcon loadingIcon = new LoadingIcon();

    public TransportDialog(JTable table, int counterSelected) {
        super(mainFrame, LABEL_TRANSPORT_ALTERNATIVE_PARTNERS, true);
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
            String sourceTenantName = currentTenantName;

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    List<AlternativePartner> alternativePartnersToTransport = new ArrayList<>();
                    for (int i = 0; i < table.getModel().getRowCount(); i++) {
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
                    List<String> scenarioPidsToTransport = new ArrayList<>(uniquePidsToTransport);

                    String selectedTenantName = (String) tenantDropdown.getSelectedItem();

                    boolean overwrite = overwriteCheckBox.isSelected();
                    boolean includeLandscape = landscapeCheckBox.isSelected();

                    List<String> transportErrors = new ArrayList<>();

                    LOGGER.info(LABEL_TRANSPORT_START_1 + "{}" + LABEL_TRANSPORT_START_2 + "\"{}\".", counterSelected, selectedTenantName);

                    try {
                        TenantCredentials selectedTenant = tenantCredentialsList.stream()
                                .filter(tenant -> tenant.getName().equalsIgnoreCase(selectedTenantName))
                                .findFirst()
                                .orElse(null);

                        if (selectedTenant == null) {
                            JOptionPane.showMessageDialog(mainFrame, LABEL_ERROR_SELECTED_TENANT_NOT_FOUND, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                        }

                        HttpRequestHandler httpTransportHandler = new HttpRequestHandler(selectedTenant);

                        httpTransportHandler.transportAlternativePartners(alternativePartnersToTransport, overwrite, transportErrors);

                        // Binary Parameters
                        JSONObject jsonBinaryParametersToTransport = httpRequestHandler.getBinaryParametersToTransport(uniquePidsToTransport);
                        if (jsonBinaryParametersToTransport != null) {
                            httpTransportHandler.transportBinaryParameters(jsonBinaryParametersToTransport, overwrite, transportErrors, false, null);
                        }

                        // String Parameters
                        if (includeLandscape) {
                            uniquePidsToTransport.add(STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE);
                        }
                        JSONObject jsonStringParametersToTransport = httpRequestHandler.getStringParametersToTransport(uniquePidsToTransport);
                        if (jsonStringParametersToTransport != null) {
                            httpTransportHandler.transportStringParameters(jsonStringParametersToTransport, overwrite, transportErrors, false, null);
                        }

                        // Receiver-level default queues (tab: Sender / Receiver System Settings)
                        Set<String> receiversToTransport = extractReceiversForScenarios(
                                scenarioPidsToTransport,
                                jsonBinaryParametersToTransport,
                                jsonStringParametersToTransport);
                        JSONObject jsonReceiverQueuesToTransport = httpRequestHandler.getReceiverSpecificDefaultQueuesToTransport(receiversToTransport);
                        if (jsonReceiverQueuesToTransport != null) {
                            httpTransportHandler.transportStringParameters(jsonReceiverQueuesToTransport, overwrite, transportErrors, false, null);
                        }

                        if (transportErrors.isEmpty()) {
                            mainFrame.setSelectedTenant(sourceTenantName);
                            dispose();

                            String logTransport = LABEL_TRANSPORT_FINISHED + LABEL_TRANSPORT_SUCCESSFUL;
                            JOptionPane.showMessageDialog(mainFrame, logTransport, LABEL_SUCCESS, JOptionPane.INFORMATION_MESSAGE);
                            LOGGER.info(logTransport);
                        } else {
                            dispose();

                            Set<String> uniqueTransportErrors = new LinkedHashSet<>(transportErrors);
                            String logTransport = LABEL_TRANSPORT_FINISHED + LABEL_TRANSPORT_FAILED_1 + uniqueTransportErrors.size() + LABEL_TRANSPORT_FAILED_2;
                            JTextArea errorTextArea = new JTextArea(String.join("\n\n", uniqueTransportErrors));
                            errorTextArea.setEditable(false);
                            errorTextArea.setLineWrap(true);
                            errorTextArea.setWrapStyleWord(true);
                            errorTextArea.setCaretPosition(0);

                            JScrollPane errorScrollPane = new JScrollPane(errorTextArea);
                            errorScrollPane.setPreferredSize(new Dimension(UI_DIALOG_WIDTH * 2, UI_DIALOG_HEIGHT * 2));

                            JOptionPane.showMessageDialog(mainFrame,
                                    new Object[]{logTransport, errorScrollPane},
                                    LABEL_WARNING,
                                    JOptionPane.WARNING_MESSAGE);
                            LOGGER.warn(logTransport);
                        }
                    } catch (Exception ex) {
                        LOGGER.error(ex);
                        dispose();
                        JOptionPane.showMessageDialog(mainFrame, LABEL_ERROR_TRANSPORT_TRY_AGAIN, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                    }
                    return null;
                }
            };
            worker.execute();

        });
        southPanel.add(transportButton);

        southPanel.add(loadingIcon);

        add(southPanel, BorderLayout.SOUTH);

        setSize(UI_DIALOG_WIDTH, UI_DIALOG_HEIGHT);
        setLocationRelativeTo(mainFrame);
        setVisible(true);
    }

    private Set<String> extractReceiversForScenarios(List<String> scenarioPids,
                                                     JSONObject binaryParameters,
                                                     JSONObject stringParameters) {
        Set<String> receiverNames = new LinkedHashSet<>();
        Set<String> scenarioPidSet = new HashSet<>(scenarioPids);

        if (binaryParameters != null) {
            JSONArray binaryResults = binaryParameters.getJSONObject(JSON_KEY_D).getJSONArray(JSON_KEY_RESULTS);
            for (int i = 0; i < binaryResults.length(); i++) {
                JSONObject result = binaryResults.getJSONObject(i);
                String pid = result.optString(JSON_KEY_PID, "");
                String id = result.optString(JSON_KEY_ID, "");

                if (!scenarioPidSet.contains(pid) || !ID_RECEIVER_DETERMINATION.equals(id)) {
                    continue;
                }

                String valueEncoded = result.optString(JSON_KEY_VALUE, "");
                try {
                    String xslt = new String(Base64.getDecoder().decode(valueEncoded));
                    receiverNames.addAll(new BinaryParameter(pid, id, JSON_VALUE_XSL, xslt).getSetReceiverNames());
                } catch (Exception e) {
                    LOGGER.warn("Could not parse receiver determination XSLT for pid {}.", pid);
                }
            }
        }

        if (stringParameters != null) {
            JSONArray stringResults = stringParameters.getJSONObject(JSON_KEY_D).getJSONArray(JSON_KEY_RESULTS);
            for (int i = 0; i < stringResults.length(); i++) {
                JSONObject result = stringResults.getJSONObject(i);
                String pid = result.optString(JSON_KEY_PID, "");
                String id = result.optString(JSON_KEY_ID, "");

                if (!scenarioPidSet.contains(pid)) {
                    continue;
                }

                if (ID_RECEIVER_DETERMINATION.equals(id)) {
                    String receiver = result.optString(JSON_KEY_VALUE, "").trim();
                    if (!receiver.isEmpty()) {
                        receiverNames.add(receiver);
                    }
                } else if (id.startsWith(STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE + "_")) {
                    String receiver = id.substring((STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE + "_").length()).trim();
                    if (!receiver.isEmpty()) {
                        receiverNames.add(receiver);
                    }
                }
            }
        }

        return receiverNames;
    }
}
