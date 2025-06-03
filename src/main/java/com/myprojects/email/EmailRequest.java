package com.myprojects.email;

import lombok.Data;

@Data
public class EmailRequest {
    private String emailContent;
    private String tone;

}
