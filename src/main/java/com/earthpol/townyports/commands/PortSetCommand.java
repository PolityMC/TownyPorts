package com.earthpol.townyports.commands;

import com.earthpol.earthPolLib.location.LocationUtil;
import com.earthpol.townyports.config.Config;
import com.earthpol.townyports.data.PortDAO;
import com.earthpol.townyports.util.Msg;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PortSetCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    public PortSetCommand() {}

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            TownyMessaging.sendErrorMsg(sender, "This command can only be used in-game.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            Msg.usage(player, Msg.plain("/t set port spawn").color(Msg.ACCENT));
            Msg.usage(player, Msg.plain("/t set port price <amount>").color(Msg.ACCENT));
            Msg.usage(player, Msg.plain("/t set port remove").color(Msg.ACCENT));
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        switch (action) {

            case "spawn": {
                Location loc = player.getLocation();

                Town town = validateCanSetPortSpawn(player, loc);
                if (town == null) return true;

                PortDAO.setPortSpawn(town, loc);
                Msg.success(player, "Port spawn set to your current location.");
                return true;
            }

            case "price": {
                if (args.length != 2) {
                    Msg.usage(player, Msg.plain("/t set port price <amount>").color(Msg.ACCENT));
                    return true;
                }

                double fee;
                try {
                    fee = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    Msg.error(player, "Invalid number: " + args[1]);
                    return true;
                }

                Town town = validateCanSetPortPrice(player, fee);
                if (town == null) return true;

                PortDAO.setPortPrice(town, fee);
                Msg.success(player, "Port fee for " + town.getName() + " set to " + fee + " " + Config.CURRENCY_SIGN.getString() + ".");
                return true;
            }

            case "remove": {
                Town town = validateCanManagePort(player);
                if (town == null) return true;

                PortDAO.removePort(town);
                Msg.success(player, "Removed port settings for " + town.getName() + ".");
                return true;
            }

            default:
                Msg.error(player, "Unknown subcommand. Try /t set port help");
                return true;
        }
    }

    /**
     * Shared baseline: must have a town + permission.
     */
    private static Town validateCanManagePort(@NotNull Player p) {
        Resident resident = TownyAPI.getInstance().getResident(p);
        if (resident == null) {
            Msg.error(p, "Failed to load your Towny Resident record. Please relog.");
            return null;
        }

        if (!resident.hasTown()) {
            Msg.error(p, "You must have a town to manage a town port.");
            return null;
        }

        boolean hasPermission = p.hasPermission("townyports.set.spawn") || p.hasPermission("townyports.set.price") || p.isOp();
        if (!hasPermission) {
            Msg.error(p, "You do not have permission to manage town ports.");
            return null;
        }

        return resident.getTownOrNull();
    }

    /**
     * Validates + returns the player's Town if spawn can be set.
     */
    private static Town validateCanSetPortSpawn(@NotNull Player p, @NotNull Location loc) {
        Resident resident = TownyAPI.getInstance().getResident(p);
        if (resident == null) {
            Msg.error(p, "Failed to load your Towny Resident record. Please relog.");
            return null;
        }

        if (!resident.hasTown()) {
            Msg.error(p, "You must have a town to set a town port.");
            return null;
        }

        boolean hasPermission = p.hasPermission("townyports.set.spawn") || p.isOp();
        if (!hasPermission) {
            Msg.error(p, "You do not have permission to set the town port.");
            return null;
        }

        // Location must be inside your own town
        TownBlock locTownBlock = TownyAPI.getInstance().getTownBlock(WorldCoord.parseWorldCoord(loc));
        if (locTownBlock == null) {
            Msg.error(p, "This location has no associated town block.");
            return null;
        }

        Town town = resident.getTownOrNull();
        if (town == null || locTownBlock.getTownOrNull() != town) {
            Msg.error(p, "You can only set the port for your own town.");
            return null;
        }

        // Safety
        if (!LocationUtil.isSafeLocation(loc)) {
            Msg.error(p, "This location is not safe.");
            return null;
        }

        return town;
    }

    /**
     * Validates + returns the player's Town if price can be set.
     */
    private static Town validateCanSetPortPrice(@NotNull Player p, double price) {

        Resident resident = TownyAPI.getInstance().getResident(p);
        if (resident == null) {
            Msg.error(p, "Failed to load your Towny Resident record. Please relog.");
            return null;
        }

        if (!resident.hasTown()) {
            Msg.error(p, "You must have a town to set a town port price.");
            return null;
        }


        boolean hasPermission = p.hasPermission("townyports.set.price") || p.isOp();
        if (!hasPermission) {
            Msg.error(p, "You do not have permission to set the town port price.");
            return null;
        }

        if (!Config.USES_ECONOMY.getBool()) {
            Msg.error(p, "Economy is disabled.");
            return null;
        }

        int min = Config.MINIMUM_PORT_FEE.getInt();
        int max = Config.MAXIMUM_PORT_FEE.getInt();

        if (price < min || price > max) {
            Msg.error(p, "Fee must be between " + min + " and " + max + ".");
            return null;
        }

        return resident.getTownOrNull();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("spawn", "price", "remove", "help");
            String token = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String s : subs) if (s.startsWith(token)) out.add(s);
            return out;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("price")) {
            return Arrays.asList("1", "5", "10", "25", "50", "100");
        }

        return Collections.emptyList();
    }
}
