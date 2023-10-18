package de.dereingerostete.ngrok.util;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.IntelliJTheme;
import de.dereingerostete.ngrok.Bootstrap;
import de.dereingerostete.ngrok.util.config.Theming;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.*;

/**
 * Look and Feel manager
 */
@Data
public class LAFManager {
    private static final @NotNull MavenArtifact DEFAULT_THEME = new MavenArtifact("com/formdev", "flatlaf");
    private final @NotNull Theming theming;
    private @Nullable URLClassLoader themeClassLoader;

    public void load() throws IOException {
        String type = theming.getThemeType();
        if (type.equalsIgnoreCase("JAR")) {
            String jarPath = theming.getJarFile();
            String className = theming.getJarClass();
            loadJarTheme(jarPath, className);
        } else if (type.equalsIgnoreCase("JSON")) {
            String filePath = theming.getJsonFile();
            loadJsonTheme(filePath);
        } else {
            throw new IOException("Unknown theming type: " + type);
        }
    }

    public void close() throws IOException {
        if (themeClassLoader != null) {
            themeClassLoader.close();
        }
    }

    private void loadJsonTheme(@NotNull String filePath) throws IOException {
        if (filePath.isBlank()) throw new IllegalStateException("No theme.json file path was defined");

        File file = new File(Bootstrap.DATA_FOLDER, filePath);
        InputStream inputStream = new FileInputStream(file);

        try {
            FlatLaf flatLaf = IntelliJTheme.createLaf(inputStream);
            Exception exception = setLookAndFeel(flatLaf).get(1, TimeUnit.MINUTES);
            if (exception == null) {
                flatLaf.initialize();
            } else {
                throw new IOException("Failed to set look and feel", exception);
            }
        } catch (ExecutionException | TimeoutException | InterruptedException exception) {
            throw new IOException(exception);
        }
    }

    private void loadJarTheme(@NotNull String filePath, @NotNull String className) throws IOException {
        if (filePath.isBlank()) throw new IllegalStateException("No jar file path was defined");
        if (className.isBlank()) throw new IllegalStateException("No class name was defined");

        File file = new File(Bootstrap.DATA_FOLDER, filePath);
        if (!file.exists() && !file.getName().equals("FlatLaf.jar")) { // Check if the default is missing
            String latestVersion = DEFAULT_THEME.getLatestVersion();
            DEFAULT_THEME.downloadArtifact(latestVersion, file);
        }

        URL[] urls = {file.toURI().toURL()};
        ClassLoader platformClassLoader = ClassLoader.getPlatformClassLoader();

        try {
            this.themeClassLoader = new URLClassLoader("LAFThemeClassLoader", urls, platformClassLoader);
            Class<?> aClass = themeClassLoader.loadClass(className);
            Class<? extends LookAndFeel> lafClass = aClass.asSubclass(LookAndFeel.class);

            Constructor<? extends LookAndFeel> constructor = lafClass.getConstructor();
            LookAndFeel lookAndFeel = constructor.newInstance();

            Exception exception = setLookAndFeel(lookAndFeel).get(1, TimeUnit.MINUTES);
            if (exception != null) {
                throw new IOException("Failed to set look and feel", exception);
            }
        } catch (ReflectiveOperationException | ExecutionException |
                 TimeoutException | InterruptedException exception) {
            throw new IOException(exception);
        }
    }

    @NotNull
    private Future<Exception> setLookAndFeel(@NotNull LookAndFeel lookAndFeel) throws IOException {
        CompletableFuture<Exception> future = new CompletableFuture<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    Bootstrap.LOGGER.info("Setting look and feel to '" + lookAndFeel.getName() + "'");
                    UIManager.put("ProgressBar.arc", 100);
                    UIManager.setLookAndFeel(lookAndFeel);
                    future.complete(null);
                } catch (Exception exception) {
                    future.complete(exception);
                }
            });
        } catch (InterruptedException exception) {
            throw new IOException(exception);
        } catch (InvocationTargetException exception) {
            future.complete(exception);
        }
        return future;
    }

}
