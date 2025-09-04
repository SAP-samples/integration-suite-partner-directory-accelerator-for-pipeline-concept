package org.example.api;

import org.example.model.AlternativePartner;
import org.example.model.BinaryParameter;
import org.example.model.StringParameter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static org.example.utils.SharedData.*;

public class JsonApiHandler {
    public void parseAlternativePartnersJson(String jsonResponse, boolean includeLandscape, Set<String> uniquePids) {
        List<AlternativePartner> alternativePartnerList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject dObject = jsonObject.getJSONObject(JSON_KEY_D);
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

    public void parseAlternativePartnersJsonLandscape(String jsonResponse) {
        List<AlternativePartner> alternativePartnerList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject dObject = jsonObject.getJSONObject(JSON_KEY_D);
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

    public Set<String> getUniquePidsFromEndpoints(String jsonResponseBinary, String jsonResponseString) {
        Set<String> uniquePids = new HashSet<>();

        uniquePids.addAll(parsePidsFromJson(jsonResponseBinary));
        uniquePids.addAll(parsePidsFromJson(jsonResponseString));

        return uniquePids;
    }

    private Set<String> parsePidsFromJson(String jsonResponse) {
        Set<String> pids = new HashSet<>();
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject dObject = jsonObject.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            String pid = resultObject.getString(JSON_KEY_PID);
            pids.add(pid);
        }

        return pids;
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

    public void parseStringParameterLandscapeJson(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONObject dObject = jsonObject.getJSONObject(JSON_KEY_D);
        JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);

        currentLandscapeTenantParameters.clear();

        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            String id = resultObject.getString(JSON_KEY_ID);
            String value = resultObject.getString(JSON_KEY_VALUE);

            currentLandscapeTenantParameters.put(id, value);
        }
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