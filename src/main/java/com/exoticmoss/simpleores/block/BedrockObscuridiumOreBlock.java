package com.exoticmoss.simpleores.block;

import com.exoticmoss.simpleores.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class BedrockObscuridiumOreBlock extends Block {

    // requiresCorrectToolForDrops + no "mineable" tag: no tool can harvest it, so players never get
    // its drops by mining it (NeoForge's canHarvestBlock(...) = false does not exist on Fabric)
    public BedrockObscuridiumOreBlock(Properties properties) {
        super(properties.strength(9999f, 15).sound(SoundType.STONE).requiresCorrectToolForDrops());
    }

    // Called after an explosion removed the block (vanilla already dropped its loot table)
    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        double chance = Math.random();
        if (chance <= 0.33) {
            popResource(level, pos, new ItemStack(ModItems.RAW_OBSCURIDIUM.get()));
        }
        super.wasExploded(level, pos, explosion);
    }
}
