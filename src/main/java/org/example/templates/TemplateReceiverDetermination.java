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

import static org.example.utils.SharedData.*;

public class TemplateReceiverDetermination implements TemplateObjects {
    private Map<String, String> namespaces = new HashMap<>();
    private final Set<String> params = new HashSet<>(); // e.g. dc_country like in <xsl:if test="$dc_country = 'DE'"> and <xsl:param name="dc_country"/>
    private String type;
    private String defaultReceiver;
    private final Map<String, List<String>> hashMapConditionReceiver = new LinkedHashMap<>(); // XPath condition with receiver system(s)

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

        Pattern pattern = Pattern.compile("\\$(\\w+)(?=\\s|=)"); // extract params from conditions

        for (String condition : hashMapConditionReceiver.keySet()) {
            Matcher matcher = pattern.matcher(condition);
            while (matcher.find()) {
                params.add(matcher.group(1));
            }

        }
    }

    // type

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

    // default receiver

    public String getDefaultReceiver() {
        return defaultReceiver;
    }

    public void setDefaultReceiver(String defaultReceiver) {
        this.defaultReceiver = defaultReceiver;
    }

    // conditions and receivers

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

    // helper methods

    public void clear() {
        this.namespaces.clear();
        this.params.clear();
        this.type = null;
        this.defaultReceiver = null;
        this.hashMapConditionReceiver.clear();
    }

    public void xsltToObjectReceiverDetermination(String xslt) throws Exception {
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

        // type
        XPathSelector selector = xpath.compile("//ReceiverNotDetermined/Type/text()").load();
        selector.setContextItem(processor.newDocumentBuilder().wrap(document));
        XdmItem typeItem = selector.evaluateSingle();
        String type = (typeItem == null) ? "" : typeItem.getStringValue();
        this.setType(type);

        // default receiver
        if (this.type.equals(LABEL_DEFAULT)) {
            selector = xpath.compile("//ReceiverNotDetermined/DefaultReceiver/Service/text()").load();
            selector.setContextItem(processor.newDocumentBuilder().wrap(document));
            XdmItem defaultReceiverItem = selector.evaluateSingle();
            String defaultReceiver = (defaultReceiverItem == null) ? "" : defaultReceiverItem.getStringValue();
            this.setDefaultReceiver(defaultReceiver);
        }

        // conditions and receivers
        selector = xpath.compile("//Receiver/Service/text()").load();
        selector.setContextItem(processor.newDocumentBuilder().wrap(document));
        for (XdmItem item : selector) {
            String receiverName = item.getStringValue();

            XPathSelector conditionSelector = xpath.compile("//*[@test]/Receiver/Service[text()='" + receiverName + "']/parent::Receiver/../@test").load();
            conditionSelector.setContextItem(processor.newDocumentBuilder().wrap(document));
            XdmItem conditionItem = conditionSelector.evaluateSingle();
            String condition = (conditionItem == null) ? "" : conditionItem.getStringValue();

            this.setHashMapConditionReceiver(condition, receiverName);
        }
    }
}
