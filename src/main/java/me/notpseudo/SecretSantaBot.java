package me.notpseudo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.github.cdimascio.dotenv.Dotenv;
import me.notpseudo.listeners.GroupStartCommand;
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

    static {
        Dotenv dotenv = Dotenv.load();
        TOKEN = dotenv.get("TOKEN");
        MONGO_TOKEN = dotenv.get("MONGO_TOKEN");
        CLIENT = MongoClients.create(MONGO_TOKEN);
    }

    public static void main(String[] args) {
        try {
            DiscordApi api = new DiscordApiBuilder().setToken(TOKEN).addListener(GroupStartCommand::new).addListener(MessageButtonListener::new).addListener(WishlistListener::new).login().join();
            SlashCommand.with("startgroup", "Run this command to start aSecret Santa group and wait for members to join!",
                            Arrays.asList(
                                    SlashCommandOption.create(SlashCommandOptionType.STRING, "minimum", "Required minimum cost of gift", true),
                                    SlashCommandOption.create(SlashCommandOptionType.STRING, "maximum", "Optional maximum cost of gift", false),
                                    SlashCommandOption.create(SlashCommandOptionType.CHANNEL, "channel", "An optional channel to send the embed in", false)
                            )
                    )
                    .createGlobal(api)
                    .join();
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

}