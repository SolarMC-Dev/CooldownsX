package com.github.sirblobman.plugin.cooldown.listener;

import com.github.sirblobman.plugin.cooldown.CooldownPlugin;
import com.github.sirblobman.plugin.cooldown.api.configuration.Cooldown;
import com.github.sirblobman.plugin.cooldown.api.data.PlayerCooldown;
import com.github.sirblobman.plugin.cooldown.api.data.PlayerCooldownManager;
import com.github.sirblobman.plugin.cooldown.api.listener.CooldownListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Set;

public final class ListenerDeath extends CooldownListener {

    public ListenerDeath(CooldownPlugin plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerCooldownManager cooldownManager = getCooldownManager();
        PlayerCooldown cooldownData = cooldownManager.getData(player);

        Set<Cooldown> activeCooldowns = cooldownData.getActiveCooldowns();
        if (activeCooldowns.isEmpty()) return;

        for (Cooldown cooldown : activeCooldowns) {
            if (cooldown.isResetsOnDeath()) {
                cooldownData.removeCooldown(cooldown);
            }
        }
    }
}