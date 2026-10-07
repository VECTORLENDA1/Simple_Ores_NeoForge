package com.vector.simpleores.block;


import com.vector.simpleores.block.custom.AtomicCraftingTable;
import com.vector.simpleores.block.custom.SimpleCraftingTable;
import com.vector.simpleores.block.custom.UltraCraftingTable;
import com.vector.simpleores.gravity.CoreTier;
import com.vector.simpleores.gravity.GravityCoreBlock;
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

import java.util.function.Function;
import java.util.function.Supplier;

import static com.vector.simpleores.SimpleOres.MODID;


public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);

    public static final DeferredBlock<Block> END_STONE_CELESTINE_ORE = registerBlockItem("end_stone_celestine_ore", p -> new DropExperienceBlock(UniformInt.of(5,10), p),
            () -> BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<Block> NEXALITE_BLOCK = registerBlockItem("nexalite_block", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE)
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<Block> IGNITHRA_BLOCK = registerBlockItem("ignithra_block", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<Block> DEEPSLATE_IGNITHRA_ORE = registerBlockItem("deepslate_ignithra_ore", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> IGNITHRA_ORE = registerBlockItem("ignithra_ore", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(1.5f,6).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<Block> RAW_IGNITHRA_BLOCK = registerBlockItem("raw_ignithra_block", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(5f,6).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<Block> ASTRALITE_BLOCK = registerBlockItem("astralite_block", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<Block> DEEPSLATE_ASTRALITE_ORE = registerBlockItem("deepslate_astralite_ore", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> END_STONE_ZENITHRA_ORE = registerBlockItem("end_stone_zenithra_ore", p -> new DropExperienceBlock(UniformInt.of(5,10), p),
            () -> BlockBehaviour.Properties.of()
                    .strength(5f,6).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<Block> RAW_ASTRALITE_BLOCK = registerBlockItem("raw_astralite_block", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(5f,6).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<Block> ZENITHRA_BLOCK = registerBlockItem("zenithra_block", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.AMETHYST));
    public static final DeferredBlock<Block> RAW_NEXALITE_BLOCK = registerBlockItem("raw_nexalite_block", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(5f,6).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<Block> CELESTINE_BLOCK = registerBlockItem("celestine_block", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.AMETHYST));
    public static final DeferredBlock<Block> DEEPSLATE_NEXALITE_ORE = registerBlockItem("deepslate_nexalite_ore", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> NETHER_ANTRACITE_ORE = registerBlockItem("nether_antracite_ore", p -> new DropExperienceBlock(UniformInt.of(1,4), p),
            () -> BlockBehaviour.Properties.of()
                    .strength(1f).requiresCorrectToolForDrops().sound(SoundType.NETHERRACK));
    public static final DeferredBlock<Block> ANTRACITE_BLOCK = registerBlockItem("antracite_block", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(3f,6).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<Block> RAW_OBSCURIDIUM_BLOCK = registerBlockItem("raw_obscuridium_block", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(6f,20).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<Block> OBSCURITE_BLOCK = registerBlockItem("obscurite_block", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(5f,10).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<Block> OBSCURIDIUM_BLOCK = registerBlockItem("obscuridium_block", Block::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(6f,20).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<Block> BEDROCK_OBSCURIDIUM_ORE = registerBlockItem("bedrock_obscuridium_ore", BedrockObscuridiumOreBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(9999f).sound(SoundType.STONE));


    //Entity Blocks
    // (The 3 crafting tables are DISABLED: still registered, but with no recipe and hidden from the creative tab)
    public static final DeferredBlock<Block> SIMPLE_CRAFTING_TABLE = registerBlockItem("simple_crafting_table", SimpleCraftingTable::new,
            () -> BlockBehaviour.Properties.of().strength(2.5f,10)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL));

    public static final DeferredBlock<Block> ULTRA_CRAFTING_TABLE = registerBlockItem("ultra_crafting_table", UltraCraftingTable::new,
            () -> BlockBehaviour.Properties.of().strength(2.5f,10)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL));

    public static final DeferredBlock<Block> ATOMIC_CRAFTING_TABLE = registerBlockItem("atomic_crafting_table", AtomicCraftingTable::new,
            () -> BlockBehaviour.Properties.of().strength(2.5f,10)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL));

    //Gravity Cores (one per tier)
    public static final DeferredBlock<Block> GRAVITY_CORE_SUN = registerGravityCore(CoreTier.SUN);
    public static final DeferredBlock<Block> GRAVITY_CORE_RED_GIANT = registerGravityCore(CoreTier.RED_GIANT);
    public static final DeferredBlock<Block> GRAVITY_CORE_PULSAR = registerGravityCore(CoreTier.PULSAR);
    public static final DeferredBlock<Block> GRAVITY_CORE_BLACK_HOLE = registerGravityCore(CoreTier.BLACK_HOLE);

    // Creates the block "gravity_core_<tier>" (e.g. gravity_core_sun)
    private static DeferredBlock<Block> registerGravityCore(CoreTier tier) {
        return registerBlockItem("gravity_core_" + tier.id, properties -> new GravityCoreBlock(tier, properties),
                () -> BlockBehaviour.Properties.of().strength(3f, 6).requiresCorrectToolForDrops()
                        .sound(SoundType.AMETHYST).noOcclusion().lightLevel(state -> tier.light));
    }


    // The Properties must contain the block ID: registerBlock(...) takes care of that automatically
    public static <T extends Block> DeferredBlock<T> registerBlockItem(String name, Function<BlockBehaviour.Properties, T> block,
                                                                       Supplier<BlockBehaviour.Properties> properties) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, block, properties);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    public static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.registerSimpleBlockItem(name, block);
    }

    public static void Register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
