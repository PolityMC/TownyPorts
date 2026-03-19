package com.earthpol.townyports.commands;

import com.earthpol.townyports.PortsMain;
import com.earthpol.townyports.config.Config;
import com.earthpol.townyports.data.Port;
import com.earthpol.townyports.data.PortDAO;
import com.earthpol.townyports.util.HelpMenu;
import com.earthpol.townyports.util.Msg;
import com.palmergames.bukkit.towny.TownyAPI;
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
                    Msg.error(sender, "common.error.no-permission");
                    return true;
                }

                int page = 1;
                if (args.length >= 2 && args[1].matches("\\d+")) {
                    page = Integer.parseInt(args[1]);
                } else if (args.length >= 2) {
                    Msg.usage(sender, "port.usage.list");
                    return true;
                }

                listAllActivePorts(sender, page);
                return true;
            }

            case "price": {
                if (args.length != 2) {
                    Msg.usage(sender, "port.usage.price");
                    return true;
                }

                if (!sender.hasPermission("townyports.port.price") && !sender.isOp()) {
                    Msg.error(sender, "common.error.no-permission");
                    return true;
                }

                try {
                    Town town = getTownOrThrow(args[1]);
                    Port port = PortDAO.getPort(town);
                    if (port == null) {
                        Msg.error(sender, "port.error.town-no-port");
                        return true;
                    }

                    double fee = port.portPrice();
                    String sign = Config.CURRENCY_SIGN.getString();
                    Msg.info(sender, "port.info.fee-for-town", port.getName(), fee, sign);

                } catch (TownyException e) {
                    Msg.errorRaw(sender, e.getMessage(sender));
                }
                return true;
            }

            case "reload": {
                boolean hasPermission = sender.hasPermission("townyports.reload") || sender.isOp();
                if (!hasPermission) {
                    Msg.error(sender, "common.error.no-permission");
                    return true;
                }

                try {
                    PortsMain.getReloadableConfigHandler().reload();
                } catch (IOException e) {
                    Msg.error(sender, "port.reload.error.failed");
                    log().severe(e.getMessage(), e);
                    return true;
                }

                Msg.success(sender, "port.reload.success");
                return true;
            }

            case "info": {
                if (!Msg.isPlayer(sender)) {
                    Msg.error(sender, "common.error.in-game-only");
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

                Msg.usage(p, "port.usage.info-optional-town");
                return true;
            }


            case "here": {
                if (!Msg.isPlayer(sender)) {
                    Msg.error(sender, "common.error.in-game-only");
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
            Msg.warn(sender, "port.list.error.none-active");
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

        Msg.send(sender, Msg.prefix(sender).append(Component.text(Msg.tr(sender, "port.list.header.title"), Msg.GOOD))
                .append(Component.text(Msg.tr(sender, "port.list.header.total", all.size()), Msg.MUTED))
                .append(Component.text(Msg.tr(sender, "port.list.header.page", page, totalPages), Msg.MUTED)));

        for (Port p : sublist) {
            var loc = p.location();
            String line = Msg.tr(
                    sender,
                    "port.list.row.tooltip",
                    p.getName(),
                    p.portPrice(),
                    sign,
                    loc.getWorld().getName(),
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ()
            );

            Component row = Msg.prefix(sender)
                    .append(Component.text("• ", Msg.MUTED))
                    .append(Msg.runCmd(p.getName(), "/t port " + p.getName(),
                            Msg.tr(sender, "port.list.row.hover.travel-to", p.getName())))
                    .append(Component.text("  ", Msg.MUTED))
                    .append(Component.text(Msg.tr(sender, "port.list.row.price", p.portPrice(), sign), NamedTextColor.WHITE))
                    .hoverEvent(HoverEvent.showText(Component.text(line, NamedTextColor.WHITE)));

            Msg.send(sender, row);
        }

        Msg.send(sender, Msg.prefix(sender).append(Component.text(Msg.tr(sender, "port.list.footer.use"), Msg.MUTED))
                .append(Component.text(Msg.tr(sender, "port.usage.list-page"), Msg.ACCENT))
                .append(Component.text(Msg.tr(sender, "port.list.footer.suffix"), Msg.MUTED)));
    }

    private void showMyTownPortInfo(Player p) {
        TownyAPI api = TownyAPI.getInstance();

        Resident res = api.getResident(p);
        if (res == null) {
            Msg.error(p, "common.error.failed-resident-relog");
            return;
        }

        Town town = res.getTownOrNull();
        if (town == null) {
            Msg.error(p, "common.error.no-town");
            return;
        }

        Port port = PortDAO.getPort(town);
        if (port == null) {
            Msg.warn(p, "port.info.your-town-no-port");
            Msg.send(p, Msg.prefix(p).append(Component.text(Msg.tr(p, "port.info.set-one-with"), Msg.MUTED))
                    .append(Msg.cmd(Msg.tr(p, "set-port.usage.spawn"), Msg.tr(p, "help.command.set-spawn")))
            );
            return;
        }

        String sign = Config.CURRENCY_SIGN.getString();
        Location loc = port.location();

        Msg.send(p, Msg.prefix(p).append(Component.text(Msg.tr(p, "port.info.title.my-town-port"), Msg.BRAND)));
        Msg.send(p, Msg.prefix(p)
                .append(Component.text(Msg.tr(p, "port.info.label.town"), Msg.MUTED))
                .append(Component.text(town.getName(), Msg.ACCENT))
        );

        Msg.send(p, Msg.prefix(p)
                .append(Component.text(Msg.tr(p, "port.info.label.fee"), Msg.MUTED))
                .append(Component.text(port.portPrice() + " " + sign, Msg.GOOD))
        );

        // Coords line with click-to-copy (suggest into chat)
        String coordText = loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
        Component coords = Component.text(coordText, NamedTextColor.WHITE)
                .hoverEvent(HoverEvent.showText(Component.text(Msg.tr(p, "general.copy.coords-suggest"), NamedTextColor.WHITE)))
                .clickEvent(ClickEvent.suggestCommand(coordText));

        Msg.send(p, Msg.prefix(p)
                .append(Component.text(Msg.tr(p, "port.info.label.location"), Msg.MUTED))
                .append(coords)
        );

        // Quick travel button
        Msg.send(p, Msg.prefix(p)
                .append(Component.text(Msg.tr(p, "port.info.label.travel"), Msg.MUTED))
                .append(Msg.runCmd(Msg.tr(p, "port.info.travel.label", town.getName()),
                        "/t port " + town.getName(),
                        Msg.tr(p, "port.info.travel.hover")))
        );
    }

    private void showPortHere(Player p) {
        TownyAPI api = TownyAPI.getInstance();

        if (api.isWilderness(p.getLocation())) {
            Msg.warn(p, "port.here.error.in-wilderness");
            return;
        }

        WorldCoord wc = WorldCoord.parseWorldCoord(p.getLocation());
        TownBlock tb = api.getTownBlock(wc);
        Town hereTown = api.getTownOrNull(tb);

        if (hereTown == null) {
            Msg.warn(p, "port.here.error.not-in-town");
            return;
        }

        Port port = PortDAO.getPort(hereTown);
        if (port == null) {
            Msg.warn(p, "port.here.error.town-no-port", hereTown.getName());
            return;
        }

        WorldCoord portWc = WorldCoord.parseWorldCoord(port.location());
        boolean isPortChunk = wc.equals(portWc);

        Msg.send(p, Msg.prefix(p).append(Component.text(Msg.tr(p, "port.here.title"), Msg.BRAND)));
        Msg.send(p, Msg.prefix(p)
                .append(Component.text(Msg.tr(p, "port.info.label.town"), Msg.MUTED))
                .append(Component.text(hereTown.getName(), Msg.ACCENT))
        );

        if (isPortChunk) {
            Msg.success(p, "port.here.success.in-port-chunk");
            Msg.send(p, Msg.prefix(p)
                    .append(Component.text(Msg.tr(p, "port.info.label.travel"), Msg.MUTED))
                    .append(Msg.runCmd(Msg.tr(p, "port.here.travel.list.label"),
                            "/t port list",
                            Msg.tr(p, "port.here.travel.list.hover")))
            );
        } else {
            Msg.warn(p, "port.here.error.not-port-chunk");
            // Show where the port chunk is, with click-to-copy coords
            Location loc = port.location();
            String coordText = loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();

            Component coords = Component.text(coordText, NamedTextColor.WHITE)
                    .hoverEvent(HoverEvent.showText(Component.text(Msg.tr(p, "general.copy.coords-suggest"), NamedTextColor.WHITE)))
                    .clickEvent(ClickEvent.suggestCommand(coordText));

            Msg.send(p, Msg.prefix(p)
                    .append(Component.text(Msg.tr(p, "port.here.label.port-spawn"), Msg.MUTED))
                    .append(coords)
            );
        }
    }

    private void showTownPortInfo(Player viewer, String townName) {
        try {
            Town town = getTownOrThrow(townName);
            Port port = PortDAO.getPort(town);

            if (port == null) {
                Msg.warn(viewer, "port.info.error.town-no-port", town.getName());
                return;
            }

            String sign = Config.CURRENCY_SIGN.getString();
            var loc = port.location();

            Msg.send(viewer, Msg.prefix(viewer).append(net.kyori.adventure.text.Component.text(Msg.tr(viewer, "port.info.title.town-port"), Msg.BRAND)));
            Msg.send(viewer, Msg.prefix(viewer)
                    .append(net.kyori.adventure.text.Component.text(Msg.tr(viewer, "port.info.label.town"), Msg.MUTED))
                    .append(net.kyori.adventure.text.Component.text(town.getName(), Msg.ACCENT))
            );
            Msg.send(viewer, Msg.prefix(viewer)
                    .append(net.kyori.adventure.text.Component.text(Msg.tr(viewer, "port.info.label.fee"), Msg.MUTED))
                    .append(net.kyori.adventure.text.Component.text(port.portPrice() + " " + sign, Msg.GOOD))
            );

            String coordText = loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();

            var coords = net.kyori.adventure.text.Component.text(coordText, net.kyori.adventure.text.format.NamedTextColor.WHITE)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            net.kyori.adventure.text.Component.text(Msg.tr(viewer, "general.copy.coords-suggest"), net.kyori.adventure.text.format.NamedTextColor.WHITE)
                    ))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(coordText));

            Msg.send(viewer, Msg.prefix(viewer)
                    .append(net.kyori.adventure.text.Component.text(Msg.tr(viewer, "port.info.label.location"), Msg.MUTED))
                    .append(coords)
            );

            Msg.send(viewer, Msg.prefix(viewer)
                    .append(net.kyori.adventure.text.Component.text(Msg.tr(viewer, "port.info.label.travel"), Msg.MUTED))
                    .append(Msg.runCmd(Msg.tr(viewer, "port.info.travel.label", town.getName()),
                            "/t port " + town.getName(),
                            Msg.tr(viewer, "port.info.travel.hover")))
            );

        } catch (TownyException e) {
            Msg.errorRaw(viewer, e.getMessage());
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
