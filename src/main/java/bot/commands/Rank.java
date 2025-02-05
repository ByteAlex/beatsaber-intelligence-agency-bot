package bot.commands;

import bot.api.ApiConstants;
import bot.api.ScoreSaber;
import bot.dto.MessageEventDTO;
import bot.dto.leaderboards.LeaderboardPlayer;
import bot.dto.leaderboards.LeaderboardType;
import bot.dto.player.Player;
import bot.utils.Format;
import bot.utils.Messages;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Rank {

    private final Player player;

    public Rank(Player player) {
        this.player = player;
        if (player == null) {
            throw new NullPointerException("Command player not found.");
        }
    }

    public void sendGlobalRank(MessageEventDTO event) {
        int startPage = getPageNrFromPlayerRank(player.getRank() - 2);
        sendRank(player, startPage, 200, null, LeaderboardType.GLOBAL, event);
    }

    public void sendLocalRank(MessageEventDTO event) {
        int startPage = getPageNrFromPlayerRank(player.getCountryRank() - 2);
        sendRank(player, startPage, 200, player.getCountry(), LeaderboardType.LOCAL, event);
    }

    public void sendDACHRank(MessageEventDTO event) {
        String[] dachCodes = {"de","at","ch"};
        if (!Arrays.asList(dachCodes).contains(player.getCountry().toLowerCase())) {
            Messages.sendMessage("Your are not from Germany, Austria or Switzerland.", event.getChannel());
            return;
        }
        sendRank(player, 1, 10000, "de,at,ch", LeaderboardType.DACH, event);
    }

    private void sendRank(Player player, int startPage, int sizeLimit, String countryCode, LeaderboardType leaderboardType, MessageEventDTO event) {
        ScoreSaber ss = new ScoreSaber();
        List<LeaderboardPlayer> leaderboardEntries = ss.findLeaderboardEntriesAroundPlayer(player, countryCode, startPage, sizeLimit);
        if (leaderboardEntries == null) {
            Messages.sendMessage("Could not extract ScoreSaber profiles. Maybe your rank is too low or there is another error.", event.getChannel());
            return;
        }
        LeaderboardPlayer playerEntry = leaderboardEntries.stream()
                .filter(entry -> entry.getIdLong() == player.getPlayerIdLong())
                .findFirst()
                .orElse(null);
        int playerIndex = leaderboardEntries.indexOf(playerEntry);
        assert playerEntry != null;
        double playerPp = playerEntry.getPp();

        String resultMessage = Format.bold("------------------------------------") + "\n";
        if (playerIndex > 1) {
            resultMessage += toEntryString(leaderboardEntries.get(playerIndex - 2), playerPp, leaderboardType);
        }
        if (playerIndex > 0) {
            resultMessage += toEntryString(leaderboardEntries.get(playerIndex - 1), playerPp, leaderboardType);
        }
        resultMessage += toEntryString(playerEntry, -1, leaderboardType);
        resultMessage += toEntryString(leaderboardEntries.get(playerIndex + 1), playerPp, leaderboardType);
        resultMessage += toEntryString(leaderboardEntries.get(playerIndex + 2), playerPp, leaderboardType);

        String title = "";
        switch (leaderboardType) {
            case LOCAL:
                title = "Local leaderboard";
                break;
            case GLOBAL:
                title = "Global leaderboard";
                break;
            case DACH:
                title = "Leaderboard for Germany, Austria & Switzerland";
                break;
        }
        String titleUrl = "";
        switch (leaderboardType) {
            case LOCAL:
                titleUrl = getLeaderboardUrl(getPageNrFromPlayerRank(player.getCountryRank()), countryCode);
                break;
            case GLOBAL:
                titleUrl = getLeaderboardUrl(getPageNrFromPlayerRank(player.getRank()), countryCode);
                break;
            case DACH:
                titleUrl = getLeaderboardUrl(getPageNrFromPlayerRank(playerIndex),  countryCode);
                break;
        }

        Messages.sendMessageWithTitle(resultMessage, Format.underline(title), titleUrl, event.getChannel());
    }

    private String toEntryString(LeaderboardPlayer playerEntry, double ownPP, LeaderboardType type) {
        String entryString = "";
        if (playerEntry == null) {
            return entryString;
        }
        boolean isOwnEntry = ownPP == -1;

        int entryRank;
        switch (type) {
            case GLOBAL:
                entryRank = playerEntry.getRank();
                break;
            case LOCAL:
                entryRank = playerEntry.getCountryRank();
                break;
            case DACH:
                entryRank = playerEntry.getCustomLeaderboardRank();
                break;
            default:
                entryRank = -1;
        }
        entryString += "#" + entryRank;
        String countryName = new Locale("", playerEntry.getCountry().toUpperCase()).getDisplayCountry(Locale.ENGLISH);
        if (type == LeaderboardType.LOCAL) {
            entryString += " in " + countryName + " :flag_" + playerEntry.getCountry().toLowerCase() + ": \n";
        } else {
            entryString += "\nFrom " + countryName + " :flag_" + playerEntry.getCountry().toLowerCase() + ": \n";
        }
        entryString += Format.bold(Format.link(playerEntry.getName(), ApiConstants.USER_PRE_URL + playerEntry.getId()) + (isOwnEntry ? " (You)" : "")) + "\n";
        if (!isOwnEntry) {
            double ppDiff = playerEntry.getPp() - ownPP;
            entryString += (ppDiff > 0 ? Format.decimal(ppDiff) + "pp more than you." : Format.decimal(-ppDiff) + "pp less than you.") + "\n";
        } else {
            entryString += "Your PP: " + playerEntry.getPp() + "pp\n";
        }
        return entryString + Format.bold("------------------------------------") + "\n";
    }

    private static int getPageNrFromPlayerRank(int rank) {
        if (rank <= 0) {
            return 1;
        }
        int modulus = rank % 50;
        int page = rank / 50;
        if (modulus != 0) {
            page++;
        }
        return page;
    }

    private String getLeaderboardUrl(int pageNr, String countryCode) {
        String url = ApiConstants.PLAYER_LEADERBOARDS_URL + pageNr;
        if (countryCode != null) {
            url += "&countries=" + countryCode;
        }
        return url;
    }
}
