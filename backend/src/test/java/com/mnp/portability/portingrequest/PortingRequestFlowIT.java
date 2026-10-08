package com.mnp.portability.portingrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.mnp.portability.operator.Operator;
import com.mnp.portability.operator.OperatorRepository;
import com.mnp.portability.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Whole-application tests against a real MySQL: Flyway schema, JPA mappings, the visibility query,
 * the unique index on pending numbers, and the timeout rule, with time controlled by a mutable clock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = "porting.timeout-check-interval=PT1H")   // the test, not the scheduler, triggers sweeps
class PortingRequestFlowIT {

    private static final String URL = "/api/v1/porting-requests";
    private static final String VODAFONE_NUMBER = "01012345678";
    private static final String ETISALAT_NUMBER = "01112345678";

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @TestConfiguration
    static class ClockConfiguration {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-10-07T10:00:00Z"));
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MutableClock clock;
    @Autowired
    private PortingRequestRepository repository;
    @Autowired
    private OperatorRepository operatorRepository;
    @Autowired
    private PortingRequestService portingRequestService;

    @BeforeEach
    void cleanSlate() {
        repository.deleteAll();
        clock.reset();
    }

    // ---- the whole story ----------------------------------------------------------------------

    @Test
    void portingLifecycleFromSubmissionToOwnershipChange() throws Exception {
        int first = submit("orange", VODAFONE_NUMBER);

        // The same number cannot have two pending requests, whoever asks.
        trySubmit("etisalat", VODAFONE_NUMBER).andExpect(status().isConflict());

        // A pending request is invisible to a third party, visible to its parties.
        getRequest("etisalat", first).andExpect(status().isNotFound());
        getRequest("orange", first).andExpect(status().isOk());
        getRequest("vodafone", first).andExpect(status().isOk());

        // Only the donor decides.
        decide("orange", first, "accept").andExpect(status().isForbidden());
        decide("vodafone", first, "accept")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        decide("vodafone", first, "reject").andExpect(status().isConflict());

        // Accepted requests are public, and the number now belongs to Orange.
        getRequest("etisalat", first).andExpect(status().isOk());
        phoneNumberStatus("etisalat", VODAFONE_NUMBER)
                .andExpect(jsonPath("$.status").value("PORTED"))
                .andExpect(jsonPath("$.currentOperator").value("orange"))
                .andExpect(jsonPath("$.originalOperator").value("vodafone"));

        // The next request for that number has Orange as the donor, not Vodafone.
        trySubmit("etisalat", VODAFONE_NUMBER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donor").value("orange"))
                .andExpect(jsonPath("$.recipient").value("etisalat"));
    }

    // ---- visibility ---------------------------------------------------------------------------

    @Test
    void listShowsEachOperatorOnlyWhatItMaySee() throws Exception {
        int orangeToVodafone = submit("orange", VODAFONE_NUMBER);       // donor vodafone, recipient orange
        int vodafoneToEtisalat = submit("vodafone", ETISALAT_NUMBER);   // donor etisalat, recipient vodafone

        list("orange", "").andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(orangeToVodafone));
        list("etisalat", "").andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(vodafoneToEtisalat));
        list("vodafone", "").andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(vodafoneToEtisalat));   // newest first

        // Once accepted, the third party (orange) sees it too.
        decide("etisalat", vodafoneToEtisalat, "accept").andExpect(status().isOk());
        list("orange", "").andExpect(jsonPath("$.content.length()").value(2));
        list("orange", "?status=PENDING").andExpect(jsonPath("$.content.length()").value(1));
        list("orange", "?status=ACCEPTED").andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void pendingRequestIsHiddenFromThirdPartiesInThePhoneNumberStatusToo() throws Exception {
        submit("orange", VODAFONE_NUMBER);

        phoneNumberStatus("vodafone", VODAFONE_NUMBER).andExpect(jsonPath("$.status").value("PORTING_PENDING"));
        phoneNumberStatus("orange", VODAFONE_NUMBER).andExpect(jsonPath("$.status").value("PORTING_PENDING"));
        phoneNumberStatus("etisalat", VODAFONE_NUMBER).andExpect(jsonPath("$.status").value("NOT_PORTED"));
    }

    // ---- timeout ------------------------------------------------------------------------------

    @Test
    void sweepCancelsStaleRequestsAndFreesTheNumber() throws Exception {
        int id = submit("orange", VODAFONE_NUMBER);
        clock.advance(Duration.ofMinutes(3));

        assertThat(portingRequestService.cancelExpired()).isEqualTo(1);

        getRequest("vodafone", id).andExpect(jsonPath("$.status").value("CANCELED"));
        decide("vodafone", id, "accept").andExpect(status().isConflict());
        trySubmit("orange", VODAFONE_NUMBER).andExpect(status().isCreated());   // the number is free again
    }

    @Test
    void sweepLeavesFreshRequestsAlone() throws Exception {
        int id = submit("orange", VODAFONE_NUMBER);
        clock.advance(Duration.ofSeconds(90));

        assertThat(portingRequestService.cancelExpired()).isZero();
        getRequest("vodafone", id).andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void donorCannotAcceptAfterTheDeadlineEvenBeforeTheSweepRuns() throws Exception {
        int id = submit("orange", VODAFONE_NUMBER);
        clock.advance(Duration.ofMinutes(3));

        decide("vodafone", id, "accept").andExpect(status().isConflict());
        getRequest("vodafone", id).andExpect(jsonPath("$.status").value("PENDING"));   // not swept yet
    }

    // ---- database guard -----------------------------------------------------------------------

    @Test
    void databaseItselfRefusesASecondPendingRequestForTheSameNumber() {
        Operator vodafone = operatorRepository.findByCode("vodafone").orElseThrow();
        Operator orange = operatorRepository.findByCode("orange").orElseThrow();
        Operator etisalat = operatorRepository.findByCode("etisalat").orElseThrow();
        Instant now = clock.instant();

        repository.saveAndFlush(PortingRequest.open(VODAFONE_NUMBER, vodafone, orange, now));

        assertThatThrownBy(() ->
                repository.saveAndFlush(PortingRequest.open(VODAFONE_NUMBER, vodafone, etisalat, now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---- helpers ------------------------------------------------------------------------------

    private int submit(String organization, String number) throws Exception {
        String json = trySubmit(organization, number)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private ResultActions trySubmit(String organization, String number) throws Exception {
        return mockMvc.perform(post(URL)
                .header("organization", organization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + number + "\"}"));
    }

    private ResultActions decide(String organization, int id, String action) throws Exception {
        return mockMvc.perform(post(URL + "/" + id + "/" + action).header("organization", organization));
    }

    private ResultActions getRequest(String organization, int id) throws Exception {
        return mockMvc.perform(get(URL + "/" + id).header("organization", organization));
    }

    private ResultActions list(String organization, String query) throws Exception {
        return mockMvc.perform(get(URL + query).header("organization", organization))
                .andExpect(status().isOk());
    }

    private ResultActions phoneNumberStatus(String organization, String number) throws Exception {
        return mockMvc.perform(get("/api/v1/phone-numbers/" + number).header("organization", organization))
                .andExpect(status().isOk());
    }
}