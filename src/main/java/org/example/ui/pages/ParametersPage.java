package org.example.ui.pages;

import freemarker.template.TemplateException;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.example.exceptions.DefaultReceiverMissingException;
import org.example.exceptions.TableEmptyException;
import org.example.model.AlternativePartner;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;

import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.example.model.BinaryParameter;
import org.example.model.StringParameter;
import org.example.templates.TemplateInterfaceDetermination;
import org.example.templates.TemplateReceiverDetermination;
import org.example.templates.TemplateCombinedDetermination;
import org.example.ui.components.BackButton;
import org.example.ui.components.EditableHeader;
import org.example.ui.components.KeyButtonGroup;
import org.example.ui.components.KeyPanel;
import org.example.utils.XsltSyntaxValidator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import static org.example.ui.components.LabelTimer.showHttpResponseWithTimer;
import static org.example.utils.SharedData.*;

public class ParametersPage extends JPanel {
    private final JFrame parentFrame;

    private final TemplateReceiverDetermination objectReceiverDetermination = new TemplateReceiverDetermination();
    private final TemplateInterfaceDetermination objectInterfaceDetermination = new TemplateInterfaceDetermination();
    private final TemplateCombinedDetermination objectCombinedDetermination = new TemplateCombinedDetermination();

    private final RTextScrollPane rTextScrollPaneReceiverDetermination;
    private final RTextScrollPane rTextScrollPaneCombinedDetermination;

    private final XsltSyntaxValidator xsltSyntaxValidator = new XsltSyntaxValidator();

    private final String pid;
    private String determinationType;

    private final int defaultGap = 20;

    private final EditableHeader editableHeader;

    public ParametersPage(AlternativePartner alternativePartner, JFrame parentFrame) {
        pid = alternativePartner.getPid();
        this.parentFrame = parentFrame;

        setLayout(new BorderLayout());
        LinkedHashMap<String, String> headerValues = new LinkedHashMap<>();

        headerValues.put(LABEL_AGENCY, alternativePartner.getAgency());
        headerValues.put(LABEL_SCHEME, alternativePartner.getScheme());
        headerValues.put(LABEL_ID_ALTERNATIVE_PARTNERS, alternativePartner.getId());
        headerValues.put(LABEL_PID, pid);

        JPanel headerContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        editableHeader = new EditableHeader(headerValues, true);

        headerContainer.add(editableHeader);
        headerContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(headerContainer, BorderLayout.NORTH);

        rTextScrollPaneReceiverDetermination = initializeRSyntaxTextArea();
        rTextScrollPaneCombinedDetermination = initializeRSyntaxTextArea();
        rTextScrollPaneCombinedDetermination.getTextArea().setText(XSLT_NOT_NULL);

        JTabbedPane tabbedPane = new JTabbedPane();

        try {
            httpRequestHandler.sendGetRequestBinaryParameters(pid);
            httpRequestHandler.sendGetRequestStringParameters(pid);

            determinationType = alternativePartner.getDeterminationType();
            LOGGER.info("Type of Receiver / Interface Determination: {}", determinationType);

            AtomicReference<Set<String>> listReceiverNames = new AtomicReference<>();

            switch (determinationType) {
                case LABEL_COMBINED_XSLT -> {
                    // Combined Determination
                    JPanel panelCombinedDetermination = getPanelCombinedDetermination();
                    tabbedPane.add(LABEL_RECEIVER_INTERFACE_DETERMINATION, panelCombinedDetermination);

                    // String Parameters
                    JPanel panelStringParameters = getPanelStringParameters();
                    tabbedPane.add(LABEL_STRING_PARAMETERS, panelStringParameters);

                    // Landscape Stages
                    JPanel panelLandscapeStages = getPanelLandscapeStages();
                    tabbedPane.add(LABEL_LANDSCAPE_STAGES, panelLandscapeStages);

                    tabbedPane.addChangeListener(e -> {
                        try {
                            int index = tabbedPane.getSelectedIndex();
                            if (index == 0) { // Binary Parameters
                                httpRequestHandler.sendGetRequestBinaryParameters(pid);

                                panelCombinedDetermination.removeAll();
                                panelCombinedDetermination.add(getPanelCombinedDetermination());
                                panelCombinedDetermination.revalidate();
                                panelCombinedDetermination.repaint();
                            } else if (index == 1) { // String Parameters
                                listReceiverNames.set(getListReceiverNamesDependingOnDeterminationType(true));
                                httpRequestHandler.sendGetRequestStringParameters(pid, listReceiverNames.get());

                                panelStringParameters.removeAll();
                                panelStringParameters.add(getPanelStringParameters());
                                panelStringParameters.revalidate();
                                panelStringParameters.repaint();
                            } else if (index == 2) { // Landscape Stages
                                httpRequestHandler.sendGetRequestStringParameterLandscape();
                                httpRequestHandler.sendGetRequestAlternativePartnersLandscape(getSystemNames(true));

                                panelLandscapeStages.removeAll();
                                panelLandscapeStages.add(getPanelLandscapeStages());
                                panelLandscapeStages.revalidate();
                                panelLandscapeStages.repaint();
                            }
                        } catch (Exception ex) {
                            LOGGER.error(ex);
                        }
                    });
                }
                case LABEL_MULTIPLE_XSLTS -> {
                    // Receiver Determination
                    JPanel panelReceiverDetermination = getPanelReceiverDetermination();
                    tabbedPane.add(LABEL_RECEIVER_DETERMINATION, panelReceiverDetermination);

                    // Interface Determination
                    JPanel panelInterfaceDetermination = getPanelInterfaceDetermination();
                    tabbedPane.add(LABEL_INTERFACE_DETERMINATION, panelInterfaceDetermination);

                    // String Parameters
                    JPanel panelStringParameters = getPanelStringParameters();
                    tabbedPane.add(LABEL_STRING_PARAMETERS, panelStringParameters);

                    // Landscape Stages
                    JPanel panelLandscapeStages = getPanelLandscapeStages();
                    tabbedPane.add(LABEL_LANDSCAPE_STAGES, panelLandscapeStages);

                    tabbedPane.addChangeListener(e -> {
                        try {
                            int index = tabbedPane.getSelectedIndex();
                            if (index == 0 || index == 1) { // Binary Parameters
                                httpRequestHandler.sendGetRequestBinaryParameters(pid);
                                if (index == 0) {
                                    panelReceiverDetermination.removeAll();
                                    panelReceiverDetermination.add(getPanelReceiverDetermination());
                                    panelReceiverDetermination.revalidate();
                                    panelReceiverDetermination.repaint();
                                } else {
                                    panelInterfaceDetermination.removeAll();
                                    panelInterfaceDetermination.add(getPanelInterfaceDetermination());
                                    panelInterfaceDetermination.revalidate();
                                    panelInterfaceDetermination.repaint();
                                }
                            } else if (index == 2) { // String Parameters
                                listReceiverNames.set(getListReceiverNamesDependingOnDeterminationType(true));
                                httpRequestHandler.sendGetRequestStringParameters(pid, listReceiverNames.get());

                                panelStringParameters.removeAll();
                                panelStringParameters.add(getPanelStringParameters());
                                panelStringParameters.revalidate();
                                panelStringParameters.repaint();
                            } else if (index == 3) { // Landscape Stages
                                httpRequestHandler.sendGetRequestStringParameterLandscape();
                                httpRequestHandler.sendGetRequestAlternativePartnersLandscape(getSystemNames(true));

                                panelLandscapeStages.removeAll();
                                panelLandscapeStages.add(getPanelLandscapeStages());
                                panelLandscapeStages.revalidate();
                                panelLandscapeStages.repaint();
                            }
                        } catch (Exception ex) {
                            LOGGER.error(ex);
                        }
                    });
                }
                case LABEL_POINT_TO_POINT -> {
                    // Point to Point Determination
                    JPanel pointToPointDetermination = getPanelPointToPointDetermination();
                    tabbedPane.add(LABEL_POINT_TO_POINT_DETERMINATION, pointToPointDetermination);

                    // String Parameters
                    JPanel panelStringParameters = getPanelStringParameters();
                    tabbedPane.add(LABEL_STRING_PARAMETERS, panelStringParameters);

                    // Landscape Stages
                    JPanel panelLandscapeStages = getPanelLandscapeStages();
                    tabbedPane.add(LABEL_LANDSCAPE_STAGES, panelLandscapeStages);

                    tabbedPane.addChangeListener(e -> {
                        try {
                            int index = tabbedPane.getSelectedIndex();
                            if (index == 0) { // Point to Point String Parameters
                                listReceiverNames.set(getListReceiverNamesDependingOnDeterminationType(false));
                                httpRequestHandler.sendGetRequestStringParameters(pid, listReceiverNames.get());

                                pointToPointDetermination.removeAll();
                                pointToPointDetermination.add(getPanelPointToPointDetermination());
                                pointToPointDetermination.revalidate();
                                pointToPointDetermination.repaint();
                            } else if (index == 1) { // String Parameters
                                listReceiverNames.set(getListReceiverNamesDependingOnDeterminationType(false));
                                httpRequestHandler.sendGetRequestStringParameters(pid, listReceiverNames.get());

                                panelStringParameters.removeAll();
                                panelStringParameters.add(getPanelStringParameters());
                                panelStringParameters.revalidate();
                                panelStringParameters.repaint();
                            } else if (index == 2) { // Landscape Stages
                                httpRequestHandler.sendGetRequestStringParameterLandscape();
                                httpRequestHandler.sendGetRequestAlternativePartnersLandscape(getSystemNames(false));

                                panelLandscapeStages.removeAll();
                                panelLandscapeStages.add(getPanelLandscapeStages());
                                panelLandscapeStages.revalidate();
                                panelLandscapeStages.repaint();
                            }
                        } catch (Exception ex) {
                            LOGGER.error(ex);
                        }
                    });
                }
            }

        } catch (Exception e) {
            LOGGER.error(e);
        }

        add(tabbedPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        BackButton backButton = new BackButton(parentFrame);
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private RTextScrollPane initializeRSyntaxTextArea() {
        RSyntaxTextArea valueTextArea = new RSyntaxTextArea();
        valueTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        valueTextArea.setCodeFoldingEnabled(true);
        valueTextArea.setAntiAliasingEnabled(true);
        valueTextArea.setEditable(false);
        try (InputStream themeStream = ParametersPage.class.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/vs.xml")) {
            Theme theme = Theme.load(themeStream);
            theme.apply(valueTextArea);
        } catch (Exception e) {
            LOGGER.error("Error while loading theme to display XSLT: " + e);
        }

        RTextScrollPane rTextScrollPane = new RTextScrollPane(valueTextArea);
        rTextScrollPane.setFoldIndicatorEnabled(true);

        return rTextScrollPane;
    }

    private void showUpdatedXslt(RTextScrollPane rTextScrollPane, String rawXslt) {
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setIndentSize(4);
        format.setSuppressDeclaration(false);
        format.setEncoding("UTF-8");
        String xsltToDisplay;

        try {
            Document document = DocumentHelper.parseText(rawXslt);
            StringWriter stringWriter = new StringWriter();
            XMLWriter writer = new XMLWriter(stringWriter, format);
            writer.write(document);
            xsltToDisplay = String.valueOf(stringWriter).replace("\n\n", "\n");
        } catch (Exception e) {
            xsltToDisplay = rawXslt;
        }

        if (xsltToDisplay.endsWith("\n")) {
            xsltToDisplay = xsltToDisplay.substring(0, xsltToDisplay.length() - 1);
        }

        rTextScrollPane.getTextArea().setText(xsltToDisplay);
    }

    private KeyPanel showTableReceiverDetermination(TemplateReceiverDetermination templateReceiverDetermination, boolean showXsltButtons) {
        KeyPanel keyPanel = new KeyPanel(new BorderLayout());

        // radio buttons
        JPanel jPanelRadioButtonsWithLabel = new JPanel(new GridLayout(2, 1));

        JLabel jLabel = new JLabel(colon(LABEL_RECEIVER_NOT_FOUND));
        jPanelRadioButtonsWithLabel.add(jLabel);

        KeyButtonGroup buttonGroup = new KeyButtonGroup();
        keyPanel.addNewComponent(COMPONENT_RECEIVER_NOT_FOUND, buttonGroup);

        String[] labelRadioButtons = {LABEL_ERROR, LABEL_IGNORE, LABEL_DEFAULT};
        String typeSelected = templateReceiverDetermination.getType();

        JTextField defaultReceiverTextField = new JTextField(templateReceiverDetermination.getDefaultReceiver(), UI_TEXT_FIELD_COLUMNS);
        keyPanel.addNewComponent(COMPONENT_RECEIVER_DEFAULT, defaultReceiverTextField);

        JPanel jPanelRadioButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        for (String labelRadioButton : labelRadioButtons) {
            JRadioButton radioButton = new JRadioButton(labelRadioButton);
            radioButton.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (labelRadioButton.equals(LABEL_DEFAULT)) {
                        defaultReceiverTextField.requestFocus();
                    } else {
                        defaultReceiverTextField.setText("");
                    }
                }
            });
            buttonGroup.add(radioButton);
            buttonGroup.addNewButton(labelRadioButton, radioButton);
            jPanelRadioButtons.add(radioButton);
            if (labelRadioButton.equals(typeSelected)) {
                radioButton.setSelected(true);
            }
        }

        JLabel defaultReceiverLabel = new JLabel(colon(LABEL_RECEIVER_COMPONENT));
        jPanelRadioButtons.add(defaultReceiverLabel);

        jPanelRadioButtons.add(defaultReceiverTextField);

        jPanelRadioButtonsWithLabel.add(jPanelRadioButtons);

        keyPanel.add(jPanelRadioButtonsWithLabel, BorderLayout.NORTH);

        // table
        DefaultTableModel tableModel = new DefaultTableModel(new Object[]{LABEL_CONDITION, LABEL_RECEIVER_COMPONENT}, 0);
        JTable table = new JTable(tableModel);
        table.setName(ID_RECEIVER_DETERMINATION);
        table.getTableHeader().setReorderingAllowed(false);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        keyPanel.addNewComponent(COMPONENT_RECEIVER_TABLE_MODEL, tableModel);
        keyPanel.addNewComponent(COMPONENT_RECEIVER_TABLE, table);

        String[][] dataToInsert = templateReceiverDetermination.getHashMapConditionReceiverForTable();
        for (
                String[] strings : dataToInsert) {
            tableModel.addRow(strings);
        }

        table.setGridColor(Color.BLACK);

        JScrollPane tableScrollPane = new JScrollPane(table);
        keyPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel;
        if (showXsltButtons) {
            buttonPanel = new JPanel(new GridLayout(2, 1));

            JPanel buttonPanelTop = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanelTop.add(getAddRowButton(table, tableModel));
            buttonPanelTop.add(getMoveRowUpButton(table, tableModel));
            buttonPanelTop.add(getMoveRowDownButton(table, tableModel));
            buttonPanelTop.add(getDeleteButton(table, tableModel));
            buttonPanelTop.add(getGenerateXsltButtonReceiverDetermination(table, buttonGroup, defaultReceiverTextField));

            JPanel buttonPanelBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanelBottom.add(getSendButtonBinaryParameters(currentReceiverDetermination));

            buttonPanel.add(buttonPanelTop);
            buttonPanel.add(buttonPanelBottom);

        } else {
            buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton updateButton = getUpdateReceiversButton();
            buttonPanel.add(updateButton);
            keyPanel.addNewComponent(LABEL_UPDATE_RECEIVERS, updateButton);

            buttonPanel.add(getAddRowButton(table, tableModel));
            buttonPanel.add(getMoveRowUpButton(table, tableModel));
            buttonPanel.add(getMoveRowDownButton(table, tableModel));
            buttonPanel.add(getDeleteButton(table, tableModel));

        }
        keyPanel.add(buttonPanel, BorderLayout.SOUTH);

        return keyPanel;
    }

    private JPanel showDropdownInterfaceDetermination(JPanel cards, CardLayout cardLayout, Set<String> setReceiverNames) {
        JPanel panelDropdown = new JPanel();

        JComboBox<String> comboBox = new JComboBox<>();

        comboBox.addActionListener(e -> {
            JComboBox<?> source = (JComboBox<?>) e.getSource();
            String selectedItem = (String) source.getSelectedItem();
            if (selectedItem != null) {
                cardLayout.show(cards, selectedItem);
            }
        });

        if (!setReceiverNames.isEmpty()) {
            for (String receiverName : setReceiverNames) {
                comboBox.addItem(receiverName);
            }

            comboBox.setSelectedIndex(0);

            String firstItem = (String) comboBox.getSelectedItem();
            cardLayout.show(cards, firstItem);
        }

        panelDropdown.add(comboBox);
        return panelDropdown;
    }

    private KeyPanel showTableInterfaceDetermination(TemplateInterfaceDetermination templateInterfaceDetermination, RTextScrollPane rTextScrollPane, BinaryParameter currentInterfaceDetermination, boolean showXsltButtons) {
        DefaultTableModel tableModel = new DefaultTableModel(new Object[]{LABEL_CONDITION, LABEL_RECEIVER_INTERFACE}, 0);
        JTable table = new JTable(tableModel);
        table.setName(currentInterfaceDetermination.getId());
        table.getTableHeader().setReorderingAllowed(false);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        String[][] dataToInsert = templateInterfaceDetermination.getHashMapConditionInterfaceForTable();
        for (String[] strings : dataToInsert) {
            tableModel.addRow(strings);
        }

        table.setGridColor(Color.BLACK);

        KeyPanel keyPanel = new KeyPanel(new BorderLayout());
        keyPanel.addNewComponent(COMPONENT_INTERFACE_TABLE, table);

        JScrollPane tableScrollPane = new JScrollPane(table);
        keyPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel;
        if (showXsltButtons) {
            buttonPanel = new JPanel(new GridLayout(2, 1));

            JPanel buttonPanelTop = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanelTop.add(getAddRowButton(table, tableModel));
            buttonPanelTop.add(getMoveRowUpButton(table, tableModel));
            buttonPanelTop.add(getMoveRowDownButton(table, tableModel));
            buttonPanelTop.add(getDeleteButton(table, tableModel));
            buttonPanelTop.add(getGenerateXsltButtonInterfaceDetermination(table, rTextScrollPane, currentInterfaceDetermination));

            JPanel buttonPanelBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanelBottom.add(getSendButtonBinaryParameters(currentInterfaceDetermination));

            buttonPanel.add(buttonPanelTop);
            buttonPanel.add(buttonPanelBottom);
        } else {
            buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(getAddRowButton(table, tableModel));
            buttonPanel.add(getMoveRowUpButton(table, tableModel));
            buttonPanel.add(getMoveRowDownButton(table, tableModel));
            buttonPanel.add(getDeleteButton(table, tableModel));
        }
        keyPanel.add(buttonPanel, BorderLayout.SOUTH);

        return keyPanel;
    }

    private JPanel showTableStringParameters() {
        JPanel jPanel = new JPanel(new BorderLayout());

        JPanel browserButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton openBrowserButton = new JButton("Open documentation in browser");

        openBrowserButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(LINK_DOCUMENTATION_STRING_PARAMETERS));
            } catch (IOException | URISyntaxException ex) {
                String errorMessage = LABEL_ERROR_WHEN_OPENING_BROWSER + LINK_DOCUMENTATION_STRING_PARAMETERS;
                LOGGER.error(errorMessage);
                showErrorDialog(LABEL_ERROR, errorMessage);
            }

        });
        browserButtonPanel.add(openBrowserButton);
        jPanel.add(browserButtonPanel, BorderLayout.NORTH);

        Map<String, JTextField> supportedStringParameterList = new LinkedHashMap<>();

        JPanel panelStringParameters = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

        Set<String> listReceiverNames = new LinkedHashSet<>();
        if (determinationType.equals(LABEL_POINT_TO_POINT)) {
            try {
                listReceiverNames.add(currentStringParametersList.get(ID_RECEIVER_DETERMINATION).getValue());
            } catch (Exception e) {
                // LOGGER.error(e);
            }
        } else {
            try {
                listReceiverNames = currentReceiverDetermination.getSetReceiverNames();
            } catch (Exception e) {
                // LOGGER.error(e);
            }
        }

        List<String> stringParameterLabelList = new ArrayList<>();

        Collections.addAll(stringParameterLabelList, STRING_PARAMETER_IDS_ARRAY);

        for (String receiverName : listReceiverNames) {
            stringParameterLabelList.add(STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE + " for Receiver \"" + receiverName + "\"");
        }

        for (int i = 0; i < stringParameterLabelList.size(); i++) {
            String stringParameterLabel = stringParameterLabelList.get(i);
            String stringParameterId;
            if (stringParameterLabel.startsWith(STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE)) {
                stringParameterId = STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE + stringParameterLabel.replaceAll(".*\"(.*?)\".*", "$1"); // extract receiver for id
            } else if (stringParameterLabel.startsWith(STRING_PARAMETER_LABEL_DATA_STORE)) {
                stringParameterId = stringParameterLabel.replace(STRING_PARAMETER_LABEL_DATA_STORE, "");
            } else {
                stringParameterId = stringParameterLabel;
            }

            String stringParameterValue;
            try {
                stringParameterValue = currentStringParametersList.get(stringParameterId).getValue();
            } catch (Exception e) {
                stringParameterValue = "";
            }

            JPanel panelLabelStringParameter = createFixedSizePanel(new JLabel(colon(stringParameterLabel)), 400);
            JTextField textFieldStringParameter = new JTextField(stringParameterValue, DEFAULT_COLUMNS_TEXT_FIELD);

            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.anchor = GridBagConstraints.LINE_END;
            panelStringParameters.add(panelLabelStringParameter, gbc);

            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.LINE_START;
            panelStringParameters.add(textFieldStringParameter, gbc);

            supportedStringParameterList.put(stringParameterId, textFieldStringParameter);
        }

        JScrollPane scrollPaneStringParameters = new JScrollPane(panelStringParameters);
        scrollPaneStringParameters.setBorder(null);
        scrollPaneStringParameters.getVerticalScrollBar().setUnitIncrement(10);
        jPanel.add(scrollPaneStringParameters, BorderLayout.CENTER);

        JPanel buttonPanelSend = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel httpResponseLabel = new JLabel();
        buttonPanelSend.add((httpResponseLabel));
        buttonPanelSend.add(getSendButtonStringParameters(supportedStringParameterList, httpResponseLabel));

        jPanel.add(buttonPanelSend, BorderLayout.SOUTH);

        return jPanel;
    }

    private JPanel showViewLandscapeStages() {
        JPanel landscapePanel = new JPanel(new BorderLayout());

        if (currentLandscapeTenantParameters.isEmpty()) {
            JPanel panel = new JPanel();
            panel.add(new JLabel(LABEL_MAINTAIN_LANDSCAPE_FIRST));
            landscapePanel.add(panel, BorderLayout.CENTER);
            return landscapePanel;
        }

        Set<String> systemNames = getSystemNames(false);
        JComboBox<String> dropdown = new JComboBox<>(systemNames.toArray(new String[0]));
        JPanel dropdownPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        dropdownPanel.add(dropdown);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JRadioButton businessSystemNameButton = new JRadioButton(SCHEME_BUSINESS_SYSTEM_NAME, true);
        JRadioButton logicalSystemNameButton = new JRadioButton(SCHEME_LOGICAL_SYSTEM_NAME);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(businessSystemNameButton);
        buttonGroup.add(logicalSystemNameButton);

        radioPanel.add(businessSystemNameButton);
        radioPanel.add(logicalSystemNameButton);

        JPanel selectionPanel = new JPanel(new GridLayout(2, 1));
        selectionPanel.add(dropdownPanel);
        selectionPanel.add(radioPanel);
        landscapePanel.add(selectionPanel, BorderLayout.NORTH);

        KeyPanel contentPanel = new KeyPanel(new GridBagLayout());
        landscapePanel.add(contentPanel, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(getSendButtonLandscape(contentPanel, dropdown, buttonGroup));
        landscapePanel.add(buttonPanel, BorderLayout.SOUTH);

        try {
            String initialSystemAlias = (String) dropdown.getSelectedItem();
            updateLandscapeContent(contentPanel, gbc, initialSystemAlias, SCHEME_BUSINESS_SYSTEM_NAME);

            businessSystemNameButton.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedAlias = (String) dropdown.getSelectedItem();
                    updateLandscapeContent(contentPanel, gbc, selectedAlias, SCHEME_BUSINESS_SYSTEM_NAME);
                }
            });

            logicalSystemNameButton.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedAlias = (String) dropdown.getSelectedItem();
                    updateLandscapeContent(contentPanel, gbc, selectedAlias, SCHEME_LOGICAL_SYSTEM_NAME);
                }
            });

            dropdown.addActionListener(e -> {
                String selectedAlias = (String) dropdown.getSelectedItem();
                String scheme = getSelectedButtonText(buttonGroup);
                updateLandscapeContent(contentPanel, gbc, selectedAlias, scheme);
            });
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return landscapePanel;
    }

    private void updateLandscapeContent(KeyPanel panel, GridBagConstraints gbc, String systemAlias, String scheme) {
        panel.removeAll();
        int rowIndex = 0;
        for (Map.Entry<String, String> landscapeTenantEntry : currentLandscapeTenantParameters.entrySet()) {
            String stage = landscapeTenantEntry.getValue();

            String initialSystemName = currentLandscapeScenarioParameters.stream()
                    .filter(obj -> obj.getAgency().equals(stage)
                            && obj.getScheme().equals(scheme)
                            && obj.getPid().equals(systemAlias))
                    .map(AlternativePartner::getId)
                    .findFirst()
                    .orElse("");

            JTextField systemNameField = new JTextField(initialSystemName, 20);

            String uiComponentKey = scheme + "-" + stage + "-" + systemAlias;
            panel.addNewComponent(uiComponentKey, systemNameField);

            JLabel stageLabel = new JLabel(stage);
            stageLabel.setPreferredSize(new Dimension(100, stageLabel.getPreferredSize().height));

            JLabel httpLabel = new JLabel("");
            panel.addNewComponent(uiComponentKey + COMPONENT_SUFFIX_HTTP_LABEL, httpLabel);

            gbc.gridx = 0;
            gbc.gridy = rowIndex;
            panel.add(systemNameField, gbc);

            gbc.gridx = 1;
            panel.add(stageLabel, gbc);

            gbc.gridx = 2;
            panel.add(httpLabel, gbc);

            rowIndex++;
        }
        panel.revalidate();
        panel.repaint();
    }

    private JButton getSendButtonLandscape(KeyPanel keyPanel, JComboBox<String> dropdown, ButtonGroup buttonGroup) {
        JButton sendButton = new JButton(LABEL_SEND_CHANGES_TO_API);
        sendButton.addActionListener(e -> {
            String selectedSystemNameAlias = (String) dropdown.getSelectedItem();
            String selectedScheme = getSelectedButtonText(buttonGroup);

            for (String componentKey : keyPanel.getAllKeys()) {
                if (componentKey.startsWith(selectedScheme + "-") && componentKey.endsWith("-" + selectedSystemNameAlias)) {
                    JTextField keyField = (JTextField) keyPanel.getComponent(componentKey);

                    String stage = componentKey.split("-")[1];
                    String currentSystemName = keyField.getText();

                    String oldSystemName = currentLandscapeScenarioParameters.stream()
                            .filter(partner -> partner.getScheme().equals(selectedScheme)
                                    && partner.getAgency().equals(stage)
                                    && partner.getPid().equals(selectedSystemNameAlias))
                            .map(AlternativePartner::getId)
                            .findFirst()
                            .orElse("");

                    try {
                        if (!oldSystemName.isEmpty() && currentSystemName.isEmpty()) {
                            String httpResponse = httpRequestHandler.sendDeleteRequestAlternativePartners(stage, selectedScheme, oldSystemName);
                            showHttpResponseWithTimer((JLabel) keyPanel.getComponent(componentKey + COMPONENT_SUFFIX_HTTP_LABEL), httpResponse);

                            currentLandscapeScenarioParameters.removeIf(partner ->
                                    partner.getScheme().equals(selectedScheme)
                                            && partner.getAgency().equals(stage)
                                            && partner.getId().equals(oldSystemName)
                                            && partner.getPid().equals(selectedSystemNameAlias));

                        } else if (!oldSystemName.isEmpty() && !currentSystemName.isEmpty() && !oldSystemName.equals(currentSystemName)) {
                            httpRequestHandler.sendDeleteRequestAlternativePartners(stage, selectedScheme, oldSystemName);
                            String httpResponse = httpRequestHandler.sendPostRequestAlternativePartners(stage, selectedScheme, currentSystemName, selectedSystemNameAlias);
                            showHttpResponseWithTimer((JLabel) keyPanel.getComponent(componentKey + COMPONENT_SUFFIX_HTTP_LABEL), httpResponse);

                            currentLandscapeScenarioParameters.removeIf(partner ->
                                    partner.getScheme().equals(selectedScheme)
                                            && partner.getAgency().equals(stage)
                                            && partner.getId().equals(oldSystemName)
                                            && partner.getPid().equals(selectedSystemNameAlias));
                            currentLandscapeScenarioParameters.add(new AlternativePartner(stage, selectedScheme, currentSystemName, selectedSystemNameAlias));

                        } else if (oldSystemName.isEmpty() && !currentSystemName.isEmpty()) {
                            String httpResponse = httpRequestHandler.sendPostRequestAlternativePartners(stage, selectedScheme, currentSystemName, selectedSystemNameAlias);
                            showHttpResponseWithTimer((JLabel) keyPanel.getComponent(componentKey + COMPONENT_SUFFIX_HTTP_LABEL), httpResponse);

                            currentLandscapeScenarioParameters.add(new AlternativePartner(stage, selectedScheme, currentSystemName, selectedSystemNameAlias));
                        }
                    } catch (Exception exception) {
                        //
                    }
                }
            }
        });
        return sendButton;
    }

    private boolean areAllSystemCellsEmpty(JTable table) {
        TableModel model = table.getModel();
        int rowCount = model.getRowCount();
        int receiverSystemColumnIndex = -1;
        int receiverInterfaceColumnIndex = -1;

        for (int col = 0; col < model.getColumnCount(); col++) {
            String columnName = model.getColumnName(col);
            if (LABEL_RECEIVER_COMPONENT.equals(columnName)) {
                receiverSystemColumnIndex = col;
            } else if (LABEL_RECEIVER_INTERFACE.equals(columnName)) {
                receiverInterfaceColumnIndex = col;
            }
        }

        for (int row = 0; row < rowCount; row++) {
            if (receiverSystemColumnIndex != -1) {
                Object value = model.getValueAt(row, receiverSystemColumnIndex);
                if (value != null && !value.toString().trim().isEmpty()) {
                    return false;
                }
            }
            if (receiverInterfaceColumnIndex != -1) {
                Object value = model.getValueAt(row, receiverInterfaceColumnIndex);
                if (value != null && !value.toString().trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private JButton getUpdateReceiversButton() {
        return new JButton(LABEL_UPDATE_RECEIVERS);
    }

    private JButton getAddRowButton(JTable table, DefaultTableModel tableModel) {
        JButton addButton = new JButton(LABEL_ADD_ROW);
        addButton.addActionListener(e -> {
            tableModel.addRow(new Object[]{"", ""});
            int newRowCount = tableModel.getRowCount() - 1;
            table.setRowSelectionInterval(newRowCount, newRowCount);
        });
        return addButton;
    }

    private JButton getMoveRowButton(JTable table, DefaultTableModel tableModel, boolean moveUp) {
        String label;
        int move;
        if (moveUp) {
            label = LABEL_MOVE_ROW_UP;
            move = -1;
        } else {
            label = LABEL_MOVE_ROW_DOWN;
            move = 1;
        }
        JButton moveButton = new JButton(label);
        moveButton.addActionListener(e -> {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            int selectedRowIndex = table.getSelectedRow();
            int selectedRowNewIndex = selectedRowIndex + move;
            int totalNumberOfRows = tableModel.getRowCount();
            if (selectedRowIndex > -1 && selectedRowNewIndex > -1 && selectedRowIndex < totalNumberOfRows && selectedRowNewIndex < totalNumberOfRows) {
                tableModel.moveRow(selectedRowIndex, selectedRowIndex, selectedRowNewIndex);
                table.setRowSelectionInterval(selectedRowNewIndex, selectedRowNewIndex);
            }
        });
        return moveButton;
    }

    private JButton getMoveRowUpButton(JTable table, DefaultTableModel tableModel) {
        return getMoveRowButton(table, tableModel, true);
    }

    private JButton getMoveRowDownButton(JTable table, DefaultTableModel tableModel) {
        return getMoveRowButton(table, tableModel, false);
    }

    private JButton getDeleteButton(JTable table, DefaultTableModel tableModel) {
        JButton removeButton = new JButton(LABEL_DELETE_ROW);

        removeButton.addActionListener(e -> {
            int rowCount = tableModel.getRowCount();
            int selectedRow = table.getSelectedRow();

            if (selectedRow != -1 && rowCount > 1) {
                tableModel.removeRow(selectedRow);
            } else if (rowCount == 1) {
                showErrorDialog(LABEL_WARNING, LABEL_WARNING_DELETE_LAST_ROW);
            }
        });

        return removeButton;
    }

    private JButton getSendButtonStringParameters(Map<String, JTextField> updatedStringParametersList, JLabel jLabel) {
        JButton sendButton = new JButton(LABEL_SEND_CHANGES_TO_API);
        sendButton.addActionListener(e -> {
            for (String stringParameterIdMap : updatedStringParametersList.keySet()) {
                String stringParameterId;
                String stringParameterPid;
                if (stringParameterIdMap.startsWith(STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE)) {
                    String receiverName = stringParameterIdMap.replace(STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE, "");
                    stringParameterId = STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE;
                    stringParameterPid = receiverName;
                } else {
                    stringParameterId = stringParameterIdMap;
                    stringParameterPid = pid;
                }
                String stringParameterValue = updatedStringParametersList.get(stringParameterIdMap).getText();
                StringParameter stringParameterObject = new StringParameter(stringParameterPid, stringParameterId, stringParameterValue);

                StringParameter existingParameter = currentStringParametersList.get(stringParameterIdMap);

                String httpResponse = null;
                try {
                    if (existingParameter != null) {
                        if (stringParameterValue.isEmpty()) {
                            httpResponse = httpRequestHandler.sendDeleteRequestStringParameters(stringParameterPid, stringParameterId);
                            currentStringParametersList.remove(stringParameterIdMap);
                        } else if (!stringParameterValue.equals(existingParameter.getValue())) {
                            httpResponse = httpRequestHandler.sendPutRequestStringParameters(stringParameterPid, stringParameterId, stringParameterValue);
                            currentStringParametersList.replace(stringParameterIdMap, stringParameterObject);
                        }
                    } else {
                        if (!stringParameterValue.isEmpty()) {
                            httpResponse = httpRequestHandler.sendPostRequestStringParameters(stringParameterPid, stringParameterId, stringParameterValue);
                            currentStringParametersList.put(stringParameterIdMap, stringParameterObject);
                        }
                    }
                    if (httpResponse != null) {
                        showHttpResponseWithTimer(jLabel, httpResponse);
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex);
                }
            }
        });
        return sendButton;
    }

    private JButton getGenerateXsltButtonCombinedDetermination(KeyPanel receiverPanel, KeyPanel interfacePanel) {
        ButtonGroup buttonGroupReceiverNotFound = (ButtonGroup) receiverPanel.getComponent(COMPONENT_RECEIVER_NOT_FOUND);
        JTable tableReceiver = (JTable) receiverPanel.getComponent(COMPONENT_RECEIVER_TABLE);
        JTextField textFieldDefaultReceiver = (JTextField) receiverPanel.getComponent(COMPONENT_RECEIVER_DEFAULT);

        Map<String, JTable> mapTablesInterfaces = (Map<String, JTable>) interfacePanel.getComponent(COMPONENT_INTERFACE_TABLE);

        JButton generateXsltButton = new JButton(LABEL_GENERATE_XSLT);
        generateXsltButton.addActionListener(e -> {
            objectCombinedDetermination.clear();

            // receiver determination
            if (tableReceiver.isEditing()) {
                tableReceiver.getCellEditor().stopCellEditing();
            }
            tableReceiver.clearSelection();

            objectCombinedDetermination.receiverDetermination.setType(getSelectedButtonText(buttonGroupReceiverNotFound));
            objectCombinedDetermination.receiverDetermination.setDefaultReceiver(textFieldDefaultReceiver.getText());
            for (int i = 0; i < tableReceiver.getRowCount(); i++) {
                String condition = (String) tableReceiver.getValueAt(i, 0);
                String receiverComponent = (String) tableReceiver.getValueAt(i, 1);
                objectCombinedDetermination.receiverDetermination.setHashMapConditionReceiver(condition, receiverComponent);
            }

            try {
                // default receiver
                if (Objects.equals(getSelectedButtonText(buttonGroupReceiverNotFound), LABEL_DEFAULT) && textFieldDefaultReceiver.getText().isEmpty()) {
                    throw new DefaultReceiverMissingException();
                }

                // receiver determination
                if (areAllSystemCellsEmpty(tableReceiver)) {
                    throw new TableEmptyException(tableReceiver.getName());
                }

                // interface determinations
                for (Map.Entry<String, JTable> entry : mapTablesInterfaces.entrySet()) {
                    if (!entry.getKey().isEmpty()) {
                        JTable tableInterface = entry.getValue();
                        if (tableInterface.isEditing()) {
                            tableInterface.getCellEditor().stopCellEditing();
                        }
                        tableInterface.clearSelection();

                        if (areAllSystemCellsEmpty(tableInterface)) {
                            throw new TableEmptyException(tableInterface.getName());
                        }

                        for (int i = 0; i < tableInterface.getRowCount(); i++) {
                            String condition = (String) tableInterface.getValueAt(i, 0);
                            String interfaceName = (String) tableInterface.getValueAt(i, 1);
                            objectCombinedDetermination.mapInterfaceDeterminations.putIfAbsent(entry.getKey(), new TemplateInterfaceDetermination());
                            objectCombinedDetermination.mapInterfaceDeterminations.get(entry.getKey()).setHashMapConditionService(condition, interfaceName);
                        }
                    }
                }

                String xslt = xsltHandler.handleXslt(ID_COMBINED_DETERMINATION, objectCombinedDetermination);
                showUpdatedXslt(this.rTextScrollPaneCombinedDetermination, xslt);
                currentReceiverDetermination.setValue(xslt);
                showDialogInvalidXslt(xslt);
            } catch (Exception ex) {
                handleExceptionXsltGeneration(ex);
            }
        });

        return generateXsltButton;
    }

    private JButton getGenerateXsltButtonReceiverDetermination(JTable table, ButtonGroup buttonGroup, JTextField jTextField) {
        JButton generateXsltButton = new JButton(LABEL_GENERATE_XSLT);
        generateXsltButton.addActionListener(e -> {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            table.clearSelection();

            try {
                if (Objects.equals(getSelectedButtonText(buttonGroup), LABEL_DEFAULT) && jTextField.getText().isEmpty()) {
                    throw new DefaultReceiverMissingException();
                }

                if (areAllSystemCellsEmpty(table)) {
                    throw new TableEmptyException(table.getName());
                }

                objectReceiverDetermination.clear();

                String defaultBehavior = getSelectedButtonText(buttonGroup);
                objectReceiverDetermination.setType(defaultBehavior);
                objectReceiverDetermination.setDefaultReceiver(jTextField.getText());
                for (int i = 0; i < table.getRowCount(); i++) {
                    String condition = (String) table.getValueAt(i, 0);
                    String receiverComponent = (String) table.getValueAt(i, 1);
                    objectReceiverDetermination.setHashMapConditionReceiver(condition, receiverComponent);
                }

                String xslt = xsltHandler.handleXslt(ID_RECEIVER_DETERMINATION, objectReceiverDetermination);
                showUpdatedXslt(this.rTextScrollPaneReceiverDetermination, xslt);
                currentReceiverDetermination.setValue(xslt);
                showDialogInvalidXslt(xslt);
            } catch (Exception ex) {
                handleExceptionXsltGeneration(ex);
            }
        });
        return generateXsltButton;
    }

    private JButton getGenerateXsltButtonInterfaceDetermination(JTable table, RTextScrollPane rTextScrollPane, BinaryParameter currentInterfaceDetermination) {
        JButton generateXsltButton = new JButton(LABEL_GENERATE_XSLT);
        generateXsltButton.addActionListener(e -> {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            table.clearSelection();

            try {
                if (areAllSystemCellsEmpty(table)) {
                    throw new TableEmptyException(table.getName());
                }

                objectInterfaceDetermination.clear();

                for (int i = 0; i < table.getRowCount(); i++) {
                    String condition = (String) table.getValueAt(i, 0);
                    String combined = (String) table.getValueAt(i, 1);
                    objectInterfaceDetermination.setHashMapConditionService(condition, combined);
                }

                String xslt = xsltHandler.handleXslt(ID_INTERFACE_DETERMINATION, objectInterfaceDetermination);
                showUpdatedXslt(rTextScrollPane, xslt);
                currentInterfaceDetermination.setValue(xslt);
                showDialogInvalidXslt(xslt);
            } catch (IOException | TemplateException ex) {
                LOGGER.error(ex);
            } catch (TableEmptyException ex) {
                String errorMessage = LABEL_ERROR_WHEN_GENERATING_TABLE + ex.getTableName();
                LOGGER.error(errorMessage);
                showErrorDialog(LABEL_ERROR, errorMessage);
            }
        });
        return generateXsltButton;
    }

    private JPanel getSendButtonBinaryParameters(BinaryParameter binaryParameter) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton sendButton = new JButton(LABEL_SEND_XSLT_TO_API);
        JLabel responseLabel = new JLabel();
        sendButton.addActionListener(e -> {
            try {
                boolean sendToApi = true;
                String resultXsltValidation = validateXsltSyntax(binaryParameter.getValue());

                if (!resultXsltValidation.isEmpty()) { // if xslt contains errors -> show warning
                    String[] options = {LABEL_SEND_ANYWAY, LABEL_CANCEL};

                    int option = JOptionPane.showOptionDialog(
                            null,
                            colon(LABEL_SHOWN_XSLT_INVALID_SYNTAX) + "\n" + resultXsltValidation + "\n" + LABEL_SEND_ANYWAY_QUESTION,
                            LABEL_CONFIRMATION,
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[1]
                    );

                    sendToApi = option == 0; // option 0 = send; option 1 = cancel
                }

                if (sendToApi) {
                    String httpResponsePut = httpRequestHandler.sendPutRequestBinaryParameters(pid, binaryParameter.getId(), binaryParameter.getValue());
                    showHttpResponseWithTimer(responseLabel, httpResponsePut);
                }
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
        });
        if (determinationType.equals(LABEL_COMBINED_XSLT)) {
            buttonPanel.add(sendButton);
            buttonPanel.add(responseLabel);
        } else {
            buttonPanel.add(responseLabel);
            buttonPanel.add(sendButton);
        }

        add(buttonPanel);
        return buttonPanel;
    }

    private String getSelectedButtonText(ButtonGroup buttonGroup) {
        for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements(); ) {
            AbstractButton button = buttons.nextElement();

            if (button.isSelected()) {
                return button.getText();
            }
        }

        return null;
    }

    private void showErrorDialog(String typeOfDialog, String errorMessage) {
        int messageType;
        if (typeOfDialog.equals(LABEL_ERROR)) {
            messageType = JOptionPane.ERROR_MESSAGE;
        } else {
            messageType = JOptionPane.WARNING_MESSAGE;
        }
        JOptionPane.showMessageDialog(parentFrame, errorMessage, typeOfDialog, messageType);
    }

    private JPanel getPanelCombinedDetermination() {
        JPanel panelCombinedDetermination = new JPanel(new GridLayout(1, 2, defaultGap, 0));

        JPanel panelTables = new JPanel(new GridLayout(2, 1, 0, defaultGap));

        try {
            objectCombinedDetermination.xsltToObjectCombinedDetermination(currentReceiverDetermination.getValueNotEmpty());
        } catch (Exception e) {
            LOGGER.error(e);
        }

        // Receiver Determination
        KeyPanel panelReceiverDetermination = showTableReceiverDetermination(objectCombinedDetermination.receiverDetermination, false);
        panelTables.add(panelReceiverDetermination);
        KeyButtonGroup buttonGroupReceiverNotFound = (KeyButtonGroup) panelReceiverDetermination.getComponent(COMPONENT_RECEIVER_NOT_FOUND);
        JTextField defaultReceiverTextField = (JTextField) panelReceiverDetermination.getComponent(COMPONENT_RECEIVER_DEFAULT);

        // Interface Determination
        Map<String, JTable> tablesInterfaceDeterminations = new LinkedHashMap<>();
        KeyPanel panelInterfaceDetermination = new KeyPanel(new BorderLayout());

        CardLayout cardLayoutInterfaceDetermination = new CardLayout();
        KeyPanel cardsInterfaceDetermination = new KeyPanel(cardLayoutInterfaceDetermination);

        Set<String> setReceiverNames = new LinkedHashSet<>(objectCombinedDetermination.mapInterfaceDeterminations.keySet());

        for (String receiverName : setReceiverNames) {
            try {
                BinaryParameter binaryParameter = currentInterfaceDeterminationsList.computeIfAbsent(receiverName, key -> new BinaryParameter(pid, ID_INTERFACE_DETERMINATION_ + key, JSON_VALUE_XSL, currentReceiverDetermination.getValue()));

                String xsltInterface = binaryParameter.getValueNotEmpty();
                objectCombinedDetermination.mapInterfaceDeterminations.get(receiverName).xsltToObjectInterfaceDetermination(xsltInterface, receiverName);

                JPanel cardPanel = new JPanel(getGridLayout());

                KeyPanel paneLabelInterfaceDetermination = showTableInterfaceDetermination(objectCombinedDetermination.mapInterfaceDeterminations.get(receiverName), null, binaryParameter, false);
                tablesInterfaceDeterminations.put(receiverName, (JTable) paneLabelInterfaceDetermination.getComponent(COMPONENT_INTERFACE_TABLE));
                cardPanel.add(paneLabelInterfaceDetermination);

                cardsInterfaceDetermination.add(cardPanel, receiverName);
                cardsInterfaceDetermination.addNewComponent(receiverName, cardPanel);
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
        }

        panelInterfaceDetermination.addNewComponent(COMPONENT_INTERFACE_TABLE, tablesInterfaceDeterminations);

        JPanel radioButtonsInterfaces = showDropdownInterfaceDetermination(cardsInterfaceDetermination, cardLayoutInterfaceDetermination, setReceiverNames);

        panelInterfaceDetermination.add(radioButtonsInterfaces, BorderLayout.NORTH);
        panelInterfaceDetermination.add(cardsInterfaceDetermination, BorderLayout.CENTER);

        panelTables.add(panelInterfaceDetermination);

        // Listeners
        DefaultTableModel receiverTableModel = (DefaultTableModel) panelReceiverDetermination.getComponent(COMPONENT_RECEIVER_TABLE_MODEL);
        JTable receiverTable = (JTable) panelReceiverDetermination.getComponent(COMPONENT_RECEIVER_TABLE);

        class UpdateHandler {
            void updateReceivers() {
                setReceiverNames.clear();

                String defaultReceiver = defaultReceiverTextField.getText();
                if (!defaultReceiver.isEmpty()) {
                    setReceiverNames.add(defaultReceiver);
                }

                for (int i = 0; i < receiverTableModel.getRowCount(); i++) {
                    String receiverName = ((String) receiverTableModel.getValueAt(i, 1));
                    if (!receiverName.isEmpty()) {
                        setReceiverNames.add(receiverName);
                    }
                }

                Set<String> cardNamesInterfaceDetermination = cardsInterfaceDetermination.getAllKeys();

                for (String receiverName : setReceiverNames) {
                    if (!cardNamesInterfaceDetermination.contains(receiverName)) {
                        BinaryParameter binaryParameter = currentInterfaceDeterminationsList.computeIfAbsent(receiverName, key -> new BinaryParameter(pid, ID_INTERFACE_DETERMINATION_ + key, JSON_VALUE_XSL, currentReceiverDetermination.getValue()));

                        String xsltInterface = binaryParameter.getValueNotEmpty();

                        try {
                            objectCombinedDetermination.mapInterfaceDeterminations.put(receiverName, new TemplateInterfaceDetermination());
                            objectCombinedDetermination.mapInterfaceDeterminations.get(receiverName).xsltToObjectInterfaceDetermination(xsltInterface, receiverName);
                        } catch (Exception ex) {
                            LOGGER.error(ex);
                        }

                        JPanel cardPanel = new JPanel(getGridLayout());

                        KeyPanel paneLabelInterfaceDetermination = showTableInterfaceDetermination(objectCombinedDetermination.mapInterfaceDeterminations.get(receiverName), null, binaryParameter, false);
                        tablesInterfaceDeterminations.put(receiverName, (JTable) paneLabelInterfaceDetermination.getComponent(COMPONENT_INTERFACE_TABLE));
                        cardPanel.add(paneLabelInterfaceDetermination);

                        cardsInterfaceDetermination.addNewComponent(receiverName, cardPanel);
                        cardsInterfaceDetermination.add(cardPanel, receiverName);
                    }
                }

                panelInterfaceDetermination.removeAll();

                JPanel newRadioButtonsInterfaces = showDropdownInterfaceDetermination(cardsInterfaceDetermination, cardLayoutInterfaceDetermination, setReceiverNames);

                panelInterfaceDetermination.add(newRadioButtonsInterfaces, BorderLayout.NORTH);
                panelInterfaceDetermination.add(cardsInterfaceDetermination, BorderLayout.CENTER);

                panelInterfaceDetermination.revalidate();
                panelInterfaceDetermination.repaint();
            }
        }

        UpdateHandler updateHandler = new UpdateHandler();

        receiverTableModel.addTableModelListener(e -> updateHandler.updateReceivers());

        boolean[] hasChanged = {false};

        defaultReceiverTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                hasChanged[0] = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                hasChanged[0] = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                hasChanged[0] = true;
            }
        });

        defaultReceiverTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                buttonGroupReceiverNotFound.getButton(LABEL_DEFAULT).setSelected(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (hasChanged[0]) {
                    updateHandler.updateReceivers();

                    hasChanged[0] = false;
                }
            }
        });

        for (Map.Entry<String, JRadioButton> entry : buttonGroupReceiverNotFound.getButtonMap().entrySet()) {
            String labelRadioButton = entry.getKey();
            JRadioButton radioButton = entry.getValue();
            if (labelRadioButton.equals(LABEL_DEFAULT)) {
                radioButton.addItemListener(e -> {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        defaultReceiverTextField.requestFocus();
                    }
                });
            } else {
                radioButton.addItemListener(e -> {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        defaultReceiverTextField.setText("");
                        updateHandler.updateReceivers();
                    }
                });
            }
        }

        JButton updateButton = (JButton) panelReceiverDetermination.getComponent(LABEL_UPDATE_RECEIVERS);
        updateButton.addActionListener(e -> {
            if (receiverTable.isEditing()) {
                receiverTable.getCellEditor().stopCellEditing();
            }
            receiverTable.clearSelection();
        });

        updateHandler.updateReceivers();

        // XSLT
        JPanel panelXslt = new JPanel(new BorderLayout());

        panelXslt.add(this.rTextScrollPaneCombinedDetermination, BorderLayout.CENTER);

        showUpdatedXslt(this.rTextScrollPaneCombinedDetermination, currentReceiverDetermination.getValueNotEmpty());

        JPanel panelXsltButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelXsltButtons.add(getGenerateXsltButtonCombinedDetermination(panelReceiverDetermination, panelInterfaceDetermination));
        panelXsltButtons.add(getSendButtonBinaryParameters(currentReceiverDetermination));
        panelXslt.add(panelXsltButtons, BorderLayout.SOUTH);

        panelCombinedDetermination.add(panelTables);
        panelCombinedDetermination.add(panelXslt);

        return panelCombinedDetermination;
    }

    private JPanel getPanelReceiverDetermination() {
        JPanel panelReceiverDetermination = new JPanel(new BorderLayout());

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);

        String xsltReceiver = currentReceiverDetermination.getValueNotEmpty();

        JPanel cardPanel = new JPanel(getGridLayout());

        try {
            objectReceiverDetermination.xsltToObjectReceiverDetermination(xsltReceiver);

            cardPanel.add(showTableReceiverDetermination(objectReceiverDetermination, true));
            cardPanel.add(rTextScrollPaneReceiverDetermination);
            showUpdatedXslt(rTextScrollPaneReceiverDetermination, xsltReceiver);
        } catch (Exception e) {
            LOGGER.error(e);
        }

        cards.add(cardPanel);
        cardLayout.show(cards, LABEL_RECEIVER_DETERMINATION);

        panelReceiverDetermination.add(cards, BorderLayout.CENTER);

        return panelReceiverDetermination;
    }

    private JPanel getPanelInterfaceDetermination() {
        JPanel panelInterfaceDetermination = new JPanel(new BorderLayout());

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);

        Set<String> listReceiverNames = currentReceiverDetermination.getSetReceiverNames();

        for (String id : listReceiverNames) {
            try {
                BinaryParameter binaryParameter = currentInterfaceDeterminationsList.computeIfAbsent(id, key -> new BinaryParameter(pid, ID_INTERFACE_DETERMINATION_ + key, "", ""));

                String currentValue = binaryParameter.getValue();
                String xsltInterface = !currentValue.isEmpty() ? currentValue : XSLT_NOT_NULL;
                objectInterfaceDetermination.xsltToObjectInterfaceDetermination(xsltInterface);

                JPanel cardPanel = new JPanel(getGridLayout());

                RTextScrollPane rTextScrollPane = initializeRSyntaxTextArea();

                cardPanel.add(showTableInterfaceDetermination(objectInterfaceDetermination, rTextScrollPane, binaryParameter, true));

                showUpdatedXslt(rTextScrollPane, xsltInterface);
                cardPanel.add(rTextScrollPane);

                cards.add(cardPanel, id);
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
        }

        JPanel panelRadioButtons = showDropdownInterfaceDetermination(cards, cardLayout, listReceiverNames);

        panelInterfaceDetermination.add(panelRadioButtons, BorderLayout.NORTH);
        panelInterfaceDetermination.add(cards, BorderLayout.CENTER);

        return panelInterfaceDetermination;
    }

    private JPanel getPanelPointToPointDetermination() {
        JPanel panelPointToPoint = new JPanel(new FlowLayout(FlowLayout.LEFT));

        int labelWidthDescription = 130;
        int labelWidthResponse = 220;

        JPanel panelComponents = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

        // Panel Receiver Determination
        AtomicReference<String> receiverNameInitial = new AtomicReference<>();
        try {
            receiverNameInitial.set(currentStringParametersList.get(ID_RECEIVER_DETERMINATION).getValue());
        } catch (Exception e) {
            receiverNameInitial.set("");
        }

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        JPanel panelLabelReceiver = createFixedSizePanel(createLabel(colonAsterisk(LABEL_RECEIVER_COMPONENT)), labelWidthDescription);
        panelComponents.add(panelLabelReceiver, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        JTextField textFieldReceiver = new JTextField(receiverNameInitial.get(), DEFAULT_COLUMNS_TEXT_FIELD);
        panelComponents.add(textFieldReceiver, gbc);

        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        JPanel panelResponseReceiver = createFixedSizePanel(createLabel(""), labelWidthResponse);
        panelComponents.add(panelResponseReceiver, gbc);

        // Panel Interface Determination
        AtomicReference<String> interfaceNameInitial = new AtomicReference<>(currentStringParametersList.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(ID_INTERFACE_DETERMINATION_))
                .map(entry -> entry.getValue().getValue())
                .findFirst()
                .orElse(null));

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        JPanel panelLabelInterface = createFixedSizePanel(createLabel(colonAsterisk(LABEL_RECEIVER_INTERFACE)), labelWidthDescription);
        panelComponents.add(panelLabelInterface, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        JTextField textFieldInterface = new JTextField(interfaceNameInitial.get(), DEFAULT_COLUMNS_TEXT_FIELD);
        panelComponents.add(textFieldInterface, gbc);

        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        JPanel panelResponseInterface = createFixedSizePanel(createLabel(""), labelWidthResponse);
        panelComponents.add(panelResponseInterface, gbc);

        // Button Panel
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        JButton buttonSend = new JButton(LABEL_SEND_CHANGES_TO_API);
        panelComponents.add(buttonSend, gbc);

        // Action Listener for button
        buttonSend.addActionListener(e -> {
            String receiverName = textFieldReceiver.getText();
            String interfaceName = textFieldInterface.getText();
            if (receiverName.isEmpty() || interfaceName.isEmpty()) {
                showErrorDialog(LABEL_ERROR, LABEL_ERROR_EMPTY_INPUT);
            } else {
                try {
                    boolean receiverChanged = !receiverName.equals(receiverNameInitial.get());
                    boolean interfaceChanged = !interfaceName.equals(interfaceNameInitial.get());

                    if (receiverChanged) {
                        httpRequestHandler.sendDeleteRequestStringParametersExistingDetermination(pid, ID_RECEIVER_DETERMINATION);
                        showHttpResponseWithTimer((JLabel) panelResponseReceiver.getComponent(0), httpRequestHandler.sendPostRequestStringParameters(pid, ID_RECEIVER_DETERMINATION, receiverName));

                        httpRequestHandler.sendDeleteRequestStringParametersExistingDetermination(pid, ID_INTERFACE_DETERMINATION_);
                        showHttpResponseWithTimer((JLabel) panelResponseInterface.getComponent(0), httpRequestHandler.sendPostRequestStringParameters(pid, ID_INTERFACE_DETERMINATION_ + receiverName, interfaceName));

                        StringParameter stringParameterReceiverDetermination = currentStringParametersList.get(ID_RECEIVER_DETERMINATION);
                        if (stringParameterReceiverDetermination == null) {
                            stringParameterReceiverDetermination = new StringParameter(pid, ID_RECEIVER_DETERMINATION, receiverName);
                        } else {
                            stringParameterReceiverDetermination.setValue(receiverName);
                        }
                        currentStringParametersList.put(ID_RECEIVER_DETERMINATION, stringParameterReceiverDetermination);

                        StringParameter stringParameterInterfaceDetermination = currentStringParametersList.get(ID_INTERFACE_DETERMINATION_ + receiverNameInitial);
                        if (stringParameterInterfaceDetermination == null) {
                            stringParameterInterfaceDetermination = new StringParameter(pid, ID_INTERFACE_DETERMINATION_ + receiverName, null);
                        }
                        stringParameterInterfaceDetermination.setValue(receiverName);
                        currentStringParametersList.remove(ID_INTERFACE_DETERMINATION_ + receiverNameInitial);
                        currentStringParametersList.put(ID_INTERFACE_DETERMINATION_ + receiverName, stringParameterInterfaceDetermination);

                        receiverNameInitial.set(receiverName);
                        interfaceNameInitial.set(interfaceName);
                    } else if (interfaceChanged) {
                        httpRequestHandler.sendDeleteRequestStringParametersExistingDetermination(pid, ID_INTERFACE_DETERMINATION_);
                        showHttpResponseWithTimer((JLabel) panelResponseInterface.getComponent(0), httpRequestHandler.sendPostRequestStringParameters(pid, ID_INTERFACE_DETERMINATION_ + receiverName, interfaceName));

                        StringParameter stringParameter = currentStringParametersList.get(ID_INTERFACE_DETERMINATION_ + receiverName);
                        stringParameter.setValue(receiverName);
                        currentStringParametersList.remove(ID_INTERFACE_DETERMINATION_ + receiverName);
                        currentStringParametersList.put(ID_INTERFACE_DETERMINATION_ + receiverName, stringParameter);

                        interfaceNameInitial.set(interfaceName);
                    }
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        });

        panelPointToPoint.add(panelComponents);

        return panelPointToPoint;
    }

    private JPanel getPanelStringParameters() {
        JPanel panelStringParameters = new JPanel(new BorderLayout());

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);

        JPanel cardPanel = new JPanel(getGridLayout());
        cardPanel.add(showTableStringParameters());

        cards.add(cardPanel);
        cardLayout.show(cards, LABEL_STRING_PARAMETERS);

        panelStringParameters.add(cards, BorderLayout.CENTER);

        return panelStringParameters;
    }

    private JPanel getPanelLandscapeStages() {
        JPanel panelLandscapeStages = new JPanel(new BorderLayout());

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);

        JPanel cardPanel = new JPanel(getGridLayout());
        cardPanel.add(showViewLandscapeStages());

        cards.add(cardPanel);
        cardLayout.show(cards, LABEL_STRING_PARAMETERS);

        panelLandscapeStages.add(cards, BorderLayout.CENTER);

        return panelLandscapeStages;
    }

    private GridLayout getGridLayout() {
        GridLayout layout = new GridLayout(1, 2);
        layout.setHgap(defaultGap);
        return layout;
    }

    private JPanel createFixedSizePanel(JLabel label, int width) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(width, 20));
        panel.setMinimumSize(new Dimension(width, 20));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createLabel(String text) {
        return new JLabel(text);
    }

    private void handleExceptionXsltGeneration(Exception ex) {
        String errorMessage;
        if (ex instanceof IOException) {
            errorMessage = ex.getMessage();
        } else if (ex instanceof DefaultReceiverMissingException) {
            errorMessage = LABEL_ERROR_WHEN_GENERATING_DEFAULT_RECEIVER;
        } else if (ex instanceof TableEmptyException) {
            errorMessage = LABEL_ERROR_WHEN_GENERATING_TABLE + ((TableEmptyException) ex).getTableName();
        } else if (ex instanceof TemplateException) {
            errorMessage = LABEL_ERROR_WHEN_GENERATING_INPUT;
        } else {
            errorMessage = LABEL_ERROR_WHEN_GENERATING + ex.getMessage();
        }
        LOGGER.error(errorMessage);
        showErrorDialog(LABEL_ERROR, errorMessage);
    }

    private String validateXsltSyntax(String xslt) {
        return xsltSyntaxValidator.validateXsltSyntax(xslt);
    }

    private void showDialogInvalidXslt(String xslt) {
        String result = validateXsltSyntax(xslt);
        if (!result.isEmpty()) {
            showErrorDialog(LABEL_WARNING, colon(LABEL_GENERATED_XSLT_INVALID_SYNTAX) + "\n" + result);
        }
    }

    private Set<String> getListReceiverNamesDependingOnDeterminationType(boolean shouldSendRequest) {
        Set<String> listReceiverNames;
        if (determinationType.equals(LABEL_POINT_TO_POINT)) {
            listReceiverNames = new LinkedHashSet<>();
            try {
                listReceiverNames.add(currentStringParametersList.get(ID_RECEIVER_DETERMINATION).getValue());
            } catch (Exception e) {
                // LOGGER.error(e);
            }
        } else {
            if (shouldSendRequest) {
                try {
                    httpRequestHandler.sendGetRequestBinaryParameters(pid);
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
            listReceiverNames = currentReceiverDetermination.getSetReceiverNames();
        }
        return listReceiverNames;
    }

    private Set<String> getSystemNames(boolean shouldSendRequest) {
        Set<String> systemNames = new LinkedHashSet<>();
        systemNames.add(editableHeader.currentHeaderValues.get(LABEL_AGENCY));
        systemNames.addAll(getListReceiverNamesDependingOnDeterminationType(shouldSendRequest));
        return systemNames;
    }
}