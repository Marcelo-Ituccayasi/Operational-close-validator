package com.marceloituccayasi.ocv.operationalclose.infrastructure.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class RandomUuidGeneratorTest {

    private final RandomUuidGenerator generator =
            new RandomUuidGenerator();

    @Test
    void generatesRandomVersionFourUuid() {
        UUID generatedUuid =
                generator.next();

        assertThat(generatedUuid)
                .isNotNull();

        assertThat(generatedUuid.version())
                .isEqualTo(4);
    }

    @Test
    void generatesDifferentValuesAcrossInvocations() {
        UUID firstUuid =
                generator.next();

        UUID secondUuid =
                generator.next();

        assertThat(secondUuid)
                .isNotEqualTo(firstUuid);
    }

}
