package org.example.ui.dialogs;

import org.example.model.AlternativePartner;
import org.example.ui.components.EditableHeader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.util.LinkedHashMap;

import static org.example.utils.SharedData.*;

public class AddAlternativePartnerDialog extends JDialog {

    public AddAlternativePartnerDialog(JFrame parent, LinkedHashMap<String, String> headerValues, DefaultTableModel tableModel) {
        super(parent, LABEL_ADD_ALTERNATIVE_PARTNER, true);
        setLayout(new BorderLayout());

        EditableHeader headerContainer = new EditableHeader(headerValues, false);

        add(headerContainer, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        JButton cancelButton = new JButton(LABEL_CANCEL);
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        JButton saveButton = new JButton(LABEL_SEND_NEW_TO_API);
        saveButton.addActionListener(e -> {
            String agency = headerContainer.currentHeaderValues.get(LABEL_AGENCY);
            String scheme = headerContainer.currentHeaderValues.get(LABEL_SCHEME);
            String id = headerContainer.currentHeaderValues.get(LABEL_ID_ALTERNATIVE_PARTNERS);
            String pid = headerContainer.currentHeaderValues.get(LABEL_PID);
            String determinationType = headerContainer.currentHeaderValues.get(LABEL_SELECT_DETERMINATION_TYPE);

            try {
                httpRequestHandler.sendPostRequestAlternativePartners(agency, scheme, id, pid);

                tableModel.addRow(new String[]{agency, scheme, id, pid});

                currentAlternativePartnersList.add(new AlternativePartner(agency, scheme, id, pid, determinationType));
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
            dispose();
        });
        buttonPanel.add(saveButton);

        add(buttonPanel, BorderLayout.SOUTH);

        setSize(800, 500);
        setLocationRelativeTo(parent);
        setVisible(true);
    }
}
