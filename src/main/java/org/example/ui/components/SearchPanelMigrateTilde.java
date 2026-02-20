package org.example.ui.components;

import org.example.model.AlternativePartnerMigrateTilde;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.example.utils.SharedData.*;

public class SearchPanelMigrateTilde extends JPanel {
    private final JTextField oldPidSearchField;
    private final JTextField newPidSearchField;
    private final JButton searchButton;
    private final JButton resetButton;
    private final JLabel entryCountLabel;
    private final List<AlternativePartnerMigrateTilde> originalData;
    private final Consumer<List<AlternativePartnerMigrateTilde>> refreshTableConsumer;
    private final JTable table;
    private final DefaultTableModel tableModel;

    public SearchPanelMigrateTilde(List<AlternativePartnerMigrateTilde> originalData, Consumer<List<AlternativePartnerMigrateTilde>> refreshTableConsumer, JTable table, DefaultTableModel tableModel) {
        this.originalData = originalData;
        this.refreshTableConsumer = refreshTableConsumer;
        this.table = table;
        this.tableModel = tableModel;

        setLayout(new FlowLayout(FlowLayout.LEFT));

        oldPidSearchField = new JTextField(SEARCH_FIELD_WIDTH);
        newPidSearchField = new JTextField(SEARCH_FIELD_WIDTH);

        searchButton = new JButton(LABEL_SEARCH);
        resetButton = new JButton(LABEL_RESET);
        entryCountLabel = new JLabel(colonSpace(LABEL_ENTRIES) + originalData.size());

        add(new JLabel(colon(LABEL_PID_OLD)));
        add(oldPidSearchField);
        add(new JLabel(colon(LABEL_PID_NEW)));
        add(newPidSearchField);
        add(searchButton);
        add(resetButton);
        add(entryCountLabel);

        setupListeners();
    }

    private void setupListeners() {
        searchButton.addActionListener(e -> performSearch());
        resetButton.addActionListener(e -> resetSearch());

        KeyAdapter keyAdapter = new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                performSearch();
            }
        };

        oldPidSearchField.addKeyListener(keyAdapter);
        newPidSearchField.addKeyListener(keyAdapter);
    }

    private void performSearch() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        // before actual search, add changed of new pids to objects to be searched
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String oldPidTable = (String) tableModel.getValueAt(row, 0);
            String newPidTable = (String) tableModel.getValueAt(row, 1);

            AlternativePartnerMigrateTilde matchingObject = originalData.stream()
                    .filter(obj -> obj.getOldPid().equals(oldPidTable))
                    .findFirst()
                    .orElse(null);

            matchingObject.setNewPid(newPidTable);
        }

        String[] searchTerms = new String[]{
                oldPidSearchField.getText().toLowerCase(),
                newPidSearchField.getText().toLowerCase()
        };

        List<AlternativePartnerMigrateTilde> filtered = originalData.stream()
                .filter(p -> (searchTerms[0].isEmpty() || p.getOldPid().toLowerCase().contains(searchTerms[0]))
                        && (searchTerms[1].isEmpty() || p.getNewPid().toLowerCase().contains(searchTerms[1])))
                .collect(Collectors.toList());

        refreshTableConsumer.accept(filtered);
        updateEntryCountLabel(filtered.size());
    }

    public void resetSearch() {
        oldPidSearchField.setText("");
        newPidSearchField.setText("");
        performSearch();
    }

    private void updateEntryCountLabel(int count) {
        entryCountLabel.setText(colonSpace(LABEL_ENTRIES) + count);
    }
}
