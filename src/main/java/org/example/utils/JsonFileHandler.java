package org.example.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static org.example.utils.SharedData.*;
import static org.example.utils.TenantCredentials.*;

public class JsonFileHandler {

    public JsonFileHandler() {
        File file = new File(TENANTS_FILE_NAME);
        if (!file.exists()) {
            createDefaultJsonFile();
        } else {
            loadJsonFileToSharedData();
        }
    }

    private void createDefaultJsonFile() {
        try (FileWriter fileWriter = new FileWriter(TENANTS_FILE_NAME)) {
            JSONObject defaultJson = new JSONObject();
            defaultJson.put(JSON_KEY_IMPORTANT, JSON_VALUE_GENERATED);

            JSONArray tenantsArray = new JSONArray();
            defaultJson.put(JSON_KEY_TENANTS, tenantsArray);

            fileWriter.write(defaultJson.toString(4));
            fileWriter.flush();
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    public void loadJsonFileToSharedData() {
        List<TenantCredentials> tenantsList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(TENANTS_FILE_NAME))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            JSONObject jsonObject = new JSONObject(jsonContent.toString());
            JSONArray tenantsArray = jsonObject.getJSONArray(JSON_KEY_TENANTS);

            for (int i = 0; i < tenantsArray.length(); i++) {
                JSONObject tenantObject = tenantsArray.getJSONObject(i);
                TenantCredentials credentials = new TenantCredentials(tenantObject.getString(JSON_KEY_NAME), tenantObject.getBoolean(JSON_KEY_CRITICAL), tenantObject.getString(JSON_KEY_URL), tenantObject.getString(JSON_KEY_TOKEN_URL), tenantObject.getString(JSON_KEY_CLIENT_ID), tenantObject.getString(JSON_KEY_CLIENT_SECRET));
                if (tenantObject.has(JSON_KEY_ACCESS_TOKEN) && tenantObject.has(JSON_KEY_TOKEN_EXPIRATION_DATE_TIME)) {
                    credentials.setAccessToken(tenantObject.getString(JSON_KEY_ACCESS_TOKEN));
                    credentials.setTokenExpirationDateTime(tenantObject.getString(JSON_KEY_TOKEN_EXPIRATION_DATE_TIME));
                }
                tenantsList.add(credentials);
            }

            setTenantCredentialsList(tenantsList);

        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    public void saveJsonFile() {
        try (FileWriter fileWriter = new FileWriter(TENANTS_FILE_NAME)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_IMPORTANT, JSON_VALUE_GENERATED);

            JSONArray tenantsArray = new JSONArray();

            for (TenantCredentials tenant : tenantCredentialsList) {
                JSONObject tenantObject = new JSONObject();
                tenantObject.put(JSON_KEY_NAME, tenant.getName());
                tenantObject.put(JSON_KEY_CRITICAL, tenant.isCritical());
                tenantObject.put(JSON_KEY_CLIENT_ID, tenant.getClientid());
                tenantObject.put(JSON_KEY_CLIENT_SECRET, tenant.getClientsecret());
                tenantObject.put(JSON_KEY_TOKEN_URL, tenant.getTokenurl());
                tenantObject.put(JSON_KEY_URL, tenant.getUrl());
                tenantObject.put(JSON_KEY_ACCESS_TOKEN, tenant.getAccessToken());
                tenantObject.put(JSON_KEY_TOKEN_EXPIRATION_DATE_TIME, tenant.getTokenExpirationDateTime());

                tenantsArray.put(tenantObject);
            }

            jsonObject.put(JSON_KEY_TENANTS, tenantsArray);
            fileWriter.write(jsonObject.toString(4));
            fileWriter.flush();
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    public void addTenant(TenantCredentials newTenant) {
        TenantCredentials existingTenant = getExistingTenantByUrl(newTenant.getUrl());
        if (existingTenant == null) {
            addTenantCredentials(newTenant);
        } else {
            updateExistingTenantCredentials(existingTenant, newTenant);
        }
        saveJsonFile();
    }

    public void replaceTenant(TenantCredentials oldTenant, TenantCredentials newTenant) {
        updateExistingTenantCredentials(oldTenant, newTenant);
        saveJsonFile();
    }

    public TenantCredentials getExistingTenantByUrl(String url) {
        for (TenantCredentials tenant : tenantCredentialsList) {
            if (tenant.getUrl().equalsIgnoreCase(url)) {
                return tenant;
            }
        }
        return null;
    }

    public boolean isNameUniqueAdd(String name) {
        for (TenantCredentials tenantCredentials : tenantCredentialsList) {
            if (tenantCredentials.getName().equals(name)) {
                return false;
            }
        }
        return true;
    }

    public boolean isNameUniqueReplace(String name, String nameToExclude) {
        for (TenantCredentials tenantCredentials : tenantCredentialsList) {
            if (tenantCredentials.getName().equals(name)) {
                if (!name.equals(nameToExclude)) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<TenantCredentials> readJsonFile() {
        List<TenantCredentials> tenantsList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(TENANTS_FILE_NAME))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            JSONObject jsonObject = new JSONObject(jsonContent.toString());
            JSONArray tenantsArray = jsonObject.getJSONArray(JSON_KEY_TENANTS);

            for (int i = 0; i < tenantsArray.length(); i++) {
                JSONObject tenantObject = tenantsArray.getJSONObject(i);
                TenantCredentials tenant = new TenantCredentials(tenantObject.getString(JSON_KEY_NAME), tenantObject.getBoolean(JSON_KEY_CRITICAL), tenantObject.getString(JSON_KEY_URL), tenantObject.getString(JSON_KEY_TOKEN_URL), tenantObject.getString(JSON_KEY_CLIENT_ID), tenantObject.getString(JSON_KEY_CLIENT_SECRET));
                if (tenantObject.has(JSON_KEY_ACCESS_TOKEN) && tenantObject.has(JSON_KEY_TOKEN_EXPIRATION_DATE_TIME)) {
                    tenant.setAccessToken(tenantObject.getString(JSON_KEY_ACCESS_TOKEN));
                    tenant.setTokenExpirationDateTime(tenantObject.getString(JSON_KEY_TOKEN_EXPIRATION_DATE_TIME));
                }
                tenantsList.add(tenant);
            }

        } catch (IOException e) {
            LOGGER.error(e);
        }

        return tenantsList;
    }

    public void updateTenantToken(String url, String accessToken, String expirationDateTime) {
        TenantCredentials oldTenant = getExistingTenantByUrl(url);

        if (oldTenant != null) {
            TenantCredentials newTenant = new TenantCredentials(oldTenant.getName(), oldTenant.isCritical(), oldTenant.getUrl(), oldTenant.getTokenurl(), oldTenant.getClientid(), oldTenant.getClientsecret(), accessToken, expirationDateTime);
            replaceTenant(oldTenant, newTenant);
        }
    }
}