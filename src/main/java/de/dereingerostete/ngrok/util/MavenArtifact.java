/*
 * Copyright (c) 2023 - DerEingerostete
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package de.dereingerostete.ngrok.util;

import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Data
public class MavenArtifact {
    private static final @NotNull String BASE_URL = "https://repo1.maven.org/maven2/";
    private final @NotNull String groupId;
    private final @NotNull String artifactId;

    @NotNull
    public String getLatestVersion() throws IOException {
        URL url = new URL(BASE_URL + groupId + "/" + artifactId + "/maven-metadata.xml");
        String metadata = IOUtils.toString(url, StandardCharsets.UTF_8);

        int index = metadata.indexOf("<release>") + 9;
        int endIndex = metadata.indexOf('<', index);
        return metadata.substring(index, endIndex);
    }

    public void downloadArtifact(@NotNull String version, @NotNull File output) throws IOException {
        String fileName = artifactId + "-" + version + ".jar";
        URL url = new URL(BASE_URL + groupId + "/" + artifactId + "/" + version + "/" + fileName);
        FileUtils.copyURLToFile(url, output);
    }

}
