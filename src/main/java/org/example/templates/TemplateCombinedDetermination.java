package org.example.templates;

import org.example.exceptions.XsltNotExistsException;
import org.example.exceptions.XsltSyntaxException;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.example.utils.SharedData.*;

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
    }
}
