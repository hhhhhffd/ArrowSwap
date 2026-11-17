package com.arrowswap;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

final class ArrowScreen extends Screen {
    private static final int TEXT = 0xFFF0F4EE;
    private static final int MUTE = 0xFFA9B9AE;
    private final Screen parent;
    private final ItemStack[] picked = new ItemStack[2];
    private final int[] amount = new int[2];
    private final List<Choice> choices = new ArrayList<>();
    private int x;
    private int y;
    private int w;
    private int page;
    private int selected;
    private int rows;
    private int rowsTop;
    private int pagerY;
    private int buttonsY;
    private String problem;

    ArrowScreen(Screen parent) {
        super(Component.literal("Arrow Swap Settings"));
        this.parent = parent;
    }

    @Override protected void init() {
        w = Math.min(330, width - 20);
        x = (width - w) / 2;
        y = 8;
        rowsTop = y + 85;
        rows = Math.clamp((height - 144) / 22, 1, 8);
        pagerY = rowsTop + rows * 22 + 1;
        buttonsY = pagerY + 24;
        if (picked[0] == null) {
            picked[0] = ArrowConfig.get().getPreset(minecraft, 0);
            picked[1] = ArrowConfig.get().getPreset(minecraft, 1);
        }
        choices.clear();
        if (minecraft.player != null) {
            var inv = minecraft.player.getInventory();
            for (int i = 0; i < SwapPlan.END_MAIN; i++) addChoice(inv.getItem(i), true);
            addChoice(inv.getItem(Inventory.SLOT_OFFHAND), true);
        }
        addChoice(new ItemStack(Items.ARROW), false);
        addChoice(new ItemStack(Items.SPECTRAL_ARROW), false);
        addChoice(ArrowConfig.standard(0), false);
        addChoice(ArrowConfig.standard(1), false);
        addChoice(PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.HARMING), false);
        addChoice(PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.WEAKNESS), false);
        addChoice(PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.HEALING), false);
        addChoice(PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.SLOW_FALLING), false);
        page = Math.clamp(page, 0, pages() - 1);
        for (int i = 0; i < 2; i++) {
            amount[i] = 0;
            for (var c : choices) if (ArrowSwap.same(c.stack, picked[i])) amount[i] = c.count;
        }

        int left = x + 9, inner = w - 18;
        int half = (inner - 4) / 2;
        for (int slot = 0; slot < 2; slot++) {
            int which = slot;
            var key = slot == 0 ? ArrowSwap.FIRST : ArrowSwap.SECOND;
            Component title = Component.literal((selected == slot ? "› " : "")
                    + (slot == 0 ? "Arrow A  [" : "Arrow B  [")).append(key.getTranslatedKeyMessage()).append("]");
            addRenderableWidget(Button.builder(title, b -> {
                selected = which;
                problem = null;
                rebuildWidgets();
            }).bounds(left + slot * (half + 4), y + 25, half, 19).build());
        }

        int offset = rows * page;
        for (int i = 0; i < rows && offset + i < choices.size(); i++) {
            var choice = choices.get(offset + i);
            int rowY = rowsTop + i * 22;
            var name = Component.literal(ArrowSwap.same(choice.stack, picked[selected]) ? "› " : "")
                    .append(choice.stack.getHoverName()).append(choice.count > 0 ? "  ×" + choice.count : "");
            var b = Button.builder(name, button -> {
                picked[selected] = choice.stack.copyWithCount(1);
                problem = null;
                rebuildWidgets();
            }).bounds(left, rowY, inner, 19).build();
            b.setTooltip(Tooltip.create(choice.stack.getHoverName()));
            addRenderableWidget(b);
        }

        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page--;
            rebuildWidgets();
        }).bounds(left, pagerY, 37, 18).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page++;
            rebuildWidgets();
        }).bounds(x + w - 46, pagerY, 37, 18).build()).active = page + 1 < pages();

        int third = (inner - 8) / 3;
        addRenderableWidget(Button.builder(Component.literal("Default arrows"), b -> {
            picked[0] = ArrowConfig.standard(0);
            picked[1] = ArrowConfig.standard(1);
            problem = null;
            rebuildWidgets();
        }).bounds(left, buttonsY, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> save())
                .bounds(left + third + 4, buttonsY, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(left + 2 * (third + 4), buttonsY, third, 20).build());
    }

    private void addChoice(ItemStack stack, boolean inInventory) {
        if (!ArrowSwap.isArrow(stack)) return;
        for (int i = 0; i < choices.size(); i++) {
            var existing = choices.get(i);
            if (ArrowSwap.same(existing.stack, stack)) {
                if (inInventory) choices.set(i, new Choice(existing.stack, existing.count + stack.getCount()));
                return;
            }
        }
        choices.add(new Choice(stack.copyWithCount(1), inInventory ? stack.getCount() : 0));
    }

    private int pages() { return Math.max(1, (choices.size() + rows - 1) / rows); }

    @Override public void render(GuiGraphics g, int mx, int my, float tickDelta) {
        int bottom = buttonsY + 27;
        g.fill(x, y, x + w, bottom, 0xF2202C26);
        g.renderOutline(x, y, w, bottom - y, 0xFF617869);
        g.fill(x + 1, y + 1, x + w - 1, y + 3, 0xFF92D59C);
        g.drawString(font, getTitle(), x + 9, y + 11, TEXT);
        int half = (w - 22) / 2;
        for (int i = 0; i < 2; i++) {
            int left = x + 10 + i * (half + 4);
            g.renderFakeItem(picked[i], left, y + 46);
            var name = picked[i].getHoverName().getString();
            g.drawString(font, font.plainSubstrByWidth(name, half - 23), left + 19, y + 50, TEXT);
            g.drawString(font, amount[i] > 0 ? amount[i] + " in inventory" : "Not in inventory", left + 19, y + 60, MUTE);
        }
        g.drawString(font, problem == null ? "Choose an arrow for the selected shortcut" : problem,
                x + 9, rowsTop - 12, problem == null ? MUTE : 0xFFFF9C92);
        g.drawCenteredString(font, (page + 1) + " / " + pages(), width / 2, pagerY + 5, TEXT);

        super.render(g, mx, my, tickDelta);
        for (int i = 0; i < rows && page * rows + i < choices.size(); i++) {
            g.renderFakeItem(choices.get(page * rows + i).stack, x + 14, rowsTop + i * 22 + 2);
        }
    }

    private void save() {
        try {
            if (ArrowConfig.get().apply(minecraft, picked[0], picked[1])) {
                onClose();
                return;
            }
        } catch (RuntimeException ignored) { }
        problem = "Could not save these arrow presets.";
    }

    @Override protected void repositionElements() { rebuildWidgets(); }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    private record Choice(ItemStack stack, int count) { }
}
