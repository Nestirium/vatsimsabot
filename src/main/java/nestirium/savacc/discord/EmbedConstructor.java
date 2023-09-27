package nestirium.savacc.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class EmbedConstructor extends EmbedBuilder {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM. dd, yyyy HH:mm");

    public EmbedConstructor asSuccess() {
        super.setTitle(":white_check_mark: OK");
        super.setColor(Color.GREEN);
        return this;
    }

    public EmbedConstructor asError() {
        super.setTitle(":x: NO");
        super.setColor(Color.RED);
        return this;
    }

    public EmbedConstructor describe(String description) {
        super.setDescription(description);
        return this;
    }

    @NotNull
    @Override
    public MessageEmbed build() {
        super.setFooter(LocalDateTime.now(ZoneOffset.UTC).format(formatter) + " UTC © VATSIM Saudi Arabia");
        return super.build();
    }

    public static DateTimeFormatter getDateTimeFormatter() {
        return formatter;
    }

}
