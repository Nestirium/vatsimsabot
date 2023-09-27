package nestirium.savacc.discord;

import nestirium.savacc.discord.schemas.Broadcast;
import nestirium.savacc.discord.schemas.BroadcastManager;
import nestirium.savacc.exceptions.ApiException;
import nestirium.savacc.exceptions.NotFoundException;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DiscordEventListener extends ListenerAdapter {


    private static final Logger log = LoggerFactory.getLogger(DiscordEventListener.class);
    private final SlashCommandManager cm;
    private final AccountSyncService ass;
    private final Map<String, Long> cooldown = new HashMap<>();
    private final BroadcastManager broadcastManager;
    private final EmbedConstructor ec = new EmbedConstructor();

    public DiscordEventListener(SlashCommandManager cm,
                                AccountSyncService ass,
                                BroadcastManager broadcastManager) {
        this.cm = cm;
        this.ass = ass;
        this.broadcastManager = broadcastManager;
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent e) {
        try {
            ass.performAccountSync(e.getJDA(), e.getMember());
        } catch (NotFoundException ex) {
            log.error(ex.getMessage());
            e.getMember().kick().queue();
        } catch (ApiException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent e) {
        if (cm.hasCommand(e.getName())) {
            cm.getCommand(e.getName()).handle(e);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent e) {
        if (e.getComponentId().equals("sync-btn")) {
            e.deferReply(true).queue();
            log.info("Sync button clicker: " + e.getMember().getEffectiveName());
            String id = e.getMember().getId();
            if (cooldown.containsKey(id)) {
                long stampedTime = cooldown.get(id);
                long currentTime = System.currentTimeMillis();
                if ((currentTime - stampedTime) < 300000) {
                    long remainingTime = 300000 - (currentTime - stampedTime);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime);
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime - TimeUnit.MINUTES.toMillis(minutes));
                    String waitMessage = String.format("You must wait %d minutes and %d seconds before doing this again.", minutes, seconds);
                    e.getHook().sendMessage(waitMessage).setEphemeral(true).queue();
                    return;
                }
            }
            try {
                ass.performAccountSync(e.getJDA(), e.getMember());
                cooldown.put(id, System.currentTimeMillis());
                e.getHook().sendMessage("Account updated successfully.").setEphemeral(true).queue();
            } catch (ApiException ex) {
                cooldown.put(id, System.currentTimeMillis());
                e.getHook().sendMessage("""
                        Account update failed. You probably don't have your account linked to the community hub.
                        If you need assistance, contact @Nestirium#6394
                        """).setEphemeral(true).queue();
                log.error(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent e) {
        if (!e.getModalId().equals("announcer")) {
            return;
        }
        Broadcast.Builder builder = broadcastManager.getBroadcast(e.getMember().getId());
        if (builder == null) {
            e.reply("""
                    You took too long to submit your form, please try again.
                    Forms must be submitted within 15 minutes.
                    """).setEphemeral(true).queue();
            return;
        }
        String title = e.getValue("subject").getAsString();
        String body = e.getValue("body").getAsString();
        Broadcast broadcast = builder.title(title).message(body).build();

        GuildChannel guildChannel = e.getJDA().getGuildChannelById(broadcast.getChannelId());

        if (guildChannel == null) {
            e.reply("Requested channel no longer exists.").setEphemeral(true).queue();
            return;
        }

        List<String> mentionStrings = new ArrayList<>();
        for (Message.MentionType mentionType : Message.MentionType.values()) {
            Pattern pattern = mentionType.getPattern();
            Matcher matcher = pattern.matcher(broadcast.getMessage());
            while (matcher.find()) {
                mentionStrings.add("||"+matcher.group()+"||");
            }
        }
        String mentions = String.join(" ", mentionStrings);
        ec.setColor(Color.decode(broadcast.getHexColor()));
        if (!broadcast.getTitle().isEmpty()) {
            ec.setTitle(broadcast.getTitle());
        }
        ec.setDescription(broadcast.getMessage());
        if (!broadcast.isAnonymous()) {
            ec.setAuthor(e.getMember().getEffectiveName(), null, e.getMember().getEffectiveAvatarUrl());
        }
        MessageEmbed embed = ec.build();
        ec.clear();

        MessageCreateAction messageCreateAction;
        if (guildChannel instanceof TextChannel textChannel) {

            messageCreateAction = textChannel.sendMessage(mentions).addEmbeds(embed);
            String[] metadata = broadcast.getMetaData();
            for (String meta : metadata) {
                if (meta.equals("sync-message-buttons")) {
                    messageCreateAction.addActionRow(Button.primary("sync-btn", "♻ Synchronize"));
                }
            }
            messageCreateAction.queue();
        } else if (guildChannel instanceof NewsChannel newsChannel) {

            messageCreateAction = newsChannel.sendMessage(mentions).addEmbeds(embed);
            String[] metadata = broadcast.getMetaData();
            for (String meta : metadata) {
                if (meta.equals("sync-message-buttons")) {
                    messageCreateAction.addActionRow(Button.primary("sync-btn", "♻ Synchronize"));
                }
            }
            messageCreateAction.queue();
        }
        e.reply("Message has been dispatched.").setEphemeral(true).queue();
        broadcastManager.removeBroadcast(e.getMember().getId());

    }

}
