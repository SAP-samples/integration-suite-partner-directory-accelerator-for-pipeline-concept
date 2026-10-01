package org.example.ui.components;

import org.example.model.AlternativePartner;
import org.example.ui.pages.ParametersPage;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static org.example.model.AlternativePartner.addAlternativePartnerToList;
import static org.example.model.AlternativePartner.removeAlternativePartnerFromList;
import static org.example.model.AlternativePartner.removeAlternativePartnersByPid;
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
                                currentHeaderValues.put(LABEL_SCHEME, SCHEME_SENDER_INTERFACE);
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
            } else if (!isExistingEntry && key.equals(LABEL_SELECT_DETERMINATION_TYPE)) {
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

            GridBagConstraints deleteButtonsGbc = new GridBagConstraints();
            deleteButtonsGbc.gridx = 2;
            deleteButtonsGbc.gridy = 0;
            deleteButtonsGbc.gridwidth = 3;
            deleteButtonsGbc.anchor = GridBagConstraints.WEST;
            deleteButtonsGbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

            JPanel deleteButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, UI_PADDING, 0));
            
            JButton copyScenarioButton = new JButton(LABEL_COPY_SCENARIO);
            copyScenarioButton.addActionListener(e -> copyCurrentScenario());
            deleteButtonsPanel.add(copyScenarioButton);

            JButton deleteEntryButton = new JButton(LABEL_DELETE_ENTRY);
            deleteEntryButton.addActionListener(e -> deleteCurrentAlternativePartner());
            deleteButtonsPanel.add(deleteEntryButton);

            JButton deletePartnerIdButton = new JButton(LABEL_DELETE_PARTNER_ID);
            deletePartnerIdButton.addActionListener(e -> deleteCurrentPartnerId());
            deleteButtonsPanel.add(deletePartnerIdButton);
            add(deleteButtonsPanel, deleteButtonsGbc);

            gbc.gridx = 2;
            gbc.gridy = 3;
            JButton changePidButton = new JButton(LABEL_CHANGE_PID);
            changePidButton.addActionListener(e -> {
                String oldPid = currentHeaderValues.get(LABEL_PID);

                JDialog dialog = new JDialog(mainFrame, LABEL_CHANGE_PID, true);
                dialog.setLayout(new BorderLayout());
                dialog.setSize(UI_DIALOG_WIDTH, UI_DIALOG_HEIGHT);
                dialog.setLocationRelativeTo(mainFrame);

                JPanel enterNewPidPanel = new JPanel(new GridBagLayout());
                GridBagConstraints c = new GridBagConstraints();
                c.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);
                JLabel newPidLabel = new JLabel(colon(LABEL_ENTER_NEW_PID));
                enterNewPidPanel.add(newPidLabel, c);
                JTextField newPidTextField = new JTextField(oldPid, UI_TEXT_FIELD_COLUMNS);
                enterNewPidPanel.add(newPidTextField, c);
                dialog.add(enterNewPidPanel, BorderLayout.CENTER);

                JPanel buttonPanel = new JPanel(new FlowLayout());
                JButton cancelButton = new JButton(LABEL_CANCEL);
                cancelButton.addActionListener(e1 -> dialog.dispose());
                buttonPanel.add(cancelButton);
                LoadingIcon loadingIcon = new LoadingIcon();

                JButton saveButton = new JButton(LABEL_CONFIRM_CHANGE_PID);
                saveButton.addActionListener(e1 -> {
                    String newPid = newPidTextField.getText();
                    boolean newPidExists = true;

                    try {
                        newPidExists = httpRequestHandler.sendGetRequestAlternativePartnerCheckIfPidExists(newPid);
                    } catch (Exception ex) {
                        LOGGER.error(ex);
                    }

                    if (newPidExists) {
                        JOptionPane.showMessageDialog(mainFrame, LABEL_ERROR_NEW_PID_ALREADY_EXISTS, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                    } else {
                        loadingIcon.startTimer();

                        SwingWorker<Void, Void> worker = new SwingWorker<>() {
                            @Override
                            protected Void doInBackground() {

                                LOGGER.info("Start to change pid of scenario from {} to {}", oldPid, newPid);

                                HashMap<String, String> oldAndNewPids = new HashMap<>();
                                oldAndNewPids.put(oldPid, newPid);
                                List<String> oldPids = new ArrayList<>(oldAndNewPids.keySet());

                                String agency = currentHeaderValues.get(LABEL_AGENCY);
                                String scheme = currentHeaderValues.get(LABEL_SCHEME);
                                String id = currentHeaderValues.get(LABEL_ID_ALTERNATIVE_PARTNERS);

                                List<String> changePidErrors = new ArrayList<>();

                                // Alternative Partner
                                JSONObject jsonAlternativePartnersToTransport = httpRequestHandler.getAlternativePartnersToTransport(oldPid);
                                if (jsonAlternativePartnersToTransport != null) {
                                    httpRequestHandler.transportAlternativePartnerPutOnly(jsonAlternativePartnersToTransport, newPid, changePidErrors);
                                }

                                // Binary Parameters
                                JSONObject jsonBinaryParametersToTransport = httpRequestHandler.getBinaryParametersToTransport(oldPids);
                                if (jsonBinaryParametersToTransport != null) {
                                    httpRequestHandler.transportBinaryParameters(jsonBinaryParametersToTransport, false, changePidErrors, true, oldAndNewPids);
                                }

                                // String Parameters
                                JSONObject jsonStringParametersToTransport = httpRequestHandler.getStringParametersToTransport(oldPids);
                                if (jsonStringParametersToTransport != null) {
                                    httpRequestHandler.transportStringParameters(jsonStringParametersToTransport, false, changePidErrors, true, oldAndNewPids);
                                }

                                // repaint UI
                                AlternativePartner alternativePartner = currentAlternativePartnersList.stream()
                                        .filter(obj -> obj.getAgency().equals(agency)
                                                && obj.getScheme().equals(scheme)
                                                && obj.getId().equals(id)
                                                && obj.getPid().equals(oldPid))
                                        .findFirst()
                                        .orElse(new AlternativePartner(agency, scheme, id, oldPid));
                                alternativePartner.setPid(newPid);
                                ParametersPage binaryParameterDetailPage = new ParametersPage(alternativePartner);
                                panelContainer.add(binaryParameterDetailPage, newPid);
                                cardLayout.show(panelContainer, newPid);

                                dialog.dispose();

                                if (changePidErrors.isEmpty()) {
                                    String logTransport = LABEL_CHANGE_PID_FINISHED + LABEL_CHANGE_PID_SUCCESSFUL;
                                    JOptionPane.showMessageDialog(mainFrame, logTransport, LABEL_SUCCESS, JOptionPane.INFORMATION_MESSAGE);
                                    LOGGER.info(logTransport);
                                } else {
                                    String logTransport = LABEL_CHANGE_PID_FINISHED + LABEL_CHANGE_PID_FAILED_1 + changePidErrors.size() + LABEL_CHANGE_PID_FAILED_2;
                                    JOptionPane.showMessageDialog(mainFrame, logTransport, LABEL_WARNING, JOptionPane.WARNING_MESSAGE);
                                    LOGGER.warn(logTransport);
                                }

                                return null;
                            }
                        };
                        worker.execute();
                    }
                });
                buttonPanel.add(saveButton);
                buttonPanel.add(loadingIcon);

                dialog.add(buttonPanel, BorderLayout.SOUTH);

                dialog.setVisible(true);
            });
            add(changePidButton, gbc);
        }

    }

    private void deleteCurrentAlternativePartner() {
        if (!showDeleteConfirmationDialog(LABEL_SURE_TO_DELETE_ENTRY, LABEL_CONFIRM_DELETE_ENTRY)) {
            return;
        }

        AlternativePartner alternativePartnerToDelete = new AlternativePartner(
                currentHeaderValues.get(LABEL_AGENCY),
                currentHeaderValues.get(LABEL_SCHEME),
                currentHeaderValues.get(LABEL_ID_ALTERNATIVE_PARTNERS),
                currentHeaderValues.get(LABEL_PID)
        );

        try {
            String httpResponse = httpRequestHandler.sendDeleteRequestAlternativePartners(
                    alternativePartnerToDelete.getAgency(),
                    alternativePartnerToDelete.getScheme(),
                    alternativePartnerToDelete.getId()
            );

            if (httpRequestHandler.wasLatestResponseSuccessful()) {
                removeAlternativePartnerFromList(alternativePartnerToDelete);
                if (!reloadAlternativePartnersOverview()) {
                    showHttpResponseWithTimer(httpResponseLabelHeader, httpResponse);
                }
                showAlternativePartnersOverview();
                JOptionPane.showMessageDialog(mainFrame, LABEL_DELETE_ENTRY_SUCCESSFUL, LABEL_SUCCESS, JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainFrame, httpRequestHandler.getLatestErrorMessageForUi(), LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
            JOptionPane.showMessageDialog(mainFrame, ex.getMessage(), LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

     private void deleteCurrentPartnerId() {
         String pid = currentHeaderValues.get(LABEL_PID);
         String confirmationText = LABEL_SURE_TO_DELETE_PARTNER_ID_1 + pid + LABEL_SURE_TO_DELETE_PARTNER_ID_2;
         if (!showDeleteConfirmationDialog(confirmationText, LABEL_CONFIRM_DELETE_PARTNER_ID)) {
             return;
         }

         List<String> deleteErrors = httpRequestHandler.deletePartnerId(pid);
         boolean overviewReloaded = reloadAlternativePartnersOverview();

         if (deleteErrors.isEmpty()) {
             if (!overviewReloaded) {
                 removeAlternativePartnersByPid(pid);
             }
             showAlternativePartnersOverview();
             JOptionPane.showMessageDialog(mainFrame, LABEL_DELETE_PARTNER_ID_SUCCESSFUL, LABEL_SUCCESS, JOptionPane.INFORMATION_MESSAGE);
             return;
         }

         String warningMessage = LABEL_DELETE_PARTNER_ID_FAILED_1 + deleteErrors.size() + LABEL_DELETE_PARTNER_ID_FAILED_2;
         if (overviewReloaded) {
             showAlternativePartnersOverview();
             JOptionPane.showMessageDialog(mainFrame, warningMessage, LABEL_WARNING, JOptionPane.WARNING_MESSAGE);
         } else {
             JOptionPane.showMessageDialog(mainFrame, warningMessage + "\n\n" + LABEL_DELETE_PARTNER_ID_RELOAD_REQUIRED, LABEL_WARNING, JOptionPane.WARNING_MESSAGE);
         }
     }

     private void copyCurrentScenario() {
         String currentAgency = currentHeaderValues.get(LABEL_AGENCY);
         String currentScheme = currentHeaderValues.get(LABEL_SCHEME);
         String currentId = currentHeaderValues.get(LABEL_ID_ALTERNATIVE_PARTNERS);
         String currentPid = currentHeaderValues.get(LABEL_PID);
 
         // Create a copy scenario dialog
         JDialog copyDialog = new JDialog(mainFrame, LABEL_COPY_SCENARIO_DIALOG_TITLE, true);
         copyDialog.setLayout(new BorderLayout());

         // Create input panel with editable fields
         JPanel inputPanel = new JPanel(new GridBagLayout());
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.weightx = 1.0;

         // Row 0: Sender System
         gbc.gridx = 0;
         gbc.gridy = 0;
         JLabel agencyLabel = new JLabel(colonAsterisk(LABEL_AGENCY));
         inputPanel.add(agencyLabel, gbc);

         gbc.gridx = 1;
         JTextField agencyTextField = new JTextField(currentAgency, UI_TEXT_FIELD_COLUMNS);
         inputPanel.add(agencyTextField, gbc);

         // Row 1: Scheme (now fully editable)
         gbc.gridx = 0;
         gbc.gridy = 1;
         JLabel schemeLabel = new JLabel(colonAsterisk(LABEL_SCHEME));
         inputPanel.add(schemeLabel, gbc);

         gbc.gridx = 1;
         JTextField schemeTextField = new JTextField(currentScheme, UI_TEXT_FIELD_COLUMNS);
         inputPanel.add(schemeTextField, gbc);

         // Row 2: Sender Interface
         gbc.gridx = 0;
         gbc.gridy = 2;
         JLabel idLabel = new JLabel(colonAsterisk(LABEL_ID_ALTERNATIVE_PARTNERS));
         inputPanel.add(idLabel, gbc);

         gbc.gridx = 1;
         JTextField idTextField = new JTextField(currentId, UI_TEXT_FIELD_COLUMNS);
         inputPanel.add(idTextField, gbc);

         // Row 3: Partner ID
         gbc.gridx = 0;
         gbc.gridy = 3;
         JLabel pidLabel = new JLabel(colonAsterisk(LABEL_PID));
         inputPanel.add(pidLabel, gbc);

         gbc.gridx = 1;
         JTextField pidTextField = new JTextField(currentPid, UI_TEXT_FIELD_COLUMNS);
         inputPanel.add(pidTextField, gbc);

         // Add parameter checkboxes with more spacing from input fields
         JPanel checkboxPanel = new JPanel();
         checkboxPanel.setBorder(BorderFactory.createEmptyBorder(UI_PADDING * 3, UI_PADDING, UI_PADDING, UI_PADDING));
         checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));

         JCheckBox copyBinaryParametersCheckbox = new JCheckBox(LABEL_COPY_BINARY_PARAMETERS, true);
         checkboxPanel.add(copyBinaryParametersCheckbox);

         JCheckBox copyStringParametersCheckbox = new JCheckBox(LABEL_COPY_STRING_PARAMETERS_COPY, true);
         checkboxPanel.add(copyStringParametersCheckbox);

         // Center the content vertically
         JPanel centerPanel = new JPanel(new GridBagLayout());
         GridBagConstraints centerGbc = new GridBagConstraints();
         centerGbc.gridx = 0;
         centerGbc.gridy = 0;
         centerGbc.weighty = 1.0;
         centerGbc.anchor = GridBagConstraints.NORTH;
         JPanel topSpacer = new JPanel();
         centerPanel.add(topSpacer, centerGbc);

         centerGbc.gridy = 1;
         centerGbc.weighty = 0.0;
         centerGbc.anchor = GridBagConstraints.CENTER;
         JPanel contentWrapper = new JPanel(new BorderLayout());
         contentWrapper.add(inputPanel, BorderLayout.NORTH);
         contentWrapper.add(checkboxPanel, BorderLayout.CENTER);
         centerPanel.add(contentWrapper, centerGbc);

         centerGbc.gridy = 2;
         centerGbc.weighty = 1.0;
         centerGbc.anchor = GridBagConstraints.SOUTH;
         JPanel bottomSpacer = new JPanel();
         centerPanel.add(bottomSpacer, centerGbc);

         copyDialog.add(centerPanel, BorderLayout.CENTER);

         // Button panel
         JPanel buttonPanel = new JPanel();

         JButton cancelButton = new JButton(LABEL_CANCEL);
         cancelButton.addActionListener(e -> copyDialog.dispose());
         buttonPanel.add(cancelButton);

         JButton confirmButton = new JButton(LABEL_SEND_NEW_TO_API);
         confirmButton.addActionListener(e -> {
             String newAgency = agencyTextField.getText().trim();
             String newScheme = schemeTextField.getText().trim();
             String newId = idTextField.getText().trim();
             String newPid = pidTextField.getText().trim();

             if (newAgency.isEmpty() || newScheme.isEmpty() || newId.isEmpty() || newPid.isEmpty()) {
                 JOptionPane.showMessageDialog(copyDialog, LABEL_FILL_OUT_ALL_FIELDS, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                 return;
             }

             // Check if the new alternative partner already exists
             AlternativePartner newPartner = new AlternativePartner(newAgency, newScheme, newId, newPid);
             if (AlternativePartner.isDuplicate(newPartner)) {
                 JOptionPane.showMessageDialog(copyDialog, "This alternative partner already exists.", LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                 return;
             }

             try {
                 // Create the new alternative partner
                 httpRequestHandler.sendPostRequestAlternativePartners(newAgency, newScheme, newId, newPid);

                 boolean shouldCopyParameters = !currentPid.equals(newPid);

                 // Copy binary parameters if checked and the PID changed
                 if (shouldCopyParameters && copyBinaryParametersCheckbox.isSelected()) {
                     try {
                         List<String> oldPidList = new ArrayList<>();
                         oldPidList.add(currentPid);
                         JSONObject jsonBinaryParametersToTransport = httpRequestHandler.getBinaryParametersToTransport(oldPidList);
                         if (jsonBinaryParametersToTransport != null) {
                             HashMap<String, String> pidMapping = new HashMap<>();
                             pidMapping.put(currentPid, newPid);
                             List<String> copyErrors = new ArrayList<>();
                             // Use false for changeMode to ensure we don't delete originals - just create copies
                             httpRequestHandler.transportBinaryParameters(jsonBinaryParametersToTransport, false, copyErrors, false, pidMapping);
                         }
                     } catch (Exception ex) {
                         LOGGER.warn("Error copying binary parameters: {}", ex.getMessage());
                     }
                 }

                 // Copy string parameters if checked and the PID changed
                 if (shouldCopyParameters && copyStringParametersCheckbox.isSelected()) {
                     try {
                         List<String> oldPidList = new ArrayList<>();
                         oldPidList.add(currentPid);
                         JSONObject jsonStringParametersToTransport = httpRequestHandler.getStringParametersToTransport(oldPidList);
                         if (jsonStringParametersToTransport != null) {
                             HashMap<String, String> pidMapping = new HashMap<>();
                             pidMapping.put(currentPid, newPid);
                             List<String> copyErrors = new ArrayList<>();
                             // Use false for changeMode to ensure we don't delete originals - just create copies
                             httpRequestHandler.transportStringParameters(jsonStringParametersToTransport, false, copyErrors, false, pidMapping);
                         }
                     } catch (Exception ex) {
                         LOGGER.warn("Error copying string parameters: {}", ex.getMessage());
                     }
                 }
                 
                 // Add to the list
                 addAlternativePartnerToList(newPartner);
                 
                 // Refresh the table
                 alternativePartnersPage.refreshTableData(currentAlternativePartnersList);
                 
                 copyDialog.dispose();
                 JOptionPane.showMessageDialog(mainFrame, LABEL_COPY_SCENARIO_SUCCESSFUL, LABEL_SUCCESS, JOptionPane.INFORMATION_MESSAGE);
             } catch (Exception ex) {
                 LOGGER.error(ex);
                 JOptionPane.showMessageDialog(copyDialog, ex.getMessage(), LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
             }
         });
         buttonPanel.add(confirmButton);

         copyDialog.add(buttonPanel, BorderLayout.SOUTH);

         copyDialog.setSize(UI_DIALOG_WIDTH, UI_DIALOG_HEIGHT);
         copyDialog.setLocationRelativeTo(mainFrame);
         copyDialog.setVisible(true);
     }

    private boolean showDeleteConfirmationDialog(String message, String title) {
        String[] options = {LABEL_DELETE, LABEL_CANCEL};

        int option = JOptionPane.showOptionDialog(
                mainFrame,
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        return option == 0;
    }

    private boolean reloadAlternativePartnersOverview() {
        try {
            String httpResponse = httpRequestHandler.sendGetRequestAlternativePartners(true);
            showHttpResponseWithTimer(httpResponseLabelHeader, httpResponse);
            return true;
        } catch (Exception ex) {
            LOGGER.error(ex);
            return false;
        }
    }

    private void showAlternativePartnersOverview() {
        panelContainer.removeAll();
        panelContainer.add(new org.example.ui.pages.AlternativePartnersPage());
        panelContainer.revalidate();
        panelContainer.repaint();
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