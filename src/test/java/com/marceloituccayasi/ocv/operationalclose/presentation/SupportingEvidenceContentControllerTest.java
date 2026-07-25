package com.marceloituccayasi.ocv.operationalclose.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.marceloituccayasi.ocv.operationalclose.application.GetSupportingEvidenceContent;
import com.marceloituccayasi.ocv.operationalclose.application.GetSupportingEvidenceContentResult;
import com.marceloituccayasi.ocv.operationalclose.application.StoredSupportingEvidenceContent;
import com.marceloituccayasi.ocv.operationalclose.application.SupportingEvidenceStorageReference;

class SupportingEvidenceContentControllerTest {

    private static final UUID CLOSE_UUID =
            UUID.fromString(
                    "7d1bdc81-f358-43cd-82dc-670345100001");

    private static final UUID EVENT_UUID =
            UUID.fromString(
                    "7d1bdc81-f358-43cd-82dc-670345100002");

    private static final UUID EVIDENCE_UUID =
            UUID.fromString(
                    "7d1bdc81-f358-43cd-82dc-670345100003");

    private static final byte[] CONTENT =
            new byte[] {
                    1,
                    2,
                    3,
                    4
            };

    private final GetSupportingEvidenceContent
            getSupportingEvidenceContent =
                    mock(
                            GetSupportingEvidenceContent.class);

    private MockMvc mockMvc;

    @BeforeEach
    void configureMvc() {
        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(
                                new SupportingEvidenceContentController(
                                        getSupportingEvidenceContent))
                        .build();
    }

    @Test
    void deliversPdfAsAttachmentWithSecurityHeaders()
            throws Exception {

        StoredSupportingEvidenceContent storedContent =
                storedContent(
                        "pdf");

        when(
                getSupportingEvidenceContent.execute(
                        CLOSE_UUID,
                        EVENT_UUID,
                        EVIDENCE_UUID))
                .thenReturn(
                        GetSupportingEvidenceContentResult
                                .found(
                                        storedContent));

        MvcResult mvcResult =
                mockMvc.perform(
                                get(
                                        contentRoute()))
                        .andExpect(
                                status().isOk())
                        .andExpect(
                                content().bytes(
                                        CONTENT))
                        .andExpect(
                                content().contentType(
                                        MediaType.APPLICATION_PDF))
                        .andExpect(
                                header().string(
                                        HttpHeaders.CACHE_CONTROL,
                                        "no-store"))
                        .andExpect(
                                header().string(
                                        "X-Content-Type-Options",
                                        "nosniff"))
                        .andExpect(
                                header().longValue(
                                        HttpHeaders.CONTENT_LENGTH,
                                        CONTENT.length))
                        .andReturn();

        ContentDisposition disposition =
                ContentDisposition.parse(
                        mvcResult.getResponse()
                                .getHeader(
                                        HttpHeaders.CONTENT_DISPOSITION));

        assertThat(disposition.getType())
                .isEqualTo(
                        "attachment");

        assertThat(disposition.getFilename())
                .isEqualTo(
                        "evidence-"
                                + EVIDENCE_UUID
                                + ".pdf");
    }

    @Test
    void deliversImageInline()
            throws Exception {

        StoredSupportingEvidenceContent storedContent =
                storedContent(
                        "png");

        when(
                getSupportingEvidenceContent.execute(
                        CLOSE_UUID,
                        EVENT_UUID,
                        EVIDENCE_UUID))
                .thenReturn(
                        GetSupportingEvidenceContentResult
                                .found(
                                        storedContent));

        MvcResult mvcResult =
                mockMvc.perform(
                                get(
                                        contentRoute()))
                        .andExpect(
                                status().isOk())
                        .andExpect(
                                content().contentType(
                                        MediaType.IMAGE_PNG))
                        .andReturn();

        ContentDisposition disposition =
                ContentDisposition.parse(
                        mvcResult.getResponse()
                                .getHeader(
                                        HttpHeaders.CONTENT_DISPOSITION));

        assertThat(disposition.getType())
                .isEqualTo(
                        "inline");

        assertThat(disposition.getFilename())
                .isEqualTo(
                        "evidence-"
                                + EVIDENCE_UUID
                                + ".png");
    }

    @Test
    void returnsNotFoundWhenEvidenceOrStoredContentIsUnavailable()
            throws Exception {

        when(
                getSupportingEvidenceContent.execute(
                        CLOSE_UUID,
                        EVENT_UUID,
                        EVIDENCE_UUID))
                .thenReturn(
                        GetSupportingEvidenceContentResult
                                .notFound());

        mockMvc.perform(
                        get(
                                contentRoute()))
                .andExpect(
                        status().isNotFound())
                .andExpect(
                        content().bytes(
                                new byte[0]));
    }

    @Test
    void rejectsMalformedIdentifierBeforeExecutingApplicationQuery()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/closes/not-a-uuid/events/"
                                        + EVENT_UUID
                                        + "/supporting-evidence/"
                                        + EVIDENCE_UUID
                                        + "/content"))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        content().bytes(
                                new byte[0]));

        verifyNoInteractions(
                getSupportingEvidenceContent);
    }

    private static String contentRoute() {
        return "/closes/"
                + CLOSE_UUID
                + "/events/"
                + EVENT_UUID
                + "/supporting-evidence/"
                + EVIDENCE_UUID
                + "/content";
    }

    private static StoredSupportingEvidenceContent
            storedContent(
                    String extension) {

        SupportingEvidenceStorageReference reference =
                SupportingEvidenceStorageReference.parse(
                        "stored:evidence/"
                                + EVIDENCE_UUID
                                + "/"
                                + sha256Hex(
                                        CONTENT)
                                + "."
                                + extension);

        return new StoredSupportingEvidenceContent(
                reference,
                CONTENT);
    }

    private static String sha256Hex(
            byte[] content) {

        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance(
                                    "SHA-256")
                                    .digest(
                                            content));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    exception);
        }
    }

}