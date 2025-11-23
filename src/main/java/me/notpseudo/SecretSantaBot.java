package me.notpseudo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import me.notpseudo.listeners.CommandListener;
import me.notpseudo.listeners.MessageButtonListener;
import me.notpseudo.listeners.WishlistListener;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.Arrays;
import java.util.concurrent.CompletionException;

public class SecretSantaBot {

    private static final String TOKEN;
    private static final String MONGO_TOKEN;
    private static final MongoClient CLIENT;
    private static final MongoDatabase SERVER_DATABASE;
    private static final MongoDatabase USER_DATABASE;
    private static final long OWNERID;
    private static String OWNERTAG;

    static {
        Dotenv dotenv = Dotenv.load();
        TOKEN = dotenv.get("TOKEN");
        MONGO_TOKEN = dotenv.get("MONGO_TOKEN");
        OWNERID = Long.parseLong(dotenv.get("OWNERID"));
        CLIENT = MongoClients.create(SecretSantaBot.getMongoToken());
        SERVER_DATABASE = CLIENT.getDatabase("servers");
        USER_DATABASE = CLIENT.getDatabase("users");
    }

    public static void main(String[] args) {
        try {
            DiscordApi api = new DiscordApiBuilder().setToken(TOKEN).addListener(CommandListener::new).addListener(MessageButtonListener::new).addListener(WishlistListener::new).login().join();
            SlashCommand.with("hostgroup", "Run this command to set up a Secret Santa group and wait for members to join!",
                            Arrays.asList(
                                    SlashCommandOption.create(SlashCommandOptionType.STRING, "minimum", "Required minimum cost of gift", true),
                                    SlashCommandOption.create(SlashCommandOptionType.STRING, "maximum", "Optional maximum cost of gift", false),
                                    SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "channel", "An optional channel to send the embed in", false)
                            )
                    )
                    .createGlobal(api)
                    .join();
            SlashCommand.with("wishlist", "View or edit your wishlist for a server").createGlobal(api).join();
            SlashCommand.with("help", "Get info about this bot or commands").createGlobal(api).join();
            OWNERTAG = api.getUserById(OWNERID).join().getMentionTag();
        } catch (CompletionException e) {
            System.out.println("\u001B[31mThere was an issue trying to log in to your bot");
        }

    }

    public static String getMongoToken() {
        return MONGO_TOKEN;
    }

    public static MongoClient getClient() {
        return CLIENT;
    }

    public static MongoDatabase getServerDatabase() {
        return SERVER_DATABASE;
    }

    public static MongoDatabase getUserDatabase() {
        return USER_DATABASE;
    }

    public static String getOwnerTag() {
        return OWNERTAG;
    }

}