package de.townity.proxysystem.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.townity.proxysystem.config.TownityMessages;
import de.townity.proxysystem.rangshop.RangShopDialogService;
import de.townity.proxysystem.util.Messages;

public final class RangShopCommand implements SimpleCommand {

  private final RangShopDialogService dialogs;

  public RangShopCommand(RangShopDialogService dialogs) {
    this.dialogs = dialogs;
  }

  @Override
  public void execute(Invocation invocation) {
    if (!(invocation.source() instanceof Player player)) {
      Messages.sendError(invocation.source(), TownityMessages.RANGSHOP_PLAYERS_ONLY);
      return;
    }
    dialogs.open(player);
  }
}
