package org.example.api;

import org.apache.commons.codec.binary.Hex;
import org.example.model.AlternativePartner;
import org.example.utils.TenantCredentials;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.example.utils.SharedData.*;

public class HttpRequestHandler {
    private final Builder requestBuilder;
    private final HttpClient client;
    private final int indentFactor = 4;
    private final String url;

    private String latestResponseLabel;
    private int latestStatusCode;
    private String latestResponseType;
    private HttpResponse<String> latestResponse;

    private final TenantCredentials tenantCredentials;

    public HttpRequestHandler(TenantCredentials tenantCredentials) throws Exception {
        this.tenantCredentials = tenantCredentials;

        String baseUrl = tenantCredentials.getUrl();

        this.client = HttpClient.newHttpClient();

        String token;
        if (tenantCredentials.isTokenValid()) {
            token = tenantCredentials.getAccessToken();
        } else {
            token = requestToken();
        }

        requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
        this.url = baseUrl;
    }

    private String requestToken() throws Exception {
        HttpRequest requestToken = HttpRequest.newBuilder()
                .uri(URI.create(tenantCredentials.getTokenurl()))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((tenantCredentials.getClientid() + ":" + tenantCredentials.getClientsecret()).getBytes()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();

        HttpResponse<String> responseToken = client.send(requestToken, HttpResponse.BodyHandlers.ofString());

        setResponseAndLog(responseToken, requestToken.method());

        JSONObject tokenResponse = new JSONObject(responseToken.body());
        String token = tokenResponse.getString(JSON_KEY_ACCESS_TOKEN);
        long tokenExpiresInSeconds = tokenResponse.getInt(JSON_KEY_EXPIRES_IN);
        String tokenExpirationDateTime = calculateExpirationDateTime(tokenExpiresInSeconds);

        tenantCredentials.setAccessToken(token);
        tenantCredentials.setTokenExpirationDateTime(tokenExpirationDateTime);
        jsonFileHandler.saveJsonFile();

        return token;
    }

    private void requestTokenIfExpired() throws Exception {
        if (!tenantCredentials.isTokenValid()) {
            String token = requestToken();
            requestBuilder.setHeader("Authorization", "Bearer " + token);
        }
    }

    private String calculateExpirationDateTime(long tokenExpiresInSeconds) {
        Instant now = Instant.now();
        Instant expirationInstant = now.plusSeconds(tokenExpiresInSeconds);
        LocalDateTime expirationDateTime = LocalDateTime.ofInstant(expirationInstant, ZoneId.of("UTC"));
        return expirationDateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER_PATTERN)) + " UTC";
    }

    // JSON request bodies

    private String createRequestBodyPostAlternativePartners(String agency, String scheme, String id, String pid) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_KEY_AGENCY, agency);
        jsonObject.put(JSON_KEY_SCHEME, scheme);
        jsonObject.put(JSON_KEY_ID, id);
        jsonObject.put(JSON_KEY_PID, pid);
        return jsonObject.toString(indentFactor);
    }

    private String createRequestBodyPutAlternativePartners(String pid) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_KEY_PID, pid);
        return jsonObject.toString(indentFactor);
    }

    private String createRequestBodyPostBinaryParameters(String pid, String id, String valueEncoded) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_KEY_PID, pid);
        jsonObject.put(JSON_KEY_ID, id);
        jsonObject.put(JSON_KEY_CONTENT_TYPE, JSON_VALUE_XSL);
        jsonObject.put(JSON_KEY_VALUE, valueEncoded);
        return jsonObject.toString(indentFactor);
    }

    private String createRequestBodyPutBinaryParameters(String valueEncoded) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_KEY_CONTENT_TYPE, JSON_VALUE_XSL);
        jsonObject.put(JSON_KEY_VALUE, valueEncoded);
        return jsonObject.toString(indentFactor);
    }

    private String createRequestBodyPostStringParameters(String pid, String id, String valueEncoded) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_KEY_PID, pid);
        jsonObject.put(JSON_KEY_ID, id);
        jsonObject.put(JSON_KEY_VALUE, valueEncoded);
        return jsonObject.toString(indentFactor);
    }

    private String createRequestBodyPutStringParameters(String valueEncoded) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_KEY_VALUE, valueEncoded);
        return jsonObject.toString(indentFactor);
    }

    // AlternativePartners

    public String sendGetRequestAlternativePartners(boolean includeStringParameters) throws Exception {
        JSONObject pidsFromBinaryParameters = sendGetRequestParametersWithReceiverDetermination(API_BINARY_PARAMETERS);

        JSONObject pidsFromStringParameters = null;
        if (includeStringParameters) {
            pidsFromStringParameters = sendGetRequestParametersWithReceiverDetermination(API_STRING_PARTNERS);
        }
        Set<String> uniquePids = jsonApiHandler.getUniquePidsFromEndpoints(pidsFromBinaryParameters, pidsFromStringParameters);

        JSONObject jsonResponseBody = sendGetRequestsAndHandlePagination(API_ALTERNATIVE_PARTNERS);
        jsonApiHandler.parseAlternativePartnersJson(jsonResponseBody, false, uniquePids);
        return this.latestResponseLabel;
    }

    public void sendGetRequestAlternativePartnersLandscape(Set<String> listSystemNames) throws Exception {
        StringBuilder pidFilter = new StringBuilder();
        for (String receiverName : listSystemNames) {
            if (!pidFilter.isEmpty()) {
                pidFilter.append("%20or%20");
            }
            pidFilter.append("Pid%20eq%20'").append(receiverName).append("'");
        }

        String filter = "?$filter=(" + pidFilter + ")%20and%20(Scheme%20eq%20'" + SCHEME_BUSINESS_SYSTEM_NAME
                + "'%20or%20Scheme%20eq%20'" + SCHEME_LOGICAL_SYSTEM_NAME + "')";

        String endpoint = API_ALTERNATIVE_PARTNERS + filter;
        JSONObject jsonResponseBody = sendGetRequestsAndHandlePagination(endpoint);
        jsonApiHandler.parseAlternativePartnersJsonLandscape(jsonResponseBody);
    }

    public String sendPostRequestAlternativePartners(String agency, String scheme, String id, String pid) throws Exception {
        return sendPostRequestAlternativePartners(agency, scheme, id, pid, true);
    }

    private String sendPostRequestAlternativePartners(String agency, String scheme, String id, String pid, boolean isResponseLogged) throws Exception {
        String jsonBody = createRequestBodyPostAlternativePartners(agency, scheme, id, pid);
        return sendPostRequest(API_ALTERNATIVE_PARTNERS, jsonBody, isResponseLogged);
    }

    private void sendPutRequestAlternativePartners(String agency, String scheme, String id, String pid) throws Exception {
        String hexAgency = convertStringToHexstring(agency);
        String hexScheme = convertStringToHexstring(scheme);
        String hexId = convertStringToHexstring(id);
        String endpoint = API_ALTERNATIVE_PARTNERS + "(" + JSON_KEY_HEXAGENCY + "='" + hexAgency + "'," + JSON_KEY_HEXSCHEME + "='" + hexScheme + "'," + JSON_KEY_HEXID + "='" + hexId + "')";
        String jsonBody = createRequestBodyPutAlternativePartners(pid);
        sendPutRequest(endpoint, jsonBody, false);
    }

    public String sendDeleteRequestAlternativePartners(String agency, String scheme, String id) throws Exception {
        String hexAgency = convertStringToHexstring(agency);
        String hexScheme = convertStringToHexstring(scheme);
        String hexId = convertStringToHexstring(id);
        String endpoint = API_ALTERNATIVE_PARTNERS + "(" + JSON_KEY_HEXAGENCY + "='" + hexAgency + "'," + JSON_KEY_HEXSCHEME + "='" + hexScheme + "'," + JSON_KEY_HEXID + "='" + hexId + "')";
        return sendDeleteRequest(endpoint);
    }

    // both BinaryParameters and StringParameters

    public List<String> sendGetRequestsPidsWithTilde() throws Exception {
        JSONObject pidsFromBinaryParameters = sendGetRequestParametersWithReceiverDetermination(API_BINARY_PARAMETERS);
        JSONObject pidsFromStringParameters = sendGetRequestParametersWithReceiverDetermination(API_STRING_PARTNERS);

        Set<String> uniquePidsWithReceiverDetermination = jsonApiHandler.getUniquePidsFromEndpoints(pidsFromBinaryParameters, pidsFromStringParameters);
        uniquePidsWithReceiverDetermination.removeIf(s -> !s.contains(TILDE));

        return uniquePidsWithReceiverDetermination.stream()
                .sorted()
                .toList();
    }

    public JSONObject sendGetRequestParametersWithReceiverDetermination(String endpoint) throws Exception {
        return sendGetRequestsAndHandlePagination(endpoint + "?$filter=" + JSON_KEY_ID + "%20eq%20'" + ID_RECEIVER_DETERMINATION + "'&$select=" + JSON_KEY_PID);
    }

    // BinaryParameters

    public void sendGetRequestBinaryParameters(String pid) throws Exception {
        String endpoint = API_BINARY_PARAMETERS + "?$filter=startswith(" + JSON_KEY_CONTENT_TYPE + ",'" + JSON_VALUE_XSL + "')%20and%20" + JSON_KEY_PID + "%20eq%20'" + pid + "'";
        JSONObject jsonResponseBody = sendGetRequestsAndHandlePagination(endpoint);
        jsonApiHandler.parseBinaryParametersJson(jsonResponseBody);
    }

    public Set<String> sendGetRequestIdsBinaryParameters(String pid) throws Exception { // get all binary parameters of scenario to delete before mering XSLTs
        String endpoint = API_BINARY_PARAMETERS + "?$filter=" + JSON_KEY_PID + "%20eq%20'" + pid + "'%20and%20(" + JSON_KEY_ID + "%20eq%20'" + ID_RECEIVER_DETERMINATION + "'%20or%20startswith(" + JSON_KEY_ID + ",%20'" + ID_INTERFACE_DETERMINATION_ + "'))%20and%20startswith(" + JSON_KEY_CONTENT_TYPE + ",%20'" + JSON_VALUE_XSL + "')&$select=" + JSON_KEY_ID;
        JSONObject jsonResponseBody = sendGetRequestsAndHandlePagination(endpoint);
        return jsonApiHandler.parseIdsFromJson(jsonResponseBody);
    }

    public Set<String> sendGetRequestBinaryParametersIdsInterfaceDetermination() throws Exception { // for merging overview
        String endpoint = API_BINARY_PARAMETERS + "?$filter=startswith(" + JSON_KEY_ID + ",%20'" + ID_INTERFACE_DETERMINATION_ + "')%20and%20startswith(" + JSON_KEY_CONTENT_TYPE + ",%20'" + JSON_VALUE_XSL + "')&$select=" + JSON_KEY_PID;
        JSONObject jsonResponseBody = sendGetRequestsAndHandlePagination(endpoint);
        return jsonApiHandler.parsePidsFromJson(jsonResponseBody);
    }

    public Set<String> sendGetRequestBinaryParametersXsltsReceiverDetermination(List<String> pidsMultipleXslts) throws Exception {
        String endpoint = API_BINARY_PARAMETERS + "?$filter=" + JSON_KEY_ID + "%20eq%20'" + ID_RECEIVER_DETERMINATION + "'%20and%20startswith(" + JSON_KEY_CONTENT_TYPE + ",%20'" + JSON_VALUE_XSL + "')&$select=" + JSON_KEY_PID + ",%20" + JSON_KEY_VALUE;
        JSONObject jsonResponseBody = sendGetRequestsAndHandlePagination(endpoint);
        return jsonApiHandler.checkXsltsForMerging(jsonResponseBody, pidsMultipleXslts);
    }

    public void sendPostRequestBinaryParameters(String pid, String id, String valueAsString) throws Exception {
        sendPostRequestBinaryParameters(pid, id, valueAsString, true);
    }

    private void sendPostRequestBinaryParameters(String pid, String id, String valueAsString, boolean isResponseLogged) throws Exception {
        String valueEncoded = base64Encoding(valueAsString);
        String jsonBody = createRequestBodyPostBinaryParameters(pid, id, valueEncoded);
        sendPostRequest(API_BINARY_PARAMETERS, jsonBody, isResponseLogged);
    }

    public void sendPutRequestBinaryParameters(String pid, String id, String valueAsString, boolean isResponseLogged) throws Exception {
        String endpoint = API_BINARY_PARAMETERS + "(" + JSON_KEY_PID + "='" + pid + "'," + JSON_KEY_ID + "='" + id + "')";
        String valueEncoded = base64Encoding(valueAsString);
        String jsonBody = createRequestBodyPutBinaryParameters(valueEncoded);

        sendPutRequest(endpoint, jsonBody, isResponseLogged);
    }

    public String sendPutPostRequestBinaryParameters(String pid, String id, String valueAsString) throws Exception {
        sendPutRequestBinaryParameters(pid, id, valueAsString, false);

        if (this.latestStatusCode == 404) {
            this.sendPostRequestBinaryParameters(pid, id, valueAsString, false);
        }

        logLatestResponse();
        return latestResponseLabel;
    }

    public void sendDeleteRequestBinaryParameters(String pid, String id) throws Exception {
        String endpoint = API_BINARY_PARAMETERS + "(" + JSON_KEY_PID + "='" + pid + "'," + JSON_KEY_ID + "='" + id + "')";
        sendDeleteRequest(endpoint);
    }

    // String Parameters

    public void sendGetRequestStringParameters(String pid) throws Exception {
        String endpoint = API_STRING_PARTNERS + "?$filter=" + JSON_KEY_PID + "%20eq%20'" + pid + "'";
        JSONObject jsonResponseBody = sendGetRequestsAndHandlePagination(endpoint);
        jsonApiHandler.parseStringParametersJson(jsonResponseBody);
    }

    public void sendGetRequestStringParameters(String pid, Set<String> listReceiverNames) throws Exception {
        StringBuilder filterReceiverSpecificQueue = new StringBuilder();
        for (String receiverName : listReceiverNames) {
            if (!filterReceiverSpecificQueue.isEmpty()) {
                filterReceiverSpecificQueue.append("%20or%20");
            }
            filterReceiverSpecificQueue.append(JSON_KEY_PID + "%20eq%20'").append(receiverName).append("'");
        }

        String endpoint = API_STRING_PARTNERS + "?$filter="
                + JSON_KEY_PID + "%20eq%20'" + pid + "'";

        if (!listReceiverNames.isEmpty()) {
            endpoint += "%20or%20(" + JSON_KEY_ID + "%20eq%20'" + STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE
                    + "'%20and%20(" + filterReceiverSpecificQueue + "))";
        }

        JSONObject jsonResponseBody = sendGetRequestsAndHandlePagination(endpoint);
        jsonApiHandler.parseStringParametersJson(jsonResponseBody);
    }

    public void sendGetRequestStringParameterLandscape() throws Exception {
        String endpoint = API_STRING_PARTNERS + "?$filter=" + JSON_KEY_PID + "%20eq%20'" + STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE + "'";
        JSONObject jsonResponseBody = sendGetRequestsAndHandlePagination(endpoint);
        jsonApiHandler.parseStringParameterLandscapeJson(jsonResponseBody);
    }

    public String sendPostRequestStringParameters(String pid, String id, String value) throws Exception {
        return sendPostRequestStringParameters(pid, id, value, true);
    }

    public String sendPostRequestStringParameters(String pid, String id, String value, boolean isResponseLogged) throws Exception {
        String jsonBody = createRequestBodyPostStringParameters(pid, id, value);
        return sendPostRequest(API_STRING_PARTNERS, jsonBody, isResponseLogged);
    }

    public String sendPutRequestStringParameters(String pid, String id, String value) throws Exception {
        return sendPutRequestStringParameters(pid, id, value, true);
    }

    private String sendPutRequestStringParameters(String pid, String id, String value, boolean isResponseLogged) throws Exception {
        String endpoint = API_STRING_PARTNERS + "(" + JSON_KEY_PID + "='" + pid + "'," + JSON_KEY_ID + "='" + id + "')";
        String jsonBody = createRequestBodyPutStringParameters(value);
        sendPutRequest(endpoint, jsonBody, isResponseLogged);
        return this.latestResponseLabel;
    }

    public String sendDeleteRequestStringParameters(String pid, String id) throws Exception {
        String endpoint = API_STRING_PARTNERS + "(" + JSON_KEY_PID + "='" + pid + "'," + JSON_KEY_ID + "='" + id + "')";
        return sendDeleteRequest(endpoint);
    }

    // Transport methods

    public void sendGetRequestAlternativePartnersTransport() throws Exception {
        JSONObject pidsFromBinaryParameters = sendGetRequestParametersWithReceiverDetermination(API_BINARY_PARAMETERS);
        JSONObject pidsFromStringParameters = sendGetRequestParametersWithReceiverDetermination(API_STRING_PARTNERS);
        Set<String> uniquePids = jsonApiHandler.getUniquePidsFromEndpoints(pidsFromBinaryParameters, pidsFromStringParameters);

        sendGetRequestStringParameterLandscape();

        JSONObject jsonResponseBody = sendGetRequestsAndHandlePagination(API_ALTERNATIVE_PARTNERS);
        jsonApiHandler.parseAlternativePartnersJson(jsonResponseBody, true, uniquePids);
    }

    public void transportAlternativePartners(List<AlternativePartner> alternativePartnersToTransport, boolean overwrite, List<String> transportErrors) {
        for (AlternativePartner alternativePartner : alternativePartnersToTransport) {
            String agency = alternativePartner.getAgency();
            String scheme = alternativePartner.getScheme();
            String id = alternativePartner.getId();
            String pid = alternativePartner.getPid();

            try {
                sendPostRequestAlternativePartners(agency, scheme, id, pid, false);

                if (this.latestStatusCode == 400 && overwrite) {
                    sendPutRequestAlternativePartners(agency, scheme, id, pid);
                }
                logLatestResponse();

                if (!(this.latestStatusCode >= 200 && this.latestStatusCode <= 299)) {
                    transportErrors.add(this.latestResponseLabel);
                }
            } catch (Exception e) {
                String errorMessage = "Error sending HTTP request for alternative partner (agency: " + agency + ", scheme: " + scheme + ", id: " + id + ", pid: " + pid + "): ";
                LOGGER.error("{}{}", errorMessage, e);
                transportErrors.add(errorMessage + e.getMessage());
            }
        }
    }

    public JSONObject getBinaryParametersToTransport(List<String> pidsToTransport) {
        String filter = buildPidFilterBinaryParameters(pidsToTransport);

        try {
            String endpoint = API_BINARY_PARAMETERS + filter;
            return sendGetRequestsAndHandlePagination(endpoint);
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return null;
    }

    public void transportBinaryParameters(JSONObject jsonObjectToTransport, boolean overwrite, List<String> transportErrors, boolean shouldDeleteOldEntries, HashMap<String, String> oldAndNewPids) {
        try {
            JSONObject dObject = jsonObjectToTransport.getJSONObject(JSON_KEY_D);
            JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);
            LOGGER.info(LABEL_TRANSPORT_FOUND_X + "{}" + LABEL_TRANSPORT_FOUND_BINARY, resultsArray.length());

            for (int i = 0; i < resultsArray.length(); i++) {
                try {
                    JSONObject resultObject = resultsArray.getJSONObject(i);

                    String pid;
                    String oldPid = "";
                    if (oldAndNewPids != null) { // for migrate tilde
                        oldPid = resultObject.getString(JSON_KEY_PID);
                        pid = oldAndNewPids.get(oldPid);
                    } else { // for transport
                        pid = resultObject.getString(JSON_KEY_PID);
                    }
                    String id = resultObject.getString(JSON_KEY_ID);
                    String valueAsString = base64Decoding(resultObject.getString(JSON_KEY_VALUE));

                    try {
                        sendPostRequestBinaryParameters(pid, id, valueAsString, false);

                        if (this.latestStatusCode == 400 && overwrite) {
                            sendPutRequestBinaryParameters(pid, id, valueAsString, false);
                        }

                        logLatestResponse();

                        if (!(this.latestStatusCode >= 200 && this.latestStatusCode <= 299)) {
                            transportErrors.add(this.latestResponseLabel);
                        }
                    } catch (Exception e) {
                        String errorMessage = "Error sending HTTP request for binary parameter (id: " + id + ", pid: " + pid + "): ";
                        LOGGER.error("{}{}", errorMessage, e);
                        transportErrors.add(errorMessage + e.getMessage());
                    }

                    if (shouldDeleteOldEntries) {
                        try {
                            sendDeleteRequestBinaryParameters(oldPid, id);
                        } catch (Exception e) {
                            //
                        }
                    }
                } catch (Exception e) {
                    String errorMessage = "Error reading JSON for binary parameter at index " + i + ": ";
                    LOGGER.error("{}{}", errorMessage, e);
                    transportErrors.add(errorMessage + e.getMessage());
                }
            }
        } catch (JSONException e) {
            LOGGER.warn(LABEL_TRANSPORT_NOT_FOUND_BINARY);
        } catch (Exception e) {
            String errorMessage = "Error transporting binary parameters: ";
            LOGGER.error("{}{}", errorMessage, e);
            transportErrors.add(errorMessage + e.getMessage());
        }
    }

    public JSONObject getStringParametersToTransport(List<String> pidsToTransport) {
        String filter = buildPidFilterStringParameters(pidsToTransport);

        try {
            String endpoint = API_STRING_PARTNERS + filter;
            return sendGetRequestsAndHandlePagination(endpoint);
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return null;
    }

    public void transportStringParameters(JSONObject jsonObjectToTransport, boolean overwrite, List<String> transportErrors, boolean shouldDeleteOldEntries, HashMap<String, String> oldAndNewPids) {
        try {
            JSONObject dObject = jsonObjectToTransport.getJSONObject(JSON_KEY_D);
            JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);
            LOGGER.info(LABEL_TRANSPORT_FOUND_X + "{}" + LABEL_TRANSPORT_FOUND_STRING, resultsArray.length());

            for (int i = 0; i < resultsArray.length(); i++) {
                try {
                    JSONObject resultObject = resultsArray.getJSONObject(i);

                    String pid;
                    String oldPid = "";
                    if (oldAndNewPids != null) {
                        oldPid = resultObject.getString(JSON_KEY_PID);
                        pid = oldAndNewPids.get(oldPid);
                    } else {
                        pid = resultObject.getString(JSON_KEY_PID);
                    }
                    String id = resultObject.getString(JSON_KEY_ID);
                    String value = resultObject.getString(JSON_KEY_VALUE);

                    try {
                        sendPostRequestStringParameters(pid, id, value, false);

                        if (this.latestStatusCode == 400 && overwrite) {
                            sendPutRequestStringParameters(pid, id, value, false);
                        }

                        logLatestResponse();

                        if (!(this.latestStatusCode >= 200 && this.latestStatusCode <= 299)) {
                            transportErrors.add(this.latestResponseLabel);
                        }
                    } catch (Exception e) {
                        String errorMessage = "Error sending HTTP request for string parameter (id: " + id + ", pid: " + pid + "): ";
                        LOGGER.error("{}{}", errorMessage, e);
                        transportErrors.add(errorMessage + e.getMessage());
                    }

                    if (shouldDeleteOldEntries) {
                        try {
                            sendDeleteRequestStringParameters(oldPid, id);
                        } catch (Exception e) {
                            //
                        }
                    }
                } catch (Exception e) {
                    String errorMessage = "Error reading JSON for string parameter at index " + i + ": ";
                    LOGGER.error("{}{}", errorMessage, e);
                    transportErrors.add(errorMessage + e.getMessage());
                }
            }

        } catch (JSONException e) {
            LOGGER.warn(LABEL_TRANSPORT_NOT_FOUND_STRING);
        } catch (Exception e) {
            LOGGER.error(e);
            transportErrors.add(e.getMessage());
        }
    }

    // String methods

    private String base64Encoding(String forEncoding) {
        return Base64.getEncoder().encodeToString(forEncoding.getBytes());
    }

    private String base64Decoding(String forDecoding) {
        return new String(Base64.getDecoder().decode(forDecoding));
    }

    private String convertStringToHexstring(String str) {
        char[] chars = Hex.encodeHex(str.getBytes(StandardCharsets.UTF_8));
        return String.valueOf(chars);
    }

    private String buildPidFilterBinaryParameters(List<String> pids) {
        if (pids == null || pids.isEmpty()) {
            return "";
        }

        StringBuilder filterBuilder = new StringBuilder();
        filterBuilder.append("?$filter=startswith(").append(JSON_KEY_CONTENT_TYPE).append(",'").append(JSON_VALUE_XSL).append("')%20and%20(");
        String prefix = "";

        for (String pid : pids) {
            filterBuilder.append(prefix);
            filterBuilder.append(JSON_KEY_PID + "%20eq%20'").append(pid).append("'");
            prefix = "%20or%20";
        }
        filterBuilder.append(")");

        return filterBuilder.toString();
    }

    private String buildPidFilterStringParameters(List<String> pids) {
        if (pids == null || pids.isEmpty()) {
            return "";
        }

        StringBuilder filterBuilder = new StringBuilder();
        filterBuilder.append("?$filter=");
        String prefix = "";

        for (String pid : pids) {
            filterBuilder.append(prefix);
            filterBuilder.append(JSON_KEY_PID + "%20eq%20'").append(pid).append("'");
            prefix = "%20or%20";
        }

        return filterBuilder.toString();
    }

    // send requests unified

    private JSONObject sendGetRequestsAndHandlePagination(String endpoint) throws Exception {
        JSONObject combinedJson = new JSONObject();
        JSONArray combinedResultsArray = new JSONArray();
        String nextEndpoint = endpoint;
        String uri;

        requestTokenIfExpired();

        do {
            uri = this.url + nextEndpoint;

            HttpRequest httpRequest = requestBuilder
                    .uri(URI.create(uri))
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            setResponseAndLog(httpResponse, httpRequest.method());

            JSONObject responseJson = new JSONObject(httpResponse.body());
            JSONArray currentResults = responseJson.getJSONObject(JSON_KEY_D).getJSONArray(JSON_KEY_RESULTS);

            for (int i = 0; i < currentResults.length(); i++) {
                combinedResultsArray.put(currentResults.get(i));
            }

            nextEndpoint = responseJson.getJSONObject(JSON_KEY_D).optString(JSON_KEY___NEXT, null);

        } while (nextEndpoint != null && !nextEndpoint.isEmpty());

        combinedJson.put(JSON_KEY_D, new JSONObject().put(JSON_KEY_RESULTS, combinedResultsArray));

        return combinedJson;
    }

    private String sendPostRequest(String endpoint, String jsonBody, boolean isResponseLogged) throws Exception {
        requestTokenIfExpired();
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(this.url + endpoint))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        String returnString;
        if (isResponseLogged) {
            returnString = setResponseAndLog(httpResponse, httpRequest.method());
            if (this.latestStatusCode != 201) {
                String errorMessage;
                try {
                    errorMessage = (new JSONObject(this.latestResponse.body()).getJSONObject("error").getJSONObject("message").getString("value"));
                } catch (Exception e) {
                    errorMessage = this.latestResponse.body();
                }
                throw new Exception(errorMessage);
            }
        } else {
            returnString = setResponseAttributes(httpResponse, httpRequest.method());
        }
        return returnString;
    }

    private void sendPutRequest(String endpoint, String jsonBody, boolean isResponseLogged) throws Exception {
        requestTokenIfExpired();
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(this.url + endpoint))
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (isResponseLogged) {
            setResponseAndLog(httpResponse, httpRequest.method());
        } else {
            setResponseAttributes(httpResponse, httpRequest.method());
        }
    }

    private String sendDeleteRequest(String endpoint) throws Exception {
        requestTokenIfExpired();
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(this.url + endpoint))
                .DELETE()
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return setResponseAndLog(httpResponse, httpRequest.method());
    }

    // helper methods for logging and working with HTTP responses

    private String setResponseAttributes(HttpResponse<String> response, String requestMethod) {
        this.latestStatusCode = response.statusCode();
        this.latestResponseType = getResponseType(latestStatusCode);
        this.latestResponse = response;
        this.latestResponseLabel = latestResponseType + ": " + requestMethod + " Status Code: " + latestStatusCode;
        return this.latestResponseLabel;
    }

    private String getResponseType(int latestStatusCode) {
        if (latestStatusCode >= 200 && latestStatusCode <= 299) {
            return LABEL_HTTP_SUCCESS;
        } else if (latestStatusCode >= 400 && latestStatusCode <= 599) {
            return LABEL_HTTP_ERROR;
        } else {
            return LABEL_HTTP_WARNING;
        }
    }

    private void logLatestResponse() {
        if (this.latestResponseType.equals(LABEL_HTTP_SUCCESS)) {
            LOGGER.info(this.latestResponse);
        } else if (this.latestResponseType.equals(LABEL_HTTP_ERROR)) {
            LOGGER.error(this.latestResponse);
            LOGGER.error(this.latestResponse.body());
        } else {
            LOGGER.warn(this.latestResponse);
        }
    }

    private String setResponseAndLog(HttpResponse<String> response, String requestMethod) {
        setResponseAttributes(response, requestMethod);
        logLatestResponse();
        return this.latestResponseLabel;
    }
}
