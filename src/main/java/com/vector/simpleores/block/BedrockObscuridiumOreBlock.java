package com.vector.simpleores.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import com.vector.simpleores.item.ModItems;

public class BedrockObscuridiumOreBlock extends Block {

    public BedrockObscuridiumOreBlock(Properties properties) {
        super(properties.strength(9999f, 15).sound(SoundType.STONE));
    }

    @Override
    public void onBlockExploded(BlockState state, Level world, BlockPos pos, Explosion explosion) {
        if (!world.isClientSide) {
            double chance = Math.random();
            if (chance <= 0.30) {
                popResource(world, pos, new ItemStack(ModItems.RAW_OBSCURIDIUM.get()));
            }
        }
        world.removeBlock(pos, false);
        super.onBlockExploded(state, world, pos, explosion);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        return false;
    }
}
