package nestirium.savacc;

import com.fasterxml.jackson.databind.ObjectMapper;
import nestirium.savacc.discord.*;
import nestirium.savacc.discord.schemas.BroadcastManager;
import nestirium.savacc.mysql.DatabaseClient;
import nestirium.savacc.vatsim.threads.AutoDataUpdater;
import nestirium.savacc.discord.commands.BroadcastCommand;
import nestirium.savacc.discord.commands.FetchCommand;
import nestirium.savacc.discord.commands.MetarCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Core {

    private static final Logger log = LoggerFactory.getLogger(Core.class);
    private static final ObjectMapper om = new ObjectMapper();

    private static final DatabaseClient dbClient = new DatabaseClient();
    private static final Map<String, String> config = dbClient.fetchConfiguration();
    private static final SlashCommandManager commandManager = new SlashCommandManager();

    private static final AutoDataUpdater autoDataUpdater = new AutoDataUpdater(dbClient, config);

    public static void main(String[] args) {
        autoDataUpdater.startService();
        initializeJDA();
    }

    private static void initializeJDA() {

        try {
            AccountSyncService accountSyncService = new AccountSyncService.
                    Builder()
                    .setDatabaseClient(dbClient)
                    .setConfig(config)
                    .build();
            BroadcastManager broadcastManager = new BroadcastManager();
            DiscordEventListener discordEventListener = new DiscordEventListener(commandManager, accountSyncService, broadcastManager);
            JDA jda = JDABuilder.createDefault(config.get("discord_token"))
                    .enableIntents(
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.GUILD_MESSAGES
                    )
                    .addEventListeners(discordEventListener)
                    .build()
                    .awaitReady();
            Guild guild = jda.getGuildById(config.get("guild_id"));


            commandManager.registerCommands(
                    new MetarCommand(config),
                    new BroadcastCommand(broadcastManager),
                    new FetchCommand(config, dbClient)
            );

            if (guild != null) {
                guild.updateCommands().addCommands(commandManager.getSlashCommandData()).queue();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static ObjectMapper objectMapper() {
        return om;
    }


}
