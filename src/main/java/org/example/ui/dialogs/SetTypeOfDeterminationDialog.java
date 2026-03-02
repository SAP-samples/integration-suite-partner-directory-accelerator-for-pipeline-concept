package org.example.ui.dialogs;

import org.example.model.AlternativePartner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import static org.example.utils.SharedData.*;

public class SetTypeOfDeterminationDialog extends JDialog {

    public SetTypeOfDeterminationDialog(AlternativePartner alternativePartner) {
        JDialog dialogSetTypeReceiverInterfaceDetermination = new JDialog(mainFrame, LABEL_SELECT_DETERMINATION_TYPE, true);
        dialogSetTypeReceiverInterfaceDetermination.setLayout(new BorderLayout());
        dialogSetTypeReceiverInterfaceDetermination.setSize(UI_DIALOG_WIDTH, UI_DIALOG_HEIGHT);
        dialogSetTypeReceiverInterfaceDetermination.setLocationRelativeTo(mainFrame);

        dialogSetTypeReceiverInterfaceDetermination.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialogSetTypeReceiverInterfaceDetermination.dispose();
            }
        });

        ButtonGroup buttonGroup = new ButtonGroup();
        List<JRadioButton> radioButtons = new ArrayList<>();
        JPanel panelRadioButtons = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        for (String labelRadioButton : LABELS_DETERMINATION_TYPES) {
            JRadioButton radioButton = new JRadioButton(labelRadioButton);
            buttonGroup.add(radioButton);
            radioButtons.add(radioButton);
            panelRadioButtons.add(radioButton, c);
            if (labelRadioButton.equals(LABEL_COMBINED_XSLT)) {
                radioButton.setSelected(true);
            }
            c.gridx++;
        }
        dialogSetTypeReceiverInterfaceDetermination.add(panelRadioButtons, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton cancelButton = new JButton(LABEL_DEFAULT_COMBINED_DETERMINATION);
        cancelButton.addActionListener(event -> {
            alternativePartner.setDeterminationType(LABEL_COMBINED_XSLT);
            dialogSetTypeReceiverInterfaceDetermination.dispose();
        });
        buttonPanel.add(cancelButton);

        JButton saveButton = new JButton(LABEL_SAVE_SELECTION);
        saveButton.addActionListener(event -> {
            ButtonModel selectedModel = buttonGroup.getSelection();
            for (JRadioButton radioButton : radioButtons) {
                if (radioButton.getModel() == selectedModel) {
                    alternativePartner.setDeterminationType(radioButton.getText());
                    dialogSetTypeReceiverInterfaceDetermination.dispose();
                }
            }
        });
        buttonPanel.add(saveButton);

        dialogSetTypeReceiverInterfaceDetermination.add(buttonPanel, BorderLayout.SOUTH);

        dialogSetTypeReceiverInterfaceDetermination.setVisible(true);
    }
}
