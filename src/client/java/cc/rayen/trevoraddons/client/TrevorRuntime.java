package cc.rayen.trevoraddons.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;

public final class TrevorRuntime {
    private static final float ALWAYS_VALID_TREVOR_HP = 1024.0f;
    private static final float TRACKABLE_HP = 100.0f;
    private static final float UNTRACKABLE_HP = 500.0f;
    private static final float UNDETECTED_HP = 1000.0f;
    private static final float ENDANGERED_HP = 5000.0f;
    private static final float ELUSIVE_HP = 10000.0f;
    private static final Set<Integer> MARKED_ENTITY_IDS = new HashSet<>();
    private static boolean OPEN_SETTINGS_NEXT_TICK = false;

    private TrevorRuntime() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(TrevorRuntime::onTick);
    }

    private static void onTick(MinecraftClient client) {
        if (OPEN_SETTINGS_NEXT_TICK) {
            OPEN_SETTINGS_NEXT_TICK = false;
            client.setScreen(new TrevorSettingsScreen(client.currentScreen));
        }

        if (client.world == null || client.player == null) return;
        Set<Integer> now = new HashSet<>();
        for (Entity e : client.world.getEntities()) {
            if (isTrevorAnimal(e)) {
                now.add(e.getId());
            }
        }
        MARKED_ENTITY_IDS.clear();
        MARKED_ENTITY_IDS.addAll(now);
    }

    private static boolean isTrevorAnimal(Entity entity) {
        if (entity instanceof PlayerEntity) return false;
        if (entity instanceof AbstractHorseEntity) return true;
        if (!(entity instanceof CowEntity
                || entity instanceof PigEntity
                || entity instanceof SheepEntity
                || entity instanceof RabbitEntity
                || entity instanceof ChickenEntity)) {
            return false;
        }
        if (!(entity instanceof LivingEntity living)) return false;
        float maxHealth = living.getMaxHealth();
        return healthMatches(maxHealth, ALWAYS_VALID_TREVOR_HP) || isKnownTrevorTierHealth(maxHealth);
    }

    public static boolean shouldMarkTrevorAnimal(Entity entity) {
        return isTrevorAnimal(entity);
    }

    public static Set<Integer> getMarkedEntityIds() {
        return Set.copyOf(MARKED_ENTITY_IDS);
    }

    private static boolean isKnownTrevorTierHealth(float hp) {
        return healthMatches(hp, TRACKABLE_HP)
                || healthMatches(hp, UNTRACKABLE_HP)
                || healthMatches(hp, UNDETECTED_HP)
                || healthMatches(hp, ENDANGERED_HP)
                || healthMatches(hp, ELUSIVE_HP);
    }

    private static boolean healthMatches(float value, float expected) {
        return Math.abs(value - expected) < 0.01f;
    }

    public static void requestOpenSettings() {
        OPEN_SETTINGS_NEXT_TICK = true;
    }
}
