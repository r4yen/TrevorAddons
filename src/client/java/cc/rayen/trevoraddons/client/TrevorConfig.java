package cc.rayen.trevoraddons.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TrevorConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("trevoraddons.json");

    public static final String DEFAULT_PRESET_ID = "default";
    public static final String HORSE_FAMILY_KEY = "trevoraddons:horse_family";

    private static final List<EntityRule> DEFAULT_ENTITY_RULES = List.of(
            new EntityRule(HORSE_FAMILY_KEY, "Horse Family", List.of(1024.0)),
            new EntityRule("minecraft:cow", "Cow", List.of(100.0, 500.0, 1000.0, 5000.0, 10000.0)),
            new EntityRule("minecraft:pig", "Pig", List.of(100.0, 500.0, 1000.0, 5000.0, 10000.0)),
            new EntityRule("minecraft:sheep", "Sheep", List.of(100.0, 500.0, 1000.0, 5000.0, 10000.0)),
            new EntityRule("minecraft:rabbit", "Rabbit", List.of(100.0, 500.0, 1000.0, 5000.0, 10000.0)),
            new EntityRule("minecraft:chicken", "Chicken", List.of(100.0, 500.0, 1000.0, 5000.0, 10000.0))
    );

    public boolean markTrevorAnimals = true;
    public boolean lineToTrevorAnimals = true;
    public double tracerLineWidth = 4.0;
    public int tracerLineColor = 0xFFF9B233;
    public String activePresetId = DEFAULT_PRESET_ID;
    public List<Preset> presets = new ArrayList<>();

    public static TrevorConfig load() {
        TrevorConfig cfg = null;
        try {
            if (Files.exists(PATH)) {
                cfg = GSON.fromJson(Files.readString(PATH), TrevorConfig.class);
            }
        } catch (Exception ignored) {
        }

        if (cfg == null) {
            cfg = new TrevorConfig();
        }

        cfg.normalize();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            normalize();
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }

    public Preset getPresetById(String presetId) {
        if (presetId == null) return null;
        for (Preset preset : presets) {
            if (presetId.equals(preset.id)) {
                return preset;
            }
        }
        return null;
    }

    public Preset getActivePreset() {
        Preset active = getPresetById(activePresetId);
        if (active != null) {
            return active;
        }
        Preset fallback = getPresetById(DEFAULT_PRESET_ID);
        if (fallback != null) {
            return fallback;
        }
        return presets.isEmpty() ? null : presets.get(0);
    }

    public String getActivePresetName() {
        Preset active = getActivePreset();
        return active != null ? active.name : "Default";
    }

    public boolean isActivePresetLocked() {
        Preset active = getActivePreset();
        return active == null || !active.editable;
    }

    public Preset duplicatePreset(String sourcePresetId) {
        Preset source = getPresetById(sourcePresetId);
        if (source == null) {
            source = getPresetById(DEFAULT_PRESET_ID);
        }
        if (source == null) {
            source = createDefaultPreset();
        }

        Preset copy = source.copy();
        copy.id = nextPresetId();
        copy.name = nextPresetName();
        copy.editable = true;
        presets.add(copy);
        activePresetId = copy.id;
        return copy;
    }

    public boolean deletePreset(String presetId) {
        if (presetId == null || DEFAULT_PRESET_ID.equals(presetId)) {
            return false;
        }

        boolean removed = presets.removeIf(preset -> presetId.equals(preset.id));
        if (removed && presetId.equals(activePresetId)) {
            activePresetId = DEFAULT_PRESET_ID;
        }
        return removed;
    }

    public void setActivePreset(String presetId) {
        if (getPresetById(presetId) != null) {
            activePresetId = presetId;
        }
    }

    public boolean matchesTrevorAnimal(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            return false;
        }

        Preset active = getActivePreset();
        if (active == null) {
            return false;
        }
        return active.matches(entity, living.getMaxHealth());
    }

    private void normalize() {
        if (presets == null) {
            presets = new ArrayList<>();
        }

        for (int i = 0; i < presets.size(); i++) {
            normalizePreset(presets.get(i));
        }

        if (getPresetById(DEFAULT_PRESET_ID) == null) {
            presets.add(0, createDefaultPreset());
        } else if (presets.isEmpty() || presets.get(0) == null || !DEFAULT_PRESET_ID.equals(presets.get(0).id)) {
            Preset defaultPreset = getPresetById(DEFAULT_PRESET_ID);
            presets.remove(defaultPreset);
            presets.add(0, defaultPreset);
        }

        if (getPresetById(activePresetId) == null) {
            activePresetId = DEFAULT_PRESET_ID;
        }
    }

    private void normalizePreset(Preset preset) {
        if (preset == null) return;
        if (preset.id == null || preset.id.isBlank()) {
            preset.id = nextPresetId();
        }
        if (preset.name == null || preset.name.isBlank()) {
            preset.name = DEFAULT_PRESET_ID.equals(preset.id) ? "Default" : "Preset";
        }
        preset.editable = !DEFAULT_PRESET_ID.equals(preset.id);
        if (preset.entities == null) {
            preset.entities = new ArrayList<>();
        }

        for (EntityRule defaultRule : DEFAULT_ENTITY_RULES) {
            if (preset.getRule(defaultRule.id) == null) {
                preset.entities.add(defaultRule.copy());
            }
        }

        for (int i = 0; i < preset.entities.size(); i++) {
            EntityRule rule = preset.entities.get(i);
            if (rule == null) {
                preset.entities.set(i, new EntityRule("unknown", "Unknown", List.of()));
            } else {
                rule.normalize();
            }
        }
    }

    private String nextPresetId() {
        int index = 1;
        while (getPresetById("preset-" + index) != null) {
            index++;
        }
        return "preset-" + index;
    }

    private String nextPresetName() {
        int index = 1;
        while (hasPresetNamed("Preset " + index)) {
            index++;
        }
        return "Preset " + index;
    }

    private boolean hasPresetNamed(String presetName) {
        for (Preset preset : presets) {
            if (presetName.equalsIgnoreCase(preset.name)) {
                return true;
            }
        }
        return false;
    }

    public static Preset createDefaultPreset() {
        Preset preset = new Preset();
        preset.id = DEFAULT_PRESET_ID;
        preset.name = "Default";
        preset.editable = false;
        preset.entities = new ArrayList<>();
        for (EntityRule rule : DEFAULT_ENTITY_RULES) {
            preset.entities.add(rule.copy());
        }
        return preset;
    }

    private static String displayNameForKey(String key) {
        if (HORSE_FAMILY_KEY.equals(key)) {
            return "Horse Family";
        }
        Identifier id = Identifier.tryParse(key);
        if (id == null) {
            return key;
        }

        String path = id.getPath().replace('_', ' ');
        StringBuilder out = new StringBuilder(path.length());
        boolean capitalizeNext = true;
        for (char c : path.toCharArray()) {
            if (capitalizeNext && Character.isLetter(c)) {
                out.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                out.append(c);
            }
            if (c == ' ') {
                capitalizeNext = true;
            }
        }
        return out.toString();
    }

    private static boolean matchesRule(Entity entity, String key, double maxHealth) {
        if (HORSE_FAMILY_KEY.equals(key)) {
            return entity instanceof AbstractHorseEntity;
        }

        Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
        if (entityId == null || !key.equals(entityId.toString())) {
            return false;
        }

        return true;
    }

    public static final class Preset {
        public String id;
        public String name;
        public boolean editable = true;
        public List<EntityRule> entities = new ArrayList<>();

        public Preset copy() {
            Preset out = new Preset();
            out.id = id;
            out.name = name;
            out.editable = editable;
            out.entities = new ArrayList<>();
            for (EntityRule rule : entities) {
                out.entities.add(rule.copy());
            }
            return out;
        }

        public EntityRule getRule(String ruleId) {
            if (ruleId == null) return null;
            for (EntityRule rule : entities) {
                if (ruleId.equals(rule.id)) {
                    return rule;
                }
            }
            return null;
        }

        public boolean matches(Entity entity, double maxHealth) {
            for (EntityRule rule : entities) {
                if (rule.matches(entity, maxHealth)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class EntityRule {
        public String id;
        public String name;
        public List<Double> healthValues = new ArrayList<>();

        public EntityRule() {
        }

        public EntityRule(String id, String name, List<Double> healthValues) {
            this.id = id;
            this.name = name;
            this.healthValues = new ArrayList<>(healthValues);
        }

        public EntityRule copy() {
            return new EntityRule(id, name, healthValues == null ? List.of() : healthValues);
        }

        public void normalize() {
            if (id == null || id.isBlank()) {
                id = "unknown";
            }
            if (name == null || name.isBlank()) {
                name = displayNameForKey(id);
            }
            if (healthValues == null) {
                healthValues = new ArrayList<>();
            }
        }

        public boolean matches(Entity entity, double maxHealth) {
            if (!matchesRule(entity, id, maxHealth)) {
                return false;
            }
            for (Double value : healthValues) {
                if (value != null && (Double.isNaN(value) || Math.abs(value - maxHealth) < 0.01d)) {
                    return true;
                }
            }
            return false;
        }
    }
}
