package de.townity.proxysystem.util;

import de.townity.proxysystem.TownityProxySystem;
import de.townity.proxysystem.config.PluginConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class MotdService {

  private MotdService() {}

  public static Component buildMotd(TownityProxySystem plugin) {
    PluginConfig config = plugin.config();
    PluginConfig.Motd motd = config.motd();
    MessageUtil.Placeholders placeholders =
        new MessageUtil.Placeholders(plugin.onlinePlayers(), config.maxPlayers());

    List<String> lines = resolveLines(plugin, motd);
    return MessageUtil.joinLines(lines, motd.format(), placeholders);
  }

  private static List<String> resolveLines(TownityProxySystem plugin, PluginConfig.Motd motd) {
    String fileName = motd.file();
    if (fileName != null && !fileName.isBlank()) {
      Path path = plugin.dataDirectory().resolve(fileName);
      if (Files.isRegularFile(path)) {
        try {
          List<String> fileLines = Files.readAllLines(path, StandardCharsets.UTF_8).stream()
              .map(String::stripTrailing)
              .filter(line -> !line.isBlank() && !line.trim().startsWith("#"))
              .toList();
          if (!fileLines.isEmpty()) {
            return fileLines;
          }
        } catch (IOException e) {
          plugin.logger().warn("Could not read MOTD file {}: {}", path, e.getMessage());
        }
      } else {
        plugin.logger().warn("MOTD file not found: {}", path);
      }
    }

    List<String> configured = motd.lines();
    if (configured == null || configured.isEmpty()) {
      return List.of();
    }

    // Support a single config entry that already contains a line break from website exports
    if (configured.size() == 1 && configured.getFirst().contains("\n")) {
      List<String> split = new ArrayList<>();
      for (String part : configured.getFirst().split("\\R", -1)) {
        split.add(part);
      }
      return split;
    }
    return configured;
  }
}
