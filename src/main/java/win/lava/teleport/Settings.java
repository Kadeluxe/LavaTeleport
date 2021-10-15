package win.lava.teleport;

import com.google.inject.Singleton;
import org.bukkit.configuration.Configuration;

import java.util.HashMap;
import java.util.Objects;

@Singleton
public class Settings {
  private final HashMap<PlayerTeleportData.TELEPORT_KIND, Integer> teleportCooldowns = new HashMap<>();
  private final HashMap<PlayerTeleportData.TELEPORT_KIND, Integer> teleportDelays = new HashMap<>();
  private double protectionFallRadius;
  private int protectionFallDuration;
  private double protectionSuffocationRadius;
  private int protectionPlayerDuration;
  private boolean authSpawnOnJoin;
  private int rtpRadius;
  private int tpaLifetime;
  private boolean restoreLocationOnAuth;

  public void reload(Configuration config) {
    protectionFallRadius = Math.pow(config.getDouble("protection.fall.radius"), 2);
    protectionFallDuration = config.getInt("protection.fall.duration");
    protectionSuffocationRadius = Math.pow(config.getDouble("protection.suffocation.radius"), 2);
    protectionPlayerDuration = config.getInt("protection.player.duration");


    tpaLifetime = config.getInt("tpa.lifetime");

    authSpawnOnJoin = config.getBoolean("auth.spawn on join");
    restoreLocationOnAuth = config.getBoolean("auth.restore location on auth");

    rtpRadius = config.getInt("rtp.radius");

    for (var kind : PlayerTeleportData.TELEPORT_KIND.values()) {
      teleportCooldowns.put(kind, config.getInt(kind.toString().toLowerCase() + ".cooldown"));
      teleportDelays.put(kind, config.getInt(kind.toString().toLowerCase() + ".delay"));
    }
  }

  public int getTeleportDelay(PlayerTeleportData.TELEPORT_KIND kind) {
    return teleportDelays.get(Objects.requireNonNullElse(kind, PlayerTeleportData.TELEPORT_KIND.GLOBAL));
  }

  public int getTeleportCooldown(PlayerTeleportData.TELEPORT_KIND kind) {
    return teleportCooldowns.get(Objects.requireNonNullElse(kind, PlayerTeleportData.TELEPORT_KIND.GLOBAL));
  }

  public boolean isRestoreLocationOnAuth() {
    return restoreLocationOnAuth;
  }

  public int getTpaLifetime() {
    return tpaLifetime;
  }

  public boolean isAuthSpawnOnJoin() {
    return authSpawnOnJoin;
  }

  public int getRtpRadius() {
    return rtpRadius;
  }

  public int getProtectionPlayerDuration() {
    return protectionPlayerDuration;
  }

  public double getProtectionFallRadius() {
    return protectionFallRadius;
  }

  public int getProtectionFallDuration() {
    return protectionFallDuration;
  }

  public double getProtectionSuffocationRadius() {
    return protectionSuffocationRadius;
  }
}
