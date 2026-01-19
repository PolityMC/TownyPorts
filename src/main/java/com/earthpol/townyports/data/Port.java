package com.earthpol.townyports.data;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public record Port(
        Location location,
        @NotNull Town town,
        double portPrice
) {
    // Get the String name of the town that contains this port.
    public String getName(){ return town.getName(); }

    @Override public @NotNull String toString(){
        return "{" +
                town.getName() +
                " " +
                "CurrentPortPrice:" + portPrice +
                " " +
                location.toString() +
                "}";
    }
}
