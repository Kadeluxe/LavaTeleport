package win.lava.teleport;

import com.earth2me.essentials.utils.EnumUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocationUtil {
  public static final Set<Material> DANGEROUS_BLOCKS = EnumUtil.getAllMatching(Material.class, "LAVA", "FIRE");
  public static final Set<Material> WATER_TYPES = EnumUtil.getAllMatching(Material.class, "WATER", "FLOWING_WATER");
  public static final Set<Material> HOLLOW_MATERIALS = new HashSet<>();
  public static final Set<Material> TRANSPARENT_MATERIALS = new HashSet<>();

  public static final List<Vector> VECTOR_LIST = new ArrayList<>();

  public static void cacheMaterials() {
    var materials = Material.values();

    for(int i = 0; i < materials.length; ++i) {
      Material mat = materials[i];
      if (mat.isTransparent()) {
        HOLLOW_MATERIALS.add(mat);
      }
    }

    TRANSPARENT_MATERIALS.addAll(HOLLOW_MATERIALS);
    TRANSPARENT_MATERIALS.addAll(WATER_TYPES);

    VECTOR_LIST.add(new Vector(7, 0, 7));
    VECTOR_LIST.add(new Vector(3, 0, 3));
    VECTOR_LIST.add(new Vector(10, 0, 10));
  }

  public static boolean isLocationSafe(Location loc) {
    try {
      return !com.earth2me.essentials.utils.LocationUtil.isBlockUnsafe(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    } catch (Exception e) {
      return false;
    }
  }
}
