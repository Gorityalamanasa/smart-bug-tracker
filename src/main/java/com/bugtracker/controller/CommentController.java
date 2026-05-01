package com.bugtracker.controller;

import com.bugtracker.model.Comment;
import com.bugtracker.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * REST controller for comment operations on issues.
 */
@RestController
@RequestMapping("/api/issues/{issueId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** GET /api/issues/{issueId}/comments — List comments for an issue */
    @GetMapping
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long issueId) {
        return ResponseEntity.ok(commentService.getCommentsByIssueId(issueId));
    }

    /** POST /api/issues/{issueId}/comments — Add a comment to an issue */
    @PostMapping
    public ResponseEntity<Comment> addComment(@PathVariable Long issueId,
                                              @RequestBody Map<String, Object> body) {
        Long authorId = Long.valueOf(body.get("authorId").toString());
        String content = body.get("content").toString();

        Comment comment = commentService.addComment(issueId, authorId, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    /** DELETE /api/issues/{issueId}/comments/{commentId} — Delete a comment */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long issueId,
                                              @PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
