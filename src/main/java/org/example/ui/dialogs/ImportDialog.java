package org.example.ui.dialogs;

import org.example.model.AlternativePartner;
import org.example.ui.components.LoadingIcon;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static org.example.utils.SharedData.*;

public class ImportDialog extends JDialog {

    private final LoadingIcon loadingIcon = new LoadingIcon();
    private enum ResourceType { ALTERNATIVE_PARTNERS, BINARY_PARAMETERS, STRING_PARAMETERS, UNKNOWN }

    public ImportDialog(File[] files) {
        super(mainFrame, LABEL_IMPORT_PARTNER_DIRECTORY, true);
        setLayout(new BorderLayout());

        List<File> alternativePartnersFiles = new ArrayList<>();
        List<File> binaryParametersFiles = new ArrayList<>();
        List<File> stringParametersFiles = new ArrayList<>();

        for (File file : files) {
            ResourceType detectedType = detectResourceType(file);
            if (detectedType == ResourceType.ALTERNATIVE_PARTNERS) {
                alternativePartnersFiles.add(file);
            } else if (detectedType == ResourceType.BINARY_PARAMETERS) {
                binaryParametersFiles.add(file);
            } else if (detectedType == ResourceType.STRING_PARAMETERS) {
                stringParametersFiles.add(file);
            }
        }

        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

        int gridy = 0;

        gbc.gridx = 0;
        gbc.gridy = gridy++;
        gbc.gridwidth = 2;
        centerPanel.add(new JLabel(LABEL_IMPORT_FILES_DETECTED), gbc);
        gbc.gridwidth = 1;

        for (File file : alternativePartnersFiles) {
            gbc.gridx = 0;
            gbc.gridy = gridy++;
            centerPanel.add(new JLabel("✓ " + file.getName()), gbc);
        }

        for (File file : binaryParametersFiles) {
            gbc.gridx = 0;
            gbc.gridy = gridy++;
            centerPanel.add(new JLabel("✓ " + file.getName()), gbc);
        }

        for (File file : stringParametersFiles) {
            gbc.gridx = 0;
            gbc.gridy = gridy++;
            centerPanel.add(new JLabel("✓ " + file.getName()), gbc);
        }

        boolean hasSupportedFiles = !alternativePartnersFiles.isEmpty() || !binaryParametersFiles.isEmpty() || !stringParametersFiles.isEmpty();
        if (!hasSupportedFiles) {
            gbc.gridx = 0;
            gbc.gridy = gridy++;
            gbc.gridwidth = 2;
            centerPanel.add(new JLabel(LABEL_IMPORT_NO_SUPPORTED_FILES), gbc);
            gbc.gridwidth = 1;
        }

        gbc.gridx = 0;
        gbc.gridy = gridy++;
        centerPanel.add(new JLabel(colon(LABEL_OVERWRITE_EXISTING_ENTRIES)), gbc);
        gbc.gridx = 1;
        JCheckBox overwriteCheckBox = new JCheckBox();
        centerPanel.add(overwriteCheckBox, gbc);

        add(centerPanel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton cancelButton = new JButton(LABEL_CANCEL);
        cancelButton.addActionListener(e -> dispose());
        southPanel.add(cancelButton);

        List<File> finalAlternativePartnersFiles = alternativePartnersFiles;
        List<File> finalBinaryParametersFiles = binaryParametersFiles;
        List<File> finalStringParametersFiles = stringParametersFiles;

        JButton importButton = new JButton(LABEL_IMPORT_START);
        importButton.setEnabled(hasSupportedFiles);
        importButton.addActionListener(e -> {
            loadingIcon.startTimer();
            boolean overwrite = overwriteCheckBox.isSelected();

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    List<String> importErrors = new ArrayList<>();

                    try {
                        if (!finalAlternativePartnersFiles.isEmpty()) {
                            List<AlternativePartner> alternativePartners = new ArrayList<>();
                            for (File file : finalAlternativePartnersFiles) {
                                JSONObject json = readJsonFile(file);
                                alternativePartners.addAll(parseAlternativePartnersFromJson(json));
                            }
                            LOGGER.info("Importing {} alternative partner(s).", alternativePartners.size());
                            httpRequestHandler.transportAlternativePartners(alternativePartners, overwrite, importErrors);
                        }

                        for (File file : finalBinaryParametersFiles) {
                            JSONObject json = readJsonFile(file);
                            httpRequestHandler.transportBinaryParameters(json, overwrite, importErrors, false, null);
                        }

                        for (File file : finalStringParametersFiles) {
                            JSONObject json = readJsonFile(file);
                            httpRequestHandler.transportStringParameters(json, overwrite, importErrors, false, null);
                        }

                        // Reload from API so the table reflects newly imported entries immediately.
                        httpRequestHandler.sendGetRequestAlternativePartners(true);
                        if (alternativePartnersPage != null) {
                            SwingUtilities.invokeLater(() -> {
                                alternativePartnersPage.refreshTableData(currentAlternativePartnersList);
                                alternativePartnersPage.revalidate();
                                alternativePartnersPage.repaint();
                            });
                        }

                        dispose();

                        if (importErrors.isEmpty()) {
                            String msg = LABEL_IMPORT_FINISHED + LABEL_IMPORT_SUCCESSFUL;
                            JOptionPane.showMessageDialog(mainFrame, msg, LABEL_SUCCESS, JOptionPane.INFORMATION_MESSAGE);
                            LOGGER.info(msg);
                        } else {
                            String msg = LABEL_IMPORT_FINISHED + LABEL_IMPORT_FAILED_1 + importErrors.size() + LABEL_IMPORT_FAILED_2;
                            JOptionPane.showMessageDialog(mainFrame, msg, LABEL_WARNING, JOptionPane.WARNING_MESSAGE);
                            LOGGER.warn(msg);
                        }
                    } catch (Exception ex) {
                        LOGGER.error(ex);
                        JOptionPane.showMessageDialog(mainFrame, LABEL_ERROR_IMPORT_TRY_AGAIN, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                    }
                    return null;
                }
            };
            worker.execute();
        });
        southPanel.add(importButton);
        southPanel.add(loadingIcon);

        add(southPanel, BorderLayout.SOUTH);

        setSize(UI_DIALOG_WIDTH, UI_DIALOG_HEIGHT);
        setLocationRelativeTo(mainFrame);
        setVisible(true);
    }

    private JSONObject readJsonFile(File file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return new JSONObject(content.toString());
    }

    private List<AlternativePartner> parseAlternativePartnersFromJson(JSONObject json) {
        List<AlternativePartner> list = new ArrayList<>();
        JSONArray results = json.getJSONObject(JSON_KEY_D).getJSONArray(JSON_KEY_RESULTS);
        for (int i = 0; i < results.length(); i++) {
            JSONObject obj = results.getJSONObject(i);
            String agency = obj.optString(JSON_KEY_AGENCY);
            String scheme = obj.optString(JSON_KEY_SCHEME);
            String id = obj.optString(JSON_KEY_ID);
            String pid = obj.optString(JSON_KEY_PID);
            list.add(new AlternativePartner(agency, scheme, id, pid));
        }
        return list;
    }

    private ResourceType detectResourceType(File file) {
        String name = file.getName();
        if (name.contains(API_ALTERNATIVE_PARTNERS)) {
            return ResourceType.ALTERNATIVE_PARTNERS;
        }
        if (name.contains(API_BINARY_PARAMETERS)) {
            return ResourceType.BINARY_PARAMETERS;
        }
        if (name.contains(API_STRING_PARTNERS)) {
            return ResourceType.STRING_PARAMETERS;
        }

        try {
            JSONObject json = readJsonFile(file);
            JSONObject dObject = json.optJSONObject(JSON_KEY_D);
            if (dObject == null) {
                return ResourceType.UNKNOWN;
            }

            JSONArray results = dObject.optJSONArray(JSON_KEY_RESULTS);
            if (results == null || results.isEmpty()) {
                return ResourceType.UNKNOWN;
            }

            JSONObject first = results.getJSONObject(0);
            JSONObject metadata = first.optJSONObject("__metadata");
            if (metadata != null) {
                String type = metadata.optString("type", "");
                String uri = metadata.optString("uri", "");
                if (type.contains("AlternativePartner") || uri.contains("/AlternativePartners(")) {
                    return ResourceType.ALTERNATIVE_PARTNERS;
                }
                if (type.contains("BinaryParameter") || uri.contains("/BinaryParameters(")) {
                    return ResourceType.BINARY_PARAMETERS;
                }
                if (type.contains("StringParameter") || uri.contains("/StringParameters(")) {
                    return ResourceType.STRING_PARAMETERS;
                }
            }

            if (first.has(JSON_KEY_AGENCY) && first.has(JSON_KEY_SCHEME) && first.has(JSON_KEY_ID) && first.has(JSON_KEY_PID)) {
                return ResourceType.ALTERNATIVE_PARTNERS;
            }
            if (first.has(JSON_KEY_CONTENT_TYPE)) {
                return ResourceType.BINARY_PARAMETERS;
            }
            if (first.has(JSON_KEY_ID) && first.has(JSON_KEY_PID) && first.has(JSON_KEY_VALUE)) {
                return ResourceType.STRING_PARAMETERS;
            }
        } catch (Exception ex) {
            LOGGER.warn("Could not inspect import file {}", file.getName());
        }

        return ResourceType.UNKNOWN;
    }
}

