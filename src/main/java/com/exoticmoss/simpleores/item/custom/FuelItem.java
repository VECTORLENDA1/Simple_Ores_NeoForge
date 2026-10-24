package com.exoticmoss.simpleores.item.custom;

import net.minecraft.world.item.Item;

public class FuelItem extends Item {
    private int burntime = 0;

    public FuelItem(Properties pProperties, int burntime) {
        super(pProperties);
        this.burntime = burntime;
    }

    // The burn time is given to the furnaces through Fabric's FuelValueEvents (see ModItems)
    public int getBurnTime() {
        return this.burntime;
    }

}
