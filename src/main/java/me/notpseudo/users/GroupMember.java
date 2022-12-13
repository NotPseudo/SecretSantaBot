package me.notpseudo.users;

import org.javacord.api.entity.user.User;

import java.util.Optional;

public class GroupMember {

    private final long USERID;
    private GroupMember giftReceiver;

    public GroupMember(long userID) {
        USERID = userID;
        giftReceiver = null;
    }

    public long getUserID() {
        return USERID;
    }

    public GroupMember getGiftReceiver() {
        return giftReceiver;
    }

    public void setGiftReceiver(User user) {
        giftReceiver = new GroupMember(user.getId());
    }

    public void setGiftReceiver(long receiverID) {
        giftReceiver = new GroupMember(receiverID);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof GroupMember groupMember)) {
            return false;
        }
        return USERID == groupMember.USERID;
    }

    @Override
    public String toString() {
        return "GroupMember [ USERID: " + USERID + ", giftReceiver: " + giftReceiver + " ]";
    }
}