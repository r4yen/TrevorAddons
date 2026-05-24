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
                drawLine(entry, lineConsumer, tracerStart, entityCenter, TrevorAddonsClient.CONFIG.tracerLineColor);
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
}
