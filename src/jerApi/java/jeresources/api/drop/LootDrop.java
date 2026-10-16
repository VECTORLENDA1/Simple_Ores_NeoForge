// Copy of a small part of the Just Enough Resources API, only used to COMPILE the JER plugin of Simple Ores.
// It is NOT inside the Simple Ores jar: in the game the real classes of JER are used.
package jeresources.api.drop;

import jeresources.api.conditionals.Conditional;
import net.minecraft.world.item.ItemStack;

public class LootDrop {
    public LootDrop(ItemStack item, float chance) {
    }

    public LootDrop(ItemStack item, int minDrop, int maxDrop, Conditional... conditionals) {
    }
}
