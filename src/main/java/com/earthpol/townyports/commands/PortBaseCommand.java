package com.earthpol.townyports.commands;

import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.cache.PortEntry;
import com.earthpol.townyports.data.Port;
import com.earthpol.townyports.data.PortDAO;
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

import java.util.*;
import java.util.stream.Collectors;

public class PortBaseCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    private static final int PORTS_PER_PAGE = 50;

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
            sender.sendMessage("§6/t port list §7- List all active ports");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list": {
                if (!sender.hasPermission("townyports.port.list")) {
                    sender.sendMessage("§6[TownyPorts] §cYou do not have permission to use this.");
                    return true;
                }

                if (args.length == 1) {
                    // /t port list
                    listAllActivePorts(sender, 1);
                    return true;
                }

                if (args.length == 2) {
                    // Could be a page number or a town
                    if (args[1].matches("\\d+")) {
                        int page = Integer.parseInt(args[1]);
                        listAllActivePorts(sender, page);
                        return true;
                    } else {
                        // /t port list <town>
                        return true;
                    }
                }

                if (args.length >= 3 && args[2].matches("\\d+")) {
                    // /t port list <town> <page>
                    return true;
                }

                // fallback
                sender.sendMessage("§6[TownyPorts] §dUsage: /t port list [page]");
                return true;
            }

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
                    Port port = PortDAO.getPort(town);
                    if (port == null) {
                        sender.sendMessage("§6[TownyPorts] §cThat town does not have a port.");
                        return true;
                    }
                    double fee = port.portPrice();
                    String sign = PortsMain.getCustomConfig().getString("currency-sign");
                    sender.sendMessage(PortsMain.PREFIX + "§aPort fee for " + port.getName() + " is " + fee + " " + sign);
                } catch (TownyException e) {
                    TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
                }
                return true;

            default:
                return new PortCommand().onCommand(sender, command, label, args);
        }
    }

    private void listAllActivePorts(CommandSender sender, int page) {
        String sign = PortsMain.getCustomConfig().getString("currency-sign", "");
        List<Port> all = PortDAO.getAllPorts();

        if (all.isEmpty()) {
            sender.sendMessage("§6[TownyPorts] §eNo active ports found on the server.");
            return;
        }

        // Sort by town name for consistency
        all.sort(Comparator.comparing(pe -> pe.getName().toLowerCase(Locale.ROOT)));

        int totalPages = (int) Math.ceil(all.size() / (double) PORTS_PER_PAGE);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * PORTS_PER_PAGE;
        int end = Math.min(start + PORTS_PER_PAGE, all.size());
        List<Port> sublist = all.subList(start, end);

        sender.sendMessage("§6[TownyPorts] §aActive ports on the server (" + all.size() + " total) — Page " + page + "/" + totalPages + ":");

        Map<String, List<Port>> byTown = sublist.stream()
                .collect(Collectors.groupingBy(pe -> pe.getName(), TreeMap::new, Collectors.toList()));

        for (Map.Entry<String, List<Port>> e : byTown.entrySet()) {
            String townName = e.getKey();
            List<Port> ports = e.getValue();
            double price = ports.isEmpty() ? 0 : ports.get(0).portPrice();

            sender.sendMessage("§7 - §b" + townName + " §7(ports: " + ports.size() + ", price: §f" + price + " " + sign + "§7)");
            int i = 1;
            for (Port p : ports) {
                sender.sendMessage("   §8" + (i++) + ") §7" + p.location().getWorld().getName() +
                        " §7§8(" + p.location().getX() + "," + p.location().getY() + "," + p.location().getZ());
            }
        }

        sender.sendMessage("§8Use §f/t port list <page> §8to view other pages.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("price", "list");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("price") || args[0].equalsIgnoreCase("<town>")) {
                return filterTownNames(args[1]);
            }
            if (args[0].equalsIgnoreCase("list")) {
                // Suggest only towns with active ports
                String token = args[1].toLowerCase(Locale.ROOT);
                return PortsMain.instance.getPortRegistry().activeTownNames().stream()
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(token))
                        .limit(20)
                        .collect(Collectors.toList());
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