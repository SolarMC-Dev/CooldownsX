package com.github.sirblobman.cooldowns.dictionary;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.EntityType;

import com.github.sirblobman.cooldowns.api.CooldownsX;

public final class EntityDictionary extends Dictionary<EntityType> {
    public EntityDictionary(@NotNull CooldownsX plugin) {
        super(plugin, "dictionary/entity.yml", EntityType.class);
    }
}
