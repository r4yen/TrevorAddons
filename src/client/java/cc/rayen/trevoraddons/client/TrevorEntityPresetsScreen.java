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
    private static final int PRESETS_PER_PAGE = 7;
    private static final double[] QUICK_VALUES = {100.0, 500.0, 1000.0, 5000.0, 10000.0};
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

    private Rect presetCard = Rect.empty();
    private Rect entityCard = Rect.empty();
    private Rect editorCard = Rect.empty();
    private Rect[] presetRows = new Rect[0];
    private Rect[] entityRows = new Rect[0];
    private Rect[] valueChipRects = new Rect[0];
    private Rect[] quickAddRects = new Rect[0];

    private Rect prevPageRect = Rect.empty();
    private Rect nextPageRect = Rect.empty();
    private Rect newRect = Rect.empty();
    private Rect duplicateRect = Rect.empty();
    private Rect deleteRect = Rect.empty();
    private Rect useRect = Rect.empty();
    private Rect lifeFieldRect = Rect.empty();
    private Rect applyRect = Rect.empty();
    private Rect appendRect = Rect.empty();
    private Rect clearRect = Rect.empty();
    private Rect resetRect = Rect.empty();
    private Rect backRect = Rect.empty();

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
        syncLifeInputFromSelection();
        updateLayout();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        updateLayout();
        renderInGameBackground(context);

        int panelLeft = this.width / 2 - 430;
        int panelTop = this.height / 2 - 260;
        int panelRight = panelLeft + 860;
        int panelBottom = panelTop + 520;
        int accentColor = TrevorAddonsClient.CONFIG.tracerLineColor;
        int accentSoft = scaleRgb(accentColor, 0.76f);
        int accentDark = scaleRgb(accentColor, 0.44f);

        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE0161A21);
        context.fill(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, 0xFF11151B);
        context.fill(panelLeft, panelTop, panelLeft + 4, panelBottom, accentColor);

        context.drawText(this.textRenderer, Text.literal("TrevorAddons").styled(s -> s.withBold(true)), panelLeft + 16, panelTop + 14, accentColor, false);
        context.drawText(this.textRenderer, Text.literal("Entity presets"), panelLeft + 16, panelTop + 30, 0xFF9AA3AF, false);
        drawChip(context, panelRight - 188, panelTop + 14, 172, 20, "Active: " + TrevorAddonsClient.CONFIG.getActivePresetName(), accentSoft);

        drawPresetColumn(context, mouseX, mouseY);
        drawEntityColumn(context, mouseX, mouseY);
        drawEditor(context, mouseX, mouseY);
        drawFooter(context, mouseX, mouseY, accentSoft, accentDark);

        if (!statusMessage.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal(statusMessage), panelLeft + 16, panelBottom - 18, 0xFFE4EAF2, false);
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
            TrevorConfig.Preset copy = TrevorAddonsClient.CONFIG.duplicatePreset(TrevorConfig.DEFAULT_PRESET_ID);
            TrevorAddonsClient.CONFIG.save();
            selectPresetById(copy.id);
            syncLifeInputFromSelection();
            statusMessage = "Created " + copy.name + ".";
            return true;
        }
        if (duplicateRect.contains(mouseX, mouseY)) {
            TrevorConfig.Preset copy = TrevorAddonsClient.CONFIG.duplicatePreset(selectedPreset().id);
            TrevorAddonsClient.CONFIG.save();
            selectPresetById(copy.id);
            syncLifeInputFromSelection();
            statusMessage = "Duplicated to " + copy.name + ".";
            return true;
        }
        if (deleteRect.contains(mouseX, mouseY)) {
            if (!selectedPreset().editable) {
                statusMessage = "Default preset cannot be deleted.";
                return true;
            }
            TrevorAddonsClient.CONFIG.deletePreset(selectedPreset().id);
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
            replaceValuesFromInput();
            return true;
        }
        if (appendRect.contains(mouseX, mouseY)) {
            appendValuesFromInput();
            return true;
        }
        if (clearRect.contains(mouseX, mouseY)) {
            clearValues();
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

        for (int i = 0; i < valueChipRects.length; i++) {
            Rect row = valueChipRects[i];
            if (row.contains(mouseX, mouseY)) {
                removeValueAt(i);
                return true;
            }
        }

        for (int i = 0; i < quickAddRects.length; i++) {
            Rect row = quickAddRects[i];
            if (row.contains(mouseX, mouseY)) {
                addValue(QUICK_VALUES[i]);
                return true;
            }
        }

        lifeFieldFocused = false;
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == 256) {
            close();
            return true;
        }
        if (lifeFieldFocused) {
            if (input.getKeycode() == 257 || input.getKeycode() == 335) {
                replaceValuesFromInput();
                return true;
            }
            if (input.getKeycode() == 259) {
                if (!lifeInput.isEmpty()) {
                    lifeInput = lifeInput.substring(0, lifeInput.length() - 1);
                }
                return true;
            }
            if (input.getKeycode() == 261) {
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
        if (isAllowedLifeChar(chr) && lifeInput.length() < 96) {
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
        int left = presetCard.x + 12;
        int top = presetCard.y + 36;
        int width = presetCard.w - 24;
        context.drawText(this.textRenderer, Text.literal("Presets"), left, presetCard.y + 14, 0xFFE4EAF2, false);

        int start = presetPage * PRESETS_PER_PAGE;
        int end = Math.min(TrevorAddonsClient.CONFIG.presets.size(), start + PRESETS_PER_PAGE);
        presetRows = new Rect[end - start];
        for (int i = start; i < end; i++) {
            int rowY = top + ((i - start) * 26);
            Rect row = new Rect(left, rowY, width, 22);
            presetRows[i - start] = row;

            TrevorConfig.Preset preset = TrevorAddonsClient.CONFIG.presets.get(i);
            boolean selected = i == selectedPresetIndex;
            boolean active = preset.id.equals(TrevorAddonsClient.CONFIG.activePresetId);
            int fill = selected ? 0xFF314153 : 0xFF222A34;
            if (active) {
                fill = 0xFF2B3B2E;
            }
            if (row.contains(mouseX, mouseY)) {
                fill = scaleRgb(fill, 1.10f);
            }

            context.fill(row.x, row.y, row.x + row.w, row.y + row.h, fill);
            context.drawText(this.textRenderer, Text.literal(preset.name), row.x + 10, row.y + 6, 0xFFEAF0F7, false);
            context.drawText(this.textRenderer, Text.literal(active ? "Active" : (preset.editable ? "Custom" : "Default")), row.x + row.w - 58, row.y + 6, active ? 0xFFB6F5C1 : 0xFF9AA3AF, false);
        }

        prevPageRect = new Rect(left, presetCard.y + presetCard.h - 26, 44, 18);
        nextPageRect = new Rect(left + 52, presetCard.y + presetCard.h - 26, 44, 18);
        drawSmallButton(context, prevPageRect, "<", mouseX, mouseY);
        drawSmallButton(context, nextPageRect, ">", mouseX, mouseY);

        context.drawText(this.textRenderer,
                Text.literal((presetPage + 1) + "/" + Math.max(1, ((TrevorAddonsClient.CONFIG.presets.size() - 1) / PRESETS_PER_PAGE) + 1)),
                left + 108, presetCard.y + presetCard.h - 20, 0xFF9AA3AF, false);
    }

    private void drawEntityColumn(DrawContext context, int mouseX, int mouseY) {
        int left = entityCard.x + 12;
        int top = entityCard.y + 36;
        int width = entityCard.w - 24;
        context.drawText(this.textRenderer, Text.literal("Entities"), left, entityCard.y + 14, 0xFFE4EAF2, false);

        entityRows = new Rect[ENTITY_KEYS.length];
        for (int i = 0; i < ENTITY_KEYS.length; i++) {
            int rowY = top + (i * 26);
            Rect row = new Rect(left, rowY, width, 22);
            entityRows[i] = row;

            boolean selected = i == selectedEntityIndex;
            int fill = selected ? 0xFF314153 : 0xFF222A34;
            if (row.contains(mouseX, mouseY)) {
                fill = scaleRgb(fill, 1.10f);
            }

            context.fill(row.x, row.y, row.x + row.w, row.y + row.h, fill);
            TrevorConfig.EntityRule rule = selectedPreset().getRule(ENTITY_KEYS[i]);
            context.drawText(this.textRenderer, Text.literal(ENTITY_LABELS[i]), row.x + 10, row.y + 6, 0xFFEAF0F7, false);
            context.drawText(this.textRenderer, Text.literal(formatLives(rule.healthValues)), row.x + row.w - 132, row.y + 6, 0xFF9AA3AF, false);
        }
    }

    private void drawEditor(DrawContext context, int mouseX, int mouseY) {
        int left = editorCard.x + 12;
        int top = editorCard.y + 36;
        int width = editorCard.w - 24;
        TrevorConfig.Preset preset = selectedPreset();
        TrevorConfig.EntityRule rule = selectedRule();

        context.drawText(this.textRenderer, Text.literal("Editor"), left, editorCard.y + 14, 0xFFE4EAF2, false);
        context.drawText(this.textRenderer, Text.literal(ENTITY_LABELS[selectedEntityIndex]), left, editorCard.y + 28, 0xFFEAF0F7, false);
        context.drawText(this.textRenderer, Text.literal(preset.editable ? "Custom preset" : "Locked Default preset"), left + 112, editorCard.y + 28, preset.editable ? 0xFF9AA3AF : 0xFFB6A0A0, false);

        context.drawText(this.textRenderer, Text.literal("Current values"), left, top, 0xFFD5DBE5, false);
        valueChipRects = drawValueChips(context, left, top + 18, width, rule.healthValues, mouseX, mouseY);

        context.drawText(this.textRenderer, Text.literal("Input"), left, top + 112, 0xFFD5DBE5, false);
        lifeFieldRect = new Rect(left, top + 130, width, 22);
        boolean focused = lifeFieldFocused || lifeFieldRect.contains(mouseX, mouseY);
        context.fill(lifeFieldRect.x, lifeFieldRect.y, lifeFieldRect.x + lifeFieldRect.w, lifeFieldRect.y + lifeFieldRect.h, focused ? 0xFF2D3745 : 0xFF222A34);
        String placeholder = "Type values separated by commas";
        context.drawText(this.textRenderer, Text.literal(lifeInput.isEmpty() ? placeholder : lifeInput), lifeFieldRect.x + 10, lifeFieldRect.y + 7, lifeInput.isEmpty() ? 0xFF758197 : 0xFFEAF0F7, false);

        applyRect = new Rect(left, top + 160, 78, 20);
        appendRect = new Rect(left + 86, top + 160, 78, 20);
        clearRect = new Rect(left + 172, top + 160, 78, 20);
        resetRect = new Rect(left + 258, top + 160, 78, 20);
        drawSmallButton(context, applyRect, "Replace", mouseX, mouseY);
        drawSmallButton(context, appendRect, "Add", mouseX, mouseY);
        drawSmallButton(context, clearRect, "Clear", mouseX, mouseY);
        drawSmallButton(context, resetRect, "Reset", mouseX, mouseY);

        context.drawText(this.textRenderer, Text.literal("Quick add"), left, top + 192, 0xFFD5DBE5, false);
        quickAddRects = new Rect[QUICK_VALUES.length];
        for (int i = 0; i < QUICK_VALUES.length; i++) {
            int bx = left + (i * 68);
            Rect rect = new Rect(bx, top + 210, 60, 20);
            quickAddRects[i] = rect;
            drawSmallButton(context, rect, formatLife(QUICK_VALUES[i]), mouseX, mouseY);
        }

        if (!preset.editable) {
            context.drawText(this.textRenderer, Text.literal("The Default preset is locked and cannot be changed."), left, editorCard.y + editorCard.h - 18, 0xFF9AA3AF, false);
        }
    }

    private Rect[] drawValueChips(DrawContext context, int left, int top, int width, List<Double> values, int mouseX, int mouseY) {
        if (values == null || values.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal("No values set."), left, top + 4, 0xFF758197, false);
            return new Rect[0];
        }

        List<Rect> rects = new ArrayList<>();
        int x = left;
        int y = top;
        for (Double value : values) {
            String label = formatLife(value);
            int chipW = Math.max(40, this.textRenderer.getWidth(label) + 18);
            if (x + chipW > left + width) {
                x = left;
                y += 26;
            }

            Rect chip = new Rect(x, y, chipW, 20);
            rects.add(chip);
            boolean hover = chip.contains(mouseX, mouseY);
            context.fill(chip.x, chip.y, chip.x + chip.w, chip.y + chip.h, hover ? 0xFF324153 : 0xFF262F3A);
            context.drawText(this.textRenderer, Text.literal(label + " x"), chip.x + 8, chip.y + 6, 0xFFEAF0F7, false);

            x += chipW + 6;
        }

        return rects.toArray(new Rect[0]);
    }

    private void drawFooter(DrawContext context, int mouseX, int mouseY, int activeColor, int baseColor) {
        int left = presetCard.x + 12;
        int bottom = this.height / 2 + 260 - 30;

        newRect = new Rect(left, bottom, 86, 20);
        duplicateRect = new Rect(left + 92, bottom, 86, 20);
        deleteRect = new Rect(left + 184, bottom, 86, 20);
        useRect = new Rect(left + 276, bottom, 86, 20);
        backRect = new Rect(editorCard.x + editorCard.w - 98, bottom, 86, 20);

        drawActionButton(context, newRect, "New", mouseX, mouseY, activeColor, baseColor);
        drawActionButton(context, duplicateRect, "Duplicate", mouseX, mouseY, activeColor, baseColor);
        drawActionButton(context, deleteRect, "Delete", mouseX, mouseY, activeColor, baseColor);
        drawActionButton(context, useRect, "Use", mouseX, mouseY, activeColor, baseColor);
        drawActionButton(context, backRect, "Back", mouseX, mouseY, activeColor, baseColor);
    }

    private void drawChip(DrawContext context, int x, int y, int w, int h, String label, int fill) {
        context.fill(x, y, x + w, y + h, fill);
        context.drawText(this.textRenderer, Text.literal(label), x + 8, y + 6, 0xFFFFFFFF, false);
    }

    private void drawSmallButton(DrawContext context, Rect rect, String label, int mouseX, int mouseY) {
        drawActionButton(context, rect, label, mouseX, mouseY, 0xFF394556, 0xFF252D38);
    }

    private void drawActionButton(DrawContext context, Rect rect, String label, int mouseX, int mouseY, int hoverColor, int baseColor) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? hoverColor : baseColor);
        context.drawText(this.textRenderer, Text.literal(label), rect.x + 8, rect.y + 5, 0xFFFFFFFF, false);
    }

    private void replaceValuesFromInput() {
        TrevorConfig.Preset preset = selectedPreset();
        if (!preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }

        List<Double> parsed = parseLifeValues(lifeInput);
        if (parsed.isEmpty()) {
            statusMessage = "Enter at least one numeric value.";
            return;
        }

        selectedRule().healthValues = parsed;
        TrevorAddonsClient.CONFIG.save();
        syncLifeInputFromSelection();
        statusMessage = ENTITY_LABELS[selectedEntityIndex] + " values replaced.";
    }

    private void appendValuesFromInput() {
        TrevorConfig.Preset preset = selectedPreset();
        if (!preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }

        List<Double> parsed = parseLifeValues(lifeInput);
        if (parsed.isEmpty()) {
            statusMessage = "Enter at least one numeric value.";
            return;
        }

        TrevorConfig.EntityRule rule = selectedRule();
        for (Double value : parsed) {
            if (!containsValue(rule.healthValues, value)) {
                rule.healthValues.add(value);
            }
        }
        sortValues(rule.healthValues);
        TrevorAddonsClient.CONFIG.save();
        syncLifeInputFromSelection();
        lifeInput = "";
        statusMessage = ENTITY_LABELS[selectedEntityIndex] + " values appended.";
    }

    private void addValue(double value) {
        TrevorConfig.Preset preset = selectedPreset();
        if (!preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }

        TrevorConfig.EntityRule rule = selectedRule();
        if (!containsValue(rule.healthValues, value)) {
            rule.healthValues.add(value);
            sortValues(rule.healthValues);
        }
        TrevorAddonsClient.CONFIG.save();
        syncLifeInputFromSelection();
        statusMessage = formatLife(value) + " added.";
    }

    private void removeValueAt(int index) {
        TrevorConfig.Preset preset = selectedPreset();
        if (!preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }
        TrevorConfig.EntityRule rule = selectedRule();
        if (index >= 0 && index < rule.healthValues.size()) {
            rule.healthValues.remove(index);
            TrevorAddonsClient.CONFIG.save();
            syncLifeInputFromSelection();
            statusMessage = "Value removed.";
        }
    }

    private void clearValues() {
        TrevorConfig.Preset preset = selectedPreset();
        if (!preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }
        selectedRule().healthValues.clear();
        TrevorAddonsClient.CONFIG.save();
        syncLifeInputFromSelection();
        statusMessage = "Values cleared.";
    }

    private void resetSelectedEntityToDefault() {
        TrevorConfig.Preset preset = selectedPreset();
        if (!preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }

        TrevorConfig.EntityRule defaultRule = TrevorConfig.createDefaultPreset().getRule(ENTITY_KEYS[selectedEntityIndex]);
        selectedRule().healthValues = new ArrayList<>(defaultRule.healthValues);
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

    private static boolean containsValue(List<Double> values, double candidate) {
        for (Double value : values) {
            if (value != null && Math.abs(value - candidate) < 0.01d) {
                return true;
            }
        }
        return false;
    }

    private static void sortValues(List<Double> values) {
        values.sort(Double::compareTo);
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

    private void updateLayout() {
        int panelLeft = this.width / 2 - 430;
        int panelTop = this.height / 2 - 260;

        presetCard = new Rect(panelLeft + 12, panelTop + 56, 220, 204);
        entityCard = new Rect(panelLeft + 12, panelTop + 270, 220, 190);
        editorCard = new Rect(panelLeft + 244, panelTop + 56, 604, 404);
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
