package com.earthpol.townyports.commands;

import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.utils.PortPlotUtil;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.AddonCommand;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PortSetCommand extends BaseCommand implements CommandExecutor {

    public PortSetCommand() {
        AddonCommand cmd = new AddonCommand(TownyCommandAddonAPI.CommandType.TOWN_SET, "port", this);
        TownyCommandAddonAPI.addSubCommand(cmd);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // Ensure in-game and mayor
        if (!(sender instanceof Player)) {
            TownyMessaging.sendErrorMsg(sender, "This command can only be used in-game.");
            return true;
        }
        Player player = (Player) sender;
        Resident res = TownyAPI.getInstance().getResident(player);
        Town town = res.getTownOrNull();

        if (args.length == 0) {
            sender.sendMessage("§6[TownyPorts] §dUsage:");
            sender.sendMessage("§6/t set port price <amount>");
            sender.sendMessage("§6/t set port spawn");
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("spawn")) {
            if(!sender.hasPermission("townyports.port.set")) {
                return true;
            }

            TownBlock tb = TownyAPI.getInstance().getTownBlock(player.getLocation());
            if (tb == null) {
                TownyMessaging.sendErrorMsg(sender, "§c You must stand on your own town's port plot to set its spawn.");
                return true;
            }
            try {
                if (!tb.getTown().equals(town)) {
                    TownyMessaging.sendErrorMsg(sender, "§c You must be in your own town to set the port spawn.");
                    return true;
                }
            } catch (NotRegisteredException e) {
                throw new RuntimeException(e);
            }
            if (!PortPlotUtil.isPortPlot(tb)) {
                TownyMessaging.sendErrorMsg(sender, "§c You must stand on a port plot to set the spawn.");
                return true;
            }


            // Set port spawn to player's exact location
            Location loc = player.getLocation();
            try {
                PortPlotUtil.setPortSpawnLocation(town, loc);
                sender.sendMessage(PortsMain.PREFIX + "§aPort spawn set to your current location.");
                PortsMain.instance.getPortRegistry().rebuildTown(town.getUUID());
            } catch (TownyException e) {
                TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
            }
            return true;
        }

        if (action.equals("price")) {
            // /t set port price <amount>
            if (args.length != 2) {
                sender.sendMessage("§6[TownyPorts] §dUsage: /t set port price <amount>");
                return true;
            }

            if(!sender.hasPermission("townyports.port.set")) {
                return true;
            }

            if (!PortsMain.getCustomConfig().getBoolean("uses-economy")) {
                sender.sendMessage(PortsMain.PREFIX + "§cEconomy is disabled.");
                return true;
            }
            double fee;
            try {
                fee = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(PortsMain.PREFIX + "§cInvalid number: " + args[1]);
                return true;
            }
            int max = PortsMain.getCustomConfig().getInt("maximum-port-fee");
            if (fee < 0 || fee > max) {
                sender.sendMessage(PortsMain.PREFIX + "§cFee must be between 0 and " + max + ".");
                return true;
            }
            String uuid = town.getUUID().toString();
            PortsMain.instance.getConfig().set(uuid, (Object) fee);
            PortsMain.instance.saveConfig();
            sender.sendMessage(PortsMain.PREFIX + "§aPort fee for " + town.getName() + " set to " + fee + PortsMain.getCustomConfig().getString("currency-sign") + ".");
            return true;
        }

        // Unknown subcommand
        sender.sendMessage("§6[TownyPorts] §dUsage:");
        sender.sendMessage("§6/t set port price <amount>");
        sender.sendMessage("§6/t set port spawn");
        return true;
    }
}
