package com.dqc.cardsdp;

import com.dqc.cardsdp.definitions.BuiltinDefinitions;
import com.dqc.cardsdp.definitions.CardsDefinitions;
import com.dqc.cardsdp.i18n.I18nService;
import com.dqc.cardsdp.item.CardsItemService;
import com.dqc.cardsdp.listener.DeckListener;
import com.dqc.cardsdp.listener.JokerListener;
import com.dqc.cardsdp.listener.TableListener;
import com.dqc.cardsdp.table.TableService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CardsPlugin extends JavaPlugin {
    private I18nService i18n;
    private TableService tableService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.i18n = new I18nService(this);

        CardsDefinitions definitions = BuiltinDefinitions.create();
        CardsItemService itemService = new CardsItemService(this, definitions);
        this.tableService = new TableService(this, itemService, i18n);

        itemService.registerRecipes();
        tableService.startMaintenanceTask();

        Bukkit.getPluginManager().registerEvents(new TableListener(tableService), this);
        Bukkit.getPluginManager().registerEvents(new DeckListener(itemService), this);
        Bukkit.getPluginManager().registerEvents(new JokerListener(itemService), this);

        i18n.info(getLogger(), "plugin.enabled", Placeholder.unparsed("lang", i18n.locale()));
        i18n.info(getLogger(), "plugin.logic_loaded");
    }

    @Override
    public void onDisable() {
        if (i18n != null) {
            i18n.info(getLogger(), "plugin.disabled");
        }
        if (tableService != null) {
            tableService.shutdown();
            tableService = null;
        }
    }
}
