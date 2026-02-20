package org.example.ui.pages;

import org.example.model.AlternativePartner;
import org.example.ui.components.BackButton;
import org.example.ui.components.SearchPanel;
import org.example.ui.dialogs.TransportDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import static org.example.utils.SharedData.*;

public class TransportPage extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;

    public TransportPage() {
        setLayout(new BorderLayout());

        String[] columnNames = {LABEL_TRANSPORT, LABEL_AGENCY, LABEL_SCHEME, LABEL_ID_ALTERNATIVE_PARTNERS, LABEL_PID};
        tableModel = new DefaultTableModel(columnNames, 0);

        SearchPanel searchPanel = new SearchPanel(currentAlternativePartnersList, this::refreshTableData);
        add(searchPanel, BorderLayout.NORTH);

        refreshTableData(currentAlternativePartnersList);

        table = new JTable(tableModel) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
        };

        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.setGridColor(Color.BLACK);

        JCheckBox checkBoxRenderer = new JCheckBox();
        checkBoxRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Boolean) {
                    checkBoxRenderer.setSelected((Boolean) value);
                }
                if (isSelected) {
                    checkBoxRenderer.setBackground(table.getSelectionBackground());
                } else {
                    checkBoxRenderer.setBackground(table.getBackground());
                }
                return checkBoxRenderer;
            }
        });

        JCheckBox checkBoxEditor = new JCheckBox();
        checkBoxEditor.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(checkBoxEditor));

        table.getModel().addTableModelListener(event -> {
            int row = event.getFirstRow();
            if (event.getColumn() == 0) {
                if (row < 0 || row >= tableModel.getRowCount()) return;

                Boolean isSelected = (Boolean) tableModel.getValueAt(row, 0);
                currentAlternativePartnersList.stream()
                        .filter(p -> p.getAgency().equals(tableModel.getValueAt(row, 1))
                                && p.getScheme().equals(tableModel.getValueAt(row, 2))
                                && p.getId().equals(tableModel.getValueAt(row, 3))
                                && p.getPid().equals(tableModel.getValueAt(row, 4)))
                        .findFirst().ifPresent(partner -> partner.setSelected(isSelected));
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        BackButton backButton = new BackButton();
        buttonPanel.add(backButton);

        JButton deselectAllButton = new JButton(LABEL_BUTTON_DESELECT_ALL);
        deselectAllButton.addActionListener(e -> actionSelect(tableModel, false));
        buttonPanel.add(deselectAllButton);

        JButton selectAllButton = new JButton(LABEL_BUTTON_SELECT_ALL);
        selectAllButton.addActionListener(e -> actionSelect(tableModel, true));
        buttonPanel.add(selectAllButton);

        JButton transportButton = new JButton(LABEL_TRANSPORT_ALTERNATIVE_PARTNERS);
        transportButton.addActionListener(e -> {
            searchPanel.resetSearch();
            int counterSelected = 0;
            for (AlternativePartner partner : currentAlternativePartnersList) {
                if (partner.isSelected()) {
                    counterSelected += 1;
                }
            }
            if (counterSelected > 0) {
                new TransportDialog(table, counterSelected);
            } else {
                JOptionPane.showMessageDialog(mainFrame, LABEL_ERROR_SELECT_AT_LEAST_ONE_ENTRY, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(transportButton);

        add(buttonPanel, BorderLayout.AFTER_LAST_LINE);
    }

    private void refreshTableData(List<AlternativePartner> data) {
        tableModel.setRowCount(0);
        for (AlternativePartner partner : data) {
            tableModel.addRow(new Object[]{partner.isSelected(), partner.getAgency(), partner.getScheme(), partner.getId(), partner.getPid()});
        }
    }

    private void actionSelect(DefaultTableModel tableModel, boolean toSelect) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(toSelect, i, 0);
            String agency = (String) tableModel.getValueAt(i, 1);
            String scheme = (String) tableModel.getValueAt(i, 2);
            String id = (String) tableModel.getValueAt(i, 3);
            String pid = (String) tableModel.getValueAt(i, 4);

            currentAlternativePartnersList.stream()
                    .filter(p -> p.getAgency().equals(agency)
                            && p.getScheme().equals(scheme)
                            && p.getId().equals(id)
                            && p.getPid().equals(pid))
                    .forEach(p -> p.setSelected(toSelect));
        }
    }
}