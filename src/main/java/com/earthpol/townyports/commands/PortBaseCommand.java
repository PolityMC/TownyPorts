package com.earthpol.townyports.commands;

import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.config.Config;
import com.earthpol.townyports.data.Port;
import com.earthpol.townyports.data.PortDAO;
import com.earthpol.townyports.util.HelpMenu;
import com.earthpol.townyports.util.Msg;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.earthpol.townyports.PortsMain.log;

public class PortBaseCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    private static final int PORTS_PER_PAGE = 25;

    public PortBaseCommand() {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Default -> help
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            HelpMenu.sendPortHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {

            case "list": {
                if (!sender.hasPermission("townyports.port.list") && !sender.isOp()) {
                    Msg.error(sender, "You do not have permission to use this.");
                    return true;
                }

                int page = 1;
                if (args.length >= 2 && args[1].matches("\\d+")) {
                    page = Integer.parseInt(args[1]);
                } else if (args.length >= 2) {
                    Msg.usage(sender, Msg.plain("/t port list [page]").color(Msg.ACCENT));
                    return true;
                }

                listAllActivePorts(sender, page);
                return true;
            }

            case "price": {
                if (args.length != 2) {
                    Msg.usage(sender, Msg.plain("/t port price <town>").color(Msg.ACCENT));
                    return true;
                }

                if (!sender.hasPermission("townyports.port.price") && !sender.isOp()) {
                    Msg.error(sender, "You do not have permission to use this.");
                    return true;
                }

                try {
                    Town town = getTownOrThrow(args[1]);
                    Port port = PortDAO.getPort(town);
                    if (port == null) {
                        Msg.error(sender, "That town does not have a port.");
                        return true;
                    }

                    double fee = port.portPrice();
                    String sign = Config.CURRENCY_SIGN.getString();
                    Msg.info(sender, "Port fee for " + port.getName() + " is " + fee + " " + sign + ".");

                } catch (TownyException e) {
                    TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
                }
                return true;
            }

            case "reload": {
                boolean hasPermission = sender.hasPermission("townyports.reload") || sender.isOp();
                if (!hasPermission) {
                    Msg.error(sender, "You do not have permission to use this.");
                    return true;
                }

                try {
                    PortsMain.getReloadableConfigHandler().reload();
                } catch (IOException e) {
                    Msg.error(sender, "Failed to reload config. Check console.");
                    log().severe(e.getMessage(), e);
                    return true;
                }

                Msg.success(sender, "Reloaded configuration.");
                return true;
            }

            case "info": {
                if (!Msg.isPlayer(sender)) {
                    Msg.error(sender, "This command can only be used in-game.");
                    return true;
                }

                Player p = (Player) sender;

                if (args.length == 1) {
                    showMyTownPortInfo(p);
                    return true;
                }

                if (args.length == 2) {
                    showTownPortInfo(p, args[1]);
                    return true;
                }

                Msg.usage(p, Msg.plain("/t port info [town]").color(Msg.ACCENT));
                return true;
            }


            case "here": {
                if (!Msg.isPlayer(sender)) {
                    Msg.error(sender, "This command can only be used in-game.");
                    return true;
                }
                showPortHere((Player) sender);
                return true;
            }


            default:
                // Treat anything else as a destination-town name
                return PortsMain.getPortCommand().onCommand(sender, command, label, args);
        }
    }

    private void listAllActivePorts(CommandSender sender, int page) {
        String sign = Config.CURRENCY_SIGN.getString();
        List<Port> all = PortDAO.getAllPorts();

        if (all.isEmpty()) {
            Msg.warn(sender, "No active ports found on the server.");
            return;
        }

        all.sort(Comparator.comparing(p -> p.getName().toLowerCase(Locale.ROOT)));

        int totalPages = (int) Math.ceil(all.size() / (double) PORTS_PER_PAGE);
        if (totalPages < 1) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * PORTS_PER_PAGE;
        int end = Math.min(start + PORTS_PER_PAGE, all.size());
        List<Port> sublist = all.subList(start, end);

        Msg.send(sender, Msg.PREFIX.append(Component.text("Active ports ", Msg.GOOD))
                .append(Component.text("(" + all.size() + " total) ", Msg.MUTED))
                .append(Component.text("— Page " + page + "/" + totalPages, Msg.MUTED)));

        for (Port p : sublist) {
            var loc = p.location();
            String line = p.getName() + " • " + p.portPrice() + " " + sign +
                    " • " + loc.getWorld().getName() +
                    " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";

            Component row = Msg.PREFIX
                    .append(Component.text("• ", Msg.MUTED))
                    .append(Msg.runCmd(p.getName(), "/t port " + p.getName(),
                            "Click to travel to " + p.getName()))
                    .append(Component.text("  ", Msg.MUTED))
                    .append(Component.text("[" + p.portPrice() + " " + sign + "]", NamedTextColor.WHITE))
                    .hoverEvent(HoverEvent.showText(Component.text(line, NamedTextColor.WHITE)));

            Msg.send(sender, row);
        }

        Msg.send(sender, Msg.PREFIX.append(Component.text("Use ", Msg.MUTED))
                .append(Component.text("/t port list <page>", Msg.ACCENT))
                .append(Component.text(" to view other pages.", Msg.MUTED)));
    }

    private void showMyTownPortInfo(Player p) {
        TownyAPI api = TownyAPI.getInstance();

        Resident res = api.getResident(p);
        if (res == null) {
            Msg.error(p, "Failed to load your Towny Resident record. Please relog.");
            return;
        }

        Town town = res.getTownOrNull();
        if (town == null) {
            Msg.error(p, "You do not belong to a town.");
            return;
        }

        Port port = PortDAO.getPort(town);
        if (port == null) {
            Msg.warn(p, "Your town does not have a port set.");
            Msg.send(p, Msg.PREFIX.append(Component.text("Set one with ", Msg.MUTED))
                    .append(Msg.cmd("/t set port spawn", "Set your town's port spawn to your current location."))
            );
            return;
        }

        String sign = Config.CURRENCY_SIGN.getString();
        Location loc = port.location();

        Msg.send(p, Msg.PREFIX.append(Component.text("Your town port", Msg.BRAND)));
        Msg.send(p, Msg.PREFIX
                .append(Component.text("• Town: ", Msg.MUTED))
                .append(Component.text(town.getName(), Msg.ACCENT))
        );

        Msg.send(p, Msg.PREFIX
                .append(Component.text("• Fee: ", Msg.MUTED))
                .append(Component.text(port.portPrice() + " " + sign, Msg.GOOD))
        );

        // Coords line with click-to-copy (suggest into chat)
        String coordText = loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
        Component coords = Component.text(coordText, NamedTextColor.WHITE)
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy (suggest) coords", NamedTextColor.WHITE)))
                .clickEvent(ClickEvent.suggestCommand(coordText));

        Msg.send(p, Msg.PREFIX
                .append(Component.text("• Location: ", Msg.MUTED))
                .append(coords)
        );

        // Quick travel button
        Msg.send(p, Msg.PREFIX
                .append(Component.text("• Travel: ", Msg.MUTED))
                .append(Msg.runCmd("Click to run /t port " + town.getName(),
                        "/t port " + town.getName(),
                        "Run the travel command"))
        );
    }

    private void showPortHere(Player p) {
        TownyAPI api = TownyAPI.getInstance();

        if (api.isWilderness(p.getLocation())) {
            Msg.warn(p, "You are currently in the wilderness.");
            return;
        }

        WorldCoord wc = WorldCoord.parseWorldCoord(p.getLocation());
        TownBlock tb = api.getTownBlock(wc);
        Town hereTown = api.getTownOrNull(tb);

        if (hereTown == null) {
            Msg.warn(p, "You are not standing in a town.");
            return;
        }

        Port port = PortDAO.getPort(hereTown);
        if (port == null) {
            Msg.warn(p, "This town (" + hereTown.getName() + ") does not have a port set.");
            return;
        }

        WorldCoord portWc = WorldCoord.parseWorldCoord(port.location());
        boolean isPortChunk = wc.equals(portWc);

        Msg.send(p, Msg.PREFIX.append(Component.text("Port chunk check", Msg.BRAND)));
        Msg.send(p, Msg.PREFIX
                .append(Component.text("• Town: ", Msg.MUTED))
                .append(Component.text(hereTown.getName(), Msg.ACCENT))
        );

        if (isPortChunk) {
            Msg.success(p, "You are standing in this town's port chunk.");
            Msg.send(p, Msg.PREFIX
                    .append(Component.text("• Travel: ", Msg.MUTED))
                    .append(Msg.runCmd("Click to run /t port " + hereTown.getName(),
                            "/t port " + hereTown.getName(),
                            "Run the travel command"))
            );
        } else {
            Msg.warn(p, "You are NOT standing in this town's port chunk.");
            // Show where the port chunk is, with click-to-copy coords
            Location loc = port.location();
            String coordText = loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();

            Component coords = Component.text(coordText, NamedTextColor.WHITE)
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy (suggest) coords", NamedTextColor.WHITE)))
                    .clickEvent(ClickEvent.suggestCommand(coordText));

            Msg.send(p, Msg.PREFIX
                    .append(Component.text("• Port spawn: ", Msg.MUTED))
                    .append(coords)
            );
        }
    }

    private void showTownPortInfo(Player viewer, String townName) {
        try {
            Town town = getTownOrThrow(townName);
            Port port = PortDAO.getPort(town);

            if (port == null) {
                Msg.warn(viewer, town.getName() + " does not have a port set.");
                return;
            }

            String sign = Config.CURRENCY_SIGN.getString();
            var loc = port.location();

            Msg.send(viewer, Msg.PREFIX.append(net.kyori.adventure.text.Component.text("Port info", Msg.BRAND)));
            Msg.send(viewer, Msg.PREFIX
                    .append(net.kyori.adventure.text.Component.text("• Town: ", Msg.MUTED))
                    .append(net.kyori.adventure.text.Component.text(town.getName(), Msg.ACCENT))
            );
            Msg.send(viewer, Msg.PREFIX
                    .append(net.kyori.adventure.text.Component.text("• Fee: ", Msg.MUTED))
                    .append(net.kyori.adventure.text.Component.text(port.portPrice() + " " + sign, Msg.GOOD))
            );

            String coordText = loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();

            var coords = net.kyori.adventure.text.Component.text(coordText, net.kyori.adventure.text.format.NamedTextColor.WHITE)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            net.kyori.adventure.text.Component.text("Click to copy (suggest) coords", net.kyori.adventure.text.format.NamedTextColor.WHITE)
                    ))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(coordText));

            Msg.send(viewer, Msg.PREFIX
                    .append(net.kyori.adventure.text.Component.text("• Location: ", Msg.MUTED))
                    .append(coords)
            );

            Msg.send(viewer, Msg.PREFIX
                    .append(net.kyori.adventure.text.Component.text("• Travel: ", Msg.MUTED))
                    .append(Msg.runCmd("Click to run /t port " + town.getName(),
                            "/t port " + town.getName(),
                            "Run the travel command"))
            );

        } catch (TownyException e) {
            Msg.error(viewer, e.getMessage());
        }
    }



    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String token = args[0].toLowerCase(Locale.ROOT);

            // 1) Subcommands first (in this exact order)
            List<String> subs = Arrays.asList("help", "list", "price", "info", "here");

            List<String> out = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(token)) out.add(s);
            }

            // 2) Then towns
            out.addAll(
                    TownyAPI.getInstance().getTowns().stream()
                            .map(Town::getName)
                            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(token))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .toList()
            );

            return out;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String token = args[1].toLowerCase(Locale.ROOT);

            if (sub.equals("price") || sub.equals("info")) return filterTownNames(token);
            if (sub.equals("list")) return Arrays.asList("1", "2", "3", "4", "5");
        }

        return Collections.emptyList();
    }

    private List<String> filterTownNames(String token) {
        return TownyAPI.getInstance().getTowns().stream()
                .map(Town::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(token))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}
