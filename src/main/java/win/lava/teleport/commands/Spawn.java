package win.lava.teleport.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.lib.PaperLib;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.lava.teleport.LavaTeleport;
import win.lava.teleport.Permissions;
import win.lava.teleport.PlayerTeleportData;
import win.lava.teleport.TeleportManager;
import win.lava.teleport.tasks.TeleportTask;

import java.util.Map;

import static win.lava.teleport.Lang.t;

@Singleton
@CommandAlias("spawn")
public class Spawn extends BaseCommand {
  @Inject private LavaTeleport plugin;
  @Inject private TeleportManager teleportManager;

  public Spawn() {

  }

  @Default
  @CommandPermission("lava.teleport.basic.spawn")
  @CommandCompletion("@nothing")
  public void handler(CommandSender sender) {
    if (!(sender instanceof Player)) return;
    var player = (Player) sender;

    var playerData = teleportManager.getPlayerTeleportData(player);
    if (!playerData.checkAndNotifyAboutCooldown(player, PlayerTeleportData.TELEPORT_KIND.GLOBAL)) {
      return;
    }

    teleportManager.teleportPlayer(new TeleportTask(player, plugin.getEssentialsSpawn().getSpawn("default")) {
      @Override
      public void run() {
        super.run();

        PaperLib.teleportAsync(player, location);
        plugin.log(player, location, "spawn");
      }

      @Override
      public PlayerTeleportData.TELEPORT_KIND getTeleportKind() {
        return PlayerTeleportData.TELEPORT_KIND.GLOBAL;
      }
    });
  }

  @Default
  @CommandPermission(Permissions.ADMIN)
  @CommandCompletion("*")
  public void handler(CommandSender sender, OnlinePlayer player) {
    var target = player.getPlayer();

    teleportManager.teleportPlayer(
        new TeleportTask(target, plugin.getEssentialsSpawn().getSpawn("default")) {
          @Override
          public void run() {
            super.run();
            PaperLib.teleportAsync(target, location);

            sender.sendMessage(t("player teleported to spawn", Map.of("[player]", target.getDisplayName())));
          }

          @Override
          public PlayerTeleportData.TELEPORT_KIND getTeleportKind() {
            return null;
          }
        }
            .setHighPriority(true)
            .setShouldIgnoreDelay(true)
    );
  }
}
