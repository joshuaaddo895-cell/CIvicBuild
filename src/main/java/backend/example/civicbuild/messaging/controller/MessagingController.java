package backend.example.civicbuild.messaging.controller;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.messaging.service.MessagingService;
import backend.example.civicbuild.messaging.service.MessagingService.MessageResponse;
import backend.example.civicbuild.messaging.service.MessagingService.SendMessageRequest;
import backend.example.civicbuild.messaging.service.MessagingService.StartThreadRequest;
import backend.example.civicbuild.messaging.service.MessagingService.ThreadResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping("/threads")
    public ResponseEntity<ApiResponse<List<ThreadResponse>>> listThreads(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(messagingService.listThreads(user)));
    }

    @PostMapping("/threads")
    public ResponseEntity<ApiResponse<ThreadResponse>> startThread(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody StartThreadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thread created", messagingService.startThread(user, request)));
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> listMessages(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID threadId) {
        return ResponseEntity.ok(ApiResponse.ok(messagingService.listMessages(user, threadId)));
    }

    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID threadId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Message sent", messagingService.sendMessage(user, threadId, request)));
    }

    @PatchMapping("/threads/{threadId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID threadId) {
        messagingService.markRead(user, threadId);
        return ResponseEntity.ok(ApiResponse.message("Thread marked as read"));
    }
}
