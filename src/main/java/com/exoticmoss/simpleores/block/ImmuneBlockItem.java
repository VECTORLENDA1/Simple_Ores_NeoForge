package com.exoticmoss.simpleores.block;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/// The item of the Gravity Cores and of the Magnetar: on the ground it is never destroyed by the damage in the tag
/// ModBlocks.GRAVITY_CORE_IMMUNE (fire, lava, explosions, lightning, cactus...).
public class ImmuneBlockItem extends BlockItem {
    public ImmuneBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canBeHurtBy(DamageSource source) {
        return !source.is(ModBlocks.GRAVITY_CORE_IMMUNE) && super.canBeHurtBy(source);
    }
}
