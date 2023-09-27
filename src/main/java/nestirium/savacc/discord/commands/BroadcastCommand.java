package nestirium.savacc.discord.commands;

import nestirium.savacc.discord.SlashCommand;
import nestirium.savacc.discord.schemas.Broadcast;
import nestirium.savacc.discord.schemas.BroadcastManager;
import nestirium.savacc.utils.Utils;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;

public class BroadcastCommand extends SlashCommand {

    private final BroadcastManager broadcastManager;

    public BroadcastCommand(BroadcastManager broadcastManager) {
        super(
                Commands.slash(
                        "broadcast", "Broadcasts a fancy message to the channel you like."
                ).addOptions(
                        new OptionData(OptionType.CHANNEL, "channel", "The target text channel.", true),
                        new OptionData(OptionType.BOOLEAN, "anonymous", "Cite the author of the broadcast. (TRUE by default)", false),
                        new OptionData(OptionType.STRING, "metadata", "Developers only.", false),
                        new OptionData(OptionType.STRING, "color", "The color of the embed.", false)
                )
        );
        this.broadcastManager = broadcastManager;
    }

    @Override
    public void handle(SlashCommandInteractionEvent e) {
        OptionMapping channelOption = e.getOption("channel");
        OptionMapping anonymousOption = e.getOption("anonymous");
        OptionMapping metaDataOption = e.getOption("metadata");
        OptionMapping colorOption = e.getOption("color");
        GuildChannel guildChannel = channelOption.getAsChannel();
        if (guildChannel.getType() != ChannelType.TEXT && guildChannel.getType() != ChannelType.NEWS) {
            e.reply("Channel must be a text or news channel.").setEphemeral(true).queue();
            return;
        }
        TextInput title = TextInput.create("subject", "Subject", TextInputStyle.SHORT)
                .setPlaceholder("The title of the announcement.")
                .setMaxLength(30)
                .setRequired(true)
                .build();
        TextInput body = TextInput.create("body", "Body", TextInputStyle.PARAGRAPH)
                .setPlaceholder("""
                        You can use @everyone, @here.
                        For mentioning users: <@!userID>
                        For mentioning roles: <@&roleID>
                        """)
                .setMaxLength(4000)
                .setRequired(true)
                .build();
        Modal modal = Modal.create("announcer", "Announcer")
                .addComponents(ActionRow.of(title), ActionRow.of(body))
                .build();

        String authorId = e.getMember().getId();
        String channelId = guildChannel.getId();
        boolean isAnonymous = anonymousOption == null || anonymousOption.getAsBoolean();

        try {

            String hexColor = colorOption == null ? Utils.colorToHex(Color.GREEN) : Utils.colorToHex(Color.decode(colorOption.getAsString()));

            Broadcast.Builder builder = new Broadcast.Builder()
                    .authorId(authorId)
                    .channelId(channelId)
                    .anonymous(isAnonymous)
                    .hexColor(hexColor);

            if (metaDataOption != null) {
                String meta = metaDataOption.getAsString();
                String[] metaData;
                if (meta.contains(",")) {
                    metaData = meta.split(",");
                } else {
                    metaData = new String[]{meta};
                }
                builder.metaData(metaData);
            } else {
                builder.metaData(new String[0]);
            }

            broadcastManager.storeBroadcast(e.getMember().getId(), builder);

            e.replyModal(modal).queue();

        } catch (NumberFormatException exception) {
            e.reply("Invalid hex code provided.").setEphemeral(true).queue();
        }

    }

}


