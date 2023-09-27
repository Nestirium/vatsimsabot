package nestirium.savacc.discord.schemas;

public class Broadcast {
    private final String authorId;
    private final String title;
    private final String message;
    private final String channelId;
    private final boolean anonymous;
    private final String[] metaData;

    private final String hexColor;

    private Broadcast(String authorId, String title, String message, String channelId, boolean anonymous, String[] metaData, String hexColor) {
        this.authorId = authorId;
        this.title = title;
        this.message = message;
        this.channelId = channelId;
        this.anonymous = anonymous;
        this.metaData = metaData;
        this.hexColor = hexColor;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getChannelId() {
        return channelId;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public String[] getMetaData() {
        return metaData;
    }

    public String getHexColor() {
        return hexColor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String authorId;
        private String title;
        private String message;
        private String channelId;
        private boolean anonymous;
        private String[] metaData;

        private String hexColor;

        public Builder authorId(String authorId) {
            this.authorId = authorId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder channelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        public Builder anonymous(boolean anonymous) {
            this.anonymous = anonymous;
            return this;
        }

        public Builder metaData(String[] metaData) {
            this.metaData = metaData;
            return this;
        }

        public Builder hexColor(String hexColor) {
            this.hexColor = hexColor;
            return this;
        }

        public Broadcast build() {
            return new Broadcast(authorId, title, message, channelId, anonymous, metaData, hexColor);
        }
    }
}
