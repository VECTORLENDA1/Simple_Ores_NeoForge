// Copy of a small part of the Just Enough Resources API, only used to COMPILE the JER plugin of Simple Ores.
// It is NOT inside the Simple Ores jar: in the game the real classes of JER are used.
package jeresources.api;

import jeresources.api.distributions.DistributionBase;
import jeresources.api.drop.LootDrop;
import jeresources.api.restrictions.Restriction;
import net.minecraft.world.item.ItemStack;

public interface IWorldGenRegistry {
    void register(ItemStack block, ItemStack deepSlateBlock, DistributionBase distribution, Restriction restriction, boolean silktouch, LootDrop... drops);

    void register(ItemStack block, DistributionBase distribution, Restriction restriction, boolean silktouch, LootDrop... drops);
}
