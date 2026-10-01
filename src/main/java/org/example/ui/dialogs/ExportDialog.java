package org.example.ui.dialogs;

import org.example.model.AlternativePartner;
import org.example.ui.components.LoadingIcon;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.example.utils.SharedData.*;

public class ExportDialog extends JDialog {

    private final LoadingIcon loadingIcon = new LoadingIcon();

    public ExportDialog(JTable table, int counterSelected, boolean allSelected) {
        super(mainFrame, LABEL_EXPORT_ALTERNATIVE_PARTNERS, true);
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

        JCheckBox includeBinaryCheckBox = new JCheckBox();
        JCheckBox includeStringCheckBox = new JCheckBox();
        includeBinaryCheckBox.setSelected(true);
        includeStringCheckBox.setSelected(true);

        JLabel infoLabel = new JLabel(LABEL_EXPORT_1 + counterSelected + LABEL_EXPORT_2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        centerPanel.add(infoLabel, gbc);

        if (allSelected) {
            gbc.gridwidth = 1;
            gbc.gridx = 0;
            gbc.gridy = 1;
            JLabel binaryLabel = new JLabel(colon(LABEL_EXPORT_BINARY_PARAMETERS));
            centerPanel.add(binaryLabel, gbc);

            gbc.gridx = 1;
            centerPanel.add(includeBinaryCheckBox, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            JLabel stringLabel = new JLabel(colon(LABEL_EXPORT_STRING_PARAMETERS));
            centerPanel.add(stringLabel, gbc);

            gbc.gridx = 1;
            centerPanel.add(includeStringCheckBox, gbc);
        }

        add(centerPanel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton cancelButton = new JButton(LABEL_CANCEL);
        cancelButton.addActionListener(e -> dispose());
        southPanel.add(cancelButton);

        JButton exportButton = new JButton(LABEL_EXPORT_ALTERNATIVE_PARTNERS);
        exportButton.addActionListener(e -> {
            loadingIcon.startTimer();

            boolean includeBinary = !allSelected || includeBinaryCheckBox.isSelected();
            boolean includeString = !allSelected || includeStringCheckBox.isSelected();

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try {
                        List<AlternativePartner> selectedAlternativePartners = getSelectedAlternativePartners(table);
                        List<String> uniquePids = selectedAlternativePartners.stream()
                                .map(AlternativePartner::getPid)
                                .distinct()
                                .toList();

                        File outputDirectory = createOutputDirectory();

                        JSONObject alternativePartnersJson = httpRequestHandler.getAlternativePartnersToExport(uniquePids);
                        if (alternativePartnersJson != null) {
                            JSONObject filteredAlternativePartnersJson = filterAlternativePartnersBySelection(alternativePartnersJson, selectedAlternativePartners);
                            writeJsonToFile(filteredAlternativePartnersJson, new File(outputDirectory, API_ALTERNATIVE_PARTNERS + ".response.json"));
                        }

                        if (includeBinary) {
                            JSONObject binaryParametersJson = httpRequestHandler.getBinaryParametersToTransport(uniquePids);
                            if (binaryParametersJson != null) {
                                writeJsonToFile(binaryParametersJson, new File(outputDirectory, API_BINARY_PARAMETERS + ".response.json"));
                            }
                        }

                        if (includeString) {
                            JSONObject stringParametersJson = httpRequestHandler.getStringParametersToTransport(uniquePids);
                            if (stringParametersJson != null) {
                                writeJsonToFile(stringParametersJson, new File(outputDirectory, API_STRING_PARTNERS + ".response.json"));
                            }
                        }

                        dispose();
                        JOptionPane.showMessageDialog(mainFrame, LABEL_EXPORT_COMPLETED + outputDirectory.getAbsolutePath(), LABEL_SUCCESS, JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        LOGGER.error(ex);
                        JOptionPane.showMessageDialog(mainFrame, LABEL_ERROR_EXPORT_TRY_AGAIN, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                    }
                    return null;
                }
            };
            worker.execute();

        });
        southPanel.add(exportButton);

        southPanel.add(loadingIcon);

        add(southPanel, BorderLayout.SOUTH);

        setSize(UI_DIALOG_WIDTH, UI_DIALOG_HEIGHT);
        setLocationRelativeTo(mainFrame);
        setVisible(true);
    }

    private List<AlternativePartner> getSelectedAlternativePartners(JTable table) {
        List<AlternativePartner> selectedAlternativePartners = new ArrayList<>();

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
                        .ifPresent(selectedAlternativePartners::add);
            }
        }

        return selectedAlternativePartners;
    }

    private JSONObject filterAlternativePartnersBySelection(JSONObject alternativePartnersJson, List<AlternativePartner> selectedAlternativePartners) {
        JSONArray allResults = alternativePartnersJson.getJSONObject(JSON_KEY_D).getJSONArray(JSON_KEY_RESULTS);
        JSONArray filteredResults = new JSONArray();

        for (int i = 0; i < allResults.length(); i++) {
            JSONObject resultObject = allResults.getJSONObject(i);

            String agency = resultObject.optString(JSON_KEY_AGENCY);
            String scheme = resultObject.optString(JSON_KEY_SCHEME);
            String id = resultObject.optString(JSON_KEY_ID);
            String pid = resultObject.optString(JSON_KEY_PID);

            boolean isSelected = selectedAlternativePartners.stream()
                    .anyMatch(partner -> partner.getAgency().equals(agency)
                            && partner.getScheme().equals(scheme)
                            && partner.getId().equals(id)
                            && partner.getPid().equals(pid));

            if (isSelected) {
                filteredResults.put(resultObject);
            }
        }

        JSONObject dObject = new JSONObject();
        dObject.put(JSON_KEY_RESULTS, filteredResults);

        JSONObject rootObject = new JSONObject();
        rootObject.put(JSON_KEY_D, dObject);
        return rootObject;
    }

    private File createOutputDirectory() {
        File tenantsFile = new File(TENANTS_FILE_NAME).getAbsoluteFile();
        File baseDirectory = tenantsFile.getParentFile() != null ? tenantsFile.getParentFile() : new File(".").getAbsoluteFile();

        File exportRootDirectory = new File(baseDirectory, "export");

        String safeTenantName = currentTenantName == null ? "tenant" : currentTenantName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File outputDirectory = new File(exportRootDirectory, safeTenantName + "_" + timestamp);

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        return outputDirectory;
    }

    private void writeJsonToFile(JSONObject jsonObject, File file) throws Exception {
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(jsonObject.toString(4));
            fileWriter.flush();
        }
    }
}

