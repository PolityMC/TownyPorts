package net.euromc.townyports.listener;

import net.euromc.townyports.PortsMain;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownCreate implements Listener {
    @EventHandler
    public void onTown(NewTownEvent event) {
        String sUUID = event.getTown().getUUID().toString();
        if(PortsMain.instance.getConfig().getString(sUUID) == null) {
            PortsMain.instance.getConfig().createSection(sUUID);
            int iVal = PortsMain.getCustomConfig().getInt("default-port-fee");
            PortsMain.instance.getConfig().set(sUUID, Double.valueOf(iVal));
            PortsMain.instance.saveConfig();
        }
    }
}
