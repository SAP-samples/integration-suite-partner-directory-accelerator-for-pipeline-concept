package org.example.ui.pages;

import org.example.model.AlternativePartner;
import org.example.ui.components.SearchPanel;
import org.example.ui.dialogs.AddAlternativePartnerDialog;
import org.example.ui.dialogs.LandscapeDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;

import static org.example.utils.SharedData.*;

public class AlternativePartnersPage extends JPanel {
    private final JFrame parentFrame;
    private final DefaultTableModel tableModel;
    private final JTable table;

    public AlternativePartnersPage(JFrame parentFrame) {
        this.parentFrame = parentFrame;

        setLayout(new BorderLayout());
        String[] columnNames = {LABEL_AGENCY, LABEL_SCHEME, LABEL_ID_ALTERNATIVE_PARTNERS, LABEL_PID};

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        refreshTableData(currentAlternativePartnersList);

        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.setGridColor(Color.BLACK);

        // Set the default sort key to column index 3 (PID column) in ascending order
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(3, SortOrder.ASCENDING)));

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                if (row >= 0) {
                    String agency = (String) table.getValueAt(row, 0);
                    String scheme = (String) table.getValueAt(row, 1);
                    String id = (String) table.getValueAt(row, 2);
                    String pid = (String) table.getValueAt(row, 3);

                    AlternativePartner alternativePartner = currentAlternativePartnersList.stream()
                            .filter(obj -> obj.getAgency().equals(agency)
                                    && obj.getScheme().equals(scheme)
                                    && obj.getId().equals(id)
                                    && obj.getPid().equals(pid))
                            .findFirst()
                            .orElse(new AlternativePartner(agency, scheme, id, pid));
                    ParametersPage binaryParameterDetailPage = new ParametersPage(alternativePartner, parentFrame);
                    panelContainer.add(binaryParameterDetailPage, pid);
                    cardLayout.show(panelContainer, pid);
                }
            }
        });

        SearchPanel searchPanel = new SearchPanel(currentAlternativePartnersList, this::refreshTableData);
        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(getSouthButtonPanel(), BorderLayout.AFTER_LAST_LINE);

        alternativePartnersPage = this;
    }

    private JPanel getSouthButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton landscapeButton = new JButton("Maintain " + STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE);
        landscapeButton.addActionListener(e -> new LandscapeDialog(parentFrame));
        buttonPanel.add(landscapeButton);

        JButton addButton = new JButton(LABEL_ADD_ALTERNATIVE_PARTNER);
        addButton.addActionListener(e -> {
            LinkedHashMap<String, String> headerValues = new LinkedHashMap<>();
            headerValues.put(LABEL_AGENCY, "");
            headerValues.put(LABEL_SCHEME, "");
            headerValues.put(LABEL_ID_ALTERNATIVE_PARTNERS, "");
            headerValues.put(LABEL_PID, "");
            headerValues.put(LABEL_SELECT_DETERMINATION_TYPE, "");
            new AddAlternativePartnerDialog(parentFrame, headerValues, tableModel);
        });
        buttonPanel.add(addButton);

        JButton transportButton = new JButton(LABEL_TRANSPORT_ALTERNATIVE_PARTNERS);
        transportButton.addActionListener(e -> {
            LOGGER.info("Replication Page selected");
            if (tenantCredentialsList.size() <= 1) {
                JOptionPane.showMessageDialog(parentFrame, LABEL_TRANSPORT_ERROR_ADD_TENANT, LABEL_WARNING, JOptionPane.WARNING_MESSAGE);
            } else {
                try {
                    httpRequestHandler.sendGetRequestAlternativePartnersTransport();
                } catch (Exception exception) {
                    //
                }
                for (AlternativePartner partner : currentAlternativePartnersList) {
                    partner.setSelected(false);
                }
                TransportPage transportPage = new TransportPage(parentFrame);
                panelContainer.add(transportPage, LABEL_TRANSPORT_ID);
                cardLayout.show(panelContainer, LABEL_TRANSPORT_ID);
            }
        });
        buttonPanel.add(transportButton);

        return buttonPanel;
    }

    public void refreshTableData(List<AlternativePartner> data) {
        tableModel.setRowCount(0);
        for (AlternativePartner partner : data) {
            tableModel.addRow(new Object[]{partner.getAgency(), partner.getScheme(), partner.getId(), partner.getPid()});
        }
    }
}