package cc.rayen.trevoraddons.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TrevorConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("trevoraddons.json");

    public boolean markTrevorAnimals = true;
    public boolean lineToTrevorAnimals = true;
    public double tracerLineWidth = 4.0;
    public int tracerLineColor = 0xFFF9B233;

    public static TrevorConfig load() {
        try {
            if (Files.exists(PATH)) {
                TrevorConfig cfg = GSON.fromJson(Files.readString(PATH), TrevorConfig.class);
                return cfg != null ? cfg : new TrevorConfig();
            }
        } catch (Exception ignored) {
        }
        TrevorConfig cfg = new TrevorConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }
}
