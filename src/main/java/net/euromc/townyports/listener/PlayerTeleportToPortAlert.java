package net.euromc.townyports.listener;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.TownBlock;


import net.euromc.townyports.PortsMain;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportToPortAlert implements Listener {
    @EventHandler
    public void onTP(PlayerTeleportEvent event) {

        if (!PortsMain.getCustomConfig().getBoolean("enable-port-arrival-alert")) {
            return;
        }

        if(!event.getCause().equals(PlayerTeleportEvent.TeleportCause.PLUGIN))
            return;

        TownBlock toTB = TownyAPI.getInstance().getTownBlock(event.getTo());
        TownBlock fromTB = TownyAPI.getInstance().getTownBlock(event.getFrom());
        if (toTB == null || fromTB == null) // Wilderness involced.
            return;

        if (!toTB.getTypeName().equals("port") || !fromTB.getTypeName().equals("port"))
            return;

        TownyMessaging.sendPrefixedTownMessage(TownyAPI.getInstance().getTown(event.getTo()), "§4" + event.getPlayer().getName() + " has arrived at the town port.");
    }
}
