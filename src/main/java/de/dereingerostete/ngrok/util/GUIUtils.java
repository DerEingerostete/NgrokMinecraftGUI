/*
 * Copyright (c) 2023 - DerEingerostete
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package de.dereingerostete.ngrok.util;

import de.dereingerostete.ngrok.Bootstrap;
import de.dereingerostete.ngrok.gui.panel.MainPanel;
import de.dereingerostete.ngrok.util.config.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class GUIUtils {
    public static final @NotNull Font DEFAULT_FONT = loadFont("Kanit-Regular.ttf", 16f);
    public static final @NotNull Font BUTTON_FONT = DEFAULT_FONT.deriveFont(14f);
    public static final @NotNull Font BOLD_FONT = loadFont("Kanit-SemiBold.ttf", 16f);
    public static final @NotNull Font TITLE_FONT = BOLD_FONT.deriveFont(24f);

    @NotNull
    public static ActionListener createDefaultPortListener(@NotNull MainPanel panel) {
        return event -> {
            Configuration configuration = Bootstrap.getConfiguration();
            String message = "Please enter the new default port (1.000 - 65.535)";

            String input = JOptionPane.showInputDialog(panel, message, configuration.getDefaultPort());
            if (input == null) {
                return;
            }

            int port;
            try {
                Locale locale = Locale.getDefault();
                NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
                Number number = numberFormat.parse(input);
                port = number.intValue();
            } catch (ParseException exception) {
                showWarning(panel, "Invalid input", "You need to enter a valid number!");
                return;
            }

            if (port < 1000 || port > 65535) {
                showWarning(panel, "Invalid input", "The port must be between 1.000 and 65.535!");
                return;
            }

            try {
                configuration.setDefaultPort(port);
                configuration.save(Bootstrap.getConfigFile());

                panel.setDefaultPort(port);
            } catch (IOException exception) {
                Bootstrap.LOGGER.warn("Failed to save config", exception);
                showWarning(panel, "Unexpected exception", "Failed to save configuration. Please try again later.");
            }
        };
    }

    @NotNull
    public static MouseListener createOnClickListener() {
        return new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent event) {
                try {
                    if (!SwingUtilities.isLeftMouseButton(event)) return;
                    if (!Desktop.isDesktopSupported()) return;

                    Desktop desktop = Desktop.getDesktop();
                    desktop.browse(URI.create("https://dereingerostete.dev/"));
                } catch (IOException exception) {
                    Bootstrap.LOGGER.warn("Failed to open url in browser", exception);
                }
            }

        };
    }

    @NotNull
    public static MouseListener createCopyClickListener(@NotNull JLabel label) {
        return new MouseAdapter() {
            private final @NotNull Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            @Override
            public void mouseReleased(@NotNull MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) return;

                String text = label.getText();
                if (text.equals("None")) return;

                StringSelection stringSelection = new StringSelection(text);
                clipboard.setContents(stringSelection, null);
            }

        };
    }

    @NotNull
    public static ActionListener createFolderSelectListener(@NotNull MainPanel panel) {
        return event -> {
            Configuration configuration = Bootstrap.getConfiguration();
            File currentFolder = configuration.getFolderOrDefault();

            JFileChooser chooser = new JFileChooser(currentFolder);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Minecraft directory");
            chooser.setDialogType(JFileChooser.OPEN_DIALOG);
            chooser.setMultiSelectionEnabled(false);
            chooser.setSelectedFile(currentFolder);

            Action details = chooser.getActionMap().get("viewTypeDetails");
            details.actionPerformed(null);

            int result = chooser.showOpenDialog(panel);
            if (result != JFileChooser.APPROVE_OPTION) return;

            File selectedFile = chooser.getSelectedFile();
            if (!selectedFile.exists()) {
                showWarning(panel, "Invalid option", "The selected directory does not exist!");
                return;
            } else if (!selectedFile.isDirectory()) {
                showWarning(panel, "Invalid option", "You need to select a directory!");
                return;
            }

            try {
                String path = selectedFile.getCanonicalPath();
                configuration.setMinecraftFolder(path);
                configuration.save(Bootstrap.getConfigFile());
                panel.setFolderPath(path);
            } catch (IOException exception) {
                Bootstrap.LOGGER.warn("Failed to save config", exception);
                showWarning(panel, "Unexpected exception", "Failed to save configuration. Please try again later.");
            }
        };
    }

    @NotNull
    public static ImageIcon loadIcon(@NotNull String name) throws IllegalStateException {
        URL url = getResourceAsURL(name);
        return new ImageIcon(url);
    }

    @NotNull
    public static BufferedImage loadImage(@NotNull String name) throws IllegalStateException {
        try {
            URL url = getResourceAsURL(name);
            return ImageIO.read(url);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @NotNull
    public static URL getResourceAsURL(@NotNull String name) {
        ClassLoader loader = GUIUtils.class.getClassLoader();
        URL url = loader.getResource(name);
        if (url == null) throw new IllegalStateException(name + " was not found as resource");
        return url;
    }

    @NotNull
    public static Font loadFont(@NotNull String name, float size) throws IllegalStateException {
        ClassLoader loader = GUIUtils.class.getClassLoader();
        try (InputStream inputStream = loader.getResourceAsStream("fonts/" + name)) {
            if (inputStream == null) throw new IllegalStateException("No font found with that name");

            Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream);
            return font.deriveFont(size);
        } catch (IOException | FontFormatException exception) {
            throw new IllegalStateException("Failed to load font", exception);
        }
    }

    public static void showWarning(@Nullable Component parentComponent, @NotNull String title, @NotNull String message) {
        JOptionPane.showMessageDialog(parentComponent, message, title, JOptionPane.WARNING_MESSAGE);
    }

}
