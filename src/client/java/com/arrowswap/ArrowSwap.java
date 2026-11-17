package com.arrowswap;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public final class ArrowSwap {
    public static final KeyMapping FIRST = new KeyMapping("key.arrowswap.first", GLFW.GLFW_KEY_G, KeyMapping.Category.GAMEPLAY);
    public static final KeyMapping SECOND = new KeyMapping("key.arrowswap.second", GLFW.GLFW_KEY_V, KeyMapping.Category.GAMEPLAY);
    private static final ItemStack BOW = new ItemStack(Items.BOW);

    private ArrowSwap() { }

    public static void tick(Minecraft client) {
        boolean first = drain(FIRST), second = drain(SECOND);
        if ((!first && !second) || client.screen != null || client.player == null
                || client.gameMode == null || client.getConnection() == null) return;
        select(client, second ? 1 : 0);
    }

    private static boolean drain(KeyMapping mapping) {
        boolean pressed = false;
        while (mapping.consumeClick()) pressed = true;
        return pressed;
    }

    public static boolean isArrow(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW));
    }

    static boolean same(ItemStack a, ItemStack b) {
        return isArrow(a) && isArrow(b) && ItemStack.isSameItemSameComponents(a, b);
    }

    private static void select(Minecraft mc, int preset) {
        LocalPlayer player = mc.player;
        if (player.isSpectator() || !player.isAlive() || player.containerMenu != player.inventoryMenu) return;
        if (player.isUsingItem()) {
            say(player, Component.literal("Release your item before switching arrows.").withColor(0xFFD079));
            return;
        }
        InventoryMenu menu = player.inventoryMenu;
        if (!menu.getCarried().isEmpty()) {
            say(player, Component.literal("Empty the inventory cursor first.").withColor(0xFFD079));
            return;
        }

        var inv = player.getInventory();
        ItemStack want = ArrowConfig.get().getPreset(mc, preset);
        ItemStack current = player.getProjectile(BOW);
        if (same(want, current) && hasReference(inv, current)) {
            selected(player, preset, current);
            return;
        }

        int sourceIndex = -1;
        for (int i = 0; i < SwapPlan.END_MAIN; i++) {
            var stack = inv.getItem(i);
            if (same(stack, want) && (sourceIndex < 0 || stack.getCount() > inv.getItem(sourceIndex).getCount())) {
                sourceIndex = i;
            }
        }
        if (sourceIndex < 0) {
            say(player, Component.literal("Missing: ").append(want.getHoverName()).withColor(0xFFFF8585));
            return;
        }
        if (current.isEmpty()) return;
        // Resolve the actual inventory reference used by vanilla, including a competing held arrow.
        int priorityIndex = inv.getItem(Inventory.SLOT_OFFHAND) == current ? SwapPlan.OFFHAND : -1;
        if (priorityIndex < 0) {
            for (int i = 0; i < SwapPlan.END_MAIN; i++) if (inv.getItem(i) == current) { priorityIndex = i; break; }
        }
        if (!isArrow(current) || !SwapPlan.validPrioritySlot(priorityIndex)) {
            say(player, Component.literal("An unsupported arrow already has priority.").withColor(0xFFD079));
            return;
        }

        var from = findSlot(menu, inv, sourceIndex);
        var first = findSlot(menu, inv, priorityIndex);
        if (from == null || first == null) return;
        ItemStack toFirst = from.getItem().copy();
        ItemStack toSource = first.getItem().copy();
        if (!canExchange(from, toSource, player) || !canExchange(first, toFirst, player)) return;

        SwapPlan.Plan plan = SwapPlan.between(sourceIndex, priorityIndex);
        if (plan.type() == SwapPlan.Type.OFFHAND_SWAP || plan.type() == SwapPlan.Type.HOTBAR_SWAP) {
            Slot clicked = plan.clickedInventoryIndex() == sourceIndex ? from : first;
            click(mc, menu, clicked, plan.swapKey(), ClickType.SWAP);
        } else if (plan.type() == SwapPlan.Type.PICKUP_SWAP) {
            click(mc, menu, from, 0, ClickType.PICKUP);
            if (ItemStack.matches(menu.getCarried(), toFirst) && from.getItem().isEmpty()
                    && ItemStack.matches(first.getItem(), toSource)) {
                click(mc, menu, first, 0, ClickType.PICKUP);
            }
            // Complete the swap or return the pickup to the original slot, never drop a stack.
            ItemStack cursor = menu.getCarried();
            if (!cursor.isEmpty() && from.getItem().isEmpty() && from.mayPlace(cursor)
                    && cursor.getCount() <= from.getMaxStackSize(cursor)) {
                click(mc, menu, from, 0, ClickType.PICKUP);
            }
        }
        if (menu.getCarried().isEmpty() && ItemStack.matches(first.getItem(), toFirst)
                && ItemStack.matches(from.getItem(), toSource)
                && same(want, player.getProjectile(BOW))) {
            selected(player, preset, player.getProjectile(BOW));
        } else if (!menu.getCarried().isEmpty()) {
            say(player, Component.literal("Swap stopped. Open inventory to return the cursor stack.").withColor(0xFFFF8585));
        } else {
            say(player, Component.literal("Arrow swap could not be applied.").withColor(0xFFFF8585));
        }
    }

    private static boolean hasReference(Inventory inv, ItemStack stack) {
        if (inv.getItem(Inventory.SLOT_OFFHAND) == stack) return true;
        for (int i = 0; i < SwapPlan.END_MAIN; i++) if (inv.getItem(i) == stack) return true;
        return false;
    }

    private static Slot findSlot(InventoryMenu menu, Inventory inv, int index) {
        for (Slot slot : menu.slots) {
            if (slot.container == inv && slot.getContainerSlot() == index) return slot;
        }
        return null;
    }

    private static boolean canExchange(Slot slot, ItemStack stack, LocalPlayer player) {
        return slot.isActive() && slot.mayPickup(player) && slot.mayPlace(stack)
                && stack.getCount() <= Math.min(slot.getMaxStackSize(stack), stack.getMaxStackSize());
    }

    private static void click(Minecraft mc, InventoryMenu menu, Slot slot, int key, ClickType type) {
        if (mc.player != null && mc.player.containerMenu == menu && mc.gameMode != null) {
            mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, key, type, mc.player);
        }
    }

    private static void selected(LocalPlayer player, int preset, ItemStack stack) {
        say(player, Component.literal(preset == 0 ? "Arrow A: " : "Arrow B: ")
                .withColor(preset == 0 ? 0xABF299 : 0xC8B8FF)
                .append(stack.getHoverName().copy().withColor(0xFFFFFF))
                .append(Component.literal(" ×" + stack.getCount()).withColor(0xB5B5B5)));
    }

    private static void say(LocalPlayer player, Component component) {
        player.displayClientMessage(component, true);
    }
}
