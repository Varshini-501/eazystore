package com.eazybytes.eazystore.controller;

import com.eazybytes.eazystore.dto.CommentDto;
import com.eazybytes.eazystore.dto.CommentRequestDto;
import com.eazybytes.eazystore.dto.CreatePostDto;
import com.eazybytes.eazystore.dto.PostDto;
import com.eazybytes.eazystore.dto.VendorProfileDto;
import com.eazybytes.eazystore.service.ISocialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
public class SocialController {

    private final ISocialService iSocialService;

    // ----- Posts -----

    @PostMapping("/posts")
    public ResponseEntity<PostDto> createPost(@Valid @RequestBody CreatePostDto createPostDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(iSocialService.createPost(createPostDto));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        iSocialService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/mine")
    public ResponseEntity<List<PostDto>> getMyPosts() {
        return ResponseEntity.ok(iSocialService.getMyPosts());
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<PostDto>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean followingOnly) {
        return ResponseEntity.ok(iSocialService.getFeed(page, size, followingOnly));
    }

    // ----- Vendors -----

    @GetMapping("/vendors/{vendorId}")
    public ResponseEntity<VendorProfileDto> getVendorProfile(@PathVariable Long vendorId) {
        return ResponseEntity.ok(iSocialService.getVendorProfile(vendorId));
    }

    @GetMapping("/vendors/{vendorId}/posts")
    public ResponseEntity<Page<PostDto>> getVendorPosts(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(iSocialService.getVendorPosts(vendorId, page, size));
    }

    @PostMapping("/vendors/{vendorId}/follow")
    public ResponseEntity<Map<String, Object>> toggleFollow(@PathVariable Long vendorId) {
        return ResponseEntity.ok(iSocialService.toggleFollow(vendorId));
    }

    // ----- Likes & Comments -----

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long postId) {
        return ResponseEntity.ok(iSocialService.toggleLike(postId));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(iSocialService.getComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentDto> addComment(@PathVariable Long postId,
            @Valid @RequestBody CommentRequestDto commentRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(iSocialService.addComment(postId, commentRequestDto));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", exception.getMessage()));
    }

}
