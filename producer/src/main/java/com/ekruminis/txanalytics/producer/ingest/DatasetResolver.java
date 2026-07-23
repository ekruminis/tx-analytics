package com.ekruminis.txanalytics.producer.ingest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Component
public class DatasetResolver {

    private static final Logger log = LoggerFactory.getLogger(DatasetResolver.class);
    private static final String S3_SCHEME = "s3://";

    private final String inputDir;
    private final S3Client s3;

    public DatasetResolver(@Value("${producer.input-dir}") String inputDir,
                           S3Client s3) {
        this.inputDir = inputDir;
        this.s3 = s3;
    }

    public Path resolve(String dataset) {
        if (dataset != null && dataset.startsWith(S3_SCHEME)) {
            return downloadFromS3(dataset);
        }
        return Paths.get(inputDir, dataset);
    }

    private Path downloadFromS3(String uri) {
        S3Location loc = parse(uri);
        try {
            ResponseBytes<GetObjectResponse> object = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(loc.bucket()).key(loc.key()).build());
            Path tmp = Files.createTempFile("dataset-", ".json");
            tmp.toFile().deleteOnExit();
            Files.write(tmp, object.asByteArray());
            log.info("downloaded s3://{}/{} ({} bytes) -> {}",
                    loc.bucket(), loc.key(), object.asByteArray().length, tmp);
            return tmp;
        } catch (Exception e) {
            throw new IllegalStateException("failed to download dataset from " + uri, e);
        }
    }

    static S3Location parse(String uri) {
        String withoutScheme = uri.substring(S3_SCHEME.length());
        int slash = withoutScheme.indexOf('/');
        if (slash < 1 || slash == withoutScheme.length() - 1) {
            throw new IllegalArgumentException("malformed S3 URI (expected s3://bucket/key): " + uri);
        }
        return new S3Location(withoutScheme.substring(0, slash), withoutScheme.substring(slash + 1));
    }

    record S3Location(String bucket, String key) {
    }
}
