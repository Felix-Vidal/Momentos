package app.momentos.media;

import app.momentos.common.exception.ForbiddenException;
import app.momentos.common.exception.NotFoundException;
import app.momentos.media.dto.MediaView;
import app.momentos.media.dto.UpdateMediaRequest;
import app.momentos.security.GroupAccessGuard;
import app.momentos.storage.StorageService;
import app.momentos.storage.StoredObject;
import app.momentos.users.User;
import app.momentos.users.UserRepository;
import app.momentos.users.dto.UserSummary;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaService {

    private final MediaRepository media;
    private final MediaGroupShareRepository shares;
    private final UserRepository users;
    private final StorageService storage;
    private final GroupAccessGuard access;

    public MediaService(MediaRepository media, MediaGroupShareRepository shares,
            UserRepository users, StorageService storage, GroupAccessGuard access) {
        this.media = media;
        this.shares = shares;
        this.users = users;
        this.storage = storage;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public Media require(UUID mediaId) {
        return media.findById(mediaId).orElseThrow(() -> NotFoundException.of("Mídia"));
    }

    /**
     * Uma mídia é visível para o dono e para quem participa de algum grupo em que
     * ela foi compartilhada.
     */
    @Transactional(readOnly = true)
    public Media requireVisible(UUID mediaId, UUID userId) {
        Media found = require(mediaId);
        if (found.getOwnerId().equals(userId) || shares.isVisibleTo(mediaId, userId)) {
            return found;
        }
        throw new ForbiddenException("Você não tem acesso a essa mídia.");
    }

    @Transactional(readOnly = true)
    public StoredObject openOriginal(UUID mediaId, UUID userId) {
        return storage.load(requireVisible(mediaId, userId).getStorageKey());
    }

    @Transactional(readOnly = true)
    public StoredObject openThumbnail(UUID mediaId, UUID userId) {
        Media found = requireVisible(mediaId, userId);
        String key = found.getThumbnailKey();
        if (key == null) {
            throw NotFoundException.of("Miniatura");
        }
        return storage.load(key);
    }

    @Transactional
    public MediaView update(UUID mediaId, UpdateMediaRequest request, UUID userId) {
        Media found = requireOwned(mediaId, userId);
        if (request.capturedAt() != null) {
            found.setCapturedAt(request.capturedAt());
        }
        if (request.caption() != null) {
            found.setCaption(request.caption().isBlank() ? null : request.caption().trim());
        }
        return MediaView.of(found, summaryOf(found.getOwnerId()));
    }

    @Transactional
    public void delete(UUID mediaId, UUID userId) {
        Media found = requireOwned(mediaId, userId);
        // O arquivo sai do armazenamento junto; os compartilhamentos caem por cascata.
        storage.delete(found.getStorageKey());
        if (found.getThumbnailKey() != null) {
            storage.delete(found.getThumbnailKey());
        }
        media.delete(found);
    }

    @Transactional
    public void share(UUID mediaId, List<UUID> groupIds, UUID userId) {
        requireOwned(mediaId, userId);
        for (UUID groupId : groupIds) {
            // Só é possível compartilhar com grupos dos quais se participa.
            access.requireMember(groupId, userId);
            if (!shares.existsByMediaIdAndGroupId(mediaId, groupId)) {
                shares.save(new MediaGroupShare(mediaId, groupId, userId));
            }
        }
    }

    @Transactional
    public void unshare(UUID mediaId, UUID groupId, UUID userId) {
        Media found = require(mediaId);
        boolean isOwner = found.getOwnerId().equals(userId);
        if (!isOwner) {
            // Administradores podem retirar do grupo uma mídia que não é deles.
            access.requireAdmin(groupId, userId);
        }
        shares.findByMediaIdAndGroupId(mediaId, groupId).ifPresent(shares::delete);
    }

    @Transactional(readOnly = true)
    public List<UUID> groupsOf(UUID mediaId, UUID userId) {
        requireOwned(mediaId, userId);
        return shares.findByMediaId(mediaId).stream().map(MediaGroupShare::getGroupId).toList();
    }

    /** Mídias que um integrante publicou em um grupo (usado no perfil dentro do grupo). */
    @Transactional(readOnly = true)
    public List<MediaView> byMemberInGroup(UUID groupId, UUID ownerId, UUID viewerId, int limit) {
        access.requireMember(groupId, viewerId);
        UserSummary owner = summaryOf(ownerId);
        return media.findByOwnerInGroup(ownerId, groupId, PageRequest.of(0, limit)).stream()
                .map(m -> MediaView.of(m, owner))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MediaView> mine(UUID ownerId, int limit) {
        UserSummary owner = summaryOf(ownerId);
        return media.findByOwnerIdOrderByCapturedAtDesc(ownerId, PageRequest.of(0, limit)).stream()
                .map(m -> MediaView.of(m, owner))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, UserSummary> summariesOf(List<UUID> userIds) {
        return users.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, UserSummary::from, (a, b) -> a));
    }

    public UserSummary summaryOf(UUID userId) {
        return users.findById(userId).map(UserSummary::from)
                .orElseThrow(() -> NotFoundException.of("Usuário"));
    }

    private Media requireOwned(UUID mediaId, UUID userId) {
        Media found = require(mediaId);
        if (!found.getOwnerId().equals(userId)) {
            throw new ForbiddenException("Só quem enviou a mídia pode alterá-la.");
        }
        return found;
    }

    Function<UUID, UserSummary> ownerLookup() {
        return this::summaryOf;
    }
}
