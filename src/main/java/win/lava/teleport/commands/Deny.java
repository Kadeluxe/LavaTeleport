package win.lava.teleport.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.lava.teleport.CallManager;
import win.lava.teleport.Request;

import static win.lava.teleport.Lang.t;

@Singleton
@CommandAlias("tpdeny|tpno")
public class Deny extends BaseCommand {
  @Inject private CallManager callManager;

  public Deny() {

  }

  @Default
  @CommandPermission("lava.teleport.basic.deny")
  @CommandCompletion("@playersExcludingSender")
  @Syntax("<игрок>")
  public void handler(CommandSender sender, @Optional OnlinePlayer src) {
    if (!(sender instanceof Player)) return;
    var player = (Player) sender;

    Request request;

    if (src != null) {
      request = callManager.getRequestBySource(src.getPlayer());
    } else {
      request = callManager.getLastRequestTo(player);
    }

    if (request == null || request.dest != player || request.isInProgress()) {
      sender.sendMessage(t("error-no-request"));
    } else {
      request.deny();
    }
  }
}
