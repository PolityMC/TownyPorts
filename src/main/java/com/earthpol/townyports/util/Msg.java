package com.earthpol.townyports.util;

import com.earthpol.earthPolLib.translation.TranslationService;
import com.earthpol.earthPolLib.translation.Translations;
import com.earthpol.townyports.PortsMain;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();
    private static final String FALLBACK_PREFIX = "&6[TownyPorts]&r ";

    // Palette
    public static final NamedTextColor BRAND = NamedTextColor.GOLD;
    public static final NamedTextColor ACCENT = NamedTextColor.AQUA;
    public static final NamedTextColor GOOD = NamedTextColor.GREEN;
    public static final NamedTextColor WARN = NamedTextColor.YELLOW;
    public static final NamedTextColor BAD  = NamedTextColor.RED;
    public static final NamedTextColor MUTED = NamedTextColor.GRAY;

    private static TranslationService translationService() {
        return PortsMain.getTranslationService();
    }

    public static String tr(String key, Object... args) {
        TranslationService service = translationService();
        if (service == null) {
            return key;
        }
        return Translations.raw(service, key, args);
    }

    public static String tr(CommandSender sender, String key, Object... args) {
        TranslationService service = translationService();
        if (service == null) {
            return key;
        }
        return Translations.raw(service, sender, key, args);
    }

    public static Component prefix(CommandSender sender) {
        String rawPrefix = tr(sender, Translations.DEFAULT_PREFIX_KEY);
        if (Translations.DEFAULT_PREFIX_KEY.equals(rawPrefix)) {
            rawPrefix = FALLBACK_PREFIX;
        }
        return LEGACY_SERIALIZER.deserialize(rawPrefix).decoration(TextDecoration.ITALIC, false);
    }

    public static Component plain(String s) {
        return Component.text(Objects.requireNonNullElse(s, "")).decoration(TextDecoration.ITALIC, false);
    }

    public static void send(CommandSender to, Component component) {
        if (to == null) return;
        to.sendMessage(component.decoration(TextDecoration.ITALIC, false));
    }

    public static void info(CommandSender to, String key, Object... args) {
        send(to, prefix(to).append(Component.text(tr(to, key, args), NamedTextColor.WHITE)));
    }

    public static void success(CommandSender to, String key, Object... args) {
        send(to, prefix(to).append(Component.text(tr(to, key, args), GOOD)));
    }

    public static void warn(CommandSender to, String key, Object... args) {
        send(to, prefix(to).append(Component.text(tr(to, key, args), WARN)));
    }

    public static void error(CommandSender to, String key, Object... args) {
        send(to, prefix(to).append(Component.text(tr(to, key, args), BAD)));
    }

    public static void errorRaw(CommandSender to, String message) {
        send(to, prefix(to).append(Component.text(message, BAD)));
    }

    public static void usage(CommandSender to, String usageKey, Object... args) {
        Component usageLine = Component.text(tr(to, usageKey, args), ACCENT);
        send(to, prefix(to).append(Component.text(tr(to, "general.usage-label"), MUTED)).append(usageLine));
    }

    public static void usage(CommandSender to, Component usageLine) {
        send(to, prefix(to).append(Component.text(tr(to, "general.usage-label"), MUTED)).append(usageLine));
    }

    /** Click-to-copy (suggests the command in chat). */
    public static Component cmd(String command, String description) {
        return Component.text(command, ACCENT)
                .hoverEvent(HoverEvent.showText(Component.text(description, NamedTextColor.WHITE)))
                .clickEvent(ClickEvent.suggestCommand(command))
                .decoration(TextDecoration.ITALIC, false);
    }

    public static Component cmd(CommandSender sender, String command, String descriptionKey, Object... args) {
        return cmd(command, tr(sender, descriptionKey, args));
    }

    /** Click-to-run (executes the command). */
    public static Component runCmd(String label, String run, String hover) {
        return Component.text(label, ACCENT)
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.WHITE)))
                .clickEvent(ClickEvent.runCommand(run))
                .decoration(TextDecoration.ITALIC, false);
    }

    public static Component runCmd(CommandSender sender, String labelKey, String run, String hoverKey, Object... args) {
        return runCmd(tr(sender, labelKey, args), run, tr(sender, hoverKey));
    }

    public static Component styledCoords(CommandSender sender, int x, int y, int z) {
        String raw = x + " " + y + " " + z;

        return Component.text()
                .append(Component.text("X ", MUTED))
                .append(Component.text(x, ACCENT))
                .append(Component.text("  Y ", MUTED))
                .append(Component.text(y, ACCENT))
                .append(Component.text("  Z ", MUTED))
                .append(Component.text(z, ACCENT))
                .hoverEvent(HoverEvent.showText(
                        Component.text(tr(sender, "general.copy.coords"), NamedTextColor.WHITE)))
                .clickEvent(ClickEvent.suggestCommand(raw))
                .build();
    }

    public static boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }
}
