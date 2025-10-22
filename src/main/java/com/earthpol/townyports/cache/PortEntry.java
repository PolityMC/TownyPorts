package com.earthpol.townyports.cache;

import com.palmergames.bukkit.towny.object.WorldCoord;

import java.util.UUID;

public final class PortEntry {
    public final UUID townUUID;
    public final String townName;
    public final String worldName;
    public final int chunkX;
    public final int chunkZ;
    public final int centerX; // (chunkX << 4) + 8
    public final int centerZ; // (chunkZ << 4) + 8
    public final double price; // resolved from config at cache time

    public PortEntry(UUID townUUID, String townName, String worldName, int chunkX, int chunkZ, double price) {
        this.townUUID = townUUID;
        this.townName = townName;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.centerX = (chunkX << 4) + 8;
        this.centerZ = (chunkZ << 4) + 8;
        this.price = price;
    }

    public String key() {
        return worldName + ":" + chunkX + ":" + chunkZ;
    }

    public WorldCoord worldCoord() {
        return new WorldCoord(worldName, chunkX, chunkZ);
    }
}
