package org.example.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.example.utils.SharedData.*;

public class TenantCredentials {
    private final String name;
    private final String headerColorHex;
    private final String url;
    private final String tokenurl;
    private final String clientid;
    private final String clientsecret;
    private String accessToken;
    private String tokenExpirationDateTime;

    public TenantCredentials(String name, String url, String tokenurl, String clientid, String clientsecret, String accessToken, String tokenExpirationDateTime, String headerColorHex) {
        this.name = name;
        this.headerColorHex = headerColorHex;
        this.url = url;
        this.tokenurl = tokenurl;
        this.clientid = clientid;
        this.clientsecret = clientsecret;
        this.accessToken = accessToken;
        this.tokenExpirationDateTime = tokenExpirationDateTime;
    }

    public TenantCredentials(String name, String url, String tokenurl, String clientid, String clientsecret, String accessToken, String tokenExpirationDateTime) {
        this(name, url, tokenurl, clientid, clientsecret, accessToken, tokenExpirationDateTime, DEFAULT_TENANT_HEADER_COLOR_HEX);
    }

    public TenantCredentials(String name, String url, String tokenurl, String clientid, String clientsecret, String headerColorHex) {
        this(name, url, tokenurl, clientid, clientsecret, null, null, headerColorHex);
    }

    public String getName() {
        return name;
    }

    public String getHeaderColorHex() {
        return headerColorHex;
    }

    public String getUrl() {
        return url;
    }

    public String getTokenurl() {
        return tokenurl;
    }

    public String getClientid() {
        return clientid;
    }

    public String getClientsecret() {
        return clientsecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenExpirationDateTime() {
        return tokenExpirationDateTime;
    }

    public void setTokenExpirationDateTime(String tokenExpirationDateTime) {
        this.tokenExpirationDateTime = tokenExpirationDateTime;
    }

    public LocalDateTime convertToLocalDateTime(String dateTimeStr) {
        String dateTimeStrWithoutUTC = dateTimeStr.replace(" UTC", "");
        return LocalDateTime.parse(dateTimeStrWithoutUTC, DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER_PATTERN));
    }

    public boolean isTokenValid() {
        if (accessToken == null || tokenExpirationDateTime == null) {
            return false;
        }

        return LocalDateTime.now(ZoneId.of("UTC")).isBefore(convertToLocalDateTime(tokenExpirationDateTime).minusMinutes(5));
    }

    public static void setTenantCredentialsList(List<TenantCredentials> newTenantCredentialsList) {
        tenantCredentialsList = newTenantCredentialsList;
    }

    public static void addTenantCredentials(TenantCredentials tenant) {
        tenantCredentialsList.add(tenant);
    }

    public static void updateExistingTenantCredentials(TenantCredentials oldTenant, TenantCredentials newTenant) {
        if (oldTenant.isTokenValid() && !newTenant.isTokenValid()) {
            newTenant.setAccessToken(oldTenant.getAccessToken());
            newTenant.setTokenExpirationDateTime(oldTenant.getTokenExpirationDateTime());
        }

        int index = tenantCredentialsList.indexOf(oldTenant);
        tenantCredentialsList.remove(oldTenant);

        if (index >= 0) {
            tenantCredentialsList.add(index, newTenant);
        } else {
            tenantCredentialsList.add(newTenant);
        }
    }

    public static void deleteTenantCredentials(TenantCredentials tenant) {
        tenantCredentialsList.remove(tenant);
    }


    public static TenantCredentials getTenantObjectByCredentials(String url, String tokenurl, String clientid, String clientsecret) {
        return tenantCredentialsList.stream()
                .filter(tenant -> tenant.getUrl().equals(url) &&
                        tenant.getTokenurl().equals(tokenurl) &&
                        tenant.getClientid().equals(clientid) &&
                        tenant.getClientsecret().equals(clientsecret))
                .findFirst()
                .orElseGet(() -> {
                    TenantCredentials newTenant = new TenantCredentials(url, url, tokenurl, clientid, clientsecret, null, null, DEFAULT_TENANT_HEADER_COLOR_HEX);
                    tenantCredentialsList.add(newTenant);
                    return newTenant;
                });
    }
}
