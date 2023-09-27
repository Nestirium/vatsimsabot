package nestirium.savacc.checkwx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nestirium.savacc.Core;
import nestirium.savacc.exceptions.AuthException;
import nestirium.savacc.exceptions.ApiException;
import nestirium.savacc.exceptions.NotFoundException;
import nestirium.savacc.exceptions.RateLimitException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WxApiClient {

    private WxApiClient() {}
    private static final OkHttpClient http =
            new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

    private static final ObjectMapper om = Core.objectMapper();
    private static final Logger log = LoggerFactory.getLogger(WxApiClient.class);

    private static final String METAR_URL = "https://api.checkwx.com/metar/";

    public static String[] requestMetar(String apiKey, String... icao) throws ApiException {
        if (icao.length > 20) {
            throw new ApiException("Maximum of 20 stations per request.");
        }
        Request request = new Request.Builder()
                .url(METAR_URL + String.join(",", icao))
                .addHeader("x-api-key", apiKey)
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                JsonNode root = om.readValue(responseBody.string(), JsonNode.class);
                log.trace("API call for metar returned data.");
                return om.readValue(root.get("data").toString(), String[].class);
            } else switch (response.code()) {
                case 401 -> throw new AuthException("Unauthorized request for metar data.");
                case 404 -> throw new NotFoundException("Metar for specified station not found.");
                case 429 -> throw new RateLimitException("Too many requests for metar data..");
                case 422 -> throw new ApiException("Validation error while requesting metar data.");
                default -> throw new ApiException("Error from server while requesting metar data.");
            }
        } catch (IOException e) {
            throw new ApiException("Unable to parse JSON data of metar.", e);
        }
    }


}
