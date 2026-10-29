package app.momentos.users;

import app.momentos.common.exception.NotFoundException;
import app.momentos.users.dto.UpdateProfileRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
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
}
