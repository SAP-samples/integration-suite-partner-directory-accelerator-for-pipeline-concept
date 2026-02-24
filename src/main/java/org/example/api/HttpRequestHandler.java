package org.example.api;

import org.apache.commons.codec.binary.Hex;
import org.example.model.AlternativePartner;
import org.example.utils.TenantCredentials;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
import java.util.List;
import java.util.Set;

import static org.example.utils.SharedData.*;

public class HttpRequestHandler {
    private final Builder requestBuilder;
    private final HttpClient client;
    private final int indentFactor = 4;
    private final String url;

    private final TenantCredentials tenantCredentials;

    public HttpRequestHandler(TenantCredentials tenantCredentials) throws IOException, InterruptedException {
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

    private String requestToken() throws IOException, InterruptedException {
        HttpRequest requestToken = HttpRequest.newBuilder()
                .uri(URI.create(tenantCredentials.getTokenurl()))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((tenantCredentials.getClientid() + ":" + tenantCredentials.getClientsecret()).getBytes()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();

        HttpResponse<String> responseToken = client.send(requestToken, HttpResponse.BodyHandlers.ofString());
        logHttpResponse(responseToken, requestToken.method());

        JSONObject tokenResponse = new JSONObject(responseToken.body());
        String token = tokenResponse.getString(JSON_KEY_ACCESS_TOKEN);
        long tokenExpiresInSeconds = tokenResponse.getInt(JSON_KEY_EXPIRES_IN);
        String tokenExpirationDateTime = calculateExpirationDateTime(tokenExpiresInSeconds);

        tenantCredentials.setAccessToken(token);
        tenantCredentials.setTokenExpirationDateTime(tokenExpirationDateTime);
        jsonFileHandler.saveJsonFile();

        return token;
    }

    private void requestTokenIfExpired() throws IOException, InterruptedException {
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

    private String createRequestBodyPostBinaryParameters(String pid, String id, String encodedString) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_KEY_PID, pid);
        jsonObject.put(JSON_KEY_ID, id);
        jsonObject.put(JSON_KEY_CONTENT_TYPE, JSON_VALUE_XSL);
        jsonObject.put(JSON_KEY_VALUE, encodedString);
        return jsonObject.toString(indentFactor);
    }

    private String createRequestBodyPutBinaryParameters(String encodedString) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_KEY_CONTENT_TYPE, JSON_VALUE_XSL);
        jsonObject.put(JSON_KEY_VALUE, encodedString);
        return jsonObject.toString(indentFactor);
    }

    private String createRequestBodyPostStringParameters(String pid, String id, String value) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_KEY_PID, pid);
        jsonObject.put(JSON_KEY_ID, id);
        jsonObject.put(JSON_KEY_VALUE, value);
        return jsonObject.toString(indentFactor);
    }

    private String createRequestBodyPutStringParameters(String value) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_KEY_VALUE, value);
        return jsonObject.toString(indentFactor);
    }

    // AlternativePartners

    public String sendGetRequestAlternativePartners() throws IOException, InterruptedException {
        requestTokenIfExpired();

        String pidsFromBinaryParameters = sendGetRequestParametersWithReceiverDetermination(API_BINARY_PARAMETERS);
        String pidsFromStringParameters = sendGetRequestParametersWithReceiverDetermination(API_STRING_PARTNERS);
        Set<String> uniquePids = jsonApiHandler.getUniquePidsFromEndpoints(pidsFromBinaryParameters, pidsFromStringParameters);

        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_ALTERNATIVE_PARTNERS + "?$orderby=" + JSON_KEY_AGENCY))
                .GET()
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        String returnString = logHttpResponse(httpResponse, httpRequest.method());
        jsonApiHandler.parseAlternativePartnersJson(httpResponse.body(), false, uniquePids);
        return returnString;
    }

    public void sendGetRequestAlternativePartnersLandscape(Set<String> listSystemNames) throws IOException, InterruptedException {
        requestTokenIfExpired();

        StringBuilder pidFilter = new StringBuilder();
        for (String receiverName : listSystemNames) {
            if (!pidFilter.isEmpty()) {
                pidFilter.append("%20or%20");
            }
            pidFilter.append("Pid%20eq%20'").append(receiverName).append("'");
        }

        String filter = "?$filter=(" + pidFilter + ")%20and%20(Scheme%20eq%20'" + SCHEME_BUSINESS_SYSTEM_NAME
                + "'%20or%20Scheme%20eq%20'" + SCHEME_LOGICAL_SYSTEM_NAME + "')";

        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_ALTERNATIVE_PARTNERS + filter))
                .GET()
                .build();

        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        logHttpResponse(httpResponse, httpRequest.method());
        jsonApiHandler.parseAlternativePartnersJsonLandscape(httpResponse.body());
    }

    public String sendPostRequestAlternativePartners(String agency, String scheme, String id, String pid) throws IOException, InterruptedException {
        requestTokenIfExpired();
        String jsonBody = createRequestBodyPostAlternativePartners(agency, scheme, id, pid);
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_ALTERNATIVE_PARTNERS))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return logHttpResponse(httpResponse, httpRequest.method());
    }

    public String sendDeleteRequestAlternativePartners(String agency, String scheme, String id) throws IOException, InterruptedException {
        requestTokenIfExpired();
        String hexAgency = convertStringToHexstring(agency);
        String hexScheme = convertStringToHexstring(scheme);
        String hexId = convertStringToHexstring(id);
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_ALTERNATIVE_PARTNERS + "(" + JSON_KEY_HEXAGENCY + "='" + hexAgency + "'," + JSON_KEY_HEXSCHEME + "='" + hexScheme + "'," + JSON_KEY_HEXID + "='" + hexId + "')"))
                .DELETE()
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return logHttpResponse(httpResponse, httpRequest.method());
    }

    // both BinaryParameters and StringParameters

    public String sendGetRequestParametersWithReceiverDetermination(String endpoint) throws IOException, InterruptedException {
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + endpoint + "?$filter=" + JSON_KEY_ID + "%20eq%20'" + ID_RECEIVER_DETERMINATION + "'&$select=" + JSON_KEY_PID))
                .GET()
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        logHttpResponse(httpResponse, httpRequest.method());
        return httpResponse.body();
    }

    // BinaryParameters

    public void sendGetRequestBinaryParameters(String pid) throws IOException, InterruptedException {
        requestTokenIfExpired();
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_BINARY_PARAMETERS + "?$filter=startswith(" + JSON_KEY_CONTENT_TYPE + ",'" + JSON_VALUE_XSL + "')%20and%20" + JSON_KEY_PID + "%20eq%20'" + pid + "'"))
                .GET()
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        logHttpResponse(httpResponse, httpRequest.method());
        jsonApiHandler.parseBinaryParametersJson(httpResponse.body());
    }

    public String sendPostRequestBinaryParameters(String pid, String id, String valueAsString) throws IOException, InterruptedException {
        requestTokenIfExpired();
        String valueEncoded = base64Encoding(valueAsString);
        String jsonBody = createRequestBodyPostBinaryParameters(pid, id, valueEncoded);

        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_BINARY_PARAMETERS))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return logHttpResponse(httpResponse, httpRequest.method());
    }

    public String sendPutRequestBinaryParameters(String pid, String id, String valueAsString) throws IOException, InterruptedException {
        requestTokenIfExpired();
        String valueEncoded = base64Encoding(valueAsString);
        String jsonBody = createRequestBodyPutBinaryParameters(valueEncoded);

        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_BINARY_PARAMETERS + "(" + JSON_KEY_PID + "='" + pid + "'," + JSON_KEY_ID + "='" + id + "')"))
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (httpResponse.statusCode() != 404) {
            return logHttpResponse(httpResponse, httpRequest.method());
        } else {
            return this.sendPostRequestBinaryParameters(pid, id, valueAsString);
        }
    }

    // String Parameters

    public void sendGetRequestStringParameters(String pid) throws IOException, InterruptedException {
        requestTokenIfExpired();
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_STRING_PARTNERS + "?$filter=" + JSON_KEY_PID + "%20eq%20'" + pid + "'"))
                .GET()
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        logHttpResponse(httpResponse, httpRequest.method());
        jsonApiHandler.parseStringParametersJson(httpResponse.body());
    }

    public void sendGetRequestStringParameters(String pid, Set<String> listReceiverNames) throws IOException, InterruptedException {
        requestTokenIfExpired();

        StringBuilder filterReceiverSpecificQueue = new StringBuilder();
        for (String receiverName : listReceiverNames) {
            if (!filterReceiverSpecificQueue.isEmpty()) {
                filterReceiverSpecificQueue.append("%20or%20");
            }
            filterReceiverSpecificQueue.append(JSON_KEY_PID + "%20eq%20'").append(receiverName).append("'");
        }

        String uri = url + API_STRING_PARTNERS + "?$filter="
                + JSON_KEY_PID + "%20eq%20'" + pid + "'";

        if (!listReceiverNames.isEmpty()) {
            uri += "%20or%20(" + JSON_KEY_ID + "%20eq%20'" + STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE
                    + "'%20and%20(" + filterReceiverSpecificQueue + "))";
        }

        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        logHttpResponse(httpResponse, httpRequest.method());
        jsonApiHandler.parseStringParametersJson(httpResponse.body());
    }

    public void sendGetRequestStringParameterLandscape() throws IOException, InterruptedException {
        requestTokenIfExpired();
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_STRING_PARTNERS + "?$filter=" + JSON_KEY_PID + "%20eq%20'" + STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE + "'"))
                .GET()
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        logHttpResponse(httpResponse, httpRequest.method());

        jsonApiHandler.parseStringParameterLandscapeJson(httpResponse.body());
    }

    public void sendDeleteRequestStringParametersExistingDetermination(String pid, String id) {
        try {
            requestTokenIfExpired();
            HttpRequest httpRequest = requestBuilder
                    .uri(URI.create(url + API_STRING_PARTNERS + "?$filter=" + JSON_KEY_PID + "%20eq%20'" + pid + "'%20and%20startswith(" + JSON_KEY_ID + ",'" + id + "')"))
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            logHttpResponse(httpResponse, httpRequest.method());

            if (!jsonApiHandler.isResultsEmpty(httpResponse.body())) {
                String uriToDelete = jsonApiHandler.getUriFromStringParametersJson(httpResponse.body());
                sendDeleteRequestStringParameters(uriToDelete);
            }
        } catch (Exception e) {
            LOGGER.warn(e);
        }
    }

    public String sendPostRequestStringParameters(String pid, String id, String value) throws IOException, InterruptedException {
        String jsonBody = createRequestBodyPostStringParameters(pid, id, value);
        requestTokenIfExpired();
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_STRING_PARTNERS))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return logHttpResponse(httpResponse, httpRequest.method());
    }

    public String sendPutRequestStringParameters(String pid, String id, String value) throws IOException, InterruptedException {
        String jsonBody = createRequestBodyPutStringParameters(value);
        requestTokenIfExpired();
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_STRING_PARTNERS + "(" + JSON_KEY_PID + "='" + pid + "'," + JSON_KEY_ID + "='" + id + "')"))
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        String outputPut = logHttpResponse(httpResponse, httpRequest.method());
        if (httpResponse.statusCode() != 404) {
            return outputPut;
        } else {
            return this.sendPostRequestStringParameters(pid, id, value);
        }
    }

    public String sendDeleteRequestStringParameters(String pid, String id) throws IOException, InterruptedException {
        requestTokenIfExpired();
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_STRING_PARTNERS + "(" + JSON_KEY_PID + "='" + pid + "'," + JSON_KEY_ID + "='" + id + "')"))
                .DELETE()
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return logHttpResponse(httpResponse, httpRequest.method());
    }

    public void sendDeleteRequestStringParameters(String uriToDelete) throws IOException, InterruptedException {
        requestTokenIfExpired();
        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(uriToDelete))
                .DELETE()
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        logHttpResponse(httpResponse, httpRequest.method());
    }

    // Transport methods

    public void sendGetRequestAlternativePartnersTransport() throws IOException, InterruptedException {
        requestTokenIfExpired();

        String pidsFromBinaryParameters = sendGetRequestParametersWithReceiverDetermination(API_BINARY_PARAMETERS);
        String pidsFromStringParameters = sendGetRequestParametersWithReceiverDetermination(API_STRING_PARTNERS);
        Set<String> uniquePids = jsonApiHandler.getUniquePidsFromEndpoints(pidsFromBinaryParameters, pidsFromStringParameters);

        sendGetRequestStringParameterLandscape();

        HttpRequest httpRequest = requestBuilder
                .uri(URI.create(url + API_ALTERNATIVE_PARTNERS + "?$orderby=" + JSON_KEY_AGENCY))
                .GET()
                .build();
        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        logHttpResponse(httpResponse, httpRequest.method());
        jsonApiHandler.parseAlternativePartnersJson(httpResponse.body(), true, uniquePids);
    }

    public void transportAlternativePartners(List<AlternativePartner> alternativePartnersToTransport, boolean overwrite, List<String> transportErrors) {
        for (AlternativePartner alternativePartner : alternativePartnersToTransport) {
            String agency = alternativePartner.getAgency();
            String scheme = alternativePartner.getScheme();
            String id = alternativePartner.getId();
            String pid = alternativePartner.getPid();

            try {
                String jsonBody = createRequestBodyPostAlternativePartners(agency, scheme, id, pid);
                requestTokenIfExpired();
                HttpRequest httpRequest = requestBuilder
                        .uri(URI.create(url + API_ALTERNATIVE_PARTNERS))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() == 400 && overwrite) {
                    jsonBody = createRequestBodyPutAlternativePartners(pid);
                    String hexAgency = convertStringToHexstring(agency);
                    String hexScheme = convertStringToHexstring(scheme);
                    String hexId = convertStringToHexstring(id);
                    requestTokenIfExpired();
                    httpRequest = requestBuilder
                            .uri(URI.create(url + API_ALTERNATIVE_PARTNERS + "(" + JSON_KEY_HEXAGENCY + "='" + hexAgency + "'," + JSON_KEY_HEXSCHEME + "='" + hexScheme + "'," + JSON_KEY_HEXID + "='" + hexId + "')"))
                            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();
                    httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                }

                String logging = logHttpResponse(httpResponse, httpRequest.method());
                int statusCode = httpResponse.statusCode();
                if (!(statusCode >= 200 && statusCode <= 299)) {
                    transportErrors.add(logging);
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
            requestTokenIfExpired();
            HttpRequest httpRequest = requestBuilder
                    .uri(URI.create(url + API_BINARY_PARAMETERS + filter))
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            logHttpResponse(httpResponse, httpRequest.method());
            return new JSONObject(httpResponse.body());
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return null;
    }

    public void transportBinaryParameters(JSONObject jsonObjectToTransport, boolean overwrite, List<String> transportErrors) {
        try {
            JSONObject dObject = jsonObjectToTransport.getJSONObject(JSON_KEY_D);
            JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);
            LOGGER.info(LABEL_TRANSPORT_FOUND_X + "{}" + LABEL_TRANSPORT_FOUND_BINARY, resultsArray.length());

            for (int i = 0; i < resultsArray.length(); i++) {
                try {
                    JSONObject resultObject = resultsArray.getJSONObject(i);
                    String pid = resultObject.getString(JSON_KEY_PID);
                    String id = resultObject.getString(JSON_KEY_ID);
                    String value = new String(Base64.getDecoder().decode(resultObject.getString(JSON_KEY_VALUE)));

                    String valueEncoded = base64Encoding(value);
                    String jsonBody = createRequestBodyPostBinaryParameters(pid, id, valueEncoded);

                    try {
                        requestTokenIfExpired();
                        HttpRequest httpRequest = requestBuilder
                                .uri(URI.create(url + API_BINARY_PARAMETERS))
                                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                                .build();
                        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                        if (httpResponse.statusCode() == 400 && overwrite) {
                            jsonBody = createRequestBodyPutBinaryParameters(valueEncoded);

                            requestTokenIfExpired();
                            httpRequest = requestBuilder
                                    .uri(URI.create(url + API_BINARY_PARAMETERS + "(" + JSON_KEY_PID + "='" + pid + "'," + JSON_KEY_ID + "='" + id + "')"))
                                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                                    .build();
                            httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                        }

                        String logging = logHttpResponse(httpResponse, httpRequest.method());
                        int statusCode = httpResponse.statusCode();
                        if (!(statusCode >= 200 && statusCode <= 299)) {
                            transportErrors.add(logging);
                        }
                    } catch (Exception e) {
                        String errorMessage = "Error sending HTTP request for binary parameter (id: " + id + ", pid: " + pid + "): ";
                        LOGGER.error("{}{}", errorMessage, e);
                        transportErrors.add(errorMessage + e.getMessage());
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
            requestTokenIfExpired();
            HttpRequest httpRequest = requestBuilder
                    .uri(URI.create(url + API_STRING_PARTNERS + filter))
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            logHttpResponse(httpResponse, httpRequest.method());
            return new JSONObject(httpResponse.body());
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return null;
    }

    public void transportStringParameters(JSONObject jsonObjectToTransport, boolean overwrite, List<String> transportErrors) {
        try {
            JSONObject dObject = jsonObjectToTransport.getJSONObject(JSON_KEY_D);
            JSONArray resultsArray = dObject.getJSONArray(JSON_KEY_RESULTS);
            LOGGER.info(LABEL_TRANSPORT_FOUND_X + "{}" + LABEL_TRANSPORT_FOUND_STRING, resultsArray.length());

            for (int i = 0; i < resultsArray.length(); i++) {
                try {
                    JSONObject resultObject = resultsArray.getJSONObject(i);
                    String pid = resultObject.getString(JSON_KEY_PID);
                    String id = resultObject.getString(JSON_KEY_ID);
                    String value = resultObject.getString(JSON_KEY_VALUE);

                    String jsonBody = createRequestBodyPostStringParameters(pid, id, value);

                    try {
                        requestTokenIfExpired();
                        HttpRequest httpRequest = requestBuilder
                                .uri(URI.create(url + API_STRING_PARTNERS))
                                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                                .build();
                        HttpResponse<String> httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                        if (httpResponse.statusCode() == 400 && overwrite) {
                            jsonBody = createRequestBodyPutStringParameters(value);

                            requestTokenIfExpired();
                            httpRequest = requestBuilder
                                    .uri(URI.create(url + API_STRING_PARTNERS + "(" + JSON_KEY_PID + "='" + pid + "'," + JSON_KEY_ID + "='" + id + "')"))
                                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                                    .build();
                            httpResponse = this.client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                        }

                        String logging = logHttpResponse(httpResponse, httpRequest.method());
                        int statusCode = httpResponse.statusCode();
                        if (!(statusCode >= 200 && statusCode <= 299)) {
                            transportErrors.add(logging);
                        }

                    } catch (Exception e) {
                        String errorMessage = "Error sending HTTP request for string parameter (id: " + id + ", pid: " + pid + "): ";
                        LOGGER.error("{}{}", errorMessage, e);
                        transportErrors.add(errorMessage + e.getMessage());
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

    private String logHttpResponse(HttpResponse<String> response, String requestMethod) {
        int statusCode = response.statusCode();
        String responseType;
        if (statusCode >= 200 && statusCode <= 299) {
            responseType = LABEL_HTTP_SUCCESS;
            LOGGER.info(response);
        } else if (statusCode >= 400 && statusCode <= 599) {
            responseType = LABEL_HTTP_ERROR;
            LOGGER.error(response);
            LOGGER.error(response.body());
        } else {
            responseType = LABEL_HTTP_WARNING;
            LOGGER.warn(response);
        }
        return responseType + ": " + requestMethod + " Status Code: " + response.statusCode();
    }
}
