package org.example.ui.components;

import org.example.model.AlternativePartner;
import org.example.ui.pages.ParametersPage;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static org.example.model.AlternativePartner.addAlternativePartnerToList;
import static org.example.model.AlternativePartner.removeAlternativePartnerFromList;
import static org.example.model.AlternativePartner.removeAlternativePartnersByPid;
import static org.example.ui.components.LabelTimer.showHttpResponseWithTimer;
import static org.example.utils.SharedData.*;

public class EditableHeader extends JPanel {
    private static final Color SPACE_WARNING_BACKGROUND = new Color(255, 250, 205);
    private static final Color SPACE_WARNING_BORDER = new Color(204, 153, 0);
    private static final Color SPACE_WARNING_FIELD_BORDER = new Color(204, 102, 0);
    private static final String CLIENT_PROPERTY_DEFAULT_BORDER = "spaceWarningDefaultBorder";
    private static final String CLIENT_PROPERTY_WARNING_BORDER = "spaceWarningWarningBorder";
    private static final String CLIENT_PROPERTY_WARNING_POPUP = "spaceWarningPopup";

    private final LinkedHashMap<String, String> originalHeaderValues;
    public LinkedHashMap<String, String> currentHeaderValues;

    private JButton discardAlternativePartnerChangesButton;

    public EditableHeader(LinkedHashMap<String, String> hashMap, boolean isExistingEntry) {
        originalHeaderValues = new LinkedHashMap<>(hashMap);
        currentHeaderValues = new LinkedHashMap<>(hashMap);

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

                attachSpaceWarning(valueTextField);

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
            JButton sendAlternativePartnerChangesButton = new JButton(LABEL_SEND_ALTERNATIVE_PARTNER_CHANGES);
            JButton discardAlternativePartnerChangesButton = new JButton(LABEL_DISCARD_ALTERNATIVE_PARTNER_CHANGES);

            sendAlternativePartnerChangesButton.addActionListener(e -> {
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
            });

            discardAlternativePartnerChangesButton.addActionListener(e -> {
                for (String key : originalHeaderValues.keySet()) {
                    currentHeaderValues.put(key, originalHeaderValues.get(key));
                }
                updateFieldValues();
            });

            // Line 0: Send and Cancel buttons
            GridBagConstraints sendCancelGbc = new GridBagConstraints();
            sendCancelGbc.gridx = 2;
            sendCancelGbc.gridy = 0;
            sendCancelGbc.gridwidth = 3;
            sendCancelGbc.anchor = GridBagConstraints.WEST;
            sendCancelGbc.fill = GridBagConstraints.HORIZONTAL;
            sendCancelGbc.weightx = 1.0;
            sendCancelGbc.ipady = 5;
            sendCancelGbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

            JPanel sendCancelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, UI_PADDING, 0));
            sendCancelPanel.add(sendAlternativePartnerChangesButton);
            sendCancelPanel.add(discardAlternativePartnerChangesButton);
            add(sendCancelPanel, sendCancelGbc);

            // Line 1: Copy button
            GridBagConstraints copyButtonGbc = new GridBagConstraints();
            copyButtonGbc.gridx = 2;
            copyButtonGbc.gridy = 1;
            copyButtonGbc.gridwidth = 3;
            copyButtonGbc.anchor = GridBagConstraints.WEST;
            copyButtonGbc.fill = GridBagConstraints.HORIZONTAL;
            copyButtonGbc.weightx = 1.0;
            copyButtonGbc.ipady = 5;
            copyButtonGbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

            JPanel copyButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, UI_PADDING, 0));

            JButton copyScenarioButton = new JButton(LABEL_COPY_SCENARIO);
            copyScenarioButton.addActionListener(e -> copyCurrentScenario());
            copyButtonPanel.add(copyScenarioButton);
            add(copyButtonPanel, copyButtonGbc);

            // Line 2: Delete buttons
            GridBagConstraints deleteButtonsGbc = new GridBagConstraints();
            deleteButtonsGbc.gridx = 2;
            deleteButtonsGbc.gridy = 2;
            deleteButtonsGbc.gridwidth = 3;
            deleteButtonsGbc.anchor = GridBagConstraints.WEST;
            deleteButtonsGbc.fill = GridBagConstraints.HORIZONTAL;
            deleteButtonsGbc.weightx = 1.0;
            deleteButtonsGbc.ipady = 5;
            deleteButtonsGbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

            JPanel deleteButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, UI_PADDING, 0));

            JButton deleteEntryButton = new JButton(LABEL_DELETE_ALTERNATIVE_PARTNER);
            deleteEntryButton.addActionListener(e -> deleteCurrentAlternativePartner());
            deleteButtonsPanel.add(deleteEntryButton);

            JButton deletePartnerIdButton = new JButton(LABEL_DELETE_PARTNER_ID);
            deletePartnerIdButton.addActionListener(e -> deleteCurrentPartnerId());
            deleteButtonsPanel.add(deletePartnerIdButton);
            add(deleteButtonsPanel, deleteButtonsGbc);

            // Line 3: Change PID button
            GridBagConstraints changePidGbc = new GridBagConstraints();
            changePidGbc.gridx = 2;
            changePidGbc.gridy = 3;
            changePidGbc.gridwidth = 3;
            changePidGbc.anchor = GridBagConstraints.WEST;
            changePidGbc.fill = GridBagConstraints.HORIZONTAL;
            changePidGbc.weightx = 1.0;
            changePidGbc.ipady = 5;
            changePidGbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

            JPanel changePidPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, UI_PADDING, 0));

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
                attachSpaceWarning(newPidTextField);
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
            changePidPanel.add(changePidButton);
            add(changePidPanel, changePidGbc);
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
         attachSpaceWarning(agencyTextField);
         inputPanel.add(agencyTextField, gbc);

         // Row 1: Scheme (now fully editable)
         gbc.gridx = 0;
         gbc.gridy = 1;
         JLabel schemeLabel = new JLabel(colonAsterisk(LABEL_SCHEME));
         inputPanel.add(schemeLabel, gbc);

         gbc.gridx = 1;
         JTextField schemeTextField = new JTextField(currentScheme, UI_TEXT_FIELD_COLUMNS);
         attachSpaceWarning(schemeTextField);
         inputPanel.add(schemeTextField, gbc);

         // Row 2: Sender Interface
         gbc.gridx = 0;
         gbc.gridy = 2;
         JLabel idLabel = new JLabel(colonAsterisk(LABEL_ID_ALTERNATIVE_PARTNERS));
         inputPanel.add(idLabel, gbc);

         gbc.gridx = 1;
         JTextField idTextField = new JTextField(currentId, UI_TEXT_FIELD_COLUMNS);
         attachSpaceWarning(idTextField);
         inputPanel.add(idTextField, gbc);

         // Row 3: Partner ID
         gbc.gridx = 0;
         gbc.gridy = 3;
         JLabel pidLabel = new JLabel(colonAsterisk(LABEL_PID));
         inputPanel.add(pidLabel, gbc);

         gbc.gridx = 1;
         JTextField pidTextField = new JTextField(currentPid, UI_TEXT_FIELD_COLUMNS);
         attachSpaceWarning(pidTextField);
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
        // Buttons are now always visible, so no need to toggle visibility
    }

    /**
     * Registers a listener on the given text field which checks after every insert, change or removal of a character
     * whether the value contains a space character. If so, a tooltip-like warning popup is shown below the field and
     * the field is highlighted with a border which has exactly the same insets as the original one.
     * The warning itself is rendered in a floating popup, therefore neither the size of the field nor the layout of
     * the page (and with it the position of all buttons) ever changes.
     */
    public static void attachSpaceWarning(JTextField textField) {
        if (textField == null) {
            return;
        }

        Border defaultBorder = textField.getBorder();
        textField.putClientProperty(CLIENT_PROPERTY_DEFAULT_BORDER, defaultBorder);
        textField.putClientProperty(CLIENT_PROPERTY_WARNING_BORDER, createWarningBorder(textField, defaultBorder));

        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateSpaceWarning(textField));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateSpaceWarning(textField));
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> updateSpaceWarning(textField));
            }
        });

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                updateSpaceWarning(textField);
            }

            @Override
            public void focusLost(FocusEvent e) {
                hideSpaceWarningPopup(textField);
            }
        });

        // Hide the floating popup as soon as the field is not visible anymore (e.g. page switch or closed dialog)
        textField.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !textField.isShowing()) {
                hideSpaceWarningPopup(textField);
            }
        });

        // Hide the floating popup while the surrounding window is moved or the field is removed
        textField.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                // nothing to do
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                hideSpaceWarningPopup(textField);
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                hideSpaceWarningPopup(textField);
            }
        });

        updateSpaceWarning(textField);
    }

    /**
     * Checks whether the given value contains a space character.
     */
    public static boolean containsSpace(String value) {
        return value != null && value.contains(" ");
    }

    /**
     * Creates the highlight border for a field containing a space character. The returned border has exactly the same
     * insets as the original border of the field, so switching between both borders never changes the preferred size
     * or the text position of the field.
     */
    private static Border createWarningBorder(JTextField textField, Border defaultBorder) {
        if (defaultBorder == null) {
            // Without a default border every added border would enlarge the field, therefore none is used
            return null;
        }

        Insets insets = defaultBorder.getBorderInsets(textField);
        if (insets == null) {
            return null;
        }

        Border lineBorder = BorderFactory.createLineBorder(SPACE_WARNING_FIELD_BORDER, 1);
        return BorderFactory.createCompoundBorder(lineBorder, BorderFactory.createEmptyBorder(
                Math.max(0, insets.top - 1),
                Math.max(0, insets.left - 1),
                Math.max(0, insets.bottom - 1),
                Math.max(0, insets.right - 1)));
    }

    private static void updateSpaceWarning(JTextField textField) {
        if (containsSpace(textField.getText())) {
            textField.setToolTipText(LABEL_WARNING_SPACE_IN_INPUT);
            applyBorder(textField, textField.getClientProperty(CLIENT_PROPERTY_WARNING_BORDER));
            showSpaceWarningPopup(textField);
        } else {
            textField.setToolTipText(null);
            applyBorder(textField, textField.getClientProperty(CLIENT_PROPERTY_DEFAULT_BORDER));
            hideSpaceWarningPopup(textField);
        }
    }

    private static void applyBorder(JTextField textField, Object border) {
        Border borderToApply = (border instanceof Border castBorder) ? castBorder : null;
        if (borderToApply != null && borderToApply != textField.getBorder()) {
            textField.setBorder(borderToApply);
        }
    }

    private static void showSpaceWarningPopup(JTextField textField) {
        // Tooltip-like behaviour: the popup is only shown for the field which is currently edited
        if (!textField.isShowing() || !textField.isEnabled() || !textField.isFocusOwner()) {
            hideSpaceWarningPopup(textField);
            return;
        }

        // The field never changes its size or position, therefore an already visible popup can simply stay as it is
        if (textField.getClientProperty(CLIENT_PROPERTY_WARNING_POPUP) instanceof Popup) {
            return;
        }

        JLabel warningLabel = new JLabel(LABEL_WARNING_SPACE_IN_INPUT);
        warningLabel.setOpaque(true);
        warningLabel.setBackground(SPACE_WARNING_BACKGROUND);
        warningLabel.setForeground(Color.BLACK);
        warningLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SPACE_WARNING_BORDER, 1),
                BorderFactory.createEmptyBorder(UI_PADDING / 2, UI_PADDING, UI_PADDING / 2, UI_PADDING)));

        Point locationOnScreen = textField.getLocationOnScreen();
        int x = locationOnScreen.x;
        int y = locationOnScreen.y + textField.getHeight() + (UI_PADDING / 2);

        Popup popup = PopupFactory.getSharedInstance().getPopup(textField, warningLabel, x, y);
        popup.show();
        textField.putClientProperty(CLIENT_PROPERTY_WARNING_POPUP, popup);
    }

    private static void hideSpaceWarningPopup(JTextField textField) {
        Object popup = textField.getClientProperty(CLIENT_PROPERTY_WARNING_POPUP);
        textField.putClientProperty(CLIENT_PROPERTY_WARNING_POPUP, null);
        if (popup instanceof Popup castPopup) {
            castPopup.hide();
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