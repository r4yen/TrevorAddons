package cc.rayen.trevoraddons.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrevorEntityPresetsScreen extends Screen {
    private static final int PRESETS_PER_PAGE = 6;
    private static final String[] ENTITY_KEYS = {
            TrevorConfig.HORSE_FAMILY_KEY,
            "minecraft:cow",
            "minecraft:pig",
            "minecraft:sheep",
            "minecraft:rabbit",
            "minecraft:chicken"
    };

    private static final String[] ENTITY_LABELS = {
            "Horse Family",
            "Cow",
            "Pig",
            "Sheep",
            "Rabbit",
            "Chicken"
    };

    private final Screen parent;

    private Rect backRect = Rect.empty();
    private Rect useRect = Rect.empty();
    private Rect newRect = Rect.empty();
    private Rect deleteRect = Rect.empty();
    private Rect prevPageRect = Rect.empty();
    private Rect nextPageRect = Rect.empty();
    private Rect lifeFieldRect = Rect.empty();
    private Rect applyRect = Rect.empty();
    private Rect resetRect = Rect.empty();
    private Rect[] presetRows = new Rect[0];
    private Rect[] entityRows = new Rect[0];

    private int selectedPresetIndex = 0;
    private int selectedEntityIndex = 0;
    private int presetPage = 0;
    private boolean lifeFieldFocused = false;
    private String lifeInput = "";
    private String statusMessage = "";

    public TrevorEntityPresetsScreen(Screen parent) {
        super(Text.literal("TrevorAddons Entity Presets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        syncSelectionToConfig();
        updateLayout();
        syncLifeInputFromSelection();
    }

    private void updateLayout() {
        if (TrevorAddonsClient.CONFIG == null) {
            return;
        }
        if (TrevorAddonsClient.CONFIG.presets.isEmpty()) {
            selectedPresetIndex = 0;
            selectedEntityIndex = 0;
            presetPage = 0;
            return;
        }

        selectedPresetIndex = Math.max(0, Math.min(selectedPresetIndex, TrevorAddonsClient.CONFIG.presets.size() - 1));
        int maxPage = Math.max(0, (TrevorAddonsClient.CONFIG.presets.size() - 1) / PRESETS_PER_PAGE);
        presetPage = Math.max(0, Math.min(presetPage, maxPage));
        clampSelectedPresetToPage();
        selectedEntityIndex = Math.max(0, Math.min(selectedEntityIndex, ENTITY_KEYS.length - 1));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        updateLayout();
        renderInGameBackground(context);

        int panelLeft = this.width / 2 - 310;
        int panelTop = this.height / 2 - 215;
        int panelRight = panelLeft + 620;
        int panelBottom = panelTop + 430;
        int accentColor = TrevorAddonsClient.CONFIG.tracerLineColor;
        int accentMuted = scaleRgb(accentColor, 0.72f);
        int accentDark = scaleRgb(accentColor, 0.50f);

        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE014171D);
        context.fill(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, 0xEF1A1F27);
        context.fill(panelLeft, panelTop, panelLeft + 3, panelBottom, accentColor);

        context.drawText(this.textRenderer, Text.literal("TrevorAddons").styled(s -> s.withBold(true)), panelLeft + 12, panelTop + 10, accentColor, false);
        context.drawText(this.textRenderer, Text.literal("Entity Presets"), panelLeft + 12, panelTop + 24, 0xFF9FA7B3, false);
        context.drawText(this.textRenderer, Text.literal("Preset " + (selectedPreset().editable ? "editable" : "locked") + " and active preset selection"), panelLeft + 12, panelTop + 38, 0xFF9FA7B3, false);

        drawPresetColumn(context, mouseX, mouseY);
        drawEntityColumn(context, mouseX, mouseY);
        drawEditor(context, mouseX, mouseY);
        drawFooter(context, mouseX, mouseY, accentMuted, accentDark);

        if (!statusMessage.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal(statusMessage), panelLeft + 12, panelBottom - 14, 0xFFE4EAF2, false);
        }

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (click.button() != 0) return super.mouseClicked(click, doubleClick);

        double mouseX = click.x();
        double mouseY = click.y();

        if (backRect.contains(mouseX, mouseY)) {
            close();
            return true;
        }
        if (useRect.contains(mouseX, mouseY)) {
            TrevorAddonsClient.CONFIG.setActivePreset(selectedPreset().id);
            TrevorAddonsClient.CONFIG.save();
            statusMessage = "Active preset updated.";
            return true;
        }
        if (newRect.contains(mouseX, mouseY)) {
            TrevorConfig.Preset copy = TrevorAddonsClient.CONFIG.duplicatePreset(selectedPreset().id);
            TrevorAddonsClient.CONFIG.save();
            syncSelectionToConfig();
            selectPresetById(copy.id);
            syncLifeInputFromSelection();
            statusMessage = "Created " + copy.name + ".";
            return true;
        }
        if (deleteRect.contains(mouseX, mouseY)) {
            if (!selectedPreset().editable) {
                statusMessage = "Default preset cannot be deleted.";
                return true;
            }
            String removedId = selectedPreset().id;
            TrevorAddonsClient.CONFIG.deletePreset(removedId);
            TrevorAddonsClient.CONFIG.save();
            syncSelectionToConfig();
            syncLifeInputFromSelection();
            statusMessage = "Preset deleted.";
            return true;
        }
        if (prevPageRect.contains(mouseX, mouseY)) {
            if (presetPage > 0) {
                presetPage--;
                clampSelectedPresetToPage();
            }
            return true;
        }
        if (nextPageRect.contains(mouseX, mouseY)) {
            int maxPage = Math.max(0, (TrevorAddonsClient.CONFIG.presets.size() - 1) / PRESETS_PER_PAGE);
            if (presetPage < maxPage) {
                presetPage++;
                clampSelectedPresetToPage();
            }
            return true;
        }
        if (lifeFieldRect.contains(mouseX, mouseY)) {
            lifeFieldFocused = true;
            return true;
        }
        if (applyRect.contains(mouseX, mouseY)) {
            applyLifeInput();
            return true;
        }
        if (resetRect.contains(mouseX, mouseY)) {
            resetSelectedEntityToDefault();
            return true;
        }

        for (int i = 0; i < presetRows.length; i++) {
            Rect row = presetRows[i];
            if (row.contains(mouseX, mouseY)) {
                selectedPresetIndex = Math.min(TrevorAddonsClient.CONFIG.presets.size() - 1, presetPage * PRESETS_PER_PAGE + i);
                selectedEntityIndex = 0;
                lifeFieldFocused = false;
                syncLifeInputFromSelection();
                statusMessage = "";
                return true;
            }
        }

        for (int i = 0; i < entityRows.length; i++) {
            Rect row = entityRows[i];
            if (row.contains(mouseX, mouseY)) {
                selectedEntityIndex = i;
                lifeFieldFocused = false;
                syncLifeInputFromSelection();
                statusMessage = "";
                return true;
            }
        }

        lifeFieldFocused = false;
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == 256) { // ESC
            close();
            return true;
        }
        if (lifeFieldFocused) {
            if (input.getKeycode() == 257 || input.getKeycode() == 335) { // ENTER / KP_ENTER
                applyLifeInput();
                return true;
            }
            if (input.getKeycode() == 259) { // BACKSPACE
                if (!lifeInput.isEmpty()) {
                    lifeInput = lifeInput.substring(0, lifeInput.length() - 1);
                }
                return true;
            }
            if (input.getKeycode() == 261) { // DELETE
                lifeInput = "";
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!lifeFieldFocused) {
            return super.charTyped(input);
        }
        char chr = (char) input.codepoint();
        if (isAllowedLifeChar(chr) && lifeInput.length() < 64) {
            lifeInput += chr;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        TrevorAddonsClient.CONFIG.save();
        this.client.setScreen(parent);
    }

    private void drawPresetColumn(DrawContext context, int mouseX, int mouseY) {
        int left = this.width / 2 - 310 + 12;
        int top = this.height / 2 - 215 + 56;
        int width = 180;

        context.drawText(this.textRenderer, Text.literal("Presets"), left, top - 16, 0xFFD5DBE5, false);

        int start = presetPage * PRESETS_PER_PAGE;
        int end = Math.min(TrevorAddonsClient.CONFIG.presets.size(), start + PRESETS_PER_PAGE);
        presetRows = new Rect[end - start];
        for (int i = start; i < end; i++) {
            int rowY = top + ((i - start) * 28);
            Rect row = new Rect(left, rowY, width, 22);
            presetRows[i - start] = row;

            boolean selected = i == selectedPresetIndex;
            boolean active = TrevorAddonsClient.CONFIG.presets.get(i).id.equals(TrevorAddonsClient.CONFIG.activePresetId);
            int fill = selected ? 0xFF35404E : 0xFF262D38;
            if (active) {
                fill = 0xFF313B2F;
            }
            if (row.contains(mouseX, mouseY)) {
                fill = scaleRgb(fill, 1.18f);
            }

            context.fill(row.x, row.y, row.x + row.w, row.y + row.h, fill);
            context.drawText(this.textRenderer, Text.literal(TrevorAddonsClient.CONFIG.presets.get(i).name), row.x + 8, row.y + 6, 0xFFE4EAF2, false);
            context.drawText(this.textRenderer,
                    Text.literal(active ? "Active" : (TrevorAddonsClient.CONFIG.presets.get(i).editable ? "Edit" : "Locked")),
                    row.x + row.w - 50, row.y + 6, active ? 0xFFB9F2C0 : 0xFF9FA7B3, false);
        }

        prevPageRect = new Rect(left, top + 6 + (PRESETS_PER_PAGE * 28), 84, 20);
        nextPageRect = new Rect(left + 96, top + 6 + (PRESETS_PER_PAGE * 28), 84, 20);
        drawSmallButton(context, prevPageRect, "<", mouseX, mouseY);
        drawSmallButton(context, nextPageRect, ">", mouseX, mouseY);

        context.drawText(this.textRenderer,
                Text.literal((presetPage + 1) + "/" + Math.max(1, ((TrevorAddonsClient.CONFIG.presets.size() - 1) / PRESETS_PER_PAGE) + 1)),
                left + 35, top + 6 + (PRESETS_PER_PAGE * 28) + 3, 0xFF9FA7B3, false);
    }

    private void drawEntityColumn(DrawContext context, int mouseX, int mouseY) {
        int left = this.width / 2 - 310 + 214;
        int top = this.height / 2 - 215 + 56;
        int width = 180;

        context.drawText(this.textRenderer, Text.literal("Entities"), left, top - 16, 0xFFD5DBE5, false);

        entityRows = new Rect[ENTITY_KEYS.length];
        for (int i = 0; i < ENTITY_KEYS.length; i++) {
            int rowY = top + (i * 28);
            Rect row = new Rect(left, rowY, width, 22);
            entityRows[i] = row;

            boolean selected = i == selectedEntityIndex;
            int fill = selected ? 0xFF35404E : 0xFF262D38;
            if (row.contains(mouseX, mouseY)) {
                fill = scaleRgb(fill, 1.12f);
            }

            context.fill(row.x, row.y, row.x + row.w, row.y + row.h, fill);
            context.drawText(this.textRenderer, Text.literal(ENTITY_LABELS[i]), row.x + 8, row.y + 6, 0xFFE4EAF2, false);
            context.drawText(this.textRenderer,
                    Text.literal(formatLives(selectedPreset().getRule(ENTITY_KEYS[i]).healthValues)),
                    row.x + row.w - 108, row.y + 6, 0xFF9FA7B3, false);
        }
    }

    private void drawEditor(DrawContext context, int mouseX, int mouseY) {
        int left = this.width / 2 - 310 + 214;
        int top = this.height / 2 - 215 + 250;
        int width = 180;
        TrevorConfig.Preset preset = selectedPreset();
        TrevorConfig.EntityRule rule = selectedRule();

        context.drawText(this.textRenderer, Text.literal("Editor"), left, top - 16, 0xFFD5DBE5, false);
        context.drawText(this.textRenderer, Text.literal(ENTITY_LABELS[selectedEntityIndex] + " lives"), left, top, 0xFFE4EAF2, false);
        context.drawText(this.textRenderer, Text.literal(preset.editable ? "Editable preset" : "Default preset is locked"), left, top + 12, 0xFF9FA7B3, false);

        lifeFieldRect = new Rect(left, top + 30, width, 22);
        boolean hover = lifeFieldRect.contains(mouseX, mouseY);
        context.fill(lifeFieldRect.x, lifeFieldRect.y, lifeFieldRect.x + lifeFieldRect.w, lifeFieldRect.y + lifeFieldRect.h, hover || lifeFieldFocused ? 0xFF313B49 : 0xFF262D38);
        String text = lifeInput.isEmpty() ? "Add comma-separated values" : lifeInput;
        context.drawText(this.textRenderer, Text.literal(text), lifeFieldRect.x + 8, lifeFieldRect.y + 6, lifeInput.isEmpty() ? 0xFF758197 : 0xFFE4EAF2, false);

        applyRect = new Rect(left, top + 58, 84, 20);
        resetRect = new Rect(left + 96, top + 58, 84, 20);
        drawSmallButton(context, applyRect, "Apply", mouseX, mouseY);
        drawSmallButton(context, resetRect, "Reset", mouseX, mouseY);

        if (!preset.editable) {
            context.drawText(this.textRenderer, Text.literal("The Default preset cannot be edited."), left, top + 86, 0xFF9FA7B3, false);
        } else if (rule.healthValues.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal("No life values set."), left, top + 86, 0xFF9FA7B3, false);
        }
    }

    private void drawFooter(DrawContext context, int mouseX, int mouseY, int hoverColor, int baseColor) {
        int left = this.width / 2 - 310 + 12;
        int bottom = this.height / 2 + 215 - 34;

        useRect = new Rect(left, bottom, 116, 20);
        newRect = new Rect(left + 126, bottom, 116, 20);
        deleteRect = new Rect(left + 252, bottom, 116, 20);
        backRect = new Rect(left + 388, bottom, 120, 20);

        drawActionButton(context, useRect, "Use Selected", mouseX, mouseY, hoverColor, baseColor);
        drawActionButton(context, newRect, "New Preset", mouseX, mouseY, hoverColor, baseColor);
        drawActionButton(context, deleteRect, "Delete", mouseX, mouseY, hoverColor, baseColor);
        drawActionButton(context, backRect, "Back", mouseX, mouseY, hoverColor, baseColor);

        context.drawText(this.textRenderer,
                Text.literal("Page " + (presetPage + 1)),
                left + 420, bottom + 5, 0xFF9FA7B3, false);
    }

    private void drawSmallButton(DrawContext context, Rect rect, String label, int mouseX, int mouseY) {
        drawActionButton(context, rect, label, mouseX, mouseY, 0xFF3A4658, 0xFF2A323F);
    }

    private void drawActionButton(DrawContext context, Rect rect, String label, int mouseX, int mouseY, int hoverColor, int baseColor) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? hoverColor : baseColor);
        context.drawText(this.textRenderer, Text.literal(label), rect.x + 8, rect.y + 5, 0xFFFFFFFF, false);
    }

    private void applyLifeInput() {
        TrevorConfig.Preset preset = selectedPreset();
        TrevorConfig.EntityRule rule = selectedRule();
        if (!preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }

        List<Double> parsed = parseLifeValues(lifeInput);
        if (parsed.isEmpty()) {
            statusMessage = "Enter at least one numeric value.";
            return;
        }

        rule.healthValues = parsed;
        TrevorAddonsClient.CONFIG.save();
        lifeInput = formatLives(parsed);
        statusMessage = ENTITY_LABELS[selectedEntityIndex] + " updated.";
    }

    private void resetSelectedEntityToDefault() {
        TrevorConfig.Preset preset = selectedPreset();
        if (!preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }

        TrevorConfig.EntityRule defaultRule = TrevorConfig.createDefaultPreset().getRule(ENTITY_KEYS[selectedEntityIndex]);
        TrevorConfig.EntityRule rule = selectedRule();
        rule.healthValues = new ArrayList<>(defaultRule.healthValues);
        TrevorAddonsClient.CONFIG.save();
        syncLifeInputFromSelection();
        statusMessage = ENTITY_LABELS[selectedEntityIndex] + " reset.";
    }

    private void syncSelectionToConfig() {
        selectedPresetIndex = Math.max(0, indexOfPreset(TrevorAddonsClient.CONFIG.activePresetId));
        presetPage = selectedPresetIndex / PRESETS_PER_PAGE;
        clampSelectedPresetToPage();
        selectedEntityIndex = Math.max(0, Math.min(selectedEntityIndex, ENTITY_KEYS.length - 1));
    }

    private void syncLifeInputFromSelection() {
        lifeInput = formatLives(selectedRule().healthValues);
    }

    private void clampSelectedPresetToPage() {
        int start = presetPage * PRESETS_PER_PAGE;
        int end = Math.min(TrevorAddonsClient.CONFIG.presets.size() - 1, start + PRESETS_PER_PAGE - 1);
        selectedPresetIndex = Math.max(start, Math.min(selectedPresetIndex, end));
    }

    private void selectPresetById(String presetId) {
        int index = indexOfPreset(presetId);
        if (index >= 0) {
            selectedPresetIndex = index;
            presetPage = selectedPresetIndex / PRESETS_PER_PAGE;
        }
    }

    private int indexOfPreset(String presetId) {
        for (int i = 0; i < TrevorAddonsClient.CONFIG.presets.size(); i++) {
            if (presetId.equals(TrevorAddonsClient.CONFIG.presets.get(i).id)) {
                return i;
            }
        }
        return 0;
    }

    private TrevorConfig.Preset selectedPreset() {
        if (TrevorAddonsClient.CONFIG.presets.isEmpty()) {
            return TrevorConfig.createDefaultPreset();
        }
        selectedPresetIndex = Math.max(0, Math.min(selectedPresetIndex, TrevorAddonsClient.CONFIG.presets.size() - 1));
        return TrevorAddonsClient.CONFIG.presets.get(selectedPresetIndex);
    }

    private TrevorConfig.EntityRule selectedRule() {
        TrevorConfig.Preset preset = selectedPreset();
        TrevorConfig.EntityRule rule = preset.getRule(ENTITY_KEYS[selectedEntityIndex]);
        if (rule == null && !preset.entities.isEmpty()) {
            return preset.entities.get(Math.max(0, Math.min(selectedEntityIndex, preset.entities.size() - 1)));
        }
        return rule;
    }

    private List<Double> parseLifeValues(String input) {
        List<Double> values = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return values;
        }

        String[] parts = input.split("[,\\s]+");
        for (String part : parts) {
            if (part.isBlank()) continue;
            try {
                values.add(Double.parseDouble(part));
            } catch (NumberFormatException ignored) {
                return List.of();
            }
        }
        return values;
    }

    private static String formatLives(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(formatLife(values.get(i)));
        }
        return out.toString();
    }

    private static String formatLife(Double value) {
        if (value == null) {
            return "";
        }
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.0001d) {
            return String.format(Locale.ROOT, "%.0f", rounded);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static boolean isAllowedLifeChar(char chr) {
        return Character.isDigit(chr) || chr == ',' || chr == '.' || chr == ' ' || chr == '-';
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

    private record Rect(int x, int y, int w, int h) {
        static Rect empty() {
            return new Rect(0, 0, 0, 0);
        }

        boolean contains(double px, double py) {
            return px >= x && py >= y && px < (x + w) && py < (y + h);
        }
    }
}
