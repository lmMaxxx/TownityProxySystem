package de.townity.proxysystem.rangshop;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.townity.proxysystem.TownityProxySystem;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.luckperms.api.model.user.User;

public final class RangShopService {

  public enum PurchaseResult {
    SUCCESS,
    LUCKPERMS_MISSING,
    HIGHER_RANK,
    ALREADY_OWNED,
    NEED_PREVIOUS,
    NOT_ENOUGH_POINTS,
    FAILED
  }

  private final TownityProxySystem plugin;
  private final PlaytimePointsStore store;
  private final LuckPermsBridge luckPerms;
  private final Map<UUID, Long> sessionStartMs = new ConcurrentHashMap<>();
  private ScheduledTask flushTask;

  public RangShopService(TownityProxySystem plugin, PlaytimePointsStore store, LuckPermsBridge luckPerms) {
    this.plugin = plugin;
    this.store = store;
    this.luckPerms = luckPerms;
  }

  public PlaytimePointsStore store() {
    return store;
  }

  public LuckPermsBridge luckPerms() {
    return luckPerms;
  }

  public int maxPoints() {
    return ShopRank.totalCost();
  }

  public int points(Player player) {
    flushSession(player.getUniqueId(), false);
    return store.points(player.getUniqueId());
  }

  public void start() {
    flushTask = plugin.server().getScheduler()
        .buildTask(plugin, this::flushAllOnline)
        .repeat(1L, TimeUnit.MINUTES)
        .schedule();
  }

  public void shutdown() {
    if (flushTask != null) {
      flushTask.cancel();
      flushTask = null;
    }
    for (UUID uuid : sessionStartMs.keySet()) {
      flushSession(uuid, true);
    }
    sessionStartMs.clear();
  }

  public void onJoin(Player player) {
    sessionStartMs.put(player.getUniqueId(), System.currentTimeMillis());
  }

  public void onQuit(Player player) {
    flushSession(player.getUniqueId(), true);
    sessionStartMs.remove(player.getUniqueId());
  }

  public CompletableFuture<Optional<User>> loadUser(Player player) {
    return luckPerms.loadUser(player.getUniqueId());
  }

  public CompletableFuture<Boolean> hasRankAboveShop(Player player) {
    if (!luckPerms.isAvailable()) {
      return CompletableFuture.completedFuture(false);
    }
    return loadUser(player).thenApply(opt -> opt.map(luckPerms::hasRankAboveShop).orElse(false));
  }

  public CompletableFuture<Optional<ShopRank>> highestShopRank(Player player) {
    if (!luckPerms.isAvailable()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    return loadUser(player).thenApply(opt -> opt.flatMap(luckPerms::highestShopRank));
  }

  public CompletableFuture<PurchaseResult> purchase(Player player, ShopRank rank) {
    if (!luckPerms.isAvailable()) {
      return CompletableFuture.completedFuture(PurchaseResult.LUCKPERMS_MISSING);
    }

    flushSession(player.getUniqueId(), false);

    return loadUser(player).thenCompose(optional -> {
      if (optional.isEmpty()) {
        return CompletableFuture.completedFuture(PurchaseResult.FAILED);
      }
      User user = optional.get();
      if (luckPerms.hasRankAboveShop(user)) {
        return CompletableFuture.completedFuture(PurchaseResult.HIGHER_RANK);
      }

      Optional<ShopRank> owned = luckPerms.highestShopRank(user);
      if (owned.isPresent() && owned.get().order() >= rank.order()) {
        return CompletableFuture.completedFuture(PurchaseResult.ALREADY_OWNED);
      }

      Optional<ShopRank> previous = rank.previous();
      if (previous.isPresent()) {
        int ownedOrder = owned.map(ShopRank::order).orElse(0);
        if (ownedOrder < previous.get().order()) {
          return CompletableFuture.completedFuture(PurchaseResult.NEED_PREVIOUS);
        }
      }

      int balance = store.points(player.getUniqueId());
      if (balance < rank.cost()) {
        return CompletableFuture.completedFuture(PurchaseResult.NOT_ENOUGH_POINTS);
      }

      if (!store.spend(player.getUniqueId(), rank.cost())) {
        return CompletableFuture.completedFuture(PurchaseResult.NOT_ENOUGH_POINTS);
      }

      return luckPerms.assignShopRank(player.getUniqueId(), rank).thenApply(ok -> {
        if (!ok) {
          store.refund(player.getUniqueId(), rank.cost());
          return PurchaseResult.FAILED;
        }
        return PurchaseResult.SUCCESS;
      });
    });
  }

  private void flushAllOnline() {
    for (Player player : plugin.server().getAllPlayers()) {
      flushSession(player.getUniqueId(), false);
    }
  }

  private void flushSession(UUID uuid, boolean endSession) {
    Long started = sessionStartMs.get(uuid);
    if (started == null) {
      return;
    }
    long now = System.currentTimeMillis();
    long elapsedMs = now - started;
    if (endSession) {
      sessionStartMs.remove(uuid);
    } else {
      sessionStartMs.put(uuid, now);
    }

    int minutes = (int) (elapsedMs / Duration.ofMinutes(1).toMillis());
    if (minutes <= 0) {
      return;
    }

    if (!luckPerms.isAvailable()) {
      return;
    }

    luckPerms.loadUser(uuid).thenAccept(optional -> {
      if (optional.isEmpty()) {
        return;
      }
      if (luckPerms.hasRankAboveShop(optional.get())) {
        return;
      }
      Optional<ShopRank> owned = luckPerms.highestShopRank(optional.get());
      if (owned.isPresent() && owned.get() == ShopRank.GRAF) {
        return;
      }
      plugin.server().getScheduler().buildTask(plugin, () ->
          store.credit(uuid, minutes, ShopRank.totalCost())
      ).schedule();
    });
  }
}
