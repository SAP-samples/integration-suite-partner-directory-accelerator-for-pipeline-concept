package org.example.ui.pages;

import org.example.model.AlternativePartner;
import org.example.ui.components.BackButton;
import org.example.ui.components.SearchPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static org.example.utils.SharedData.*;

public class MergeXsltsPage extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<AlternativePartner> alternativePartnersMultipleXslts;

    public MergeXsltsPage(List<AlternativePartner> alternativePartners) {
        this.alternativePartnersMultipleXslts = alternativePartners;

        setLayout(new BorderLayout());

        String[] columnNames = {LABEL_AGENCY, LABEL_SCHEME, LABEL_ID_ALTERNATIVE_PARTNERS, LABEL_PID};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        refreshTableData(alternativePartnersMultipleXslts);

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

                    AlternativePartner alternativePartner = alternativePartnersMultipleXslts.stream()
                            .filter(obj -> obj.getAgency().equals(agency)
                                    && obj.getScheme().equals(scheme)
                                    && obj.getId().equals(id)
                                    && obj.getPid().equals(pid))
                            .findFirst()
                            .orElse(new AlternativePartner(agency, scheme, id, pid));
                    ParametersPage binaryParameterDetailPage = new ParametersPage(alternativePartner);
                    panelContainer.add(binaryParameterDetailPage, pid);
                    cardLayout.show(panelContainer, pid);
                }
            }
        });

        SearchPanel searchPanel = new SearchPanel(alternativePartnersMultipleXslts, this::refreshTableData);
        add(searchPanel, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        BackButton backButton = new BackButton();
        backButton.setText(LABEL_BACK_TO_ALL);
        buttonPanel.add(backButton);

        add(buttonPanel, BorderLayout.AFTER_LAST_LINE);
    }

    private void refreshTableData(List<AlternativePartner> data) {
        tableModel.setRowCount(0);
        for (AlternativePartner partner : data) {
            tableModel.addRow(new Object[]{partner.getAgency(), partner.getScheme(), partner.getId(), partner.getPid()});
        }
    }
}
