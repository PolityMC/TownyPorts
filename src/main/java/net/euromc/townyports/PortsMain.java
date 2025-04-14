package net.euromc.townyports;

import net.euromc.townyports.commands.*;
import net.euromc.townyports.listener.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class PortsMain extends JavaPlugin {

    public static PortsMain instance;
    private File customConfigFile;
    private FileConfiguration customConfig;


    public static String PREFIX;

    @Override
    public void onEnable() {
        instance = this;
        PREFIX = "§6[TownyPorts]§r ";
        createCustomConfig();
        loadConfig();
        asciiText();
        setupListeners();
        setupCommands();

        printClean(PREFIX + "Plugin has been loaded properly.");
    }


    private void setupListeners() {
        getServer().getPluginManager().registerEvents(new PlotChangeType(), this);
        getServer().getPluginManager().registerEvents(new AlreadyHavePort(), this);
        getServer().getPluginManager().registerEvents(new TownCreate(), this);
        getServer().getPluginManager().registerEvents(new PriceByDefault(), this);
        getServer().getPluginManager().registerEvents(new PlayerTeleportToPortAlert(), this);
    }

    private void setupCommands() {
        Objects.requireNonNull(getCommand("port")).setExecutor(new PortCommand());
        Objects.requireNonNull(getCommand("port")).setTabCompleter(new PortTabCommand());
        Objects.requireNonNull(getCommand("setportprice")).setExecutor(new PortPrice());
        Objects.requireNonNull(getCommand("portprice")).setExecutor(new GetPortPriceCommand());
        Objects.requireNonNull(getCommand("townuuid")).setExecutor(new TownUuid());
        Objects.requireNonNull(getCommand("townuuid")).setTabCompleter(new PortTabCommand());
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


