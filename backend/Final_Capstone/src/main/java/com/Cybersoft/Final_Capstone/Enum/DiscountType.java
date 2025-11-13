package com.Cybersoft.Final_Capstone.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DiscountType {
    PERCENTAGE(0), AMOUNT(1);
    private final int value;

    DiscountType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static DiscountType fromValue(int value) {
        for (DiscountType type : DiscountType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid DiscountType value: " + value);
    }
}
