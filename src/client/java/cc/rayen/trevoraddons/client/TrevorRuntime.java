package cc.rayen.trevoraddons.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.HashSet;
import java.util.Set;

public final class TrevorRuntime {
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

        if (client.world == null || client.player == null || TrevorAddonsClient.CONFIG == null) {
            MARKED_ENTITY_IDS.clear();
            return;
        }
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
        return TrevorAddonsClient.CONFIG.matchesTrevorAnimal(entity);
    }

    public static boolean shouldMarkTrevorAnimal(Entity entity) {
        return TrevorAddonsClient.CONFIG != null && isTrevorAnimal(entity);
    }

    public static Set<Integer> getMarkedEntityIds() {
        return Set.copyOf(MARKED_ENTITY_IDS);
    }

    public static void requestOpenSettings() {
        OPEN_SETTINGS_NEXT_TICK = true;
    }
}
