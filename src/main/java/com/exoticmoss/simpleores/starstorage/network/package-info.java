/// The network: StarNetwork finds what is connected to a Magnetar (drives, Quasars, Terminal II, linked Magnetars)
/// and puts items / liquids in and takes them out.
///  - StarAccess:     moves items and liquids between the network and any block (NeoForge transfer API)
///  - NetworkHandler: the network seen by the pipes and machines of other mods (through the Binary Star)
///  - StarFluids:     liquids and their buckets
///  - StarChunks:     chunk loading of the Wormhole Links
///  - NetworkMenu:    what every screen of the network (terminal, Quasar) has in common
package com.exoticmoss.simpleores.starstorage.network;
