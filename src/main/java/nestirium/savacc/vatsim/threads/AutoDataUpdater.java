package nestirium.savacc.vatsim.threads;

import nestirium.savacc.mysql.DatabaseClient;
import nestirium.savacc.exceptions.ApiException;
import nestirium.savacc.vatsim.VatsimApiClient;
import nestirium.savacc.vatsim.schemas.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Every 24 hours, a new API call to vatsim database is made to request for any changes in the pilot and controller ratings, and the subdivision roster.
 * Because the pilot ratings and controller ratings in the vatsim database are presented in the v3 live feed in form of a JSON array, which suggests that the data
 * is subject to change in the future. And I had to program this for future proofing.
 * The requested data is stored in 2 MySQL tables, 1 for pilot ratings, and 1 for controller ratings. With an additional initially null column that stores the
 * ID of the discord role that will be associated with this rating and assigned to the discord user.
 */
public class AutoDataUpdater {

    private static final Logger log = LoggerFactory.getLogger(AutoDataUpdater.class);
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private final DatabaseClient dbClient;
    private final Map<String, String> config;

    public AutoDataUpdater(DatabaseClient dbClient, Map<String, String> config) {
        this.dbClient = dbClient;
        this.config = config;
    }

    public void startService() {
        executorService.scheduleAtFixedRate(() -> {
            log.info("AutoDataUpdater service tick.");
            try {
                DataEndpoints de = VatsimApiClient.requestDataEndpoints();
                List<ControllerRating> cro = VatsimApiClient.requestControllerRatings(de);
                List<PilotRating> pro = VatsimApiClient.requestPilotRatings(de);
                List<Facility> facilities = VatsimApiClient.requestFacilities(de);
                dbClient.updateControllerRatings(cro);
                dbClient.updatePilotRatings(pro);
                dbClient.updateFacilities(facilities);
                List<VatsimMember> mo = VatsimApiClient.requestSubdivisionRoster("SAU", config.get("vatsim_api_key"));
                dbClient.updateMembers(mo);
            } catch (ApiException e) {
                log.error(e.getMessage(), e);
            }
        }, 0, 1, TimeUnit.DAYS);
        log.info("Successfully started the AutoDataUpdater service.");
    }

    public void stopService() {
        try {
            if (executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                log.info("Successfully stopped the AutoDataUpdater service.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.error("Failed to properly stop the AutoDataUpdater service.");
    }


}
