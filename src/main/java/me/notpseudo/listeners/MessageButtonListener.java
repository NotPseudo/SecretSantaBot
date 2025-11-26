package me.notpseudo.listeners;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import me.notpseudo.SecretSantaBot;
import me.notpseudo.users.GroupMember;
import me.notpseudo.util.JSONUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.TextInput;
import org.javacord.api.entity.message.component.TextInputStyle;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MessageButtonListener implements MessageComponentCreateListener {

    private final DiscordApi API;
    private static final MongoDatabase SERVER_DATABASE;
    private static final MongoDatabase USER_DATABASE;
    private static Set<Long> endIDs = new HashSet<>();

    static {
        SERVER_DATABASE = SecretSantaBot.getServerDatabase();
        USER_DATABASE = SecretSantaBot.getUserDatabase();
    }

    public MessageButtonListener(DiscordApi api) {
        API = api;
    }

    @Override
    public void onComponentCreate(MessageComponentCreateEvent createEvent) {
        MessageComponentInteraction interaction = createEvent.getMessageComponentInteraction();
        interaction.getServer().ifPresentOrElse(s -> {
            if (interaction.getCustomId().equals("editlist")) {
                sendModal(interaction);
                return;
            }
            InteractionOriginalResponseUpdater responseUpdater = interaction.respondLater(true).join();
            long userID = interaction.getUser().getId();
            if (interaction.getCustomId().equals("viewlist")) {
                MongoCollection<Document> userDocs = USER_DATABASE.getCollection(s.getIdAsString());
                Document userDoc = userDocs.find(new Document("memberid", userID)).first();
                if (userDoc == null) {
                    responseUpdater.setContent("You have not made a wishlist yet!").update();
                    return;
                }
                responseUpdater.setContent("Your Wishlist: \n```" + userDoc.getString("wishlist") + "```\nYour instructions/info: \n```" + userDoc.getString("extrainfo") + " ```").update();
                return;
            }
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
            GroupMember member = new GroupMember(userID);
            long origMessageId = document.getLong("message"), channelId = document.getLong("channelId");
            String groupName = document.getString("groupName");
            TextChannel channel = s.getTextChannelById(channelId).orElse(null);
            String minimum = document.getString("min"), maximum = document.getString("max");
            switch (interaction.getCustomId()) {
                case "join" -> {
                    if (document.getBoolean("started")) {
                        responseUpdater.setContent("You can not join this group because the host already started it!").update();
                        return;
                    }
                    if (addUser(userSet, member)) {
                        if (channel != null) {
                            Message origMessage = API.getMessageById(origMessageId, channel).join();
                            EmbedBuilder embed = new EmbedBuilder()
                                    .setTitle("**Secret Santa Group**")
                                    .setDescription("Click the buttons below to interact with this group!\n## Group Name: " + groupName + "\n- Member Count: `" + userSet.size() + "`\n- Minimum Gift Value: " + minimum + "\n- Maximum Gift Value: " + maximum + "\n- Host: " + API.getUserById(document.getLong("host")).join().getMentionTag())
                                    .setColor(Color.CYAN)
                                    .addField("Made by ", SecretSantaBot.getOwnerTag())
                                    .setTimestampToNow();
                            origMessage.createUpdater().setContent("").setEmbed(embed).applyChanges().join();
                        }
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
                        if (channel != null) {
                            Message origMessage = API.getMessageById(origMessageId, channel).join();
                            EmbedBuilder embed = new EmbedBuilder()
                                    .setTitle("**Secret Santa Group**")
                                    .setDescription("Click the buttons below to interact with this group!\n## Group Name: " + groupName + "\n- Member Count: `" + userSet.size() + "`\n- Minimum Gift Value: " + minimum + "\n- Maximum Gift Value: " + maximum + "\n- Host: " + API.getUserById(document.getLong("host")).join().getMentionTag())
                                    .setColor(Color.CYAN)
                                    .addField("Made by ", SecretSantaBot.getOwnerTag())
                                    .setTimestampToNow();
                            origMessage.createUpdater().setContent("").setEmbed(embed).applyChanges().join();
                        }
                        responseUpdater.setContent("You left the Secret Santa group!").update();
                    } else {
                        responseUpdater.setContent("You are not part of this group").update();
                    }
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
                    API.getUserById(foundMember.getGiftReceiver().getUserID()).whenComplete((receiver, error) -> {
                        if (error != null) {
                            System.out.println("There was an error getting a user by ID");
                            responseUpdater.setContent("There was an error finding your receiver").update();
                            return;
                        }
                        MongoCollection<Document> userDocs = USER_DATABASE.getCollection(s.getIdAsString());
                        Document userDoc = userDocs.find(new Document("memberid", receiver.getId())).first();
                        String response = "Your gift receiver is " + receiver.getMentionTag() + "\n";
                        if (userDoc != null) {
                            response += "Their Wishlist: \n```" + userDoc.getString("wishlist") + "```\nTheir instructions/info: \n```" + userDoc.getString("extrainfo") + " ```";
                        } else {
                            response += "They have not made a wishlist yet";
                        }
                        responseUpdater.setContent(response).update();
                    });
                }
                case "startgroup" -> {
                    if (userID != document.getLong("host")) {
                        responseUpdater.setContent("You are not the host of this group!").update();
                        return;
                    }
                    if (document.getBoolean("started")) {
                        responseUpdater.setContent("You have already started this group! If there are issues with member assignments, please remake the group with </hostgroup:1052739101826228265>").update();
                        return;
                    }
                    if (!startGroup(userSet)) {
                        responseUpdater.setContent("Could not start this group!").update();
                        return;
                    }
                    if (channel != null) {
                        Message origMessage = API.getMessageById(origMessageId, channel).join();
                        String min = document.getString("min"), max = document.getString("max");
                        EmbedBuilder embed = new EmbedBuilder()
                                .setTitle("**Secret Santa Group**")
                                .setDescription("Group has **STARTED**!\nClick the buttons below to interact with this group!\n## Group Name: " + groupName + "\n- Member Count: " + userSet.size() + "\n- Minimum Gift Value: " + min + "\n- Maximum Gift Value: " + max + "\n- Host: " + API.getUserById(document.getLong("host")).join().getMentionTag())
                                .setColor(Color.GREEN)
                                .addField("Made by ", SecretSantaBot.getOwnerTag())
                                .setTimestampToNow();
                        origMessage.createUpdater().setContent("").setEmbed(embed).addComponents(
                                ActionRow.of(
                                        Button.secondary("viewreceiver", "View Your Person"),
                                        Button.secondary("editlist", "Edit Your Wishlist")
                                ),
                                ActionRow.of(Button.danger("endgroup", "Fully End the Group"))
                        ).applyChanges().join();
                        String mention = "";
                        for (GroupMember gm : userSet) {
                            mention += API.getUserById(gm.getUserID()).join().getMentionTag() + " ";
                        }
                        mention = "||" + mention +"||\n## " + groupName + " has started!\n" + origMessage.getLink();
                        channel.sendMessage(mention).join();
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
                        if (channel != null) {
                            Message origMessage = API.getMessageById(origMessageId, channel).join();
                            EmbedBuilder embed = new EmbedBuilder()
                                    .setTitle("**Secret Santa Group**")
                                    .setDescription("Group has **ENDED**!\n## Group Name: " + groupName)
                                    .setColor(Color.RED)
                                    .addField("Made by ", SecretSantaBot.getOwnerTag())
                                .setTimestampToNow();
                            origMessage.createUpdater().setContent("").setEmbed(embed).removeAllComponents().applyChanges().join();
                        }
                        responseUpdater.setContent("Ended and removed this group").update();
                        documents.deleteOne(new Document("message",  messageID));
                    } else {
                        responseUpdater.setContent("CAREFUL! This will completely end and remove this group. No other actions will work! Click again within 10 seconds to confirm").update();
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
        }, () -> interaction.respondLater(true).join().setContent("You must use this feature in a server").update());

    }

    private static boolean addUser(Set<GroupMember> group, GroupMember member) {
        return group.add(member);
    }

    private static boolean removeUser(Set<GroupMember> group, GroupMember member) {
        return group.remove(member);
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
        List<GroupMember> members = new ArrayList<>(group);
        // Keep track of which members have been assigned as receivers
        List<GroupMember> receiverAssignments = new ArrayList<>();

        for (GroupMember currentMember : members) {
            // Create a list of members that can be assigned as receivers for the current member
            List<GroupMember> potentialReceivers = members.stream()
                    .filter(member -> member.getUserID() != currentMember.getUserID()) // Exclude current member
                    .filter(member -> !receiverAssignments.contains(member)).toList();

            if (potentialReceivers.isEmpty()) {
                // No remaining members to assign as receivers, so we cannot continue
                return false;
            }

            // Select a random receiver from the potential receivers
            int receiverIndex = rand.nextInt(potentialReceivers.size());
            GroupMember receiver = potentialReceivers.get(receiverIndex);
            // Assign the receiver to the current member
            currentMember.setGiftReceiver(receiver.getUserID());
            // Add the receiver to the list of receiver assignments
            receiverAssignments.add(receiver);
        }

        return true;
    }

    private void sendModal(MessageComponentInteraction interaction) {
        MongoCollection<Document> userDocs = USER_DATABASE.getCollection(interaction.getServer().get().getIdAsString());
        Document userDoc = userDocs.find(new Document("memberid", interaction.getUser().getId())).first();
        String wishList = "";
        String extraInfo = "";
        if (userDoc != null) {
            wishList = userDoc.getString("wishlist");
            extraInfo =  userDoc.getString("extrainfo");
        }
        interaction.respondWithModal("wishlist", "Tell us what you wish for!",
                ActionRow.of(TextInput.create(TextInputStyle.PARAGRAPH, "itemlist", "Wishlist link or individual items/links here", wishList, wishList, true)),
                ActionRow.of(TextInput.create(TextInputStyle.PARAGRAPH, "extrainfo", "Enter any extra info or instructions here", extraInfo, extraInfo))
        ).exceptionally(throwable -> {
            System.out.println(throwable.getMessage());
            return null;
        });
    }

}