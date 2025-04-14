package net.euromc.townyports.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import net.euromc.townyports.PortsMain;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TownUuid implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player) {

            Player p = (Player) sender;

            if (!p.hasPermission("townyports.townuuid")) {
                p.sendMessage(PortsMain.PREFIX + "§cYou do not have the required permission for this command.");
                return true;
            }

            if (args.length != 1) {
                p.sendMessage(PortsMain.PREFIX + "§5Correct usage: `/townuuid <town-name>`");
                return true;
            }

            String townName = args[0];

            if (TownyAPI.getInstance().getTown(townName) == null) {
                p.sendMessage(PortsMain.PREFIX + "§cThis town does not exist!");
                return true;
            }

            String sUUID = TownyAPI.getInstance().getTown(townName).getUUID().toString();
            p.sendMessage(PortsMain.PREFIX + "§e" + sUUID);

        } else {

            if (args.length != 1) {
                Bukkit.getLogger().info(PortsMain.PREFIX + "§5Correct usage: `/townuuid <town-name>`");
                return true;
            }

            String townName = args[0];

            if (TownyAPI.getInstance().getTown(townName) == null) {
                Bukkit.getLogger().info(PortsMain.PREFIX + "§cThis town does not exist.");
                return true;
            }

            String sUUID = TownyAPI.getInstance().getTown(townName).getUUID().toString();
            Bukkit.getLogger().info(PortsMain.PREFIX + "§e" + sUUID);

        }
        return false;
    }
}
