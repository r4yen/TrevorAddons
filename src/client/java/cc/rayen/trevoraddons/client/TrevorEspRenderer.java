package cc.rayen.trevoraddons.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TrevorEspRenderer {
    private static final float BOX_A = 1.0f;
    private static final double TRACER_START_OFFSET = 0.18;
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
        VertexConsumer lineConsumer = renderLines ? consumers.getBuffer(RenderLayer.getDebugLineStrip((float) TrevorAddonsClient.CONFIG.tracerLineWidth)) : null;
        Quaternionf cameraOrientation = context.worldState().cameraRenderState.orientation;
        Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f).rotate(cameraOrientation);
        Vec3d tracerStart = new Vec3d(forward.x, forward.y, forward.z).multiply(TRACER_START_OFFSET);

        for (Integer id : TrevorRuntime.getMarkedEntityIds()) {
            Entity entity = client.world.getEntityById(id);
            if (entity == null || !entity.isAlive() || entity.isRemoved()) continue;
            if (!TrevorRuntime.shouldMarkTrevorAnimal(entity)) continue;

            if (boxConsumer != null) {
                Box box = entity.getBoundingBox().expand(0.08).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                VertexRendering.drawBox(entry, boxConsumer, box, boxR, boxG, boxB, BOX_A);
            }

            if (lineConsumer != null) {
                Vec3d entityCenter = new Vec3d(
                        entity.getX() - cameraPos.x,
                        entity.getY() + (entity.getHeight() * 0.5) - cameraPos.y,
                        entity.getZ() - cameraPos.z
                );
                int lineColor = distanceColor(TrevorAddonsClient.CONFIG.tracerLineColor, entityCenter.length());
                drawLine(entry, lineConsumer, tracerStart, entityCenter, lineColor);
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

    private static int distanceColor(int baseColor, double distance) {
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
}
