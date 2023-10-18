package de.dereingerostete.ngrok.util.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Data
public class Configuration {
    private @Nullable String authToken;
    private @Nullable String minecraftFolder;
    private int defaultPort;

    public Configuration() {
        this.authToken = null;
        this.minecraftFolder = null;
        this.defaultPort = 25565;
    }

    @NotNull
    public File getFolderOrDefault() {
        if (minecraftFolder != null) return new File(minecraftFolder);
        String home = System.getProperty("user.home", ".");
        return new File(home);
    }

    public void save(@NotNull File file) throws IOException {
        FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);

        Representer representer = new Representer(options);
        representer.addClassTag(Configuration.class, Tag.MAP);

        Yaml yaml = new Yaml(representer);
        yaml.dump(this, writer);
        writer.close();
    }

    @NotNull
    public static Configuration getConfiguration(@NotNull File file) throws IOException {
        String content = ConfigUtils.loadOrCreateData(file);

        Yaml yaml = new Yaml();
        return yaml.loadAs(content, Configuration.class);
    }

}
