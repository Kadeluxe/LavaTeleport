package win.lava.teleport.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import win.lava.teleport.LavaTeleport;
import win.lava.teleport.Permissions;
import win.lava.teleport.modules.HomeModule;

import static win.lava.teleport.Lang.t;
import static win.lava.teleport.modules.HomeModule.findPlayer;

@Singleton
@CommandAlias("ahome")
@CommandPermission(Permissions.ADMIN)
public class HomeAdmin extends BaseCommand {
  @Inject private LavaTeleport plugin;
  @Inject private HomeModule homeModule;

  public HomeAdmin() {

  }

  @Default
  @CommandCompletion("@players")
  @Syntax("<имя_игрока>")
  public void list(CommandSender sender, String playerName) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      var player = findPlayer(playerName);

      homeModule.list(sender, player);
    });
  }

  @Default
  @CommandCompletion("@players *")
  @Syntax("<имя_игрока> <название_позиции>")
  public void teleport(CommandSender sender, String playerName, String name) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      var player = findPlayer(playerName);

      homeModule.teleport(sender, player, name);
    });
  }

  @Subcommand("delete")
  @CommandCompletion("@players *")
  @Syntax("<имя_игрока> <название_позиции>")
  public void delete(CommandSender sender, String playerName, String name) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      var player = findPlayer(playerName);

      homeModule.delhome(sender, player, name);
    });
  }
}
