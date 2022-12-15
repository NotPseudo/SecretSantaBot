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

public class CommandListener implements SlashCommandCreateListener {

    private final DiscordApi API;
    private static final MongoDatabase SERVER_DATABASE;

    static {
        MongoClient client = MongoClients.create(SecretSantaBot.getMongoToken());
        SERVER_DATABASE = client.getDatabase("servers");
    }

    public CommandListener(DiscordApi api) {
        API = api;
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent createEvent) {
        SlashCommandInteraction interaction = createEvent.getSlashCommandInteraction();
        InteractionOriginalResponseUpdater responseUpdater = interaction.respondLater(true).join();
        List<SlashCommandInteractionOption> options = interaction.getArguments();
        interaction.getServer().ifPresentOrElse(s -> {
            switch (interaction.getCommandName()) {
                case "hostgroup" -> {
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
                                .setTitle("**Secret Santa Group**")
                                .setDescription("Click the buttons below to interact with this group!\n> Minimum Gift Value: " + finalMin + "\n> Maximum Gift Value: " + finalMax + "\n> Host: " + interaction.getUser().getMentionTag())
                                .setColor(Color.CYAN)
                                .setFooter("Made by Yume#0505");

                        MessageBuilder message = new MessageBuilder()
                                .setContent("")
                                .setEmbed(embed)
                                .addComponents(
                                        ActionRow.of(Button.success("join", "Join the Group"),
                                                Button.danger("leave", "Leave the Group"),
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
                    }, () -> responseUpdater.setContent("There was an issue!").update());
                }
                case "wishlist" -> {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("**Wishlist Info**")
                            .setDescription("Click on the buttons below to view or edit your wishlist for this server. Your wishlists are not global. You have a different list for each server this bot is in")
                            .setColor(Color.CYAN)
                            .setFooter("Made by Yume#0505");

                    MessageBuilder message = new MessageBuilder()
                            .setContent("")
                            .setEmbed(embed)
                            .addComponents(
                                    ActionRow.of(
                                            Button.secondary("editlist", "Edit Your Wishlist"),
                                            Button.secondary("viewlist", "View Your Current Wishlist")
                                    )
                            );
                    interaction.getChannel().ifPresentOrElse(c -> message.send(c).whenComplete((m, err) -> {
                       if (err != null) {
                           System.out.println("There was an error sending the message");
                       }
                    }), () -> responseUpdater.setContent("There was an issue!").update());
                }
                case "help" -> interaction.getChannel().ifPresentOrElse(c -> {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setAuthor("SecretSantaBot", "https://discord.com/api/oauth2/authorize?client_id=1052020092445151254&permissions=148176751680&scope=bot", "https://images-ext-2.discordapp.net/external/KX6W6aNQdxt_D9lko6uV_1c6IvomCgPcoTDqARJ0XOA/%3Fsize%3D4096/https/cdn.discordapp.com/avatars/1052020092445151254/28d5919df312e6e35c8599cd5b7a7248.png?width=905&height=905")
                            .setTitle("**Secret Santa Bot Info**")
                            .setDescription("This is an information panel for the Secret Santa Bot. Don't worry! There aren;t too many commands, and the bot should be simple to use")
                            .addField("**1. Commands**", """
                                    Host a new Secret Santa Group in a server by using the </hostgroup:1052739101826228265> command
                                    > There is a required minimum value option, an optional maximum value option, and an optional channel to send to option

                                    View or edit your wishlist in a server with the </wishlist:1052738943432536224> command""")
                            .addField("**2. Basic Group Actions**", """
                                    Everyone who sees a group will have the option to join it, leave it, and view their assigned gift receiver
                                    > **THE HOST MUST STILL JOIN THE GROUP.** The host is not automatically placed into the group. They too must click the button
                                    > You can not join or leave a group after the host has started the group
                                    > You do not have an assigned gift receiver until the host starts the group. Check back later
                                    > When you view your receiver, you will be given their Discord tag and their wishlist, if they made one""")
                            .addField("**3. Host Group Actions**", """
                                    Hosts have the options to start and end a Secret Santa group
                                    > Starting a group requires at least 2 people in the group
                                    > Starting a group locks the group, and nobody can join or leave the group after it is started
                                    > Everybody in the group will be assigned another member as the gift recipient once the group starts. You can not view the recipient before the group starts
                                    > Hosts can completely end and delete a Secret Santa Group if needed. You will need to click the button twice within 10 seconds to confirm""")
                            .addField("**4. Wishlist Actions**", """
                                    Use the </wishlist:1052738943432536224> command to edit or view your server wishlist
                                    > You can use this command any time in a server the bot is in, even if you are not part of Secret Santa groups
                                    > When you submit info in the Edit option, it will completely overwrite any previous wishlists. You may want to check your wishlist first before rewriting it
                                    > You can always view your wishlist using the View option to see what you've put so far""")
                            .addField("**5. Other Info**", "Invite the Bot to your Server: https://discord.com/api/oauth2/authorize?client_id=1052020092445151254&permissions=148176751680&scope=bot\n" +
                                    "GitHub Repo for this Bot: https://github.com/NotPseudo/SecretSantaBot")
                            .setColor(Color.CYAN)
                            .setFooter("Made by Yume#0505");
                    c.sendMessage(embed).whenComplete((m, err) -> {
                        if (err != null) {
                            System.out.println("There was an error sending the message");
                        }
                    });
                }, () -> responseUpdater.setContent("There was an issue!").update());
            }
        }, () -> responseUpdater.setContent("You must use commands in a server").update());

    }

}
