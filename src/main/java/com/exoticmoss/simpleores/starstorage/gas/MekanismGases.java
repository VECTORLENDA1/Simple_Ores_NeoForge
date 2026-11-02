package com.exoticmoss.simpleores.starstorage.gas;

import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/// The gases of Mekanism (its chemicals), with its API. Only used when Mekanism is installed (see Gases).
final class MekanismGases {
    /// The same capability Mekanism gives its blocks (by name).
    private static final BlockCapability<IChemicalHandler, Direction> CHEMICAL =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"), IChemicalHandler.class);

    private MekanismGases() {
    }

    private static @Nullable Chemical chemical(ResourceLocation id) {
        Chemical chemical = MekanismAPI.CHEMICAL_REGISTRY.get(id);
        return chemical == null || chemical.isEmptyType() ? null : chemical;
    }

    static @Nullable Gases.Tanks tanks(Level level, BlockPos pos, @Nullable Direction side) {
        IChemicalHandler handler = level.getCapability(CHEMICAL, pos, side);
        return handler == null || handler.getChemicalTanks() <= 0 ? null : new Handler(handler);
    }

    static Component name(ResourceLocation id) {
        Chemical chemical = chemical(id);
        return chemical == null ? Component.literal(id.toString()) : chemical.getTextComponent();
    }

    static int color(ResourceLocation id) {
        Chemical chemical = chemical(id);
        return chemical == null ? 0xFFA0A0A0 : 0xFF000000 | chemical.getTint();
    }

    static @Nullable Gas fromIngredient(Object ingredient) {
        if (!(ingredient instanceof ChemicalStack stack) || stack.isEmpty()) return null;
        return new Gas(stack.getChemical().getRegistryName(), stack.getAmount());
    }

    /// The tanks of a block of Mekanism (or of another mod with its gases).
    private record Handler(IChemicalHandler handler) implements Gases.Tanks {
        @Override
        public int count() {
            return handler.getChemicalTanks();
        }

        @Override
        public @Nullable ResourceLocation gas(int tank) {
            ChemicalStack stack = handler.getChemicalInTank(tank);
            return stack.isEmpty() ? null : stack.getChemical().getRegistryName();
        }

        @Override
        public long amount(int tank) {
            return handler.getChemicalInTank(tank).getAmount();
        }

        @Override
        public long capacity(int tank) {
            return handler.getChemicalTankCapacity(tank);
        }

        @Override
        public long fill(ResourceLocation gas, long amount, boolean simulate) {
            Chemical chemical = chemical(gas);
            if (chemical == null || amount <= 0) return 0;
            ChemicalStack left = handler.insertChemical(chemical.getStack(amount), simulate ? Action.SIMULATE : Action.EXECUTE);
            return amount - left.getAmount();
        }

        @Override
        public long drain(ResourceLocation gas, long amount, boolean simulate) {
            Chemical chemical = chemical(gas);
            if (chemical == null || amount <= 0) return 0;
            return handler.extractChemical(chemical.getStack(amount), simulate ? Action.SIMULATE : Action.EXECUTE).getAmount();
        }
    }
}
