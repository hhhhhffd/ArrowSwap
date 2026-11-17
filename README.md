# Arrow Swap 1.0.0

Client-side mod for Minecraft Java **1.21.11**, Fabric Loader **0.18.4+**, and Java 21.
Put the JAR in the client's `mods` folder. Fabric API and a server mod are not
required. It does not aim, draw, or release the bow.

- **G** selects Arrow A, **Poison** by default.
- **V** selects Arrow B, **Slowness** by default.
- Change both shortcuts in **Options → Controls → Key Binds → Gameplay**.
- In optional **Mod Menu 17 → Arrow Swap → Configure**, select A or B and choose
  a preset or an arrow stack already in your inventory. Stored choices include
  custom names and other item components, not just the base item or potion.
  Click **Done** to save `config/arrowswap.json`.

Use the shortcuts with the bow ready but **not drawn**. If an arrow of the chosen
kind is already first, nothing moves. Otherwise the mod puts a matching stack in
the inventory position a vanilla bow would use first, using normal inventory
slot swaps. It leaves your bow and any shield, totem or other non-arrow offhand
item alone. If you already have a competing arrow in the offhand, it swaps that
arrow normally; vanilla gives held ammunition priority.

The short action-bar message shows the selected stack or says which arrow is
missing. Only one swap is done per key press. If the priority stack empties,
vanilla can move on to other ammo; press the shortcut again to select another
stack. Servers that replace ordinary bow ammo selection with custom plugin
logic can ignore vanilla inventory priority.

## Build

Use Java 21: `./gradlew build` (`gradlew.bat build` on Windows).

Standalone swap-plan checks:

```sh
javac -d build/arrow-tests src/client/java/com/arrowswap/SwapPlan.java tests/SwapPlanTest.java
java -cp build/arrow-tests SwapPlanTest
```
