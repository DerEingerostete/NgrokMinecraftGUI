/*
 * Copyright (c) 2023 - DerEingerostete
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package de.dereingerostete.ngrok.util.config;

import de.dereingerostete.ngrok.util.GUIUtils;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A read only configuration for additional values
 * The setter methods are only internally used and should be ignored
 */
@Data
public class ReadOnlyConfig {
    private @Nullable String region;
    private @NotNull String executable;
    private @NotNull String downloadPath;
    private @NotNull Map<String, String> parameters;
    private @NotNull Theming theming;

    public ReadOnlyConfig() {
        this.region = null;
        this.executable = "ngrok.exe";
        this.downloadPath = "https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-windows-amd64.zip";
        this.parameters = new HashMap<>();
        this.theming = new Theming();
    }

    @NotNull
    public static ReadOnlyConfig getConfiguration(@NotNull File file) throws IOException {
        String content = ConfigUtils.loadOrCreateData(file);
        Yaml yaml = new Yaml();
        return yaml.loadAs(content, ReadOnlyConfig.class);
    }

}
