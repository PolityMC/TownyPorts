package net.euromc.townyports.commands;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.utils.NameUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PortTabCommand implements TabCompleter {

    private static final List<String> townConsoleTabCompletes = Arrays.asList(
            "?",
            "town-name"
    );

    public static List<String> filterByStartOrGetTownyStartingWith(List<String> filters, String arg, String type) {
        List<String> filtered = NameUtil.filterByStart(filters, arg);
        if (type.isEmpty())
            return filtered;
        else if (type.contains("+")) {
            filtered.addAll(getTownyStartingWith(arg, type));
            return filtered;
        } else {
            if (filtered.size() > 0) {
                return filtered;
            } else {
                return getTownyStartingWith(arg, type);
            }
        }
    }

    public static List<String> getTownyStartingWith(String arg, String type) {

        List<String> matches = new ArrayList<>();
        TownyUniverse townyUniverse = TownyUniverse.getInstance();
        if (type.contains("t")) {
            matches.addAll(townyUniverse.getTownsTrie().getStringsFromKey(arg));
        }
        return matches;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player) {
            if(args.length == 1) {
                return PortTabCommand.filterByStartOrGetTownyStartingWith(townConsoleTabCompletes, args[0], "t");
            }
        }
        return null;
    }


}
