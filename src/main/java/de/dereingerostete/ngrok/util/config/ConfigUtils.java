package de.dereingerostete.ngrok.util.config;

import de.dereingerostete.ngrok.Bootstrap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

public class ConfigUtils {
    private static final int MAX_DISPLAY_LENGTH = 14;

    @NotNull
    public static String loadOrCreateData(@NotNull File file) throws IOException {
        if (!file.exists()) {
            String name = file.getName();

            ClassLoader loader = ReadOnlyConfig.class.getClassLoader();
            URL url = loader.getResource(name);
            if (url == null) throw new IllegalStateException("Missing config file in resources");

            String content = IOUtils.toString(url, StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8, false);
            return content;
        } else {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        }
    }

    @NotNull
    public static String displayPath(@Nullable String path) {
        if (path == null) return "None";

        // Hacky way to check for the %appdata% environment path
        String folderName = FilenameUtils.getName(path);
        String appdata = System.getenv("appdata");
        if (appdata != null) {
            File baseInstallFile = new File(appdata, folderName);
            File file = new File(path);
            if (baseInstallFile.equals(file)) {
                return "%appdata%/" + folderName;
            }
        }

        int length = path.length();
        if (length > MAX_DISPLAY_LENGTH) {
            path = path.substring(length - MAX_DISPLAY_LENGTH - 5);
            return "[...]" + path;
        } else {
            return path;
        }
    }

    // Currently only supports Windows
    @Nullable
    public static File detectMinecraftInstance() {
        String appdata = System.getenv("appdata");
        if (appdata == null) return null;

        File minecraftFolder = new File(appdata, ".minecraft");
        return minecraftFolder.exists() && minecraftFolder.isDirectory() ? minecraftFolder : null;
    }

    @NotNull
    public static OptionalInt detectMinecraftPort() throws IOException {
        Configuration configuration = Bootstrap.getConfiguration();
        String folderPath = configuration.getMinecraftFolder();
        if (folderPath == null) return OptionalInt.empty();

        File logsDir = new File(folderPath, "logs");
        if (!logsDir.exists() || !logsDir.isDirectory()) {
            Bootstrap.LOGGER.warn("Minecraft's log folder does not exist or isn't a directory");
            return OptionalInt.empty();
        }

        File latestLogFile = new File(logsDir, "latest.log");
        if (!latestLogFile.exists() || !latestLogFile.isFile()) {
            Bootstrap.LOGGER.warn("latest.log does not exist or isn't a file");
            return OptionalInt.empty();
        }

        List<String> lines = FileUtils.readLines(latestLogFile, StandardCharsets.UTF_8);
        Collections.reverse(lines);
        for (String line : lines) {
            if (!line.contains("Started serving") && !line.contains("Local game hosted on port")) continue;

            int lastSpace = line.lastIndexOf(' ');
            if (lastSpace == -1) continue;

            String portString = line.substring(lastSpace)
                    .replace("\n", "")
                    .replace("\r", "")
                    .trim();

            try {
                int port = Integer.parseInt(portString);
                if (port >= 1000 && port <= 65535) return OptionalInt.of(port);
                else Bootstrap.LOGGER.warn("Invalid port number (" + port + "). Skipped");
            } catch (NumberFormatException exception) {
                Bootstrap.LOGGER.warn("Failed to parse port. Maybe not the right port?", exception);
            }
        }

        // Nothing found
        return OptionalInt.empty();
    }

}
