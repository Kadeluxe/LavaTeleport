package win.lava.teleport;

import org.bukkit.ChatColor;

import java.util.Map;
import java.util.Objects;

import static win.lava.teleport.LavaTeleport.getLanguageConfig;

public class Lang {
  public static String t(String key) {
    return ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getLanguageConfig().getString(key, key)));
  }

  public static String t(String key, Map<String, String> vars) {
    var str = t(key);
    for (var it : vars.entrySet()) {
      str = str.replace(it.getKey(), it.getValue());
    }
    return str;
  }
}
