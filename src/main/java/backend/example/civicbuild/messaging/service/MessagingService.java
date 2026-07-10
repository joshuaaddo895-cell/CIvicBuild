package backend.example.civicbuild.messaging.service;

import backend.example.civicbuild.agency.entity.Agency;
import backend.example.civicbuild.agency.exception.AgencyNotFoundException;
import backend.example.civicbuild.agency.repository.AgencyRepository;
import backend.example.civicbuild.auth.entity.User;
import backend.example.civicbuild.auth.exception.UserNotFoundException;
import backend.example.civicbuild.auth.repository.UserRepository;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.common.exception.ForbiddenException;
import backend.example.civicbuild.common.exception.NotFoundException;
import backend.example.civicbuild.messaging.entity.Message;
import backend.example.civicbuild.messaging.entity.MessageThread;
import backend.example.civicbuild.messaging.entity.ThreadReadState;
import backend.example.civicbuild.messaging.repository.MessageRepository;
import backend.example.civicbuild.messaging.repository.MessageThreadRepository;
import backend.example.civicbuild.messaging.repository.ThreadReadStateRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessagingService {

    private final MessageThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final ThreadReadStateRepository readStateRepository;
    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final Clock clock;

    public MessagingService(
            MessageThreadRepository threadRepository,
            MessageRepository messageRepository,
            ThreadReadStateRepository readStateRepository,
            UserRepository userRepository,
            AgencyRepository agencyRepository,
            Clock clock) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.readStateRepository = readStateRepository;
        this.userRepository = userRepository;
        this.agencyRepository = agencyRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ThreadResponse> listThreads(AuthenticatedUser actor) {
        return threadRepository.findForUser(actor.id()).stream()
                .map(thread -> toThreadResponse(thread, actor.id()))
                .toList();
    }

    @Transactional
    public ThreadResponse startThread(AuthenticatedUser actor, StartThreadRequest request) {
        User customer = userRepository.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        Agency agency = agencyRepository
                .findById(request.agencyId())
                .orElseThrow(AgencyNotFoundException::new);
        MessageThread thread = threadRepository
                .findByCustomerIdAndAgencyId(customer.getId(), agency.getId())
                .orElseGet(() -> threadRepository.save(MessageThread.builder()
                        .customer(customer)
                        .agency(agency)
                        .build()));
        return toThreadResponse(thread, actor.id());
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(AuthenticatedUser actor, UUID threadId) {
        MessageThread thread = requireThreadAccess(actor, threadId);
        return messageRepository.findByThreadIdOrderBySentAtAsc(thread.getId()).stream()
                .map(message -> toMessageResponse(message, actor.id()))
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(AuthenticatedUser actor, UUID threadId, SendMessageRequest request) {
        MessageThread thread = requireThreadAccess(actor, threadId);
        User sender = userRepository.findById(actor.id()).orElseThrow(UserNotFoundException::new);
        Message message = messageRepository.save(Message.builder()
                .thread(thread)
                .sender(sender)
                .text(request.text().trim())
                .build());
        thread.setUpdatedAt(clock.instant());
        return toMessageResponse(message, actor.id());
    }

    @Transactional
    public void markRead(AuthenticatedUser actor, UUID threadId) {
        requireThreadAccess(actor, threadId);
        ThreadReadState state = readStateRepository
                .findByThreadIdAndUserId(threadId, actor.id())
                .orElseGet(() -> ThreadReadState.builder()
                        .threadId(threadId)
                        .userId(actor.id())
                        .build());
        state.setLastReadAt(clock.instant());
        readStateRepository.save(state);
    }

    private MessageThread requireThreadAccess(AuthenticatedUser actor, UUID threadId) {
        MessageThread thread = threadRepository.findById(threadId).orElseThrow(() -> new NotFoundException("Thread not found"));
        boolean isCustomer = thread.getCustomer().getId().equals(actor.id());
        boolean isAgencyOwner = thread.getAgency().getOwner().getId().equals(actor.id());
        if (!isCustomer && !isAgencyOwner) {
            throw new ForbiddenException("Thread access denied");
        }
        return thread;
    }

    private ThreadResponse toThreadResponse(MessageThread thread, UUID viewerId) {
        List<Message> messages = messageRepository.findByThreadIdOrderBySentAtAsc(thread.getId());
        Message last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        boolean isCustomer = thread.getCustomer().getId().equals(viewerId);
        String participantName = isCustomer ? thread.getAgency().getName() : thread.getCustomer().getFullName();
        String participantLogo = isCustomer ? thread.getAgency().getLogoUrl() : thread.getCustomer().getProfilePictureUrl();
        Instant lastRead = readStateRepository
                .findByThreadIdAndUserId(thread.getId(), viewerId)
                .map(ThreadReadState::getLastReadAt)
                .orElse(Instant.EPOCH);
        long unread = messages.stream()
                .filter(m -> !m.getSender().getId().equals(viewerId))
                .filter(m -> m.getSentAt().isAfter(lastRead))
                .count();
        return new ThreadResponse(
                thread.getId(),
                participantName,
                participantLogo,
                last != null ? last.getText() : null,
                last != null ? last.getSentAt() : thread.getCreatedAt(),
                (int) unread);
    }

    private MessageResponse toMessageResponse(Message message, UUID viewerId) {
        return new MessageResponse(
                message.getId(),
                message.getThread().getId(),
                message.getText(),
                message.getSentAt(),
                message.getSender().getId().equals(viewerId));
    }

    public record StartThreadRequest(@NotNull UUID agencyId) {}

    public record SendMessageRequest(@NotBlank String text) {}

    public record ThreadResponse(
            UUID id,
            String participantName,
            String participantLogoUrl,
            String lastMessage,
            Instant lastMessageAt,
            int unreadCount) {}

    public record MessageResponse(
            UUID id, UUID threadId, String text, Instant sentAt, boolean isOutgoing) {}
}
