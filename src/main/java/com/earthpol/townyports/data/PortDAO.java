package com.earthpol.townyports.data;

import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.config.Config;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.DecimalDataField;
import com.palmergames.bukkit.towny.object.metadata.LocationDataField;
import com.palmergames.bukkit.towny.utils.MetaDataUtil;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PortDAO {

    // Metadata keys
    private static final LocationDataField portLocationField = new LocationDataField("townyPorts_portLocation");
    private static final DecimalDataField portPriceField = new DecimalDataField("townyPorts_portPrice");

    // Return a Port object built from the corresponding Towny metadata fields.
    public static @Nullable Port getPort(@Nullable Town town){

        if(town == null) return null;

        // If the port location metadata is missing, then return null port.
        if (!MetaDataUtil.hasMeta(town,portLocationField)) return null;

        // If there is no price metadata, the price will just be whatever the default price is.
        double portPrice;
        if (!MetaDataUtil.hasMeta(town,portPriceField)){
            portPrice = Config.DEFAULT_PORT_FEE.getInt();
        }
        else portPrice = MetaDataUtil.getDouble(town,portPriceField);

        return new Port(
                MetaDataUtil.getLocation(town,portLocationField),
                town,
                portPrice
        );
    }

    public static boolean hasPort(Town town){
        return getPort(town) != null;
    }

    public static void setPortSpawn(Town town, Location location){
        MetaDataUtil.setLocation(town,portLocationField,location,true);
    }

    public static void setPortPrice(Town town, double price){
        MetaDataUtil.setDouble(town,portPriceField,price,true);
    }

    // Returns all ports in the server
    public static List<Port> getAllPorts(){
        TownyAPI townyAPI = TownyAPI.getInstance();
        List<Port> portList = new ArrayList<>();

        for(Town t : townyAPI.getTowns() ){
            Port port = getPort(t);
            if(port != null) portList.add(port);
        }
        return portList;
    }

    // Removes port metadata fields.
    public static void removePort(Town town){
        town.removeMetaData(portLocationField,true);
        town.removeMetaData(portPriceField,true);
    }

}
