package net.euromc.townyports.commands;

import net.euromc.townyports.PortsMain;
import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PortPrice implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player)) {
            Bukkit.getLogger().info(PortsMain.PREFIX + "You must be a player to run this command.");
            Bukkit.getLogger().info(PortsMain.PREFIX + "If you want to see the price as an administrator, check config.yml");
            Bukkit.getLogger().info(PortsMain.PREFIX + "You can use the ``/townuuid`` command to see the town's UUID");
            return true;
        }

        Player player = (Player) sender;

        if (!PortsMain.getCustomConfig().getBoolean("uses-economy")) {
            player.sendMessage(PortsMain.PREFIX + "§cEconomy is disabled for TownyPorts.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(PortsMain.PREFIX + "§5Correct usage: `/setportprice <amount>`");
            return true;
        }

        if (!args[0].matches("[0-9]+")) {
            player.sendMessage(PortsMain.PREFIX + "§c That is not an integer.");
            return true;
        }

        if (!TownyAPI.getInstance().getResident(player.getName()).isMayor()) {
            player.sendMessage(PortsMain.PREFIX + "§c You are not the mayor of your town.");
            return true;
        }

        double dbl = Double.parseDouble(args[0]);

        if (dbl < PortsMain.getCustomConfig().getInt("minimum-port-fee")) {
            player.sendMessage(PortsMain.PREFIX + "§c A port's travel fee must not be lower than " + + PortsMain.getCustomConfig().getInt("minimum-port-fee") + PortsMain.getCustomConfig().getString("currency-sign"));
            return true;
        }

        if (PortsMain.getCustomConfig().getInt("maximum-port-fee") < dbl) {
            player.sendMessage(PortsMain.PREFIX + "§c A port's travel fee must not be higher than " + PortsMain.getCustomConfig().getInt("maximum-port-fee") + PortsMain.getCustomConfig().getString("currency-sign"));
        }

        if (PortsMain.instance.getConfig().getString(TownyAPI.getInstance().getResident(player.getName()).getTownOrNull().getUUID().toString()) != null) {
            PortsMain.instance.getConfig().createSection(TownyAPI.getInstance().getResident(player.getName()).getTownOrNull().getUUID().toString());
        }
        PortsMain.instance.getConfig().set(TownyAPI.getInstance().getResident(player.getName()).getTownOrNull().getUUID().toString(), dbl);
        PortsMain.instance.saveConfig();
        player.sendMessage(PortsMain.PREFIX + "§a Successfully set the port travel fee to " + dbl + PortsMain.getCustomConfig().getString("currency-sign") + ".");

        return false;
    }
}
