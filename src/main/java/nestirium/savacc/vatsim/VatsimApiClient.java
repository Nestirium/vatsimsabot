package nestirium.savacc.vatsim;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nestirium.savacc.Core;
import nestirium.savacc.exceptions.ApiException;
import nestirium.savacc.exceptions.AuthException;
import nestirium.savacc.exceptions.NotFoundException;
import nestirium.savacc.vatsim.schemas.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VatsimApiClient {

    private VatsimApiClient() {}
    private static final OkHttpClient http =
            new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

    private static final ObjectMapper om = Core.objectMapper();
    private static final Logger log = LoggerFactory.getLogger(VatsimApiClient.class);

    private static final String SUBDIV_ROSTER_URL = "https://api.vatsim.net/v2/orgs/subdivision/";
    private static final String DISCORD_ID_URL = "https://api.vatsim.net/v2/members/discord/";
    private static final String MEMBER_URL = "https://api.vatsim.net/v2/members/";
    private static final String DATA_URLS = "https://status.vatsim.net/status.json";

    public static String requestCIDByDiscord(String discordId) throws ApiException {
        Request request = new Request.Builder()
                .url(DISCORD_ID_URL + discordId)
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                JsonNode root = om.readValue(responseBody.string(), JsonNode.class);
                String cid = root.get("user_id").asText();
                log.trace("API call for vatsim ID using discord account ID returned: " + cid);
                return cid;
            } else if (response.code() == 404) {
                throw new NotFoundException("No vatsim ID is associated with the requested discord ID.");
            }
            throw new ApiException("Unexpected error while requesting vatsim ID by discord ID.");
        } catch (IOException e) {
            throw new ApiException("Unable to parse JSON data of vatsim ID requested by discord ID.", e);
        }
    }

    public static List<VatsimMember> requestSubdivisionRoster(String subdivision, String apiKey) throws ApiException {
        Request request = new Request.Builder()
                .url(SUBDIV_ROSTER_URL + subdivision.toUpperCase())
                .addHeader("Authorization", "Token " + apiKey)
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                JsonNode root = om.readValue(responseBody.string(), JsonNode.class);
                JsonNode items = root.get("items");
                String count = root.get("count").asText();
                List<VatsimMember> vatsimMembers = om.readValue(items.toString(), new TypeReference<>() {});
                log.trace("API call for subdivision roster returned " + count + " members.");
                return vatsimMembers;
            } else switch (response.code()) {
                case 404 -> throw new NotFoundException("Subdivision with the specified ID is not found.");
                case 403 -> throw new AuthException("Unauthorized request to subdivision roster.");
                default -> throw new ApiException("Error from server while requesting subdivision roster.");
            }
        } catch (IOException e) {
            throw new ApiException("Unable to parse JSON data of subdivision roster.", e);
        }
    }

    public static VatsimMember requestVatsimMember(String cid, String apiKey) throws ApiException {
        Request request = new Request.Builder()
                .url(MEMBER_URL + cid)
                .addHeader("Authorization", "Token " + apiKey)
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                JsonNode root = om.readValue(responseBody.string(), JsonNode.class);
                VatsimMember vatsimMember = om.readValue(root.toString(), VatsimMember.class);
                log.trace("API call for vatsim member data returned a schema.");
                return vatsimMember;
            } else if (response.code() == 404) {
                throw new NotFoundException("Vatsim member with requested CID is not found.");
            }
            throw new ApiException("Unexpected error while requesting vatsim member by CID.");
        } catch (IOException e) {
            throw new ApiException("Unable to parse JSON data of vatsim member schema.", e);
        }
    }

    public static DataEndpoints requestDataEndpoints() throws ApiException {
        Request request = new Request.Builder()
                .url(DATA_URLS)
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                DataEndpoints dataEndpoints = om.readValue(responseBody.string(), DataEndpoints.class);
                log.trace("API call for vatsim live data endpoints returned data.");
                return dataEndpoints;
            } else {
                throw new ApiException("Unexpected error while requesting live data endpoints from vatsim.");
            }
        } catch (IOException e) {
            throw new ApiException("Unable to parse JSON data of vatsim live data endpoints.", e);
        }
    }

    public static List<ControllerRating> requestControllerRatings(DataEndpoints dataEndpoints) throws ApiException {
        Request request = new Request.Builder()
                .url(dataEndpoints.data().v3()[0])
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                JsonNode root = om.readValue(responseBody.string(), JsonNode.class);
                JsonNode controllerRatingsNode = root.get("ratings");
                List<ControllerRating> controllerRatings = om.readValue(controllerRatingsNode.toString(), new TypeReference<>() {});
                log.trace("API call for controller ratings returned data.");
                return controllerRatings;
            } else {
                throw new ApiException("Unexpected error while requesting controller ratings from vatsim.");
            }
        } catch (IOException e) {
            throw new ApiException("Unable to parse JSON data of vatsim controller ratings.", e);
        }
    }

    public static List<PilotRating> requestPilotRatings(DataEndpoints dataEndpoints) throws ApiException {
        Request request = new Request.Builder()
                .url(dataEndpoints.data().v3()[0])
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                JsonNode root = om.readValue(responseBody.string(), JsonNode.class);
                JsonNode pilotRatingsNode = root.get("pilot_ratings");
                List<PilotRating> pilotRatings = om.readValue(pilotRatingsNode.toString(), new TypeReference<>() {});
                log.trace("API call for pilot ratings returned data.");
                return pilotRatings;
            } else {
                throw new ApiException("Unexpected error while requesting pilot ratings from vatsim.");
            }
        } catch (IOException e) {
            throw new ApiException("Unable to parse JSON data of vatsim pilot ratings.", e);
        }
    }

    public static List<Facility> requestFacilities(DataEndpoints dataEndpoints) throws ApiException {
        Request request = new Request.Builder()
                .url(dataEndpoints.data().v3()[0])
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                JsonNode root = om.readValue(responseBody.string(), JsonNode.class);
                JsonNode facilitiesNode = root.get("facilities");
                List<Facility> facilities = om.readValue(facilitiesNode.toString(), new TypeReference<>() {});
                log.trace("API call for vatsim facilities returned data.");
                return facilities;
            } else {
                throw new ApiException("Unexpected error while requesting facilities from vatsim.");
            }
        } catch (IOException e) {
            throw new ApiException("Unable to parse JSON data of vatsim facilities.", e);
        }
    }

}
