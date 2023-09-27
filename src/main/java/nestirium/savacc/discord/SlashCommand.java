package nestirium.savacc.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public abstract class SlashCommand {

    private final SlashCommandData slashCommandData;

    public SlashCommand(SlashCommandData slashCommandData) {
        this.slashCommandData = slashCommandData;
    }

    public SlashCommandData getSlashCommandData() {
        return slashCommandData;
    }

    public abstract void handle(SlashCommandInteractionEvent event);

}
