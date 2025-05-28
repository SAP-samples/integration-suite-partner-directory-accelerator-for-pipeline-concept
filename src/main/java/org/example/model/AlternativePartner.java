package org.example.model;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.Arrays;

import static org.example.utils.SharedData.*;

public class AlternativePartner {
    private boolean isSelected;
    private final String agency;
    private final String scheme;
    private final String id;
    private final String pid;
    private String determinationType;

    public AlternativePartner(String agency, String scheme, String id, String pid, String determinationType) {
        this.agency = agency;
        this.scheme = scheme;
        this.id = id;
        this.pid = pid;
        this.determinationType = determinationType;
    }

    public AlternativePartner(String agency, String scheme, String id, String pid) {
        this.agency = agency;
        this.scheme = scheme;
        this.id = id;
        this.pid = pid;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getAgency() {
        return agency;
    }

    public String getScheme() {
        return scheme;
    }

    public String getId() {
        return id;
    }

    public String getPid() {
        return pid;
    }

    public String getDeterminationType() {
        if (this.determinationType != null) {
            return this.determinationType;
        } else if (currentStringParametersList.containsKey(ID_RECEIVER_DETERMINATION)) {
            this.setDeterminationType(LABEL_POINT_TO_POINT);
        } else {
            try {
                String xslt = currentReceiverDetermination.getValue();
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(xslt)));
                XPath xPath = XPathFactory.newInstance().newXPath();

                XPathExpression expression = xPath.compile("//Receiver/Interfaces");
                NodeList nodeListInterfaces = (NodeList) expression.evaluate(document, XPathConstants.NODESET);

                expression = xPath.compile("//Receiver");
                NodeList nodeListReceivers = (NodeList) expression.evaluate(document, XPathConstants.NODESET);

                if (nodeListInterfaces.getLength() > 0) {
                    this.setDeterminationType(LABEL_COMBINED_XSLT);
                } else if (nodeListReceivers.getLength() > 0) {
                    this.setDeterminationType(LABEL_MULTIPLE_XSLTS);
                }
            } catch (Exception e) {
                this.setDeterminationType(LABEL_COMBINED_XSLT);
            }
        }
        return this.determinationType;
    }

    public void setDeterminationType(String determinationType) {
        if (Arrays.asList(LABELS_DETERMINATION_TYPES).contains(determinationType)) {
            this.determinationType = determinationType;
        } else {
            this.determinationType = LABEL_COMBINED_XSLT;
        }
    }

    public static void removeAlternativePartnerFromList(AlternativePartner partnerToRemove) {
        currentAlternativePartnersList.removeIf(partner -> partner.getAgency().equals(partnerToRemove.getAgency())
                && partner.getScheme().equals(partnerToRemove.getScheme())
                && partner.getPid().equals(partnerToRemove.getPid())
                && partner.getId().equals(partnerToRemove.getId()));
    }

    public static void addAlternativePartnerToList(AlternativePartner partnerToAdd) {
        if (!isDuplicate(partnerToAdd)) {
            currentAlternativePartnersList.add(partnerToAdd);
        }
    }

    public static boolean isDuplicate(AlternativePartner newPartner) {
        for (AlternativePartner partner : currentAlternativePartnersList) {
            if (partner.getAgency().equals(newPartner.getAgency()) &&
                    partner.getScheme().equals(newPartner.getScheme()) &&
                    partner.getId().equals(newPartner.getId()) &&
                    partner.getPid().equals(newPartner.getPid())) {
                return true;
            }
        }
        return false;
    }
}