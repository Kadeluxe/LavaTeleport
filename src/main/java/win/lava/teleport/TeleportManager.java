package win.lava.teleport;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import win.lava.teleport.tasks.TeleportTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static win.lava.teleport.Lang.t;

@Singleton
public class TeleportManager implements Listener {
  public HashMap<UUID, PlayerTeleportData> playerDataMap = new HashMap<>();
  public HashMap<Player, TeleportTask> playerTeleportTaskMap = new HashMap<>();

  @Inject protected LavaTeleport plugin;
  @Inject protected Settings settings;

  public TeleportManager() {

  }

  public TeleportTask getPlayerTeleportTask(Player player) {
    return playerTeleportTaskMap.get(player);
  }

  public void setPlayerTeleportTask(Player player, TeleportTask task) {
    if (task == null) {
      playerTeleportTaskMap.remove(player);
      return;
    }

    playerTeleportTaskMap.put(player, task);
  }

  public PlayerTeleportData getPlayerTeleportData(Player player) {
    return playerDataMap.get(player.getUniqueId());
  }

  public void teleportPlayer(TeleportTask task) {
    plugin.getInjector().injectMembers(task);

    var currentTask = getPlayerTeleportTask(task.getPlayer());

    if (currentTask != null) {
      if ((currentTask.isCancelable() && currentTask.isOverwriteAllowed()) || task.isHighPriority()) {
        currentTask.cancel(CancelReason.OVERRIDE);
      } else {
        task.cancel(CancelReason.CANNOT_OVERRIDE);
        return;
      }
    }

    BukkitTask bukkitTask;

    task.setInitialLocation(task.getPlayer().getLocation());

    if (task.shouldIgnoreDelay()) {
      bukkitTask = Bukkit.getScheduler().runTask(plugin, task);
    } else {
      var delay = settings.getTeleportDelay(task.getTeleportKind());
      task.getPlayer().sendMessage(t("standby", Map.of("[delay]", Integer.toString(delay))));

      bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, LavaTeleport.TPS * delay);
//      task.setProgressTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new ReportProgressTask(task.getPlayer(), 20 * delay), 0, ReportProgressTask.INTERVAL));
    }

    task.setBukkitTask(bukkitTask);
    playerTeleportTaskMap.put(task.getPlayer(), task);
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent ev) {
    var entity = ev.getEntity();
    if (!(entity instanceof Player)) return;

    var player = (Player) entity;

    var task = getPlayerTeleportTask(player);
    if (task != null && task.isCancelable()) {
      task.cancel(CancelReason.DAMAGE);
      return;
    }

    var cause = ev.getCause();
    var playerData = getPlayerTeleportData(player);

    if (cause == EntityDamageEvent.DamageCause.SUFFOCATION && playerData.isProtectedFromSuffocationDamage()) {
      ev.setCancelled(true);

      if (playerData.getTimeSinceLastTeleport() < 3) {
        player.teleport(playerData.getLastLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
      }
    } else if (cause == EntityDamageEvent.DamageCause.FALL && playerData.isProtectedFromFallingDamage()) {
      ev.setCancelled(true);
    } else if (cause == EntityDamageEvent.DamageCause.VOID && playerData.isProtectedFromFallingDamage()) {
      ev.setCancelled(true);

      var spawn = plugin.getEssentialsSpawn().getSpawn("default");
      player.teleport(plugin.getEssentialsSpawn().getSpawn("default"));
      plugin.log(player, spawn, "void damage");
    }
  }

  @EventHandler
  public void onEntityDamageByEntity(EntityDamageByEntityEvent ev) {
    var damager = ev.getDamager();
    var entity = ev.getEntity();

    if (!(damager instanceof Player) && !(entity instanceof Player)) return;

    if (entity instanceof Player) {
      var playerData = getPlayerTeleportData((Player) entity);

      if (playerData.isProtectedFromPlayerDamage()) {
        ev.setCancelled(true);

        if (damager instanceof Player) {
          damager.sendMessage(t("target-protected"));
        }
      } else {
        var task = getPlayerTeleportTask((Player) entity);
        if (task != null && task.isCancelable()) {
          task.cancel(CancelReason.DAMAGE);
        }
      }
    }

    if (damager instanceof Player) {
      var playerData = getPlayerTeleportData((Player) damager);

      if (playerData.isProtectedFromPlayerDamage() && entity instanceof Player) {
        ev.setCancelled(true);

        damager.sendMessage(t("damage-protected"));
      }
    }
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent ev) {
    var player = ev.getPlayer();

    var playerData = getPlayerTeleportData(player);
    playerData.updateProtectionStatus(player);

    var task = getPlayerTeleportTask(player);
    if (task == null) return;

    Location initial = task.getInitialLocation();
    Location to = ev.getTo();
    if (to == null) return;

    if (to.getBlockX() != initial.getBlockX() ||
            to.getBlockY() != initial.getBlockY() ||
            to.getBlockZ() != initial.getBlockZ()
    ) {
      if (task.isCancelable() && !player.hasPermission(Permissions.ADMIN)) {
        task.cancel(CancelReason.MOVEMENT);
      }
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent ev) {
    var playerData = playerDataMap.computeIfAbsent(ev.getPlayer().getUniqueId(), uuid -> new PlayerTeleportData(plugin.getSettings()));

    playerData.markAsDeleted(false);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent ev) {
    var task = playerTeleportTaskMap.remove(ev.getPlayer());
    if (task != null) {
      task.cancel(CancelReason.SRC_LEAVE);
    }

    playerDataMap.get(ev.getPlayer().getUniqueId()).markAsDeleted(true);
  }

  public void cleanup() {
    playerDataMap.entrySet().removeIf(pair -> pair.getValue().getDeletedAt() > 0);
  }
}
