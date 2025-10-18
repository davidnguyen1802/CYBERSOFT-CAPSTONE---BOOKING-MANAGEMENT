package com.Cybersoft.Final_Capstone.payload.request;

import lombok.Data;

import java.util.List;

@Data
public class AmenityRequest {
    private List<Integer> ids;
    private int idProperty;
}
