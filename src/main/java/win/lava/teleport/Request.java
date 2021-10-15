package win.lava.teleport;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.papermc.lib.PaperLib;
import win.lava.common._deps_.net.kyori.text.TextComponent;
import win.lava.common._deps_.net.kyori.text.adapter.bukkit.TextAdapter;
import win.lava.common._deps_.net.kyori.text.event.ClickEvent;
import win.lava.common._deps_.net.kyori.text.event.HoverEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import win.lava.teleport.tasks.TeleportTask;

import java.util.HashMap;
import java.util.Map;

import static win.lava.teleport.Lang.t;
import static win.lava.teleport.LavaTeleport.getLavaPassive;

public class Request {
  public Player src;
  public Player dest;
  public String srcName;
  public String destName;

  private LavaTeleport plugin;
  private TeleportManager teleportManager;
  private CallManager callManager;
  private Settings settings;

  private BukkitRunnable expiration;
  private BukkitRunnable timer;

  @AssistedInject
  public Request(TeleportManager teleportManager, LavaTeleport plugin, CallManager callManager, Settings settings, @Assisted("src") Player src, @Assisted("dest") Player dest) {
    this.teleportManager = teleportManager;
    this.plugin = plugin;
    this.callManager = callManager;
    this.settings = settings;

    this.src = src;
    this.dest = dest;

    srcName = src.getDisplayName();
    destName = dest.getDisplayName();

    expiration = new BukkitRunnable() {
      @Override
      public void run() {
        Request.this.cancel(CancelReason.EXPIRED);
      }
    };
    expiration.runTaskLater(plugin, settings.getTpaLifetime() * LavaTeleport.TPS);
  }

  public void accept() {
    expiration.cancel();

    teleportManager.teleportPlayer(new TeleportTask(src, null) {
      @Override
      public void run() {
        var location = dest.getLocation();
        this.location = location;

        super.run();

        var offset = location.clone().add(0, -1, 0);
        src.sendBlockChange(offset, offset.getBlock().getBlockData());

        PaperLib.teleportAsync(src, location, PlayerTeleportEvent.TeleportCause.PLUGIN);
        plugin.log(player, location, "tpa to " + destName);

        destroy();
      }

      @Override
      public void cancel(CancelReason reason) {
        super.cancel(reason);

        Request.this.cancel(reason);
      }

      @Override
      protected void sendPlayerCancelReason(CancelReason reason) {

      }

      @Override
      public PlayerTeleportData.TELEPORT_KIND getTeleportKind() {
        return PlayerTeleportData.TELEPORT_KIND.TPA;
      }
    });

    var placeholders = getPlaceholders();
    placeholders.put("[delay]", Integer.toString(settings.getTeleportDelay(PlayerTeleportData.TELEPORT_KIND.TPA)));

    dest.sendMessage(t("accept-dest", getPlaceholders()));
  }

  public void deny() {
    cancel(CancelReason.DEST_DENY);
  }

  public void cancel(CancelReason reason) {
    var placeholders = getPlaceholders();

    String lang = "";
    boolean cancelledBySrc = false;
    boolean sendToSrc = true;
    boolean sendToDest = true;

    if (reason == CancelReason.EXPIRED) {
      lang = "expired";
    } else if (reason == CancelReason.MOVEMENT) {
      lang = "movement";
      cancelledBySrc = true;
    } else if (reason == CancelReason.DAMAGE) {
      lang = "damage";
      cancelledBySrc = true;
    } else if (reason == CancelReason.SRC_LEAVE) {
      lang = "src-leave";
      sendToSrc = false;
    } else if (reason == CancelReason.DEST_DENY) {
      lang = "deny";
    } else if (reason == CancelReason.DEST_LEAVE) {
      lang = "dest-leave";
      sendToDest = false;
    } else if (reason == CancelReason.ANOTHER_REQUEST) {
      lang = "another";
      cancelledBySrc = true;
    } else {
      lang = "src";
      cancelledBySrc = true;
    }

    if (sendToSrc)
      src.sendMessage(t(String.format("cancel-%s-src", lang), placeholders));

    if (sendToDest) {
      if (cancelledBySrc) {
        dest.sendMessage(t("cancel-src-dest", placeholders));
      } else {
        dest.sendMessage(t(String.format("cancel-%s-dest", lang), placeholders));
      }
    }

    destroy();
  }

  public void destroy() {
    if (expiration != null) {
      expiration.cancel();
      expiration = null;
    }

    if (timer != null) {
      timer.cancel();
      timer = null;
    }

    callManager.unregister(this);
  }

  public void sendRequestToSource() {
    var b = TextComponent.builder(t("request-src", getPlaceholders()));

    if (getLavaPassive().isPvpEnabled(dest)) {
      b.append("\n").append(t("warn-pvp"));
    }

    b
        .append("\n")
        .append(
            TextComponent.of(t("button-cancel"))
                .hoverEvent(HoverEvent.showText(TextComponent.of("/tpcancel")))
                .clickEvent(ClickEvent.runCommand(String.format("/tpcancel")))
        );

    TextAdapter.sendComponent(src, b.build());
  }

  public void sendRequestToDest() {
    var placeholders = getPlaceholders();

    var b = TextComponent.builder();

    b.append(TextComponent.of(t("line"))); // -----
    b.append("\n").append(TextComponent.of(t("request-dest", placeholders))); // message

    if (getLavaPassive().isPvpEnabled(src)) {
      b.append("\n").append(t("warn-pvp"));
    }

    b.append("\n").append(
        TextComponent.builder()
            .append(
                TextComponent.of(t("button-accept"))
                    .hoverEvent(HoverEvent.showText(TextComponent.of("/tpaccept")))
                    .clickEvent(ClickEvent.runCommand(String.format("/tpaccept %s", src.getDisplayName())))
            )
            .append(" ")
            .append(
                TextComponent.of(t("button-deny"))
                    .hoverEvent(HoverEvent.showText(TextComponent.of("/tpdeny")))
                    .clickEvent(ClickEvent.runCommand(String.format("/tpdeny %s", src.getDisplayName())))
            )
            .build()
    );
    b.append("\n").append(TextComponent.of(t("line"))); // -----

    TextAdapter.sendComponent(dest, b.build());
  }

  public interface Factory {
    Request create(@Assisted("src") Player src, @Assisted("dest") Player dest);
  }

  public boolean isInProgress() {
    return timer != null;
  }

  Map<String, String> getPlaceholders() {
    var placeholders = new HashMap<String, String>();
    placeholders.put("[src]", srcName);
    placeholders.put("[dest]", destName);
    placeholders.put("[delay]", Integer.toString(settings.getTeleportDelay(PlayerTeleportData.TELEPORT_KIND.TPA)));

    return placeholders;
  }
}
