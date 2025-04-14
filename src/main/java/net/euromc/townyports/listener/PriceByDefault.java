package net.euromc.townyports.listener;

import net.euromc.townyports.PortsMain;

import com.palmergames.bukkit.towny.event.PlotChangeTypeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PriceByDefault implements Listener {
    @EventHandler
    public void onPlotChangeType(PlotChangeTypeEvent event) {
        if(!event.getTownBlock().getTypeName().equals("port")) {
            return;
        }
        String sUUID = event.getTownBlock().getTownOrNull().getUUID().toString();
        if(PortsMain.instance.getConfig().getString(sUUID) == null) {
            int iVal = PortsMain.getCustomConfig().getInt("default-port-fee");
            PortsMain.instance.getConfig().createSection(sUUID);
            PortsMain.instance.getConfig().set(sUUID, Double.valueOf(iVal));
            PortsMain.instance.saveConfig();
        }
    }
}
