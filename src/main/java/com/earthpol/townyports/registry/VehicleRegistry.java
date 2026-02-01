package com.earthpol.townyports.registry;

import org.bukkit.entity.*;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class VehicleRegistry {

    private VehicleRegistry() {}

    private static final Set<Class<? extends Vehicle>> ALLOWED_VEHICLES = Set.of(
            AbstractHorse.class,
            AbstractNautilus.class,
            Boat.class,
            Pig.class,
            Strider.class
    );

    public static boolean isAllowedVehicle(@Nullable Vehicle vehicle) {
        if (vehicle == null) return false;

        for (Class<? extends Vehicle> allowed : ALLOWED_VEHICLES) {
            if (allowed.isInstance(vehicle)) {
                return true;
            }
        }
        return false;
    }

}
