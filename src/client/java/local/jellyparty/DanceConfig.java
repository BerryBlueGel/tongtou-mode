package local.jellyparty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DanceConfig {
    public int configVersion = 1;
    public double radius = 24;
    public double spinStartSeconds = 10.24;
    public double bpm = 130;
    public double beatOffsetSeconds = 0;
    public double spinDegreesPerSecond = 240;
    public double hopHeight = 0.18;
    public double transitionSeconds = 0.2;
    public float volume = 0.8f;

    public static DanceConfig load() {
        return load(FabricLoader.getInstance().getConfigDir().resolve("jellyparty.json"));
    }

    static DanceConfig load(Path path) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        DanceConfig config = new DanceConfig();
        try {
            boolean save = !Files.exists(path);
            if (Files.exists(path)) {
                JsonObject document = gson.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
                if (document != null) {
                    config = gson.fromJson(document, DanceConfig.class);
                    // v0.1.0 had no version marker. Upgrade its default speed once,
                    // preserving other speeds and every unrelated setting.
                    if (!document.has("configVersion")) {
                        if (config.spinDegreesPerSecond == 180) config.spinDegreesPerSecond = 240;
                        save = true;
                    }
                }
            }
            if (save) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, gson.toJson(config) + "\n", StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException e) {
            JellyPartyClient.LOGGER.warn("Could not read jellyparty.json; using defaults", e);
        }
        config.radius = valid(config.radius, 24, 3, 96);
        config.spinStartSeconds = valid(config.spinStartSeconds, 10.24, 0, 54.33);
        config.bpm = valid(config.bpm, 130, 30, 300);
        config.beatOffsetSeconds = valid(config.beatOffsetSeconds, 0, 0, 10);
        config.spinDegreesPerSecond = valid(config.spinDegreesPerSecond, 240, 0, 720);
        config.hopHeight = valid(config.hopHeight, 0.18, 0, 0.75);
        config.transitionSeconds = valid(config.transitionSeconds, 0.2, 0.05, 2);
        config.volume = (float) valid(config.volume, 0.8, 0.05, 1);
        return config;
    }

    private static double valid(double value, double fallback, double min, double max) {
        return Double.isFinite(value) ? DanceMath.clamp(value, min, max) : fallback;
    }
}
