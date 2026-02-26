package org.example.templates;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateInterfaceDetermination implements TemplateObjects {
    private Map<String, String> namespaces = new HashMap<>();
    private final Set<String> params = new HashSet<>(); // e.g. dc_country like in <xsl:if test="$dc_country = 'DE'"> and <xsl:param name="dc_country"/>
    private final Map<String, List<String>> hashMapConditionService = new LinkedHashMap<>(); // XPath condition with receiver interface(s)

    // namespaces

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces = new HashMap<>(namespaces);
    }

    public String getNamespacesAsString() {
        StringBuilder namespacesString = new StringBuilder();
        for (String key : namespaces.keySet()) {
            namespacesString.append(" xmlns:").append(key).append("=\"").append(namespaces.get(key)).append("\"");
        }
        return namespacesString.toString();
    }

    // params

    public Set<String> getParams() {
        return params;
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

    // conditions and interfaces

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
        if (this.getHashMapConditionService().isEmpty()) {
            result.add(new String[]{"", ""});
        }

        return result.toArray(new String[0][]);
    }

    // helper methods

    public void clear() {
        this.namespaces.clear();
        this.params.clear();
        this.hashMapConditionService.clear();
    }

    public void xsltToObjectInterfaceDetermination(String xslt) throws Exception { // only for multiple XSLTs
        this.clear();

        Processor processor = new Processor(false);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xslt)));

        XPathCompiler xpath = processor.newXPathCompiler();

        // namespaces
        Element root = document.getDocumentElement();
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            if (attr.getNodeName().startsWith("xmlns:") &&
                    !attr.getNodeValue().equals("http://www.w3.org/1999/XSL/Transform")) {
                String prefix = attr.getNodeName().substring(6);
                xpath.declareNamespace(prefix, attr.getNodeValue());
                this.namespaces.put(prefix, attr.getNodeValue());
            }
        }

        // conditions and interfaces
        XPathSelector selector = xpath.compile("//Interface/Service/text()").load();
        selector.setContextItem(processor.newDocumentBuilder().wrap(document));
        for (XdmItem item : selector) {
            String interfaceName = item.getStringValue();

            XPathSelector conditionSelector = xpath.compile("//*[@test]/Interface/Service[text()='" + interfaceName + "']/parent::Interface/../@test").load();
            conditionSelector.setContextItem(processor.newDocumentBuilder().wrap(document));
            XdmItem conditionItem = conditionSelector.evaluateSingle();
            String condition = (conditionItem == null) ? "" : conditionItem.getStringValue();

            this.setHashMapConditionService(condition, interfaceName);
        }
    }

    public void xsltToObjectInterfaceDetermination(String xslt, String receiverName) throws Exception { // only for combined XSLTs
        this.clear();

        Processor processor = new Processor(false);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xslt)));

        XPathCompiler xpath = processor.newXPathCompiler();

        // namespaces
        Element root = document.getDocumentElement();
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            if (attr.getNodeName().startsWith("xmlns:") &&
                    !attr.getNodeValue().equals("http://www.w3.org/1999/XSL/Transform")) {
                String prefix = attr.getNodeName().substring(6);
                xpath.declareNamespace(prefix, attr.getNodeValue());
                this.namespaces.put(prefix, attr.getNodeValue());
            }
        }

        // conditions and interfaces
        XPathSelector selector = xpath.compile("//Service[text()='" + receiverName + "']/following-sibling::Interfaces//Interface/Service/text()").load();
        selector.setContextItem(processor.newDocumentBuilder().wrap(document));
        for (XdmItem item : selector) {
            String interfaceName = item.getStringValue();

            XPathSelector conditionSelector = xpath.compile("//Service[text()='" + receiverName + "']/following-sibling::Interfaces//Interface/Service[text()='" + interfaceName + "']/parent::Interface/../@test").load();
            conditionSelector.setContextItem(processor.newDocumentBuilder().wrap(document));
            XdmItem conditionItem = conditionSelector.evaluateSingle();
            String condition = (conditionItem == null) ? "" : conditionItem.getStringValue();

            this.setHashMapConditionService(condition, interfaceName);
        }
    }
}
