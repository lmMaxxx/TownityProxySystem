package de.townity.proxysystem.rangshop;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;
import org.slf4j.Logger;

/**
 * LuckPerms auf Velocity – Gruppen lesen/setzen für den Rangshop.
 */
public final class LuckPermsBridge {

  private static final Set<String> IGNORED_GROUPS = Set.of(
      "default",
      "defaults",
      "member",
      "members",
      "spieler",
      "player",
      "players"
  );

  private final Logger logger;
  private Boolean available;

  public LuckPermsBridge(Logger logger) {
    this.logger = logger;
  }

  public boolean isAvailable() {
    if (available != null) {
      return available;
    }
    try {
      LuckPermsProvider.get();
      available = true;
    } catch (IllegalStateException e) {
      available = false;
      logger.warn("LuckPerms is not loaded – Rangshop disabled until LuckPerms is installed on Velocity.");
    }
    return available;
  }

  private LuckPerms api() {
    return LuckPermsProvider.get();
  }

  public CompletableFuture<Optional<User>> loadUser(UUID uuid) {
    if (!isAvailable()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    User cached = api().getUserManager().getUser(uuid);
    if (cached != null) {
      return CompletableFuture.completedFuture(Optional.of(cached));
    }
    return api().getUserManager().loadUser(uuid).thenApply(Optional::ofNullable);
  }

  public Optional<ShopRank> highestShopRank(User user) {
    ShopRank highest = null;
    for (String group : inheritedGroupNames(user)) {
      Optional<ShopRank> rank = ShopRank.byGroup(group);
      if (rank.isPresent() && (highest == null || rank.get().order() > highest.order())) {
        highest = rank.get();
      }
    }
    return Optional.ofNullable(highest);
  }

  /**
   * True, wenn der Spieler eine LuckPerms-Gruppe außerhalb Shop/Default hat (z. B. Team).
   */
  public boolean hasRankAboveShop(User user) {
    for (String group : inheritedGroupNames(user)) {
      if (IGNORED_GROUPS.contains(group) || ShopRank.byGroup(group).isPresent()) {
        continue;
      }
      return true;
    }
    return false;
  }

  public CompletableFuture<Boolean> assignShopRank(UUID uuid, ShopRank rank) {
    if (!isAvailable()) {
      return CompletableFuture.completedFuture(false);
    }
    return loadUser(uuid).thenCompose(optional -> {
      if (optional.isEmpty()) {
        return CompletableFuture.completedFuture(false);
      }
      User user = optional.get();
      user.data().clear(node ->
          node.getType() == NodeType.INHERITANCE
              && ShopRank.byGroup(((InheritanceNode) node).getGroupName()).isPresent()
      );
      user.data().add(InheritanceNode.builder(rank.group()).build());
      try {
        user.setPrimaryGroup(rank.group());
      } catch (Exception e) {
        logger.warn("Could not set primary group {} for {}: {}", rank.group(), uuid, e.getMessage());
      }
      return api().getUserManager().saveUser(user).thenApply(v -> true);
    });
  }

  private Collection<String> inheritedGroupNames(User user) {
    return user.getInheritedGroups(QueryOptions.nonContextual()).stream()
        .map(Group::getName)
        .map(name -> name.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
  }

}
