package com.luiz.webflux.integration;

import com.luiz.webflux.domain.Anime;
import com.luiz.webflux.repository.AnimeRepository;
import com.luiz.webflux.util.AnimeCreator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.blockhound.BlockHound;
import reactor.blockhound.BlockingOperationError;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureWebTestClient
public class AnimeControllerIT {

    private static final String REGULAR_USER = "david";
    private static final String ADMIN_USER = "luiz";

    @MockBean
    private AnimeRepository animeRepository;

    @Autowired
    private WebTestClient client;

    private final Anime anime = AnimeCreator.createValidAnime();

    @BeforeAll
    public static void blockHoundSetup() {
        BlockHound.install();
    }

    @Test
    public void blockHoundWorks() {
        try {
            FutureTask<?> task = new FutureTask<>(() -> {
                Thread.sleep(0);
                return "";
            });
            Schedulers.parallel().schedule(task);

            task.get(10, TimeUnit.SECONDS);
            Assertions.fail("should fail");
        } catch (Exception e) {
            Assertions.assertTrue(e.getCause() instanceof BlockingOperationError);
        }
    }

    @BeforeEach
    public void setUp() {
        BDDMockito.when(animeRepository.findAll()).thenReturn(Flux.just(anime));
        BDDMockito.when(animeRepository.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.just(anime));
        BDDMockito.when(animeRepository.save(AnimeCreator.createAnimeToBeSaved())).thenReturn(Mono.just(anime));
        BDDMockito.when(animeRepository.saveAll(List.of(AnimeCreator.createAnimeToBeSaved(), AnimeCreator.createAnimeToBeSaved()))).thenReturn(Flux.just(anime, anime));
        BDDMockito.when(animeRepository.delete(ArgumentMatchers.any(Anime.class))).thenReturn(Mono.empty());
        BDDMockito.when(animeRepository.save(AnimeCreator.createValidAnime())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("listAll returns unauthorized when user is not authenticate")
    public void listAll_ReturnUnauthorized_WhenIsNotAuthenticated() {
        client
                .get()
                .uri("/anime")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("listAll returns forbidden when user is successfully auth and does not have the role ADMIN")
    @WithUserDetails(REGULAR_USER)
    public void listAll_ReturnForbidden_WhenUserDoesNotHaveRoleAdmin() {
        client
                .get()
                .uri("/anime")
                .exchange()
                .expectStatus().isForbidden();
    }


    @Test
    @DisplayName("listAll returns a flux of anime when user is successfully auth and has the role ADMIN")
    @WithUserDetails(ADMIN_USER)
    public void listAll_ReturnFluxOfAnime_WhenSuccessful() {
        client
                .get()
                .uri("/anime")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.[0].id").isEqualTo(anime.getId())
                .jsonPath("$.[0].name").isEqualTo(anime.getName());
    }


    @Test
    @DisplayName("findAll returns a flux of anime when user is successfully auth and has the role ADMIN")
    @WithUserDetails(ADMIN_USER)
    public void listAll_Flavor2_ReturnFluxOfAnime_WhenSuccessful() {
        client
                .get()
                .uri("/anime")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Anime.class)
                .hasSize(1)
                .contains(anime);
    }

    @Test
    @DisplayName("findById returns Mono with anime when it exists and user is successfully auth and has the role USER")
    @WithUserDetails(REGULAR_USER)
    public void findById_ReturnMonoAnime_WhenSuccessful() {
        client
                .get()
                .uri("/anime/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Anime.class)
                .isEqualTo(anime);
    }

    @Test
    @DisplayName("findById returns Mono error with anime does not exist and user is successfully auth and has the role USER")
    @WithUserDetails(REGULAR_USER)
    public void findById_ReturnMonoError_WhenEmptyMonoIsReturned() {
        BDDMockito.when(animeRepository.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());
        client
                .get()
                .uri("/anime/{id}", 1)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.developerMessage").isEqualTo("A ResponseStatusException Happened");
    }

    @Test
    @DisplayName("saves create an anime when successful and when user is successfully auth and has the role ADMIN")
    @WithUserDetails(ADMIN_USER)
    public void save_CreatesAnime_WhenSuccessful() {
        Anime animeToBeSaved = AnimeCreator.createAnimeToBeSaved();
        client
                .post()
                .uri("/anime")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(animeToBeSaved))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Anime.class)
                .isEqualTo(anime);
    }

    @Test
    @DisplayName("saveBatch create a list of anime when successful and user is successfully auth and has the role ADMIN")
    @WithUserDetails(ADMIN_USER)
    public void saveBatch_CreatesListOfAnime_WhenSuccessful() {
        Anime animeToBeSaved = AnimeCreator.createAnimeToBeSaved();
        client
                .post()
                .uri("/anime/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(List.of(animeToBeSaved, animeToBeSaved)))
                .exchange()
                .expectStatus().isCreated()
                .expectBodyList(Anime.class)
                .hasSize(2)
                .contains(anime);
    }

    @Test
    @DisplayName("saves returns mono error when name is empty and user is successfully auth and has the role ADMIN")
    @WithUserDetails(ADMIN_USER)
    public void save_ReturnError_WhenNameIsEmpty() {
        Anime animeToBeSaved = AnimeCreator.createAnimeToBeSaved().withName("");
        client
                .post()
                .uri("/anime")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(animeToBeSaved))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    @DisplayName("saveBatch return Mono error when list of anime contains null or empty name and user is successfully auth and has the role ADMIN")
    @WithUserDetails(ADMIN_USER)
    public void saveBatch_ReturnError_WhenContainsInvalidName() {
        Anime animeToBeSaved = AnimeCreator.createAnimeToBeSaved().withName("");

        BDDMockito.when(animeRepository.saveAll(ArgumentMatchers.anyIterable())).thenReturn(Flux.just(anime, anime.withName("")));

        client
                .post()
                .uri("/anime")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(List.of(animeToBeSaved, animeToBeSaved)))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    @DisplayName("delete remove the anime when successful and user is successfully auth and has the role ADMIN")
    @WithUserDetails(ADMIN_USER)
    public void delete_RemoveAnime_WhenSuccessful() {
        client
                .delete()
                .uri("/anime/{id}", 1)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("delete returns Mono error when anime does not exists and user is successfully auth and has the role ADMIN")
    @WithUserDetails(ADMIN_USER)
    public void delete_ReturnMonoError_WhenEmptyMonoIsReturned() {
        BDDMockito.when(animeRepository.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());
        client
                .delete()
                .uri("/anime/{id}", 1)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.developerMessage").isEqualTo("A ResponseStatusException Happened");
    }

    @Test
    @DisplayName("update save updated anime and returns empty mono when successful and user is successfully auth and has the role ADMIN")
    @WithUserDetails(ADMIN_USER)
    public void update_SaveUpdateAnime_WhenSuccessful() {
        client
                .put()
                .uri("/anime/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(anime))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("update returns Mono error when anime not exist and user is successfully auth and has the role ADMIN")
    @WithUserDetails(ADMIN_USER)
    public void update_ReturnMonoError_WhenEmptyMonoIsReturned() {
        BDDMockito.when(animeRepository.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());
        client
                .put()
                .uri("/anime/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(anime))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.developerMessage").isEqualTo("A ResponseStatusException Happened");
    }
}
