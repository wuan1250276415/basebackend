package com.basebackend.logging.audit.crypto;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.model.AuditLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class AuditSignatureServiceTest {

    private AuditSignatureService signatureService;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        Map<String, KeyPair> keys = new HashMap<>();
        keys.put("test-key", keyPair);

        signatureService = new AuditSignatureService(
                "SHA256withRSA",
                keys,
                new HashMap<>(),
                "test-key"
        );
    }

    // --- sign() ---

    @Test
    void sign_setsSignatureAndCertificateId() {
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);

        AuditLogEntry signed = signatureService.sign(entry);

        assertThat(signed).isSameAs(entry);
        assertThat(signed.getSignature()).isNotNull().isNotEmpty();
        assertThat(signed.getCertificateId()).isEqualTo("test-key");
    }

    @Test
    void sign_nullEntry_returnsNull() {
        assertThat(signatureService.sign(null)).isNull();
    }

    @Test
    void sign_differentEntries_produceDifferentSignatures() {
        AuditLogEntry e1 = buildEntry("user-1", AuditEventType.LOGIN);
        AuditLogEntry e2 = buildEntry("user-2", AuditEventType.LOGOUT);

        signatureService.sign(e1);
        signatureService.sign(e2);

        assertThat(e1.getSignature()).isNotEqualTo(e2.getSignature());
    }

    // --- verify() ---

    @Test
    void verify_nullEntry_returnsFalse() {
        assertThat(signatureService.verify(null)).isFalse();
    }

    @Test
    void verify_entryWithoutSignature_returnsFalse() {
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        assertThat(signatureService.verify(entry)).isFalse();
    }

    @Test
    void verify_signedEntry_noCertificate_returnsFalse() {
        // The verify() method looks up the certificate from certificateStore,
        // which is empty in our test setup, so it returns false
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        signatureService.sign(entry);

        assertThat(signatureService.verify(entry)).isFalse();
    }

    // --- verifyBatch() ---

    @Test
    void verifyBatch_nullList_returnsTrue() {
        assertThat(signatureService.verifyBatch(null)).isTrue();
    }

    @Test
    void verifyBatch_emptyList_returnsTrue() {
        assertThat(signatureService.verifyBatch(List.of())).isTrue();
    }

    // --- rotateKey() ---

    @Test
    void rotateKey_changesActiveKeyId() {
        String originalKeyId = signatureService.getActiveKeyId();

        signatureService.rotateKey();

        assertThat(signatureService.getActiveKeyId()).isNotEqualTo(originalKeyId);
    }

    @Test
    void rotateKey_oldEntriesStillHaveOldKeyId() {
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        signatureService.sign(entry);
        String oldKeyId = entry.getCertificateId();

        signatureService.rotateKey();

        AuditLogEntry newEntry = buildEntry("user-2", AuditEventType.LOGIN);
        signatureService.sign(newEntry);

        assertThat(oldKeyId).isNotEqualTo(newEntry.getCertificateId());
    }

    // --- needsKeyRotation() ---

    @Test
    void needsKeyRotation_freshService_returnsFalse() {
        assertThat(signatureService.needsKeyRotation()).isFalse();
    }

    // --- getAlgorithm() ---

    @Test
    void getAlgorithm_returnsConfiguredAlgorithm() {
        assertThat(signatureService.getAlgorithm()).isEqualTo("SHA256withRSA");
    }

    // --- Default constructor (auto key generation) ---

    @Test
    void defaultConstructor_generatesKeyAutomatically() {
        AuditSignatureService svc = new AuditSignatureService("SHA256withRSA");

        assertThat(svc.getActiveKeyId()).isNotNull();

        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        svc.sign(entry);
        assertThat(entry.getSignature()).isNotNull();
    }

    // --- ECDSA support ---

    @Test
    void ecdsaAlgorithm_signsSuccessfully() {
        AuditSignatureService ecSvc = new AuditSignatureService("SHA256withECDSA");

        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        ecSvc.sign(entry);

        assertThat(entry.getSignature()).isNotNull().isNotEmpty();
    }

    // --- Helpers ---

    private AuditLogEntry buildEntry(String userId, AuditEventType type) {
        return AuditLogEntry.builder()
                .id(java.util.UUID.randomUUID().toString())
                .timestamp(Instant.parse("2025-06-01T12:00:00Z"))
                .userId(userId)
                .eventType(type)
                .resource("/api/test")
                .result("SUCCESS")
                .operation("test")
                .build();
    }
}
