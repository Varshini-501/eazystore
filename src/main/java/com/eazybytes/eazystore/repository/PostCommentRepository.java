package com.eazybytes.eazystore.repository;

import com.eazybytes.eazystore.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    List<PostComment> findByPost_PostIdOrderByCreatedAtAsc(Long postId);

    long countByPost_PostId(Long postId);
}
