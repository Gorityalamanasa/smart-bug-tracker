package com.bugtracker.controller;

import com.bugtracker.model.Comment;
import com.bugtracker.model.User;
import com.bugtracker.security.CustomUserDetails;
import com.bugtracker.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for comment operations on issues.
 * Author is identified from JWT — no authorId in request body.
 */
@RestController
@RequestMapping("/api/issues/{issueId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** Get current authenticated user from SecurityContext */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }

    /** GET /api/issues/{issueId}/comments — List comments for an issue */
    @GetMapping
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long issueId) {
        return ResponseEntity.ok(commentService.getCommentsByIssueId(issueId));
    }

    /**
     * POST /api/issues/{issueId}/comments — Add a comment to an issue.
     * Author is automatically set from JWT token.
     */
    @PostMapping
    public ResponseEntity<Comment> addComment(@PathVariable Long issueId,
                                               @RequestBody Map<String, Object> body) {
        User currentUser = getCurrentUser();
        String content = body.get("content").toString();

        Comment comment = commentService.addComment(issueId, currentUser.getId(), content);
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
