package com.viettel.demo1.payload;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.time.ZoneId;
@Setter
@Getter
public class EmailRequest {
    @Email
    @NotEmpty
    private String email;

    @NotEmpty
    private String subject;

    @NotEmpty
    private String body;

    @NotEmpty
    private LocalDateTime dateTime;

    @NotEmpty
    private ZoneId timeZone;


}
