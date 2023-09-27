package nestirium.savacc.discord.commands;

import nestirium.savacc.exceptions.ApiException;
import nestirium.savacc.discord.EmbedConstructor;
import nestirium.savacc.discord.SlashCommand;
import nestirium.savacc.checkwx.WxApiClient;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Map;

public class MetarCommand extends SlashCommand {

    private Map<String, String> config;
    private final EmbedConstructor eb = new EmbedConstructor();

    public MetarCommand(Map<String, String> config) {
        super(
                Commands.slash(
                        "metar", "Provides the METAR of the specified station identifier(s)."
                ).addOptions(
                        new OptionData(OptionType.STRING, "icao", "Multiples can be accepted e.g klax,OEJN,OeRk...", true)
                )
        );
        this.config = config;
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String[] input = event.getOption("icao").getAsString().split(",");
        try {
            String[] requestResults = WxApiClient.requestMetar(config.get("wx_api_key"), input);
            EmbedConstructor normalEb = eb.asSuccess();
            for (String requestResult : requestResults) {
                normalEb.appendDescription("`"+requestResult+"`")
                        .appendDescription("\n")
                        .appendDescription("\n");
            }
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            normalEb.clear();
        } catch (ApiException e) {
            event.getHook().sendMessageEmbeds(eb.asError().setDescription(e.getMessage()).build()).queue();
            eb.clear();
        }
    }

}
