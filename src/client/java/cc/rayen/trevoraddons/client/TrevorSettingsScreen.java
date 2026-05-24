package cc.rayen.trevoraddons.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.Locale;

public class TrevorSettingsScreen extends Screen {
    private static final int WINDOW_W = 760;
    private static final int WINDOW_H = 430;

    private final Screen parent;

    private Rect detectionCard = Rect.empty();
    private Rect presetCard = Rect.empty();
    private Rect appearanceCard = Rect.empty();
    private Rect espToggleRect = Rect.empty();
    private Rect tracerToggleRect = Rect.empty();
    private Rect thicknessTrackRect = Rect.empty();
    private Rect svRect = Rect.empty();
    private Rect hueRect = Rect.empty();
    private Rect openPresetsRect = Rect.empty();
    private Rect closeRect = Rect.empty();

    private boolean draggingThickness = false;
    private boolean draggingSv = false;
    private boolean draggingHue = false;
    private boolean configDirty = false;

    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float value = 1.0f;

    public TrevorSettingsScreen(Screen parent) {
        super(Text.literal("TrevorAddons Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        syncPickerFromConfig();
        updateLayout();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        updateLayout();
        renderInGameBackground(context);

        int panelLeft = this.width / 2 - WINDOW_W / 2;
        int panelTop = this.height / 2 - WINDOW_H / 2;
        int panelRight = panelLeft + WINDOW_W;
        int panelBottom = panelTop + WINDOW_H;

        int accentColor = primaryColor();
        int accentSoft = scaleRgb(accentColor, 0.76f);
        int accentDark = scaleRgb(accentColor, 0.46f);
        int frame = 0xE0171B22;
        int card = 0xFF202631;
        int cardHover = 0xFF252D39;

        context.fill(panelLeft, panelTop, panelRight, panelBottom, frame);
        context.fill(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, 0xFF11151B);
        context.fill(panelLeft, panelTop, panelLeft + 4, panelBottom, accentColor);

        context.drawText(this.textRenderer, Text.literal("TrevorAddons").styled(s -> s.withBold(true)), panelLeft + 16, panelTop + 14, accentColor, false);
        context.drawText(this.textRenderer, Text.literal("Client settings"), panelLeft + 16, panelTop + 30, 0xFF9AA3AF, false);
        drawStatusChip(context, panelRight - 188, panelTop + 14, 172, 20, "Active preset: " + TrevorAddonsClient.CONFIG.getActivePresetName(), accentSoft);

        drawCard(context, detectionCard, "Detection", "Toggle boxes and tracers for Trevor animals.", mouseX, mouseY, card, cardHover);
        drawCard(context, presetCard, "Presets", "Open the preset editor to manage entity life values.", mouseX, mouseY, card, cardHover);
        drawCard(context, appearanceCard, "Appearance", "Adjust tracer thickness and choose a color.", mouseX, mouseY, card, cardHover);

        drawToggle(context, espToggleRect, "Trevor Animals ESP", TrevorAddonsClient.CONFIG.markTrevorAnimals, mouseX, mouseY);
        drawToggle(context, tracerToggleRect, "Trevor Animals Tracer", TrevorAddonsClient.CONFIG.lineToTrevorAnimals, mouseX, mouseY);

        context.drawText(this.textRenderer, Text.literal("Tracer thickness"), thicknessTrackRect.x, thicknessTrackRect.y - 14, 0xFFD5DBE5, false);
        drawThicknessSlider(context, mouseX, mouseY);

        context.drawText(this.textRenderer, Text.literal("Tracer color"), svRect.x, svRect.y - 14, 0xFFD5DBE5, false);
        drawColorPicker(context);

        context.fill(svRect.x + svRect.w + 28, svRect.y + 10, svRect.x + svRect.w + 100, svRect.y + 38, 0xFF000000);
        context.fill(svRect.x + svRect.w + 29, svRect.y + 11, svRect.x + svRect.w + 99, svRect.y + 37, 0xFF000000 | (TrevorAddonsClient.CONFIG.tracerLineColor & 0xFFFFFF));
        context.drawText(this.textRenderer, Text.literal(hexColor()), svRect.x + svRect.w + 28, svRect.y + 48, 0xFFC4CCD9, false);
        context.drawText(this.textRenderer, Text.literal("Current preset is stored separately from the visual settings."), svRect.x + svRect.w + 28, svRect.y + 72, 0xFF9AA3AF, false);

        drawActionButton(context, openPresetsRect, "Open Entity Presets", mouseX, mouseY, accentSoft, accentDark);
        drawActionButton(context, closeRect, "Close", mouseX, mouseY, accentSoft, accentDark);

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (click.button() != 0) return super.mouseClicked(click, doubleClick);

        double mouseX = click.x();
        double mouseY = click.y();

        if (espToggleRect.contains(mouseX, mouseY)) {
            TrevorAddonsClient.CONFIG.markTrevorAnimals = !TrevorAddonsClient.CONFIG.markTrevorAnimals;
            markConfigDirty();
            return true;
        }
        if (tracerToggleRect.contains(mouseX, mouseY)) {
            TrevorAddonsClient.CONFIG.lineToTrevorAnimals = !TrevorAddonsClient.CONFIG.lineToTrevorAnimals;
            markConfigDirty();
            return true;
        }
        if (thicknessTrackRect.contains(mouseX, mouseY)) {
            draggingThickness = true;
            setDragging(true);
            updateThicknessFromMouse(mouseX);
            return true;
        }
        if (svRect.contains(mouseX, mouseY)) {
            draggingSv = true;
            setDragging(true);
            updateSvFromMouse(mouseX, mouseY);
            return true;
        }
        if (hueRect.contains(mouseX, mouseY)) {
            draggingHue = true;
            setDragging(true);
            updateHueFromMouse(mouseY);
            return true;
        }
        if (openPresetsRect.contains(mouseX, mouseY)) {
            this.client.setScreen(new TrevorEntityPresetsScreen(this));
            return true;
        }
        if (closeRect.contains(mouseX, mouseY)) {
            close();
            return true;
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (click.button() != 0) return super.mouseDragged(click, deltaX, deltaY);
        if (draggingThickness) {
            updateThicknessFromMouse(click.x());
            return true;
        }
        if (draggingSv) {
            updateSvFromMouse(click.x(), click.y());
            return true;
        }
        if (draggingHue) {
            updateHueFromMouse(click.y());
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingThickness = false;
        draggingSv = false;
        draggingHue = false;
        setDragging(false);
        flushConfigIfDirty();
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == 256) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        flushConfigIfDirty();
        this.client.setScreen(parent);
    }

    private void drawCard(DrawContext context, Rect rect, String title, String subtitle, int mouseX, int mouseY, int baseColor, int hoverColor) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? hoverColor : baseColor);
        context.drawText(this.textRenderer, Text.literal(title), rect.x + 12, rect.y + 12, 0xFFEAF0F7, false);
        context.drawText(this.textRenderer, Text.literal(subtitle), rect.x + 12, rect.y + 28, 0xFF9AA3AF, false);
    }

    private void drawStatusChip(DrawContext context, int x, int y, int w, int h, String label, int fill) {
        context.fill(x, y, x + w, y + h, fill);
        context.drawText(this.textRenderer, Text.literal(label), x + 8, y + 6, 0xFFFFFFFF, false);
    }

    private void drawToggle(DrawContext context, Rect rect, String label, boolean value, int mouseX, int mouseY) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? 0xFF2A333F : 0xFF222A34);
        context.drawText(this.textRenderer, Text.literal(label), rect.x + 10, rect.y + 7, 0xFFE7EDF5, false);

        int chipW = 52;
        int chipH = 16;
        int chipX = rect.x + rect.w - chipW - 10;
        int chipY = rect.y + (rect.h - chipH) / 2;
        context.fill(chipX, chipY, chipX + chipW, chipY + chipH, value ? primaryColor() : 0xFF6D3140);
        context.drawText(this.textRenderer, Text.literal(value ? "ON" : "OFF"), chipX + 15, chipY + 4, 0xFFFFFFFF, false);
    }

    private void drawThicknessSlider(DrawContext context, int mouseX, int mouseY) {
        Rect r = thicknessTrackRect;
        context.fill(r.x, r.y + 5, r.x + r.w, r.y + 10, 0xFF2A313D);

        double normalized = (TrevorAddonsClient.CONFIG.tracerLineWidth - 1.0) / 9.0;
        normalized = clamp01(normalized);
        int knobX = r.x + (int) Math.round(normalized * (r.w - 1));
        int knobColor = r.contains(mouseX, mouseY) || draggingThickness ? primaryColor() : 0xFFD9E2EE;
        context.fill(knobX - 3, r.y + 1, knobX + 4, r.y + 14, knobColor);

        context.drawText(this.textRenderer,
                Text.literal(String.format(Locale.ROOT, "%.1f", TrevorAddonsClient.CONFIG.tracerLineWidth)),
                r.x + r.w + 12, r.y + 2, 0xFFD5DBE5, false);
    }

    private void drawColorPicker(DrawContext context) {
        final int step = 2;
        for (int y = 0; y < svRect.h; y += step) {
            float rowValue = 1.0f - (y / (float) (svRect.h - 1));
            for (int x = 0; x < svRect.w; x += step) {
                float rowSaturation = x / (float) (svRect.w - 1);
                int rgb = hsvToRgb(hue, rowSaturation, rowValue);
                int rx2 = Math.min(svRect.x + x + step, svRect.x + svRect.w);
                int ry2 = Math.min(svRect.y + y + step, svRect.y + svRect.h);
                context.fill(svRect.x + x, svRect.y + y, rx2, ry2, 0xFF000000 | rgb);
            }
        }

        for (int y = 0; y < hueRect.h; y++) {
            float h = y / (float) (hueRect.h - 1);
            int rgb = hsvToRgb(h, 1.0f, 1.0f);
            context.fill(hueRect.x, hueRect.y + y, hueRect.x + hueRect.w, hueRect.y + y + 1, 0xFF000000 | rgb);
        }

        int svX = svRect.x + Math.round(saturation * (svRect.w - 1));
        int svY = svRect.y + Math.round((1.0f - value) * (svRect.h - 1));
        context.fill(svX - 3, svY, svX + 4, svY + 1, 0xFFFFFFFF);
        context.fill(svX, svY - 3, svX + 1, svY + 4, 0xFFFFFFFF);
        context.fill(svX - 2, svY, svX + 3, svY + 1, 0xFF000000);
        context.fill(svX, svY - 2, svX + 1, svY + 3, 0xFF000000);

        int hueY = hueRect.y + Math.round(hue * (hueRect.h - 1));
        context.fill(hueRect.x - 2, hueY - 1, hueRect.x + hueRect.w + 2, hueY + 2, 0xFFFFFFFF);
        context.fill(hueRect.x - 1, hueY, hueRect.x + hueRect.w + 1, hueY + 1, 0xFF000000);
    }

    private void drawActionButton(DrawContext context, Rect rect, String label, int mouseX, int mouseY, int hoverColor, int baseColor) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? hoverColor : baseColor);
        context.drawText(this.textRenderer, Text.literal(label), rect.x + 10, rect.y + 5, 0xFFFFFFFF, false);
    }

    private void updateThicknessFromMouse(double mouseX) {
        double t = (mouseX - thicknessTrackRect.x) / thicknessTrackRect.w;
        t = clamp01(t);
        double width = 1.0 + (9.0 * t);
        TrevorAddonsClient.CONFIG.tracerLineWidth = Math.round(width * 10.0) / 10.0;
        markConfigDirty();
    }

    private void updateSvFromMouse(double mouseX, double mouseY) {
        double sx = (mouseX - svRect.x) / svRect.w;
        double sy = (mouseY - svRect.y) / svRect.h;
        saturation = (float) clamp01(sx);
        value = (float) (1.0 - clamp01(sy));
        applyPickerToConfig();
    }

    private void updateHueFromMouse(double mouseY) {
        double hy = (mouseY - hueRect.y) / hueRect.h;
        hue = (float) clamp01(hy);
        applyPickerToConfig();
    }

    private void applyPickerToConfig() {
        TrevorAddonsClient.CONFIG.tracerLineColor = 0xFF000000 | hsvToRgb(hue, saturation, value);
        markConfigDirty();
    }

    private void markConfigDirty() {
        configDirty = true;
    }

    private void flushConfigIfDirty() {
        if (!configDirty) return;
        TrevorAddonsClient.CONFIG.save();
        configDirty = false;
    }

    private void syncPickerFromConfig() {
        int color = TrevorAddonsClient.CONFIG.tracerLineColor & 0xFFFFFF;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        if (delta == 0.0f) {
            hue = 0.0f;
        } else if (max == r) {
            hue = ((g - b) / delta) % 6.0f;
            hue /= 6.0f;
            if (hue < 0.0f) hue += 1.0f;
        } else if (max == g) {
            hue = ((b - r) / delta + 2.0f) / 6.0f;
        } else {
            hue = ((r - g) / delta + 4.0f) / 6.0f;
        }

        saturation = max == 0.0f ? 0.0f : (delta / max);
        value = max;
    }

    private void updateLayout() {
        int panelLeft = this.width / 2 - WINDOW_W / 2;
        int panelTop = this.height / 2 - WINDOW_H / 2;

        detectionCard = new Rect(panelLeft + 12, panelTop + 56, 230, 128);
        presetCard = new Rect(panelLeft + 12, panelTop + 192, 230, 110);
        appearanceCard = new Rect(panelLeft + 254, panelTop + 56, 494, 246);

        espToggleRect = new Rect(detectionCard.x + 12, detectionCard.y + 48, detectionCard.w - 24, 24);
        tracerToggleRect = new Rect(detectionCard.x + 12, detectionCard.y + 78, detectionCard.w - 24, 24);

        thicknessTrackRect = new Rect(appearanceCard.x + 16, appearanceCard.y + 58, 190, 16);
        svRect = new Rect(appearanceCard.x + 16, appearanceCard.y + 98, 184, 120);
        hueRect = new Rect(appearanceCard.x + 210, appearanceCard.y + 98, 14, 120);

        openPresetsRect = new Rect(presetCard.x + 12, presetCard.y + 60, presetCard.w - 24, 22);
        closeRect = new Rect(panelLeft + WINDOW_W - 114, panelTop + WINDOW_H - 32, 102, 20);
    }

    private String hexColor() {
        return String.format(Locale.ROOT, "#%06X", TrevorAddonsClient.CONFIG.tracerLineColor & 0xFFFFFF);
    }

    private static int primaryColor() {
        return 0xFF000000 | (TrevorAddonsClient.CONFIG.tracerLineColor & 0xFFFFFF);
    }

    private static int scaleRgb(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r = Math.max(0, Math.min(255, Math.round(r * factor)));
        g = Math.max(0, Math.min(255, Math.round(g * factor)));
        b = Math.max(0, Math.min(255, Math.round(b * factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        float c = value * saturation;
        float x = c * (1.0f - Math.abs((h % 2.0f) - 1.0f));
        float m = value - c;

        float r;
        float g;
        float b;
        if (h < 1.0f) {
            r = c;
            g = x;
            b = 0.0f;
        } else if (h < 2.0f) {
            r = x;
            g = c;
            b = 0.0f;
        } else if (h < 3.0f) {
            r = 0.0f;
            g = c;
            b = x;
        } else if (h < 4.0f) {
            r = 0.0f;
            g = x;
            b = c;
        } else if (h < 5.0f) {
            r = x;
            g = 0.0f;
            b = c;
        } else {
            r = c;
            g = 0.0f;
            b = x;
        }

        int ri = (int) ((r + m) * 255.0f);
        int gi = (int) ((g + m) * 255.0f);
        int bi = (int) ((b + m) * 255.0f);
        return (ri << 16) | (gi << 8) | bi;
    }

    private record Rect(int x, int y, int w, int h) {
        static Rect empty() {
            return new Rect(0, 0, 0, 0);
        }

        boolean contains(double px, double py) {
            return px >= x && py >= y && px < (x + w) && py < (y + h);
        }
    }
}
