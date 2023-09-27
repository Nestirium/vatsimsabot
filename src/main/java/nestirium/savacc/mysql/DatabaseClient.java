package nestirium.savacc.mysql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import nestirium.savacc.Core;
import nestirium.savacc.vatsim.schemas.ControllerRating;
import nestirium.savacc.vatsim.schemas.Facility;
import nestirium.savacc.vatsim.schemas.PilotRating;
import nestirium.savacc.vatsim.schemas.VatsimMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseClient {

    private static final ObjectMapper om = Core.objectMapper();
    private static final Logger log = LoggerFactory.getLogger(DatabaseClient.class);

    private HikariDataSource db;

    public DatabaseClient() {
        try {
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.json");
            JsonNode configNode = om.readValue(inputStream, JsonNode.class);
            JsonNode sqlNode = configNode.get("mysql");
            String host = sqlNode.get("host").asText();
            String port = sqlNode.get("port").asText();
            String schema = sqlNode.get("schema").asText();
            HikariConfig hc = new HikariConfig();
            hc.setUsername(sqlNode.get("username").asText());
            hc.setPassword(sqlNode.get("password").asText());
            hc.setSchema(schema);
            hc.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC", host, port, schema));
            hc.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hc.setMinimumIdle(3);
            hc.setMaximumPoolSize(6);
            db = new HikariDataSource(hc);
            log.info("Initialized DatabaseClient.");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public Map<String, String> fetchConfiguration() {
        String select = """
                SELECT * FROM config
                """;
        try (Connection connection = db.getConnection();
        PreparedStatement stmt = connection.prepareStatement(select)) {
            ResultSet set = stmt.executeQuery();
            Map<String, String> config = new HashMap<>();
            while (set.next()) {
                String key = set.getString(1);
                String value = set.getString(2);
                config.put(key, value);
            }
            log.info("Database call for GET configuration values complete.");
            return config;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return new HashMap<>();
    }

    public ControllerRating fetchControllerRating(String ratingId) {
        String select = """
                SELECT id, `short`, `long` FROM c_ratings WHERE id=?
                """;
        try (Connection connection = db.getConnection();
        PreparedStatement stmt = connection.prepareStatement(select)) {
            stmt.setString(1, ratingId);
            ResultSet set = stmt.executeQuery();
            if (set.next()) {
                return new ControllerRating(
                        set.getString(1),
                        set.getString(2),
                        set.getString(3)
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateControllerRatings(List<ControllerRating> controllerRatings) {
        String updateInsert = """
                INSERT INTO c_ratings (id, `short`, `long`) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE `short`=VALUES(`short`), `long`=VALUES(`long`)
                """;
        try (Connection connection = db.getConnection();
        PreparedStatement stmt = connection.prepareStatement(updateInsert)) {
            for (ControllerRating controllerRating : controllerRatings) {
                stmt.setString(1, controllerRating.id());
                stmt.setString(2, controllerRating.shortName());
                stmt.setString(3, controllerRating.longName());
                stmt.executeUpdate();
            }
            log.info("Database call for POST controller ratings complete.");
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String fetchRoleIdForCRating(String ratingId) {
        String select = """
                SELECT discord_role_id FROM c_ratings WHERE id=?
                """;
        try (Connection connection = db.getConnection();
             PreparedStatement stmt = connection.prepareStatement(select)) {
            stmt.setString(1, ratingId);
            ResultSet set = stmt.executeQuery();
            if (set.next()) {
                return set.getString(1);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public List<String> fetchCRatingRoleIds() {
        String select = """
                SELECT discord_role_id FROM c_ratings
                """;
        try (Connection connection = db.getConnection();
             PreparedStatement stmt = connection.prepareStatement(select)) {
            ResultSet set = stmt.executeQuery();
            List<String> roleIds = new ArrayList<>();
            while (set.next()) {
                roleIds.add(set.getString(1));
            }
            return roleIds;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    public void updatePilotRatings(List<PilotRating> pilotRatings) {
        log.info("Database call for updating the pilot ratings.");
        String updateInsert = """
                INSERT INTO p_ratings (id, short_name, long_name) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                short_name=VALUES(short_name),
                long_name=VALUES(long_name)
                """;
        try (Connection connection = db.getConnection();
             PreparedStatement stmt = connection.prepareStatement(updateInsert)) {
            for (PilotRating pilotRating : pilotRatings) {
                stmt.setString(1, pilotRating.id());
                stmt.setString(2, pilotRating.shortName());
                stmt.setString(3, pilotRating.longName());
                stmt.executeUpdate();
            }
            log.info("Database call complete.");
        } catch (SQLException e) {
            log.error("Database call failed.");
            e.printStackTrace();
        }
    }

    public void updateFacilities(List<Facility> facilities) {
        log.info("Database call for updating the facilities.");
        String updateInsert = """
                INSERT INTO facilities (id, `short`, `long`) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                `short`=VALUES(`short`),
                `long`=VALUES(`long`)
                """;
        try (Connection connection = db.getConnection();
             PreparedStatement stmt = connection.prepareStatement(updateInsert)) {
            for (Facility facility : facilities) {
                stmt.setString(1, facility.id());
                stmt.setString(2, facility.shortName());
                stmt.setString(3, facility.longName());
                stmt.executeUpdate();
            }
            log.info("Database call complete.");
        } catch (SQLException e) {
            log.error("Database call failed.", e);
        }
    }

    public Facility fetchFacility(String facilityId) {
        String select = """
                SELECT * FROM facilities WHERE id=?
                """;
        try (Connection connection = db.getConnection();
             PreparedStatement stmt = connection.prepareStatement(select)) {
            stmt.setString(1, facilityId);
            ResultSet set = stmt.executeQuery();
            if (set.next()) {
                return new Facility(
                        set.getString(1),
                        set.getString(2),
                        set.getString(3)
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public PilotRating fetchPilotRating(String ratingId) {
        String select = """
                SELECT id, `short_name`, `long_name` FROM p_ratings WHERE id=?
                """;
        try (Connection connection = db.getConnection();
             PreparedStatement stmt = connection.prepareStatement(select)) {
            stmt.setString(1, ratingId);
            ResultSet set = stmt.executeQuery();
            if (set.next()) {
                return new PilotRating(
                        set.getString(1),
                        set.getString(2),
                        set.getString(3)
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean memberExists(String cid) {
        String select = """
                SELECT cid FROM members WHERE cid=?
                """;
        try (Connection connection = db.getConnection();
        PreparedStatement stmt = connection.prepareStatement(select)) {
            stmt.setString(1, cid);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    public void updateMembers(List<VatsimMember> vatsimMembers) {
        String updateInsert = """
                    INSERT INTO members
                    (cid, name_first, name_last, email, rating, pilotrating, susp_date, reg_date, region_id, division_id,
                    subdivision_id, lastratingchange)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE
                    name_first=VALUES(name_first),
                    name_last=VALUES(name_last),
                    email=VALUES(email),
                    rating=VALUES(rating),
                    pilotrating=VALUES(pilotrating),
                    susp_date=VALUES(susp_date),
                    reg_date=VALUES(reg_date),
                    region_id=VALUES(region_id),
                    division_id=VALUES(division_id),
                    subdivision_id=VALUES(subdivision_id),
                    lastratingchange=VALUES(lastratingchange)
                    """;
        try (Connection connection = db.getConnection();
        PreparedStatement stmt = connection.prepareStatement(updateInsert)) {
            for (VatsimMember vatsimMember : vatsimMembers) {
                stmt.setString(1, vatsimMember.cid());
                stmt.setString(2, vatsimMember.firstName());
                stmt.setString(3, vatsimMember.lastName());
                stmt.setString(4, vatsimMember.email());
                stmt.setString(5, vatsimMember.rating());
                stmt.setString(6, vatsimMember.pilotRating());
                stmt.setString(7, vatsimMember.suspensionDate());
                stmt.setString(8, vatsimMember.registrationDate());
                stmt.setString(9, vatsimMember.regionId());
                stmt.setString(10, vatsimMember.divisionId());
                stmt.setString(11, vatsimMember.subdivisionId());
                stmt.setString(12, vatsimMember.lastRatingChange());
                stmt.executeUpdate();
            }
            log.info("Database call for POST members data completed.");
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }



}
