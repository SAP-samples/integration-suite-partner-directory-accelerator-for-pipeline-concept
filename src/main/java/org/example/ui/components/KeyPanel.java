package org.example.ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KeyPanel extends JPanel {
    private final Map<String, Object> objectMap;

    public KeyPanel(LayoutManager layoutManager) {
        this.setLayout(layoutManager);
        this.objectMap = new HashMap<>();
    }

    public void addNewComponent(String key, Object component) {
        this.objectMap.put(key, component);
    }

    public Set<String> getAllKeys() {
        return this.objectMap.keySet();
    }

    public Object getComponent(String key) {
        return this.objectMap.get(key);
    }
}
