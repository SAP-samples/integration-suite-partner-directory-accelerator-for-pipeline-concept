package org.example.ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.example.utils.SharedData.*;

public class LandscapeDialog extends JDialog {
    private final JPanel mainPanel;
    private final List<JTextField> idFields;
    private final List<JTextField> valueFields;
    private final List<JButton> deleteButtons;

    int inputFieldColumns = 20;
    int dimensionWidth = 100;

    public LandscapeDialog() {
        super(mainFrame, LABEL_MAINTAIN_STRING_PARAMETER + STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE, true);
        setLayout(new BorderLayout());
        setSize(UI_DIALOG_WIDTH, UI_DIALOG_HEIGHT);
        setLocationRelativeTo(mainFrame);

        try {
            httpRequestHandler.sendGetRequestStringParameterLandscape();
        } catch (Exception ex) {
            LOGGER.error(ex);
        }

        idFields = new ArrayList<>();
        valueFields = new ArrayList<>();
        deleteButtons = new ArrayList<>();

        mainPanel = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel idLabel = new JLabel(colon(LABEL_ID + (" (tenant ID)")));
        idLabel.setPreferredSize(new Dimension(dimensionWidth, idLabel.getPreferredSize().height));
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(idLabel, gbc);

        JLabel valueLabel = new JLabel(colon(LABEL_VALUE + " (e.g., DEV / QA / PRD)"));
        valueLabel.setPreferredSize(new Dimension(200, valueLabel.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.gridy = 0;
        mainPanel.add(valueLabel, gbc);

        JButton addButton = new JButton(LABEL_ADD_ENTRY);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(addButton, gbc);

        addButton.addActionListener(e -> addRow());

        if (currentLandscapeTenantParameters != null && !currentLandscapeTenantParameters.isEmpty()) {
            for (Map.Entry<String, String> entry : currentLandscapeTenantParameters.entrySet()) {
                addRow(entry.getKey(), entry.getValue());
            }
        } else {
            addRow();
        }

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = new JButton(LABEL_SEND_CHANGES_TO_API);
        JButton cancelButton = new JButton(LABEL_CANCEL);

        saveButton.addActionListener(e -> save());
        cancelButton.addActionListener(e -> dispose());

        controlPanel.add(cancelButton);
        controlPanel.add(saveButton);
        add(controlPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void addRow() {
        addRow(null, null);
    }

    private void addRow(String initialId, String initialValue) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField idField = new JTextField(inputFieldColumns);
        idField.setPreferredSize(new Dimension(dimensionWidth, idField.getPreferredSize().height));
        if (initialId != null) {
            idField.setText(initialId);
        }

        JTextField valueField = new JTextField(inputFieldColumns);
        valueField.setPreferredSize(new Dimension(dimensionWidth, valueField.getPreferredSize().height));
        if (initialValue != null) {
            valueField.setText(initialValue);
        }

        JButton deleteButton = new JButton(LABEL_DELETE_ENTRY);

        int row = idFields.size() + 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(idField, gbc);

        gbc.gridx = 1;
        mainPanel.add(valueField, gbc);

        gbc.gridx = 2;
        mainPanel.add(deleteButton, gbc);

        idFields.add(idField);
        valueFields.add(valueField);
        deleteButtons.add(deleteButton);

        deleteButton.addActionListener(e -> deleteRow(idFields.indexOf(idField)));

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void deleteRow(int index) {
        mainPanel.remove(idFields.get(index));
        mainPanel.remove(valueFields.get(index));
        mainPanel.remove(deleteButtons.get(index));

        idFields.remove(index);
        valueFields.remove(index);
        deleteButtons.remove(index);

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void save() {
        Map<String, String> newLandscape = new HashMap<>();

        for (int i = 0; i < idFields.size(); i++) {
            String newLandscapeId = idFields.get(i).getText();
            String newLandscapeValue = valueFields.get(i).getText();

            newLandscape.put(newLandscapeId, newLandscapeValue);

            try {
                if (currentLandscapeTenantParameters.containsKey(newLandscapeId)) {
                    if (!newLandscape.get(newLandscapeId).equals(currentLandscapeTenantParameters.get(newLandscapeId))) {
                        httpRequestHandler.sendPutRequestStringParameters(STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE, newLandscapeId, newLandscapeValue);
                    }
                } else {
                    httpRequestHandler.sendPostRequestStringParameters(STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE, newLandscapeId, newLandscapeValue);
                }
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }

        for (String key : currentLandscapeTenantParameters.keySet()) {
            try {
                if (!newLandscape.containsKey(key)) {
                    httpRequestHandler.sendDeleteRequestStringParameters(STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE, key);
                }
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }

        dispose();
    }
}