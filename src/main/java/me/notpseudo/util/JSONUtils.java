package me.notpseudo.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.notpseudo.users.GroupMember;

public class JSONUtils {

    private static final Gson GSON;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();

        GSON = builder.create();
    }

    public static GroupMember getGroupMember(String jsonString) {
        return GSON.fromJson(jsonString, GroupMember.class);
    }

    public static String getJSONString(GroupMember member) {
        return GSON.toJson(member);
    }

}
