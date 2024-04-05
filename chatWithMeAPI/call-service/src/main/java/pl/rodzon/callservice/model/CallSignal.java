package pl.rodzon.callservice.model;

import lombok.Data;

@Data
public class CallSignal {
    private String type;
    private String sessionDescription;
}
