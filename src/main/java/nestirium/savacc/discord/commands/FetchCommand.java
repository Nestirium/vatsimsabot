package nestirium.savacc.discord.commands;

import nestirium.savacc.mysql.DatabaseClient;
import nestirium.savacc.exceptions.ApiException;
import nestirium.savacc.discord.EmbedConstructor;
import nestirium.savacc.discord.SlashCommand;
import nestirium.savacc.vatsim.VatsimApiClient;
import nestirium.savacc.vatsim.schemas.ControllerRating;
import nestirium.savacc.vatsim.schemas.PilotRating;
import nestirium.savacc.vatsim.schemas.VatsimMember;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

public class FetchCommand extends SlashCommand {

    private final Map<String, String> config;
    private final DatabaseClient dbClient;
    private final EmbedConstructor ec = new EmbedConstructor();

    public FetchCommand(Map<String, String> config, DatabaseClient dbClient) {
        super(
                Commands.slash("fetch", "Fetch user information from the VATSIM database.")
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "cid", "Vatsim ID of the user.", true)
                        )
        );
        this.config = config;
        this.dbClient = dbClient;
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String cidInput = Objects.requireNonNull(event.getOption("cid")).getAsString();
        try {
            VatsimMember vatsimMember = VatsimApiClient.requestVatsimMember(cidInput, config.get("vatsim_api_key"));
            EmbedConstructor normalConstructor = ec.asSuccess();
            normalConstructor.addField(
                    ":identification_card: Name",
                    String.format(
                            "%s %s",
                            vatsimMember.firstName() == null ? "||:no_entry_sign:||" : "`"+vatsimMember.firstName()+"`",
                            vatsimMember.lastName() == null ? "||:no_entry_sign:||" : "`"+vatsimMember.lastName()+"`"
                            ),
                    false
            );
            normalConstructor.addField(":identification_card: CID", "`"+vatsimMember.cid()+"`", false);
            normalConstructor.addField(
                    ":identification_card: Email",
                    vatsimMember.email() == null ? "||:no_entry_sign:||" : "||"+vatsimMember.email()+"||",
                    false
            );
            ControllerRating cRating = dbClient.fetchControllerRating(vatsimMember.rating());
            normalConstructor.addField(
                    ":satellite: Controller Rating",
                    String.format(
                            "`%s (%s)`",
                            cRating.shortName(),
                            cRating.longName()
                    ),
                    false
            );
            PilotRating pRating = dbClient.fetchPilotRating(vatsimMember.pilotRating());
            normalConstructor.addField(
                    ":man_pilot: Pilot Rating",
                    String.format(
                            "`%s (%s)`",
                            pRating.shortName(),
                            pRating.longName()
                    ),
                    false
            );
            normalConstructor.addField(
                    ":date: Registration Date",
                    "`"+LocalDateTime.parse(vatsimMember.registrationDate()).format(EmbedConstructor.getDateTimeFormatter())+" UTC`",
                    false
            );
            normalConstructor.addField(
                    ":date: Last Rating Change",
                    vatsimMember.lastRatingChange() == null ? "||:no_entry_sign:||" :
                            "`"+LocalDateTime.parse(vatsimMember.lastRatingChange()).format(EmbedConstructor.getDateTimeFormatter())+" UTC`",
                    false
            );
            normalConstructor.addField(
                    ":date: Suspension Date",
                    vatsimMember.suspensionDate() == null ? "||:no_entry_sign:||" :
                            "`"+LocalDateTime.parse(vatsimMember.suspensionDate()).format(EmbedConstructor.getDateTimeFormatter())+" UTC`",
                    false
            );
            normalConstructor.addField(
                    "Region",
                    "`"+vatsimMember.regionId()+"`",
                    false
            );
            normalConstructor.addField(
                    "Division",
                    "`"+vatsimMember.divisionId()+"`",
                    false
            );
            normalConstructor.addField(
                    "Sub-Division",
                    vatsimMember.subdivisionId() == null ? "||:no_entry_sign:||" : "`"+vatsimMember.subdivisionId()+"`",
                    false
            );
            event.getHook().sendMessageEmbeds(normalConstructor.build()).queue();
            normalConstructor.clear();
        } catch (ApiException e) {
            event.getHook().sendMessageEmbeds(ec.asError().describe(e.getMessage()).build()).queue();
            ec.clear();
        }
    }

}
