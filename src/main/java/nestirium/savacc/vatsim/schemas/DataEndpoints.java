package nestirium.savacc.vatsim.schemas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DataEndpoints(
        @JsonProperty("data") Data data,
        @JsonProperty("user") String[] user,
        @JsonProperty("metar") String[] metar
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("v3") String[] v3,
            @JsonProperty("transceivers") String[] transceivers,
            @JsonProperty("servers") String[] servers,
            @JsonProperty("servers_sweatbox") String[] sweatboxServers,
            @JsonProperty("servers_all") String[] allServers
    ) {}

}
