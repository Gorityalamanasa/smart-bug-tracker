package com.bugtracker.service;

import com.bugtracker.model.Comment;
import com.bugtracker.model.Issue;
import com.bugtracker.model.User;
import com.bugtracker.repository.CommentRepository;
import com.bugtracker.repository.IssueRepository;
import com.bugtracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Service layer for comment management.
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          IssueRepository issueRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    public List<Comment> getCommentsByIssueId(Long issueId) {
        return commentRepository.findByIssueIdOrderByCreatedAtDesc(issueId);
    }

    public Comment addComment(Long issueId, Long authorId, String content) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + authorId));

        Comment comment = new Comment(content, author, issue);
        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}
