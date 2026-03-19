package com.earthpol.townyports;

import com.earthpol.earthPolLib.config.ReloadableConfigHandler;
import com.earthpol.earthPolLib.logging.EnhancedLogger;
import com.earthpol.earthPolLib.translation.TranslationService;
import com.earthpol.townyports.commands.PortBaseCommand;
import com.earthpol.townyports.commands.PortCommand;
import com.earthpol.townyports.commands.PortSetCommand;
import com.earthpol.townyports.config.Config;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.object.AddonCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class PortsMain extends JavaPlugin {

    public static PortsMain instance;

    private static EnhancedLogger logger;
    public static EnhancedLogger log() { return logger; }

    public static String PREFIX;
    private static TranslationService translationService;
    public static TranslationService getTranslationService() { return translationService; }

    // Config
    public static ReloadableConfigHandler<Config> reloadableConfigHandler;
    public static ReloadableConfigHandler<Config> getReloadableConfigHandler() { return reloadableConfigHandler; }

    // Singletons for command handlers (so we don't create new objects per command execution)
    private static final PortCommand PORT_COMMAND = new PortCommand();
    private static final PortBaseCommand PORT_BASE_COMMAND = new PortBaseCommand();
    private static final PortSetCommand PORT_SET_COMMAND = new PortSetCommand();

    public static PortCommand getPortCommand() { return PORT_COMMAND; }

    @Override
    public void onEnable() {
        instance = this;
        PREFIX = "§6[TownyPorts]§r ";

        // Logger
        logger = EnhancedLogger.create(this, PREFIX, true);
        log().getLogRetentionTask().startNow();

        // Config
        try {
            reloadableConfigHandler = new ReloadableConfigHandler<>(this, "config.yml", Config.class);
        } catch (IOException e) {
            log().severe("Failed to load config file! Shutting down.", e);
            this.setEnabled(false);
            return;
        }

        try {
            translationService = new TranslationService(this, PortsMain.class);
            translationService.load();
        } catch (Exception e) {
            log().severe("Failed to load translations! Shutting down.", e);
            this.setEnabled(false);
            return;
        }

        asciiText();
        setupListeners();
        setupCommands();

        printClean(PREFIX + "Plugin has been loaded properly.");
    }

    @Override
    public void onDisable() {
        // keep behavior identical, but consider removing if ReloadableConfigHandler owns persistence
        saveConfig();
    }

    private void setupListeners() {
        // Intentionally empty for now.
        // Recommendation: add travel-arrival alert listener here if you want it enabled.
    }

    private void setupCommands() {
        /*
         * /t port <town>
         * /t port price <town>
         * /t port list [page]
         * /t port reload
         *
         * /t set port spawn
         * /t set port price <amount>
         * /t set port remove
         */

        AddonCommand port = new AddonCommand(TownyCommandAddonAPI.CommandType.TOWN, "port", PORT_BASE_COMMAND);
        port.setTabCompleter(PORT_BASE_COMMAND);
        TownyCommandAddonAPI.addSubCommand(port);

        AddonCommand setPort = new AddonCommand(TownyCommandAddonAPI.CommandType.TOWN_SET, "port", PORT_SET_COMMAND);
        setPort.setTabCompleter(PORT_SET_COMMAND);
        TownyCommandAddonAPI.addSubCommand(setPort);
    }

    private void asciiText() {
        printClean("§e█████████████████████████████████████████████████");
        printClean("§e██████████████████ §a TownyPorts §e██████████████████");
        printClean("§e█████████████████████████████████████████████████");
        printClean("");
        printClean("§a ████████  ██████  ██     ██ ███    ██ ██    ██ ");
        printClean("§a    ██    ██    ██ ██     ██ ████   ██  ██  ██  ");
        printClean("§a    ██    ██    ██ ██  █  ██ ██ ██  ██   ████   ");
        printClean("§a    ██    ██    ██ ██ ███ ██ ██  ██ ██    ██    ");
        printClean("§a    ██     ██████   ███ ███  ██   ████    ██    ");
        printClean("");
        printClean("§6    ██████   ██████  ██████  ████████ ███████  ");
        printClean("§6    ██   ██ ██    ██ ██   ██    ██    ██       ");
        printClean("§6    ██████  ██    ██ ██████     ██    ███████  ");
        printClean("§6    ██      ██    ██ ██   ██    ██         ██  ");
        printClean("§6    ██       ██████  ██   ██    ██    ███████  ");
        printClean("");
    }

    public static void printClean(String message) {
        instance.getServer().getConsoleSender().sendMessage(message);
    }
}
