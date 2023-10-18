/*
 * Copyright (c) 2023 - DerEingerostete
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package de.dereingerostete.ngrok.gui;

import de.dereingerostete.ngrok.Bootstrap;
import de.dereingerostete.ngrok.client.NgrokProcessThread;
import de.dereingerostete.ngrok.gui.panel.ActivePanel;
import de.dereingerostete.ngrok.gui.panel.LoadingPanel;
import de.dereingerostete.ngrok.gui.panel.MainPanel;
import de.dereingerostete.ngrok.util.GUIUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class NgrokGUI {
    private final @NotNull JFrame frame;
    private @Getter @NotNull ActivePanel activePanel;

    public NgrokGUI() {
        frame = new JFrame();
        frame.setVisible(false);
        frame.setTitle("Ngrok Minecraft GUI");
        frame.setBounds(100, 100, 650, 395);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowCloseListener());
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setLayout(null);

        JLabel titleText = new JLabel("Ngrok Minecraft GUI");
        titleText.setToolTipText("This is a title. Isn't that cool?");
        titleText.setFont(GUIUtils.TITLE_FONT);
        titleText.setHorizontalAlignment(SwingConstants.CENTER);
        titleText.setBounds(10, 0, 615, 25);
        frame.add(titleText);

        JSeparator titleSeparator = new JSeparator();
        titleSeparator.setBounds(220, 25, 200, 10);
        frame.add(titleSeparator);

        this.activePanel = new LoadingPanel("Loading...");
        changePanel(activePanel);
    }

    public void changePanel(@NotNull ActivePanel panel) {
        frame.remove(activePanel);
        frame.add(panel);

        panel.setBounds(10, 40, 615, 305);
        panel.updateComponentSize();

        this.activePanel = panel;
        SwingUtilities.updateComponentTreeUI(frame);
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    private static class WindowCloseListener extends WindowAdapter {

        @Override
        public void windowClosing(@NotNull WindowEvent event) {
            ActivePanel activePanel = Bootstrap.getGui().getActivePanel();
            if (activePanel instanceof MainPanel) {
                MainPanel mainPanel = (MainPanel) activePanel;
                NgrokProcessThread thread = mainPanel.getThread();
                if (thread != null) {
                    Bootstrap.LOGGER.info("Closing Ngrok process");
                    thread.close();
                }
            }

            Thread thread = new Thread(Bootstrap::handleExit);
            thread.setName("ClosingThread");
            thread.setDaemon(true);
            thread.start();
        }

    }

}
