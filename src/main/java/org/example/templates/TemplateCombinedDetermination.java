package org.example.templates;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TemplateCombinedDetermination implements TemplateObjects {
    public TemplateReceiverDetermination receiverDetermination = new TemplateReceiverDetermination();
    public Map<String, TemplateInterfaceDetermination> mapInterfaceDeterminations = new LinkedHashMap<>();
    private Set<String> params = new HashSet<>();

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

    public Set<String> getParams() {
        return params;
    }

    public void setParams() {
        params.clear();

        receiverDetermination.setParams();
        params.addAll(receiverDetermination.getParams());

        for (TemplateInterfaceDetermination templateInterfaceDetermination : mapInterfaceDeterminations.values()) {
            templateInterfaceDetermination.setParams();
            params.addAll(templateInterfaceDetermination.getParams());
        }
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
