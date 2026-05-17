package cc.rayen.trevoraddons.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public final class TrevorCommand {
    private static final int DEFAULT_DEBUG_RANGE = 128;
    private static final int MAX_DEBUG_RANGE = 512;

    private TrevorCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("trevoraddons")
                        .executes(ctx -> openSettings(ctx.getSource()))
                        .then(ClientCommandManager.literal("debug")
                                .executes(ctx -> debugLookedAtEntity(ctx.getSource(), DEFAULT_DEBUG_RANGE))
                                .then(ClientCommandManager.argument("range", IntegerArgumentType.integer(1, MAX_DEBUG_RANGE))
                                        .executes(ctx -> debugLookedAtEntity(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "range")))))
                        .then(ClientCommandManager.literal("settings").executes(ctx -> openSettings(ctx.getSource())))
        );

        dispatcher.register(
                ClientCommandManager.literal("ta")
                        .executes(ctx -> openSettings(ctx.getSource()))
                        .then(ClientCommandManager.literal("debug")
                                .executes(ctx -> debugLookedAtEntity(ctx.getSource(), DEFAULT_DEBUG_RANGE))
                                .then(ClientCommandManager.argument("range", IntegerArgumentType.integer(1, MAX_DEBUG_RANGE))
                                        .executes(ctx -> debugLookedAtEntity(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "range")))))
                        .then(ClientCommandManager.literal("settings").executes(ctx -> openSettings(ctx.getSource())))
        );
    }

    private static int openSettings(FabricClientCommandSource source) {
        TrevorRuntime.requestOpenSettings();
        source.sendFeedback(Text.literal("Opening TrevorAddons settings...").formatted(Formatting.GRAY));
        return 1;
    }

    private static int debugLookedAtEntity(FabricClientCommandSource source, int range) {
        MinecraftClient client = source.getClient();
        Entity entity = findEntityInSight(client, range);
        if (entity == null) {
            source.sendFeedback(Text.literal("No entity targeted in " + range + " blocks.").formatted(Formatting.RED));
            return 0;
        }
        boolean trevorMatch = TrevorRuntime.shouldMarkTrevorAnimal(entity);
        boolean espEnabled = TrevorAddonsClient.CONFIG != null && TrevorAddonsClient.CONFIG.markTrevorAnimals;
        boolean shouldBeMarkedNow = trevorMatch && espEnabled;

        StringBuilder entityDebug = new StringBuilder();
        entityDebug.append("Entity Details").append('\n')
                .append("------------------------------").append('\n')
                .append("Name: ").append(entity.getName().getString()).append('\n')
                .append("Type: ").append(entity.getType()).append('\n')
                .append("UUID: ").append(entity.getUuidAsString()).append('\n')
                .append("Entity ID: ").append(entity.getId()).append('\n')
                .append("Custom Name: ").append(entity.hasCustomName()).append('\n')
                .append("Age: ").append(entity.age).append('\n')
                .append("Fire Ticks: ").append(entity.getFireTicks()).append('\n')
                .append("Air: ").append(entity.getAir()).append('\n')
                .append("Silent: ").append(entity.isSilent()).append('\n')
                .append("Invulnerable: ").append(entity.isInvulnerable()).append('\n')
                .append("Sneaking: ").append(entity.isSneaking()).append('\n')
                .append("Sprinting: ").append(entity.isSprinting()).append('\n')
                .append("Swimming: ").append(entity.isSwimming()).append('\n')
                .append("Glowing: ").append(entity.isGlowing()).append('\n')
                .append("On Ground: ").append(entity.isOnGround()).append('\n')
                .append("Touching Water: ").append(entity.isTouchingWater()).append('\n')
                .append("Submerged in Water: ").append(entity.isSubmergedInWater()).append('\n')
                .append("In Lava: ").append(entity.isInLava()).append('\n')
                .append("Trevor Match (Max Health): ").append(trevorMatch).append('\n')
                .append("ESP Enabled (Settings): ").append(espEnabled).append('\n')
                .append("Should Be Marked: ").append(shouldBeMarkedNow).append('\n')
                .append("Velocity: ").append(fmt(entity.getVelocity().x)).append(", ").append(fmt(entity.getVelocity().y)).append(", ").append(fmt(entity.getVelocity().z)).append('\n')
                .append("Yaw/Pitch: ").append(fmt(entity.getYaw())).append(" / ").append(fmt(entity.getPitch())).append('\n')
                .append("Pos: ").append(fmt(entity.getX())).append(", ").append(fmt(entity.getY())).append(", ").append(fmt(entity.getZ())).append('\n')
                .append("BlockPos: ").append(entity.getBlockPos()).append('\n');

        StringBuilder livingDebug = new StringBuilder();
        if (entity instanceof LivingEntity living) {
            livingDebug.append("Living Stats").append('\n')
                    .append("------------------------------").append('\n')
                    .append("Health: ").append(fmt(living.getHealth())).append('\n')
                    .append("Max Health: ").append(fmt(living.getMaxHealth())).append('\n')
                    .append("Absorption: ").append(fmt(living.getAbsorptionAmount())).append('\n')
                    .append("Body Yaw: ").append(fmt(living.bodyYaw)).append('\n')
                    .append("Death Time: ").append(living.deathTime).append('\n')
                    .append("Fall Distance: ").append(fmt(living.fallDistance)).append('\n');
        }

        StringBuilder playerDebug = new StringBuilder();
        if (client.player != null) {
            playerDebug.append("Client Player").append('\n')
                    .append("------------------------------").append('\n')
                    .append("Name: ").append(client.player.getName().getString()).append('\n')
                    .append("Pos: ").append(fmt(client.player.getX())).append(", ").append(fmt(client.player.getY())).append(", ").append(fmt(client.player.getZ())).append('\n')
                    .append("Yaw/Pitch: ").append(fmt(client.player.getYaw())).append(" / ").append(fmt(client.player.getPitch())).append('\n')
                    .append("Health: ").append(fmt(client.player.getHealth())).append('\n')
                    .append("Hunger: ").append(client.player.getHungerManager().getFoodLevel()).append('\n')
                    .append("Saturation: ").append(fmt(client.player.getHungerManager().getSaturationLevel())).append('\n');
        }

        StringBuilder worldDebug = new StringBuilder();
        if (client.world != null) {
            worldDebug.append("Client World").append('\n')
                    .append("------------------------------").append('\n')
                    .append("Dimension: ").append(client.world.getRegistryKey().getValue()).append('\n')
                    .append("Time of Day: ").append(client.world.getTimeOfDay()).append('\n')
                    .append("Difficulty: ").append(client.world.getDifficulty().getName()).append('\n');
        }

        MutableText line = Text.literal("TrevorAddons ").formatted(Formatting.YELLOW, Formatting.BOLD)
                .append(Text.literal("» ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY).withBold(false)))
                .append(Text.literal("Debug Mode: ").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(false)))
                .append(bracketHoverChip("Entity Details", entityDebug.toString()));
        if (!livingDebug.isEmpty()) {
            line.append(Text.literal(" ").setStyle(Style.EMPTY.withBold(false)))
                    .append(bracketHoverChip("Living Stats", livingDebug.toString()));
        }
        if (!playerDebug.isEmpty()) {
            line.append(Text.literal(" ").setStyle(Style.EMPTY.withBold(false)))
                    .append(bracketHoverChip("Client Player", playerDebug.toString()));
        }
        if (!worldDebug.isEmpty()) {
            line.append(Text.literal(" ").setStyle(Style.EMPTY.withBold(false)))
                    .append(bracketHoverChip("Client World", worldDebug.toString()));
        }
        source.sendFeedback(line);
        return 1;
    }

    private static Entity findEntityInSight(MinecraftClient client, double range) {
        if (client.player == null || client.world == null) return null;

        Vec3d start = client.player.getEyePos();
        Vec3d look = client.player.getRotationVec(1.0F);
        Vec3d end = start.add(look.multiply(range));
        Box searchBox = client.player.getBoundingBox().stretch(look.multiply(range)).expand(1.0);

        return client.world.getOtherEntities(client.player, searchBox, entity -> entity.isAlive() && !entity.isSpectator())
                .stream()
                .map(entity -> {
                    Box box = entity.getBoundingBox().expand(entity.getTargetingMargin());
                    if (box.contains(start)) {
                        return new HitCandidate(entity, 0.0);
                    }
                    Optional<Vec3d> hit = box.raycast(start, end);
                    if (hit.isEmpty()) return null;
                    return new HitCandidate(entity, start.squaredDistanceTo(hit.get()));
                })
                .filter(candidate -> candidate != null)
                .min(Comparator.comparingDouble(HitCandidate::distanceSq))
                .map(HitCandidate::entity)
                .orElse(null);
    }

    private record HitCandidate(Entity entity, double distanceSq) {
    }

    private static MutableText bracketHoverChip(String label, String hoverText) {
        return Text.literal("[")
                .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY).withBold(false))
                .append(Text.literal(label).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(false)))
                .append(Text.literal("]").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY).withBold(false)))
                .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY).withBold(false).withHoverEvent(new HoverEvent.ShowText(buildColoredHoverText(hoverText))));
    }

    private static Text buildColoredHoverText(String raw) {
        String[] lines = raw.split("\n");
        MutableText out = Text.empty();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            MutableText lineText;
            if (line.startsWith("---")) {
                lineText = Text.literal(line).setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY).withBold(false));
            } else if (line.contains(": ")) {
                int idx = line.indexOf(": ");
                String key = line.substring(0, idx + 2);
                String value = line.substring(idx + 2);
                lineText = Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(false))
                        .append(Text.literal(value).setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false)));
            } else {
                lineText = Text.literal(line).setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withBold(true));
            }

            out.append(lineText);
            if (i < lines.length - 1) {
                out.append(Text.literal("\n"));
            }
        }
        return out;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
