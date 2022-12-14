package me.notpseudo.listeners;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import me.notpseudo.SecretSantaBot;
import org.bson.Document;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import java.awt.*;
import java.util.List;

public class GroupStartCommand implements SlashCommandCreateListener {

    private final DiscordApi API;
    private static final MongoDatabase SERVER_DATABASE;

    static {
        MongoClient client = MongoClients.create(SecretSantaBot.getMongoToken());
        SERVER_DATABASE = client.getDatabase("servers");
    }

    public GroupStartCommand(DiscordApi api) {
        API = api;
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent createEvent) {
        SlashCommandInteraction interaction = createEvent.getSlashCommandInteraction();
        InteractionOriginalResponseUpdater responseUpdater = interaction.respondLater(true).join();
        List<SlashCommandInteractionOption> options = interaction.getArguments();
        interaction.getServer().ifPresentOrElse(s -> {
            switch (interaction.getCommandName()) {
                case "startgroup" -> {
                    interaction.getUser().openPrivateChannel().whenComplete((channel, error) -> {
                        if (error != null) {
                            System.out.println("There was an issue with opening a private channel for " + interaction.getUser().getName());
                            return;
                        }
                    });
                    String min = "5", max = "Not specififed";
                    if (options.size() >= 1) {
                        min = options.get(0).getStringValue().orElse("5");
                    }
                    if (options.size() >= 2) {
                        max = options.get(1).getStringValue().orElse("Not specified");
                    }
                    String finalMin = min, finalMax = max;
                    interaction.getChannel().ifPresentOrElse(c -> {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setAuthor("SecretSantaBot", "https://discord.com/api/oauth2/authorize?client_id=1052020092445151254&permissions=148176751680&scope=bot", "https://images-ext-2.discordapp.net/external/KX6W6aNQdxt_D9lko6uV_1c6IvomCgPcoTDqARJ0XOA/%3Fsize%3D4096/https/cdn.discordapp.com/avatars/1052020092445151254/28d5919df312e6e35c8599cd5b7a7248.png?width=905&height=905")
                                .setTitle("Secret Santa Group")
                                .setDescription("React to this embed below to join this Secret Santa group!\nMin: " + finalMin + "\nMax: " + finalMax + "\nHost: " + interaction.getUser().getMentionTag())
                                .setColor(Color.CYAN)
                                .setFooter("Made by Yume#0505");

                        MessageBuilder message = new MessageBuilder()
                                .setContent("")
                                .setEmbed(embed)
                                .addComponents(
                                        ActionRow.of(Button.success("join", "Join the Group"),
                                                Button.danger("leave", "Leave the Group")
                                        ),
                                        ActionRow.of(
                                                Button.secondary("editlist", "Edit Your Wishlist"),
                                                Button.secondary("viewreceiver", "View Your Person")
                                        ),
                                        ActionRow.of(Button.success("startgroup", "Start Group and Assign Receivers"),
                                                Button.danger("endgroup", "Fully End the Group")
                                        )
                                );
                        if (options.size() >= 3) {
                            options.get(2).getChannelValue().ifPresent(channel -> {
                                if (channel instanceof TextChannel textChannel) {
                                    message.send(textChannel).whenComplete((m, error) -> {
                                        if (error != null) {
                                            System.out.println("There was an error sending a message to a specified text channel");
                                            return;
                                        }
                                        Document document = new Document("message", m.getId()).append("host", interaction.getUser().getId()).append("started", false);
                                        SERVER_DATABASE.getCollection(s.getIdAsString()).insertOne(document);
                                    });
                                } else {
                                    message.send(c).whenComplete((m, error) -> {
                                        if (error != null) {
                                            System.out.println("There was an error sending a message to the original text channel");
                                            return;
                                        }
                                        Document document = new Document("message", m.getId()).append("host", interaction.getUser().getId()).append("started", false);
                                        SERVER_DATABASE.getCollection(s.getIdAsString()).insertOne(document);
                                    });
                                }
                            });
                        } else {
                            message.send(c).whenComplete((m, error) -> {
                                if (error != null) {
                                    System.out.println("There was an error sending a message to the original text channel");
                                    return;
                                }
                                Document document = new Document("message", m.getId()).append("host", interaction.getUser().getId()).append("started", false);
                                SERVER_DATABASE.getCollection(s.getIdAsString()).insertOne(document);
                            });
                        }
                    }, () -> {
                        responseUpdater.setContent("There was an issue!").update();
                    });
                }
            }
        }, () -> responseUpdater.setContent("You must use commands in a server").update());

    }

}
