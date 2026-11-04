package top.speedcubing.limbo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import top.speedcubing.lib.bukkit.pluginMessage.BungeePluginMessage;
import top.speedcubing.server.utils.MapManager;

public class Limbo extends JavaPlugin implements Listener {
    // any activity sends the player back; one request every few seconds is enough
    // (move events fire per packet) while still retrying if the lobby was unreachable
    private static final long SWITCH_INTERVAL_MS = 3000;

    private final Map<UUID, Long> lastSwitch = new HashMap<>();
    private World world;

    public void lobby(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastSwitch.get(player.getUniqueId());
        if (last != null && now - last < SWITCH_INTERVAL_MS)
            return;
        lastSwitch.put(player.getUniqueId(), now);
        BungeePluginMessage.switchServer(player, "lobby");
    }

    public void onEnable() {
        MinecraftServer.getServer().getPropertyManager().setProperty("server-name", "limbo");
        Bukkit.getPluginManager().registerEvents(this, this);

        try {
            world = MapManager.install("map_limbo", "map_limbo", Difficulty.PEACEFUL, 6000, false);
            if (world == null)
                throw new IllegalStateException("map_limbo did not load");
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getServer().shutdown();
        }
    }

    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent e) {
        e.setJoinMessage("");
        e.getPlayer().setGameMode(GameMode.SURVIVAL);
        e.getPlayer().teleport(new Location(world, 0.5, 100, 0.5, 0, 0));
        for (Player p : Bukkit.getOnlinePlayers()) {
            e.getPlayer().hidePlayer(p);
            p.hidePlayer(e.getPlayer());
        }
    }

    @EventHandler
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void PlayerMoveEvent(PlayerMoveEvent e) {
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
            lobby(e.getPlayer());
    }

    @EventHandler
    public void PlayerToggleSneakEvent(PlayerToggleSneakEvent e) {
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
            lobby(e.getPlayer());
    }

    @EventHandler
    public void BlockBreakEvent(BlockBreakEvent e) {
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            lobby(e.getPlayer());
        }
    }

    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent e) {
        e.setQuitMessage("");
        lastSwitch.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void FoodLevelChangeEvent(FoodLevelChangeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void EntityDamageEvent(EntityDamageEvent e) {
        e.setCancelled(true);
    }
}
