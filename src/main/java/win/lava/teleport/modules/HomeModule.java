package win.lava.teleport.modules;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import win.lava.common._deps_.ormlite.misc.TransactionManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import win.lava.common._deps_.net.kyori.text.TextComponent;
import win.lava.common._deps_.net.kyori.text.adapter.bukkit.TextAdapter;
import win.lava.common._deps_.net.kyori.text.event.ClickEvent;
import win.lava.common._deps_.net.kyori.text.event.HoverEvent;
import win.lava.common._deps_.net.kyori.text.format.TextColor;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.lava.teleport.*;
import win.lava.teleport.tasks.TeleportSafeAsyncTask;
import win.lava.teleport.tasks.TeleportTask;

import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static win.lava.teleport.Lang.t;
import static win.lava.teleport.LavaTeleport.getLuckPerms;

@Singleton
public class HomeModule {
  @Inject private LavaTeleport plugin;
  @Inject private TeleportManager teleportManager;
  @Inject private TeleportSafeAsyncTask.Factory teleportSafeAsyncTaskFactory;

  private Pattern pattern = Pattern.compile("^[a-z0-9_\\-]+$");

  public HomeModule() {

  }

  public static OfflinePlayer findPlayer(String playerName) {
    var player = Bukkit.getPlayer(playerName);
    if (player == null) {
      return Bukkit.getOfflinePlayer(playerName);
    }

    return player;
  }

  static boolean isMemberOfAnyRegion(OfflinePlayer player, ApplicableRegionSet set) {
    if (set.size() == 0) return true;

    var wgPlayer = WorldGuardPlugin.inst().wrapOfflinePlayer(player);
    for (var it : set) {
      if (it.isOwner(wgPlayer) || it.isMember(wgPlayer)) return true;
    }

    return false;
  }

  static boolean isHomeAllowed(Player player, ApplicableRegionSet set) {
    var flag = (StateFlag) WorldGuard.getInstance().getFlagRegistry().get("area-sethome");

    var localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
    return player.hasPermission(Permissions.ADMIN) || set.testState(localPlayer, flag);
  }

  public boolean isSafeLocation(Location location) {
    var feet = location.getBlock();
    if (!feet.getType().isTransparent() && !feet.getLocation().add(0, 1, 0).getBlock().getType().isTransparent()) {
      return false;
    }
    var head = feet.getRelative(BlockFace.UP);
    if (!head.getType().isTransparent()) {
      return false;
    }

    var ground = feet.getRelative(BlockFace.DOWN);
    if (!ground.getType().isSolid()) {
      return false;
    }
    return true;
  }

  public void list(CommandSender sender, OfflinePlayer player) {
    var user = getLuckPerms().getUserManager().loadUser(player.getUniqueId()).join();
    if (user == null) {
      sender.sendMessage(t("no-player-data"));
      return;
    }
    var meta = user.getCachedData().getMetaData(QueryOptions.nonContextual());
    var limit = Integer.parseInt(meta.getMetaValue("lava.teleport.max-homes"));

    var lines = new ArrayList<TextComponent>();

    var line = TextComponent.builder();

    List<win.lava.teleport.entities.PositionEntity> positions;

    try {
      positions = Database.getPositionEntityDao().queryForEq("uuid", player.getUniqueId().toString());
    } catch (SQLException e) {
      e.printStackTrace();
      return;
    }

    if (sender == player) {
      lines.add(TextComponent.of(t("home:saved positions", Map.of(
          "[count]", Integer.toString(positions.size()),
          "[max]", Integer.toString(limit))))
      );
    } else {
      lines.add(TextComponent.of(t("home:saved positions admin", Map.of(
          "[name]", player.getName() == null ? "N/A" : player.getName(),
          "[count]", Integer.toString(positions.size()),
          "[max]", Integer.toString(limit))))
      );
    }

    if (positions.size() == 0) {
      lines.add(TextComponent.of(t("home:no saved positions")));
    } else {
      for (var it : positions) {
        var t = TextComponent.builder(it.getName(), TextColor.AQUA);

        if (sender == player) {
          t.clickEvent(ClickEvent.runCommand(String.format("/home %s", it.getName())));
        } else {
          t.clickEvent(ClickEvent.runCommand(String.format("/ahome %s %s", player.getName(), it.getName())));
        }

        var df = new DecimalFormat("#");
        df.setRoundingMode(RoundingMode.FLOOR);

        t.hoverEvent(HoverEvent.showText(
            TextComponent
                .builder(t("world")).append(" ").append(it.getWorld(), TextColor.WHITE).append("\n")
                .append(t("coordinates")).append(" ").append(String.format("(%s, %s, %s)", df.format(it.getX()), df.format(it.getY()), df.format(it.getZ())), TextColor.WHITE).append("\n")
                .append("\n").append(t("click to teleport"))
                .build()
        ));

        line.append(t.build()).append(" ");
      }
    }

    lines.add(line.build());

    lines.forEach(x -> TextAdapter.sendComponent(sender, x));
  }

  public void delhome(CommandSender sender, OfflinePlayer player, String name) {
    if (name == null) name = "home";
    name = name.toLowerCase();

    if (!pattern.matcher(name).matches()) {
      sender.sendMessage(t("home:incorrect name"));
      return;
    }

    win.lava.teleport.entities.PositionEntity positionEntity;

    try {
      positionEntity = Database.getPositionEntityDao().queryBuilder().where().eq("uuid", player.getUniqueId().toString()).and().eq("name", name).queryForFirst();
      if (positionEntity == null) {
        sender.sendMessage(t("home:position not found", Map.of("[name]", name)));
        return;
      }

      Database.getPositionEntityDao().delete(positionEntity);
    } catch (SQLException e) {
      e.printStackTrace();
      return;
    }

    sender.sendMessage(t("home:deleted", Map.of("[name]", name)));
  }

  public void sethome(CommandSender sender, OfflinePlayer player, String name) {
    if (!(sender instanceof Player)) return;

    var senderPlayer = (Player) sender;

    if (name == null) name = "home";
    name = name.toLowerCase();

    if (!pattern.matcher(name).matches()) {
      sender.sendMessage(t("home:incorrect name"));
      return;
    }

    try {
      String finalName = name;
      TransactionManager.callInTransaction(Database.getConnectionSource(), (Callable<Void>) () -> {
            var count = Database.getPositionEntityDao().queryBuilder().where().eq("uuid", player.getUniqueId().toString()).countOf();
            var exists = Database.getPositionEntityDao().queryBuilder().where().eq("uuid", player.getUniqueId().toString()).and().eq("name", finalName).countOf() > 0;

            if (exists) {
              sender.sendMessage(t("home:position already saved", Map.of("[name]", finalName)));
              return null;
            }

            var user = getLuckPerms().getUserManager().loadUser(player.getUniqueId()).join();
            if (user == null) {
              sender.sendMessage(t("no-player-data"));
              return null;
            }

            var meta = user.getCachedData().getMetaData(QueryOptions.nonContextual());
            var limit = Integer.parseInt(meta.getMetaValue("lava.teleport.max-homes"));

            if (count == limit) {
              sender.sendMessage(t("home:limit exceeded", Map.of("[count]", Integer.toString(limit))));
              return null;
            }

            var location = senderPlayer.getLocation();
            if (player.isOnline()) {
              if (!sender.hasPermission(Permissions.ADMIN)) {
                var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                var query = container.createQuery();
                var regions = query.getApplicableRegions(BukkitAdapter.adapt(location));

                if (!isMemberOfAnyRegion(player, regions) && !isHomeAllowed((Player) player, regions)) {
                  sender.sendMessage(t("home:no access"));
                  return null;
                }
              }
            }

            var entity = new win.lava.teleport.entities.PositionEntity();
            entity.setUuid(player.getUniqueId().toString());
            entity.setName(finalName);
            entity.setWorld(location.getWorld().getName());
            entity.setX(location.getBlockX());
            entity.setY(Math.ceil(location.getY()));
            entity.setZ(location.getBlockZ());
            entity.setPitch(location.getPitch());
            entity.setYaw(location.getYaw());

            Database.getPositionEntityDao().createOrUpdate(entity);

            var df = new DecimalFormat("#");
            df.setRoundingMode(RoundingMode.FLOOR);

            sender.sendMessage(
                t("home:saved", Map.of(
                    "[name]", finalName,
                    "[x]", df.format(location.getX()),
                    "[y]", df.format(location.getY()),
                    "[z]", df.format(location.getZ())
                    )
                )
            );

            return null;
          }
      );
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void teleport(CommandSender sender, OfflinePlayer player, String name) {
    if (!(sender instanceof Player)) return;

    var senderPlayer = (Player) sender;

    if (name == null) name = "home";
    name = name.toLowerCase();

    if (!pattern.matcher(name).matches()) {
      senderPlayer.sendMessage(t("home:incorrect name"));
      return;
    }

    try {
      var entity = Database.getPositionEntityDao().queryBuilder().where().eq("uuid", player.getUniqueId().toString()).and().eq("name", name).queryForFirst();
      if (entity == null) {
        senderPlayer.sendMessage(t("home:position not found", Map.of("[name]", name)));
        return;
      }

      var world = Bukkit.getWorld(entity.getWorld());
      var location = new Location(world, entity.getX(), entity.getY(), entity.getZ(), (float) entity.getYaw(), (float) entity.getPitch());

      var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
      var query = container.createQuery();
      var regions = query.getApplicableRegions(BukkitAdapter.adapt(location));
      if(!isHomeAllowed(senderPlayer, regions)) {
        senderPlayer.sendMessage(t("home:no access"));
        return;
      }

      String finalName = name;
      teleportManager.teleportPlayer(new TeleportTask(senderPlayer, location) {
        @Override
        public void run() {
          super.run();

          teleportSafeAsyncTaskFactory.create(senderPlayer, location).run();

          senderPlayer.sendMessage(t("home:teleported", Map.of("[name]", finalName)));
        }

        @Override
        public void cancel(CancelReason reason) {
          super.cancel(reason);
        }

        @Override
        public PlayerTeleportData.TELEPORT_KIND getTeleportKind() {
          return PlayerTeleportData.TELEPORT_KIND.HOME;
        }
      });
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
