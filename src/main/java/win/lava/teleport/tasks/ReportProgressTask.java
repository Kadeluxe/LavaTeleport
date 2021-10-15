package win.lava.teleport.tasks;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ReportProgressTask implements Runnable {
  public static final int INTERVAL = 3;
  public static final int LENGTH = 25;


  Player player;
  int i = 0;
  int total;

  public ReportProgressTask(Player player, int total) {
    this.player = player;
    this.total = total;
  }

  @Override
  public void run() {
    var progress = (i / (float) total);

    var c = (int) (LENGTH * progress);
    c = Math.min(LENGTH, Math.max(c, 0));

    var str = "&b" + "▉".repeat(c) + "&7" + "▉".repeat(LENGTH - c);
    i += INTERVAL;

    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', str)));
  }
}
