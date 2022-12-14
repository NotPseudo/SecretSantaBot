package me.notpseudo.listeners;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.notpseudo.SecretSantaBot;
import me.notpseudo.users.GroupMember;
import org.bson.Document;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.ModalSubmitEvent;
import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.listener.interaction.ModalSubmitListener;

public class WishlistListener implements ModalSubmitListener {

    private final DiscordApi API;
    private static final MongoDatabase USER_DATABASE;

    static {
        USER_DATABASE = SecretSantaBot.getClient().getDatabase("users");
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
        }, () -> {});

    }

}