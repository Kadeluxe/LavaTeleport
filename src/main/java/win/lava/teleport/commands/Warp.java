package win.lava.teleport.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.lib.PaperLib;
import win.lava.common._deps_.net.kyori.text.TextComponent;
import win.lava.common._deps_.net.kyori.text.adapter.bukkit.TextAdapter;
import win.lava.common._deps_.net.kyori.text.event.ClickEvent;
import win.lava.common._deps_.net.kyori.text.event.HoverEvent;
import win.lava.common._deps_.net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.lava.teleport.*;
import win.lava.teleport.tasks.TeleportTask;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static win.lava.teleport.Lang.t;

@Singleton
@CommandAlias("warp")
@CommandPermission(Permissions.USER)
public class Warp extends BaseCommand {
  @Inject private LavaTeleport plugin;
  @Inject private TeleportManager teleportManager;

  public Warp() {

  }

  @Default
  @Subcommand("list")
  @CommandAlias("warps")
  public void default_(CommandSender sender) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      ArrayList<String> result = new ArrayList<>();

      try {
        var warps = Database.getWarpEntityDao().queryForAll();
        for (var it : warps) {
          if (sender.hasPermission(Permissions.WARP(it.getName()))) result.add(it.getName());
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }

      Collections.sort(result);

      var b = TextComponent.builder();
      b.append(t("available warps"));

      if (result.size() == 0) {
        b.append(t("empty"));
      } else {
        for (var warp : result) {
          b.append(
              TextComponent.of(warp, TextColor.AQUA)
                  .clickEvent(ClickEvent.suggestCommand("/warp " + warp))
                  .hoverEvent(HoverEvent.showText(TextComponent.of("/warp " + warp, TextColor.AQUA)))
          );
          b.append(" ");
        }
      }

      TextAdapter.sendComponent(sender, b.build());
    });
  }

  @Default
  @CommandCompletion("@warps @nothing")
  public void warp(CommandSender sender, String name) {
    if (!(sender instanceof Player)) return;
    var player = (Player) sender;

    var playerData = teleportManager.getPlayerTeleportData(player);
    if (!playerData.checkAndNotifyAboutCooldown(player, PlayerTeleportData.TELEPORT_KIND.WARP)) {
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        var warp = Database.getWarpEntityDao().queryBuilder().where().eq("name", name).queryForFirst();
        if (warp == null) {
          sender.sendMessage(t("warps.warp not found", Map.of("[name]", name)));
          return;
        }

        if (!player.hasPermission(Permissions.WARP(warp.getName()))) {
          sender.sendMessage(t("warps.no permissions", Map.of("[name]", name)));
          return;
        }

        var location = new Location(Bukkit.getWorld(warp.getWorld()), warp.getX(), warp.getY(), warp.getZ(), (float) warp.getYaw(), (float) warp.getPitch());
        teleportManager.teleportPlayer(new TeleportTask(player, location) {
          @Override
          public void run() {
            super.run();

            PaperLib.teleportAsync(player, location);
            plugin.log(player, location, "warp to " + name);

            player.sendMessage(t("warps.teleported", Map.of("[name]", name)));
          }

          @Override
          public PlayerTeleportData.TELEPORT_KIND getTeleportKind() {
            return PlayerTeleportData.TELEPORT_KIND.WARP;
          }
        });
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
    });
  }
}
