package cc.rayen.trevoraddons.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.Locale;

public class TrevorSettingsScreen extends Screen {
    private static final int WINDOW_W = 760;
    private static final int WINDOW_H = 430;
    private static final int COLOR_PICKER_STEP = 4;
    private static final double MIN_TRACER_LINE_WIDTH = 0.02;
    private static final double MAX_TRACER_LINE_WIDTH = 48.0;
    private static final double TRACER_WIDTH_CURVE = 3.4;

    private enum Page {
        VISUALS,
        PRESETS
    }

    private final Screen parent;

    private Page page = Page.VISUALS;
    private Rect tabVisualsRect = Rect.empty();
    private Rect tabPresetsRect = Rect.empty();
    private Rect presetCreateRect = Rect.empty();
    private Rect closeRect = Rect.empty();
    private Rect visualDetectionCard = Rect.empty();
    private Rect visualAppearanceCard = Rect.empty();
    private Rect presetInfoCard = Rect.empty();
    private Rect espToggleRect = Rect.empty();
    private Rect tracerToggleRect = Rect.empty();
    private Rect distanceBlackeningToggleRect = Rect.empty();
    private Rect thicknessTrackRect = Rect.empty();
    private Rect svRect = Rect.empty();
    private Rect hueRect = Rect.empty();

    private boolean draggingThickness = false;
    private boolean draggingSv = false;
    private boolean draggingHue = false;
    private boolean configDirty = false;

    private final TrevorPresetEditorScreen presetEditor;
    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float value = 1.0f;
    private String statusMessage = "";

    public TrevorSettingsScreen(Screen parent) {
        super(Text.literal("TrevorAddons Settings"));
        this.parent = parent;
        this.presetEditor = new TrevorPresetEditorScreen(this, true);
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
        int accentMuted = scaleRgb(accentColor, 0.78f);
        int accentDark = scaleRgb(accentColor, 0.46f);

        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE0171B22);
        context.fill(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, 0xFF11151B);
        context.fill(panelLeft, panelTop, panelLeft + 4, panelBottom, accentColor);

        context.drawText(this.textRenderer, Text.literal("TrevorAddons").styled(s -> s.withBold(true)), panelLeft + 16, panelTop + 14, accentColor, false);
        context.drawText(this.textRenderer, Text.literal("Client settings"), panelLeft + 16, panelTop + 30, 0xFF9AA3AF, false);
        drawChip(context, panelRight - 188, panelTop + 14, 172, 20, trim("Active: " + TrevorAddonsClient.CONFIG.getActivePresetName(), 160), accentMuted);

        drawTab(context, tabVisualsRect, "General", page == Page.VISUALS, mouseX, mouseY, accentColor);
        drawTab(context, tabPresetsRect, "Presets", page == Page.PRESETS, mouseX, mouseY, accentColor);
        if (page == Page.PRESETS) {
            drawSmallButton(context, presetCreateRect, "+", mouseX, mouseY, accentMuted, accentDark);
        }

        if (page == Page.VISUALS) {
            drawVisualsPage(context, mouseX, mouseY);
        } else {
            syncEmbeddedPresetEditor();
            presetEditor.render(context, mouseX, mouseY, deltaTicks);
        }

        if (!statusMessage.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal(trim(statusMessage, WINDOW_W - 32)), panelLeft + 16, panelBottom - 18, 0xFFE4EAF2, false);
        }

        drawButton(context, closeRect, "Close", mouseX, mouseY, accentMuted, accentDark);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (click.button() != 0) return super.mouseClicked(click, doubleClick);

        double mouseX = click.x();
        double mouseY = click.y();

        if (tabVisualsRect.contains(mouseX, mouseY)) {
            page = Page.VISUALS;
            statusMessage = "";
            return true;
        }
        if (tabPresetsRect.contains(mouseX, mouseY)) {
            page = Page.PRESETS;
            statusMessage = "";
            return true;
        }
        if (page == Page.PRESETS && presetCreateRect.contains(mouseX, mouseY)) {
            syncEmbeddedPresetEditor();
            presetEditor.createPresetFromSettings();
            statusMessage = "";
            return true;
        }
        if (closeRect.contains(mouseX, mouseY)) {
            close();
            return true;
        }

        if (page == Page.VISUALS) {
            return handleVisualsClick(mouseX, mouseY);
        }
        syncEmbeddedPresetEditor();
        return presetEditor.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (click.button() != 0) return super.mouseDragged(click, deltaX, deltaY);
        if (page == Page.VISUALS) {
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
        }
        syncEmbeddedPresetEditor();
        return page == Page.PRESETS && presetEditor.mouseDragged(click, deltaX, deltaY) ? true : super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingThickness = false;
        draggingSv = false;
        draggingHue = false;
        setDragging(false);
        flushConfigIfDirty();
        syncEmbeddedPresetEditor();
        return page == Page.PRESETS && presetEditor.mouseReleased(click) ? true : super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (page == Page.PRESETS) {
            syncEmbeddedPresetEditor();
            return presetEditor.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == 256) {
            close();
            return true;
        }
        syncEmbeddedPresetEditor();
        if (page == Page.PRESETS && presetEditor.keyPressed(input)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        syncEmbeddedPresetEditor();
        if (page == Page.PRESETS && presetEditor.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        flushConfigIfDirty();
        this.client.setScreen(parent);
    }

    private boolean handleVisualsClick(double mouseX, double mouseY) {
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
        if (distanceBlackeningToggleRect.contains(mouseX, mouseY)) {
            TrevorAddonsClient.CONFIG.tracerDistanceBlackening = !TrevorAddonsClient.CONFIG.tracerDistanceBlackening;
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
        return false;
    }

    private void drawVisualsPage(DrawContext context, int mouseX, int mouseY) {
        drawCard(context, visualDetectionCard, "Visuals", "Toggle boxes, tracers, and distance shading.");
        drawCard(context, visualAppearanceCard, "Appearance", "Adjust tracer color and thickness.");

        context.drawText(this.textRenderer, Text.literal("Visuals"), visualDetectionCard.x + 12, visualDetectionCard.y + 12, 0xFFEAF0F7, false);
        context.drawText(this.textRenderer, Text.literal("Appearance"), visualAppearanceCard.x + 12, visualAppearanceCard.y + 12, 0xFFEAF0F7, false);

        drawToggle(context, espToggleRect, "Trevor Animals ESP", TrevorAddonsClient.CONFIG.markTrevorAnimals, mouseX, mouseY);
        drawToggle(context, tracerToggleRect, "Trevor Animals Tracer", TrevorAddonsClient.CONFIG.lineToTrevorAnimals, mouseX, mouseY);
        drawToggle(context, distanceBlackeningToggleRect, "Distance blackening", TrevorAddonsClient.CONFIG.tracerDistanceBlackening, mouseX, mouseY);

        context.drawText(this.textRenderer, Text.literal("Line thickness"), thicknessTrackRect.x, thicknessTrackRect.y - 14, 0xFFD5DBE5, false);
        drawThicknessSlider(context, mouseX, mouseY);

        context.drawText(this.textRenderer, Text.literal("Tracer color"), svRect.x, svRect.y - 14, 0xFFD5DBE5, false);
        drawColorPicker(context);
        context.drawText(this.textRenderer, Text.literal(hexColor()), svRect.x + svRect.w + 24, svRect.y + 6, 0xFFC4CCD9, false);
        context.fill(svRect.x + svRect.w + 24, svRect.y + 24, svRect.x + svRect.w + 96, svRect.y + 48, 0xFF000000);
        context.fill(svRect.x + svRect.w + 25, svRect.y + 25, svRect.x + svRect.w + 95, svRect.y + 47, 0xFF000000 | (TrevorAddonsClient.CONFIG.tracerLineColor & 0xFFFFFF));
    }

    private void updateLayout() {
        int panelLeft = this.width / 2 - WINDOW_W / 2;
        int panelTop = this.height / 2 - WINDOW_H / 2;

        tabVisualsRect = new Rect(panelLeft + 16, panelTop + 52, 76, 20);
        tabPresetsRect = new Rect(panelLeft + 96, panelTop + 52, 76, 20);
        presetCreateRect = new Rect(panelLeft + 180, panelTop + 52, 20, 20);
        closeRect = new Rect(panelLeft + WINDOW_W - 100, panelTop + WINDOW_H - 28, 88, 18);

        visualDetectionCard = new Rect(panelLeft + 12, panelTop + 84, 240, 164);
        visualAppearanceCard = new Rect(panelLeft + 264, panelTop + 84, 484, 260);
        espToggleRect = new Rect(visualDetectionCard.x + 12, visualDetectionCard.y + 44, visualDetectionCard.w - 24, 24);
        tracerToggleRect = new Rect(visualDetectionCard.x + 12, visualDetectionCard.y + 74, visualDetectionCard.w - 24, 24);
        distanceBlackeningToggleRect = new Rect(visualDetectionCard.x + 12, visualDetectionCard.y + 104, visualDetectionCard.w - 24, 24);
        thicknessTrackRect = new Rect(visualAppearanceCard.x + 16, visualAppearanceCard.y + 54, 190, 16);
        svRect = new Rect(visualAppearanceCard.x + 16, visualAppearanceCard.y + 98, 184, 120);
        hueRect = new Rect(visualAppearanceCard.x + 210, visualAppearanceCard.y + 98, 14, 120);
    }

    private void markConfigDirty() {
        configDirty = true;
    }

    private void syncEmbeddedPresetEditor() {
        presetEditor.syncHost(this.client, this.width, this.height);
    }

    private void flushConfigIfDirty() {
        if (!configDirty) return;
        TrevorAddonsClient.CONFIG.save();
        configDirty = false;
    }

    private void updateThicknessFromMouse(double mouseX) {
        double t = (mouseX - thicknessTrackRect.x) / (double) thicknessTrackRect.w;
        t = clamp01(t);
        double shaped = Math.pow(t, TRACER_WIDTH_CURVE);
        TrevorAddonsClient.CONFIG.tracerLineWidth = Math.round((MIN_TRACER_LINE_WIDTH + (MAX_TRACER_LINE_WIDTH - MIN_TRACER_LINE_WIDTH) * shaped) * 100.0) / 100.0;
        markConfigDirty();
    }

    private void updateSvFromMouse(double mouseX, double mouseY) {
        double sx = (mouseX - svRect.x) / (double) svRect.w;
        double sy = (mouseY - svRect.y) / (double) svRect.h;
        saturation = (float) clamp01(sx);
        value = (float) (1.0 - clamp01(sy));
        applyPickerToConfig();
    }

    private void updateHueFromMouse(double mouseY) {
        double hy = (mouseY - hueRect.y) / (double) hueRect.h;
        hue = (float) clamp01(hy);
        applyPickerToConfig();
    }

    private void applyPickerToConfig() {
        TrevorAddonsClient.CONFIG.tracerLineColor = 0xFF000000 | hsvToRgb(hue, saturation, value);
        markConfigDirty();
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

    private void drawCard(DrawContext context, Rect rect, String title, String subtitle) {
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, 0xFF202631);
        context.fill(rect.x, rect.y, rect.x + 3, rect.y + rect.h, primaryColor());
        context.drawText(this.textRenderer, Text.literal(title), rect.x + 12, rect.y + 12, 0xFFEAF0F7, false);
        drawWrappedText(context, Text.literal(subtitle), rect.x + 12, rect.y + 28, rect.w - 24, 0xFF9AA3AF);
    }

    private void drawTab(DrawContext context, Rect rect, String label, boolean selected, int mouseX, int mouseY, int accentColor) {
        boolean hover = rect.contains(mouseX, mouseY);
        int fill = selected ? accentColor : (hover ? 0xFF2A333F : 0xFF222A34);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, fill);
        context.drawText(this.textRenderer, Text.literal(trim(label, rect.w - 20)), rect.x + 10, rect.y + 5, 0xFFFFFFFF, false);
    }

    private void drawToggle(DrawContext context, Rect rect, String label, boolean value, int mouseX, int mouseY) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? 0xFF2A333F : 0xFF222A34);
        context.drawText(this.textRenderer, Text.literal(trim(label, rect.w - 92)), rect.x + 10, rect.y + 7, 0xFFE7EDF5, false);
        int chipW = 44;
        int chipH = 16;
        int chipX = rect.x + rect.w - chipW - 8;
        int chipY = rect.y + (rect.h - chipH) / 2;
        context.fill(chipX, chipY, chipX + chipW, chipY + chipH, value ? primaryColor() : 0xFF6D3140);
        context.drawText(this.textRenderer, Text.literal(value ? "ON" : "OFF"), chipX + 11, chipY + 4, 0xFFFFFFFF, false);
    }

    private void drawThicknessSlider(DrawContext context, int mouseX, int mouseY) {
        Rect r = thicknessTrackRect;
        context.fill(r.x, r.y + 5, r.x + r.w, r.y + 10, 0xFF2A313D);
        double normalized = (TrevorAddonsClient.CONFIG.tracerLineWidth - MIN_TRACER_LINE_WIDTH) / (MAX_TRACER_LINE_WIDTH - MIN_TRACER_LINE_WIDTH);
        normalized = Math.pow(clamp01(normalized), 1.0 / TRACER_WIDTH_CURVE);
        normalized = clamp01(normalized);
        int knobX = r.x + (int) Math.round(normalized * (r.w - 1));
        int knobColor = r.contains(mouseX, mouseY) || draggingThickness ? primaryColor() : 0xFFD9E2EE;
        context.fill(knobX - 3, r.y + 1, knobX + 4, r.y + 14, knobColor);
        context.drawText(this.textRenderer, Text.literal(formatThickness(TrevorAddonsClient.CONFIG.tracerLineWidth)), r.x + r.w + 12, r.y + 2, 0xFFD5DBE5, false);
    }

    private static String formatThickness(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void drawColorPicker(DrawContext context) {
        for (int y = 0; y < svRect.h; y += COLOR_PICKER_STEP) {
            float rowValue = 1.0f - (y / (float) (svRect.h - 1));
            for (int x = 0; x < svRect.w; x += COLOR_PICKER_STEP) {
                float rowSaturation = x / (float) (svRect.w - 1);
                int rgb = hsvToRgb(hue, rowSaturation, rowValue);
                int rx2 = Math.min(svRect.x + x + COLOR_PICKER_STEP, svRect.x + svRect.w);
                int ry2 = Math.min(svRect.y + y + COLOR_PICKER_STEP, svRect.y + svRect.h);
                context.fill(svRect.x + x, svRect.y + y, rx2, ry2, 0xFF000000 | rgb);
            }
        }

        for (int y = 0; y < hueRect.h; y += COLOR_PICKER_STEP) {
            float h = y / (float) (hueRect.h - 1);
            int rgb = hsvToRgb(h, 1.0f, 1.0f);
            int ry2 = Math.min(hueRect.y + y + COLOR_PICKER_STEP, hueRect.y + hueRect.h);
            context.fill(hueRect.x, hueRect.y + y, hueRect.x + hueRect.w, ry2, 0xFF000000 | rgb);
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

    private void drawButton(DrawContext context, Rect rect, String label, int mouseX, int mouseY, int hoverColor, int baseColor) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? hoverColor : baseColor);
        context.drawText(this.textRenderer, Text.literal(trim(label, rect.w - 16)), rect.x + 8, rect.y + 5, 0xFFFFFFFF, false);
    }

    private void drawSmallButton(DrawContext context, Rect rect, String label, int mouseX, int mouseY, int hoverColor, int baseColor) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? hoverColor : baseColor);
        context.drawText(this.textRenderer, Text.literal(label), rect.x + 6, rect.y + 4, 0xFFFFFFFF, false);
    }

    private void drawChip(DrawContext context, int x, int y, int w, int h, String label, int fill) {
        context.fill(x, y, x + w, y + h, fill);
        context.drawText(this.textRenderer, Text.literal(trim(label, w - 16)), x + 8, y + 6, 0xFFFFFFFF, false);
    }

    private void drawWrappedText(DrawContext context, Text text, int x, int y, int width, int color) {
        int offset = 0;
        for (OrderedText line : this.textRenderer.wrapLines(text, width)) {
            context.drawText(this.textRenderer, line, x, y + offset, color, false);
            offset += 10;
        }
    }

    private String trim(String text, int width) {
        return this.textRenderer.trimToWidth(text, width);
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
