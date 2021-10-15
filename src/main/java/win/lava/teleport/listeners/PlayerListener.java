package win.lava.teleport.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.Vector;
import win.lava.teleport.Database;
import win.lava.teleport.LavaTeleport;
import win.lava.teleport.Settings;
import win.lava.teleport.TeleportManager;
import win.lava.teleport.entities.QuitPositionEntity;

import java.sql.SQLException;

@Singleton
public class PlayerListener implements Listener {
  @Inject
  LavaTeleport plugin;
  @Inject
  Settings settings;
  @Inject
  TeleportManager teleportManager;

  AuthMeApi authMe = AuthMeApi.getInstance();

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent ev) {
    if (settings.isAuthSpawnOnJoin()) {
      var player = ev.getPlayer();
      player.setVelocity(new Vector());
      player.teleport(plugin.getEssentialsSpawn().getSpawn("default"));
    }
  }

  @EventHandler
  public void onPlayerRespawn(PlayerRespawnEvent ev) {
    if(!ev.isBedSpawn() && !ev.isAnchorSpawn()) {
      ev.setRespawnLocation(plugin.getEssentialsSpawn().getSpawn("default"));
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent ev) {
    var player = ev.getPlayer();
    if (!player.isOnline() || !authMe.isAuthenticated(player)) return;

    var location = /*player.isDead() ? plugin.getEssentialsSpawn().getSpawn("default") :*/ player.getLocation().clone();
    var velocity = player.getVelocity();
    var isGliding = player.isGliding();

    var task = teleportManager.getPlayerTeleportTask(player);

    if (task != null && !task.isCancelable()) {
      location = task.getLocation();
      velocity = new Vector();
      isGliding = false;
    }

//    if (settings.isAuthSpawnOnJoin()) {
//      var nmsPlayer = ((CraftPlayer) player).getHandle();
//      nmsPlayer.defaultContainer.b(nmsPlayer);
//      if (nmsPlayer.activeContainer != null) {
//        nmsPlayer.activeContainer.b(nmsPlayer);
//      }
//
//      player.teleport(plugin.getEssentialsSpawn().getSpawn("default"));
//    }

    Location _location = location;
    Vector _velocity = velocity;
    boolean _isGliding = isGliding;
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        var e = Database.getQuitPositionEntityDao().queryBuilder().where().eq("uuid", player.getUniqueId().toString()).queryForFirst();
        if (e == null) e = new QuitPositionEntity();

        e.setUuid(player.getUniqueId().toString());
        e.setWorld(_location.getWorld().getName());
        e.setX(_location.getX());
        e.setY(Math.ceil(_location.getY()));
        e.setZ(_location.getZ());
        e.setPitch(_location.getPitch());
        e.setYaw(_location.getYaw());

        e.setVelX(_velocity.getX());
        e.setVelY(_velocity.getY());
        e.setVelZ(_velocity.getZ());

        e.setGliding(_isGliding);

        Database.getQuitPositionEntityDao().createOrUpdate(e);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    });
  }

  @EventHandler
  public void onPlayerLogin(LoginEvent ev) {
    if(!settings.isRestoreLocationOnAuth()) return;

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      var player = ev.getPlayer();

      try {
        var e = Database.getQuitPositionEntityDao().queryBuilder().where().eq("uuid", player.getUniqueId().toString()).queryForFirst();
        if (e == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
          var syncPlayer = Bukkit.getPlayer(player.getUniqueId());

          if (syncPlayer != null && !syncPlayer.isDead()) {
            var world = Bukkit.getServer().getWorld(e.getWorld());
            var location = new Location(
                world,
                e.getX(),
                e.getY(),
                e.getZ(),
                e.getYaw(),
                e.getPitch()
            );

            PaperLib.teleportAsync(syncPlayer, location).thenAccept(status -> {
              plugin.log(player, location, "login");

              if (player.isDead()) return;

              syncPlayer.setVelocity(new Vector(e.getVelX(), e.getVelY(), e.getVelZ()));
              Bukkit.getScheduler().runTaskLater(plugin, () -> {
                syncPlayer.setGliding(true);
              }, 1);
            });
          }
        });
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
    });
  }
}
