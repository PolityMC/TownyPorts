package net.euromc.townyports.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import net.euromc.townyports.PortsMain;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GetPortPriceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Bukkit.getLogger().info(PortsMain.PREFIX + "§cYou can only run this command as a player.");
            return true;
        }

        Player p = (Player) sender;

        if (!PortsMain.getCustomConfig().getBoolean("uses-economy")) {
            p.sendMessage(PortsMain.PREFIX + "§cEconomy is disabled for TownyPorts.");
            return true;
        }

        if (TownyAPI.getInstance().getTown(args[0]) == null) {
            p.sendMessage(PortsMain.PREFIX + "§cThis town does not exist.");
            return true;
        }

        if (!PortsMain.instance.getConfig().contains(TownyAPI.getInstance().getTown(args[0]).getUUID().toString())) {
            p.sendMessage("§4[FATAL ERROR]§c THIS TOWN DOES NOT HAVE A PORT FEE REGISTERED");
            return true;
        }

        Town t = TownyAPI.getInstance().getTown(args[0]);
        String portFeeString = String.valueOf(PortsMain.instance.getConfig().getDouble(t.getUUID().toString()));
        p.sendMessage(PortsMain.PREFIX + "§a" + t.getName() + "'s port travel fee is " + portFeeString + PortsMain.getCustomConfig().getString("currency-sign") + ".");

        return false;
    }
}
