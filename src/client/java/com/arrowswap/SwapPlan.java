package com.arrowswap;

public final class SwapPlan {
    public static final int OFFHAND = 40;
    public static final int FIRST_MAIN = 9;
    public static final int END_MAIN = 36;

    public enum Type { NO_CHANGE, HOTBAR_SWAP, OFFHAND_SWAP, PICKUP_SWAP, INVALID }

    public record Plan(Type type, int clickedInventoryIndex, int swapKey) { }

    private SwapPlan() { }

    public static Plan between(int desired, int current) {
        if (desired < 0 || desired >= END_MAIN || !validPrioritySlot(current)) return new Plan(Type.INVALID, -1, -1);
        if (desired == current) return new Plan(Type.NO_CHANGE, -1, -1);
        if (current == OFFHAND) return new Plan(Type.OFFHAND_SWAP, desired, OFFHAND);
        if (current < FIRST_MAIN) return new Plan(Type.HOTBAR_SWAP, desired, current);
        if (desired < FIRST_MAIN) return new Plan(Type.HOTBAR_SWAP, current, desired);
        return new Plan(Type.PICKUP_SWAP, desired, -1);
    }

    static boolean validPrioritySlot(int current) {
        return (current >= 0 && current < END_MAIN) || current == OFFHAND;
    }
}
