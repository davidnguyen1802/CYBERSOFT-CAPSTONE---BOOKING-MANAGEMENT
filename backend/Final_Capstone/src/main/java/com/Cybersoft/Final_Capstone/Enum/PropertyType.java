package com.Cybersoft.Final_Capstone.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PropertyType {
    APARTMENT, HOUSE, HOTEL, VILLA;

    @JsonCreator
    public static PropertyType fromValue(int value) {
        for (PropertyType type : PropertyType.values()) {
            if (type.ordinal() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid PropertyType value: " + value);
    }

    @JsonValue
    public int getValue() {
        return this.ordinal();
    }
}