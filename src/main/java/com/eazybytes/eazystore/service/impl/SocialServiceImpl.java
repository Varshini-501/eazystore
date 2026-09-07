package com.eazybytes.eazystore.service.impl;

import com.eazybytes.eazystore.dto.CommentDto;
import com.eazybytes.eazystore.dto.CommentRequestDto;
import com.eazybytes.eazystore.dto.CreatePostDto;
import com.eazybytes.eazystore.dto.NotificationDto;
import com.eazybytes.eazystore.dto.PostDto;
import com.eazybytes.eazystore.dto.VendorProfileDto;
import com.eazybytes.eazystore.entity.Customer;
import com.eazybytes.eazystore.entity.Notification;
import com.eazybytes.eazystore.entity.Post;
import com.eazybytes.eazystore.entity.PostComment;
import com.eazybytes.eazystore.entity.PostLike;
import com.eazybytes.eazystore.entity.Product;
import com.eazybytes.eazystore.entity.VendorFollow;
import com.eazybytes.eazystore.entity.VendorProfile;
import com.eazybytes.eazystore.exception.ResourceNotFoundException;
import com.eazybytes.eazystore.repository.CustomerRepository;
import com.eazybytes.eazystore.repository.NotificationRepository;
import com.eazybytes.eazystore.repository.PostCommentRepository;
import com.eazybytes.eazystore.repository.PostLikeRepository;
import com.eazybytes.eazystore.repository.PostRepository;
import com.eazybytes.eazystore.repository.ProductRepository;
import com.eazybytes.eazystore.repository.VendorFollowRepository;
import com.eazybytes.eazystore.repository.VendorProfileRepository;
import com.eazybytes.eazystore.service.ISocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements ISocialService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final VendorFollowRepository vendorFollowRepository;
    private final NotificationRepository notificationRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Override
    public PostDto createPost(CreatePostDto createPostDto) {
        VendorProfile vendorProfile = getAuthenticatedVendorProfile();
        Post post = new Post();
        post.setVendor(vendorProfile);
        post.setContent(createPostDto.getContent());
        post.setImageUrl(createPostDto.getImageUrl());
        if (createPostDto.getProductId() != null) {
            Product product = productRepository.findById(createPostDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id",
                            String.valueOf(createPostDto.getProductId())));
            post.setProduct(product);
        }
        post = postRepository.save(post);

        notifyFollowersOfNewPost(vendorProfile, post);

        return transformPost(post, vendorProfile.getCustomer().getCustomerId());
    }

    @Override
    public void deletePost(Long postId) {
        VendorProfile vendorProfile = getAuthenticatedVendorProfile();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", String.valueOf(postId)));
        if (!post.getVendor().getVendorId().equals(vendorProfile.getVendorId())) {
            throw new IllegalArgumentException("You can only delete your own posts");
        }
        postRepository.delete(post);
    }

    @Override
    public List<PostDto> getMyPosts() {
        VendorProfile vendorProfile = getAuthenticatedVendorProfile();
        Long viewerId = vendorProfile.getCustomer().getCustomerId();
        return postRepository
                .findByVendor_VendorIdOrderByCreatedAtDesc(vendorProfile.getVendorId(), Pageable.unpaged())
                .getContent().stream().map(post -> transformPost(post, viewerId)).toList();
    }

    @Override
    public Page<PostDto> getFeed(int page, int size, boolean followingOnly) {
        Customer customer = getAuthenticatedCustomerOrNull();
        Long viewerId = customer != null ? customer.getCustomerId() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Post> posts;
        if (followingOnly && viewerId != null) {
            List<Long> followedVendorIds = vendorFollowRepository.findFollowedVendorIds(viewerId);
            if (followedVendorIds.isEmpty()) {
                return Page.empty(pageable);
            }
            posts = postRepository.findByVendor_VendorIdInOrderByCreatedAtDesc(followedVendorIds, pageable);
        } else {
            posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return posts.map(post -> transformPost(post, viewerId));
    }

    @Override
    public VendorProfileDto getVendorProfile(Long vendorId) {
        VendorProfile vendorProfile = vendorProfileRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", String.valueOf(vendorId)));
        Customer viewer = getAuthenticatedCustomerOrNull();
        VendorProfileDto dto = new VendorProfileDto();
        dto.setVendorId(vendorProfile.getVendorId());
        dto.setStoreName(vendorProfile.getStoreName());
        dto.setStoreDescription(vendorProfile.getStoreDescription());
        dto.setLogoUrl(vendorProfile.getLogoUrl());
        dto.setFollowerCount(vendorFollowRepository.countByVendor_VendorId(vendorId));
        dto.setPostCount(postRepository
                .findByVendor_VendorIdOrderByCreatedAtDesc(vendorId, Pageable.unpaged()).getTotalElements());
        if (viewer != null) {
            dto.setFollowingCurrentUser(vendorFollowRepository
                    .existsByVendor_VendorIdAndCustomer_CustomerId(vendorId, viewer.getCustomerId()));
        }
        return dto;
    }

    @Override
    public Page<PostDto> getVendorPosts(Long vendorId, int page, int size) {
        if (!vendorProfileRepository.existsById(vendorId)) {
            throw new ResourceNotFoundException("Vendor", "id", String.valueOf(vendorId));
        }
        Customer viewer = getAuthenticatedCustomerOrNull();
        Long viewerId = viewer != null ? viewer.getCustomerId() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findByVendor_VendorIdOrderByCreatedAtDesc(vendorId, pageable)
                .map(post -> transformPost(post, viewerId));
    }

    @Override
    public Map<String, Object> toggleLike(Long postId) {
        Customer customer = getAuthenticatedCustomer();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", String.valueOf(postId)));

        Optional<PostLike> existingLike = postLikeRepository
                .findByPost_PostIdAndCustomer_CustomerId(postId, customer.getCustomerId());
        boolean liked;
        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            liked = false;
        } else {
            PostLike like = new PostLike();
            like.setPost(post);
            like.setCustomer(customer);
            postLikeRepository.save(like);
            liked = true;
            notifyVendorOwner(post, customer, "LIKE",
                    customer.getName() + " liked your post");
        }
        long likeCount = postLikeRepository.countByPost_PostId(postId);
        return Map.of("liked", liked, "likeCount", likeCount);
    }

    @Override
    public CommentDto addComment(Long postId, CommentRequestDto commentRequestDto) {
        Customer customer = getAuthenticatedCustomer();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", String.valueOf(postId)));
        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setCustomer(customer);
        comment.setContent(commentRequestDto.getContent());
        comment = postCommentRepository.save(comment);

        notifyVendorOwner(post, customer, "COMMENT",
                customer.getName() + " commented on your post");

        return transformComment(comment);
    }

    @Override
    public List<CommentDto> getComments(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", String.valueOf(postId));
        }
        return postCommentRepository.findByPost_PostIdOrderByCreatedAtAsc(postId)
                .stream().map(this::transformComment).toList();
    }

    @Override
    public Map<String, Object> toggleFollow(Long vendorId) {
        Customer customer = getAuthenticatedCustomer();
        VendorProfile vendorProfile = vendorProfileRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", String.valueOf(vendorId)));

        if (vendorProfile.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new IllegalArgumentException("You cannot follow your own vendor store");
        }

        Optional<VendorFollow> existingFollow = vendorFollowRepository
                .findByVendor_VendorIdAndCustomer_CustomerId(vendorId, customer.getCustomerId());
        boolean following;
        if (existingFollow.isPresent()) {
            vendorFollowRepository.delete(existingFollow.get());
            following = false;
        } else {
            VendorFollow follow = new VendorFollow();
            follow.setVendor(vendorProfile);
            follow.setCustomer(customer);
            vendorFollowRepository.save(follow);
            following = true;

            Notification notification = new Notification();
            notification.setRecipient(vendorProfile.getCustomer());
            notification.setActor(customer);
            notification.setType("FOLLOW");
            notification.setMessage(customer.getName() + " started following " + vendorProfile.getStoreName());
            notificationRepository.save(notification);
        }
        long followerCount = vendorFollowRepository.countByVendor_VendorId(vendorId);
        return Map.of("following", following, "followerCount", followerCount);
    }

    @Override
    public List<NotificationDto> getMyNotifications() {
        Customer customer = getAuthenticatedCustomer();
        return notificationRepository.findByRecipient_CustomerIdOrderByCreatedAtDesc(customer.getCustomerId())
                .stream().map(this::transformNotification).toList();
    }

    @Override
    public void markNotificationRead(Long notificationId) {
        Customer customer = getAuthenticatedCustomer();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id",
                        String.valueOf(notificationId)));
        if (!notification.getRecipient().getCustomerId().equals(customer.getCustomerId())) {
            throw new IllegalArgumentException("You cannot modify another user's notification");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllNotificationsRead() {
        Customer customer = getAuthenticatedCustomer();
        List<Notification> notifications = notificationRepository
                .findByRecipient_CustomerIdOrderByCreatedAtDesc(customer.getCustomerId());
        notifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }

    // ---------- helpers ----------

    private void notifyFollowersOfNewPost(VendorProfile vendorProfile, Post post) {
        List<VendorFollow> follows = vendorFollowRepository.findByVendor_VendorId(vendorProfile.getVendorId());
        List<Notification> notifications = new ArrayList<>();
        for (VendorFollow follow : follows) {
            Notification notification = new Notification();
            notification.setRecipient(follow.getCustomer());
            notification.setActor(vendorProfile.getCustomer());
            notification.setType("NEW_POST");
            notification.setMessage(vendorProfile.getStoreName() + " shared a new post");
            notification.setPost(post);
            notifications.add(notification);
        }
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    private void notifyVendorOwner(Post post, Customer actor, String type, String message) {
        Customer vendorOwner = post.getVendor().getCustomer();
        if (vendorOwner.getCustomerId().equals(actor.getCustomerId())) {
            return; // don't notify vendors about their own interactions with their post
        }
        Notification notification = new Notification();
        notification.setRecipient(vendorOwner);
        notification.setActor(actor);
        notification.setType(type);
        notification.setMessage(message);
        notification.setPost(post);
        notificationRepository.save(notification);
    }

    private PostDto transformPost(Post post, Long viewerCustomerId) {
        PostDto dto = new PostDto();
        dto.setPostId(post.getPostId());
        dto.setVendorId(post.getVendor().getVendorId());
        dto.setStoreName(post.getVendor().getStoreName());
        dto.setVendorLogoUrl(post.getVendor().getLogoUrl());
        dto.setContent(post.getContent());
        dto.setImageUrl(post.getImageUrl());
        dto.setCreatedAt(post.getCreatedAt());
        if (post.getProduct() != null) {
            dto.setProductId(post.getProduct().getId());
            dto.setProductName(post.getProduct().getName());
            dto.setProductImageUrl(post.getProduct().getImageUrl());
        }
        dto.setLikeCount(postLikeRepository.countByPost_PostId(post.getPostId()));
        dto.setCommentCount(postCommentRepository.countByPost_PostId(post.getPostId()));
        if (viewerCustomerId != null) {
            dto.setLikedByCurrentUser(postLikeRepository
                    .existsByPost_PostIdAndCustomer_CustomerId(post.getPostId(), viewerCustomerId));
        }
        return dto;
    }

    private CommentDto transformComment(PostComment comment) {
        CommentDto dto = new CommentDto();
        dto.setCommentId(comment.getCommentId());
        dto.setPostId(comment.getPost().getPostId());
        dto.setCustomerName(comment.getCustomer().getName());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }

    private NotificationDto transformNotification(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setNotificationId(notification.getNotificationId());
        dto.setType(notification.getType());
        dto.setMessage(notification.getMessage());
        dto.setRead(Boolean.TRUE.equals(notification.getIsRead()));
        dto.setCreatedAt(notification.getCreatedAt());
        if (notification.getPost() != null) {
            dto.setPostId(notification.getPost().getPostId());
        }
        if (notification.getActor() != null) {
            dto.setActorName(notification.getActor().getName());
        }
        return dto;
    }

    private VendorProfile getAuthenticatedVendorProfile() {
        Customer customer = getAuthenticatedCustomer();
        return vendorProfileRepository.findByCustomer_CustomerId(customer.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("VendorProfile", "customerId",
                        String.valueOf(customer.getCustomerId())));
    }

    private Customer getAuthenticatedCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("You must be logged in to perform this action");
        }
        String email = authentication.getName();
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", email));
    }

    private Customer getAuthenticatedCustomerOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return customerRepository.findByEmail(authentication.getName()).orElse(null);
    }
}
