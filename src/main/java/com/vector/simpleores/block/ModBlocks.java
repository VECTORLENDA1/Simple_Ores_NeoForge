package com.vector.simpleores.block;


import com.vector.simpleores.block.custom.SimpleCraftingTable;
import com.vector.simpleores.block.custom.UltraCraftingTable;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.vector.simpleores.item.ModItems;

import java.util.function.Supplier;

import static com.vector.simpleores.SimpleOres.MODID;


public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);

    public static final DeferredBlock<Block> END_STONE_CELESTINE_ORE = registerBlockItem("end_stone_celestine_ore",
            () -> new DropExperienceBlock(UniformInt.of(5,10),BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> NEXALITE_BLOCK = registerBlockItem("nexalite_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE)
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<Block> IGNITHRA_BLOCK = registerBlockItem("ignithra_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<Block> DEEPSLATE_IGNITHRA_ORE = registerBlockItem("deepslate_ignithra_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> IGNITHRA_ORE = registerBlockItem("ignithra_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(1.5f,6).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> RAW_IGNITHRA_BLOCK = registerBlockItem("raw_ignithra_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5f,6).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> ASTRALITE_BLOCK = registerBlockItem("astralite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<Block> DEEPSLATE_ASTRALITE_ORE = registerBlockItem("deepslate_astralite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> END_STONE_ZENITHRA_ORE = registerBlockItem("end_stone_zenithra_ore",
            () -> new DropExperienceBlock(UniformInt.of(5,10),BlockBehaviour.Properties.of()
                    .strength(5f,6).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> RAW_ASTRALITE_BLOCK = registerBlockItem("raw_astralite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5f,6).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> ZENITHRA_BLOCK = registerBlockItem("zenithra_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));
    public static final DeferredBlock<Block> RAW_NEXALITE_BLOCK = registerBlockItem("raw_nexalite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5f,6).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> CELESTINE_BLOCK = registerBlockItem("celestine_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));
    public static final DeferredBlock<Block> DEEPSLATE_NEXALITE_ORE = registerBlockItem("deepslate_nexalite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> NETHER_ANTRACITE_ORE = registerBlockItem("nether_antracite_ore",
            () -> new DropExperienceBlock(UniformInt.of(1,4),BlockBehaviour.Properties.of()
                    .strength(1f).requiresCorrectToolForDrops().sound(SoundType.NETHERRACK)));
    public static final DeferredBlock<Block> ANTRACITE_BLOCK = registerBlockItem("antracite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> RAW_OBSCURIDIUM_BLOCK = registerBlockItem("raw_obscuridium_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(6f,20).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> OBSCURITE_BLOCK = registerBlockItem("obscurite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5f,10).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<Block> OBSCURIDIUM_BLOCK = registerBlockItem("obscuridium_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(6f,20).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<Block> BEDROCK_OBSCURIDIUM_ORE = registerBlockItem("bedrock_obscuridium_ore",
            () -> new BedrockObscuridiumOreBlock(BlockBehaviour.Properties.of()
                    .strength(9999f).sound(SoundType.STONE)));


    //Entity Blocks
    public static final DeferredBlock<Block> SIMPLE_CRAFTING_TABLE = registerBlockItem("simple_crafting_table",
            () -> new SimpleCraftingTable(BlockBehaviour.Properties.of().strength(2.5f,10)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final DeferredBlock<Block> ULTRA_CRAFTING_TABLE = registerBlockItem("ultra_crafting_table",
            () -> new UltraCraftingTable(BlockBehaviour.Properties.of().strength(2.5f,10)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));


    public static <T extends Block> DeferredBlock<T> registerBlockItem(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    public static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void Register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
