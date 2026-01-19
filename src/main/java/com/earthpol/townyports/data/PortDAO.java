package com.earthpol.townyports.data;

import com.earthpol.earthPolLib.location.LocationUtil;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.DecimalDataField;
import com.palmergames.bukkit.towny.object.metadata.LocationDataField;
import com.palmergames.bukkit.towny.utils.MetaDataUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class PortDAO {

    // Metadata keys
    private static final LocationDataField portLocationField = new LocationDataField("townyPorts_portLocation");
    private static final DecimalDataField portPriceField = new DecimalDataField("townyPorts_portPrice");

    // Return a Port object built from the corresponding Towny metadata fields.
    public static @Nullable Port getPort(Town town){
        // If either of the metadata fields are missing, the town is treated as not having a port. Null port will be returned.
        if (!MetaDataUtil.hasMeta(town,portLocationField)) return null;
        if (MetaDataUtil.hasMeta(town,portPriceField)) return null;

        return new Port(
                MetaDataUtil.getLocation(town,portLocationField),
                town,
                MetaDataUtil.getDouble(town,portPriceField)
        );
    }

    public static boolean hasPort(Town town){return getPort(town) != null;}

    // Sets the port location for a town.
    public static void setPort(Town town,
                               Location location,
                               double price,
                               @Nullable Player p){

        // Extra checks and player-facing error messaging are done if this is called by a player.
        boolean calledByPlayer = (p != null);

        // Permissions check
        if(calledByPlayer){
            if(!p.hasPermission("townyports.set.spawn")){
                p.sendMessage("You do not have permission to set the town port.");
                return;
            }
        }

        // Ensure the port location is actually inside of the town.
        if(!town.isInsideTown(location)) {
            if(calledByPlayer) p.sendMessage("This location is not inside your town.");
            // elogger
            return;
        }

        // Location safety check
        if(!com.earthpol.earthPolLib.location.LocationUtil.isSafeLocation(location)) {
            if(calledByPlayer) p.sendMessage("This location is not safe!");
            return;
        }

        // Remove any existing port on this town.
        removePort(town);

        // Set the port metadata
        MetaDataUtil.setLocation(town,portLocationField,location,true);
        MetaDataUtil.setDouble(town,portPriceField,price,true);

    }

    // Convenience override -- for calling with no player involved. (admin commands, etc.)
    public static void setPort(Town town, Location portLocation, double portPrice){
        setPort(town,portLocation,portPrice,null);
    }

    // Removes port metadata fields.
    public static void removePort(Town town){
        town.removeMetaData(portLocationField,true);
        town.removeMetaData(portPriceField,true);
    }

}
