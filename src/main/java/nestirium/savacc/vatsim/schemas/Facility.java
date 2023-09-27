package nestirium.savacc.vatsim.schemas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Facility(
        @JsonProperty("id") String id,
        @JsonProperty("short") String shortName,
        @JsonProperty("long") String longName
) {}
