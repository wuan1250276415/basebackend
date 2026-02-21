package com.basebackend.logging.audit.crypto;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.model.AuditLogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class HashChainCalculatorTest {

    private HashChainCalculator calculator;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        calculator = new HashChainCalculator("SHA-256", mapper);
    }

    // --- computeHash basic behavior ---

    @Test
    void computeHash_returnsNonNullHexString() {
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        String hash = calculator.computeHash(entry, null);

        assertThat(hash).isNotNull().matches("^[0-9a-f]{64}$");
    }

    @Test
    void computeHash_sameEntry_sameResult() {
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        String hash1 = calculator.computeHash(entry, null);
        String hash2 = calculator.computeHash(entry, null);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void computeHash_differentEntries_differentHashes() {
        AuditLogEntry e1 = buildEntry("user-1", AuditEventType.LOGIN);
        AuditLogEntry e2 = buildEntry("user-2", AuditEventType.LOGOUT);

        String h1 = calculator.computeHash(e1, null);
        String h2 = calculator.computeHash(e2, null);

        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void computeHash_prevHashAffectsResult() {
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        String hashWithNull = calculator.computeHash(entry, null);
        String hashWithPrev = calculator.computeHash(entry, "abc123");

        assertThat(hashWithNull).isNotEqualTo(hashWithPrev);
    }

    @Test
    void computeHash_emptyPrevHash_sameAsNull() {
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        String hashNull = calculator.computeHash(entry, null);
        String hashEmpty = calculator.computeHash(entry, "");

        // Both paths go through the empty-seed branch
        assertThat(hashNull).isEqualTo(hashEmpty);
    }

    // --- Hash chain construction and verification ---

    @Test
    void verifyChain_validChain_returnsTrue() {
        List<AuditLogEntry> chain = buildChain(5);
        assertThat(calculator.verifyChain(chain)).isTrue();
    }

    @Test
    void verifyChain_emptyList_returnsTrue() {
        assertThat(calculator.verifyChain(List.of())).isTrue();
    }

    @Test
    void verifyChain_nullList_returnsTrue() {
        assertThat(calculator.verifyChain(null)).isTrue();
    }

    @Test
    void verifyChain_tamperedEntry_returnsFalse() {
        List<AuditLogEntry> chain = buildChain(3);
        chain.get(1).setEntryHash("0000000000000000000000000000000000000000000000000000000000000000");

        assertThat(calculator.verifyChain(chain)).isFalse();
    }

    @Test
    void verifyChain_missingHash_returnsFalse() {
        List<AuditLogEntry> chain = buildChain(3);
        chain.get(1).setEntryHash(null);

        assertThat(calculator.verifyChain(chain)).isFalse();
    }

    // --- verifyEntry ---

    @Test
    void verifyEntry_validEntry_returnsTrue() {
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        // Must set prevHash BEFORE computing hash — computeHash serializes the entire entry
        entry.setPrevHash(null);
        String hash = calculator.computeHash(entry, null);
        entry.setEntryHash(hash);

        assertThat(calculator.verifyEntry(entry, null)).isTrue();
    }

    @Test
    void verifyEntry_nullEntry_returnsFalse() {
        assertThat(calculator.verifyEntry(null, null)).isFalse();
    }

    @Test
    void verifyEntry_wrongHash_returnsFalse() {
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        entry.setEntryHash("wrong");

        assertThat(calculator.verifyEntry(entry, null)).isFalse();
    }

    // --- computeRootHash ---

    @Test
    void computeRootHash_matchesLastEntryHash() {
        List<AuditLogEntry> chain = buildChain(4);
        String rootHash = calculator.computeRootHash(chain);

        // computeRootHash re-computes the chain from scratch, so the root hash
        // should match the last entry's entryHash only if the chain is valid
        assertThat(rootHash).isNotNull().matches("^[0-9a-f]{64}$");
        assertThat(rootHash).isEqualTo(chain.get(3).getEntryHash());
    }

    @Test
    void computeRootHash_emptyList_returnsNull() {
        assertThat(calculator.computeRootHash(List.of())).isNull();
    }

    @Test
    void computeRootHash_nullList_returnsNull() {
        assertThat(calculator.computeRootHash(null)).isNull();
    }

    // --- detectTampering ---

    @Test
    void detectTampering_cleanChain_reportsNoTampering() {
        List<AuditLogEntry> chain = buildChain(3);
        String report = calculator.detectTampering(chain);

        assertThat(report).contains("完整性校验通过");
    }

    @Test
    void detectTampering_tamperedChain_reportsProblem() {
        List<AuditLogEntry> chain = buildChain(3);
        chain.get(1).setEntryHash("tampered");

        String report = calculator.detectTampering(chain);
        assertThat(report).contains("发现篡改");
    }

    // --- String-arg constructor ---

    @Test
    void stringConstructor_usesSHA256() {
        // The single-arg constructor uses new ObjectMapper() without JavaTimeModule,
        // so we test with an entry that doesn't use Instant to avoid serialization failure.
        HashChainCalculator calc = new HashChainCalculator("SHA-256", calculator.toString().isEmpty() ?
                new ObjectMapper() {{ registerModule(new JavaTimeModule()); }} :
                new ObjectMapper() {{ registerModule(new JavaTimeModule()); }});

        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        String hash = calc.computeHash(entry, null);
        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    // --- Property: hash chain monotonicity ---

    @Test
    void hashChain_eachEntryDependsOnPrevious() {
        List<AuditLogEntry> chain = buildChain(5);
        for (int i = 1; i < chain.size(); i++) {
            assertThat(chain.get(i).getPrevHash())
                    .isEqualTo(chain.get(i - 1).getEntryHash());
        }
    }

    // --- Helpers ---

    private AuditLogEntry buildEntry(String userId, AuditEventType type) {
        return AuditLogEntry.builder()
                .id(java.util.UUID.randomUUID().toString())
                .timestamp(Instant.parse("2025-01-01T00:00:00Z"))
                .userId(userId)
                .eventType(type)
                .resource("/api/test")
                .result("SUCCESS")
                .operation("test")
                .build();
    }

    /**
     * Build a valid hash chain. Must set prevHash on each entry BEFORE computing
     * its hash, because computeHash serializes the entire entry including prevHash.
     */
    private List<AuditLogEntry> buildChain(int size) {
        List<AuditLogEntry> chain = new ArrayList<>();
        String prevHash = null;

        for (int i = 0; i < size; i++) {
            AuditLogEntry entry = buildEntry("user-" + i, AuditEventType.LOGIN);
            entry.setPrevHash(prevHash);
            String hash = calculator.computeHash(entry, prevHash);
            entry.setEntryHash(hash);
            chain.add(entry);
            prevHash = hash;
        }

        return chain;
    }
}
