package org.example.ui.pages;

import org.example.model.AlternativePartner;
import org.example.ui.components.SearchPanel;
import org.example.ui.dialogs.AddAlternativePartnerDialog;
import org.example.ui.dialogs.LandscapeDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.example.ui.components.LabelTimer.showHttpResponseWithTimer;
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

        // landscape button
        JButton landscapeButton = new JButton("Maintain " + STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE);
        landscapeButton.addActionListener(e -> new LandscapeDialog(parentFrame));
        buttonPanel.add(landscapeButton);

        // add button
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

        // replicate button
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

//        // migrate deprecated entries
//        JButton migrateDeprecatedEntries = new JButton(LABEL_MIGRATE_DEPRECATED_ENTRIES);
//        migrateDeprecatedEntries.addActionListener(e -> {
//            LOGGER.info("Migrate Deprecated Page selected");
//            MigrateTildePage migrateDeprecatedEntriesPage = new MigrateTildePage(parentFrame);
//            panelContainer.add(migrateDeprecatedEntriesPage, LABEL_MIGRATE_DEPRECATED_ENTRIES_ID);
//            cardLayout.show(panelContainer, LABEL_MIGRATE_DEPRECATED_ENTRIES_ID);
//        });
//        buttonPanel.add(migrateDeprecatedEntries);

//        // migrate button
//        JButton migrateTildeButton = new JButton(LABEL_MIGRATE_TILDE);
//        migrateTildeButton.addActionListener(e -> {
//            LOGGER.info("Migrate Tilde Page selected");
//            MigrateTildePage migrateTildePage = new MigrateTildePage(parentFrame);
//            panelContainer.add(migrateTildePage, LABEL_MIGRATE_TILDE_ID);
//            cardLayout.show(panelContainer, LABEL_MIGRATE_TILDE_ID);
//        });
//        buttonPanel.add(migrateTildeButton);

        // merge button
        JButton mergeButton = new JButton(LABEL_MERGE_XSLTS);
        mergeButton.addActionListener(e -> {
            LOGGER.info("Merge Page selected");

            try {
                String httpResponse = httpRequestHandler.sendGetRequestAlternativePartners(false);
                Set<String> pidsWithInterfaceDetermination = httpRequestHandler.sendGetRequestBinaryParametersIdsInterfaceDetermination();

                List<AlternativePartner> alternativePartnersMultipleXslts = currentAlternativePartnersList.stream()
                        .filter(partner -> pidsWithInterfaceDetermination.contains(partner.getPid()))
                        .toList();

                List<String> pidsMultipleXslts = alternativePartnersMultipleXslts.stream()
                        .map(AlternativePartner::getPid)
                        .toList();

                Set<String> pidsWithoutCombinedDetermination = httpRequestHandler.sendGetRequestBinaryParametersXsltsReceiverDetermination(pidsMultipleXslts);

                alternativePartnersMultipleXslts = alternativePartnersMultipleXslts.stream()
                        .filter(partner -> pidsWithoutCombinedDetermination.contains(partner.getPid()))
                        .toList();

                MergeXsltsPage mergeXsltsPage = new MergeXsltsPage(parentFrame, alternativePartnersMultipleXslts);
                panelContainer.add(mergeXsltsPage, LABEL_MERGE_XSLTS_ID);
                cardLayout.show(panelContainer, LABEL_MERGE_XSLTS_ID);
                showHttpResponseWithTimer(httpResponseLabelHeader, httpResponse);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
            }
        });
        buttonPanel.add(mergeButton);

        return buttonPanel;
    }

    public void refreshTableData(List<AlternativePartner> data) {
        tableModel.setRowCount(0);
        for (AlternativePartner partner : data) {
            tableModel.addRow(new Object[]{partner.getAgency(), partner.getScheme(), partner.getId(), partner.getPid()});
        }
    }
}