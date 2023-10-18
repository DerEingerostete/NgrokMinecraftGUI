/*
 * Copyright (c) 2023 - DerEingerostete
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package de.dereingerostete.ngrok.client;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.dereingerostete.ngrok.Bootstrap.LOGGER;

public class NgrokProcessThread extends Thread {
    private final @NotNull Process process;
    private final @NotNull BufferedReader inputReader;
    private final @NotNull BufferedReader errorReader;
    private final @NotNull Runnable onExitRunnable;
    private boolean exiting;
    private boolean stopped;

    public NgrokProcessThread(@NotNull Process process, @NotNull Runnable onExit) {
        this.process = process;
        this.onExitRunnable = onExit;
        setName("NgrokProcessThread-" + getId());
        setPriority(8);
        setDaemon(true);

        BufferedInputStream inputStream = new BufferedInputStream(process.getInputStream());
        BufferedInputStream errorStream = new BufferedInputStream(process.getErrorStream());
        this.inputReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        this.errorReader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));
        this.exiting = false;
        this.stopped = false;
    }

    @Override
    public void run() {
        while (!exiting) {
            try {
                if (errorReader.ready()) {
                    String errorMessage = errorReader.readLine();
                    LOGGER.info("Ngrok Error: " + errorMessage);
                }

                if (inputReader.ready()) {
                    String message = inputReader.readLine();
                    LOGGER.info("Ngrok process send message: " + message);
                    NgrokHandler.handle(message, this);
                }
            } catch (IOException exception) {
                LOGGER.warn("Failed to read from Ngrok process", exception);
            }
        }

        handleExit();
        onExitRunnable.run();
    }

    protected void handleExit() {
        try {
            LOGGER.info("Handling Ngrok process exit");
            Process finishedProcess = process.onExit().get(1, TimeUnit.MINUTES);

            int exitValue = finishedProcess.exitValue();
            LOGGER.info("Ngrok process exited with code " + exitValue);
        } catch (InterruptedException | TimeoutException exception) {
            LOGGER.warn("Ngrok process exit timed out", exception);
            LOGGER.warn("Exiting Ngrok forcibly (This should normally not happen!)");
            process.destroyForcibly();
        } catch (ExecutionException exception) {
            LOGGER.warn("Ngrok process exit failed with error", exception);
        }

        try {
            inputReader.close();
            errorReader.close();
        } catch (IOException exception) {
            LOGGER.warn("Failed to close Ngrok process input streams", exception);
        }

        this.stopped = true;
    }

    public void close() {
        process.destroy();
        exiting = true;
    }

    public boolean hasStopped() {
        return stopped;
    }

}
