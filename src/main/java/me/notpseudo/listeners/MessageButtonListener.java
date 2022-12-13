package me.notpseudo.listeners;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.notpseudo.SecretSantaBot;
import org.bson.Document;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;

public class MessageButtonListener implements MessageComponentCreateListener {

    private final DiscordApi API;
    private static final MongoDatabase DATABASE = SecretSantaBot.getDatabase();

    public MessageButtonListener(DiscordApi api) {
        API = api;
    }

    @Override
    public void onComponentCreate(MessageComponentCreateEvent createEvent) {
        MessageComponentInteraction interaction = createEvent.getMessageComponentInteraction();
        switch (interaction.getCustomId()) {
            case "join" -> {
                interaction.getServer().ifPresent(s -> {
                    String serverID = s.getIdAsString();
                    MongoCollection<Document> documents = DATABASE.getCollection(serverID);
                    long messageID = interaction.getMessage().getId();
                    Document document = documents.find(new Document("message", messageID)).first();
                    if (document == null) {
                        document = new Document("message", messageID);
                    }
                    // Access array of entered users and add the user who just interacted to join
                });


            }
            case "leave" -> {

            }
        }
    }

}