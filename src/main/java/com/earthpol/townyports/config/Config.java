package com.earthpol.townyports.config;

import com.earthpol.earthPolLib.config.ReloadableConfigNode;
import com.earthpol.earthPolLib.config.ReloadableConfiguration;

public enum Config implements ReloadableConfiguration {

    USES_ECONOMY(
            ReloadableConfigNode.of("uses-economy", boolean.class, true)
    ),

    CURRENCY_SIGN(
            ReloadableConfigNode.of("currency-sign", String.class, "Gold")
    ),

    MINIMUM_PORT_FEE(
            ReloadableConfigNode.of("minimum-port-fee", int.class, 10)
    ),

    MAXIMUM_PORT_FEE(
            ReloadableConfigNode.of("maximum-port-fee", int.class, 100)
    ),

    DEFAULT_PORT_FEE(
            ReloadableConfigNode.of("default-port-fee", int.class, 50)
    ),

    MAXIMUM_PORT_DISTANCE_IN_CHUNKS(
            ReloadableConfigNode.of("maximum-port-distance-in-chunks", int.class, 2750)
    ),

    ENABLE_PORT_ARRIVAL_ALERT(
            ReloadableConfigNode.of("enable-port-arrival-alert", boolean.class, false)
    ),

    PORT_TRAVEL_WARMUP_IN_TICKS(
            ReloadableConfigNode.of("port-travel-warmup-in-ticks", long.class, 200L)
    ),

    PORT_TRAVEL_COOLDOWN_IN_SECONDS(
            ReloadableConfigNode.of("port-travel-cooldown-in-seconds", long.class, 60L)
    ),

    PORT_TRAVEL_DENIES_FOR_ENEMIES(
            ReloadableConfigNode.of("port-travel-denies-for-enemies", boolean.class, true)
    ),

    ;

    private final ReloadableConfigNode<?> node;
    Config(ReloadableConfigNode<?> node) { this.node = node; }
    @Override public ReloadableConfigNode<?> node() { return node; }
}
