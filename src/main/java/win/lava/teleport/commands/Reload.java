package win.lava.teleport.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.command.CommandSender;
import win.lava.teleport.LavaTeleport;
import win.lava.teleport.Permissions;

@Singleton
@CommandAlias("teleport")
public class Reload extends BaseCommand {
  @Inject private LavaTeleport plugin;

  public Reload() {

  }

  @Subcommand("reload")
  @CommandPermission(Permissions.ADMIN)
  public void handler(CommandSender sender) {
    plugin.reload();

    sender.sendMessage("Lava Teleport reloaded");
  }
}
