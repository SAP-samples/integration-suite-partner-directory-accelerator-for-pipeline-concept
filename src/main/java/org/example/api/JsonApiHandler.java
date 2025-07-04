package org.example.api;

import org.example.model.AlternativePartner;
import org.example.model.BinaryParameter;
import org.example.model.StringParameter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static org.example.utils.SharedData.*;

public class JsonApiHandler {
    public void parseAlternativePartnersJson(String jsonResponse) {
        List<AlternativePartner> partnerList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject dObject = jsonObject.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            String pid = resultObject.getString(JSON_KEY_PID);
            String agency = resultObject.getString(JSON_KEY_AGENCY);
            String id = resultObject.getString(JSON_KEY_ID);
            String scheme = resultObject.getString(JSON_KEY_SCHEME);
            partnerList.add(new AlternativePartner(agency, scheme, id, pid));
        }
        currentAlternativePartnersList = partnerList;
    }

    public void parseBinaryParametersJson(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject dObject = jsonObject.getJSONObject(JSON_KEY_D);
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

    public void parseStringParametersJson(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject dObject = jsonObject.getJSONObject(JSON_KEY_D);
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

    public Map<String, String> parseStringParameterLandscapeJson(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject dObject = jsonObject.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        Map<String, String> landscapeMapIdValue = new HashMap<>();

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            String id = resultObject.getString(JSON_KEY_ID);
            String value = resultObject.getString(JSON_KEY_VALUE);

            landscapeMapIdValue.put(id, value);
        }

        return landscapeMapIdValue;
    }

    public String getUriFromStringParametersJson(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject dObject = jsonObject.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        JSONObject resultObject = resultsArray.getJSONObject(0);
        JSONObject metadataObject = resultObject.getJSONObject(JSON_KEY_METADATA);
        return metadataObject.getString(JSON_KEY_URI);
    }

    public boolean isResultsEmpty(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject dObject = jsonObject.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);
        return resultsArray.isEmpty();
    }
}