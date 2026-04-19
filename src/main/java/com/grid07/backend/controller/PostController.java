package com.grid07.backend.controller;

import com.grid07.backend.entity.*;
import com.grid07.backend.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping
    public Post createPost(@RequestBody PostRequest req) {
        return postService.createPost(req.authorId, req.authorType, req.content);
    }

    @PostMapping("/{postId}/like")
    public Post likePost(@PathVariable Long postId, @RequestParam Long userId) {
        return postService.likePost(postId, userId);
    }

    @PostMapping("/{postId}/comments")
    public Comment addComment(@PathVariable Long postId, @RequestBody CommentRequest req) {
        return postService.addComment(postId, req.authorId, req.authorType,
                req.content, req.depthLevel, req.botId, req.humanId);
    }

    static class PostRequest {
        public Long authorId;
        public AuthorType authorType;
        public String content;
    }

    static class CommentRequest {
        public Long authorId;
        public AuthorType authorType;
        public String content;
        public int depthLevel;
        public Long botId;
        public Long humanId;
    }
}