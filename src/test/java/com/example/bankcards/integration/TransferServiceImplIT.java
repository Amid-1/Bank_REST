package com.example.bankcards.integration;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.entity.card.BankCard;
import com.example.bankcards.entity.card.BankCardStatus;
import com.example.bankcards.entity.user.AppUser;
import com.example.bankcards.entity.user.UserRole;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.TransferRecordsRepository;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.service.card.CardsService;
import com.example.bankcards.service.card.TransferService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TransferServiceImplIT {

    @MockitoBean
    CardsService cardsService;

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("card_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);

        r.add("jwt.secret", () -> "XavvhwWBp7MKtBOk1Cxtds1U8wUCq+l0pYeQHBFtDvY=");
        r.add("jwt.expiration", () -> "86400000");
    }

    @Autowired TransferService transferService;
    @Autowired UsersRepository usersRepository;
    @Autowired CardsRepository cardsRepository;
    @Autowired TransferRecordsRepository transferRecordsRepository;

    private AppUser user;
    private BankCard cardA;
    private BankCard cardB;

    @BeforeEach
    void setUp() {
        transferRecordsRepository.deleteAll();
        cardsRepository.deleteAll();
        usersRepository.deleteAll();

        user = usersRepository.save(AppUser.builder()
                .name("User")
                .email("user@test.local")
                .passwordHash("{noop}x")
                .role(UserRole.ROLE_USER)
                .enabled(true)
                .accountNonLocked(true)
                .build());

        cardA = cardsRepository.save(newCard(user, "1111", new BigDecimal("100.00"), BankCardStatus.ACTIVE, LocalDate.now().plusYears(3)));
        cardB = cardsRepository.save(newCard(user, "2222", new BigDecimal("10.00"), BankCardStatus.ACTIVE, LocalDate.now().plusYears(3)));
    }

    @Test
    void transfer_success_updatesBalancesAndCreatesRecord() {
        var resp = transferService.transfer(user.getId(), new TransferRequest(cardA.getId(), cardB.getId(), new BigDecimal("25.50")));

        assertNotNull(resp.id());
        assertNotNull(resp.createdAt());
        assertEquals(cardA.getId(), resp.fromCardId());
        assertEquals(cardB.getId(), resp.toCardId());
        assertEquals(new BigDecimal("25.50"), resp.amount());

        BankCard a = cardsRepository.findById(cardA.getId()).orElseThrow();
        BankCard b = cardsRepository.findById(cardB.getId()).orElseThrow();

        assertEquals(new BigDecimal("74.50"), a.getBalance());
        assertEquals(new BigDecimal("35.50"), b.getBalance());

        assertEquals(1, transferRecordsRepository.count());
    }

    @Test
    void transfer_insufficientFunds_throwsIllegalState_andNoRecordCreated() {
        assertThrows(IllegalStateException.class, () ->
                transferService.transfer(user.getId(), new TransferRequest(cardA.getId(), cardB.getId(), new BigDecimal("999.00")))
        );

        BankCard a = cardsRepository.findById(cardA.getId()).orElseThrow();
        BankCard b = cardsRepository.findById(cardB.getId()).orElseThrow();

        assertEquals(new BigDecimal("100.00"), a.getBalance());
        assertEquals(new BigDecimal("10.00"), b.getBalance());
        assertEquals(0, transferRecordsRepository.count());
    }

    @Test
    void transfer_fromEqualsTo_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(user.getId(), new TransferRequest(cardA.getId(), cardA.getId(), new BigDecimal("1.00")))
        );
        assertEquals(0, transferRecordsRepository.count());
    }

    @Test
    void transfer_scaleTooHigh_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(user.getId(), new TransferRequest(cardA.getId(), cardB.getId(), new BigDecimal("10.001")))
        );
        assertEquals(0, transferRecordsRepository.count());
    }

    @Test
    void transfer_blockedOrExpired_throwsIllegalState() {
        // blocked
        BankCard blocked = cardsRepository.save(newCard(user, "3333", new BigDecimal("50.00"), BankCardStatus.BLOCKED, LocalDate.now().plusYears(2)));
        assertThrows(IllegalStateException.class, () ->
                transferService.transfer(user.getId(), new TransferRequest(blocked.getId(), cardB.getId(), new BigDecimal("1.00")))
        );

        // expired
        BankCard expired = cardsRepository.save(newCard(user, "4444", new BigDecimal("50.00"), BankCardStatus.ACTIVE, LocalDate.now().minusDays(1)));
        assertThrows(IllegalStateException.class, () ->
                transferService.transfer(user.getId(), new TransferRequest(expired.getId(), cardB.getId(), new BigDecimal("1.00")))
        );

        assertEquals(0, transferRecordsRepository.count());
    }

    @Test
    void concurrentTransfers_shouldNotOverspend_oneSucceedsOneFails() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> task = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                transferService.transfer(user.getId(), new TransferRequest(cardA.getId(), cardB.getId(), new BigDecimal("80.00")));
                return true;
            } catch (IllegalStateException ex) {
                return false;
            }
        };

        Future<Boolean> f1 = pool.submit(task);
        Future<Boolean> f2 = pool.submit(task);

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        boolean r1 = f1.get(10, TimeUnit.SECONDS);
        boolean r2 = f2.get(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        int success = (r1 ? 1 : 0) + (r2 ? 1 : 0);
        int fail = 2 - success;

        assertEquals(1, success);
        assertEquals(1, fail);

        BankCard a = cardsRepository.findById(cardA.getId()).orElseThrow();
        BankCard b = cardsRepository.findById(cardB.getId()).orElseThrow();

        assertEquals(new BigDecimal("20.00"), a.getBalance());
        assertEquals(new BigDecimal("90.00"), b.getBalance());
        assertEquals(1, transferRecordsRepository.count());
    }

    @Test
    void oppositeDirectionTransfers_shouldNotDeadlock() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Void> t1 = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            transferService.transfer(user.getId(), new TransferRequest(cardA.getId(), cardB.getId(), new BigDecimal("1.00")));
            return null;
        };
        Callable<Void> t2 = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            transferService.transfer(user.getId(), new TransferRequest(cardB.getId(), cardA.getId(), new BigDecimal("1.00")));
            return null;
        };

        Future<Void> f1 = pool.submit(t1);
        Future<Void> f2 = pool.submit(t2);

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        assertDoesNotThrow(() -> f1.get(10, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> f2.get(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        BankCard a = cardsRepository.findById(cardA.getId()).orElseThrow();
        BankCard b = cardsRepository.findById(cardB.getId()).orElseThrow();

        assertEquals(new BigDecimal("100.00"), a.getBalance());
        assertEquals(new BigDecimal("10.00"), b.getBalance());
        assertEquals(2, transferRecordsRepository.count());
    }

    private static BankCard newCard(AppUser owner,
                                    String last4,
                                    BigDecimal balance,
                                    BankCardStatus status,
                                    LocalDate exp) {

        String panHash64 = random64Hex();
        return BankCard.builder()
                .owner(owner)
                .panHash(panHash64)
                .maskedCardNumber("**** **** **** " + last4)
                .encryptedCardNumber("iv:cipher")
                .expirationDate(exp)
                .balance(balance)
                .status(status)
                .build();
    }

    private static String random64Hex() {
        String u = UUID.randomUUID().toString().replace("-", "");
        return (u + u).toLowerCase();
    }
}
