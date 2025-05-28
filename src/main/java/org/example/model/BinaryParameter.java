package org.example.model;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.StringReader;
import java.util.*;

import static org.example.utils.SharedData.*;

public class BinaryParameter {
    private String pid;
    private String id;
    private String contentType;
    private String value;

    public BinaryParameter(String id) {
        this.id = id;
    }

    public BinaryParameter(String pid, String id, String contentType, String value) {
        this.pid = pid;
        this.id = id;
        this.contentType = contentType;
        this.value = value;
    }

    public String getPid() {
        return pid;
    }

    public String getId() {
        return id;
    }

    public String getContentType() {
        return contentType;
    }

    public String getValue() {
        return value;
    }

    public String getValueNotEmpty() {
        if (value == null || value.isEmpty()) {
            return XSLT_NOT_NULL;
        } else {
            return value;
        }
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void clear() {
        this.pid = null;
        if (id != null && !id.equals(ID_RECEIVER_DETERMINATION)) {
            this.id = null;
        }
        this.contentType = null;
        this.value = null;
    }

    public void updateCurrentBinaryParameter(String pid, String id, String contentType, String value) {
        this.pid = pid;
        this.id = id;
        this.contentType = contentType;
        this.value = value;
    }

    public Set<String> getSetReceiverNames() {
        Set<String> receiverNames = new LinkedHashSet<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(this.getValueNotEmpty())));
            XPath xPath = XPathFactory.newInstance().newXPath();

            XPathExpression expression = xPath.compile("//ReceiverNotDetermined/Type/text()");
            String type = (String) expression.evaluate(document, XPathConstants.STRING);
            if (type.equals("Default")) {
                expression = xPath.compile("//ReceiverNotDetermined/DefaultReceiver/Service/text()");
                String defaultReceiver = (String) expression.evaluate(document, XPathConstants.STRING);
                receiverNames.add(defaultReceiver);
            }

            expression = xPath.compile("//Receiver/Service/text()");
            NodeList nodeList = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                String receiverName = node.getNodeValue();
                receiverNames.add(receiverName);
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return receiverNames;
    }
}