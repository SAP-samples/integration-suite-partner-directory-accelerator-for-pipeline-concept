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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TemplateInterfaceDetermination implements Clearable {
    private final Map<String, List<String>> hashMapConditionService = new LinkedHashMap<>();

    public Map<String, List<String>> getHashMapConditionService() {
        return hashMapConditionService;
    }

    public void setHashMapConditionService(String xpathForInterface, String interfaceService) {
        this.hashMapConditionService.computeIfAbsent(xpathForInterface, k -> new ArrayList<>()).stream()
                .filter(service -> service.equals(interfaceService))
                .findFirst()
                .ifPresentOrElse(service -> {
                        },
                        () -> this.hashMapConditionService.get(xpathForInterface).add(interfaceService));
    }

    public String[][] getHashMapConditionInterfaceForTable() {
        List<String[]> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : this.getHashMapConditionService().entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                result.add(new String[]{key, value});
            }
        }
        if (this.getHashMapConditionService().entrySet().isEmpty()) {
            result.add(new String[]{"", ""});
        }

        return result.toArray(new String[0][]);
    }

    public void clear() {
        this.hashMapConditionService.clear();
    }

    public void xsltToObjectInterfaceDetermination(String xslt) throws Exception {
        this.clear();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xslt)));
        XPath xPath = XPathFactory.newInstance().newXPath();

        XPathExpression expr = xPath.compile("//Interface/Service/text()");
        NodeList nodeList = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String interfaceName = node.getNodeValue();
            expr = xPath.compile("//*[@test]/Interface/Service[text()='" + node.getNodeValue() + "']/parent::Interface/../@test");
            String condition = (String) expr.evaluate(document, XPathConstants.STRING);
            this.setHashMapConditionService(condition, interfaceName);
        }
    }

    public void xsltToObjectInterfaceDetermination(String xslt, String receiverName) throws Exception {
        this.clear();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xslt)));
        XPath xPath = XPathFactory.newInstance().newXPath();

        XPathExpression expr = xPath.compile("//Interface/Service/text()");
        NodeList nodeList = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String interfaceName = node.getNodeValue();

            // interfaces with condition
            expr = xPath.compile("//*[@test]/Interface/Service[text()='" + interfaceName + "']/../../../../child::Service/text()");
            String receiverOfNode = (String) expr.evaluate(document, XPathConstants.STRING);
            if (receiverOfNode.equals(receiverName)) {
                expr = xPath.compile("//*[@test]/Interface/Service[text()='" + interfaceName + "']/parent::Interface/../@test");
                String condition = (String) expr.evaluate(document, XPathConstants.STRING);
                this.setHashMapConditionService(condition, interfaceName);
            } else {
                // interfaces without condition
                expr = xPath.compile("//Interface/Service[text()='" + interfaceName + "']/../../../child::Service/text()");
                receiverOfNode = (String) expr.evaluate(document, XPathConstants.STRING);
                if (receiverOfNode.equals(receiverName)) {
                    this.setHashMapConditionService("", interfaceName);
                }
            }
        }
    }
}
