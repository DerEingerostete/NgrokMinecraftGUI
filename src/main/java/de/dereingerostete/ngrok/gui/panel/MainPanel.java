/*
 * Copyright (c) 2023 - DerEingerostete
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package de.dereingerostete.ngrok.gui.panel;

import de.dereingerostete.ngrok.Bootstrap;
import de.dereingerostete.ngrok.client.NgrokClient;
import de.dereingerostete.ngrok.client.NgrokProcessThread;
import de.dereingerostete.ngrok.util.GUIUtils;
import de.dereingerostete.ngrok.util.config.ConfigUtils;
import de.dereingerostete.ngrok.util.config.Configuration;
import de.dereingerostete.ngrok.util.config.ReadOnlyConfig;
import de.dereingerostete.ngrok.util.config.Theming;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.OptionalInt;

public class MainPanel extends ActivePanel {
    private static final int PADDING = 35;
    private final @NotNull JButton openPortButton;
    private final @NotNull JFormattedTextField portField;
    private final @NotNull JLabel defaultPortValue;
    private final @NotNull JLabel statusValue;
    private final @NotNull JLabel folderValue;
    private final @NotNull JLabel ipValue;
    private @Getter @Nullable NgrokProcessThread thread;

    public MainPanel() {
        setLayout(null);
        setSize(615, 305);

        // Config
        Configuration configuration = Bootstrap.getConfiguration();

        JLabel statusTitle = new JLabel("Status:");
        statusTitle.setBounds(PADDING, 0, 60, 30);
        statusTitle.setFont(GUIUtils.BOLD_FONT);
        add(statusTitle);

        this.statusValue = new JLabel("Offline");
        statusValue.setFont(GUIUtils.DEFAULT_FONT);
        statusValue.setBounds(PADDING + 80, 0, 90, 30);
        add(statusValue);

        JLabel ipTitle = new JLabel("Ngrok IP:");
        ipTitle.setFont(GUIUtils.BOLD_FONT);
        ipTitle.setBounds(PADDING, 30, 70, 30);
        add(ipTitle);

        this.ipValue = new JLabel("None");
        ipValue.setBounds(PADDING + 80, 30, 165, 30);
        ipValue.addMouseListener(GUIUtils.createCopyClickListener(ipValue));
        ipValue.setToolTipText("Click to copy");
        ipValue.setFont(GUIUtils.DEFAULT_FONT);
        add(ipValue);

        JLabel optionalTitle = new JLabel("Optional:");
        optionalTitle.setFont(GUIUtils.BOLD_FONT);
        optionalTitle.setBounds(PADDING, 100, 70, 30);
        add(optionalTitle);

        JSeparator optionalSeparator = new JSeparator();
        optionalSeparator.setBounds(PADDING - 5, 130, 85, 2);
        add(optionalSeparator);

        JLabel folderTitle = new JLabel("Minecraft Folder:");
        folderTitle.setFont(GUIUtils.BOLD_FONT);
        folderTitle.setBounds(PADDING, 140, 135, 30);
        add(folderTitle);

        this.folderValue = new JLabel("/");
        folderValue.setFont(GUIUtils.DEFAULT_FONT);
        folderValue.setBounds(PADDING + 135, 140, 300, 30);
        add(folderValue);
        setFolderPath(configuration.getMinecraftFolder());

        JLabel defaultPortTitle = new JLabel("Default Port:");
        defaultPortTitle.setFont(GUIUtils.BOLD_FONT);
        defaultPortTitle.setBounds(PADDING, 170, 135, 30);
        add(defaultPortTitle);

        int defaultPort = configuration.getDefaultPort();
        this.defaultPortValue = new JLabel(String.valueOf(defaultPort));
        defaultPortValue.setFont(GUIUtils.DEFAULT_FONT);
        defaultPortValue.setBounds(PADDING + 135, 170, 100, 30);
        add(defaultPortValue);

        JButton folderButton = new JButton("Set Minecraft folder");
        folderButton.addActionListener(GUIUtils.createFolderSelectListener(this));
        folderButton.setBounds(PADDING, 220, 170, 35);
        folderButton.setFont(GUIUtils.BUTTON_FONT);
        add(folderButton);

        JButton defaultPortButton = new JButton("Set default port");
        defaultPortButton.addActionListener(GUIUtils.createDefaultPortListener(this));
        defaultPortButton.setBounds(PADDING, 260, 170, 35);
        defaultPortButton.setFont(GUIUtils.BUTTON_FONT);
        add(defaultPortButton);

        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(false);

        JPanel borderPanel = new JPanel();
        borderPanel.setBounds(getWidth() - PADDING - 160, 0, 160, 40);
        borderPanel.setBorder(BorderFactory.createRaisedBevelBorder());
        borderPanel.setLayout(null);
        add(borderPanel);

        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(false);
        formatter.setMinimum(1);
        formatter.setMaximum(65535);

        this.portField = new JFormattedTextField(formatter);
        portField.setBounds(65, 5, 90, 30);
        portField.setText(String.valueOf(configuration.getDefaultPort()));
        portField.setHorizontalAlignment(JTextField.CENTER);
        portField.setFont(GUIUtils.DEFAULT_FONT);
        portField.setOpaque(false);
        borderPanel.add(portField);
        //textField.setBounds(getWidth() - PADDING - 100, 0, 100, 30);

        JLabel portTitle = new JLabel("Port:");
        portTitle.setBounds(10, 5, 45, 30);
        portTitle.setFont(GUIUtils.BOLD_FONT);
        borderPanel.add(portTitle);
        //portTitle.setBounds(textField.getX() - 50, 0, 40, 30);

        this.openPortButton = new JButton("Open port");
        openPortButton.setBounds(getWidth() - PADDING - 160, 45, 160, 35);
        openPortButton.addActionListener(event -> openTunnel(null));
        openPortButton.setFont(GUIUtils.BUTTON_FONT);
        add(openPortButton);

        JButton detectPortButton = new JButton("Detect port");
        detectPortButton.setBounds(getWidth() - PADDING - 160, 85, 160, 35);
        detectPortButton.setFont(GUIUtils.BUTTON_FONT);
        detectPortButton.addActionListener(event -> openDetect());
        add(detectPortButton);

        JButton openDefaultButton = new JButton("Open default");
        openDefaultButton.setBounds(getWidth() - PADDING - 160, 125, 160, 35);
        openDefaultButton.addActionListener(event -> openDefault());
        openDefaultButton.setFont(GUIUtils.BUTTON_FONT);
        add(openDefaultButton);

        JLabel authorText = new JLabel("By DerEingerostete");
        authorText.setBounds(getWidth() - 120, 285, 120, 20);
        authorText.setFont(GUIUtils.BUTTON_FONT.deriveFont(12f));
        authorText.addMouseListener(GUIUtils.createOnClickListener());
        authorText.setToolTipText("I made this :D");
        add(authorText);
    }

    @Override
    public void updateComponentSize() {}

    private void openDetect() {
        if (thread != null && !thread.hasStopped()) {
            showRunningWarning();
            return;
        }

        try {
            OptionalInt detectedPort = ConfigUtils.detectMinecraftPort();
            if (detectedPort.isEmpty()) {
                GUIUtils.showWarning(this, "No port detected", "No port was detected. " +
                        "Please make sure you selected the right Minecraft folder or try again.");
                return;
            }

            openTunnel(detectedPort.getAsInt());
        } catch (IOException exception) {
            Bootstrap.LOGGER.warn("Failed to detect Minecraft port", exception);
            GUIUtils.showWarning(this, "Unexpected error",
                    "Failed to detect Minecraft port. Please try again.");
        }
    }

    private void openDefault() {
        if (thread != null && !thread.hasStopped()) {
            showRunningWarning();
            return;
        }

        Configuration configuration = Bootstrap.getConfiguration();
        int defaultPort = configuration.getDefaultPort();
        openTunnel(defaultPort);
    }

    private void showRunningWarning() {
        GUIUtils.showWarning(this, "Already running", "Ngrok is already running. " +
                "Please close it first with the 'Close port' button and then try again.");
    }

    public void openTunnel(@Nullable Integer port) {
        if (thread != null && !thread.hasStopped()) {
            thread.close();
            return;
        }

        if (port == null) {
            String text = portField.getText();
            if (text.isEmpty()) {
                GUIUtils.showWarning(this, "Missing port", "You need to enter a port!");
                return;
            }

            port = Integer.parseInt(text);
        }

        try {
            if (port < 1000 || port > 65535) {
                GUIUtils.showWarning(this, "Invalid input", "The port must be between 1.000 and 65.535!");
                return;
            }
        } catch (NumberFormatException exception) { // Should not happen due the NumberFormatter
            throw new AssertionError(exception);
        }

        try {
            NgrokClient client = Bootstrap.getClient();
            this.thread = client.createTunnel(port, () -> {
                openPortButton.setText("Open port");
                setIpText(null);
            });

            if (thread != null) {
                openPortButton.setText("Close port");
                portField.setText(port.toString());
            } else {
                askForToken();
            }
        } catch (IOException exception) {
            Bootstrap.LOGGER.warn("Failed to start Ngrok", exception);
            GUIUtils.showWarning(this, "Unexpected Error", "Failed to start Ngrok. Please try again.");
        }
    }

    public void askForToken() {
        String token = JOptionPane.showInputDialog(this,
                "Auth token is missing. Please enter your Ngrok auth token! " +
                        "If you don't have one register at https://ngrok.com/.", "Missing auth token",
                JOptionPane.INFORMATION_MESSAGE);

        if (token == null || token.isEmpty()) return;
        int tokenIndex = token.indexOf("add-authtoken");
        if (tokenIndex != -1) { // Trim auth token from command
            token = token.substring(tokenIndex + 14);
        }

        try {
            Configuration configuration = Bootstrap.getConfiguration();
            configuration.setAuthToken(token);
            configuration.save(Bootstrap.getConfigFile());
        } catch (IOException exception) {
            Bootstrap.LOGGER.warn("Failed to save config", exception);
            GUIUtils.showWarning(this, "Unexpected exception", "Failed to save configuration. Please try again later.");
        }

        JOptionPane.showMessageDialog(this, "Successfully saved the auth token. " +
                "Please press the same button again to open the port.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public void setDefaultPort(int port) {
        this.defaultPortValue.setText(String.valueOf(port));
    }

    public void setFolderPath(@Nullable String path) {
        String displayPath = ConfigUtils.displayPath(path);
        this.folderValue.setText(displayPath);
    }

    public void setIpText(@Nullable String text) {
        if (text == null) {
            ipValue.setText("None");
            ipValue.setCursor(null);
            ipValue.setForeground(folderValue.getForeground());
            statusValue.setText("Offline");
        } else {
            ReadOnlyConfig config = Bootstrap.getReadOnlyConfig();
            Theming theming = config.getTheming();

            ipValue.setText(text);
            ipValue.setForeground(Color.decode(theming.getHyperlinkColor()));
            ipValue.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            statusValue.setText("Online");
        }
    }

}