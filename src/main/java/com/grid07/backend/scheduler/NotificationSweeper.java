package com.grid07.backend.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;

@Component
public class NotificationSweeper {

    @Autowired
    private StringRedisTemplate redis;

    @Scheduled(cron = "0 */5 * * * *")
    public void sweep() {
        Set<String> keys = redis.keys("user:*:pending_notifs");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            List<String> notifs = redis.opsForList().range(key, 0, -1);
            if (notifs == null || notifs.isEmpty()) continue;

            redis.delete(key);
            System.out.println("Summarized Push Notification: " + notifs.get(0) +
                    " and " + (notifs.size() - 1) + " others interacted with your posts.");
        }
    }
}