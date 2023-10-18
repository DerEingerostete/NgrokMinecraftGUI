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
import de.dereingerostete.ngrok.gui.panel.ActivePanel;
import de.dereingerostete.ngrok.gui.panel.MainPanel;
import de.dereingerostete.ngrok.util.GUIUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.OptionalInt;

import static de.dereingerostete.ngrok.Bootstrap.LOGGER;

public class NgrokHandler {

    public static void handle(@NotNull String message, @NotNull NgrokProcessThread thread) {
        JSONObject messageObject = parseJsonMessage(message);
        if (messageObject == null) return;

        String level = messageObject.optString("lvl", null);
        if (level == null) {
            LOGGER.warn("Ngrok JSON message did not contain a log level");
            return;
        }

        if (level.equals("eror")) handleError(messageObject, thread);
        else if (level.equals("info")) handleInfo(messageObject);
        else LOGGER.warn("Ngrok JSON message contain unknown log level: " + message);
    }

    public static void handleInfo(@NotNull JSONObject messageObject) {
        if (messageObject.has("url")) handleURL(messageObject);
        else LOGGER.info("Unknown Ngrok info message: " + messageObject);
    }

    public static void handleURL(@NotNull JSONObject messageObject) {
        String url = messageObject.getString("url");
        if (url.startsWith("tcp://")) url = url.substring(6);

        ActivePanel activePanel = Bootstrap.getGui().getActivePanel();
        if (!(activePanel instanceof MainPanel)) return;

        MainPanel mainPanel = (MainPanel) activePanel;
        mainPanel.setIpText(url);
    }

    public static void handleError(@NotNull JSONObject messageObject, @NotNull NgrokProcessThread thread) {
        OptionalInt optionalError = getErrorCode(messageObject);
        if (optionalError.isEmpty()) return;

        int errorCode = optionalError.getAsInt();
        switch (errorCode) {
            case 102:
            case 103:
                handleBannedAccount(thread, errorCode);
                break;
            case 105:
                handleInvalidToken(thread);
                break;
            case 108:
                handleSessionLimit(thread);
                break;
            default:
                handleUnknownError(messageObject, errorCode);
                break;
        }
    }

    private static void handleSessionLimit(@NotNull NgrokProcessThread thread) {
        LOGGER.warn("Ngrok account has a session already running");
        thread.close();
        GUIUtils.showWarning(null, "Ngrok already running",
                "Your Ngrok account reached the max. amount of simultaneous sessions.\n" +
                        "Please close a sessions or try again later.");
    }

    private static void handleBannedAccount(@NotNull NgrokProcessThread thread, int errorCode) {
        LOGGER.warn("Ngrok account seems to be banned. Stopping process");
        thread.close();
        GUIUtils.showWarning(null, "Banned Ngrok token",
                "It seems like you entered an auth token of an account that has\n" +
                        "been banned. Please check your Ngrok account or try again.\n" +
                        "Error: ERR_NGROK_" + errorCode);
    }

    private static void handleUnknownError(@NotNull JSONObject messageObject, int code) {
        LOGGER.warn("Received unknown error: " + code);

        String error = messageObject.optString("err", "Unknown");
        GUIUtils.showWarning(null, "Unknown Ngrok error",
                "Ngrok encountered an unexpected error:\n" + error);
    }

    private static void handleInvalidToken(@NotNull NgrokProcessThread thread) {
        LOGGER.warn("Invalid Ngrok auth token. Stopping process");
        thread.close();
        GUIUtils.showWarning(null, "Invalid Ngrok token",
                "It seems like you entered an invalid auth token.\n" +
                        "Please update the auth token in the config.yml file.");
    }

    @NotNull
    private static OptionalInt getErrorCode(@NotNull JSONObject message) {
        String error = message.optString("err", null);
        if (error == null) {
            LOGGER.warn("Ngrok error seems to not be a error: " + message);
            return OptionalInt.empty();
        }

        int index = error.indexOf("ERR_NGROK_");
        if (index == -1) {
            LOGGER.warn("Ngrok error message does not include an error code: " + message);
            return OptionalInt.empty();
        }

        try {
            String errorPart = error.substring(index + 10)
                    .replace("\r", "")
                    .replace("\n", "")
                    .trim();

            int number = Integer.parseInt(errorPart);
            return OptionalInt.of(number);
        } catch (NumberFormatException exception) {
            LOGGER.warn("Failed to parse Ngrok error code", exception);
            return OptionalInt.empty();
        }
    }

    @Nullable
    private static JSONObject parseJsonMessage(@NotNull String error) {
        try {
            if (error.isBlank()) return null;
            if (error.charAt(0) != '{') {
                LOGGER.warn("Ngrok message is not JSON: " + error);
                return null;
            }

            return new JSONObject(error);
        } catch (JSONException exception) {
            LOGGER.warn("Failed to parse Ngrok JSON message", exception);
            return null;
        }
    }
}
