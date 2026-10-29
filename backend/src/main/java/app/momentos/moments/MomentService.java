package app.momentos.moments;

import app.momentos.common.exception.ForbiddenException;
import app.momentos.common.exception.NotFoundException;
import app.momentos.common.exception.ValidationException;
import app.momentos.media.MediaRepository;
import app.momentos.media.MediaService;
import app.momentos.moments.dto.CreateMomentRequest;
import app.momentos.moments.dto.MomentView;
import app.momentos.moments.dto.UpdateMomentRequest;
import app.momentos.security.GroupAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MomentService {

    private final MomentRepository moments;
    private final MediaRepository media;
    private final MediaService mediaService;
    private final GroupAccessGuard access;

    public MomentService(MomentRepository moments, MediaRepository media,
            MediaService mediaService, GroupAccessGuard access) {
        this.moments = moments;
        this.media = media;
        this.mediaService = mediaService;
        this.access = access;
    }

    /** Qualquer membro pode criar Momentos: nomear o que aconteceu é trabalho do grupo. */
    @Transactional
    public MomentView create(UUID groupId, CreateMomentRequest request, UUID userId) {
        access.requireMember(groupId, userId);
        checkInterval(request.startsAt(), request.endsAt());

        Moment moment = moments.save(new Moment(groupId, request.title().trim(),
                StringUtils.hasText(request.description()) ? request.description().trim() : null,
                request.startsAt(), request.endsAt(), userId));
        return view(moment, userId);
    }

    @Transactional(readOnly = true)
    public List<MomentView> list(UUID groupId, UUID userId) {
        access.requireMember(groupId, userId);
        return moments.findByGroupIdOrderByStartsAt(groupId).stream()
                .map(moment -> view(moment, moment.getCreatedBy()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MomentView detail(UUID momentId, UUID userId) {
        Moment moment = require(momentId);
        access.requireMember(moment.getGroupId(), userId);
        return view(moment, moment.getCreatedBy());
    }

    @Transactional
    public MomentView update(UUID momentId, UpdateMomentRequest request, UUID userId) {
        Moment moment = requireEditable(momentId, userId);

        Instant startsAt = request.startsAt() == null ? moment.getStartsAt() : request.startsAt();
        Instant endsAt = request.endsAt() == null ? moment.getEndsAt() : request.endsAt();
        checkInterval(startsAt, endsAt);

        if (StringUtils.hasText(request.title())) {
            moment.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            moment.setDescription(
                    request.description().isBlank() ? null : request.description().trim());
        }
        moment.setStartsAt(startsAt);
        moment.setEndsAt(endsAt);
        return view(moment, moment.getCreatedBy());
    }

    @Transactional
    public void delete(UUID momentId, UUID userId) {
        // Apagar o Momento não apaga mídia: as fotos voltam a aparecer soltas.
        moments.delete(requireEditable(momentId, userId));
    }

    @Transactional(readOnly = true)
    public Moment require(UUID momentId) {
        return moments.findById(momentId).orElseThrow(() -> NotFoundException.of("Momento"));
    }

    /** Quem criou o Momento pode editá-lo; administradores do grupo também. */
    private Moment requireEditable(UUID momentId, UUID userId) {
        Moment moment = require(momentId);
        var member = access.requireMember(moment.getGroupId(), userId);
        if (!moment.getCreatedBy().equals(userId) && !member.isAdmin()) {
            throw new ForbiddenException(
                    "Só quem criou o momento ou um administrador pode alterá-lo.");
        }
        return moment;
    }

    private MomentView view(Moment moment, UUID authorId) {
        int count = media.findInInterval(moment.getGroupId(), moment.getStartsAt(),
                moment.getEndsAt()).size();
        return MomentView.of(moment, count, mediaService.summaryOf(authorId));
    }

    private static void checkInterval(Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new ValidationException("O fim do momento precisa ser depois do início.");
        }
    }
}
