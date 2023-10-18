/*
 * Copyright (c) 2023 - DerEingerostete
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package de.dereingerostete.ngrok.util.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class Theming {
    private @NotNull String themeType;
    private @NotNull String jarClass;
    private @NotNull String jarFile;
    private @NotNull String jsonFile;
    private @NotNull String hyperlinkColor;

    public Theming() {
        this.themeType = "JAR";
        this.jarClass = "com.formdev.flatlaf.FlatDarkLaf";
        this.jarFile = "theme.jar";
        this.jsonFile = "theme.json";
        this.hyperlinkColor = "#489fb5";
    }

}
