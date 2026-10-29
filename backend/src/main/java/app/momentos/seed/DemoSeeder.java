package app.momentos.seed;

import app.momentos.comments.Comment;
import app.momentos.comments.CommentRepository;
import app.momentos.groups.Group;
import app.momentos.groups.GroupMember;
import app.momentos.groups.GroupMemberRepository;
import app.momentos.groups.GroupRepository;
import app.momentos.groups.GroupRole;
import app.momentos.groups.GroupTheme;
import app.momentos.groups.GroupVisibility;
import app.momentos.invites.JoinRequest;
import app.momentos.invites.JoinRequestRepository;
import app.momentos.media.CapturedAtSource;
import app.momentos.media.Media;
import app.momentos.media.MediaGroupShare;
import app.momentos.media.MediaGroupShareRepository;
import app.momentos.media.MediaRepository;
import app.momentos.media.MediaType;
import app.momentos.media.processing.FfmpegVideoProcessor;
import app.momentos.media.processing.ImageThumbnailGenerator;
import app.momentos.moments.Moment;
import app.momentos.moments.MomentRepository;
import app.momentos.storage.StorageService;
import app.momentos.users.User;
import app.momentos.users.UserRepository;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados de demonstração: a Sociedade da Roleta, com integrantes, mídias, os Momentos
 * "Corujão da LetalCompany" e "Almoço no RU" e comentários.
 *
 * <p>Roda apenas no perfil dev, com {@code momentos.seed.enabled=true}, e é idempotente:
 * se o grupo já existe, nada é recriado.
 */
@Configuration
@Profile("dev")
public class DemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);

    private static final ZoneId ACRE = ZoneId.of("America/Rio_Branco");
    private static final String GROUP_SLUG = "sociedade-da-roleta";
    private static final String GROUP_DESCRIPTION = """
            Somos um grupo do curso de Bacharelado em Sistemas de Informação da UFAC. \
            O nome vem do nosso ritual no Restaurante Universitário: toda vez que íamos \
            almoçar, girávamos uma roleta para decidir quem levaria todas as bandejas \
            para serem lavadas pelos funcionários.""";

    @Bean
    ApplicationRunner demoSeedRunner(SeedProperties properties, Seeder seeder) {
        return args -> {
            if (!properties.enabled()) {
                return;
            }
            seeder.run(properties.password());
        };
    }

    /** Separado do runner para a transação valer sobre toda a carga. */
    @org.springframework.stereotype.Component
    static class Seeder {

        private final UserRepository users;
        private final GroupRepository groups;
        private final GroupMemberRepository members;
        private final MediaRepository media;
        private final MediaGroupShareRepository shares;
        private final MomentRepository moments;
        private final CommentRepository comments;
        private final JoinRequestRepository joinRequests;
        private final StorageService storage;
        private final ImageThumbnailGenerator thumbnails;
        private final PlaceholderImageFactory images;
        private final PlaceholderVideoFactory videos;
        private final FfmpegVideoProcessor videoProcessor;
        private final PasswordEncoder passwordEncoder;

        Seeder(UserRepository users, GroupRepository groups, GroupMemberRepository members,
                MediaRepository media, MediaGroupShareRepository shares, MomentRepository moments,
                CommentRepository comments, JoinRequestRepository joinRequests,
                StorageService storage, ImageThumbnailGenerator thumbnails,
                PlaceholderImageFactory images, PlaceholderVideoFactory videos,
                FfmpegVideoProcessor videoProcessor, PasswordEncoder passwordEncoder) {
            this.users = users;
            this.groups = groups;
            this.members = members;
            this.media = media;
            this.shares = shares;
            this.moments = moments;
            this.comments = comments;
            this.joinRequests = joinRequests;
            this.storage = storage;
            this.thumbnails = thumbnails;
            this.images = images;
            this.videos = videos;
            this.videoProcessor = videoProcessor;
            this.passwordEncoder = passwordEncoder;
        }

        @Transactional
        void run(String password) {
            if (groups.existsBySlug(GROUP_SLUG)) {
                // A base já existe; os extras são aplicados à parte, para quem
                // atualizou o projeto sem recriar o banco.
                Group existing = groups.findBySlug(GROUP_SLUG).orElseThrow();
                List<User> roster = people(password);
                if (seedExtras(existing, roster)) {
                    log.info("Seed: conteúdo extra (vídeos e momentos de demonstração) aplicado.");
                } else {
                    log.info("Seed já aplicado: grupo '{}' está completo.", GROUP_SLUG);
                }
                return;
            }
            log.info("Aplicando seed de demonstração…");

            List<User> people = people(password);
            User owner = people.getFirst();

            Group group = groups.save(new Group(GROUP_SLUG, "Sociedade da Roleta",
                    GROUP_DESCRIPTION, GroupVisibility.OPEN, owner.getId()));
            group.setTheme(GroupTheme.ROULETTE);

            members.save(new GroupMember(group.getId(), owner.getId(), GroupRole.ADMIN));
            members.save(new GroupMember(group.getId(), people.get(1).getId(), GroupRole.ADMIN));
            people.stream().skip(2).limit(5).forEach(person ->
                    members.save(new GroupMember(group.getId(), person.getId(), GroupRole.MEMBER)));

            // Quem ainda não entrou: deixa um pedido pendente para a tela de aprovação.
            User candidate = people.getLast();
            joinRequests.save(new JoinRequest(group.getId(), candidate.getId(),
                    "Fui da turma de 2024, posso entrar?"));

            seedTimeline(group, people);
            seedExtras(group, people);
            seedSecondGroup(people, password);

            log.info("Seed pronto: entre com {} e a senha '{}'.", owner.getEmail(), password);
        }

        private List<User> people(String password) {
            record Person(String name, String email, String bio) {
            }
            List<Person> roster = List.of(
                    new Person("Félix Vidal", "felix@ufac.br",
                            "Quem criou o grupo e perdeu na roleta três vezes seguidas."),
                    new Person("Ana Beatriz", "ana@ufac.br", "Fotógrafa oficial dos almoços."),
                    new Person("Bruno Lima", "bruno@ufac.br", "Especialista em girar a roleta devagar."),
                    new Person("Carla Souza", "carla@ufac.br", "Nunca levou bandeja. Ainda."),
                    new Person("Diego Rocha", "diego@ufac.br", "Trouxe o jogo para o corujão."),
                    new Person("Eduarda Nunes", "eduarda@ufac.br", "Organiza as listas de presença."),
                    new Person("Gustavo Alves", "gustavo@ufac.br", "Chega sempre no fim do almoço."),
                    new Person("Helena Castro", "helena@ufac.br", "Entrou no grupo pela roleta."));

            List<User> created = new ArrayList<>();
            for (Person person : roster) {
                User user = users.findByEmail(person.email()).orElseGet(() ->
                        users.save(new User(person.name(), person.email(),
                                passwordEncoder.encode(password))));
                user.setBio(person.bio());
                created.add(user);
            }
            return created;
        }

        private void seedTimeline(Group group, List<User> people) {
            record Shot(String label, LocalDateTime at, int width, int height, int author) {
            }
            // A cronologia conta a história: almoços, a viagem, o corujão e o dia seguinte.
            List<Shot> shots = List.of(
                    new Shot("Primeira roleta do semestre", LocalDateTime.of(2026, 9, 14, 12, 10), 1400, 1000, 1),
                    new Shot("A bandeja que ninguém queria", LocalDateTime.of(2026, 9, 14, 12, 42), 1000, 1400, 2),
                    new Shot("Fila do RU às 11h58", LocalDateTime.of(2026, 9, 21, 11, 58), 1400, 900, 0),
                    new Shot("Roleta improvisada no caderno", LocalDateTime.of(2026, 9, 21, 12, 20), 1200, 1200, 3),
                    new Shot("Mesa do fundo, de novo", LocalDateTime.of(2026, 9, 28, 12, 35), 1400, 1000, 4),
                    new Shot("Sobremesa negociada", LocalDateTime.of(2026, 9, 28, 13, 5), 1100, 1400, 1),
                    new Shot("Laboratório antes da prova", LocalDateTime.of(2026, 10, 5, 19, 30), 1400, 950, 0),
                    new Shot("Café que salvou o trabalho", LocalDateTime.of(2026, 10, 5, 22, 15), 1000, 1300, 5),
                    new Shot("Almoço comemorando a entrega", LocalDateTime.of(2026, 10, 13, 12, 15), 1400, 1000, 2),
                    new Shot("A roleta decidiu: Félix", LocalDateTime.of(2026, 10, 13, 12, 48), 1200, 1200, 1),
                    new Shot("Bandejas em comboio", LocalDateTime.of(2026, 10, 13, 12, 55), 1400, 900, 3),
                    new Shot("Chegando para o corujão", LocalDateTime.of(2026, 10, 13, 22, 10), 1400, 1000, 4),
                    new Shot("Primeira partida da LetalCompany", LocalDateTime.of(2026, 10, 13, 22, 47), 1400, 950, 0),
                    new Shot("Quase deu certo", LocalDateTime.of(2026, 10, 13, 23, 31), 1200, 1300, 5),
                    new Shot("Meia-noite e ninguém desistiu", LocalDateTime.of(2026, 10, 14, 0, 2), 1400, 1000, 2),
                    new Shot("Pausa para o lanche", LocalDateTime.of(2026, 10, 14, 1, 14), 1100, 1400, 6),
                    new Shot("Estratégia na lousa", LocalDateTime.of(2026, 10, 14, 2, 30), 1400, 900, 1),
                    new Shot("Vitória às 3h47", LocalDateTime.of(2026, 10, 14, 3, 47), 1400, 1050, 3),
                    new Shot("O sol chegou antes do sono", LocalDateTime.of(2026, 10, 14, 5, 40), 1400, 950, 0),
                    new Shot("Café da manhã improvisado", LocalDateTime.of(2026, 10, 14, 7, 20), 1200, 1200, 4),
                    new Shot("Aula das 8h, heroicamente", LocalDateTime.of(2026, 10, 14, 8, 5), 1400, 900, 5),
                    new Shot("Roleta da semana seguinte", LocalDateTime.of(2026, 10, 20, 12, 12), 1400, 1000, 1),
                    new Shot("Helena girou e escapou", LocalDateTime.of(2026, 10, 20, 12, 30), 1100, 1300, 2),
                    new Shot("Último almoço antes das provas", LocalDateTime.of(2026, 10, 27, 12, 25), 1400, 1000, 0));

            List<Media> saved = new ArrayList<>();
            for (Shot shot : shots) {
                User author = people.get(shot.author() % people.size());
                saved.add(storeImage(group, author, shot.label(),
                        shot.at().atZone(ACRE).toInstant(), shot.width(), shot.height()));
            }

            // A foto do perfil de cada autor é a primeira que ele publicou.
            for (User person : people) {
                saved.stream()
                        .filter(m -> m.getOwnerId().equals(person.getId()))
                        .findFirst()
                        .ifPresent(m -> person.setAvatarMediaId(m.getId()));
            }

            Moment corujao = moments.save(new Moment(group.getId(), "Corujão da LetalCompany",
                    "Começou como uma partida rápida depois do almoço comemorativo e virou "
                            + "madrugada inteira. Ninguém foi dormir antes do amanhecer.",
                    LocalDateTime.of(2026, 10, 13, 22, 0).atZone(ACRE).toInstant(),
                    LocalDateTime.of(2026, 10, 14, 6, 0).atZone(ACRE).toInstant(),
                    people.getFirst().getId()));

            Moment almoco = moments.save(new Moment(group.getId(), "Almoço no RU",
                    "O ritual completo: fila, roleta e o sorteado levando todas as bandejas.",
                    LocalDateTime.of(2026, 10, 13, 11, 30).atZone(ACRE).toInstant(),
                    LocalDateTime.of(2026, 10, 13, 14, 0).atZone(ACRE).toInstant(),
                    people.get(1).getId()));

            seedComments(group, people, saved, corujao, almoco);
        }

        private void seedComments(Group group, List<User> people, List<Media> saved,
                Moment corujao, Moment almoco) {
            record Said(String label, int author, String body) {
            }
            List<Said> said = List.of(
                    new Said("A roleta decidiu: Félix", 2, "Justiça poética depois de três semanas escapando"),
                    new Said("A roleta decidiu: Félix", 0, "Eu ainda contesto esse giro"),
                    new Said("A roleta decidiu: Félix", 3, "Girou devagar de propósito, todo mundo viu"),
                    new Said("Bandejas em comboio", 1, "Seis bandejas em duas mãos, recorde do semestre"),
                    new Said("Primeira partida da LetalCompany", 4, "Essa foi a partida em que o Diego apagou as luzes sem avisar"),
                    new Said("Primeira partida da LetalCompany", 5, "E ninguém perdoou"),
                    new Said("Quase deu certo", 0, "Faltaram dois segundos"),
                    new Said("Meia-noite e ninguém desistiu", 6, "Alguém lembrou que tinha aula às 8h?"),
                    new Said("Pausa para o lanche", 2, "O pão de queijo da madrugada merecia um momento próprio"),
                    new Said("Vitória às 3h47", 1, "Gritaria que acordou o prédio inteiro"),
                    new Said("Vitória às 3h47", 3, "Melhor momento da madrugada, sem dúvida"),
                    new Said("O sol chegou antes do sono", 5, "A luz entrando pela janela e a gente ainda jogando"),
                    new Said("Aula das 8h, heroicamente", 4, "Presença garantida, atenção duvidosa"),
                    new Said("Helena girou e escapou", 2, "Sorte de principiante"),
                    new Said("Fila do RU às 11h58", 0, "Dois minutos adiantados, como sempre"));

            for (Said item : said) {
                saved.stream()
                        .filter(m -> m.getCaption() != null && m.getCaption().equals(item.label()))
                        .findFirst()
                        .ifPresent(m -> comments.save(Comment.onMedia(group.getId(),
                                people.get(item.author() % people.size()).getId(),
                                item.body(), m.getId())));
            }

            comments.save(Comment.onMoment(group.getId(), people.get(1).getId(),
                    "Oito horas de jogo e uma pizza. Proposta: repetir no fim do semestre.",
                    corujao.getId()));
            comments.save(Comment.onMoment(group.getId(), people.get(4).getId(),
                    "A melhor madrugada do curso até agora.", corujao.getId()));
            comments.save(Comment.onMoment(group.getId(), people.get(3).getId(),
                    "Esse é o ritual que deu nome ao grupo.", almoco.getId()));
        }

        /**
         * Conteúdo que existe para mostrar as mecânicas da linha do tempo:
         *
         * <ul>
         *   <li>vídeos com miniatura e duração, para ver o player e o selo de duração;</li>
         *   <li>um Momento longo sobreposto ao Corujão, provando que o recorte mais
         *       específico ganha as mídias;</li>
         *   <li>um Momento de dia inteiro e um sem nenhuma mídia;</li>
         *   <li>uma foto com vários comentários, para a caixa animada girar de verdade.</li>
         * </ul>
         *
         * @return true se algo foi criado agora
         */
        private boolean seedExtras(Group group, List<User> people) {
            boolean jaExiste = moments.findByGroupIdOrderByStartsAt(group.getId()).stream()
                    .anyMatch(moment -> "Semana de provas".equals(moment.getTitle()));
            if (jaExiste) {
                return false;
            }

            // --- vídeos (exercitam ffmpeg: miniatura, duração e dimensões) ---
            record Clip(String label, LocalDateTime at, int seconds, int author) {
            }
            List<Clip> clips = List.of(
                    new Clip("A roleta girando em câmera lenta",
                            LocalDateTime.of(2026, 10, 13, 12, 50), 6, 1),
                    new Clip("Partida da LetalCompany às 23h58",
                            LocalDateTime.of(2026, 10, 13, 23, 58), 5, 0),
                    new Clip("O grito da vitória, 3h47",
                            LocalDateTime.of(2026, 10, 14, 3, 47), 4, 3));

            List<Media> savedVideos = new ArrayList<>();
            for (Clip clip : clips) {
                User author = people.get(clip.author() % people.size());
                storeVideo(group, author, clip.label(),
                        clip.at().atZone(ACRE).toInstant(), clip.seconds())
                        .ifPresent(savedVideos::add);
            }

            // --- fotos de um sábado, para o Momento de dia inteiro ---
            List<Media> sabado = new ArrayList<>();
            sabado.add(storeImage(group, people.get(2), "Maratona de série, episódio 1",
                    LocalDateTime.of(2026, 10, 18, 14, 10).atZone(ACRE).toInstant(), 1400, 950));
            sabado.add(storeImage(group, people.get(5), "Pipoca acabou no episódio 4",
                    LocalDateTime.of(2026, 10, 18, 17, 35).atZone(ACRE).toInstant(), 1200, 1300));
            Media discussao = storeImage(group, people.get(1), "A discussão sobre o final",
                    LocalDateTime.of(2026, 10, 18, 22, 5).atZone(ACRE).toInstant(), 1400, 1000);
            sabado.add(discussao);

            // --- Momentos que demonstram as regras ---
            // Longo e sobreposto: o Corujão e o Almoço ficam dentro dele, mas, por serem
            // recortes menores, continuam donos das suas mídias.
            moments.save(new Moment(group.getId(), "Semana de provas",
                    "A semana inteira: estudo, almoços no RU e a madrugada de jogo no meio.",
                    LocalDateTime.of(2026, 10, 12, 0, 0).atZone(ACRE).toInstant(),
                    LocalDateTime.of(2026, 10, 17, 0, 0).atZone(ACRE).toInstant(),
                    people.getFirst().getId()));

            moments.save(new Moment(group.getId(), "Sábado de maratona",
                    "Um dia inteiro, do meio-dia à meia-noite.",
                    LocalDateTime.of(2026, 10, 18, 0, 0).atZone(ACRE).toInstant(),
                    LocalDateTime.of(2026, 10, 19, 0, 0).atZone(ACRE).toInstant(),
                    people.get(2).getId()));

            // Momento sem nenhuma mídia: o título existe e aceita comentários.
            Moment churrasco = moments.save(new Moment(group.getId(),
                    "Churrasco de fim de semestre",
                    "Ninguém tirou foto. Alguém tem alguma?",
                    LocalDateTime.of(2026, 10, 24, 12, 0).atZone(ACRE).toInstant(),
                    LocalDateTime.of(2026, 10, 24, 20, 0).atZone(ACRE).toInstant(),
                    people.get(4).getId()));
            comments.save(Comment.onMoment(group.getId(), people.get(6).getId(),
                    "Eu jurava que tinha gravado o momento da roleta aqui", churrasco.getId()));
            comments.save(Comment.onMoment(group.getId(), people.get(2).getId(),
                    "Procurei no celular e não achei nada. Perdemos esse.", churrasco.getId()));

            // --- conversa: uma foto com muitos comentários, para o carrossel girar ---
            record Said(String author, String body) {
            }
            List<Said> conversa = List.of(
                    new Said("1", "Essa discussão durou mais que o episódio"),
                    new Said("2", "Ainda acho que o final faz sentido"),
                    new Said("3", "Não faz, e eu posso provar com um diagrama"),
                    new Said("4", "Ele realmente desenhou um diagrama"),
                    new Said("5", "E a gente continuou discutindo até às duas da manhã"),
                    new Said("6", "Melhor sábado do semestre, de longe"));
            for (Said said : conversa) {
                comments.save(Comment.onMedia(group.getId(),
                        people.get(Integer.parseInt(said.author()) % people.size()).getId(),
                        said.body(), discussao.getId()));
            }

            // Comentários nos vídeos, para a caixa aparecer sobre o player também.
            if (savedVideos.size() >= 2) {
                comments.save(Comment.onMedia(group.getId(), people.get(3).getId(),
                        "Repare na mão dele segurando a roleta", savedVideos.getFirst().getId()));
                comments.save(Comment.onMedia(group.getId(), people.get(5).getId(),
                        "Câmera lenta foi exagero, mas valeu", savedVideos.getFirst().getId()));
                comments.save(Comment.onMedia(group.getId(), people.get(0).getId(),
                        "Quatro segundos que resumem oito horas", savedVideos.getLast().getId()));
            }
            return true;
        }

        private java.util.Optional<Media> storeVideo(Group group, User author, String label,
                Instant capturedAt, int seconds) {
            var generated = videos.create(label, capturedAt, ACRE, seconds);
            if (generated.isEmpty()) {
                log.info("Sem ffmpeg: vídeo de demonstração '{}' não foi criado.", label);
                return java.util.Optional.empty();
            }
            var processed = videoProcessor.process(generated.get(), ".mp4");
            byte[] bytes = processed.compressed().orElse(generated.get());

            UUID id = UUID.randomUUID();
            String key = "media/seed/" + id + ".mp4";
            storage.store(key, new ByteArrayInputStream(bytes), bytes.length, "video/mp4");

            Media entity = new Media(author.getId(), MediaType.VIDEO, key, "video/mp4",
                    bytes.length, capturedAt, CapturedAtSource.EXIF);
            entity.setCaption(label);
            entity.setWidth(1280);
            entity.setHeight(720);
            processed.duration().ifPresent(entity::setDurationSeconds);
            processed.thumbnail().ifPresent(thumbnail -> {
                String thumbnailKey = "thumbs/seed/" + id + ".jpg";
                storage.store(thumbnailKey, new ByteArrayInputStream(thumbnail),
                        thumbnail.length, "image/jpeg");
                entity.setThumbnailKey(thumbnailKey);
            });

            Media saved = media.save(entity);
            shares.save(new MediaGroupShare(saved.getId(), group.getId(), author.getId()));
            return java.util.Optional.of(saved);
        }

        /** Um segundo grupo, privado e pequeno, para a visibilidade ficar visível. */
        private void seedSecondGroup(List<User> people, String password) {
            if (groups.existsBySlug("turma-de-si")) {
                return;
            }
            Group group = groups.save(new Group("turma-de-si", "Turma de SI 2024",
                    "Grupo privado da turma: trabalhos, provas e o que não cabe no grupo grande.",
                    GroupVisibility.PRIVATE, people.getFirst().getId()));
            members.save(new GroupMember(group.getId(), people.getFirst().getId(), GroupRole.ADMIN));
            members.save(new GroupMember(group.getId(), people.get(2).getId(), GroupRole.MEMBER));

            Media media = storeImage(group, people.getFirst(), "Entrega do trabalho final",
                    LocalDateTime.of(2026, 10, 22, 21, 40).atZone(ACRE).toInstant(), 1400, 1000);
            comments.save(Comment.onMedia(group.getId(), people.get(2).getId(),
                    "21h40, quarenta minutos antes do prazo", media.getId()));
        }

        private Media storeImage(Group group, User author, String label, Instant capturedAt,
                int width, int height) {
            byte[] bytes = images.create(label, capturedAt, ACRE, width, height);
            UUID id = UUID.randomUUID();
            String key = "media/seed/" + id + ".jpg";
            storage.store(key, new ByteArrayInputStream(bytes), bytes.length, "image/jpeg");

            Media entity = new Media(author.getId(), MediaType.IMAGE, key, "image/jpeg",
                    bytes.length, capturedAt, CapturedAtSource.EXIF);
            entity.setWidth(width);
            entity.setHeight(height);
            entity.setCaption(label);
            thumbnails.generate(bytes).ifPresent(thumbnail -> {
                String thumbnailKey = "thumbs/seed/" + id + ".jpg";
                storage.store(thumbnailKey, new ByteArrayInputStream(thumbnail),
                        thumbnail.length, "image/jpeg");
                entity.setThumbnailKey(thumbnailKey);
            });

            Media saved = media.save(entity);
            shares.save(new MediaGroupShare(saved.getId(), group.getId(), author.getId()));
            return saved;
        }
    }
}
