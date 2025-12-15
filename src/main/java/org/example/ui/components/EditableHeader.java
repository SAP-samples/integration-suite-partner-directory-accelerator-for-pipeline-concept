package org.example.ui.components;

import org.example.model.AlternativePartner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static org.example.model.AlternativePartner.addAlternativePartnerToList;
import static org.example.model.AlternativePartner.removeAlternativePartnerFromList;
import static org.example.ui.components.LabelTimer.showHttpResponseWithTimer;
import static org.example.utils.SharedData.*;

public class EditableHeader extends JPanel {
    private final LinkedHashMap<String, String> originalHeaderValues;
    public LinkedHashMap<String, String> currentHeaderValues;

    private final boolean addButtons;

    private JButton sendButton;
    private JButton cancelButton;

    public EditableHeader(LinkedHashMap<String, String> hashMap, boolean isExistingEntry) {
        originalHeaderValues = new LinkedHashMap<>(hashMap);
        currentHeaderValues = new LinkedHashMap<>(hashMap);

        this.addButtons = isExistingEntry;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

        int row = 0;
        for (String key : hashMap.keySet()) {
            JLabel keyLabel = new JLabel(colonAsterisk(key));
            JComponent valueComponent;
            if (!isExistingEntry && key.equals(LABEL_SENDER_TYPE)) {
                ButtonGroup buttonGroup = new ButtonGroup();
                JPanel panelRadioButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));

                for (String labelRadioButton : LABELS_SENDER_TYPES) {
                    JRadioButton radioButton = new JRadioButton(labelRadioButton);
                    buttonGroup.add(radioButton);
                    panelRadioButtons.add(radioButton);
                    if (labelRadioButton.equals(LABEL_SENDER_DEFAULT)) {
                        radioButton.setSelected(true);
                        currentHeaderValues.put(key, LABEL_SENDER_DEFAULT);
                    }
                    radioButton.addItemListener(e -> {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            if (labelRadioButton.equals(LABEL_SENDER_DEFAULT)) {
                                getComponentAtKey(LABEL_SCHEME_XI).setEnabled(false);
                                getLabelAtKey(LABEL_ID_ALTERNATIVE_PARTNERS_XI).setText(colonAsterisk(LABEL_ID_ALTERNATIVE_PARTNERS));
                                getLabelAtKey(LABEL_SCHEME_XI).setText(colonAsterisk(LABEL_SCHEME));
                                currentHeaderValues.put(LABEL_SCHEME, "SenderInterface"); //
                                currentHeaderValues.put(LABEL_SENDER_TYPE, LABEL_SENDER_DEFAULT);
                                updateFieldValues();
                            }
                            if (labelRadioButton.equals(LABEL_SENDER_XI)) {
                                getComponentAtKey(LABEL_SCHEME).setEnabled(true);
                                getLabelAtKey(LABEL_ID_ALTERNATIVE_PARTNERS).setText(colonAsterisk(LABEL_ID_ALTERNATIVE_PARTNERS_XI));
                                getLabelAtKey(LABEL_SCHEME).setText(colonAsterisk(LABEL_SCHEME_XI));
                                currentHeaderValues.put(LABEL_SCHEME_XI, "");
                                currentHeaderValues.put(LABEL_SENDER_TYPE, LABEL_SENDER_XI);
                                updateFieldValues();

                            }
                        }
                    });
                }
                valueComponent = panelRadioButtons;
            }
            else if (!isExistingEntry && key.equals(LABEL_SELECT_DETERMINATION_TYPE)) {
                ButtonGroup buttonGroup = new ButtonGroup();
                JPanel panelRadioButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));

                for (String labelRadioButton : LABELS_DETERMINATION_TYPES) {
                    JRadioButton radioButton = new JRadioButton(labelRadioButton);
                    buttonGroup.add(radioButton);
                    panelRadioButtons.add(radioButton);
                    if (labelRadioButton.equals(LABEL_COMBINED_XSLT)) {
                        radioButton.setSelected(true);
                        currentHeaderValues.put(key, LABEL_COMBINED_XSLT);
                    }
                    radioButton.addItemListener(e -> {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            currentHeaderValues.put(key, radioButton.getText());
                        }
                    });
                }
                valueComponent = panelRadioButtons;
            } else if (isExistingEntry && key.equals(LABEL_PID)) {
                String pid = hashMap.get(key);
                JLabel valueLabel = new JLabel(pid);
                currentHeaderValues.put(key, pid);
                valueComponent = valueLabel;
            } else {
                JTextField valueTextField = new JTextField(hashMap.get(key), DEFAULT_COLUMNS_TEXT_FIELD);
                if (!isExistingEntry && key.equals(LABEL_SCHEME)) {
                    valueTextField.setText(SCHEME_SENDER_INTERFACE);
                    valueTextField.setEnabled(false);
                    currentHeaderValues.put(key, valueTextField.getText());
                }
                valueTextField.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        currentHeaderValues.put(key, valueTextField.getText());
                        checkForChanges();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        currentHeaderValues.put(key, valueTextField.getText());
                        checkForChanges();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                    }
                });

                valueComponent = valueTextField;
            }

            gbc.gridx = 0;
            gbc.gridy = row;
            add(keyLabel, gbc);

            gbc.gridx = 1;
            add(valueComponent, gbc);

            row++;

        }

        if (isExistingEntry) {
            sendButton = new JButton(LABEL_SEND_CHANGES_TO_API);
            cancelButton = new JButton(LABEL_CANCEL);

            sendButton.setVisible(false);
            cancelButton.setVisible(false);
            sendButton.addActionListener(e -> {
                List<String> differingKeys = new ArrayList<>();

                for (String key : originalHeaderValues.keySet()) {
                    String originalValue = (originalHeaderValues.get(key) == null) ? "" : originalHeaderValues.get(key);
                    String currentValue = (currentHeaderValues.get(key) == null) ? "" : currentHeaderValues.get(key);

                    if (!originalValue.equals(currentValue)) {
                        differingKeys.add(key);
                    }
                }

                try {
                    // Alternative Partners
                    if (differingKeys.contains(LABEL_SCHEME) || differingKeys.contains(LABEL_AGENCY) || differingKeys.contains(LABEL_ID_ALTERNATIVE_PARTNERS)) {
                        httpRequestHandler.sendDeleteRequestAlternativePartners(originalHeaderValues.get(LABEL_AGENCY), originalHeaderValues.get(LABEL_SCHEME), originalHeaderValues.get(LABEL_ID_ALTERNATIVE_PARTNERS));
                        String httpResponse = httpRequestHandler.sendPostRequestAlternativePartners(currentHeaderValues.get(LABEL_AGENCY), currentHeaderValues.get(LABEL_SCHEME), currentHeaderValues.get(LABEL_ID_ALTERNATIVE_PARTNERS), currentHeaderValues.get(LABEL_PID));
                        JLabel jLabel = new JLabel();
                        add(jLabel);
                        showHttpResponseWithTimer(jLabel, httpResponse);

                        removeAlternativePartnerFromList(new AlternativePartner(originalHeaderValues.get(LABEL_AGENCY), originalHeaderValues.get(LABEL_SCHEME), originalHeaderValues.get(LABEL_ID_ALTERNATIVE_PARTNERS), originalHeaderValues.get(LABEL_PID)));
                        addAlternativePartnerToList(new AlternativePartner(currentHeaderValues.get(LABEL_AGENCY), currentHeaderValues.get(LABEL_SCHEME), currentHeaderValues.get(LABEL_ID_ALTERNATIVE_PARTNERS), currentHeaderValues.get(LABEL_PID)));
                        alternativePartnersPage.refreshTableData(currentAlternativePartnersList);

                        // currentReceiverDetermination.updateCurrentBinaryParameter(currentHeaderValues.get(LABEL_PID), currentHeaderValues.get(LABEL_ID_BINARY_PARAMETERS), currentHeaderValues.get(LABEL_CONTENT_TYPE), currentHeaderValues.get(LABEL_VALUE));
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex);
                }

                originalHeaderValues.clear();
                for (String key : currentHeaderValues.keySet()) {
                    originalHeaderValues.put(key, currentHeaderValues.get(key));
                }

                sendButton.setVisible(false);
                cancelButton.setVisible(false);
            });

            cancelButton.addActionListener(e -> {
                for (String key : originalHeaderValues.keySet()) {
                    currentHeaderValues.put(key, originalHeaderValues.get(key));
                }
                updateFieldValues();
                sendButton.setVisible(false);
                cancelButton.setVisible(false);
            });

            add(sendButton);
            add(cancelButton);
        }

    }

    private void checkForChanges() {
        boolean changesDetected = !originalHeaderValues.equals(currentHeaderValues);
        if (addButtons) {
            sendButton.setVisible(changesDetected);
            cancelButton.setVisible(changesDetected);
        }
    }

    private void updateFieldValues() {
        for (Component component : getComponents()) {
            if (component instanceof JTextField textField) {
                String key = getKeyForComponent(textField);
                textField.setText(currentHeaderValues.get(key));
            }
        }
    }

    private String getKeyForComponent(Component component) {
        for (String key : currentHeaderValues.keySet()) {
            if (component.equals(getComponentAtKey(key))) {
                return key;
            }
        }
        return null;
    }

    public Component getComponentAtKey(String key) {
        for (Component component : getComponents()) {
            if (component instanceof JTextField) {
                if (colonAsterisk(key).equals(((JLabel) getComponent(getComponentZOrder(component) - 1)).getText())) {
                    return component;
                }
            }
        }
        return null;
    }

    public JLabel getLabelAtKey(String key) {
        for (Component component : getComponents()) {
            if (component instanceof JLabel) {
                if (colonAsterisk(key).equals(((JLabel) component).getText())) {
                    return (JLabel) component;
                }
            }
        }
        return null;
    }
}