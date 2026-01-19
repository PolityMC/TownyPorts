package com.earthpol.townyports.data;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Location;

public record Port(
        Location location,
        Town town,
        double portPrice
) {
    // Checks if this Port is corrupt
    // Corruption: The location or town is null. These should never be null on a Port object.
    public boolean isCorrupt(){
        if(location == null){
            // log this
            return true;
        }
        if(town == null){
            // log this
            return true;
        }

        return false;
    }


}
