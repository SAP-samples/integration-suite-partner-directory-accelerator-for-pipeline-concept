package org.example.ui.pages;

import org.example.model.AlternativePartnerMigrateTilde;
import org.example.ui.components.BackButton;
import org.example.ui.components.SearchPanelMigrateTilde;
import org.example.ui.dialogs.MigrateTildeDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.example.utils.SharedData.*;

public class MigrateTildePage extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;

    public MigrateTildePage(List<String> uniquePidsWithReceiverDetermination) {
        setLayout(new BorderLayout());

        String[] columnNames = {LABEL_PID_OLD, LABEL_PID_NEW};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0;
            }
        };

        List<AlternativePartnerMigrateTilde> listAlternativePartnerMigrateTilde = new ArrayList<>();
        for (String oldPid : uniquePidsWithReceiverDetermination) {
            listAlternativePartnerMigrateTilde.add(new AlternativePartnerMigrateTilde(oldPid));
        }

        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.setGridColor(Color.BLACK);

        SearchPanelMigrateTilde searchPanel = new SearchPanelMigrateTilde(listAlternativePartnerMigrateTilde, this::refreshTableData, table, tableModel);
        add(searchPanel, BorderLayout.NORTH);


        tableModel.setRowCount(0);
        for (String pidWithTilde : uniquePidsWithReceiverDetermination) {
            tableModel.addRow(new Object[]{pidWithTilde, ""});
        }

        table.getModel().addTableModelListener(event -> {

        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        BackButton backButton = new BackButton();
        buttonPanel.add(backButton);

        JButton migrateButton = new JButton(LABEL_MIGRATE_TILDE);
        migrateButton.addActionListener(e -> {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }

            // before actual search, add changed of new pids to objects to be searched
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                String oldPidTable = (String) tableModel.getValueAt(row, 0);
                String newPidTable = (String) tableModel.getValueAt(row, 1);

                AlternativePartnerMigrateTilde matchingObject = listAlternativePartnerMigrateTilde.stream()
                        .filter(obj -> obj.getOldPid().equals(oldPidTable))
                        .findFirst()
                        .orElse(null);

                matchingObject.setNewPid(newPidTable);
            }

            int counterSelected = 0;
            for (AlternativePartnerMigrateTilde alternativePartnerMigrateTilde : listAlternativePartnerMigrateTilde) {
                if (!alternativePartnerMigrateTilde.getNewPid().isEmpty()) {
                    counterSelected += 1;
                }
            }
            if (counterSelected > 0) {
                new MigrateTildeDialog(listAlternativePartnerMigrateTilde, counterSelected);
            } else {
                JOptionPane.showMessageDialog(mainFrame, LABEL_ERROR_ADD_AT_LEAST_ONE_NEW_PARTNER_ID_TO_MIGRATE, LABEL_ERROR, JOptionPane.ERROR_MESSAGE);
            }

        });
        buttonPanel.add(migrateButton);

        add(buttonPanel, BorderLayout.AFTER_LAST_LINE);
    }

    public void refreshTableData(List<AlternativePartnerMigrateTilde> data) {
        tableModel.setRowCount(0);
        for (AlternativePartnerMigrateTilde partner : data) {
            tableModel.addRow(new Object[]{partner.getOldPid(), partner.getNewPid()});
        }
    }
}