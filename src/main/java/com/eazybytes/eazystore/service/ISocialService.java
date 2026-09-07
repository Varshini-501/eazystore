package com.eazybytes.eazystore.service;

import com.eazybytes.eazystore.dto.CommentDto;
import com.eazybytes.eazystore.dto.CommentRequestDto;
import com.eazybytes.eazystore.dto.CreatePostDto;
import com.eazybytes.eazystore.dto.NotificationDto;
import com.eazybytes.eazystore.dto.PostDto;
import com.eazybytes.eazystore.dto.VendorProfileDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ISocialService {

    PostDto createPost(CreatePostDto createPostDto);

    void deletePost(Long postId);

    List<PostDto> getMyPosts();

    Page<PostDto> getFeed(int page, int size, boolean followingOnly);

    VendorProfileDto getVendorProfile(Long vendorId);

    Page<PostDto> getVendorPosts(Long vendorId, int page, int size);

    Map<String, Object> toggleLike(Long postId);

    CommentDto addComment(Long postId, CommentRequestDto commentRequestDto);

    List<CommentDto> getComments(Long postId);

    Map<String, Object> toggleFollow(Long vendorId);

    List<NotificationDto> getMyNotifications();

    void markNotificationRead(Long notificationId);

    void markAllNotificationsRead();
}
