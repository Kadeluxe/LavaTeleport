package win.lava.teleport.tasks;

import com.google.inject.Inject;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import win.lava.teleport.CancelReason;
import win.lava.teleport.Permissions;
import win.lava.teleport.PlayerTeleportData;
import win.lava.teleport.TeleportManager;

import static win.lava.teleport.Lang.t;

public abstract class TeleportTask implements Runnable {
  protected @Inject TeleportManager teleportManager;

  protected Player player;
  protected BukkitTask bukkitTask;
  protected BukkitTask progressTask = null;
  protected Location initialLocation;
  protected Location location;
  protected boolean shouldIgnoreDelay = false;
  protected boolean isCancelable = true;
  protected boolean isOverwriteAllowed = true;
  protected boolean isHighPriority = false;

  protected TeleportTask(Player player, Location location) {
    this.player = player;
    this.location = location;

    shouldIgnoreDelay = player.hasPermission(Permissions.ADMIN) && getTeleportKind() != PlayerTeleportData.TELEPORT_KIND.TPA;
  }

  public static String translateCancelReason(CancelReason reason) {
    String lang = null;

    if (reason == CancelReason.EXPIRED) {
      lang = "expired";
    } else if (reason == CancelReason.MOVEMENT) {
      lang = "movement";
    } else if (reason == CancelReason.DAMAGE) {
      lang = "damage";
    } else if (reason == CancelReason.ANOTHER_REQUEST) {
      lang = "another";
    } else if (reason == CancelReason.CANNOT_OVERRIDE) {
      lang = "cannot-override";
    }

    if (lang == null) return null;

    return String.format("cancel-%s-src", lang);
  }

  public boolean shouldIgnoreDelay() {
    return shouldIgnoreDelay;
  }

  @Override
  public void run() {
    var playerData = teleportManager.getPlayerTeleportData(player);
    if (!isHighPriority) {
      playerData.setLastTeleportTime(getTeleportKind());
    }

    if (location != null) {
      playerData.setLastLocation(location);

      playerData.setFallProtectionActive(true);
      playerData.setSuffocationProtectionActive(true);
    }

    teleportManager.setPlayerTeleportTask(player, null);

    if (progressTask != null) {
      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
      progressTask.cancel();
    }
  }

  public void cancelProgressTask() {
    if (progressTask != null) {
      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
      progressTask.cancel();
    }
  }

  public void cancel(CancelReason reason) {
    if (bukkitTask != null) bukkitTask.cancel();
    cancelProgressTask();

    teleportManager.setPlayerTeleportTask(player, null);

    sendPlayerCancelReason(reason);
  }

  protected void sendPlayerCancelReason(CancelReason reason) {
    var langKey = translateCancelReason(reason);
    if (langKey != null) player.sendMessage(t(langKey));
  }

  public interface Factory {
    TeleportTask create(Player player, Location location);
  }

  public BukkitTask getProgressTask() {
    return progressTask;
  }

  public void setProgressTask(BukkitTask progressTask) {
    this.progressTask = progressTask;
  }

  public BukkitTask getBukkitTask() {
    return bukkitTask;
  }

  public void setBukkitTask(BukkitTask bukkitTask) {
    this.bukkitTask = bukkitTask;
  }

  public Location getLocation() {
    return location;
  }

  public boolean isHighPriority() {
    return isHighPriority;
  }

  public TeleportTask setHighPriority(boolean highPriority) {
    isHighPriority = highPriority;
    return this;
  }

  public Location getInitialLocation() {
    return initialLocation;
  }

  public void setInitialLocation(Location initialLocation) {
    this.initialLocation = initialLocation;
  }

  public abstract PlayerTeleportData.TELEPORT_KIND getTeleportKind();

  public boolean isOverwriteAllowed() {
    return isOverwriteAllowed;
  }

  public void setOverwriteAllowed(boolean overwriteAllowed) {
    isOverwriteAllowed = overwriteAllowed;
  }

  public boolean isCancelable() {
    return isCancelable;
  }

  public TeleportTask setCancelable(boolean cancelable) {
    isCancelable = cancelable;
    return this;
  }

  public Player getPlayer() {
    return player;
  }

  public void setPlayer(Player player) {
    this.player = player;
  }

  public TeleportTask setShouldIgnoreDelay(boolean shouldIgnoreDelay) {
    this.shouldIgnoreDelay = shouldIgnoreDelay;
    return this;
  }
}
