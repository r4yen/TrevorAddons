package cc.rayen.trevoraddons.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrevorSettingsScreen extends Screen {
    private static final int WINDOW_W = 760;
    private static final int WINDOW_H = 440;
    private static final int COLOR_PICKER_STEP = 4;
    private static final int PRESETS_PER_PAGE = 6;
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

    private enum Page {
        GENERAL,
        PRESETS
    }

    private final Screen parent;

    private Page page = Page.GENERAL;

    private Rect generalDetectionCard = Rect.empty();
    private Rect generalAppearanceCard = Rect.empty();
    private Rect generalPresetCard = Rect.empty();
    private Rect presetListCard = Rect.empty();
    private Rect presetEditorCard = Rect.empty();
    private Rect entityListCard = Rect.empty();

    private Rect tabGeneralRect = Rect.empty();
    private Rect tabPresetsRect = Rect.empty();
    private Rect closeRect = Rect.empty();
    private Rect openPresetsRect = Rect.empty();
    private Rect espToggleRect = Rect.empty();
    private Rect tracerToggleRect = Rect.empty();
    private Rect thicknessTrackRect = Rect.empty();
    private Rect svRect = Rect.empty();
    private Rect hueRect = Rect.empty();

    private Rect prevPageRect = Rect.empty();
    private Rect nextPageRect = Rect.empty();
    private Rect newPresetRect = Rect.empty();
    private Rect duplicatePresetRect = Rect.empty();
    private Rect deletePresetRect = Rect.empty();
    private Rect usePresetRect = Rect.empty();
    private Rect[] presetRows = new Rect[0];
    private Rect[] entityRows = new Rect[0];
    private Rect[] valueChipRects = new Rect[0];
    private Rect lifeFieldRect = Rect.empty();
    private Rect replaceRect = Rect.empty();
    private Rect appendRect = Rect.empty();
    private Rect clearRect = Rect.empty();
    private Rect resetRect = Rect.empty();
    private Rect[] quickAddRects = new Rect[0];

    private boolean draggingThickness = false;
    private boolean draggingSv = false;
    private boolean draggingHue = false;
    private boolean configDirty = false;
    private boolean lifeFieldFocused = false;

    private int selectedPresetIndex = 0;
    private int selectedEntityIndex = 0;
    private int presetPage = 0;

    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float value = 1.0f;
    private String lifeInput = "";
    private String statusMessage = "";

    public TrevorSettingsScreen(Screen parent) {
        super(Text.literal("TrevorAddons Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        syncPickerFromConfig();
        syncSelectionToConfig();
        updateLayout();
        syncLifeInputFromSelection();
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
        int accentMuted = scaleRgb(accentColor, 0.76f);
        int accentDark = scaleRgb(accentColor, 0.46f);

        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE0171B22);
        context.fill(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, 0xFF11151B);
        context.fill(panelLeft, panelTop, panelLeft + 4, panelBottom, accentColor);

        context.drawText(this.textRenderer, Text.literal("TrevorAddons").styled(s -> s.withBold(true)), panelLeft + 16, panelTop + 14, accentColor, false);
        context.drawText(this.textRenderer, Text.literal("Client settings"), panelLeft + 16, panelTop + 30, 0xFF9AA3AF, false);
        drawChip(context, panelRight - 188, panelTop + 14, 172, 20, trim("Active: " + TrevorAddonsClient.CONFIG.getActivePresetName(), 160), accentMuted);

        drawTab(context, tabGeneralRect, "General", page == Page.GENERAL, mouseX, mouseY, accentColor);
        drawTab(context, tabPresetsRect, "Presets", page == Page.PRESETS, mouseX, mouseY, accentColor);

        if (page == Page.GENERAL) {
            drawGeneralPage(context, mouseX, mouseY, accentColor, accentMuted, accentDark);
        } else {
            drawPresetPage(context, mouseX, mouseY, accentColor, accentMuted, accentDark);
        }

        if (!statusMessage.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal(statusMessage), panelLeft + 16, panelBottom - 18, 0xFFE4EAF2, false);
        }

        drawActionButton(context, closeRect, "Close", mouseX, mouseY, accentMuted, accentDark);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (click.button() != 0) return super.mouseClicked(click, doubleClick);

        double mouseX = click.x();
        double mouseY = click.y();

        if (tabGeneralRect.contains(mouseX, mouseY)) {
            page = Page.GENERAL;
            lifeFieldFocused = false;
            statusMessage = "";
            return true;
        }
        if (tabPresetsRect.contains(mouseX, mouseY)) {
            page = Page.PRESETS;
            lifeFieldFocused = false;
            statusMessage = "";
            return true;
        }
        if (closeRect.contains(mouseX, mouseY)) {
            close();
            return true;
        }

        if (page == Page.GENERAL) {
            return handleGeneralClick(mouseX, mouseY);
        }
        return handlePresetClick(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (click.button() != 0) return super.mouseDragged(click, deltaX, deltaY);
        if (page == Page.GENERAL) {
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
        if (page == Page.PRESETS && lifeFieldFocused) {
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
        if (page != Page.PRESETS || !lifeFieldFocused) {
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
        flushConfigIfDirty();
        this.client.setScreen(parent);
    }

    private boolean handleGeneralClick(double mouseX, double mouseY) {
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
            page = Page.PRESETS;
            statusMessage = "";
            return true;
        }
        return false;
    }

    private boolean handlePresetClick(double mouseX, double mouseY) {
        if (newPresetRect.contains(mouseX, mouseY)) {
            TrevorConfig.Preset copy = TrevorAddonsClient.CONFIG.duplicatePreset(TrevorConfig.DEFAULT_PRESET_ID);
            TrevorAddonsClient.CONFIG.save();
            selectPresetById(copy.id);
            syncLifeInputFromSelection();
            statusMessage = "Created " + copy.name + ".";
            return true;
        }
        if (duplicatePresetRect.contains(mouseX, mouseY)) {
            TrevorConfig.Preset copy = TrevorAddonsClient.CONFIG.duplicatePreset(selectedPreset().id);
            TrevorAddonsClient.CONFIG.save();
            selectPresetById(copy.id);
            syncLifeInputFromSelection();
            statusMessage = "Duplicated to " + copy.name + ".";
            return true;
        }
        if (deletePresetRect.contains(mouseX, mouseY)) {
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
        if (usePresetRect.contains(mouseX, mouseY)) {
            TrevorAddonsClient.CONFIG.setActivePreset(selectedPreset().id);
            TrevorAddonsClient.CONFIG.save();
            statusMessage = "Active preset updated.";
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
        if (replaceRect.contains(mouseX, mouseY)) {
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
            if (presetRows[i].contains(mouseX, mouseY)) {
                selectedPresetIndex = Math.min(TrevorAddonsClient.CONFIG.presets.size() - 1, presetPage * PRESETS_PER_PAGE + i);
                selectedEntityIndex = 0;
                lifeFieldFocused = false;
                syncLifeInputFromSelection();
                statusMessage = "";
                return true;
            }
        }
        for (int i = 0; i < entityRows.length; i++) {
            if (entityRows[i].contains(mouseX, mouseY)) {
                selectedEntityIndex = i;
                lifeFieldFocused = false;
                syncLifeInputFromSelection();
                statusMessage = "";
                return true;
            }
        }
        for (int i = 0; i < valueChipRects.length; i++) {
            if (valueChipRects[i].contains(mouseX, mouseY)) {
                removeValueAt(i);
                return true;
            }
        }
        for (int i = 0; i < quickAddRects.length; i++) {
            if (quickAddRects[i].contains(mouseX, mouseY)) {
                addValue(QUICK_VALUES[i]);
                return true;
            }
        }
        lifeFieldFocused = false;
        return false;
    }

    private void drawGeneralPage(DrawContext context, int mouseX, int mouseY, int accentColor, int accentMuted, int accentDark) {
        context.drawText(this.textRenderer, Text.literal("Detection"), generalDetectionCard.x + 12, generalDetectionCard.y + 12, 0xFFEAF0F7, false);
        drawWrappedText(context, Text.literal("Toggle the Trevor highlight and tracer here."), generalDetectionCard.x + 12, generalDetectionCard.y + 30, generalDetectionCard.w - 24, 0xFF9AA3AF);
        drawToggle(context, espToggleRect, "Trevor Animals ESP", TrevorAddonsClient.CONFIG.markTrevorAnimals, mouseX, mouseY);
        drawToggle(context, tracerToggleRect, "Trevor Animals Tracer", TrevorAddonsClient.CONFIG.lineToTrevorAnimals, mouseX, mouseY);

        context.drawText(this.textRenderer, Text.literal("Appearance"), generalAppearanceCard.x + 12, generalAppearanceCard.y + 12, 0xFFEAF0F7, false);
        drawWrappedText(context, Text.literal("Adjust color and line thickness."), generalAppearanceCard.x + 12, generalAppearanceCard.y + 30, generalAppearanceCard.w - 24, 0xFF9AA3AF);

        context.drawText(this.textRenderer, Text.literal("Thickness"), thicknessTrackRect.x, thicknessTrackRect.y - 14, 0xFFD5DBE5, false);
        drawThicknessSlider(context, mouseX, mouseY);
        context.drawText(this.textRenderer, Text.literal("Color"), svRect.x, svRect.y - 14, 0xFFD5DBE5, false);
        drawColorPicker(context);

        context.fill(svRect.x + svRect.w + 26, svRect.y + 10, svRect.x + svRect.w + 96, svRect.y + 34, 0xFF000000);
        context.fill(svRect.x + svRect.w + 27, svRect.y + 11, svRect.x + svRect.w + 95, svRect.y + 33, 0xFF000000 | (TrevorAddonsClient.CONFIG.tracerLineColor & 0xFFFFFF));
        context.drawText(this.textRenderer, Text.literal(hexColor()), svRect.x + svRect.w + 26, svRect.y + 42, 0xFFC4CCD9, false);

        context.drawText(this.textRenderer, Text.literal("Presets"), generalPresetCard.x + 12, generalPresetCard.y + 12, 0xFFEAF0F7, false);
        drawWrappedText(context, Text.literal("Manage entity life presets in the same window."), generalPresetCard.x + 12, generalPresetCard.y + 30, generalPresetCard.w - 24, 0xFF9AA3AF);
        drawActionButton(context, openPresetsRect, "Presets", mouseX, mouseY, accentMuted, accentDark);
    }

    private void drawPresetPage(DrawContext context, int mouseX, int mouseY, int accentColor, int accentMuted, int accentDark) {
        context.drawText(this.textRenderer, Text.literal("Presets"), presetListCard.x + 12, presetListCard.y + 12, 0xFFEAF0F7, false);
        drawWrappedText(context, Text.literal("Select a preset, then edit the entity life values on the right."), presetListCard.x + 12, presetListCard.y + 30, presetListCard.w - 24, 0xFF9AA3AF);

        int start = presetPage * PRESETS_PER_PAGE;
        int end = Math.min(TrevorAddonsClient.CONFIG.presets.size(), start + PRESETS_PER_PAGE);
        presetRows = new Rect[end - start];
        int rowY = presetListCard.y + 54;
        for (int i = start; i < end; i++) {
            Rect row = new Rect(presetListCard.x + 12, rowY, presetListCard.w - 24, 24);
            presetRows[i - start] = row;
            TrevorConfig.Preset preset = TrevorAddonsClient.CONFIG.presets.get(i);
            boolean selected = i == selectedPresetIndex;
            boolean active = preset.id.equals(TrevorAddonsClient.CONFIG.activePresetId);
            int fill = selected ? 0xFF314153 : 0xFF222A34;
            if (active) fill = 0xFF2B3B2E;
            if (row.contains(mouseX, mouseY)) fill = scaleRgb(fill, 1.08f);

            context.fill(row.x, row.y, row.x + row.w, row.y + row.h, fill);
            context.drawText(this.textRenderer, trim(preset.name, row.w - 92), row.x + 10, row.y + 7, 0xFFEAF0F7, false);
            String state = active ? "Active" : (preset.editable ? "Custom" : "Default");
            context.drawText(this.textRenderer, Text.literal(state), row.x + row.w - 54, row.y + 7, active ? 0xFFB6F5C1 : 0xFF9AA3AF, false);
            rowY += 26;
        }

        prevPageRect = new Rect(presetListCard.x + 12, presetListCard.y + presetListCard.h - 26, 40, 18);
        nextPageRect = new Rect(presetListCard.x + 58, presetListCard.y + presetListCard.h - 26, 40, 18);
        drawSmallButton(context, prevPageRect, "<", mouseX, mouseY);
        drawSmallButton(context, nextPageRect, ">", mouseX, mouseY);
        context.drawText(this.textRenderer, Text.literal((presetPage + 1) + "/" + Math.max(1, ((TrevorAddonsClient.CONFIG.presets.size() - 1) / PRESETS_PER_PAGE) + 1)), presetListCard.x + 106, presetListCard.y + presetListCard.h - 20, 0xFF9AA3AF, false);

        newPresetRect = new Rect(presetListCard.x + 12, presetListCard.y + presetListCard.h - 56, 62, 20);
        duplicatePresetRect = new Rect(presetListCard.x + 80, presetListCard.y + presetListCard.h - 56, 72, 20);
        deletePresetRect = new Rect(presetListCard.x + 158, presetListCard.y + presetListCard.h - 56, 52, 20);
        usePresetRect = new Rect(presetListCard.x + 12, presetListCard.y + presetListCard.h - 30, 110, 20);
        drawSmallButton(context, newPresetRect, "New", mouseX, mouseY);
        drawSmallButton(context, duplicatePresetRect, "Copy", mouseX, mouseY);
        drawSmallButton(context, deletePresetRect, "Del", mouseX, mouseY);
        drawSmallButton(context, usePresetRect, "Use Selected", mouseX, mouseY);

        context.drawText(this.textRenderer, Text.literal("Entities"), entityListCard.x + 12, entityListCard.y + 12, 0xFFEAF0F7, false);
        drawWrappedText(context, Text.literal("Pick one entity and edit its life values."), entityListCard.x + 12, entityListCard.y + 30, entityListCard.w - 24, 0xFF9AA3AF);

        entityRows = new Rect[ENTITY_KEYS.length];
        int entityRowY = entityListCard.y + 54;
        for (int i = 0; i < ENTITY_KEYS.length; i++) {
            Rect row = new Rect(entityListCard.x + 12, entityRowY, entityListCard.w - 24, 24);
            entityRows[i] = row;
            boolean selected = i == selectedEntityIndex;
            int fill = selected ? 0xFF314153 : 0xFF222A34;
            if (row.contains(mouseX, mouseY)) fill = scaleRgb(fill, 1.08f);
            context.fill(row.x, row.y, row.x + row.w, row.y + row.h, fill);
            TrevorConfig.EntityRule rule = selectedPreset().getRule(ENTITY_KEYS[i]);
            context.drawText(this.textRenderer, Text.literal(trim(ENTITY_LABELS[i], row.w - 130)), row.x + 10, row.y + 7, 0xFFEAF0F7, false);
            context.drawText(this.textRenderer, trim(formatLives(rule.healthValues), 106), row.x + row.w - 116, row.y + 7, 0xFF9AA3AF, false);
            entityRowY += 26;
        }

        context.drawText(this.textRenderer, Text.literal("Editor"), presetEditorCard.x + 12, presetEditorCard.y + 12, 0xFFEAF0F7, false);
        drawWrappedText(context, Text.literal("Edit the selected entity's life list."), presetEditorCard.x + 12, presetEditorCard.y + 30, presetEditorCard.w - 24, 0xFF9AA3AF);

        TrevorConfig.Preset preset = selectedPreset();
        TrevorConfig.EntityRule rule = selectedRule();
        context.drawText(this.textRenderer, Text.literal(trim("Selected: " + ENTITY_LABELS[selectedEntityIndex], presetEditorCard.w - 24)), presetEditorCard.x + 12, presetEditorCard.y + 52, 0xFFEAF0F7, false);
        context.drawText(this.textRenderer, Text.literal(preset.editable ? "Editable preset" : "Locked Default preset"), presetEditorCard.x + 12, presetEditorCard.y + 66, preset.editable ? 0xFF9AA3AF : 0xFFB6A0A0, false);

        context.drawText(this.textRenderer, Text.literal("Values"), presetEditorCard.x + 12, presetEditorCard.y + 88, 0xFFD5DBE5, false);
        valueChipRects = drawValueChips(context, presetEditorCard.x + 12, presetEditorCard.y + 106, presetEditorCard.w - 24, rule.healthValues, mouseX, mouseY);

        context.drawText(this.textRenderer, Text.literal("Input"), presetEditorCard.x + 12, presetEditorCard.y + 162, 0xFFD5DBE5, false);
        lifeFieldRect = new Rect(presetEditorCard.x + 12, presetEditorCard.y + 180, presetEditorCard.w - 24, 22);
        boolean focused = lifeFieldFocused || lifeFieldRect.contains(mouseX, mouseY);
        context.fill(lifeFieldRect.x, lifeFieldRect.y, lifeFieldRect.x + lifeFieldRect.w, lifeFieldRect.y + lifeFieldRect.h, focused ? 0xFF2D3745 : 0xFF222A34);
        String placeholder = "Type values separated by commas";
        context.drawText(this.textRenderer, Text.literal(trim(lifeInput.isEmpty() ? placeholder : lifeInput, lifeFieldRect.w - 20)), lifeFieldRect.x + 10, lifeFieldRect.y + 7, lifeInput.isEmpty() ? 0xFF758197 : 0xFFEAF0F7, false);

        replaceRect = new Rect(presetEditorCard.x + 12, presetEditorCard.y + 212, 66, 20);
        appendRect = new Rect(presetEditorCard.x + 84, presetEditorCard.y + 212, 54, 20);
        clearRect = new Rect(presetEditorCard.x + 144, presetEditorCard.y + 212, 54, 20);
        resetRect = new Rect(presetEditorCard.x + 204, presetEditorCard.y + 212, 54, 20);
        drawSmallButton(context, replaceRect, "Set", mouseX, mouseY);
        drawSmallButton(context, appendRect, "Add", mouseX, mouseY);
        drawSmallButton(context, clearRect, "Clear", mouseX, mouseY);
        drawSmallButton(context, resetRect, "Reset", mouseX, mouseY);

        context.drawText(this.textRenderer, Text.literal("Quick add"), presetEditorCard.x + 12, presetEditorCard.y + 242, 0xFFD5DBE5, false);
        quickAddRects = new Rect[QUICK_VALUES.length];
        for (int i = 0; i < QUICK_VALUES.length; i++) {
            Rect rect = new Rect(presetEditorCard.x + 12 + (i * 66), presetEditorCard.y + 260, 60, 20);
            quickAddRects[i] = rect;
            drawSmallButton(context, rect, formatLife(QUICK_VALUES[i]), mouseX, mouseY);
        }

        if (!preset.editable) {
            context.drawText(this.textRenderer, Text.literal("The Default preset is locked."), presetEditorCard.x + 12, presetEditorCard.y + presetEditorCard.h - 18, 0xFF9AA3AF, false);
        }
    }

    private Rect[] drawValueChips(DrawContext context, int left, int top, int width, List<Double> values, int mouseX, int mouseY) {
        if (values == null || values.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal("No values set."), left, top + 2, 0xFF758197, false);
            return new Rect[0];
        }

        List<Rect> rects = new ArrayList<>();
        int x = left;
        int y = top;
        for (Double value : values) {
            String label = formatLife(value);
            int chipW = Math.max(38, this.textRenderer.getWidth(label) + 16);
            if (x + chipW > left + width) {
                x = left;
                y += 24;
            }

            Rect chip = new Rect(x, y, chipW, 18);
            rects.add(chip);
            boolean hover = chip.contains(mouseX, mouseY);
            context.fill(chip.x, chip.y, chip.x + chip.w, chip.y + chip.h, hover ? 0xFF324153 : 0xFF262F3A);
            context.drawText(this.textRenderer, Text.literal(label), chip.x + 8, chip.y + 5, 0xFFEAF0F7, false);
            x += chipW + 6;
        }

        return rects.toArray(new Rect[0]);
    }

    private void drawTab(DrawContext context, Rect rect, String label, boolean selected, int mouseX, int mouseY, int accentColor) {
        boolean hover = rect.contains(mouseX, mouseY);
        int fill = selected ? accentColor : (hover ? 0xFF2A333F : 0xFF222A34);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, fill);
        context.drawText(this.textRenderer, Text.literal(trim(label, rect.w - 20)), rect.x + 10, rect.y + 5, 0xFFFFFFFF, false);
    }

    private void drawChip(DrawContext context, int x, int y, int w, int h, String label, int fill) {
        context.fill(x, y, x + w, y + h, fill);
        context.drawText(this.textRenderer, Text.literal(trim(label, w - 16)), x + 8, y + 6, 0xFFFFFFFF, false);
    }

    private void drawToggle(DrawContext context, Rect rect, String label, boolean value, int mouseX, int mouseY) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? 0xFF2A333F : 0xFF222A34);
        context.drawText(this.textRenderer, Text.literal(trim(label, rect.w - 72)), rect.x + 10, rect.y + 7, 0xFFE7EDF5, false);
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
        context.drawText(this.textRenderer, Text.literal(String.format(Locale.ROOT, "%.1f", TrevorAddonsClient.CONFIG.tracerLineWidth)), r.x + r.w + 12, r.y + 2, 0xFFD5DBE5, false);
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

    private void drawActionButton(DrawContext context, Rect rect, String label, int mouseX, int mouseY, int hoverColor, int baseColor) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? hoverColor : baseColor);
        context.drawText(this.textRenderer, Text.literal(trim(label, rect.w - 20)), rect.x + 10, rect.y + 5, 0xFFFFFFFF, false);
    }

    private void drawSmallButton(DrawContext context, Rect rect, String label, int mouseX, int mouseY) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? 0xFF314153 : 0xFF222A34);
        context.drawText(this.textRenderer, Text.literal(trim(label, rect.w - 16)), rect.x + 8, rect.y + 5, 0xFFFFFFFF, false);
    }

    private void updateThicknessFromMouse(double mouseX) {
        double t = (mouseX - thicknessTrackRect.x) / (double) thicknessTrackRect.w;
        t = clamp01(t);
        double width = 1.0 + (9.0 * t);
        TrevorAddonsClient.CONFIG.tracerLineWidth = Math.round(width * 10.0) / 10.0;
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
        statusMessage = ENTITY_LABELS[selectedEntityIndex] + " values updated.";
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
        int panelLeft = this.width / 2 - WINDOW_W / 2;
        int panelTop = this.height / 2 - WINDOW_H / 2;

        tabGeneralRect = new Rect(panelLeft + 16, panelTop + 52, 72, 20);
        tabPresetsRect = new Rect(panelLeft + 94, panelTop + 52, 72, 20);
        closeRect = new Rect(panelLeft + WINDOW_W - 100, panelTop + WINDOW_H - 28, 88, 18);

        generalDetectionCard = new Rect(panelLeft + 12, panelTop + 84, 222, 118);
        generalAppearanceCard = new Rect(panelLeft + 244, panelTop + 84, 494, 248);
        generalPresetCard = new Rect(panelLeft + 12, panelTop + 212, 222, 120);
        openPresetsRect = new Rect(generalPresetCard.x + 12, generalPresetCard.y + 56, generalPresetCard.w - 24, 20);

        presetListCard = new Rect(panelLeft + 12, panelTop + 84, 230, 330);
        entityListCard = new Rect(panelLeft + 250, panelTop + 84, 210, 330);
        presetEditorCard = new Rect(panelLeft + 468, panelTop + 84, 280, 330);

        espToggleRect = new Rect(generalDetectionCard.x + 12, generalDetectionCard.y + 46, generalDetectionCard.w - 24, 24);
        tracerToggleRect = new Rect(generalDetectionCard.x + 12, generalDetectionCard.y + 76, generalDetectionCard.w - 24, 24);
        thicknessTrackRect = new Rect(generalAppearanceCard.x + 16, generalAppearanceCard.y + 54, 186, 16);
        svRect = new Rect(generalAppearanceCard.x + 16, generalAppearanceCard.y + 92, 180, 120);
        hueRect = new Rect(generalAppearanceCard.x + 204, generalAppearanceCard.y + 92, 14, 120);
    }

    private void drawWrappedText(DrawContext context, Text text, int x, int y, int width, int color) {
        List<OrderedText> lines = this.textRenderer.wrapLines(text, width);
        int offset = 0;
        for (OrderedText line : lines) {
            context.drawText(this.textRenderer, line, x, y + offset, color, false);
            offset += 10;
        }
    }

    private String trim(String text, int width) {
        return this.textRenderer.trimToWidth(text, width);
    }

    private static int primaryColor() {
        return 0xFF000000 | (TrevorAddonsClient.CONFIG.tracerLineColor & 0xFFFFFF);
    }

    private String hexColor() {
        return String.format(Locale.ROOT, "#%06X", TrevorAddonsClient.CONFIG.tracerLineColor & 0xFFFFFF);
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
