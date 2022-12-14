package me.notpseudo.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.notpseudo.users.GroupMember;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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

    public static Set<GroupMember> getMembers(ArrayList<String> userArray) {
        Set<GroupMember> userSet = new HashSet<>();
        for (String json : userArray) {
            userSet.add(getGroupMember(json));
        }
        return userSet;
    }

}
