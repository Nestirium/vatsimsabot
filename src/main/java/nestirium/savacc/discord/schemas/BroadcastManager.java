package nestirium.savacc.discord.schemas;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class BroadcastManager {


    private final Cache<String, Broadcast.Builder> incompleteBroadcasts =
            Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

    public Broadcast.Builder getBroadcast(String authorId) {
        return incompleteBroadcasts.getIfPresent(authorId);
    }

    public void storeBroadcast(String authorId, Broadcast.Builder broadcast) {
        incompleteBroadcasts.put(authorId, broadcast);
    }

    public void removeBroadcast(String authorId) {
        incompleteBroadcasts.invalidate(authorId);
    }


}
