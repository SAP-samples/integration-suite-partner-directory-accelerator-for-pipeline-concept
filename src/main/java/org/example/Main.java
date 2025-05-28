package org.example;

import org.example.ui.MainFrame;
import org.example.utils.JsonFileHandler;
import org.example.utils.TenantCredentials;

import javax.swing.SwingUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.LogManager;

import static org.example.utils.SharedData.*;

public class Main {

    public static void main(String[] args) {
        LOGGER.info("App started");

        final String locationToCredentials;
        if (args.length > 0) {
            locationToCredentials = args[0];
        } else {
            locationToCredentials = "";
        }

        try (InputStream is = Main.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        jsonFileHandler = new JsonFileHandler();
        List<TenantCredentials> tenants = jsonFileHandler.readJsonFile();
        LOGGER.info("Existing Tenant Credentials:");
        for (TenantCredentials tenant : tenants) {
            LOGGER.info("{} ({})", tenant.getName(), tenant.getUrl());
        }

        SwingUtilities.invokeLater(() -> {
            try {
                new MainFrame(locationToCredentials).setVisible(true);
            } catch (Exception e) {
                LOGGER.error(e);
            }
        });
    }
}