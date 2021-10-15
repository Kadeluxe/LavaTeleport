package win.lava.teleport.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.lava.teleport.LavaTeleport;
import win.lava.teleport.PlayerTeleportData;
import win.lava.teleport.TeleportManager;
import win.lava.teleport.modules.HomeModule;

@Singleton
public class Home extends BaseCommand {
  @Inject private LavaTeleport plugin;
  @Inject private TeleportManager teleportManager;
  @Inject private HomeModule homeModule;

  public Home() {

  }

  @CommandAlias("sethome")
  @CommandPermission("lava.teleport.home.set")
  @CommandCompletion("<название>")
  @Syntax("<название>")
  public void sethome(CommandSender sender, @Optional String name) {
    if (!(sender instanceof Player)) return;

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      homeModule.sethome(sender, (Player) sender, name);
    });
  }

  @CommandAlias("home")
  @CommandPermission("lava.teleport.home.teleport")
  @CommandCompletion("@playerHomes")
  @Syntax("<название>")
  public void home(CommandSender sender, @Optional String name) {
    if (!(sender instanceof Player)) return;
    var player = (Player) sender;

    var playerData = teleportManager.getPlayerTeleportData(player);
    if (!playerData.checkAndNotifyAboutCooldown(player, PlayerTeleportData.TELEPORT_KIND.HOME)) {
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      homeModule.teleport(player, player, name);
    });
  }

  @CommandAlias("delhome")
  @CommandPermission("lava.teleport.home.del")
  @CommandCompletion("@playerHomes")
  @Syntax("<название>")
  public void delhome(CommandSender sender, String name) {
    if (!(sender instanceof Player)) return;

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      homeModule.delhome(sender, (Player) sender, name);
    });
  }

  @CommandAlias("homes")
  @CommandPermission("lava.teleport.home.list")
  public void homes(CommandSender sender) {
    if (!(sender instanceof Player)) return;

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      homeModule.list(sender, (Player) sender);
    });
  }
}
