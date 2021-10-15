package win.lava.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static win.lava.teleport.Lang.t;

public class PlayerTeleportData {
  private Settings settings;

  private HashMap<TELEPORT_KIND, Long> lastTime = new HashMap<>();

  private long deletedAt = 0;
  private Location lastLocation = new Location(Bukkit.getWorld("world"), 0, 0, 0);

  private boolean isSuffocationProtectionActive = false;
  private boolean isFallProtectionActive = false;

  private long fallStart = 0;

  public PlayerTeleportData(Settings settings) {
    this.settings = settings;

    Arrays.stream(TELEPORT_KIND.values()).forEach(x -> lastTime.put(x, 0L));
  }

  public void updateFallData(Player player) {
    var fallDistance = player.getFallDistance();
    if (fallDistance >= 2) {
      if (fallStart == 0) {
        fallStart = Instant.now().getEpochSecond();
      }
    } else {
      fallStart = 0;
    }
  }

  public void updateProtectionStatus(Player player) {
    updateFallData(player);

    var location = player.getLocation();
    if (location.getWorld() != lastLocation.getWorld()) {
      setSuffocationProtectionActive(false);
      setFallProtectionActive(false);
      return;
    }

    var radius = Math.pow(location.getBlockX() - lastLocation.getBlockX(), 2) + Math.pow(location.getBlockZ() - lastLocation.getBlockZ(), 2);

    if (radius > settings.getProtectionSuffocationRadius()) {
      setSuffocationProtectionActive(false);
    }

    if (radius > settings.getProtectionFallRadius()) {
      setFallProtectionActive(false);
    }
  }

  public void markAsDeleted(boolean state) {
    if (state) {
      deletedAt = Instant.now().getEpochSecond();
    } else {
      deletedAt = 0;
    }
  }

  public long getTeleportCooldown(TELEPORT_KIND kind) {
    var now = Instant.now().getEpochSecond();

    var globalDif = now - getLastTime(TELEPORT_KIND.GLOBAL);
    var kindDif = now - getLastTime(kind);

    var globalCooldown = settings.getTeleportCooldown(TELEPORT_KIND.GLOBAL) - globalDif;
    var kindCooldown = settings.getTeleportCooldown(kind) - kindDif;

    return Math.max(globalCooldown, kindCooldown);
  }

  public long getLastTime(TELEPORT_KIND kind) {
    return lastTime.get(kind);
  }

  public boolean checkAndNotifyAboutCooldown(Player player, TELEPORT_KIND kind) {
    if (kind != TELEPORT_KIND.TPA && player.hasPermission(Permissions.ADMIN)) {
      return true;
    }

    var cooldown = getTeleportCooldown(kind);
    if (cooldown < 0) return true;

    player.sendMessage(t("cooldown", Map.of("[cooldown]", Long.toString(cooldown))));
    return false;
  }

  public enum TELEPORT_KIND {
    GLOBAL,
    RTP,
    TPA,
    HOME,
    WARP
  }

  public boolean isSuffocationProtectionActive() {
    return isSuffocationProtectionActive;
  }

  public void setSuffocationProtectionActive(boolean suffocationProtectionActive) {
    isSuffocationProtectionActive = suffocationProtectionActive;
  }

  public boolean isFallProtectionActive() {
    return isFallProtectionActive;
  }

  public void setFallProtectionActive(boolean fallProtectionActive) {
    isFallProtectionActive = fallProtectionActive;
  }

  public boolean isProtectedFromPlayerDamage() {
    return getTimeSinceLastTeleport() < settings.getProtectionPlayerDuration();
  }

  public boolean isProtectedFromFallingDamage() {
    return isFallProtectionActive && fallStart - getLastTime(TELEPORT_KIND.GLOBAL) <= settings.getProtectionFallDuration();
  }

  public boolean isProtectedFromSuffocationDamage() {
    return isSuffocationProtectionActive;
  }

  public long getTimeSinceLastTeleport() {
    return Instant.now().getEpochSecond() - getLastTime(TELEPORT_KIND.GLOBAL);
  }

  public long getDeletedAt() {
    return deletedAt;
  }

  public Location getLastLocation() {
    return lastLocation;
  }

  public void setLastLocation(Location lastLocation) {
    this.lastLocation = lastLocation;
  }

  public void setLastTeleportTime(TELEPORT_KIND kind) {
    if (kind == null) return;
    var now = Instant.now().getEpochSecond();

    lastTime.put(TELEPORT_KIND.GLOBAL, now);
    lastTime.put(kind, now);
  }
}
