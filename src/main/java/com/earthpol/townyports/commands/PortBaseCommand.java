package com.earthpol.townyports.commands;

import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.utils.PortPlotUtil;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.AddonCommand;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PortBaseCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    public PortBaseCommand() {
        AddonCommand cmd = new AddonCommand(TownyCommandAddonAPI.CommandType.TOWN, "port", this);
        cmd.setTabCompleter(this);
        TownyCommandAddonAPI.addSubCommand(cmd);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6[TownyPorts] §dUsage:");
            sender.sendMessage("§6/t port <town> §7- Teleport to a port");
            sender.sendMessage("§6/t port price <town> §7- View port fee");
            sender.sendMessage("§6/t port set price <amount> §7- Set port fee for your town");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "price":
                if (args.length != 2) {
                    sender.sendMessage("§6[TownyPorts] §dUsage: /t port price <town>");
                    return true;
                }

                if(!sender.hasPermission("townyports.port.price")) {
                    return true;
                }

                try {
                    Town town = getTownOrThrow(args[1]);
                    if (!PortPlotUtil.hasPortPlot(town)) {
                        sender.sendMessage("§6[TownyPorts] §cThat town does not have a port.");
                        return true;
                    }
                    double fee = PortsMain.instance.getConfig().getDouble(town.getUUID().toString());
                    String sign = PortsMain.getCustomConfig().getString("currency-sign");
                    sender.sendMessage(PortsMain.PREFIX + "§aPort fee for " + town.getName() + " is " + fee + sign);
                } catch (TownyException e) {
                    TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
                }
                return true;

            case "set":
                if (args.length != 3 || !args[1].equalsIgnoreCase("price")) {
                    sender.sendMessage("§6[TownyPorts] §dUsage: /t port set price <amount>");
                    return true;
                }

                if(!sender.hasPermission("townyports.port.set")) {
                    return true;
                }

                return new PortSetCommand().onCommand(sender, command, label, Arrays.copyOfRange(args, 2, args.length));

            default:
                return new PortCommand().onCommand(sender, command, label, args);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("price", "set");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set")) {
                return List.of("price");
            }
            if (args[0].equalsIgnoreCase("price") || args[0].equalsIgnoreCase("<town>")) {
                return filterTownNames(args[1]);
            }
        }
        // no suggestions for amount
        return Collections.emptyList();
    }

    private List<String> filterTownNames(String token) {
        return TownyAPI.getInstance().getTowns().stream()
                .map(Town::getName)
                .filter(n -> n.toLowerCase().startsWith(token.toLowerCase()))
                .collect(Collectors.toList());
    }
}