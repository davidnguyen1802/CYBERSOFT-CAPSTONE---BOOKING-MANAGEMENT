package com.Cybersoft.Final_Capstone.payload.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BaseResponse {
    private int code;
    private String message;
    private Object data;
}
