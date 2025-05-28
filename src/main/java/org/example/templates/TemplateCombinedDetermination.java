package org.example.templates;

import java.util.LinkedHashMap;
import java.util.Map;

public class TemplateCombinedDetermination implements Clearable {
    public TemplateReceiverDetermination receiverDetermination = new TemplateReceiverDetermination();
    public Map<String, TemplateInterfaceDetermination> mapInterfaceDeterminations = new LinkedHashMap<>();

    public TemplateReceiverDetermination getReceiverDetermination() {
        return receiverDetermination;
    }

    public void setReceiverDetermination(TemplateReceiverDetermination receiverDetermination) {
        this.receiverDetermination = receiverDetermination;
    }

    public Map<String, TemplateInterfaceDetermination> getMapInterfaceDeterminations() {
        return mapInterfaceDeterminations;
    }

    public void setMapInterfaceDeterminations(Map<String, TemplateInterfaceDetermination> mapInterfaceDeterminations) {
        this.mapInterfaceDeterminations = mapInterfaceDeterminations;
    }

    public void clear() {
        receiverDetermination.clear();
        mapInterfaceDeterminations.clear();
    }

    public void xsltToObjectCombinedDetermination(String xslt) throws Exception {
        this.clear();

        this.receiverDetermination.xsltToObjectReceiverDetermination(xslt);

        for (String receiverName : this.receiverDetermination.getCurrentReceiverNames()) {
            this.mapInterfaceDeterminations.put(receiverName, new TemplateInterfaceDetermination());
            this.mapInterfaceDeterminations.get(receiverName).xsltToObjectInterfaceDetermination(xslt, receiverName);
        }
    }
}
