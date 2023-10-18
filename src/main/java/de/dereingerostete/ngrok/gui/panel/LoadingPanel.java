/*
 * Copyright (c) 2023 - DerEingerostete
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package de.dereingerostete.ngrok.gui.panel;

import com.formdev.flatlaf.extras.components.FlatProgressBar;
import de.dereingerostete.ngrok.util.GUIUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class LoadingPanel extends ActivePanel {
    private final @NotNull FlatProgressBar progressBar;
    private final @NotNull JLabel infoLabel;

    public LoadingPanel(@NotNull String info) {
        setLayout(null);

        this.infoLabel = new JLabel(info);
        infoLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setFont(GUIUtils.DEFAULT_FONT);
        infoLabel.setBounds(10, 65, 595, 30);
        add(infoLabel);

        this.progressBar = new FlatProgressBar();
        progressBar.setBounds(80, 106, 430, 25);
        progressBar.setIndeterminate(true);
        add(progressBar);
    }

    public void setInfo(@NotNull String info) {
        this.infoLabel.setText(info);
    }

    @Override
    public void updateComponentSize() {
        int mainWidth = getWidth();
        int mainHeight = (int) (getHeight() / 1.25);

        int padding = getWidth() / 25;
        int componentWidth = getWidth() - (padding * 2);
        int componentHeight = 30;

        int x = (mainWidth - componentWidth) / 2;
        int yLabel = (mainHeight - 2 * componentHeight) / 2;
        int yProgressBar = yLabel + componentHeight + 5;

        this.infoLabel.setBounds(x, yLabel, componentWidth, componentHeight);
        this.progressBar.setBounds(x, yProgressBar, componentWidth, componentHeight);
    }

}

