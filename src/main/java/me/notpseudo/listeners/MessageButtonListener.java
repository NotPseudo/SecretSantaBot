package me.notpseudo.listeners;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.notpseudo.SecretSantaBot;
import me.notpseudo.users.GroupMember;
import org.bson.Document;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MessageButtonListener implements MessageComponentCreateListener {

    private final DiscordApi API;
    private static final MongoDatabase DATABASE = SecretSantaBot.getDatabase();

    public MessageButtonListener(DiscordApi api) {
        API = api;
    }

    @Override
    public void onComponentCreate(MessageComponentCreateEvent createEvent) {
        MessageComponentInteraction interaction = createEvent.getMessageComponentInteraction();
        InteractionOriginalResponseUpdater responseUpdater = interaction.respondLater(true).join();
        interaction.getServer().ifPresentOrElse(s -> {
            String serverID = s.getIdAsString();
            MongoCollection<Document> documents = DATABASE.getCollection(serverID);
            long messageID = interaction.getMessage().getId();
            Document document = documents.find(new Document("message", messageID)).first();
            if (document == null) {
                document = new Document("message", messageID);
            }
            GroupMember[] userArray = (GroupMember[]) document.get("users");
            if (userArray == null) {
                userArray = new GroupMember[50];
            }
            Set<GroupMember> userSet = new HashSet<>(Arrays.asList(userArray));
            Long userID = interaction.getUser().getId();
            GroupMember member = new GroupMember(userID);
            switch (interaction.getCustomId()) {
                case "join" -> {
                    if (userSet.add(member)) {
                        responseUpdater.setContent("You joined the Secret Santa group!").update();
                    } else {
                        responseUpdater.setContent("You are already part of this group").update();
                    }
                }
                case "leave" -> {
                    if (userSet.remove(member)) {
                        responseUpdater.setContent("You left the Secret Santa group!").update();
                    } else {
                        responseUpdater.setContent("You are not part of this group").update();
                    }
                }
            }
        }, () -> responseUpdater.setContent("You must use this feature in a server").update());

    }

}