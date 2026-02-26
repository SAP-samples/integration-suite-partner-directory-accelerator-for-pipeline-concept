package org.example.templates;

import org.example.exceptions.XsltNotExistsException;
import org.example.exceptions.XsltSyntaxException;

import java.util.*;

import static org.example.utils.SharedData.*;

public class TemplateCombinedDetermination implements TemplateObjects {
    public TemplateReceiverDetermination receiverDetermination = new TemplateReceiverDetermination();
    public Map<String, TemplateInterfaceDetermination> mapInterfaceDeterminations = new LinkedHashMap<>();

    // namespaces

    public Map<String, String> getNamespaces() {
        HashMap<String, String> namespaces = new HashMap<>(receiverDetermination.getNamespaces());

        for (TemplateInterfaceDetermination templateInterfaceDetermination : mapInterfaceDeterminations.values()) {
            namespaces.putAll(templateInterfaceDetermination.getNamespaces());
        }

        return namespaces;
    }

    public void setNamespaces(Map<String, String> namespaces) {
        receiverDetermination.setNamespaces(namespaces);

        for (TemplateInterfaceDetermination templateInterfaceDetermination : mapInterfaceDeterminations.values()) {
            templateInterfaceDetermination.setNamespaces(namespaces);
        }
    }

    public String getNamespacesAsString() {
        Map<String, String> namespaces = new HashMap<>(receiverDetermination.getNamespaces());
        for (String receiverName : this.receiverDetermination.getCurrentReceiverNames()) {
            namespaces.putAll(this.mapInterfaceDeterminations.get(receiverName).getNamespaces());
        }

        StringBuilder namespacesString = new StringBuilder();
        for (String key : namespaces.keySet()) {
            namespacesString.append(" xmlns:").append(key).append("=\"").append(namespaces.get(key)).append("\"");
        }
        return namespacesString.toString();
    }

    // params

    public Set<String> getParams() {
        Set<String> params = new HashSet<>(receiverDetermination.getParams());

        for (TemplateInterfaceDetermination templateInterfaceDetermination : mapInterfaceDeterminations.values()) {
            params.addAll(templateInterfaceDetermination.getParams());
        }

        return params;
    }

    public void setParams() {
        receiverDetermination.setParams();

        for (TemplateInterfaceDetermination templateInterfaceDetermination : mapInterfaceDeterminations.values()) {
            templateInterfaceDetermination.setParams();
        }
    }

    // receiver determination

    public TemplateReceiverDetermination getReceiverDetermination() {
        return receiverDetermination;
    }

    public void setReceiverDetermination(TemplateReceiverDetermination receiverDetermination) {
        this.receiverDetermination = receiverDetermination;
    }

    // interface determinations

    public Map<String, TemplateInterfaceDetermination> getMapInterfaceDeterminations() {
        return mapInterfaceDeterminations;
    }

    public void setMapInterfaceDeterminations(Map<String, TemplateInterfaceDetermination> mapInterfaceDeterminations) {
        this.mapInterfaceDeterminations = mapInterfaceDeterminations;
    }

    // helper methods

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

    public void xsltsToObjectCombinedDetermination(String xsltReceiver) throws XsltNotExistsException, XsltSyntaxException { // for merging XSLTs
        this.clear();

        try {
            this.receiverDetermination.xsltToObjectReceiverDetermination(xsltReceiver);
        } catch (Exception e) {
            throw new XsltSyntaxException(null, false);
        }

        String xsltInterfaceDetermination;
        for (String receiverName : this.receiverDetermination.getCurrentReceiverNames()) {
            this.mapInterfaceDeterminations.put(receiverName, new TemplateInterfaceDetermination());
            try {
                xsltInterfaceDetermination = currentInterfaceDeterminationsList.get(receiverName).getValueNotEmpty();
            } catch (Exception e) {
                throw new XsltNotExistsException(receiverName);
            }
            try {
                this.mapInterfaceDeterminations.get(receiverName).xsltToObjectInterfaceDetermination(xsltInterfaceDetermination);
            } catch (Exception e) {
                throw new XsltSyntaxException(receiverName, true);
            }
        }

        this.setParams();
    }
}
