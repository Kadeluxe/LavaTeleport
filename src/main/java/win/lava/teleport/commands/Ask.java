package win.lava.teleport.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.lava.teleport.CallManager;
import win.lava.teleport.PlayerTeleportData;
import win.lava.teleport.Request;
import win.lava.teleport.TeleportManager;

import static win.lava.teleport.Lang.t;
import static win.lava.teleport.LavaTeleport.getLavaChat;

@Singleton
@CommandAlias("tpa|tpask|call")
public class Ask extends BaseCommand {
  @Inject private TeleportManager teleportManager;
  @Inject private CallManager callManager;
  @Inject private Request.Factory requestFactory;

  public Ask() {

  }

  @Default
  @CommandPermission("lava.teleport.basic.ask")
  @CommandCompletion("@playersExcludingSender")
  @Syntax("<игрок>")
  public void handler(CommandSender sender, OnlinePlayer destOnlinePlayer) {
    if (!(sender instanceof Player)) return;

    var source = (Player) sender;
    var dest = destOnlinePlayer.getPlayer();

    if (source == dest) {
      sender.sendMessage(t("error-request-yourself"));
      return;
    }

    if (getLavaChat().isPlayerIgnoringPlayer(dest, source)) {
      sender.sendMessage(t("error-dest-ignores-you"));
      return;
    }

    {
      Request request;
      request = callManager.getRequestBySource(source);

      if (request != null && request.dest == dest) {
        sender.sendMessage(t("error-request-exists"));
        return;
      }
    }

    var playerData = teleportManager.getPlayerTeleportData(source);
    if (!playerData.checkAndNotifyAboutCooldown(source, PlayerTeleportData.TELEPORT_KIND.TPA)) {
      return;
    }

    var request = requestFactory.create(source, dest);
    callManager.dispatch(request);
  }
}
