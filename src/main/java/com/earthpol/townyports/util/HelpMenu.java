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

        lines.add(Msg.prefix(sender).append(Component.text(Msg.tr(sender, "help.header.commands"), Msg.BRAND)));
        lines.add(Component.text(" ", NamedTextColor.WHITE));

        lines.add(line(sender, Msg.tr(sender, "port.usage.base"), "help.command.port"));
        lines.add(line(sender, Msg.tr(sender, "port.usage.price"), "help.command.price"));
        lines.add(line(sender, Msg.tr(sender, "port.usage.list"), "help.command.list"));
        lines.add(line(sender, Msg.tr(sender, "port.usage.info-town"), "help.command.info"));
        lines.add(line(sender, Msg.tr(sender, "port.usage.here"), "help.command.here"));
        lines.add(line(sender, Msg.tr(sender, "set-port.usage.spawn"), "help.command.set-spawn"));
        lines.add(line(sender, Msg.tr(sender, "set-port.usage.price"), "help.command.set-price"));
        lines.add(line(sender, Msg.tr(sender, "set-port.usage.remove"), "help.command.set-remove"));

        lines.add(Component.text(" ", NamedTextColor.WHITE));
        lines.add(Msg.prefix(sender).append(Component.text(Msg.tr(sender, "help.tip.label"), Msg.MUTED))
                .append(Component.text(Msg.tr(sender, "help.tip.copy-command"), Msg.MUTED)));

        for (Component c : lines) Msg.send(sender, c);
    }

    private static Component line(CommandSender sender, String cmd, String descKey) {
        return Msg.prefix(sender)
                .append(Component.text("• ", Msg.MUTED))
                .append(Msg.cmd(sender, cmd, descKey));
    }
}
