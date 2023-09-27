package nestirium.savacc.vatsim.schemas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VatsimMember(
        @JsonProperty("id") String cid,
        @JsonProperty("name_first") String firstName,
        @JsonProperty("name_last") String lastName,
        @JsonProperty("email") String email,
        @JsonProperty("rating") String rating,
        @JsonProperty("pilotrating") String pilotRating,
        @JsonProperty("susp_date") String suspensionDate,
        @JsonProperty("reg_date") String registrationDate,
        @JsonProperty("region_id") String regionId,
        @JsonProperty("division_id") String divisionId,
        @JsonProperty("subdivision_id") String subdivisionId,
        @JsonProperty("lastratingchange") String lastRatingChange
) {}

