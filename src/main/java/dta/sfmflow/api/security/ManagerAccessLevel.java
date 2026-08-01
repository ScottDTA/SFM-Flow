package dta.sfmflow.api.security;

import net.minecraft.util.StringRepresentable;

public enum ManagerAccessLevel implements StringRepresentable {
    OWNER("owner"),
    TEAM("team"),
    PUBLIC("public");

    private final String name;

    ManagerAccessLevel(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}