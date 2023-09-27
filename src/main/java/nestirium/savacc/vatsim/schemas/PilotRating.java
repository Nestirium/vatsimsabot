package nestirium.savacc.vatsim.schemas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PilotRating(
        @JsonProperty("id") String id,
        @JsonProperty("short_name") String shortName,
        @JsonProperty("long_name") String longName
) {}