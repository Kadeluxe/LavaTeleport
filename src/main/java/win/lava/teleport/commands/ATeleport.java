package win.lava.teleport.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.lava.teleport.Database;
import win.lava.teleport.LavaTeleport;
import win.lava.teleport.Permissions;

import java.sql.SQLException;

import static win.lava.teleport.Lang.t;
import static win.lava.teleport.modules.HomeModule.findPlayer;

@Singleton
@CommandAlias("ateleport")
@CommandPermission(Permissions.ADMIN)
public class ATeleport extends BaseCommand {
  @Inject private LavaTeleport plugin;

  @Subcommand("quit-get")
  @CommandCompletion("@players @nothing")
  public void quitGet(CommandSender sender, String playerName) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      var player = findPlayer(playerName);

      try {
        var e = Database.getQuitPositionEntityDao().queryBuilder().where().eq("uuid", player.getUniqueId().toString()).queryForFirst();
        if (e == null) {
          sender.sendMessage(t("player not found"));
          return;
        }

        sender.spigot().sendMessage(ComponentSerializer.parse(
            String.format(t("quit position"), player.getName(), e.getX(), e.getY(), e.getZ(), e.getWorld(),
                e.getVelX(), e.getVelY(), e.getVelZ(),
                e.getX(), e.getY(), e.getZ(), e.getWorld()
            )
        ));
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
    });
  }

  @Subcommand("quit-set")
  @CommandCompletion("<имя> x y z world @nothing")
  public void quitSet(CommandSender sender, String playerName, @Optional() Integer x, @Optional Integer y, @Optional Integer z, @Default("world") String worldName) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      var player = findPlayer(playerName);

      Location location;

      if (x == null) {
        if (sender instanceof Player) {
          location = ((Player) sender).getLocation();
        } else {
          return;
        }
      } else if (y != null && z != null) {
        var world = Bukkit.getWorld(worldName);
        if (world == null) return;

        location = new Location(world, x, y, z);
      } else {
        return;
      }

      try {
        var e = Database.getQuitPositionEntityDao().queryBuilder().where().eq("uuid", player.getUniqueId().toString()).queryForFirst();
        if (e == null) return;

        e.setWorld(location.getWorld().getName());
        e.setX(location.getBlockX());
        e.setY(location.getBlockY());
        e.setZ(location.getBlockZ());
        e.setYaw(location.getYaw());
        e.setPitch(location.getPitch());
        e.setVelX(0);
        e.setVelY(0);
        e.setVelZ(0);
        e.setGliding(false);

        Database.getQuitPositionEntityDao().createOrUpdate(e);

        quitGet(sender, playerName);
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
    });
  }
}
