package win.lava.teleport.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.lava.teleport.*;
import win.lava.teleport.tasks.RandomTeleportTask;

import static win.lava.teleport.Lang.t;

@Singleton
@CommandAlias("rtp")
public class Rtp extends BaseCommand {
  @Inject private LavaTeleport plugin;
  @Inject private TeleportManager teleportManager;
  @Inject private RandomTeleportTask.Factory randomTeleportTaskFactory;

  public Rtp() {

  }

  @Default
  @CommandCompletion("@nothing")
  @CommandPermission(Permissions.RTP_USE)
  public void handler(CommandSender sender) {
    if (!(sender instanceof Player)) return;
    var player = (Player) sender;

    var playerData = teleportManager.getPlayerTeleportData(player);
    if (!playerData.checkAndNotifyAboutCooldown(player, PlayerTeleportData.TELEPORT_KIND.RTP)) {
      return;
    }

    if (!player.hasPermission(Permissions.RTP_WORLD(player.getLocation().getWorld().getName().toLowerCase()))) {
      player.sendMessage(t("you can not use rtp here"));
      return;
    }

    teleportManager.teleportPlayer(randomTeleportTaskFactory.create(player));
  }
}
