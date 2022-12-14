package me.notpseudo.listeners;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.notpseudo.SecretSantaBot;
import me.notpseudo.users.GroupMember;
import me.notpseudo.util.JSONUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.TextInput;
import org.javacord.api.entity.message.component.TextInputStyle;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;

import java.util.*;

public class MessageButtonListener implements MessageComponentCreateListener {

    private final DiscordApi API;
    private static final MongoDatabase SERVER_DATABASE;
    private static Set<Long> endIDs = new HashSet<>();

    static {
        MongoClient client = MongoClients.create(SecretSantaBot.getMongoToken());
        SERVER_DATABASE = client.getDatabase("servers");
    }

    public MessageButtonListener(DiscordApi api) {
        API = api;
    }

    @Override
    public void onComponentCreate(MessageComponentCreateEvent createEvent) {
        MessageComponentInteraction interaction = createEvent.getMessageComponentInteraction();
        InteractionOriginalResponseUpdater responseUpdater = interaction.respondLater(true).join();
        interaction.getServer().ifPresentOrElse(s -> {
            String serverID = s.getIdAsString();
            MongoCollection<Document> documents = SERVER_DATABASE.getCollection(serverID);
            long messageID = interaction.getMessage().getId();
            Document document = documents.find(new Document("message", messageID)).first();
            if (document == null) {
                responseUpdater.setContent("This group was ended or could not be found").update();
                return;
            }
            ArrayList<String> userArray = (ArrayList<String>) document.get("users");
            if (userArray == null) {
                userArray = new ArrayList<>();
            }
            Set<GroupMember> userSet = JSONUtils.getMembers(userArray);
            long userID = interaction.getUser().getId();
            GroupMember member = new GroupMember(userID);
            switch (interaction.getCustomId()) {
                case "join" -> {
                    if (document.getBoolean("started")) {
                        responseUpdater.setContent("You can not join this group because the host already started it!").update();
                        return;
                    }
                    if (addUser(userSet, member)) {
                        responseUpdater.setContent("You joined the Secret Santa group!").update();
                    } else {
                        responseUpdater.setContent("You are already part of this group").update();
                    }
                }
                case "leave" -> {
                    if (document.getBoolean("started")) {
                        responseUpdater.setContent("You can not leave this group because the host already started it!").update();
                        return;
                    }
                    if (removeUser(userSet, member)) {
                        responseUpdater.setContent("You left the Secret Santa group!").update();
                    } else {
                        responseUpdater.setContent("You are not part of this group").update();
                    }
                }
                case "editlist" -> {
                    responseUpdater.setContent("Enter your wishlist into the modal!").update();
                    interaction.respondWithModal("wishlist", "Tell us what you wish for!",
                            ActionRow.of(TextInput.create(TextInputStyle.PARAGRAPH, "itemlist", "List Your Wish Items Here", true)),
                            ActionRow.of(TextInput.create(TextInputStyle.PARAGRAPH, "extrainfo", "Enter any extra info here"))
                    );
                    return;
                }
                case "viewreceiver" -> {
                    if (!document.getBoolean("started")) {
                        responseUpdater.setContent("The host has not started this group yet! Wait for the group to start to be assigned someone to get a gift for").update();
                        return;
                    }
                    GroupMember foundMember = findMember(userSet, userID);
                    if (foundMember == null) {
                        responseUpdater.setContent("You do not have a gift receiver or there was an error finding your receiver").update();
                        return;
                    }
                    API.getUserById(foundMember.getGiftReceiver().getUserID()).whenComplete((r, error) -> {
                        if (error != null) {
                            System.out.println("There was an error getting a user by ID");
                            responseUpdater.setContent("There was an error finding your receiver").update();
                            return;
                        }
                        responseUpdater.setContent("Your gift receiver is " + r.getMentionTag()).update();
                    });
                }
                case "startgroup" -> {
                    if (userID != document.getLong("host")) {
                        responseUpdater.setContent("You are not the host of this group!").update();
                        return;
                    }
                    if (document.getBoolean("started")) {
                        responseUpdater.setContent("You have already started this group! If there are issues with member assignments, please remake the group with </startgroup:0>").update();
                        return;
                    }
                    if (!startGroup(userSet)) {
                        responseUpdater.setContent("Could not start this group!").update();
                        return;
                    }
                    document.put("started", true);
                    responseUpdater.setContent("Started the group!").update();
                }
                case "endgroup"-> {
                    if (userID != document.getLong("host")) {
                        responseUpdater.setContent("You are not the host of this group!").update();
                        return;
                    }
                    if (endIDs.remove(userID)) {
                        responseUpdater.setContent("Ended and removed this group").update();
                        documents.deleteOne(new Document("message",  messageID));
                    } else {
                        responseUpdater.setContent("CAREFUL! This will completely end and remove this group. No other actions will work! Click again in 10 to confirm").update();
                        endIDs.add(userID);
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                endIDs.remove(userID);
                            }
                        }, 10 * 1000);
                    }
                    return;
                }
            }
            userArray = new ArrayList<>();
            for (GroupMember gm : userSet) {
                userArray.add(JSONUtils.getJSONString(gm));
            }
            document.put("users", userArray);
            Bson query = new Document("message",  messageID);
            documents.replaceOne(query, document);
        }, () -> responseUpdater.setContent("You must use this feature in a server").update());

    }

    private static boolean addUser(Set<GroupMember> group, GroupMember member) {
        for (GroupMember gm : group) {
            if (gm.equals(member)) {
                return false;
            }
        }
        return group.add(member);
    }

    private static boolean removeUser(Set<GroupMember> group, GroupMember member) {
        return group.removeIf(gm -> gm.equals(member));
    }

    private static GroupMember findMember(Set<GroupMember> group, long id) {
        for (GroupMember gm : group) {
            if (gm.getUserID() == id) {
                return gm;
            }
        }
        return null;
    }

    private boolean startGroup(Set<GroupMember> group) {
        if (group.size() < 2) {
            return false;
        }
        Random rand = new Random();
        GroupMember[] members = group.toArray(new GroupMember[0]);
        for (int i = 0; i < members.length; i++) {
            int receiverIndex = rand.nextInt(members.length);
            while (receiverIndex == i) {
                receiverIndex = rand.nextInt(members.length);
            }
            members[i].setGiftReceiver(members[receiverIndex].getUserID());
        }
        return true;
    }

}