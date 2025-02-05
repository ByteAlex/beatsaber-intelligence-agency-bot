package bot.commands;

import bot.api.ScoreSaber;
import bot.db.DatabaseManager;
import bot.dto.player.Player;
import bot.utils.Messages;
import net.dv8tion.jda.api.entities.TextChannel;

public class UpdatePlayer {

    final DatabaseManager db;

    public UpdatePlayer(DatabaseManager db) {
        this.db = db;
    }

    public void updatePlayer(Player player, TextChannel channel) {
        ScoreSaber ss = new ScoreSaber();
        if (player == null) {
            Messages.sendMessage("Could not find player.", channel);
            return;
        }
        Player storedPlayer = db.getPlayerByName(player.getName());
        Player ssPlayer = player;

        if (storedPlayer == null) {
            Messages.sendMessage("Could not find player \"" + player.getName() + "\".", channel);
            return;
        }

        if (ssPlayer.getId() == null) {
            ssPlayer = ss.getPlayerById(storedPlayer.getId());
        }

        final long playerDiscordId = storedPlayer.getDiscordUserId();
        ssPlayer.setDiscordUserId(playerDiscordId);
        ssPlayer.setCustomAccGridImage(storedPlayer.getCustomAccGridImage());
        Messages.sendMessage("Player \"" + ssPlayer.getName() + "\"updated successfully.", channel);
    }
}
