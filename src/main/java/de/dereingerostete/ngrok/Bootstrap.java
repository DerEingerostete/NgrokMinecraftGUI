/*
 * Copyright (c) 2023 - DerEingerostete
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package de.dereingerostete.ngrok;

import de.dereingerostete.ngrok.client.NgrokClient;
import de.dereingerostete.ngrok.gui.NgrokGUI;
import de.dereingerostete.ngrok.gui.panel.LoadingPanel;
import de.dereingerostete.ngrok.gui.panel.MainPanel;
import de.dereingerostete.ngrok.util.LAFManager;
import de.dereingerostete.ngrok.util.UpdateInfo;
import de.dereingerostete.ngrok.util.config.ConfigUtils;
import de.dereingerostete.ngrok.util.config.Configuration;
import de.dereingerostete.ngrok.util.config.ReadOnlyConfig;
import de.dereingerostete.ngrok.util.config.Theming;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Bootstrap {
    public static final @NotNull File DATA_FOLDER = loadDateFolder();
    public static final @NotNull Logger LOGGER = LoggerFactory.getLogger("NgrokGUI");
    private static @Getter LAFManager lafManager;
    private static @Getter NgrokGUI gui;
    private static @Getter NgrokClient client;

    // Config
    private static @Getter ReadOnlyConfig readOnlyConfig;
    private static @Getter Configuration configuration;
    private static @Getter File configFile;

    public static void main(String[] args) {
        LOGGER.info("Starting Ngrok Minecraft GUI");

        try {
            LOGGER.info("Loading config file");
            configFile = new File(DATA_FOLDER, "config.yml");
            configuration = Configuration.getConfiguration(configFile);

            File additionalConfigFile = new File(DATA_FOLDER, "additional-config.yml");
            readOnlyConfig = ReadOnlyConfig.getConfiguration(additionalConfigFile);
        } catch (IOException exception) {
            LOGGER.error("Failed to load config file", exception);
            JOptionPane.showMessageDialog(null,
                    "Failed to load config. Please try again!",
                    "Unexpected error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        try {
            File minecraftDir = ConfigUtils.detectMinecraftInstance();
            if (minecraftDir != null) {
                configuration.setMinecraftFolder(minecraftDir.getCanonicalPath());
                configuration.save(configFile);
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to set and save minecraft folder", exception);
        }

        try {
            LOGGER.info("Loading theme");
            Theming theming = readOnlyConfig.getTheming();
            lafManager = new LAFManager(theming);
            lafManager.load();
        } catch (IOException exception) {
            LOGGER.error("Failed to update look and feel", exception);
        }

        Thread ngrokCheckThread = new Thread(() -> {
            try {
                LoadingPanel loadingPanel = (LoadingPanel) gui.getActivePanel();
                loadingPanel.setInfo("Checking Ngrok CLI...");
                LOGGER.info("Checking ngrok cli");

                client = new NgrokClient();
                if (client.isDownloaded()) {
                    LOGGER.info("Checking for ngrok updates");
                    loadingPanel.setInfo("Checking for ngrok updates...");

                    UpdateInfo info = client.update();
                    boolean updating = info.getUpdatingFuture().get(5, TimeUnit.MINUTES);
                    if (updating) loadingPanel.setInfo("Updating ngrok...");

                    info.getCompletionFuture().get(5, TimeUnit.MINUTES);
                } else {
                    LOGGER.info("Downloading ngrok");
                    loadingPanel.setInfo("Downloading ngrok...");
                    client.download();
                }

                // Switch to main GUI
                LOGGER.info("Changing to main interface");
                gui.changePanel(new MainPanel());
            } catch (IOException | InterruptedException | ExecutionException | TimeoutException exception) {
                LOGGER.warn("Failed to check ngrok", exception);
            }
        }, "NgrokCheckThread");

        EventQueue.invokeLater(() -> {
            try {
                gui = new NgrokGUI();
                gui.setVisible(true);
                ngrokCheckThread.start();
            } catch (Exception exception) {
                LOGGER.warn("GUI threw exception", exception);
            }
        });
    }

    public static void handleExit() {
        LOGGER.info("Starting shutdown handlers");

        try {
            LOGGER.info("Closing the look and feel manager");
            Bootstrap.getLafManager().close();
        } catch (IOException exception) {
            LOGGER.warn("Failed to close the look and feel manager");
        }

        LOGGER.info("Done. Bye!");
    }

    @NotNull
    private static File loadDateFolder() {
        String localAppData = System.getenv("localappdata");
        if (localAppData == null) {
            System.err.println("Environment variable not found");
        }

        File dir = localAppData == null ? new File("Ngrok_MC_GUI") : new File(localAppData, "Ngrok_MC_GUI");
        if (!dir.exists() && !dir.mkdir()) {
            throw new IllegalStateException("Failed to create data folder");
        }

        System.setProperty("dataFolder", dir.getAbsolutePath());
        return dir;
    }

}