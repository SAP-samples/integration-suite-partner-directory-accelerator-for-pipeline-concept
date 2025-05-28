package org.example.ui.components;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class KeyButtonGroup extends ButtonGroup {
    private Map<String, JRadioButton> buttonMap;

    public KeyButtonGroup() {
        this.buttonMap = new HashMap<>();
    }

    public Map<String, JRadioButton> getButtonMap() {
        return this.buttonMap;
    }

    public void addNewButton(String key, JRadioButton radioButton) {
        this.buttonMap.put(key, radioButton);
    }

    public JRadioButton getButton(String key) {
        return this.buttonMap.get(key);
    }
}
