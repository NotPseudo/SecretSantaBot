package me.notpseudo.listeners;

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

    public GroupStartCommand(DiscordApi api) {
        API = api;
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent createEvent) {
        SlashCommandInteraction interaction = createEvent.getSlashCommandInteraction();
        InteractionOriginalResponseUpdater responseUpdater = interaction.respondLater(true).join();
        List<SlashCommandInteractionOption> options = interaction.getArguments();
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
                                    ));

                    responseUpdater.setContent("It worked!").update();
                    if (options.size() >= 3) {
                        options.get(2).getChannelValue().ifPresent(channel -> {
                            if (channel instanceof TextChannel textChannel) {
                                message.send(textChannel);
                            } else {
                                message.send(c);
                            }
                        });
                    } else {
                        message.send(c);
                    }
                }, () -> {
                    responseUpdater.setContent("There was an issue!").update();
                });
            }
        }
    }

}
