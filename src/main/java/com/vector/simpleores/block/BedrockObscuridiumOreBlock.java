package com.vector.simpleores.block;

import com.vector.simpleores.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class BedrockObscuridiumOreBlock extends Block {

    public BedrockObscuridiumOreBlock(Properties properties) {
        super(properties.strength(9999f, 15).sound(SoundType.STONE));
    }

    @Override
    public void onBlockExploded(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion) {
        double chance = Math.random();
        if (chance <= 0.30) {
            popResource(level, pos, new ItemStack(ModItems.RAW_OBSCURIDIUM.get()));
        }
        level.removeBlock(pos, false);
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        return false;
    }
}