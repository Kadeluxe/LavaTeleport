package win.lava.teleport;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.WeakHashMap;

@Singleton
public class CallManager implements Listener {
  @Inject private TeleportManager teleportManager;

  public HashMap<Player, Request> requests = new HashMap<>();
  public WeakHashMap<Player, Request> lastRequests = new WeakHashMap<>();

  public CallManager() {

  }

  public void register(Request request) {
    this.requests.put(request.src, request);
    this.lastRequests.put(request.dest, request);
  }

  public void unregister(Request request) {
    requests.remove(request.src);
    lastRequests.remove(request.dest);
  }

  public void dispatch(Request request) {
    // Cancel previous requests
    var prev = requests.get(request.src);
    if (prev != null) {
      prev.cancel(CancelReason.ANOTHER_REQUEST);
    }

    // Register request
    this.register(request);

    // Send messages
    request.sendRequestToDest();
    request.sendRequestToSource();

    teleportManager.getPlayerTeleportData(request.src).setLastTeleportTime(PlayerTeleportData.TELEPORT_KIND.TPA);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent ev) {
    var player = ev.getPlayer();

    // outgoing
    var outgoing = requests.get(player);
    if (outgoing != null) {
      outgoing.cancel(CancelReason.SRC_LEAVE);
    }

    // incoming
    var incoming = lastRequests.get(player);
    if (incoming != null) {
      incoming.cancel(CancelReason.DEST_LEAVE);
    }
  }

  public Request getRequestBySource(Player source) {
    return requests.get(source);
  }

  public Request getLastRequestTo(Player dest) {
    return lastRequests.get(dest);
  }
}
