package com.earthpol.townyports.listener;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.town.TownMergeEvent;
import com.palmergames.bukkit.towny.object.*;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownMergeEventListener implements Listener {

    @EventHandler
    public void onTownMergeEvent(TownMergeEvent event) {
        Town succumbingTown = TownyAPI.getInstance().getTown(event.getSuccumbingTownUUID());
        if (succumbingTown == null)
            return;

        for (TownBlock townBlock : succumbingTown.getTownBlocks()) {
            try {
                TownBlockType type = townBlock.getType();
                if (type != null && type.getName().equalsIgnoreCase("port")) {
                    townBlock.setType(TownBlockTypeHandler.getType("default"));
                    townBlock.save(); // persist change
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[TownyPorts] Failed to reset port block at " +
                        townBlock.getWorldCoord() + ": " + e.getMessage());
            }
        }

        Bukkit.getLogger().info("[TownPorts] Reset all 'port' blocks in " + succumbingTown.getName() + " to default after a merger.");
    }
}
