package com.marceloituccayasi.ocv.operationalclose.application.port;

import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.application.StoredSupportingEvidenceContent;
import com.marceloituccayasi.ocv.operationalclose.application.SupportingEvidenceStorageReference;

/**
 * Physical storage contract for application-managed Supporting Evidence
 * content.
 *
 * <p>This port accepts only validated {@code stored:} references. Textual
 * {@code reference:} values must never be passed to this contract.
 *
 * <p>Deletion is reserved for compensation after an unsuccessful database
 * link. Logical evidence deactivation must not delete historical content.
 */
public interface SupportingEvidenceContentStorage {

    void store(
            SupportingEvidenceStorageReference reference,
            byte[] content);

    Optional<StoredSupportingEvidenceContent> find(
            SupportingEvidenceStorageReference reference);

    void delete(
            SupportingEvidenceStorageReference reference);

}