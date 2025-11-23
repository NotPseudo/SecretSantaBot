package me.notpseudo.users;

public class GroupMember {

    private final long USERID;
    private GroupMember giftReceiver;
    private String wishlist;
    private String extra;

    public GroupMember(long userID) {
        USERID = userID;
        giftReceiver = null;
        wishlist = null;
        extra = null;
    }

    public long getUserID() {
        return USERID;
    }

    public GroupMember getGiftReceiver() {
        return giftReceiver;
    }

    public void setGiftReceiver(long receiverID) {
        giftReceiver = new GroupMember(receiverID);
    }

    public String getWishlist() {
        return wishlist;
    }

    public void setWishlist(String wishlist) {
        this.wishlist = wishlist;
    }

    public String getExtraInfo() {
        return extra;
    }

    public void setExtraInfo(String extra) {
        this.extra = extra;
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
        return "GroupMember [ USERID: " + USERID + ", giftReceiver: " + giftReceiver + ", wishlist: " + wishlist + ", extra: " + extra + " ]";
    }

    @Override
    public int hashCode() {
        return Long.hashCode(USERID);
    }

}