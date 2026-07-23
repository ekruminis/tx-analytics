package com.ekruminis.txanalytics.producer.ingest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@ExtendWith(MockitoExtension.class)
class DatasetResolverTest {

    @Mock
    S3Client s3;

    @Test
    void plainNameResolvesUnderInputDir() {
        DatasetResolver resolver = new DatasetResolver("/data/input", null);

        Path resolved = resolver.resolve("txs-week.json");

        assertThat(resolved).isEqualTo(Path.of("/data/input", "txs-week.json"));
    }

    @Test
    void s3UriDownloadsToTempFile() throws Exception {
        byte[] payload = "[{\"hash\":\"a\"}]".getBytes();
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), payload));

        DatasetResolver resolver = new DatasetResolver("/data/input", s3);
        Path resolved = resolver.resolve("s3://my-bucket/datasets/txs-week.json");

        assertThat(Files.readAllBytes(resolved)).isEqualTo(payload);
    }

    @Test
    void malformedS3UriIsRejected() {
        assertThatThrownBy(() -> DatasetResolver.parse("s3://only-bucket"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
