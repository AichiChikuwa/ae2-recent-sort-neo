package net.aichichikuwa.appliedhistory.block;

import net.minecraft.util.StringRepresentable;

// the three visual states of the me logger, driven by network status.
// off: not powered / no channel. on: powered, sole logger, recording. error: more than one logger on the network.
public enum MELoggerStatus implements StringRepresentable {
    OFF("off"),
    ON("on"),
    ERROR("error");

    private final String name;

    MELoggerStatus(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
