package win.lava.teleport.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.lava.teleport.*;

import static win.lava.teleport.Lang.t;

@Singleton
@CommandAlias("tpcancel|tpacancel")
public class Cancel extends BaseCommand {
  @Inject private CallManager callManager;

  public Cancel() {

  }

  @Default
  @CommandPermission("lava.teleport.basic.cancel")
  public void handler(CommandSender sender) {
    if (!(sender instanceof Player)) return;
    var player = (Player) sender;

    Request request;
    request = callManager.getRequestBySource(player);

    if (request == null || request.isInProgress()) {
      sender.sendMessage(t("error-no-request"));
    } else {
      request.cancel(CancelReason.SRC_CANCEL);
    }
  }
}
