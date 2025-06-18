package org.example.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.example.utils.SharedData.*;

public class TenantCredentials {
    private final String name;
    private final boolean critical;
    private final String url;
    private final String tokenurl;
    private final String clientid;
    private final String clientsecret;
    private String accessToken;
    private String tokenExpirationDateTime;

    public TenantCredentials(String name, boolean critical, String url, String tokenurl, String clientid, String clientsecret, String accessToken, String tokenExpirationDateTime) {
        this.name = name;
        this.critical = critical;
        this.url = url;
        this.tokenurl = tokenurl;
        this.clientid = clientid;
        this.clientsecret = clientsecret;
        this.accessToken = accessToken;
        this.tokenExpirationDateTime = tokenExpirationDateTime;
    }

    public TenantCredentials(String name, boolean critical, String url, String tokenurl, String clientid, String clientsecret) {
        this.name = name;
        this.critical = critical;
        this.url = url;
        this.tokenurl = tokenurl;
        this.clientid = clientid;
        this.clientsecret = clientsecret;
    }

    public String getName() {
        return name;
    }

    public boolean isCritical() {
        return critical;
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

        tenantCredentialsList.remove(oldTenant);

        int index = tenantCredentialsList.indexOf(oldTenant);
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
                    TenantCredentials newTenant = new TenantCredentials(url, false, url, tokenurl, clientid, clientsecret, null, null);
                    tenantCredentialsList.add(newTenant);
                    return newTenant;
                });
    }
}
