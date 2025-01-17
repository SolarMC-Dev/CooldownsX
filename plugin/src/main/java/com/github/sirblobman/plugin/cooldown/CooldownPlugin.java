package com.github.sirblobman.plugin.cooldown;

import java.util.logging.Logger;

import com.github.sirblobman.plugin.cooldown.listener.*;
import org.jetbrains.annotations.NotNull;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.sirblobman.api.configuration.ConfigurationManager;
import com.github.sirblobman.api.core.CorePlugin;
import com.github.sirblobman.api.language.Language;
import com.github.sirblobman.api.language.LanguageManager;
import com.github.sirblobman.api.plugin.ConfigurablePlugin;
import com.github.sirblobman.api.update.SpigotUpdateManager;
import com.github.sirblobman.api.utility.VersionUtility;
import com.github.sirblobman.plugin.cooldown.api.CooldownsX;
import com.github.sirblobman.plugin.cooldown.api.configuration.EnumDictionary;
import com.github.sirblobman.plugin.cooldown.api.data.PlayerCooldownManager;
import com.github.sirblobman.plugin.cooldown.command.CommandCooldownsX;
import com.github.sirblobman.plugin.cooldown.dictionary.EntityDictionary;
import com.github.sirblobman.plugin.cooldown.dictionary.MaterialDictionary;
import com.github.sirblobman.plugin.cooldown.dictionary.PotionDictionary;
import com.github.sirblobman.plugin.cooldown.manager.CooldownManager;
import com.github.sirblobman.plugin.cooldown.placeholder.HookPlaceholderAPI;
import com.github.sirblobman.plugin.cooldown.task.ActionBarTask;
import com.github.sirblobman.plugin.cooldown.task.ExpireTask;
import com.github.sirblobman.api.shaded.bstats.bukkit.Metrics;
import com.github.sirblobman.api.shaded.bstats.charts.SimplePie;
import com.github.sirblobman.api.shaded.xseries.XMaterial;
import com.github.sirblobman.api.shaded.xseries.XPotion;

public final class CooldownPlugin extends ConfigurablePlugin implements CooldownsX {
    private final CooldownManager cooldownManager;
    private final MaterialDictionary materialDictionary;
    private final PotionDictionary potionDictionary;
    private final EntityDictionary entityDictionary;

    public CooldownPlugin() {
        this.cooldownManager = new CooldownManager(this);
        this.materialDictionary = new MaterialDictionary(this);
        this.potionDictionary = new PotionDictionary(this);
        this.entityDictionary = new EntityDictionary(this);
    }

    @Override
    public @NotNull ConfigurablePlugin getPlugin() {
        return this;
    }

    @Override
    public void onLoad() {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.saveDefault("config.yml");
        configurationManager.saveDefault("cooldowns.yml");
        configurationManager.saveDefault("dictionary/material.yml");
        configurationManager.saveDefault("dictionary/potion.yml");
        configurationManager.saveDefault("dictionary/entity.yml");

        LanguageManager languageManager = getLanguageManager();
        languageManager.saveDefaultLanguageFiles();
    }

    @Override
    public void onEnable() {
        int minorVersion = VersionUtility.getMinorVersion();
        if (minorVersion < 8) {
            Logger logger = getLogger();
            logger.warning("This plugin requires version 1.8.8 or above!");
            setEnabled(false);
            return;
        }

        reloadConfiguration();

        LanguageManager languageManager = getLanguageManager();
        languageManager.onPluginEnable();

        registerCommands();
        registerListeners(minorVersion);
        registerHooks();

        registerUpdateChecker();
        register_bStats();
    }

    @Override
    public void onDisable() {
        // Empty Method
    }

    @Override
    public void reloadConfiguration() {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.reload("config.yml");
        configurationManager.reload("cooldowns.yml");
        configurationManager.reload("dictionary/material.yml");
        configurationManager.reload("dictionary/potion.yml");

        LanguageManager languageManager = getLanguageManager();
        languageManager.reloadLanguages();

        EnumDictionary<XMaterial> materialDictionary = getMaterialDictionary();
        materialDictionary.reloadConfiguration();
        materialDictionary.saveConfiguration();

        EnumDictionary<XPotion> potionDictionary = getPotionDictionary();
        potionDictionary.reloadConfiguration();
        potionDictionary.saveConfiguration();

        EnumDictionary<EntityType> entityDictionary = getEntityDictionary();
        entityDictionary.reloadConfiguration();
        entityDictionary.saveConfiguration();

        PlayerCooldownManager cooldownManager = getCooldownManager();
        cooldownManager.reloadConfig();
        registerTasks();
    }

    @Override
    public @NotNull PlayerCooldownManager getCooldownManager() {
        return this.cooldownManager;
    }

    @Override
    public @NotNull EnumDictionary<XMaterial> getMaterialDictionary() {
        return this.materialDictionary;
    }

    @Override
    public @NotNull EnumDictionary<XPotion> getPotionDictionary() {
        return this.potionDictionary;
    }

    @Override
    public @NotNull EnumDictionary<EntityType> getEntityDictionary() {
        return this.entityDictionary;
    }

    private void registerCommands() {
        new CommandCooldownsX(this).register();
    }

    private void registerListeners(int minorVersion) {
        new ListenerConsume(this).register();
        new ListenerInteract(this).register();
        new ListenerPotionThrow(this).register();
        new ListenerDeath(this).register();

        // Totem of Undying was added in 1.11
        if (minorVersion >= 11) {
            new ListenerUndying(this).register();
        }

        // EntityPotionEffectEvent was added in Spigot 1.13.2
        if (minorVersion >= 13) {
            new ListenerPotionModern(this).register();
        } else {
            new ListenerPotionLegacy(this).register();
        }

        // EntityPlaceEvent was drafted in Spigot 1.13.2
        if (minorVersion >= 13) {
            new ListenerPlaceEntity(this).register();
        }
    }

    private void registerTasks() {
        ConfigurationManager configurationManager = getConfigurationManager();
        YamlConfiguration configuration = configurationManager.get("config.yml");
        if (configuration.getBoolean("use-action-bar")) {
            ActionBarTask actionBarTask = new ActionBarTask(this);
            actionBarTask.startAsync();
        }

        ExpireTask expireTask = new ExpireTask(this);
        expireTask.startAsync();
    }

    private void registerHooks() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            new HookPlaceholderAPI(this).register();
        }
    }

    private void registerUpdateChecker() {
        CorePlugin corePlugin = JavaPlugin.getPlugin(CorePlugin.class);
        SpigotUpdateManager updateManager = corePlugin.getSpigotUpdateManager();
        updateManager.addResource(this, 41981L);
    }

    private void register_bStats() {
        Metrics metrics = new Metrics(this, 16126);
        SimplePie languagePie = new SimplePie("selected_language", this::getDefaultLanguageCode);
        metrics.addCustomChart(languagePie);
    }

    private @NotNull String getDefaultLanguageCode() {
        LanguageManager languageManager = getLanguageManager();
        Language defaultLanguage = languageManager.getDefaultLanguage();
        return (defaultLanguage == null ? "none" : defaultLanguage.getLanguageName());
    }
}
