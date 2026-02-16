package com.dqc.cardsdp.i18n;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class I18nService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private final PlainTextComponentSerializer plainTextSerializer;
    private final String locale;
    private final String fallbackLocale;
    private final Map<String, String> localizedMessages;
    private final Map<String, String> fallbackMessages;

    public I18nService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.plainTextSerializer = PlainTextComponentSerializer.plainText();
        this.locale = normalize(plugin.getConfig().getString("i18n.locale", "zh_cn"));
        this.fallbackLocale = normalize(plugin.getConfig().getString("i18n.fallback", "en_us"));
        this.localizedMessages = loadMessages(this.locale);
        this.fallbackMessages = loadMessages(this.fallbackLocale);
    }

    public String locale() {
        return locale;
    }

    public Component component(String key, TagResolver... placeholders) {
        String template = resolve(key);
        return miniMessage.deserialize(template, placeholders);
    }

    public void send(Player player, String key, TagResolver... placeholders) {
        player.sendMessage(component(key, placeholders));
    }

    public void actionBar(Player player, String key, TagResolver... placeholders) {
        player.sendActionBar(component(key, placeholders));
    }

    public void info(Logger logger, String key, TagResolver... placeholders) {
        logger.info(plainTextSerializer.serialize(component(key, placeholders)));
    }

    public void warning(Logger logger, String key, TagResolver... placeholders) {
        logger.warning(plainTextSerializer.serialize(component(key, placeholders)));
    }

    private String resolve(String key) {
        String local = localizedMessages.get(key);
        if (local != null) {
            return local;
        }
        String fallback = fallbackMessages.get(key);
        if (fallback != null) {
            return fallback;
        }
        return "<red>Missing i18n key: " + key + "</red>";
    }

    private Map<String, String> loadMessages(String targetLocale) {
        Map<String, String> result = new HashMap<>();
        String path = "messages/" + targetLocale + ".yml";
        try (InputStream stream = plugin.getResource(path)) {
            if (stream == null) {
                return result;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
            );
            for (String key : yaml.getKeys(true)) {
                if (yaml.isString(key)) {
                    result.put(key, yaml.getString(key, ""));
                }
            }
        } catch (Exception ignored) {
            // Keep empty map and fallback to other locale.
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "zh_cn" : value.toLowerCase(Locale.ROOT).trim();
    }
}
