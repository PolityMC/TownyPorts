package com.earthpol.townyports.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Centralized styling + messaging helpers for TownyPorts.
 *
 * Goals:
 * - One consistent prefix + palette
 * - Components for clickable/hoverable UX
 * - Safe for console/non-player senders
 */
public final class Msg {

    private Msg() {}

    // Palette
    public static final NamedTextColor BRAND = NamedTextColor.GOLD;
    public static final NamedTextColor ACCENT = NamedTextColor.AQUA;
    public static final NamedTextColor GOOD = NamedTextColor.GREEN;
    public static final NamedTextColor WARN = NamedTextColor.YELLOW;
    public static final NamedTextColor BAD  = NamedTextColor.RED;
    public static final NamedTextColor MUTED = NamedTextColor.GRAY;

    public static final Component PREFIX =
            Component.text("[", BRAND)
                    .append(Component.text("TownyPorts", BRAND, TextDecoration.BOLD))
                    .append(Component.text("] ", BRAND))
                    .decoration(TextDecoration.ITALIC, false);

    public static Component plain(String s) {
        return Component.text(Objects.requireNonNullElse(s, "")).decoration(TextDecoration.ITALIC, false);
    }

    public static void send(CommandSender to, Component component) {
        if (to == null) return;
        to.sendMessage(component.decoration(TextDecoration.ITALIC, false));
    }

    public static void info(CommandSender to, String message) {
        send(to, PREFIX.append(Component.text(message, NamedTextColor.WHITE)));
    }

    public static void success(CommandSender to, String message) {
        send(to, PREFIX.append(Component.text(message, GOOD)));
    }

    public static void warn(CommandSender to, String message) {
        send(to, PREFIX.append(Component.text(message, WARN)));
    }

    public static void error(CommandSender to, String message) {
        send(to, PREFIX.append(Component.text(message, BAD)));
    }

    public static void usage(CommandSender to, Component usageLine) {
        send(to, PREFIX.append(Component.text("Usage: ", MUTED)).append(usageLine));
    }

    /** Click-to-copy (suggests the command in chat). */
    public static Component cmd(String command, String description) {
        return Component.text(command, ACCENT)
                .hoverEvent(HoverEvent.showText(Component.text(description, NamedTextColor.WHITE)))
                .clickEvent(ClickEvent.suggestCommand(command))
                .decoration(TextDecoration.ITALIC, false);
    }

    /** Click-to-run (executes the command). */
    public static Component runCmd(String label, String run, String hover) {
        TextComponent base = Component.text(label, ACCENT)
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.WHITE)))
                .clickEvent(ClickEvent.runCommand(run))
                .decoration(TextDecoration.ITALIC, false);
        return base;
    }

    public static Component styledCoords(int x, int y, int z) {
        String raw = x + " " + y + " " + z;

        return Component.text()
                .append(Component.text("X ", MUTED))
                .append(Component.text(x, ACCENT))
                .append(Component.text("  Y ", MUTED))
                .append(Component.text(y, ACCENT))
                .append(Component.text("  Z ", MUTED))
                .append(Component.text(z, ACCENT))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to copy coords", NamedTextColor.WHITE)))
                .clickEvent(ClickEvent.suggestCommand(raw))
                .build();
    }

    public static boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }
}
