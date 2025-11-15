package com.vector.simpleores.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import com.vector.simpleores.item.ModItems;

import java.util.List;

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
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        pTooltipComponents.add(Component.translatable("bedrockobscuridiumoreblock").withStyle(ChatFormatting.AQUA));

        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        return false;
    }
}
