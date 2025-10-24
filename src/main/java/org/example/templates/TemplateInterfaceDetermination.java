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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateInterfaceDetermination implements TemplateObjects {
    private final Map<String, List<String>> hashMapConditionService = new LinkedHashMap<>();
    private Set<String> params = new HashSet<>();

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

    public void setParams() {
        params.clear();

        Pattern pattern = Pattern.compile("\\$(\\w+)(?=\\s|=)");

        for (String condition : hashMapConditionService.keySet()) {
            Matcher matcher = pattern.matcher(condition);
            while (matcher.find()) {
                params.add(matcher.group(1));
            }

        }
    }

    public Set<String> getParams() {
        return params;
    }

    public void clear() {
        this.hashMapConditionService.clear();
    }

    public void xsltToObjectInterfaceDetermination(String xslt) throws Exception { // only for multiple XSLTs
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

    public void xsltToObjectInterfaceDetermination(String xslt, String receiverName) throws Exception { // only for combined XSLTs
        this.clear();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xslt)));
        XPath xPath = XPathFactory.newInstance().newXPath();

        XPathExpression expr = xPath.compile("//Service[text()='" + receiverName + "']/following-sibling::Interfaces//Interface/Service/text()");
        NodeList nodeList = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String interfaceName = node.getNodeValue();

            expr = xPath.compile("//Service[text()='" + receiverName + "']/following-sibling::Interfaces//Interface/Service[text()='" + interfaceName + "']/parent::Interface/../@test");
            String condition = (String) expr.evaluate(document, XPathConstants.STRING); // if condition does not exist in XSLT, this string is empty

            this.setHashMapConditionService(condition, interfaceName);
        }
    }
}
