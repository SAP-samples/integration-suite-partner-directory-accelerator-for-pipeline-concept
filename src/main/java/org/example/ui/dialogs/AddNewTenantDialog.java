package org.example.ui.dialogs;

import org.example.exceptions.TenantNameNotUniqueException;
import org.example.utils.TenantCredentials;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.example.utils.SharedData.*;

public class AddNewTenantDialog extends JDialog {
    private static final HeaderColorOption[] HEADER_COLOR_OPTIONS = {
            new HeaderColorOption(TENANT_HEADER_COLOR_LABELS[0], TENANT_HEADER_COLOR_HEX_VALUES[0]),
            new HeaderColorOption(TENANT_HEADER_COLOR_LABELS[1], TENANT_HEADER_COLOR_HEX_VALUES[1]),
            new HeaderColorOption(TENANT_HEADER_COLOR_LABELS[2], TENANT_HEADER_COLOR_HEX_VALUES[2]),
            new HeaderColorOption(TENANT_HEADER_COLOR_LABELS[3], TENANT_HEADER_COLOR_HEX_VALUES[3]),
            new HeaderColorOption(TENANT_HEADER_COLOR_LABELS[4], TENANT_HEADER_COLOR_HEX_VALUES[4]),
            new HeaderColorOption(TENANT_HEADER_COLOR_LABELS[5], TENANT_HEADER_COLOR_HEX_VALUES[5]),
            new HeaderColorOption(TENANT_HEADER_COLOR_LABELS[6], TENANT_HEADER_COLOR_HEX_VALUES[6]),
            new HeaderColorOption(TENANT_HEADER_COLOR_LABELS[7], TENANT_HEADER_COLOR_HEX_VALUES[7]),
            new HeaderColorOption(TENANT_HEADER_COLOR_LABELS[8], TENANT_HEADER_COLOR_HEX_VALUES[8]),
            new HeaderColorOption(TENANT_HEADER_COLOR_LABELS[9], TENANT_HEADER_COLOR_HEX_VALUES[9])
    };

    private final JTextField tenantNameField;
    private final JComboBox<HeaderColorOption> headerColorDropdown;
    private final JTextField urlField;
    private final JTextField tokenUrlField;
    private final JTextField clientIdField;
    private final JPasswordField clientSecretField;
    private final JTextField userField;

    private TenantCredentials tenantValues;

    private final JButton saveButton;
    private final JButton cancelButton;
    private final JButton uploadButton;
    private final String dialogTitle;

    public AddNewTenantDialog(String dialogTitle) {
        super(mainFrame, dialogTitle, true);
        this.dialogTitle = dialogTitle;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(UI_PADDING, UI_PADDING, UI_PADDING, UI_PADDING);

        tenantNameField = new JTextField(UI_TEXT_FIELD_COLUMNS);
        headerColorDropdown = new JComboBox<>(HEADER_COLOR_OPTIONS);
        headerColorDropdown.setRenderer(new HeaderColorRenderer());
        urlField = new JTextField(UI_TEXT_FIELD_COLUMNS);
        tokenUrlField = new JTextField(UI_TEXT_FIELD_COLUMNS);
        clientIdField = new JTextField(UI_TEXT_FIELD_COLUMNS);
        clientSecretField = new JPasswordField(UI_TEXT_FIELD_COLUMNS);
        userField = new JTextField(UI_TEXT_FIELD_COLUMNS);

        cancelButton = new JButton(LABEL_CANCEL);
        saveButton = new JButton(LABEL_SAVE);
        saveButton.setPreferredSize(cancelButton.getPreferredSize());
        uploadButton = new JButton(LABEL_SELECT_LOCAL_JSON_FILE);

        addComponents(gbc);
        setupListeners();

        setSize(UI_DIALOG_WIDTH, UI_DIALOG_HEIGHT + 60);
        setLocationRelativeTo(mainFrame);
    }

    public AddNewTenantDialog(String dialogTitle, TenantCredentials tenant) {
        this(dialogTitle);
        setInputFieldValues(tenant);
    }

    private void addComponents(GridBagConstraints gbc) {
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Tenant Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel(colonAsterisk(LABEL_TENANT_NAME)), gbc);
        gbc.gridx = 1;
        add(tenantNameField, gbc);

        // Header Color Palette
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel(colon(LABEL_HEADER_COLOR)), gbc);
        gbc.gridx = 1;
        add(headerColorDropdown, gbc);

        // URL
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel(colonAsterisk(LABEL_URL)), gbc);
        gbc.gridx = 1;
        add(urlField, gbc);

        // Token URL
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel(colonAsterisk(LABEL_TOKEN_URL)), gbc);
        gbc.gridx = 1;
        add(tokenUrlField, gbc);

        // Client ID
        gbc.gridx = 0;
        gbc.gridy = 4;
        add(new JLabel(colonAsterisk(LABEL_CLIENT_ID)), gbc);
        gbc.gridx = 1;
        add(clientIdField, gbc);

        // Client Secret
        gbc.gridx = 0;
        gbc.gridy = 5;
        add(new JLabel(colonAsterisk(LABEL_CLIENT_SECRET)), gbc);
        gbc.gridx = 1;
        add(clientSecretField, gbc);

        // User
        gbc.gridx = 0;
        gbc.gridy = 6;
        add(new JLabel(colon(LABEL_USER)), gbc);
        gbc.gridx = 1;
        add(userField, gbc);

        // Buttons
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelButtons.add(cancelButton);
        panelButtons.add(saveButton);
        add(panelButtons, gbc);

        // Upload Button
        gbc.gridx = 0;
        gbc.gridy = 8;
        add(new JLabel(colon(LABEL_ALTERNATIVE)), gbc);
        gbc.gridx = 1;
        add(uploadButton, gbc);
    }

    private void setupListeners() {
        cancelButton.addActionListener(e -> dispose());

        urlField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!urlField.getText().trim().isEmpty()) {
                    urlField.setText(normalizeTenantUrl(urlField.getText()));
                }
            }
        });

        saveButton.addActionListener(e -> {
            if (areFieldsValid()) {
                String normalizedUrl = normalizeTenantUrl(urlField.getText());
                urlField.setText(normalizedUrl);
                String user = userField.getText().trim();
                TenantCredentials newTenant = new TenantCredentials(tenantNameField.getText().trim(), normalizedUrl, tokenUrlField.getText().trim(), clientIdField.getText().trim(), new String(clientSecretField.getPassword()).trim(), user.isEmpty() ? null : user, null, null, getSelectedHeaderColorHex());

                try {
                    if (dialogTitle.equals(LABEL_EDIT_SELECTED_TENANT)) { // edit tenant
                        if (jsonFileHandler.isNameUniqueReplace(newTenant.getName(), tenantValues.getName())) {
                            jsonFileHandler.replaceTenant(tenantValues, newTenant);
                        } else {
                            throw new TenantNameNotUniqueException();
                        }
                    } else { // add tenant
                        if (jsonFileHandler.isNameUniqueAdd(newTenant.getName())) {
                            jsonFileHandler.addTenant(newTenant);
                        } else {
                            throw new TenantNameNotUniqueException();
                        }
                    }
                    dispose();

                    tenantValues = newTenant;
                    mainFrame.setSelectedTenant(tenantValues.getName());
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                } catch (TenantNameNotUniqueException ex) {
                    JOptionPane.showMessageDialog(mainFrame, LABEL_ERROR_TENANT_NAME_ALREADY_EXISTS, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, LABEL_FILL_OUT_ALL_FIELDS, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
            }
        });

        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    JSONObject jsonObject = new JSONObject(content);

                    String url = jsonObject.getJSONObject(JSON_KEY_OAUTH).getString(JSON_KEY_URL);
                    String tokenUrl = jsonObject.getJSONObject(JSON_KEY_OAUTH).getString(JSON_KEY_TOKEN_URL);
                    String clientId = jsonObject.getJSONObject(JSON_KEY_OAUTH).getString(JSON_KEY_CLIENT_ID);
                    String clientSecret = jsonObject.getJSONObject(JSON_KEY_OAUTH).getString(JSON_KEY_CLIENT_SECRET);

                    urlField.setText(url + PATH_TO_API);
                    tokenUrlField.setText(tokenUrl);
                    clientIdField.setText(clientId);
                    clientSecretField.setText(clientSecret);
                } catch (IOException ex) {
                    LOGGER.error(ex);
                    JOptionPane.showMessageDialog(this, LABEL_ERROR_READING_JSON_FILE, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    public void setInputFieldValues(TenantCredentials tenant) {
        setInputFieldValues(tenant.getName(), tenant.getHeaderColorHex(), tenant.getUrl(), tenant.getTokenurl(), tenant.getClientid(), tenant.getClientsecret(), tenant.getUser());
        tenantValues = tenant;
    }

    public void setEmptyValues() {
        setInputFieldValues(null, DEFAULT_TENANT_HEADER_COLOR_HEX, null, null, null, null, null);
        tenantValues = null;
    }

    public void setInputFieldValues(String name, String headerColorHex, String url, String tokenurl, String clientId, String clientSecret, String user) {
        tenantNameField.setText(name);
        setSelectedHeaderColor(headerColorHex);
        urlField.setText(url);
        tokenUrlField.setText(tokenurl);
        clientIdField.setText(clientId);
        clientSecretField.setText(clientSecret);
        userField.setText(user);
    }

    private String getSelectedHeaderColorHex() {
        HeaderColorOption selectedOption = (HeaderColorOption) headerColorDropdown.getSelectedItem();
        if (selectedOption == null) {
            return DEFAULT_TENANT_HEADER_COLOR_HEX;
        }
        return selectedOption.hex();
    }

    private void setSelectedHeaderColor(String headerColorHex) {
        String targetHex = headerColorHex == null || headerColorHex.isBlank() ? DEFAULT_TENANT_HEADER_COLOR_HEX : headerColorHex;
        for (HeaderColorOption colorOption : HEADER_COLOR_OPTIONS) {
            if (colorOption.hex().equalsIgnoreCase(targetHex)) {
                headerColorDropdown.setSelectedItem(colorOption);
                return;
            }
        }
        headerColorDropdown.setSelectedIndex(0);
    }

    private static class HeaderColorRenderer extends JPanel implements ListCellRenderer<HeaderColorOption> {
        private final JLabel colorPreview = new JLabel("  ");
        private final JLabel colorValue = new JLabel();

        private HeaderColorRenderer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 6, 2));
            setOpaque(true);

            colorPreview.setOpaque(true);
            colorPreview.setPreferredSize(new Dimension(12, 12));
            colorPreview.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

            add(colorPreview);
            add(colorValue);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends HeaderColorOption> list, HeaderColorOption value, int index, boolean isSelected, boolean cellHasFocus) {
            HeaderColorOption option = value == null ? HEADER_COLOR_OPTIONS[0] : value;
            colorValue.setText(option.label());

            try {
                colorPreview.setBackground(resolvePreviewColor(option.hex()));
            } catch (NumberFormatException e) {
                colorPreview.setBackground(Color.LIGHT_GRAY);
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                colorValue.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                colorValue.setForeground(list.getForeground());
            }

            return this;
        }

        private Color resolvePreviewColor(String colorValue) {
            if (DEFAULT_TENANT_HEADER_COLOR_HEX.equalsIgnoreCase(colorValue)) {
                return UIManager.getColor("Panel.background");
            }
            return Color.decode(colorValue);
        }
    }

    private record HeaderColorOption(String label, String hex) {
    }

    private boolean areFieldsValid() {
        return !tenantNameField.getText().trim().isEmpty() &&
                !urlField.getText().trim().isEmpty() &&
                !tokenUrlField.getText().trim().isEmpty() &&
                !clientIdField.getText().trim().isEmpty() &&
                clientSecretField.getPassword().length > 0;
    }

    private String normalizeTenantUrl(String url) {
        String normalizedUrl = url.trim();
        if (!normalizedUrl.endsWith(PATH_TO_API) ) {
            if (!normalizedUrl.endsWith("/")) {
                normalizedUrl += PATH_TO_API;
            }
            else {
                normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1) + PATH_TO_API;
            }
        }
        return normalizedUrl;
    }
}