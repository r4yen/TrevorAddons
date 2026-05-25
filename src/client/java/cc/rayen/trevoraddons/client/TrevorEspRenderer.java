package cc.rayen.trevoraddons.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TrevorEspRenderer {
    private static final float BOX_A = 1.0f;
    private static final double TRACER_START_OFFSET = 0.18;
    private static final double THICKNESS_WORLD_SCALE = 0.02;
    private static final int DISTANCE_BLACK = 0xFF000000;
    private static final double DISTANCE_BLACK_AT = 20.0;

    private TrevorEspRenderer() {
    }

    public static void init() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(TrevorEspRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || TrevorAddonsClient.CONFIG == null) return;

        boolean renderBoxes = TrevorAddonsClient.CONFIG.markTrevorAnimals;
        boolean renderLines = TrevorAddonsClient.CONFIG.lineToTrevorAnimals;
        if (!renderBoxes && !renderLines) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null || context.worldState().cameraRenderState == null || context.worldState().cameraRenderState.pos == null) {
            return;
        }

        Vec3d cameraPos = context.worldState().cameraRenderState.pos;
        MatrixStack matrices = context.matrices();
        MatrixStack.Entry entry = matrices.peek();
        int primaryColor = TrevorAddonsClient.CONFIG.tracerLineColor;
        float boxR = ((primaryColor >> 16) & 0xFF) / 255.0f;
        float boxG = ((primaryColor >> 8) & 0xFF) / 255.0f;
        float boxB = (primaryColor & 0xFF) / 255.0f;
        VertexConsumer boxConsumer = renderBoxes ? consumers.getBuffer(RenderLayer.getLines()) : null;
        VertexConsumer lineConsumer = renderLines ? consumers.getBuffer(RenderLayer.getLines()) : null;
        Quaternionf cameraOrientation = context.worldState().cameraRenderState.orientation;
        Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f).rotate(cameraOrientation);
        Vec3d viewDir = new Vec3d(forward.x, forward.y, forward.z);
        Vec3d tracerStart = viewDir.multiply(TRACER_START_OFFSET);

        for (Integer id : TrevorRuntime.getMarkedEntityIds()) {
            Entity entity = client.world.getEntityById(id);
            if (entity == null || !entity.isAlive() || entity.isRemoved()) continue;
            if (!TrevorRuntime.shouldMarkTrevorAnimal(entity)) continue;

            if (boxConsumer != null) {
                Box box = entity.getBoundingBox().expand(0.08).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                drawThickBox(entry, boxConsumer, box, boxR, boxG, boxB, viewDir);
            }

            if (lineConsumer != null) {
                Vec3d entityCenter = new Vec3d(
                        entity.getX() - cameraPos.x,
                        entity.getY() + (entity.getHeight() * 0.5) - cameraPos.y,
                        entity.getZ() - cameraPos.z
                );
                int lineColor = distanceColor(TrevorAddonsClient.CONFIG.tracerLineColor, entityCenter.length());
                drawThickLine(entry, lineConsumer, tracerStart, entityCenter, lineColor, viewDir);
            }
        }
    }

    private static void drawLine(MatrixStack.Entry entry, VertexConsumer consumer, Vec3d start, Vec3d end, int argbColor) {
        Vec3d normal = end.subtract(start).normalize();
        float nx = (float) normal.x;
        float ny = (float) normal.y;
        float nz = (float) normal.z;
        consumer.vertex(entry, (float) start.x, (float) start.y, (float) start.z).color(argbColor).normal(entry, nx, ny, nz);
        consumer.vertex(entry, (float) end.x, (float) end.y, (float) end.z).color(argbColor).normal(entry, nx, ny, nz);
    }

    private static void drawThickLine(MatrixStack.Entry entry, VertexConsumer consumer, Vec3d start, Vec3d end, int argbColor, Vec3d viewDir) {
        Vec3d delta = end.subtract(start);
        if (lengthSquared(delta) < 1.0e-10) {
            return;
        }

        Vec3d lineDir = normalize(delta);
        Vec3d perp1 = cross(lineDir, viewDir);
        if (lengthSquared(perp1) < 1.0e-10) {
            perp1 = cross(lineDir, new Vec3d(0.0, 1.0, 0.0));
        }
        if (lengthSquared(perp1) < 1.0e-10) {
            perp1 = new Vec3d(1.0, 0.0, 0.0);
        }
        perp1 = normalize(perp1);

        Vec3d perp2 = cross(lineDir, perp1);
        if (lengthSquared(perp2) < 1.0e-10) {
            perp2 = new Vec3d(0.0, 1.0, 0.0);
        }
        perp2 = normalize(perp2);

        double thickness = Math.max(0.00005d, TrevorAddonsClient.CONFIG.tracerLineWidth * THICKNESS_WORLD_SCALE);
        Vec3d off1 = perp1.multiply(thickness);
        Vec3d off2 = perp2.multiply(thickness);

        drawLine(entry, consumer, start, end, argbColor);
        drawLine(entry, consumer, start.add(off1), end.add(off1), argbColor);
        drawLine(entry, consumer, start.add(off1.multiply(-1.0)), end.add(off1.multiply(-1.0)), argbColor);
        drawLine(entry, consumer, start.add(off2), end.add(off2), argbColor);
        drawLine(entry, consumer, start.add(off2.multiply(-1.0)), end.add(off2.multiply(-1.0)), argbColor);
        drawLine(entry, consumer, start.add(off1).add(off2), end.add(off1).add(off2), argbColor);
        drawLine(entry, consumer, start.add(off1).add(off2.multiply(-1.0)), end.add(off1).add(off2.multiply(-1.0)), argbColor);
        drawLine(entry, consumer, start.add(off1.multiply(-1.0)).add(off2), end.add(off1.multiply(-1.0)).add(off2), argbColor);
        drawLine(entry, consumer, start.add(off1.multiply(-1.0)).add(off2.multiply(-1.0)), end.add(off1.multiply(-1.0)).add(off2.multiply(-1.0)), argbColor);
    }

    private static void drawThickBox(MatrixStack.Entry entry, VertexConsumer consumer, Box box, float r, float g, float b, Vec3d viewDir) {
        int argbColor = 0xFF000000 | (((int) Math.round(r * 255.0)) << 16) | (((int) Math.round(g * 255.0)) << 8) | (int) Math.round(b * 255.0);

        Vec3d min = new Vec3d(box.minX, box.minY, box.minZ);
        Vec3d max = new Vec3d(box.maxX, box.maxY, box.maxZ);
        Vec3d v000 = new Vec3d(min.x, min.y, min.z);
        Vec3d v001 = new Vec3d(min.x, min.y, max.z);
        Vec3d v010 = new Vec3d(min.x, max.y, min.z);
        Vec3d v011 = new Vec3d(min.x, max.y, max.z);
        Vec3d v100 = new Vec3d(max.x, min.y, min.z);
        Vec3d v101 = new Vec3d(max.x, min.y, max.z);
        Vec3d v110 = new Vec3d(max.x, max.y, min.z);
        Vec3d v111 = new Vec3d(max.x, max.y, max.z);

        drawThickLine(entry, consumer, v000, v100, argbColor, viewDir);
        drawThickLine(entry, consumer, v000, v010, argbColor, viewDir);
        drawThickLine(entry, consumer, v000, v001, argbColor, viewDir);

        drawThickLine(entry, consumer, v111, v011, argbColor, viewDir);
        drawThickLine(entry, consumer, v111, v101, argbColor, viewDir);
        drawThickLine(entry, consumer, v111, v110, argbColor, viewDir);

        drawThickLine(entry, consumer, v100, v101, argbColor, viewDir);
        drawThickLine(entry, consumer, v100, v110, argbColor, viewDir);
        drawThickLine(entry, consumer, v010, v011, argbColor, viewDir);
        drawThickLine(entry, consumer, v010, v110, argbColor, viewDir);
        drawThickLine(entry, consumer, v001, v101, argbColor, viewDir);
        drawThickLine(entry, consumer, v001, v011, argbColor, viewDir);
    }

    private static int distanceColor(int baseColor, double distance) {
        if (!TrevorAddonsClient.CONFIG.tracerDistanceBlackening) {
            return 0xFF000000 | (baseColor & 0xFFFFFF);
        }
        double t = clamp01(distance / DISTANCE_BLACK_AT);
        return lerpColor(baseColor, DISTANCE_BLACK, t);
    }

    private static int lerpColor(int a, int b, double t) {
        t = clamp01(t);
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int rr = (int) Math.round(ar + (br - ar) * t);
        int rg = (int) Math.round(ag + (bg - ag) * t);
        int rb = (int) Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (rr << 16) | (rg << 8) | rb;
    }

    private static double clamp01(double value) {
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private static Vec3d cross(Vec3d a, Vec3d b) {
        return new Vec3d(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
        );
    }

    private static double lengthSquared(Vec3d value) {
        return value.x * value.x + value.y * value.y + value.z * value.z;
    }

    private static Vec3d normalize(Vec3d value) {
        double length = Math.sqrt(lengthSquared(value));
        if (length < 1.0e-10) {
            return new Vec3d(0.0, 0.0, 0.0);
        }
        return new Vec3d(value.x / length, value.y / length, value.z / length);
    }
}
