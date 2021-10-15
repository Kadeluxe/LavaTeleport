package win.lava.teleport;

public class Permissions {
  public final static String USER = "lava.teleport.user";
  public final static String ADMIN = "lava.teleport.admin";
  public final static String RTP_USE = "lava.teleport.rtp.use";

  public static String RTP_WORLD(String worldName) {
    return "lava.teleport.rtp.worlds." + worldName;
  }

  public static String WARP(String warpName) {
    return "lava.teleport.warp." + warpName;
  }
}
