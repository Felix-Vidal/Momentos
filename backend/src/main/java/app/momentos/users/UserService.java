package app.momentos.users;

import app.momentos.common.exception.ForbiddenException;
import app.momentos.common.exception.NotFoundException;
import app.momentos.media.MediaRepository;
import app.momentos.users.dto.UpdateProfileRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserRepository users;
    private final MediaRepository media;

    public UserService(UserRepository users, MediaRepository media) {
        this.users = users;
        this.media = media;
    }

    @Transactional(readOnly = true)
    public User require(UUID id) {
        return users.findById(id).orElseThrow(() -> NotFoundException.of("Usuário"));
    }

    @Transactional
    public User updateProfile(UUID id, UpdateProfileRequest request) {
        User user = require(id);
        if (StringUtils.hasText(request.name())) {
            user.setName(request.name().trim());
        }
        if (request.bio() != null) {
            user.setBio(request.bio().isBlank() ? null : request.bio().trim());
        }
        return user;
    }

    /** A foto de perfil tem de ser uma mídia do próprio usuário. */
    @Transactional
    public User setAvatar(UUID userId, UUID mediaId) {
        User user = require(userId);
        if (mediaId == null) {
            user.setAvatarMediaId(null);
            return user;
        }
        var found = media.findById(mediaId).orElseThrow(() -> NotFoundException.of("Mídia"));
        if (!found.getOwnerId().equals(userId)) {
            throw new ForbiddenException("Escolha uma foto que você mesmo enviou.");
        }
        user.setAvatarMediaId(mediaId);
        return user;
    }
}
