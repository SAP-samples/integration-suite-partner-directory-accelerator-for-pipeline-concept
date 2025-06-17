package org.example.templates;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.utils.SharedData.*;

public class TemplateReceiverDetermination implements TemplateObjects {
    private String type;
    private String defaultReceiver;
    private final Map<String, List<String>> hashMapConditionReceiver = new LinkedHashMap<>();
    private Set<String> params = new HashSet<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null) {
            this.type = LABEL_ERROR;
        } else if (type.equals(LABEL_ERROR) || type.equals(LABEL_IGNORE) || type.equals(LABEL_DEFAULT)) {
            this.type = type;
        } else {
            this.type = LABEL_ERROR;
        }
    }

    public String getDefaultReceiver() {
        return defaultReceiver;
    }

    public void setDefaultReceiver(String defaultReceiver) {
        this.defaultReceiver = defaultReceiver;
    }

    public Map<String, List<String>> getHashMapConditionReceiver() {
        return hashMapConditionReceiver;
    }

    public void setHashMapConditionReceiver(String xpathForReceiver, String receiverSystem) {
        this.hashMapConditionReceiver.computeIfAbsent(xpathForReceiver, k -> new ArrayList<>()).stream()
                .filter(receiver -> receiver.equals(receiverSystem))
                .findFirst()
                .ifPresentOrElse(receiver -> {
                        },
                        () -> this.hashMapConditionReceiver.get(xpathForReceiver).add(receiverSystem));
    }

    public String[][] getHashMapConditionReceiverForTable() {
        List<String[]> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : this.getHashMapConditionReceiver().entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                result.add(new String[]{key, value});
            }
        }
        if (this.getHashMapConditionReceiver().isEmpty()) {
            result.add(new String[]{"", ""});
        }

        return result.toArray(new String[0][]);
    }

    public void setParams() {
        params.clear();

        Pattern pattern = Pattern.compile("\\$(\\w+)(?=\\s|=)");

        for (String condition : hashMapConditionReceiver.keySet()) {
            Matcher matcher = pattern.matcher(condition);
            while (matcher.find()) {
                params.add(matcher.group(1));
            }

        }
    }

    public Set<String> getParams() {
        return params;
    }

    public List<String> getCurrentReceiverNames() {
        Set<String> receiverNames = new HashSet<>();
        String[][] dataToInsert = this.getHashMapConditionReceiverForTable();
        for (String[] strings : dataToInsert) {
            receiverNames.add(strings[1]);
        }
        String defaultReceiver = this.getDefaultReceiver();
        if (defaultReceiver != null && !defaultReceiver.isEmpty()) {
            receiverNames.add(defaultReceiver);
        }

        List<String> sortedReceiverNames = new ArrayList<>(receiverNames);
        Collections.sort(sortedReceiverNames);
        return sortedReceiverNames;
    }

    public void clear() {
        this.type = null;
        this.defaultReceiver = null;
        this.hashMapConditionReceiver.clear();
        this.params.clear();
    }

    public void xsltToObjectReceiverDetermination(String xslt) throws Exception {
        this.clear();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xslt)));
        XPath xPath = XPathFactory.newInstance().newXPath();

        // not found
        XPathExpression expression = xPath.compile("//ReceiverNotDetermined/Type/text()");
        String type = (String) expression.evaluate(document, XPathConstants.STRING);
        this.setType(type);
        if (type.equals("Default")) {
            expression = xPath.compile("//ReceiverNotDetermined/DefaultReceiver/Service/text()");
            String defaultReceiver = (String) expression
                    .evaluate(document, XPathConstants.STRING);
            this.setDefaultReceiver(defaultReceiver);
        }

        // receivers
        XPathExpression expr = xPath.compile("//Receiver/Service/text()");
        NodeList nodeList = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String receiverName = node.getNodeValue();
            expr = xPath.compile("//*[@test]/Receiver/Service[text()='" + node.getNodeValue() + "']/parent::Receiver/../@test");
            String condition = (String) expr.evaluate(document, XPathConstants.STRING);
            this.setHashMapConditionReceiver(condition, receiverName);
        }
    }
}
