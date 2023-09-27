package nestirium.savacc.discord;

import nestirium.savacc.mysql.DatabaseClient;
import nestirium.savacc.exceptions.ApiException;
import nestirium.savacc.vatsim.VatsimApiClient;
import nestirium.savacc.vatsim.schemas.VatsimMember;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AccountSyncService {
    private static final Logger log = LoggerFactory.getLogger(AccountSyncService.class);
    private final Map<String, String> config;
    private final DatabaseClient dbClient;

    private AccountSyncService(Builder builder) {
        this.config = builder.config;
        this.dbClient = builder.dbClient;
    }

    private String requestCIDByDiscord(Member discordMember) throws ApiException {
        return VatsimApiClient.requestCIDByDiscord(discordMember.getId());
    }

    private VatsimMember requestMemberData(String cid) throws ApiException {
        return VatsimApiClient.requestVatsimMember(cid, config.get("vatsim_api_key"));
    }

    private void assignVatsimMemberRole(Guild guild, List<Role> rolesToUpdate) {
        Role vatsimMemberRole = guild.getRoleById(config.get("vatsim_member_role_id"));
        if (vatsimMemberRole == null) {
            log.error("VATSIM Member Role is null during assignVatsimMemberRole.");
            return;
        }
        rolesToUpdate.add(vatsimMemberRole);
    }

    private Role getRole(String roleId, List<Role> roles) {
        return roles.stream().filter(r -> r.getId().equals(roleId)).findFirst().orElse(null);
    }

    private void assignResidentRole(Guild guild, String cid, List<Role> rolesToUpdate) {
        if (dbClient.memberExists(cid)) {
            Role residentRole = guild.getRoleById(config.get("resident_role_id"));
            if (residentRole == null) {
                log.error("Resident role is null during assignResidentRole.");
                return;
            }
            rolesToUpdate.add(residentRole);
        } else {
            Role residentRole = getRole(config.get("resident_role_id"), rolesToUpdate);
            if (residentRole == null) {
                return;
            }
            rolesToUpdate.remove(residentRole);
            log.info("User is no longer a resident, removed the resident role.");
        }
    }

    private void assignControllerRatings(Guild guild, VatsimMember vatsimMember, List<Role> rolesToUpdate) {
        String roleId = dbClient.fetchRoleIdForCRating(vatsimMember.rating());
        Role cRatingRole = guild.getRoleById(roleId);
        if (cRatingRole == null) {
            log.error("Controller rating role was null during assignControllerRatings.");
            return;
        }
        List<String> cRatingRoleIds = dbClient.fetchCRatingRoleIds();
        rolesToUpdate.removeIf(role -> cRatingRoleIds.contains(role.getId()));
        rolesToUpdate.add(cRatingRole);
    }

    public void performAccountSync(JDA jda, Member discordMember) throws ApiException {
        Guild guild = jda.getGuildById(config.get("guild_id"));
        if (guild == null) {
            log.error("Guild is null during the account sync function.");
            return;
        }
        String cid = requestCIDByDiscord(discordMember);
        VatsimMember vatsimMember  = requestMemberData(cid);
        List<Role> rolesToUpdate = new ArrayList<>(discordMember.getRoles());
        assignVatsimMemberRole(guild, rolesToUpdate);
        assignResidentRole(guild, cid, rolesToUpdate);
        assignControllerRatings(guild, vatsimMember, rolesToUpdate);
        guild.modifyMemberRoles(discordMember, rolesToUpdate).queue();
        rolesToUpdate.clear();
        log.info(String.format("""
                
                ACCOUNT SYNC:
                --------------
                Discord Username: %s
                Discord ID: %s
                Vatsim ID: %s
               
                """, discordMember.getEffectiveName(),
                discordMember.getId(),
                vatsimMember.cid()));
    }

    public static class Builder {

        private Map<String, String> config;
        private DatabaseClient dbClient;

        public Builder setConfig(Map<String, String> config) {
            this.config = config;
            return this;
        }

        public Builder setDatabaseClient(DatabaseClient dbClient) {
            this.dbClient = dbClient;
            return this;
        }

        public AccountSyncService build() {
            return new AccountSyncService(this);
        }

    }

}
