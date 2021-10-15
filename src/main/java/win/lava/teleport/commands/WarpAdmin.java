package win.lava.teleport.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.lava.teleport.*;
import win.lava.teleport.entities.WarpEntity;
import win.lava.teleport.tasks.TeleportTask;

import java.sql.SQLException;
import java.util.Map;

import static win.lava.teleport.Lang.t;

@Singleton
@CommandAlias("awarp")
@CommandPermission(Permissions.ADMIN)
public class WarpAdmin extends BaseCommand {
  @Inject private LavaTeleport plugin;
  @Inject private TeleportManager teleportManager;

  public WarpAdmin() {

  }

  @Subcommand("set")
  @CommandCompletion("<name> @nothing")
  public void setwarp(CommandSender sender, String name) {
    if (!(sender instanceof Player)) return;

    var player = (Player) sender;
    var location = player.getLocation();

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        var e = Database.getWarpEntityDao().queryBuilder().where().eq("name", name).queryForFirst();
        if (e == null) e = new WarpEntity();

        e.setName(name);
        e.setWorld(location.getWorld().getName());
        e.setX(location.getX());
        e.setY(location.getY());
        e.setZ(location.getZ());
        e.setPitch(location.getPitch());
        e.setYaw(location.getYaw());

        Database.getWarpEntityDao().createOrUpdate(e);

        sender.sendMessage(t("warps.warp set", Map.of("[name]", name)));
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
    });
  }

  @Subcommand("remove")
  @CommandCompletion("@warps @nothing")
  public void delwarp(CommandSender sender, String name) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        var deleteBuilder = Database.getWarpEntityDao().deleteBuilder();
        deleteBuilder.where().eq("name", name);
        if (deleteBuilder.delete() > 0) {
          sender.sendMessage(t("warps.warp deleted", Map.of("[name]", name)));
          return;
        }

        sender.sendMessage(t("warps.warp not found", Map.of("[name]", name)));
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
    });
  }

  @Subcommand("tp")
  @CommandPermission(Permissions.ADMIN)
  @CommandCompletion("@warps *")
  public void tp(CommandSender sender, String name, OnlinePlayer target) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        var warp = Database.getWarpEntityDao().queryBuilder().where().eq("name", name).queryForFirst();
        if (warp == null) {
          sender.sendMessage(t("warps.warp not found", Map.of("[name]", name)));
          return;
        }

        if (!sender.hasPermission(Permissions.WARP(warp.getName()))) {
          sender.sendMessage(t("warps.no permissions", Map.of("[name]", name)));
          return;
        }

        var location = new Location(Bukkit.getWorld(warp.getWorld()), warp.getX(), warp.getY(), warp.getZ(), (float) warp.getYaw(), (float) warp.getPitch());
        teleportManager.teleportPlayer(
            new TeleportTask(target.getPlayer(), location) {
              @Override
              public void run() {
                super.run();

                PaperLib.teleportAsync(player, location);
                plugin.log(player, location, "admin warp to " + name);

                target.getPlayer().sendMessage(t("warps.teleported", Map.of("[name]", name)));
                sender.sendMessage(t("warps.player teleported", Map.of("[name]", name, "[player]", target.getPlayer().getName())));
              }

              @Override
              public PlayerTeleportData.TELEPORT_KIND getTeleportKind() {
                return PlayerTeleportData.TELEPORT_KIND.WARP;
              }
            }
                .setHighPriority(true)
                .setShouldIgnoreDelay(true)
        );
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
    });
  }
}
