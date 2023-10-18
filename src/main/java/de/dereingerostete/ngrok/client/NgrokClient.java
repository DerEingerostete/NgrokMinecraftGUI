/*
 * Copyright (c) 2023 - DerEingerostete
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package de.dereingerostete.ngrok.client;

import de.dereingerostete.ngrok.Bootstrap;
import de.dereingerostete.ngrok.util.config.Configuration;
import de.dereingerostete.ngrok.util.config.ReadOnlyConfig;
import de.dereingerostete.ngrok.util.GUIUtils;
import de.dereingerostete.ngrok.util.UpdateInfo;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static de.dereingerostete.ngrok.Bootstrap.LOGGER;

public class NgrokClient {
    private final @NotNull Configuration configuration;
    private final @NotNull ReadOnlyConfig readOnlyConfig;
    private final @NotNull File executableFile;
    private final @NotNull File ngrokConfigFile;

    public NgrokClient() throws IOException {
        this.configuration = Bootstrap.getConfiguration();
        this.readOnlyConfig = Bootstrap.getReadOnlyConfig();
        this.executableFile = new File(Bootstrap.DATA_FOLDER, readOnlyConfig.getExecutable());
        this.ngrokConfigFile = loadNgrokConfig();
    }

    @Nullable
    public NgrokProcessThread createTunnel(int port, @NotNull Runnable onExit) throws IOException {
        String authToken = configuration.getAuthToken();
        if (authToken == null || authToken.equals("YOUR_TOKEN_HERE")) return null;

        List<String> arguments = createCommandArguments(port);
        Process process = new ProcessBuilder(arguments)
                .directory(Bootstrap.DATA_FOLDER)
                .start();

        NgrokProcessThread thread = new NgrokProcessThread(process, onExit);
        thread.start();
        return thread;
    }

    @NotNull
    protected List<String> createCommandArguments(int port) throws IOException {
        String filePath = executableFile.getCanonicalPath();
        List<String> commands = new ArrayList<>();
        commands.add(filePath);
        commands.add("tcp");

        String region = readOnlyConfig.getRegion();
        if (region != null) {
            commands.add("--region");
            commands.add(region);
        }

        String authToken = configuration.getAuthToken();
        if (authToken != null) {
            commands.add("--authtoken");
            commands.add(authToken);
        } else {
            throw new IOException("Authentication token is missing");
        }

        String configPath = ngrokConfigFile.getCanonicalPath();
        commands.add("--config");
        commands.add(configPath);

        Map<String, String> parameters = readOnlyConfig.getParameters();
        parameters.forEach((key, value) -> {
            commands.add("--" + key);
            commands.add(value);
        });

        commands.add(String.valueOf(port));
        return commands;
    }

    public boolean isDownloaded() {
        return executableFile.exists();
    }

    public void download() throws IOException {
        String downloadPath = readOnlyConfig.getDownloadPath();
        URL url = new URL(downloadPath);
        File tempFile = File.createTempFile("ngrok-mc-gui", ".tmp");
        FileUtils.copyURLToFile(url, tempFile, 10_000, 300_000);

        ZipFile zipFile = new ZipFile(tempFile);
        ZipEntry entry = zipFile.entries().nextElement();
        InputStream inputStream = zipFile.getInputStream(entry);
        FileUtils.copyInputStreamToFile(inputStream, executableFile);

        // Finish download
        inputStream.close();
        zipFile.close();
        FileUtils.deleteQuietly(tempFile);


    }

    @NotNull
    public UpdateInfo update() throws IOException {
        String path = executableFile.getAbsolutePath();
        ProcessBuilder builder = new ProcessBuilder(path, "update");
        Process process = builder.directory(executableFile.getParentFile()).start();

        CompletableFuture<Boolean> updateFuture = new CompletableFuture<>();
        CompletableFuture<Process> completeFuture = process.onExit();

        Thread thread = new Thread(() -> {
            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    line = line.toLowerCase().trim();
                    if (line.contains("no update")) {
                        updateFuture.complete(false);
                        break;
                    } else if (line.contains("update available")) {
                        updateFuture.complete(true);
                        break;
                    }
                }

                bufferedReader.close();
            } catch (IOException exception) {
                LOGGER.warn("Failed to read update process", exception);
            }

        }, "UpdateOutputThread");

        thread.start();
        return new UpdateInfo(completeFuture, updateFuture);
    }

    @NotNull
    private File loadNgrokConfig() throws IOException {
        File ngrokConfig = new File(Bootstrap.DATA_FOLDER, "ngrok-config.yml");
        if (!ngrokConfig.exists()) {
            URL url = GUIUtils.getResourceAsURL(ngrokConfig.getName());
            FileUtils.copyURLToFile(url, ngrokConfig);
        }
        return ngrokConfig;
    }

}
