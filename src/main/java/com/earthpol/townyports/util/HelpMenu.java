package com.earthpol.townyports.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Clickable help menu renderer.
 *
 * Keep all copy + styling in one place so commands stay clean.
 */
public final class HelpMenu {

    private HelpMenu() {}

    public static void sendPortHelp(CommandSender sender) {
        List<Component> lines = new ArrayList<>();

        lines.add(Msg.PREFIX.append(Component.text("Commands", Msg.BRAND)));
        lines.add(Component.text(" ", NamedTextColor.WHITE));

        lines.add(line("/t port <town>", "Teleport to a town's port (requires being in a port chunk unless bypassed)."));
        lines.add(line("/t port price <town>", "View a town's port fee."));
        lines.add(line("/t port list [page]", "List all towns with active ports."));
        lines.add(line("/t set port spawn", "Set your town's port spawn to your current location."));
        lines.add(line("/t set port price <amount>", "Set your town's port fee."));
        lines.add(line("/t set port remove", "Remove your town's port spawn + fee."));

        lines.add(Component.text(" ", NamedTextColor.WHITE));
        lines.add(Msg.PREFIX.append(Component.text("Tip: ", Msg.MUTED))
                .append(Component.text("Click a command to copy it.", Msg.MUTED)));

        for (Component c : lines) Msg.send(sender, c);
    }

    private static Component line(String cmd, String desc) {
        return Msg.PREFIX
                .append(Component.text("• ", Msg.MUTED))
                .append(Msg.cmd(cmd, desc));
    }
}
