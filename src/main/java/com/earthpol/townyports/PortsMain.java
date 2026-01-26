package com.earthpol.townyports;

import com.earthpol.earthPolLib.logging.EnhancedLogger;
import com.earthpol.townyports.commands.*;
import com.earthpol.townyports.listener.*;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.object.AddonCommand;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class PortsMain extends JavaPlugin {

    public static PortsMain instance;
    private File customConfigFile;
    private FileConfiguration customConfig;

    private static EnhancedLogger logger;
    private static EnhancedLogger log() {return logger;}


    public static String PREFIX;

    @Override
    public void onEnable() {
        instance = this;
        PREFIX = "§6[TownyPorts]§r ";

        // Logger
        logger = EnhancedLogger.create(this, PREFIX,true);
        log().getLogRetentionTask().startNow();

        createCustomConfig();
        loadConfig();
        asciiText();

        setupListeners();
        setupCommands();

        printClean(PREFIX + "Plugin has been loaded properly.");
    }


    private void setupListeners() {
        getServer().getPluginManager().registerEvents(new PlotChangeType(), this);
    }

    private void setupCommands() {

        /*
        * /t set port -- Sets Port Spawn (Tab Complete)
        * /t port set price -- Sets Port Price (Tab Complete)
        * /t port price <town> -- Gets Port Price (Tab Complete)
        * /t port <town> -- Go to Port
        * /t port -- Base Command
         */

        TownyCommandAddonAPI.addSubCommand(
                new AddonCommand(TownyCommandAddonAPI.CommandType.TOWN, "port", new PortBaseCommand())
        );

        TownyCommandAddonAPI.addSubCommand(
                new AddonCommand(TownyCommandAddonAPI.CommandType.TOWN_SET, "port", new PortSetCommand())
        );
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
        printClean("                    §5by 0xBit & darthpeti       ");
        printClean("");
        printClean("§e█████████████████████████████████████████████████");
    }

	private void printClean(String line) {
		Bukkit.getConsoleSender().sendMessage(line);
	}

    public void createCustomConfig() {
        customConfigFile = new File(getDataFolder(), "settings.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("settings.yml", false);
        }

        customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }



    public static FileConfiguration getCustomConfig() {
        return instance.customConfig;
    }

    @Override
    public void onDisable() {
        printClean(PREFIX + "Plugin has been unloaded.");
        saveConfig();
    }

    public void loadConfig() {
        instance.getConfig().options().copyDefaults(false);
        instance.saveDefaultConfig();
    }
}


