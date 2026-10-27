/// The star storage (the Magnetar network): an AE2 / Refined Storage-like storage system made of "stars".
/// Each part is in its own package:
///  - network:   the network itself (what is connected, items and liquids in and out, other mods, chunk loading)
///  - magnetar:  the Magnetar Core (the center of a network) and its upgrades
///  - cell:      the Star Cells and Fluid Cells (where everything is kept) and their tiers
///  - drive:     the Stellar Drive (holds the cells)
///  - terminal:  the Stellar Terminal I and II (see and use the network, ask for auto-crafts)
///  - quasar:    the Quasar (keeps the recipes of the auto-craft, runs the long auto-crafts)
///  - autocraft: the planner and the jobs of the auto-craft
///  - binary:    the Binary Star (moves items and liquids between the network and the blocks around it)
///  - render:    how the stars look (drawn by code, not by models)
///  - client:    the screens and everything else that only exists on the player's side
/// Here: the registries of the menus (ModMenus) and of the item data (ModDataComponents).
package com.exoticmoss.simpleores.starstorage;
