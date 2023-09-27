package nestirium.savacc.discord;

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.*;
import java.util.stream.Collectors;

public class SlashCommandManager {

    private final Map<String, SlashCommand> commands = new HashMap<>();

    public boolean hasCommand(String name) {
        return commands.containsKey(name);
    }

    public SlashCommand getCommand(String name) {
        return commands.get(name);
    }

    public Collection<SlashCommandData> getSlashCommandData() {
        return commands.values().stream().map(SlashCommand::getSlashCommandData).collect(Collectors.toList());
    }

    public void registerCommands(SlashCommand... slashCommands) {
        for (SlashCommand slashCommand : slashCommands) {
            String name = slashCommand.getSlashCommandData().getName();
            if (hasCommand(name)) {
                continue;
            }
            commands.put(name, slashCommand);
        }
    }

}
