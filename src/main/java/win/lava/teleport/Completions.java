package win.lava.teleport;

import co.aikar.commands.BukkitCommandCompletionContext;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import win.lava.teleport.entities.PositionEntity;

import java.sql.SQLException;
import java.util.ArrayList;

public class Completions {
  public static ArrayList<String> playersExcludingSender(BukkitCommandCompletionContext ctx) {
    CommandSender sender = ctx.getSender();
    Validate.notNull(sender, "Sender cannot be null");

    Player senderPlayer = sender instanceof Player ? (Player) sender : null;

    ArrayList<String> matchedPlayers = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      String name = player.getName();
      if ((senderPlayer == null || senderPlayer.canSee(player)) && StringUtil.startsWithIgnoreCase(name, ctx.getInput()) && player != senderPlayer) {
        matchedPlayers.add(name);
      }
    }

    matchedPlayers.sort(String.CASE_INSENSITIVE_ORDER);
    return matchedPlayers;
  }

  public static ArrayList<String> playerHomes(BukkitCommandCompletionContext ctx) {
    ArrayList<String> result = new ArrayList<>();

    try {
      var positions = Database.getPositionEntityDao().queryForEq("uuid", ctx.getPlayer().getUniqueId().toString());
      for (var it : positions) {
        result.add(it.getName());
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return result;
  }

  public static ArrayList<String> warps(BukkitCommandCompletionContext ctx) {
    ArrayList<String> result = new ArrayList<>();

    try {
      var warps = Database.getWarpEntityDao().queryForAll();
      for (var it : warps) {
        if (ctx.getPlayer().hasPermission(Permissions.WARP(it.getName()))) result.add(it.getName());
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return result;
  }
}