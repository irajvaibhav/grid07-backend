package com.grid07.backend.service;

import com.grid07.backend.entity.*;
import com.grid07.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepo;

    @Autowired
    private CommentRepository commentRepo;

    @Autowired
    private StringRedisTemplate redis;

    public Post createPost(Long authorId, AuthorType authorType, String content) {
        Post post = new Post();
        post.setAuthorId(authorId);
        post.setAuthorType(authorType);
        post.setContent(content);
        return postRepo.save(post);
    }

    public Post likePost(Long postId, Long userId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        post.setLikeCount(post.getLikeCount() + 1);
        postRepo.save(post);
        redis.opsForValue().increment("post:" + postId + ":virality_score", 20);
        return post;
    }

    public Comment addComment(Long postId, Long authorId, AuthorType authorType,
                              String content, int depthLevel, Long botId, Long humanId) {

        if (depthLevel > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max depth reached");
        }

        if (authorType == AuthorType.BOT) {
            // INCR is atomic — this is how we prevent race conditions
            Long botCount = redis.opsForValue().increment("post:" + postId + ":bot_count");
            if (botCount > 100) {
                redis.opsForValue().decrement("post:" + postId + ":bot_count");
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bot cap reached");
            }

            String cooldownKey = "cooldown:bot_" + botId + ":human_" + humanId;
            Boolean allowed = redis.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofMinutes(10));
            if (!allowed) {
                redis.opsForValue().decrement("post:" + postId + ":bot_count");
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Cooldown active");
            }

            handleBotNotification(botId, humanId);
            redis.opsForValue().increment("post:" + postId + ":virality_score", 1);

        } else {
            redis.opsForValue().increment("post:" + postId + ":virality_score", 50);
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        comment.setAuthorType(authorType);
        comment.setContent(content);
        comment.setDepthLevel(depthLevel);
        return commentRepo.save(comment);
    }

    private void handleBotNotification(Long botId, Long humanId) {
        String cooldownKey = "notif_cooldown:user_" + humanId;
        Boolean isFirst = redis.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofMinutes(15));

        if (isFirst) {
            System.out.println("Push Notification Sent to User " + humanId);
        } else {
            redis.opsForList().rightPush("user:" + humanId + ":pending_notifs", "Bot " + botId + " replied to your post");
        }
    }
}