import com.arrowswap.SwapPlan;

public class SwapPlanTest {
    private static int checks;
    private static void check(boolean good, String message) {
        if (!good) throw new AssertionError(message);
        checks++;
    }

    public static void main(String[] args) {
        for (int source = 0; source < 36; ++source) {
            for (int current = 0; current < 36; ++current) {
                var plan = SwapPlan.between(source, current);
                if (source == current) {
                    check(plan.type() == SwapPlan.Type.NO_CHANGE, "do not move selected ammo");
                } else if (source < 9 || current < 9) {
                    check(plan.type() == SwapPlan.Type.HOTBAR_SWAP, "use vanilla hotbar swap");
                    check(plan.swapKey() < 9 && plan.swapKey() >= 0, "hotbar key range");
                    check(plan.clickedInventoryIndex() != plan.swapKey(), "swap different slots");
                    check((plan.clickedInventoryIndex() == source && plan.swapKey() == current)
                            || (plan.clickedInventoryIndex() == current && plan.swapKey() == source), "both chosen slots");
                } else {
                    check(plan.type() == SwapPlan.Type.PICKUP_SWAP, "ordinary main slot pair");
                    check(plan.clickedInventoryIndex() == source, "only selected source pickup");
                }
            }
            var plan = SwapPlan.between(source, 40);
            check(plan.type() == SwapPlan.Type.OFFHAND_SWAP && plan.clickedInventoryIndex() == source && plan.swapKey() == 40,
                    "only swap offhand when active arrow is already there");
        }
        for (int i : new int[]{-1, 36, 37, 38, 39, 41, 99}) {
            check(SwapPlan.between(3, i).type() == SwapPlan.Type.INVALID, "do not move unrelated equipment");
            check(SwapPlan.between(i, 0).type() == SwapPlan.Type.INVALID, "sources limited to 36 slots");
        }
        System.out.println("Passed " + checks + " inventory swap plan checks.");
    }
}
