package com.exoticmoss.simpleores.block;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/// Bedrock Obscuridium Ore: hidden in the bedrock, at the bottom of the world.
///  - Normal tools can't break it. Only the tools in the item tag "simpleores:mines_obscuridium" can
///    (the Mega Breaker Tools from the Ultimate tier up add themselves to that tag).
///    With them it is as hard as obsidian (MINING_HARDNESS), and Fortune and Silk Touch work.
///  - One TNT is enough to break it: then it has 50% chance to drop Raw Obscuridium.
/// If it is in the last layer of the world (y -64), the bedrock comes back after it breaks (no hole to the void).
/// The drops are in data/simpleores/loot_table/blocks/bedrock_obscuridium_ore.json
public class BedrockObscuridiumOreBlock extends Block {
    /// The tools that can mine it.
    public static final TagKey<Item> MINING_TOOLS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(SimpleOres.MODID, "mines_obscuridium"));
    /// How hard it is to mine with those tools (obsidian = 50).
    public static final float MINING_HARDNESS = 50;
    /// Resistance to explosions: low enough for one TNT to break it.
    public static final float EXPLOSION_RESISTANCE = 4;

    public BedrockObscuridiumOreBlock(Properties properties) {
        super(properties.strength(9999f, EXPLOSION_RESISTANCE).sound(SoundType.STONE));
    }

    /// How fast the player breaks it: nothing without one of the MINING_TOOLS,
    /// with them like a block of MINING_HARDNESS (their speed against obsidian, plus Efficiency).
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        ItemStack tool = player.getMainHandItem();
        if (!tool.is(MINING_TOOLS)) return 0;
        float speed = tool.getDestroySpeed(Blocks.OBSIDIAN.defaultBlockState());
        if (speed > 1) speed += (float) player.getAttributeValue(Attributes.MINING_EFFICIENCY);
        speed *= (float) player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        if (!player.onGround()) speed /= 5;
        return speed / MINING_HARDNESS / 30;
    }

    // ---- The last layer of the world (y -64) never gets a hole: the bedrock comes back

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state);
        keepBottomClosed(level, pos);
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        super.wasExploded(level, pos, explosion);
        keepBottomClosed(level, pos);
    }

    private static void keepBottomClosed(LevelAccessor level, BlockPos pos) {
        if (pos.getY() <= level.getMinY()) level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
    }
}
