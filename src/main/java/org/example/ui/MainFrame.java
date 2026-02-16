package org.example.ui;

import org.example.api.HttpRequestHandler;
import org.example.ui.dialogs.ConfigurationDialog;
import org.example.ui.pages.AlternativePartnersPage;
import org.example.api.JsonApiHandler;
import org.example.utils.TenantCredentials;
import org.example.utils.XsltHandler;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.example.ui.components.LabelTimer.showHttpResponseWithTimer;
import static org.example.utils.SharedData.*;
import static org.example.utils.TenantCredentials.getTenantObjectByCredentials;

public class MainFrame extends JFrame {
    private ConfigurationDialog dialogAdd;
    private ConfigurationDialog dialogEdit;

    private final JPanel buttonPanel;
    private final Color defaultPanelColor;
    private final DefaultComboBoxModel<String> dropdownModel;
    private final JComboBox<String> tenantDropdown;
    private TenantCredentials selectedTenant;

    public MainFrame(String locationToCredentials) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        setTitle(UI_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(screenSize.width, screenSize.height);
        setLocation(0, 0);

        jsonApiHandler = new JsonApiHandler();
        xsltHandler = new XsltHandler();

        buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        defaultPanelColor = UIManager.getColor("Panel.background");

        dropdownModel = new DefaultComboBoxModel<>();
        tenantDropdown = new JComboBox<>(dropdownModel);
        updateTenantDropdown();
        tenantDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof String && tenantDropdown.getSelectedIndex() != -1) {
                onTenantSelected();
            }
        });
        buttonPanel.add(tenantDropdown);

        JButton addTenantButton = new JButton(LABEL_ADD_NEW_TENANT);
        buttonPanel.add(addTenantButton);

        JButton editTenantButton = new JButton(LABEL_EDIT_SELECTED_TENANT);
        buttonPanel.add(editTenantButton);

        JButton deleteTenantButton = new JButton(LABEL_DELETE_SELECTED_TENANT);
        buttonPanel.add(deleteTenantButton);

        JButton reloadButton = new JButton(LABEL_RELOAD);
        buttonPanel.add(reloadButton);

        httpResponseLabelHeader = new JLabel();
        httpResponseLabelHeader.setOpaque(true);
        httpResponseLabelHeader.setBackground(defaultPanelColor);
        buttonPanel.add(httpResponseLabelHeader);

        add(buttonPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        panelContainer = new JPanel(cardLayout);
        add(panelContainer, BorderLayout.CENTER);

        if (!locationToCredentials.isEmpty()) {
            try {
                String jsonData = new String(Files.readAllBytes(Paths.get(locationToCredentials)));
                JSONObject jsonObject = new JSONObject(jsonData);

                String url = jsonObject.getJSONObject(JSON_KEY_OAUTH).getString(JSON_KEY_URL) + PATH_TO_API;
                String tokenUrl = jsonObject.getJSONObject(JSON_KEY_OAUTH).getString(JSON_KEY_TOKEN_URL);
                String clientId = jsonObject.getJSONObject(JSON_KEY_OAUTH).getString(JSON_KEY_CLIENT_ID);
                String clientSecret = jsonObject.getJSONObject(JSON_KEY_OAUTH).getString(JSON_KEY_CLIENT_SECRET);

                TenantCredentials tenant = getTenantObjectByCredentials(url, tokenUrl, clientId, clientSecret);

                dialogEdit = new ConfigurationDialog(this, LABEL_EDIT_SELECTED_TENANT, tenant);

                setSelectedTenant(tenant.getName());

                jsonFileHandler.addTenant(tenant);
            } catch (Exception e) {
                LOGGER.error(e);
                dialogEdit = new ConfigurationDialog(this, LABEL_EDIT_SELECTED_TENANT);
            }
        } else {
            if (!tenantCredentialsList.isEmpty()) {
                TenantCredentials tenant = tenantCredentialsList.get(0);
                dialogEdit = new ConfigurationDialog(this, LABEL_EDIT_SELECTED_TENANT, tenant);
                setSelectedTenant(tenant.getName());
            } else {
                dialogEdit = new ConfigurationDialog(this, LABEL_EDIT_SELECTED_TENANT);
            }
        }

        if (dialogEdit == null) {
            dialogEdit = new ConfigurationDialog(this, LABEL_EDIT_SELECTED_TENANT);
        }

        addTenantButton.addActionListener(actionEvent -> {
            if (dialogAdd == null) {
                dialogAdd = new ConfigurationDialog(this, LABEL_ADD_NEW_TENANT);
            }
            dialogAdd.setEmptyValues();
            dialogAdd.setVisible(true);
        });

        editTenantButton.addActionListener(e -> dialogEdit.setVisible(true));

        deleteTenantButton.addActionListener(e -> {
            if (tenantCredentialsList.size() < 2) {
                JOptionPane.showMessageDialog(this, LABEL_LAST_TENANT, LABEL_WARNING, JOptionPane.WARNING_MESSAGE);
            } else {
                boolean deleteTenant = false;
                String[] options = {LABEL_DELETE, LABEL_CANCEL};

                int option = JOptionPane.showOptionDialog(
                        this,
                        LABEL_SURE_TO_DELETE + currentTenantName + "\"?",
                        LABEL_CONFIRMATION,
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]
                );

                deleteTenant = option == 0; // option 0 = delete; option 1 = cancel

                if (deleteTenant) {
                    jsonFileHandler.deleteTenant(getTenantObjectByCredentials(selectedTenant.getUrl(), selectedTenant.getTokenurl(), selectedTenant.getClientid(), selectedTenant.getClientsecret()));
                    updateTenantDropdown();
                    setSelectedTenant(tenantCredentialsList.get(0).getName());
                }
            }
        });

        reloadButton.addActionListener(e -> {
            LOGGER.info("Reload Alternative Partners");
            getAndShowLatestAlternativePartners();
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateDialogLocation();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                updateDialogLocation();
            }
        });
    }

    private void updateDialogLocation() {
        if (dialogEdit != null) {
            dialogEdit.setLocationRelativeTo(MainFrame.this);
        }
    }

    private void onTenantSelected() {
        String selectedTenantName = (String) tenantDropdown.getSelectedItem();
        if (selectedTenantName != null) {
            selectedTenant = tenantCredentialsList.stream()
                    .filter(tenant -> tenant.getName().equalsIgnoreCase(selectedTenantName))
                    .findFirst()
                    .orElse(null);
            if (selectedTenant != null) {
                if (selectedTenant.isCritical()) {
                    buttonPanel.setBackground(Color.RED);
                } else {
                    buttonPanel.setBackground(defaultPanelColor);
                }

                try {
                    LOGGER.info("Tenant \"{}\" selected with URL {}", selectedTenant.getName(), selectedTenant.getUrl());
                    httpRequestHandler = new HttpRequestHandler(selectedTenant);

                    getAndShowLatestAlternativePartners();
                } catch (Exception e) {
                    httpErrorShowEmptyTable(e);
                }
                currentTenantName = selectedTenantName;
                dialogEdit.setInputFieldValues(selectedTenant);
            }
        }
    }

    public void getAndShowLatestAlternativePartners() {
        String httpResponse;
        try {
            httpResponse = httpRequestHandler.sendGetRequestAlternativePartners(true);

            panelContainer.removeAll();
            panelContainer.add(new AlternativePartnersPage(this));
            panelContainer.revalidate();
            panelContainer.repaint();

            if (selectedTenant != null) {
                setTitle(UI_TITLE + " - " + selectedTenant.getName() + " (" + selectedTenant.getUrl() + ")");
            } else {
                setTitle(UI_TITLE);
            }

            showHttpResponseWithTimer(httpResponseLabelHeader, httpResponse);
        } catch (Exception e) {
            httpErrorShowEmptyTable(e);
        }
    }

    public void setSelectedTenant(String selectedTenantName) {
        if (dropdownModel.getIndexOf(selectedTenantName) == -1) {
            updateTenantDropdown();
        }
        tenantDropdown.setSelectedIndex(-1);
        tenantDropdown.setSelectedItem(selectedTenantName);
    }

    private void updateTenantDropdown() {
        ItemListener[] listeners = tenantDropdown.getItemListeners();

        for (ItemListener listener : listeners) {
            tenantDropdown.removeItemListener(listener);
        }

        tenantDropdown.removeAllItems();
        if (tenantCredentialsList.isEmpty()) {
            tenantDropdown.addItem(LABEL_PLACEHOLDER_EMPTY_DROPDOWN);
        } else {
            for (TenantCredentials tenant : tenantCredentialsList) {
                tenantDropdown.addItem(tenant.getName());
            }
        }

        for (ItemListener listener : listeners) {
            tenantDropdown.addItemListener(listener);
        }
    }

    private void httpErrorShowEmptyTable(Exception e) {
        LOGGER.error(e);

        currentAlternativePartnersList.clear();

        panelContainer.removeAll();
        panelContainer.add(new AlternativePartnersPage(this));
        panelContainer.revalidate();
        panelContainer.repaint();

        if (selectedTenant != null) {
            setTitle(UI_TITLE + " - " + selectedTenant.getName() + " (" + selectedTenant.getUrl() + ")");
        } else {
            setTitle(UI_TITLE);
        }

        JOptionPane.showMessageDialog(this, LABEL_ERROR_WHEN_CONFIGURING + e.getMessage(), LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
    }
}