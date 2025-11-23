package me.notpseudo.listeners;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.notpseudo.SecretSantaBot;
import me.notpseudo.users.GroupMember;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.ModalSubmitEvent;
import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.listener.interaction.ModalSubmitListener;

public class WishlistListener implements ModalSubmitListener {

    private final DiscordApi API;
    private static final MongoDatabase USER_DATABASE;

    static {
        USER_DATABASE = SecretSantaBot.getUserDatabase();
    }

    public WishlistListener(DiscordApi api) {
        API = api;
    }

    @Override
    public void onModalSubmit(ModalSubmitEvent submitEvent) {
        ModalInteraction interaction = submitEvent.getModalInteraction();
        interaction.getServer().ifPresentOrElse(s -> {
            long userID = interaction.getUser().getId();
            GroupMember member = new GroupMember(userID);
            String wishlist = interaction.getTextInputValueByCustomId("itemlist").orElse("");
            String extraInfo = interaction.getTextInputValueByCustomId("extrainfo").orElse("");
            member.setWishlist(wishlist);
            member.setExtraInfo(extraInfo);
            MongoCollection<Document> documents = USER_DATABASE.getCollection(s.getIdAsString());
            Document document = documents.find(new Document("memberid", userID)).first();
            boolean found = true;
            if (document == null) {
                document = new Document("memberid", userID);
                found = false;
            }
            document.put("wishlist", wishlist);
            document.put("extrainfo", extraInfo);
            Bson query = new Document("memberid",  userID);
            if (found) {
                documents.replaceOne(query, document);
            } else {
                documents.insertOne(document);
            }
            interaction.respondLater(true).join().setContent("Updated!").update();
        }, () -> interaction.respondLater(true).join().setContent("You must use this in the server you want the wishlist to be applied to").update());
    }

}