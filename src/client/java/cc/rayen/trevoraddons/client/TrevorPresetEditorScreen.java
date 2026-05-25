package cc.rayen.trevoraddons.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TrevorPresetEditorScreen extends Screen {
    private static final int WINDOW_W = 760;
    private static final int WINDOW_H = 520;
    private static final int COLOR_PICKER_STEP = 4;
    private static final double MIN_TRACER_LINE_WIDTH = 0.12;
    private static final double MAX_TRACER_LINE_WIDTH = 14.0;
    private static final double TRACER_WIDTH_CURVE = 2.8;
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
    private final boolean embedded;
    private final Set<String> expandedPresetIds = new HashSet<>();
    private final Set<String> expandedEntityIds = new HashSet<>();
    private final List<TreeRow> treeRows = new ArrayList<>();

    private Rect visualsHeaderRect = Rect.empty();
    private Rect visualsBodyRect = Rect.empty();
    private Rect presetsHeaderRect = Rect.empty();
    private Rect presetsBodyRect = Rect.empty();
    private Rect footerRect = Rect.empty();
    private Rect closeRect = Rect.empty();
    private Rect espToggleRect = Rect.empty();
    private Rect tracerToggleRect = Rect.empty();
    private Rect thicknessTrackRect = Rect.empty();
    private Rect svRect = Rect.empty();
    private Rect hueRect = Rect.empty();
    private Rect activePresetChipRect = Rect.empty();
    private Rect presetsAddRect = Rect.empty();
    private Rect visualsToggleRect = Rect.empty();
    private Rect presetsToggleRect = Rect.empty();
    private Rect inputRect = Rect.empty();
    private Rect mobTypeRect = Rect.empty();
    private Rect addRect = Rect.empty();
    private Rect deleteRect = Rect.empty();

    private boolean visualsExpanded = true;
    private boolean presetsExpanded = true;
    private boolean draggingThickness = false;
    private boolean draggingSv = false;
    private boolean draggingHue = false;
    private boolean configDirty = false;
    private boolean inputFocused = false;
    private boolean mobTypeFocused = false;
    private boolean presetNameFocused = false;
    private boolean treeNeedsRefresh = true;

    private int treeScroll = 0;
    private int treeContentHeight = 0;

    private SelectionKind selectionKind = SelectionKind.PRESET;
    private String selectedPresetId = TrevorConfig.DEFAULT_PRESET_ID;
    private int selectedEntityIndex = 0;
    private int selectedLifeIndex = -1;

    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float value = 1.0f;
    private String mobTypeInput = "";
    private String presetNameInput = "";
    private String inputText = "";
    private String statusMessage = "";

    public TrevorPresetEditorScreen(Screen parent) {
        this(parent, false);
    }

    public TrevorPresetEditorScreen(Screen parent, boolean embedded) {
        super(Text.literal("TrevorAddons Preset Editor"));
        this.parent = parent;
        this.embedded = embedded;
    }

    public void syncHost(MinecraftClient client, int width, int height) {
        this.client = client;
        this.width = width;
        this.height = height;
        this.textRenderer = client == null ? null : client.textRenderer;
    }

    @Override
    protected void init() {
        syncPickerFromConfig();
        expandedPresetIds.clear();
        expandedEntityIds.clear();
        syncSelectionToConfig();
        syncInputFromSelection();
        rebuildTree();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        updateLayout();
        rebuildTree();

        int panelLeft = this.width / 2 - WINDOW_W / 2;
        int panelTop = this.height / 2 - layoutHeight() / 2;
        int panelRight = panelLeft + WINDOW_W;
        int panelBottom = panelTop + layoutHeight();
        int accentColor = primaryColor();
        int accentMuted = scaleRgb(accentColor, 0.78f);
        int accentDark = scaleRgb(accentColor, 0.46f);

        if (!embedded) {
            renderInGameBackground(context);
            context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xE0171B22);
            context.fill(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, 0xFF11151B);
            context.fill(panelLeft, panelTop, panelLeft + 4, panelBottom, accentColor);
            context.drawText(mc().textRenderer, Text.literal("TrevorAddons").styled(s -> s.withBold(true)), panelLeft + 16, panelTop + 14, accentColor, false);
            context.drawText(mc().textRenderer, Text.literal("Client settings"), panelLeft + 16, panelTop + 30, 0xFF9AA3AF, false);
            drawChip(context, activePresetChipRect, trim("Active: " + TrevorAddonsClient.CONFIG.getActivePresetName(), activePresetChipRect.w - 16), accentMuted);
            drawSectionHeader(context, visualsHeaderRect, "Visuals", visualsExpanded, mouseX, mouseY, accentColor);
            if (visualsExpanded) {
                drawVisualsSection(context, mouseX, mouseY, accentColor);
            }

            drawSectionHeader(context, presetsHeaderRect, "Presets", presetsExpanded, mouseX, mouseY, accentColor);
            drawSmallButton(context, presetsAddRect, "+", mouseX, mouseY, accentMuted, accentDark);

            if (presetsExpanded) {
                drawPresetsTree(context, mouseX, mouseY, accentColor);
                drawFooterEditor(context, mouseX, mouseY, accentColor, accentMuted, accentDark);
            }
        } else {
            if (presetsExpanded) {
                drawPresetsTree(context, mouseX, mouseY, accentColor);
                drawFooterEditor(context, mouseX, mouseY, accentColor, accentMuted, accentDark);
            }
        }

        if (!embedded && !statusMessage.isEmpty()) {
            context.drawText(mc().textRenderer, Text.literal(trim(statusMessage, WINDOW_W - 32)), panelLeft + 16, panelBottom - 18, 0xFFE4EAF2, false);
        }

        if (!embedded) {
            drawActionButton(context, closeRect, "Close", mouseX, mouseY, accentMuted, accentDark);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (click.button() != 0) return super.mouseClicked(click, doubleClick);

        double mouseX = click.x();
        double mouseY = click.y();

        if (!embedded && closeRect.contains(mouseX, mouseY)) {
            close();
            return true;
        }
        if (embedded) {
            if (presetsAddRect.contains(mouseX, mouseY)) {
                addPreset();
                return true;
            }
            if (presetsExpanded) {
                if (mobTypeRect.contains(mouseX, mouseY)) {
                    mobTypeFocused = true;
                    inputFocused = false;
                    presetNameFocused = false;
                    return true;
                }
                if (inputRect.contains(mouseX, mouseY)) {
                    inputFocused = true;
                    mobTypeFocused = false;
                    presetNameFocused = false;
                    return true;
                }
                if (addRect.contains(mouseX, mouseY)) {
                    applyInput(true);
                    return true;
                }
                if (deleteRect.contains(mouseX, mouseY)) {
                    deleteSelection();
                    return true;
                }
                for (TreeRow row : treeRows) {
                    if (!row.rect.contains(mouseX, mouseY)) continue;
                    if (row.addRect.contains(mouseX, mouseY) && row.canAdd) {
                        handleRowPlus(row);
                        return true;
                    }
                    if (row.deleteRect.contains(mouseX, mouseY) && row.canDelete) {
                        handleRowDelete(row);
                        return true;
                    }
                    if (doubleClick) {
                        if (row.kind == TreeKind.PRESET || row.kind == TreeKind.ENTITY) {
                            toggleRow(row);
                        }
                        return true;
                    }
                    selectRow(row);
                    inputFocused = false;
                    mobTypeFocused = false;
                    presetNameFocused = false;
                    return true;
                }
            }
            return false;
        }

        if (visualsToggleRect.contains(mouseX, mouseY)) {
            visualsExpanded = !visualsExpanded;
            return true;
        }
        if (presetsToggleRect.contains(mouseX, mouseY)) {
            presetsExpanded = !presetsExpanded;
            inputFocused = false;
            return true;
        }
        if (presetsAddRect.contains(mouseX, mouseY)) {
            addPreset();
            return true;
        }

        if (visualsExpanded && handleVisualsClick(mouseX, mouseY)) {
            return true;
        }

        if (presetsExpanded) {
            if (mobTypeRect.contains(mouseX, mouseY)) {
                mobTypeFocused = true;
                inputFocused = false;
                return true;
            }
            if (inputRect.contains(mouseX, mouseY)) {
                inputFocused = true;
                mobTypeFocused = false;
                return true;
            }
            if (addRect.contains(mouseX, mouseY)) {
                applyInput(true);
                return true;
            }
            if (deleteRect.contains(mouseX, mouseY)) {
                deleteSelection();
                return true;
            }

            for (TreeRow row : treeRows) {
                if (!row.rect.contains(mouseX, mouseY)) continue;
                if (row.addRect.contains(mouseX, mouseY) && row.canAdd) {
                    handleRowPlus(row);
                    return true;
                }
                if (row.deleteRect.contains(mouseX, mouseY) && row.canDelete) {
                    handleRowDelete(row);
                    return true;
                }
                if (doubleClick) {
                    if (row.kind == TreeKind.PRESET || row.kind == TreeKind.ENTITY) {
                        toggleRow(row);
                    }
                    return true;
                }
                selectRow(row);
                inputFocused = false;
                mobTypeFocused = false;
                presetNameFocused = false;
                return true;
            }
        }

        inputFocused = false;
        mobTypeFocused = false;
        presetNameFocused = false;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!presetsExpanded || !presetsBodyRect.contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (treeContentHeight <= presetsBodyRect.h) {
            return true;
        }
        treeScroll = clampInt(treeScroll - (int) Math.round(verticalAmount * 18.0), 0, treeContentHeight - presetsBodyRect.h);
        return true;
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
        if (presetsExpanded && presetNameFocused) {
            if (input.getKeycode() == 257 || input.getKeycode() == 335) {
                applyPresetName();
                return true;
            }
            if (input.getKeycode() == 259) {
                if (!presetNameInput.isEmpty()) {
                    presetNameInput = presetNameInput.substring(0, presetNameInput.length() - 1);
                }
                return true;
            }
            if (input.getKeycode() == 261) {
                presetNameInput = "";
                return true;
            }
        }
        if (presetsExpanded && mobTypeFocused) {
            if (input.getKeycode() == 257 || input.getKeycode() == 335) {
                applyMobType();
                return true;
            }
            if (input.getKeycode() == 259) {
                if (!mobTypeInput.isEmpty()) {
                    mobTypeInput = mobTypeInput.substring(0, mobTypeInput.length() - 1);
                }
                return true;
            }
            if (input.getKeycode() == 261) {
                mobTypeInput = "";
                return true;
            }
        }
        if (presetsExpanded && inputFocused) {
            if (input.getKeycode() == 257 || input.getKeycode() == 335) {
                applyInput(false);
                return true;
            }
            if (input.getKeycode() == 259) {
                if (!inputText.isEmpty()) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                }
                return true;
            }
            if (input.getKeycode() == 261) {
                inputText = "";
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!presetsExpanded || (!inputFocused && !mobTypeFocused && !presetNameFocused)) {
            return super.charTyped(input);
        }
        char chr = (char) input.codepoint();
        if (presetNameFocused && isAllowedNameChar(chr) && presetNameInput.length() < 96) {
            presetNameInput += chr;
            return true;
        }
        if (mobTypeFocused && isAllowedTypeChar(chr) && mobTypeInput.length() < 96) {
            mobTypeInput += chr;
            return true;
        }
        if (isAllowedLifeChar(chr) && inputText.length() < 96) {
            inputText += chr;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        flushConfigIfDirty();
        mc().setScreen(parent);
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

    private void drawVisualsSection(DrawContext context, int mouseX, int mouseY, int accentColor) {
        context.drawText(mc().textRenderer, Text.literal("Detection"), visualsBodyRect.x + 12, visualsBodyRect.y + 8, 0xFFEAF0F7, false);
        drawToggle(context, espToggleRect, "Trevor Animals ESP", TrevorAddonsClient.CONFIG.markTrevorAnimals, mouseX, mouseY);
        drawToggle(context, tracerToggleRect, "Trevor Animals Tracer", TrevorAddonsClient.CONFIG.lineToTrevorAnimals, mouseX, mouseY);

        context.drawText(mc().textRenderer, Text.literal("Tracer thickness"), thicknessTrackRect.x, thicknessTrackRect.y - 14, 0xFFD5DBE5, false);
        drawThicknessSlider(context, mouseX, mouseY);

        context.drawText(mc().textRenderer, Text.literal("Tracer color"), svRect.x, svRect.y - 14, 0xFFD5DBE5, false);
        drawColorPicker(context);
        context.drawText(mc().textRenderer, Text.literal(hexColor()), svRect.x + svRect.w + 24, svRect.y + 6, 0xFFC4CCD9, false);
        context.fill(svRect.x + svRect.w + 24, svRect.y + 24, svRect.x + svRect.w + 96, svRect.y + 48, 0xFF000000);
        context.fill(svRect.x + svRect.w + 25, svRect.y + 25, svRect.x + svRect.w + 95, svRect.y + 47, 0xFF000000 | (TrevorAddonsClient.CONFIG.tracerLineColor & 0xFFFFFF));
    }

    private void drawPresetsTree(DrawContext context, int mouseX, int mouseY, int accentColor) {
        treeRows.clear();

        int bodyX = presetsBodyRect.x;
        int bodyY = presetsBodyRect.y;
        int bodyW = presetsBodyRect.w;
        int y = bodyY - treeScroll;

        context.enableScissor(bodyX, bodyY, bodyX + bodyW, bodyY + presetsBodyRect.h);
        try {
            List<TrevorConfig.Preset> presets = TrevorAddonsClient.CONFIG.presets;
            for (int presetIndex = 0; presetIndex < presets.size(); presetIndex++) {
                TrevorConfig.Preset preset = presets.get(presetIndex);
                boolean presetExpanded = isExpandedPreset(preset.id);
                TreeRow presetRow = new TreeRow(TreeKind.PRESET, presetIndex, -1, -1, bodyX + 14, y, bodyW - 14, 26);
                presetRow.title = preset.name;
                presetRow.subtitle = preset.id.equals(TrevorConfig.DEFAULT_PRESET_ID) ? "Default preset" : "Custom preset";
                presetRow.expanded = presetExpanded;
                presetRow.selected = selectionKind == SelectionKind.PRESET && preset.id.equals(selectedPresetId);
                presetRow.active = preset.id.equals(TrevorAddonsClient.CONFIG.activePresetId);
                presetRow.editable = preset.editable;
                presetRow.canAdd = preset.editable;
                presetRow.canDelete = preset.editable;
                presetRow.canUse = !preset.id.equals(TrevorAddonsClient.CONFIG.activePresetId);
                presetRow.depth = 0;
                treeRows.add(presetRow);
                y += drawTreeRow(context, presetRow, mouseX, mouseY, accentColor, bodyX + 14, bodyW - 14);

                if (!presetExpanded) {
                    y += 2;
                    continue;
                }

                for (int entityIndex = 0; entityIndex < preset.entities.size(); entityIndex++) {
                    TrevorConfig.EntityRule rule = preset.entities.get(entityIndex);
                    boolean entityExpanded = isExpandedEntity(preset.id, entityIndex);
                    TreeRow entityRow = new TreeRow(TreeKind.ENTITY, presetIndex, entityIndex, -1, bodyX + 34, y, bodyW - 34, 24);
                    entityRow.title = "Mob " + (entityIndex + 1);
                    entityRow.subtitle = rule.name;
                    entityRow.expanded = entityExpanded;
                    entityRow.selected = selectionKind == SelectionKind.ENTITY && selectedPresetId.equals(preset.id) && selectedEntityIndex == entityIndex;
                    entityRow.editable = preset.editable;
                    entityRow.canAdd = preset.editable;
                    entityRow.canDelete = preset.editable && preset.entities.size() > 1;
                    entityRow.depth = 1;
                    treeRows.add(entityRow);
                    y += drawTreeRow(context, entityRow, mouseX, mouseY, accentColor, bodyX + 34, bodyW - 34);

                    if (!entityExpanded) {
                        y += 3;
                        continue;
                    }

                    if (rule.matchesAnyHealth) {
                        TreeRow wildcardRow = new TreeRow(TreeKind.LIFE, presetIndex, entityIndex, -1, bodyX + 52, y, bodyW - 52, 20);
                        wildcardRow.title = "All";
                        wildcardRow.subtitle = "";
                        wildcardRow.selected = selectionKind == SelectionKind.LIFE && selectedPresetId.equals(preset.id) && selectedEntityIndex == entityIndex && selectedLifeIndex == -1;
                        wildcardRow.editable = preset.editable;
                        wildcardRow.canAdd = false;
                        wildcardRow.canDelete = preset.editable;
                        wildcardRow.wildcard = true;
                        wildcardRow.depth = 2;
                        treeRows.add(wildcardRow);
                        y += drawTreeRow(context, wildcardRow, mouseX, mouseY, accentColor, bodyX + 52, bodyW - 52);
                    }

                    List<Double> lives = rule.healthValues;
                    for (int lifeIndex = 0; lifeIndex < lives.size(); lifeIndex++) {
                        TreeRow lifeRow = new TreeRow(TreeKind.LIFE, presetIndex, entityIndex, lifeIndex, bodyX + 52, y, bodyW - 52, 20);
                        lifeRow.title = formatLife(lives.get(lifeIndex));
                        lifeRow.subtitle = "";
                        lifeRow.selected = selectionKind == SelectionKind.LIFE && selectedPresetId.equals(preset.id) && selectedEntityIndex == entityIndex && selectedLifeIndex == lifeIndex;
                        lifeRow.editable = preset.editable;
                        lifeRow.canAdd = false;
                        lifeRow.canDelete = preset.editable;
                        lifeRow.wildcard = false;
                        lifeRow.depth = 2;
                        treeRows.add(lifeRow);
                        y += drawTreeRow(context, lifeRow, mouseX, mouseY, accentColor, bodyX + 52, bodyW - 52);
                    }
                    y += 3;
                }
                y += 2;
            }
        } finally {
            context.disableScissor();
        }

        treeContentHeight = Math.max(0, y - bodyY + treeScroll);
        treeScroll = clampInt(treeScroll, 0, Math.max(0, treeContentHeight - presetsBodyRect.h));
    }

    private int drawTreeRow(DrawContext context, TreeRow row, int mouseX, int mouseY, int accentColor, int startX, int width) {
        row.rect = new Rect(startX, row.y, width, row.h);
        boolean hover = row.rect.contains(mouseX, mouseY);

        int base = row.depth == 0 ? 0xFF222A34 : (row.depth == 1 ? 0xFF1F2731 : 0xFF1B222B);
        if (row.active) {
            base = 0xFF24322A;
        }
        if (row.selected) {
            base = 0xFF314153;
        } else if (hover) {
            base = scaleRgb(base, 1.08f);
        }

        context.fill(row.rect.x, row.rect.y, row.rect.x + row.rect.w, row.rect.y + row.rect.h, base);
        context.fill(row.rect.x, row.rect.y, row.rect.x + 3, row.rect.y + row.rect.h, accentColor);
        context.fill(row.rect.x, row.rect.y, row.rect.x + row.rect.w, row.rect.y + 1, 0xFF10151B);
        context.fill(row.rect.x, row.rect.y + row.rect.h - 1, row.rect.x + row.rect.w, row.rect.y + row.rect.h, 0xFF10151B);

        row.addRect = new Rect(row.rect.x + row.rect.w - 20, row.rect.y + 3, 16, row.rect.h - 6);
        row.deleteRect = new Rect(row.rect.x + row.rect.w - 40, row.rect.y + 3, 16, row.rect.h - 6);

        int titleX = row.rect.x + 28;
        int titleW = row.rect.w - 66;
        if (row.kind == TreeKind.LIFE) {
            context.drawText(mc().textRenderer, Text.literal("❤"), row.rect.x + 28, row.rect.y + 4, 0xFFE35757, false);
            if (row.selected && inputFocused) {
                drawInlineEditor(context, row.rect.x + 40, row.rect.y + 3, row.rect.w - 126, inputText, true, mouseX, mouseY);
            } else {
                context.drawText(mc().textRenderer, Text.literal(trim(row.title, titleW - 14)), row.rect.x + 40, row.rect.y + 6, 0xFFEAF0F7, false);
            }
        } else if (row.kind == TreeKind.ENTITY) {
            if (row.selected && mobTypeFocused) {
                drawInlineEditor(context, row.rect.x + 28, row.rect.y + 3, row.rect.w - 126, mobTypeInput, false, mouseX, mouseY);
            } else {
                context.drawText(mc().textRenderer, Text.literal(trim(row.title, titleW)), titleX, row.rect.y + 6, 0xFFEAF0F7, false);
                if (!row.subtitle.isEmpty()) {
                    context.drawText(mc().textRenderer, Text.literal(trim(row.subtitle, titleW)), titleX, row.rect.y + 15, 0xFF9AA3AF, false);
                }
            }
        } else {
            context.drawText(mc().textRenderer, Text.literal(trim(row.title, titleW)), titleX, row.rect.y + 6, 0xFFEAF0F7, false);
        }
        if (row.kind == TreeKind.PRESET && !row.subtitle.isEmpty()) {
            context.drawText(mc().textRenderer, Text.literal(trim(row.subtitle, titleW)), titleX, row.rect.y + 15, 0xFF9AA3AF, false);
        }

        if (row.kind != TreeKind.LIFE && row.canAdd) {
            drawSquareSymbolButton(context, row.addRect, "+", true, mouseX, mouseY, 0xFF324153, 0xFF222A34);
        }
        drawSquareSymbolButton(context, row.deleteRect, "-", row.canDelete, mouseX, mouseY, 0xFF5F2D36, 0xFF3A2229);

        return row.rect.h + 3;
    }

    private void drawFooterEditor(DrawContext context, int mouseX, int mouseY, int accentColor, int accentMuted, int accentDark) {
        int footerX = footerRect.x;
        int footerY = footerRect.y;
        int footerW = footerRect.w;

        context.fill(footerX, footerY, footerX + footerW, footerY + footerRect.h, 0xFF171C24);
        context.fill(footerX, footerY, footerX + footerW, footerY + 1, 0xFF10151B);
        if (embedded) {
            addRect = Rect.empty();
            deleteRect = Rect.empty();

            Rect editorRect = new Rect(footerX + 12, footerY + 16, footerW - 24, 20);
            String label;
            String value;
            boolean focused;
            if (selectionKind == SelectionKind.PRESET) {
                label = "Preset name";
                value = presetNameInput;
                focused = presetNameFocused || editorRect.contains(mouseX, mouseY);
            } else if (selectionKind == SelectionKind.ENTITY) {
                label = "Mob type";
                value = mobTypeInput;
                focused = mobTypeFocused || editorRect.contains(mouseX, mouseY);
            } else {
                label = "Lives";
                value = inputText;
                focused = inputFocused || editorRect.contains(mouseX, mouseY);
            }
            context.drawText(mc().textRenderer, Text.literal(label), footerX + 12, footerY + 4, 0xFFD5DBE5, false);
            context.fill(editorRect.x, editorRect.y, editorRect.x + editorRect.w, editorRect.y + editorRect.h, focused ? 0xFF2D3745 : 0xFF222A34);
            String placeholder = selectionKind == SelectionKind.PRESET ? "Enter preset name" : selectionKind == SelectionKind.ENTITY ? "Enter entity id" : "Enter life value";
            context.drawText(mc().textRenderer, Text.literal(trim(value.isEmpty() ? placeholder : value, editorRect.w - 12)), editorRect.x + 6, editorRect.y + 5, value.isEmpty() ? 0xFF758197 : 0xFFEAF0F7, false);
            mobTypeRect = selectionKind == SelectionKind.ENTITY ? editorRect : Rect.empty();
            inputRect = selectionKind == SelectionKind.LIFE ? editorRect : Rect.empty();
            return;
        }

        context.drawText(mc().textRenderer, Text.literal("Editor"), footerX + 12, footerY + 8, 0xFFEAF0F7, false);

        String selectedLabel = selectedSelectionLabel();
        context.drawText(mc().textRenderer, Text.literal(trim(selectedLabel, footerW - 180)), footerX + 12, footerY + 22, 0xFF9AA3AF, false);

        mobTypeRect = new Rect(footerX + 12, footerY + 38, footerW - 92, 20);
        boolean mobFocused = mobTypeFocused || mobTypeRect.contains(mouseX, mouseY);
        context.drawText(mc().textRenderer, Text.literal("Mob Type"), footerX + 12, footerY + 34, 0xFFD5DBE5, false);
        context.fill(mobTypeRect.x, mobTypeRect.y, mobTypeRect.x + mobTypeRect.w, mobTypeRect.y + mobTypeRect.h, mobFocused ? 0xFF2D3745 : 0xFF222A34);
        context.drawText(mc().textRenderer, Text.literal(trim(mobTypeInput.isEmpty() ? "Enter entity id" : mobTypeInput, mobTypeRect.w - 20)), mobTypeRect.x + 10, mobTypeRect.y + 6, mobTypeInput.isEmpty() ? 0xFF758197 : 0xFFEAF0F7, false);
        inputRect = new Rect(footerX + 12, footerY + 66, footerW - 24, 20);
        boolean focused = inputFocused || inputRect.contains(mouseX, mouseY);
        context.drawText(mc().textRenderer, Text.literal("Lives"), footerX + 12, footerY + 62, 0xFFD5DBE5, false);
        context.fill(inputRect.x, inputRect.y, inputRect.x + inputRect.w, inputRect.y + inputRect.h, focused ? 0xFF2D3745 : 0xFF222A34);
        String placeholder = selectionKind == SelectionKind.LIFE ? "Type a value or *" : "Type values separated by commas, or *";
        context.drawText(mc().textRenderer, Text.literal(trim(inputText.isEmpty() ? placeholder : inputText, inputRect.w - 20)), inputRect.x + 10, inputRect.y + 6, inputText.isEmpty() ? 0xFF758197 : 0xFFEAF0F7, false);

        addRect = new Rect(footerX + 12, footerY + 92, 18, 18);
        deleteRect = new Rect(footerX + 36, footerY + 92, 18, 18);

        drawSquareSymbolButton(context, addRect, "+", true, mouseX, mouseY, accentMuted, accentDark);
        drawSquareSymbolButton(context, deleteRect, "-", true, mouseX, mouseY, 0xFF5F2D36, 0xFF3A2229);
    }

    private void drawSectionHeader(DrawContext context, Rect rect, String label, boolean expanded, int mouseX, int mouseY, int accentColor) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? 0xFF222A34 : 0xFF1B212A);
        context.fill(rect.x, rect.y, rect.x + 4, rect.y + rect.h, accentColor);
        context.drawText(mc().textRenderer, Text.literal(expanded ? "v" : ">"), rect.x + 10, rect.y + 6, 0xFFEAF0F7, false);
        context.drawText(mc().textRenderer, Text.literal(label), rect.x + 28, rect.y + 6, 0xFFEAF0F7, false);
        if (rect == presetsHeaderRect) {
            context.drawText(mc().textRenderer, Text.literal(trim(TrevorAddonsClient.CONFIG.getActivePresetName(), 120)), rect.x + rect.w - 140, rect.y + 6, 0xFF9AA3AF, false);
        }
    }

    private void drawToggle(DrawContext context, Rect rect, String label, boolean value, int mouseX, int mouseY) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? 0xFF2A333F : 0xFF222A34);
        context.drawText(mc().textRenderer, Text.literal(trim(label, rect.w - 72)), rect.x + 10, rect.y + 7, 0xFFE7EDF5, false);
        int chipW = 44;
        int chipH = 16;
        int chipX = rect.x + rect.w - chipW - 12;
        int chipY = rect.y + (rect.h - chipH) / 2;
        context.fill(chipX, chipY, chipX + chipW, chipY + chipH, value ? primaryColor() : 0xFF6D3140);
        context.drawText(mc().textRenderer, Text.literal(value ? "ON" : "OFF"), chipX + 11, chipY + 4, 0xFFFFFFFF, false);
    }

    private void drawThicknessSlider(DrawContext context, int mouseX, int mouseY) {
        Rect r = thicknessTrackRect;
        context.fill(r.x, r.y + 5, r.x + r.w, r.y + 10, 0xFF2A313D);
        double normalized = (TrevorAddonsClient.CONFIG.tracerLineWidth - MIN_TRACER_LINE_WIDTH) / (MAX_TRACER_LINE_WIDTH - MIN_TRACER_LINE_WIDTH);
        normalized = Math.pow(clamp01(normalized), 1.0 / TRACER_WIDTH_CURVE);
        int knobX = r.x + (int) Math.round(normalized * (r.w - 1));
        int knobColor = r.contains(mouseX, mouseY) || draggingThickness ? primaryColor() : 0xFFD9E2EE;
        context.fill(knobX - 3, r.y + 1, knobX + 4, r.y + 14, knobColor);
        context.drawText(mc().textRenderer, Text.literal(formatThickness(TrevorAddonsClient.CONFIG.tracerLineWidth)), r.x + r.w + 12, r.y + 2, 0xFFD5DBE5, false);
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
        drawSmallButton(context, rect, label, mouseX, mouseY, hoverColor, baseColor);
    }

    private void drawSmallButton(DrawContext context, Rect rect, String label, int mouseX, int mouseY, int hoverColor, int baseColor) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? hoverColor : baseColor);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + 1, 0xFF10151B);
        context.fill(rect.x, rect.y + rect.h - 1, rect.x + rect.w, rect.y + rect.h, 0xFF10151B);
        context.drawCenteredTextWithShadow(mc().textRenderer, Text.literal(label), rect.x + rect.w / 2, rect.y + 5, 0xFFFFFFFF);
    }

    private void drawSquareSymbolButton(DrawContext context, Rect rect, String symbol, boolean enabled, int mouseX, int mouseY, int hoverColor, int baseColor) {
        int visibleHover = enabled ? hoverColor : scaleRgb(hoverColor, 0.82f);
        int visibleBase = enabled ? baseColor : scaleRgb(baseColor, 0.82f);
        drawSmallButton(context, rect, symbol, mouseX, mouseY, visibleHover, visibleBase);
    }

    private void drawTrashButton(DrawContext context, Rect rect, int mouseX, int mouseY) {
        boolean hover = rect.contains(mouseX, mouseY);
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, hover ? 0xFF7A3540 : 0xFF5F2D36);
        context.fill(rect.x + 5, rect.y + 5, rect.x + rect.w - 5, rect.y + 7, 0xFFFFFFFF);
        context.fill(rect.x + 6, rect.y + 7, rect.x + rect.w - 6, rect.y + rect.h - 4, 0xFFEDEDED);
        context.fill(rect.x + 7, rect.y + 8, rect.x + 8, rect.y + rect.h - 5, 0xFF5F2D36);
        context.fill(rect.x + 10, rect.y + 8, rect.x + 11, rect.y + rect.h - 5, 0xFF5F2D36);
        context.fill(rect.x + 13, rect.y + 8, rect.x + 14, rect.y + rect.h - 5, 0xFF5F2D36);
    }

    private void drawInlineEditor(DrawContext context, int x, int y, int width, String valueText, boolean lifeValue, int mouseX, int mouseY) {
        Rect editorRect = new Rect(x, y, Math.max(64, width), 18);
        boolean hover = editorRect.contains(mouseX, mouseY);
        boolean focused = lifeValue ? inputFocused : mobTypeFocused;
        context.fill(editorRect.x, editorRect.y, editorRect.x + editorRect.w, editorRect.y + editorRect.h, focused || hover ? 0xFF2D3745 : 0xFF222A34);
        String text = valueText == null || valueText.isEmpty()
                ? (lifeValue ? "Type a value" : "Enter entity id")
                : valueText;
        context.drawText(mc().textRenderer, Text.literal(trim(text, editorRect.w - 12)), editorRect.x + 6, editorRect.y + 5, valueText == null || valueText.isEmpty() ? 0xFF758197 : 0xFFEAF0F7, false);
    }

    private void drawChip(DrawContext context, Rect rect, String label, int fill) {
        context.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, fill);
        context.drawText(mc().textRenderer, Text.literal(trim(label, rect.w - 16)), rect.x + 8, rect.y + 6, 0xFFFFFFFF, false);
    }

    private void addPreset() {
        TrevorConfig.Preset copy = TrevorAddonsClient.CONFIG.duplicatePreset(TrevorConfig.DEFAULT_PRESET_ID);
        copy.entities = new ArrayList<>();
        TrevorAddonsClient.CONFIG.save();
        selectedPresetId = copy.id;
        selectionKind = SelectionKind.PRESET;
        selectedEntityIndex = 0;
        selectedLifeIndex = -1;
        expandedPresetIds.add(copy.id);
        syncInputFromSelection();
        statusMessage = "Created " + copy.name + ".";
        markConfigDirty();
    }

    public void createPresetFromSettings() {
        addPreset();
    }

    private void addMobToSelectedPreset() {
        TrevorConfig.Preset preset = selectedPreset();
        if (preset == null || !preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }
        TrevorConfig.EntityRule source = preset.entities.isEmpty() ? TrevorConfig.createDefaultPreset().entities.get(0) : selectedEntity() != null ? selectedEntity().copy() : preset.entities.get(0).copy();
        source.name = source.name + " " + (preset.entities.size() + 1);
        preset.entities.add(source);
        selectedEntityIndex = preset.entities.size() - 1;
        selectedLifeIndex = -1;
        selectionKind = SelectionKind.ENTITY;
        expandedPresetIds.add(preset.id);
        expandedEntityIds.add(entityKey(preset.id, selectedEntityIndex));
        TrevorAddonsClient.CONFIG.save();
        syncInputFromSelection();
        statusMessage = source.name + " added.";
    }

    private void addLifeToSelectedEntity() {
        TrevorConfig.Preset preset = selectedPreset();
        if (preset == null || !preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }
        TrevorConfig.EntityRule rule = selectedEntity();
        if (rule == null) {
            statusMessage = "Select a mob first.";
            return;
        }
        double valueToAdd;
        if (selectionKind == SelectionKind.LIFE && selectedLifeIndex >= 0 && selectedLifeIndex < rule.healthValues.size()) {
            valueToAdd = rule.healthValues.get(selectedLifeIndex);
        } else if (!rule.healthValues.isEmpty()) {
            valueToAdd = rule.healthValues.get(rule.healthValues.size() - 1);
        } else {
            valueToAdd = 100.0;
        }
        if (!containsValue(rule.healthValues, valueToAdd)) {
            rule.healthValues.add(valueToAdd);
            sortValues(rule.healthValues);
        }
        selectedLifeIndex = rule.healthValues.size() - 1;
        selectionKind = SelectionKind.LIFE;
        TrevorAddonsClient.CONFIG.save();
        syncInputFromSelection();
        statusMessage = "Life added.";
    }

    private void applyMobType() {
        TrevorConfig.Preset preset = selectedPreset();
        if (preset == null || !preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }
        TrevorConfig.EntityRule rule = selectedEntity();
        if (rule == null) {
            statusMessage = "Select a mob first.";
            return;
        }

        String value = mobTypeInput == null ? "" : mobTypeInput.trim();
        if (value.isEmpty()) {
            statusMessage = "Enter a mob type.";
            return;
        }

        String oldDisplay = displayNameFromKey(rule.id);
        rule.id = value;
        if (rule.name == null || rule.name.isBlank() || rule.name.equals(oldDisplay)) {
            rule.name = displayNameFromKey(value);
        }
        TrevorAddonsClient.CONFIG.save();
        syncInputFromSelection();
        statusMessage = "Mob type updated.";
    }

    private void applyPresetName() {
        TrevorConfig.Preset preset = selectedPreset();
        if (preset == null || !preset.editable) {
            statusMessage = "Default preset cannot be renamed.";
            return;
        }

        String value = presetNameInput == null ? "" : presetNameInput.trim();
        if (value.isEmpty()) {
            statusMessage = "Enter a preset name.";
            return;
        }

        preset.name = value;
        TrevorAddonsClient.CONFIG.save();
        syncInputFromSelection();
        statusMessage = "Preset renamed.";
    }

    private void applyInput(boolean append) {
        TrevorConfig.Preset preset = selectedPreset();
        if (preset == null || !preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }
        TrevorConfig.EntityRule rule = selectedEntity();
        if (rule == null) {
            statusMessage = "Select a mob first.";
            return;
        }
        List<Double> parsed = parseLifeValues(inputText);
        boolean wildcard = containsWildcardToken(inputText);
        if (parsed.isEmpty()) {
            if (!wildcard) {
                statusMessage = "Enter at least one value.";
                return;
            }
        }

        if (wildcard) {
            rule.matchesAnyHealth = true;
            if (selectionKind == SelectionKind.LIFE && selectedLifeIndex >= 0 && selectedLifeIndex < rule.healthValues.size()) {
                rule.healthValues.remove(selectedLifeIndex);
                selectedLifeIndex = -1;
            }
            sortValues(rule.healthValues);
            TrevorAddonsClient.CONFIG.save();
            syncInputFromSelection();
            statusMessage = "All added.";
            return;
        }

        if (selectionKind == SelectionKind.LIFE && !append) {
            Double valueToUse = parsed.get(0);
            if (selectedLifeIndex >= 0 && selectedLifeIndex < rule.healthValues.size()) {
                rule.healthValues.set(selectedLifeIndex, valueToUse);
            } else {
                rule.healthValues.add(valueToUse);
            }
            sortValues(rule.healthValues);
            TrevorAddonsClient.CONFIG.save();
            syncInputFromSelection();
            statusMessage = "Life updated.";
            return;
        }

        if (append) {
            for (Double value : parsed) {
                if (!containsValue(rule.healthValues, value)) {
                    rule.healthValues.add(value);
                }
            }
            sortValues(rule.healthValues);
            TrevorAddonsClient.CONFIG.save();
            syncInputFromSelection();
            statusMessage = "Values added.";
            return;
        }

        rule.healthValues = new ArrayList<>(parsed);
        sortValues(rule.healthValues);
        TrevorAddonsClient.CONFIG.save();
        syncInputFromSelection();
        statusMessage = "Values replaced.";
    }

    private void applyWildcard() {
        TrevorConfig.Preset preset = selectedPreset();
        if (preset == null || !preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }
        TrevorConfig.EntityRule rule = selectedEntity();
        if (rule == null) {
            statusMessage = "Select a mob first.";
            return;
        }
        rule.matchesAnyHealth = true;
        if (selectionKind == SelectionKind.LIFE && selectedLifeIndex >= 0 && selectedLifeIndex < rule.healthValues.size()) {
            rule.healthValues.remove(selectedLifeIndex);
        }
        selectedLifeIndex = -1;
        sortValues(rule.healthValues);
        TrevorAddonsClient.CONFIG.save();
        syncInputFromSelection();
        statusMessage = "All added.";
    }

    private void deleteSelection() {
        if (selectionKind == SelectionKind.PRESET) {
            TrevorConfig.Preset preset = selectedPreset();
            if (preset == null || !preset.editable) {
                statusMessage = "Default preset cannot be deleted.";
                return;
            }
            TrevorAddonsClient.CONFIG.deletePreset(preset.id);
            TrevorAddonsClient.CONFIG.save();
            syncSelectionToConfig();
            syncInputFromSelection();
            statusMessage = "Preset deleted.";
            return;
        }

        TrevorConfig.Preset preset = selectedPreset();
        if (preset == null || !preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }
        TrevorConfig.EntityRule rule = selectedEntity();
        if (rule == null) {
            return;
        }

        if (selectionKind == SelectionKind.LIFE && selectedLifeIndex == -1 && rule.matchesAnyHealth) {
            rule.matchesAnyHealth = false;
            TrevorAddonsClient.CONFIG.save();
            syncInputFromSelection();
            statusMessage = "All removed.";
            return;
        }

        if (selectionKind == SelectionKind.LIFE && selectedLifeIndex >= 0 && selectedLifeIndex < rule.healthValues.size()) {
            rule.healthValues.remove(selectedLifeIndex);
            selectedLifeIndex = Math.min(selectedLifeIndex, Math.max(0, rule.healthValues.size() - 1));
            TrevorAddonsClient.CONFIG.save();
            syncInputFromSelection();
            statusMessage = "Life removed.";
            return;
        }

        if (selectionKind == SelectionKind.ENTITY && preset.entities.size() > 1) {
            preset.entities.remove(selectedEntityIndex);
            selectedEntityIndex = Math.max(0, Math.min(selectedEntityIndex, preset.entities.size() - 1));
            selectedLifeIndex = -1;
            TrevorAddonsClient.CONFIG.save();
            syncInputFromSelection();
            statusMessage = "Mob removed.";
        }
    }

    private void useSelectedPreset() {
        if (selectionKind == SelectionKind.PRESET) {
            TrevorAddonsClient.CONFIG.setActivePreset(selectedPresetId);
            TrevorAddonsClient.CONFIG.save();
            statusMessage = "Active preset updated.";
            return;
        }
        resetSelectedEntityToDefault();
    }

    private void resetSelectedEntityToDefault() {
        TrevorConfig.Preset preset = selectedPreset();
        if (preset == null || !preset.editable) {
            statusMessage = "Default preset cannot be edited.";
            return;
        }
        TrevorConfig.EntityRule current = selectedEntity();
        if (current == null) {
            statusMessage = "Select a mob first.";
            return;
        }

        String entityId = current.id;
        TrevorConfig.Preset defaults = TrevorConfig.createDefaultPreset();
        TrevorConfig.EntityRule defaultRule = null;
        for (TrevorConfig.EntityRule rule : defaults.entities) {
            if (entityId.equals(rule.id)) {
                defaultRule = rule;
                break;
            }
        }
        if (defaultRule == null) {
            defaultRule = defaults.entities.isEmpty() ? new TrevorConfig.EntityRule(entityId, current.name, List.of()) : defaults.entities.get(0);
        }
        current.healthValues = new ArrayList<>(defaultRule.healthValues);
        TrevorAddonsClient.CONFIG.save();
        syncInputFromSelection();
        statusMessage = current.name + " reset.";
    }

    private void selectRow(TreeRow row) {
        TrevorConfig.Preset preset = presetAt(row.presetIndex);
        if (preset == null) return;
        selectedPresetId = preset.id;

        if (row.kind == TreeKind.PRESET) {
            selectionKind = SelectionKind.PRESET;
            selectedEntityIndex = 0;
            selectedLifeIndex = -1;
        } else if (row.kind == TreeKind.ENTITY) {
            selectionKind = SelectionKind.ENTITY;
            selectedEntityIndex = row.entityIndex;
            selectedLifeIndex = -1;
        } else {
            selectionKind = SelectionKind.LIFE;
            selectedEntityIndex = row.entityIndex;
            selectedLifeIndex = row.lifeIndex;
        }
        mobTypeFocused = row.kind == TreeKind.ENTITY;
        inputFocused = row.kind == TreeKind.LIFE;
        presetNameFocused = row.kind == TreeKind.PRESET;
        syncInputFromSelection();
    }

    private void toggleRow(TreeRow row) {
        TrevorConfig.Preset preset = presetAt(row.presetIndex);
        if (preset == null) return;
        if (row.kind == TreeKind.PRESET) {
            if (isExpandedPreset(preset.id)) {
                expandedPresetIds.remove(preset.id);
            } else {
                expandedPresetIds.add(preset.id);
            }
            return;
        }
        if (row.kind == TreeKind.ENTITY) {
            String key = entityKey(preset.id, row.entityIndex);
            if (isExpandedEntity(preset.id, row.entityIndex)) {
                expandedEntityIds.remove(key);
            } else {
                expandedEntityIds.add(key);
            }
        }
    }

    private void handleRowPlus(TreeRow row) {
        if (row.kind == TreeKind.PRESET) {
            selectRow(row);
            addMobToSelectedPreset();
        } else if (row.kind == TreeKind.ENTITY) {
            selectRow(row);
            addLifeToSelectedEntity();
        }
    }

    private void handleRowDelete(TreeRow row) {
        selectRow(row);
        deleteSelection();
    }

    private void syncSelectionToConfig() {
        selectedPresetId = TrevorAddonsClient.CONFIG.activePresetId;
        if (presetAt(selectedPresetId) == null) {
            selectedPresetId = TrevorConfig.DEFAULT_PRESET_ID;
        }
        selectedEntityIndex = clampInt(selectedEntityIndex, 0, Math.max(0, selectedPreset().entities.size() - 1));
        selectedLifeIndex = -1;
        selectionKind = SelectionKind.PRESET;
    }

    private void syncInputFromSelection() {
        TrevorConfig.EntityRule rule = selectedEntity();
        TrevorConfig.Preset preset = selectedPreset();
        presetNameInput = preset == null ? "" : preset.name;
        mobTypeInput = selectionKind == SelectionKind.PRESET || rule == null ? "" : rule.id;
        if (selectionKind == SelectionKind.LIFE && rule != null && selectedLifeIndex == -1 && rule.matchesAnyHealth) {
            inputText = "All";
            return;
        }
        if (selectionKind == SelectionKind.LIFE && rule != null && selectedLifeIndex >= 0 && selectedLifeIndex < rule.healthValues.size()) {
            inputText = formatLife(rule.healthValues.get(selectedLifeIndex));
            return;
        }
        if (rule != null && (selectionKind == SelectionKind.ENTITY || selectionKind == SelectionKind.LIFE)) {
            String lives = formatLives(rule.healthValues);
            inputText = rule.matchesAnyHealth ? (lives.isEmpty() ? "All" : "All, " + lives) : lives;
            return;
        }
        inputText = "";
    }

    private String displayNameFromKey(String key) {
        if (key == null || key.isBlank()) {
            return "Unknown";
        }
        if (TrevorConfig.HORSE_FAMILY_KEY.equals(key)) {
            return "All";
        }
        int colon = key.indexOf(':');
        String path = colon >= 0 ? key.substring(colon + 1) : key;
        path = path.replace('_', ' ');
        StringBuilder out = new StringBuilder(path.length());
        boolean cap = true;
        for (char c : path.toCharArray()) {
            if (cap && Character.isLetter(c)) {
                out.append(Character.toUpperCase(c));
                cap = false;
            } else {
                out.append(c);
            }
            if (c == ' ') {
                cap = true;
            }
        }
        return out.length() == 0 ? key : out.toString();
    }

    private TrevorConfig.Preset selectedPreset() {
        return presetAt(selectedPresetId);
    }

    private TrevorConfig.Preset presetAt(String presetId) {
        if (presetId == null) return null;
        for (TrevorConfig.Preset preset : TrevorAddonsClient.CONFIG.presets) {
            if (presetId.equals(preset.id)) {
                return preset;
            }
        }
        return null;
    }

    private TrevorConfig.Preset presetAt(int index) {
        if (index < 0 || index >= TrevorAddonsClient.CONFIG.presets.size()) {
            return null;
        }
        return TrevorAddonsClient.CONFIG.presets.get(index);
    }

    private TrevorConfig.EntityRule selectedEntity() {
        TrevorConfig.Preset preset = selectedPreset();
        if (preset == null) return null;
        if (selectedEntityIndex < 0 || selectedEntityIndex >= preset.entities.size()) return null;
        return preset.entities.get(selectedEntityIndex);
    }

    private String selectedSelectionLabel() {
        TrevorConfig.Preset preset = selectedPreset();
        if (preset == null) {
            return "Nothing selected";
        }
        if (selectionKind == SelectionKind.PRESET) {
            return "Preset: " + preset.name;
        }
        TrevorConfig.EntityRule rule = selectedEntity();
        if (rule == null) {
            return "Preset: " + preset.name;
        }
        if (selectionKind == SelectionKind.ENTITY) {
            return "Mob: " + rule.name;
        }
        if (rule.matchesAnyHealth && selectedLifeIndex == -1) {
            return "Life: All";
        }
        return "Life: " + formatLife(selectedLifeValue(rule));
    }

    private Double selectedLifeValue(TrevorConfig.EntityRule rule) {
        if (rule == null || selectedLifeIndex < 0 || selectedLifeIndex >= rule.healthValues.size()) {
            return null;
        }
        return rule.healthValues.get(selectedLifeIndex);
    }

    private boolean isExpandedPreset(String presetId) {
        return expandedPresetIds.contains(presetId);
    }

    private boolean isExpandedEntity(String presetId, int entityIndex) {
        return expandedEntityIds.contains(entityKey(presetId, entityIndex));
    }

    private String entityKey(String presetId, int entityIndex) {
        return presetId + ":" + entityIndex;
    }

    private int layoutHeight() {
        return embedded ? 430 : WINDOW_H;
    }

    private void rebuildTree() {
        if (!treeNeedsRefresh) return;
        treeNeedsRefresh = false;
    }

    private void updateLayout() {
        int panelLeft = this.width / 2 - WINDOW_W / 2;
        int panelTop = this.height / 2 - layoutHeight() / 2;

        closeRect = new Rect(panelLeft + WINDOW_W - 100, panelTop + layoutHeight() - 28, 88, 18);
        activePresetChipRect = new Rect(panelLeft + WINDOW_W - 188, panelTop + 14, 172, 20);

        visualsToggleRect = new Rect(panelLeft + 12, panelTop + 52, WINDOW_W - 24, 22);
        visualsHeaderRect = visualsToggleRect;
        visualsBodyRect = new Rect(panelLeft + 12, panelTop + 76, WINDOW_W - 24, 142);

        if (embedded) {
            presetsToggleRect = new Rect(panelLeft + 12, panelTop + 12, WINDOW_W - 24, 22);
            presetsHeaderRect = presetsToggleRect;
            presetsAddRect = new Rect(panelLeft + WINDOW_W - 42, panelTop + 54, 16, 16);
            footerRect = new Rect(panelLeft + 12, panelTop + layoutHeight() - 64, WINDOW_W - 24, 52);
            presetsBodyRect = new Rect(panelLeft + 12, panelTop + 84, WINDOW_W - 24, footerRect.y - (panelTop + 84) - 8);
        } else {
            presetsToggleRect = new Rect(panelLeft + 12, panelTop + 226, WINDOW_W - 24, 22);
            presetsHeaderRect = presetsToggleRect;
            presetsAddRect = new Rect(panelLeft + WINDOW_W - 42, panelTop + 229, 16, 16);
            presetsBodyRect = new Rect(panelLeft + 12, panelTop + 250, WINDOW_W - 24, 136);
            footerRect = new Rect(panelLeft + 12, panelTop + 390, WINDOW_W - 24, 112);
        }

        espToggleRect = new Rect(visualsBodyRect.x + 12, visualsBodyRect.y + 14, 210, 24);
        tracerToggleRect = new Rect(visualsBodyRect.x + 12, visualsBodyRect.y + 42, 210, 24);
        thicknessTrackRect = new Rect(visualsBodyRect.x + 12, visualsBodyRect.y + 82, 184, 16);
        svRect = new Rect(visualsBodyRect.x + 220, visualsBodyRect.y + 14, 160, 116);
        hueRect = new Rect(visualsBodyRect.x + 388, visualsBodyRect.y + 14, 14, 116);
    }

    private void markConfigDirty() {
        configDirty = true;
        treeNeedsRefresh = true;
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

    private List<Double> parseLifeValues(String input) {
        LifeParseResult result = parseLifeValuesWithWildcard(input);
        return result.values();
    }

    private LifeParseResult parseLifeValuesWithWildcard(String input) {
        List<Double> values = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return new LifeParseResult(values, false);
        }

        String[] parts = input.split("[,\\s]+");
        boolean wildcard = false;
        for (String part : parts) {
            if (part.isBlank()) continue;
            if ("*".equals(part.trim()) || "All".equalsIgnoreCase(part.trim())) {
                wildcard = true;
                continue;
            }
            try {
                values.add(Double.parseDouble(part));
            } catch (NumberFormatException ignored) {
                return new LifeParseResult(List.of(), false);
            }
        }
        return new LifeParseResult(values, wildcard);
    }

    private boolean containsWildcardToken(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String[] parts = input.split("[,\\s]+");
        for (String part : parts) {
            String trimmed = part.trim();
            if ("*".equals(trimmed) || "All".equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsValue(List<Double> values, Double candidate) {
        if (values == null) return false;
        for (Double value : values) {
            if (value == null || candidate == null) continue;
            if (!Double.isNaN(value) && !Double.isNaN(candidate) && Math.abs(value - candidate) < 0.01d) {
                return true;
            }
        }
        return false;
    }

    private static void sortValues(List<Double> values) {
        values.sort((a, b) -> {
            boolean an = a == null;
            boolean bn = b == null;
            if (an && bn) return 0;
            if (an) return 1;
            if (bn) return -1;
            return Double.compare(a, b);
        });
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
        if (value == null) return "";
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.0001d) {
            return String.format(Locale.ROOT, "%.0f", rounded);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String formatThickness(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private record LifeParseResult(List<Double> values, boolean wildcard) {
    }

    private static boolean isAllowedLifeChar(char chr) {
        return Character.isDigit(chr) || chr == ',' || chr == '.' || chr == ' ' || chr == '-' || chr == '*';
    }

    private static boolean isAllowedTypeChar(char chr) {
        return Character.isLetterOrDigit(chr) || chr == ':' || chr == '_' || chr == '.' || chr == '-';
    }

    private static boolean isAllowedNameChar(char chr) {
        return !Character.isISOControl(chr);
    }

    private String trim(String text, int width) {
        return mc().textRenderer.trimToWidth(text, width);
    }

    private static MinecraftClient mc() {
        return MinecraftClient.getInstance();
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

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    private String hexColor() {
        return String.format(Locale.ROOT, "#%06X", TrevorAddonsClient.CONFIG.tracerLineColor & 0xFFFFFF);
    }

    private enum SelectionKind {
        PRESET,
        ENTITY,
        LIFE
    }

    private enum TreeKind {
        PRESET,
        ENTITY,
        LIFE
    }

    private static final class TreeRow {
        private final TreeKind kind;
        private final int presetIndex;
        private final int entityIndex;
        private final int lifeIndex;
        private int x;
        private int y;
        private int w;
        private int h;
        private int depth;
        private String title = "";
        private String subtitle = "";
        private boolean expanded;
        private boolean selected;
        private boolean active;
        private boolean editable;
        private boolean canAdd;
        private boolean canDelete;
        private boolean canUse;
        private boolean wildcard;
        private Rect rect = Rect.empty();
        private Rect addRect = Rect.empty();
        private Rect deleteRect = Rect.empty();

        private TreeRow(TreeKind kind, int presetIndex, int entityIndex, int lifeIndex, int x, int y, int w, int h) {
            this.kind = kind;
            this.presetIndex = presetIndex;
            this.entityIndex = entityIndex;
            this.lifeIndex = lifeIndex;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
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

