package pl.rodzon.callservice.model;

import lombok.Data;

@Data
public class CallingInfo {
    private String callingInformation;
    private String username;
    private String roomId;
    private String callProvider;
}
