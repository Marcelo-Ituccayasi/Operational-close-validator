package com.marceloituccayasi.ocv.operationalclose.presentation;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.marceloituccayasi.ocv.operationalclose.application.GetSupportingEvidenceContent;
import com.marceloituccayasi.ocv.operationalclose.application.GetSupportingEvidenceContentResult;
import com.marceloituccayasi.ocv.operationalclose.application.StoredSupportingEvidenceContent;

/**
 * Authenticated HTTP delivery of application-managed Supporting Evidence
 * content.
 */
@Controller
public class SupportingEvidenceContentController {

    private static final String X_CONTENT_TYPE_OPTIONS =
            "X-Content-Type-Options";

    private final GetSupportingEvidenceContent
            getSupportingEvidenceContent;

    public SupportingEvidenceContentController(
            GetSupportingEvidenceContent
                    getSupportingEvidenceContent) {

        this.getSupportingEvidenceContent =
                Objects.requireNonNull(
                        getSupportingEvidenceContent);
    }

    @ResponseBody
    @GetMapping(
            "/closes/{closeId}/events/{eventId}/supporting-evidence/{evidenceId}/content")
    ResponseEntity<byte[]> content(
            @PathVariable String closeId,
            @PathVariable String eventId,
            @PathVariable String evidenceId) {

        UUID parsedCloseId =
                parseUuid(
                        closeId);

        UUID parsedEventId =
                parseUuid(
                        eventId);

        UUID parsedEvidenceId =
                parseUuid(
                        evidenceId);

        if (parsedCloseId == null
                || parsedEventId == null
                || parsedEvidenceId == null) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }

        GetSupportingEvidenceContentResult result =
                getSupportingEvidenceContent.execute(
                        parsedCloseId,
                        parsedEventId,
                        parsedEvidenceId);

        if (result.status()
                == GetSupportingEvidenceContentResult.Status
                        .NOT_FOUND) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        StoredSupportingEvidenceContent storedContent =
                result.content();

        byte[] body =
                storedContent.content();

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.parseMediaType(
                        storedContent.mediaType()));

        headers.setContentLength(
                body.length);

        headers.setCacheControl(
                "no-store");

        headers.set(
                X_CONTENT_TYPE_OPTIONS,
                "nosniff");

        headers.setContentDisposition(
                contentDisposition(
                        parsedEvidenceId,
                        storedContent));

        return new ResponseEntity<>(
                body,
                headers,
                HttpStatus.OK);
    }

    private static ContentDisposition contentDisposition(
            UUID evidenceId,
            StoredSupportingEvidenceContent storedContent) {

        String filename =
                "evidence-"
                        + evidenceId
                        + "."
                        + storedContent.extension();

        return switch (storedContent.mediaType()) {
            case "application/pdf" ->
                ContentDisposition
                        .attachment()
                        .filename(
                                filename)
                        .build();

            case "image/png", "image/jpeg" ->
                ContentDisposition
                        .inline()
                        .filename(
                                filename)
                        .build();

            default ->
                throw new IllegalStateException(
                        "stored supporting evidence media type is not deliverable");
        };
    }

    private static UUID parseUuid(
            String value) {

        try {
            return UUID.fromString(
                    value);
        }
        catch (IllegalArgumentException exception) {
            return null;
        }
    }

}