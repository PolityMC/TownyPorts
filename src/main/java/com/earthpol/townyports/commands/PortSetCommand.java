package com.earthpol.townyports.commands;

import com.earthpol.earthPolLib.location.LocationUtil;
import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.data.PortDAO;
import com.earthpol.townyports.utils.PortPlotUtil;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.BaseCommand;
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

        // Ensure in-game
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
            Location loc = player.getLocation();

            if(!canSetPort(loc,player)){return true;}

            PortDAO.setPortSpawn(town,player.getLocation());
            sender.sendMessage(PortsMain.PREFIX + "§aPort spawn set to your current location.");
            return true;
        }

        if (action.equals("price")) {
            // /t set port price <amount>
            if (args.length != 2) {
                sender.sendMessage("§6[TownyPorts] §dUsage: /t set port price <amount>");
                return true;
            }

            double fee;
            try {
                fee = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(PortsMain.PREFIX + "§cInvalid number: " + args[1]);
                return true;
            }

            if(!canSetPortPrice(player,fee)){return true;}
            PortDAO.setPortPrice(town,fee);
            sender.sendMessage(PortsMain.PREFIX + "§aPort fee for "
                    + town.getName() +
                    " set to " + fee + PortsMain.getCustomConfig().getString("currency-sign") + ".");
            return true;
        }

        // Unknown subcommand
        sender.sendMessage("§6[TownyPorts] §dUsage:");
        sender.sendMessage("§6/t set port price <amount>");
        sender.sendMessage("§6/t set port spawn");
        return true;
    }

    // Handles player facing permissions checks and error messaging.
    private static boolean canSetPort(@NotNull Location loc, @NotNull Player p) {
        Resident resident = TownyAPI.getInstance().getResident(p);
        if (resident == null) {throw new NullPointerException(p.getName() + " towny resident is null.");}

        //Townless check
        if(!resident.hasTown()){
            p.sendMessage("You must have a town to set a town port.");
            return false;
        }

        // Permissions check
        if(!p.hasPermission("townyports.set.spawn")){
            p.sendMessage("You do not have permission to set the town port.");
            return false;
        }

        // Location check
        // The townblock at the target location must be a part of your own town.
        TownBlock locTownBlock = TownyAPI.getInstance().getTownBlock(p);
        if(locTownBlock == null){
            p.sendMessage("This location has no associated townblock.");
            return false;
        }
        if(locTownBlock.getTownOrNull() != resident.getTownOrNull()){
            p.sendMessage("You can only set the port for your own town.");
        }

        // Location safety check
        if(!LocationUtil.isSafeLocation(loc)) {
            p.sendMessage("This location is not safe.");
            return false;
        }

        return true;

    }

    // Handles player facing permissions checks and error messaging.
    private static boolean canSetPortPrice(Player p, double price){

        // Permission check
        if(!p.hasPermission("townyports.port.set")) { //BUG this is a nonexistent permission.
            p.sendMessage("You do not have permission to set the town port price.");
            return false;
        }

        if (!PortsMain.getCustomConfig().getBoolean("uses-economy")) {
            p.sendMessage(PortsMain.PREFIX + "§cEconomy is disabled.");
            return false;
        }

        int max = PortsMain.getCustomConfig().getInt("maximum-port-fee");
        if (price < 0 || price > max) {
            p.sendMessage(PortsMain.PREFIX + "§cFee must be between 0 and " + max + ".");
            return false;
        }

        return true;

    }
}
