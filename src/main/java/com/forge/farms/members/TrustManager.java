package com.forge.farms.members;

import com.forge.farms.ForgeFarms;
import com.forge.farms.farm.Farm;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Built-in trust system: roles + granular flags per farm. Replaces the
 * paid ChestProtect dependency — no external plugin required.
 */
public final class TrustManager {
    private final ForgeFarms plugin;
    /** farmId -> (playerId -> role). */
    private final Map<UUID, Map<UUID, FarmRole>> members = new ConcurrentHashMap<>();

    public TrustManager(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    /** Load all member rows after farms are loaded. */
    public void loadAll() {
        members.clear();
        for (Map.Entry<UUID, Map<UUID, String>> e : plugin.database().loadMembers().entrySet()) {
            Map<UUID, FarmRole> roles = new ConcurrentHashMap<>();
            for (Map.Entry<UUID, String> r : e.getValue().entrySet()) {
                roles.put(r.getKey(), FarmRole.parse(r.getValue()));
            }
            members.put(e.getKey(), roles);
        }
    }

    public void clear() {
        members.clear();
    }

    public void saveAll() {
        for (Map.Entry<UUID, Map<UUID, FarmRole>> e : members.entrySet()) {
            saveFarm(e.getKey());
        }
    }

    private void saveFarm(UUID farmId) {
        Map<UUID, FarmRole> roles = members.get(farmId);
        Map<UUID, String> raw = new HashMap<>();
        if (roles != null) {
            for (Map.Entry<UUID, FarmRole> e : roles.entrySet()) {
                raw.put(e.getKey(), e.getValue().name());
            }
        }
        plugin.database().saveMembers(farmId, raw);
    }

    public void removeFarm(UUID farmId) {
        members.remove(farmId);
    }

    public Map<UUID, FarmRole> getMembers(Farm farm) {
        return new HashMap<>(members.getOrDefault(farm.id(), Map.of()));
    }

    public @Nullable FarmRole getRole(Farm farm, UUID player) {
        if (farm.owner().equals(player)) {
            return FarmRole.OWNER;
        }
        Map<UUID, FarmRole> roles = members.get(farm.id());
        return roles == null ? null : roles.get(player);
    }

    /**
     * Central permission check: owner always passes, Bukkit bypass
     * permission passes, otherwise the member's role flags decide.
     */
    public boolean can(Player player, Farm farm, FarmFlag flag) {
        if (farm.owner().equals(player.getUniqueId())) {
            return true;
        }
        if (player.hasPermission("forgefarms.bypass.protection")) {
            return true;
        }
        FarmRole role = getRole(farm, player.getUniqueId());
        return role != null && role.has(flag);
    }

    /** Non-player variant (no bypass permission available). */
    public boolean can(UUID playerId, Farm farm, FarmFlag flag) {
        if (farm.owner().equals(playerId)) {
            return true;
        }
        FarmRole role = getRole(farm, playerId);
        return role != null && role.has(flag);
    }

    public boolean addMember(Farm farm, UUID player, FarmRole role) {
        if (player.equals(farm.owner())) {
            return false;
        }
        Map<UUID, FarmRole> roles = members.computeIfAbsent(farm.id(), k -> new ConcurrentHashMap<>());
        if (roles.size() >= plugin.config().maxMembersPerFarm() && !roles.containsKey(player)) {
            return false;
        }
        roles.put(player, role);
        saveFarm(farm.id());
        return true;
    }

    public boolean removeMember(Farm farm, UUID player) {
        Map<UUID, FarmRole> roles = members.get(farm.id());
        if (roles == null || roles.remove(player) == null) {
            return false;
        }
        saveFarm(farm.id());
        return true;
    }

    public boolean setRole(Farm farm, UUID player, FarmRole role) {
        Map<UUID, FarmRole> roles = members.get(farm.id());
        if (roles == null || !roles.containsKey(player)) {
            return false;
        }
        roles.put(player, role);
        saveFarm(farm.id());
        return true;
    }
}
