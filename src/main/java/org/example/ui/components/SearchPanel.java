package org.example.ui.components;

import org.example.model.AlternativePartner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.example.utils.SharedData.*;

public class SearchPanel extends JPanel {
    private final JTextField agencySearchField;
    private final JTextField schemeSearchField;
    private final JTextField idSearchField;
    private final JTextField pidSearchField;
    private final JButton searchButton;
    private final JButton resetButton;
    private final List<AlternativePartner> originalData;
    private final Consumer<List<AlternativePartner>> refreshTableConsumer;

    public SearchPanel(List<AlternativePartner> originalData, Consumer<List<AlternativePartner>> refreshTableConsumer) {
        this.originalData = originalData;
        this.refreshTableConsumer = refreshTableConsumer;

        setLayout(new FlowLayout(FlowLayout.LEFT));

        int searchFieldWidth = 15;
        agencySearchField = new JTextField(searchFieldWidth);
        schemeSearchField = new JTextField(searchFieldWidth);
        idSearchField = new JTextField(searchFieldWidth);
        pidSearchField = new JTextField(searchFieldWidth);
        searchButton = new JButton(LABEL_SEARCH);
        resetButton = new JButton(LABEL_RESET);

        add(new JLabel(colon(LABEL_AGENCY)));
        add(agencySearchField);
        add(new JLabel(colon(LABEL_SCHEME)));
        add(schemeSearchField);
        add(new JLabel(colon(LABEL_ID_ALTERNATIVE_PARTNERS)));
        add(idSearchField);
        add(new JLabel(colon(LABEL_PID)));
        add(pidSearchField);
        add(searchButton);
        add(resetButton);

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

        agencySearchField.addKeyListener(keyAdapter);
        schemeSearchField.addKeyListener(keyAdapter);
        idSearchField.addKeyListener(keyAdapter);
        pidSearchField.addKeyListener(keyAdapter);
    }

    private void performSearch() {
        String[] searchTerms = new String[]{
                agencySearchField.getText().toLowerCase(),
                schemeSearchField.getText().toLowerCase(),
                idSearchField.getText().toLowerCase(),
                pidSearchField.getText().toLowerCase()
        };

        List<AlternativePartner> filteredList = originalData.stream()
                .filter(p -> (searchTerms[0].isEmpty() || p.getAgency().toLowerCase().contains(searchTerms[0]))
                        && (searchTerms[1].isEmpty() || p.getScheme().toLowerCase().contains(searchTerms[1]))
                        && (searchTerms[2].isEmpty() || p.getId().toLowerCase().contains(searchTerms[2]))
                        && (searchTerms[3].isEmpty() || p.getPid().toLowerCase().contains(searchTerms[3])))
                .collect(Collectors.toList());

        refreshTableConsumer.accept(filteredList);
    }

    public void resetSearch() {
        refreshTableConsumer.accept(originalData);
        agencySearchField.setText("");
        schemeSearchField.setText("");
        idSearchField.setText("");
        pidSearchField.setText("");
    }
}