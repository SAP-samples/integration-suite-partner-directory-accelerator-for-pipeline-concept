package org.example.api;

import org.example.model.AlternativePartner;
import org.example.model.BinaryParameter;
import org.example.model.StringParameter;
import org.json.JSONArray;
import org.json.JSONObject;
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
import java.util.*;

import static org.example.utils.SharedData.*;

public class JsonApiHandler {
    public void parseAlternativePartnersJson(JSONObject jsonResponseBody, boolean includeLandscape, Set<String> uniquePids) {
        List<AlternativePartner> alternativePartnerList = new ArrayList<>();
        JSONObject dObject = jsonResponseBody.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            String pid = resultObject.getString(JSON_KEY_PID);
            String agency = resultObject.getString(JSON_KEY_AGENCY);
            String id = resultObject.getString(JSON_KEY_ID);
            String scheme = resultObject.getString(JSON_KEY_SCHEME);

            if (!includeLandscape) {
                if (SCHEME_SENDER_INTERFACE.equals(scheme) || uniquePids.contains(pid)) {
                    alternativePartnerList.add(new AlternativePartner(agency, scheme, id, pid));
                }
            } else {
                if (SCHEME_SENDER_INTERFACE.equals(scheme) || uniquePids.contains(pid) || currentLandscapeTenantParameters.entrySet().stream().anyMatch(entry -> entry.getValue().equals(agency))) {
                    alternativePartnerList.add(new AlternativePartner(agency, scheme, id, pid));
                }
            }
        }

        currentAlternativePartnersList = alternativePartnerList;
    }

    public void parseAlternativePartnersJsonLandscape(JSONObject jsonResponseBody) {
        List<AlternativePartner> alternativePartnerList = new ArrayList<>();
        JSONObject dObject = jsonResponseBody.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            String pid = resultObject.getString(JSON_KEY_PID);
            String agency = resultObject.getString(JSON_KEY_AGENCY);
            String id = resultObject.getString(JSON_KEY_ID);
            String scheme = resultObject.getString(JSON_KEY_SCHEME);

            alternativePartnerList.add(new AlternativePartner(agency, scheme, id, pid));
        }

        currentLandscapeScenarioParameters = alternativePartnerList;
    }

    public Set<String> getUniquePidsFromEndpoints(JSONObject jsonResponseBodyBinary, JSONObject jsonResponseBodyString) {

        Set<String> uniquePids = new HashSet<>(parsePidsFromJson(jsonResponseBodyBinary));
        if (jsonResponseBodyString != null) {
            uniquePids.addAll(parsePidsFromJson(jsonResponseBodyString));
        }

        return uniquePids;
    }

    private Set<String> parseKeyFromJson(JSONObject jsonResponseBody, String key) {
        Set<String> stringSet = new HashSet<>();
        JSONObject dObject = jsonResponseBody.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            String valueOfKey = resultObject.getString(key);
            stringSet.add(valueOfKey);
        }

        return stringSet;
    }

    public Set<String> parsePidsFromJson(JSONObject jsonResponseBody) {
        return parseKeyFromJson(jsonResponseBody, JSON_KEY_PID);
    }

    public Set<String> parseIdsFromJson(JSONObject jsonResponseBody) {
        return parseKeyFromJson(jsonResponseBody, JSON_KEY_ID);
    }

    public Set<String> checkXsltsForMerging(JSONObject jsonResponseBody, List<String> pidsMultipleXslts) {
        JSONObject dObject = jsonResponseBody.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        Set<String> pidsWithoutCombinedDetermination = new HashSet<>();

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            String pid = resultObject.getString(JSON_KEY_PID);

            if (pidsMultipleXslts.contains(pid)) {
                try {
                    String value = new String(Base64.getDecoder().decode(resultObject.getString(JSON_KEY_VALUE)));

                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document document = builder.parse(new InputSource(new StringReader(value)));
                    XPath xPath = XPathFactory.newInstance().newXPath();

                    XPathExpression expression = xPath.compile("//Receiver/Interfaces");
                    NodeList nodeListInterfaces = (NodeList) expression.evaluate(document, XPathConstants.NODESET);

                    if (nodeListInterfaces.getLength() == 0) {
                        pidsWithoutCombinedDetermination.add(pid);
                    }
                } catch (Exception e) {
                    // if parsing doesn't work, pid is not added
                }
            }
        }

        return pidsWithoutCombinedDetermination;
    }

    public void parseBinaryParametersJson(JSONObject jsonResponseBody) {
        JSONObject dObject = jsonResponseBody.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        currentReceiverDetermination.clear();
        currentInterfaceDeterminationsList.clear();

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            String pid = resultObject.getString(JSON_KEY_PID);
            String id = resultObject.getString(JSON_KEY_ID);
            String contentType = resultObject.getString(JSON_KEY_CONTENT_TYPE);
            String value = new String(Base64.getDecoder().decode(resultObject.getString(JSON_KEY_VALUE)));

            if (id.equals(ID_RECEIVER_DETERMINATION)) {
                currentReceiverDetermination.updateCurrentBinaryParameter(pid, id, contentType, value);
            } else if (id.startsWith(ID_INTERFACE_DETERMINATION_)) {
                BinaryParameter binaryParameter = new BinaryParameter(pid, id, contentType, value);
                currentInterfaceDeterminationsList.put(id.replace(ID_INTERFACE_DETERMINATION_, ""), binaryParameter);
            }
        }
    }

    public void parseStringParametersJson(JSONObject jsonResponseBody) {
        JSONObject dObject = jsonResponseBody.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        currentStringParametersList.clear();

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            String pid = resultObject.getString(JSON_KEY_PID);
            String id = resultObject.getString(JSON_KEY_ID);
            String value = resultObject.getString(JSON_KEY_VALUE);

            StringParameter stringParameter = new StringParameter(pid, id, value);

            if (id.equals(STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE)) {
                id = id + pid;
            }

            currentStringParametersList.put(id, stringParameter);
        }
    }

    public void parseStringParameterLandscapeJson(JSONObject jsonResponseBody) {
        JSONObject dObject = jsonResponseBody.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        currentLandscapeTenantParameters.clear();

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            String id = resultObject.getString(JSON_KEY_ID);
            String value = resultObject.getString(JSON_KEY_VALUE);

            currentLandscapeTenantParameters.put(id, value);
        }
    }

    public boolean isResultsEmpty(JSONObject jsonResponseBody) {
        JSONObject dObject = jsonResponseBody.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);
        return resultsArray.isEmpty();
    }
}