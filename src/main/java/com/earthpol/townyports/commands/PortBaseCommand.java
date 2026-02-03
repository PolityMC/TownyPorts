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
import com.palmergames.bukkit.towny.object.Town;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = Arrays.asList("help", "list", "price", "reload");
            String token = args[0].toLowerCase(Locale.ROOT);

            // include town names too for /t port <town>
            List<String> towns = filterTownNames(token);

            return concatFiltered(base, towns, token);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String token = args[1].toLowerCase(Locale.ROOT);

            if (sub.equals("price")) return filterTownNames(token);
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

    private static List<String> concatFiltered(List<String> a, List<String> b, String token) {
        List<String> out = new ArrayList<>();
        for (String s : a) if (s.toLowerCase(Locale.ROOT).startsWith(token)) out.add(s);
        out.addAll(b);
        return out;
    }
}
