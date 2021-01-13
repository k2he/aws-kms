package com.bina.ist.common.media.s3;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.bina.ist.auth.StsToken;
import com.bina.ist.common.media.api.MediaInputStream;
import com.bina.ist.common.media.api.MediaStorage;
import com.bina.ist.common.media.api.MediaUri;
import com.bina.ist.common.media.api.acl.AclPermission;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.S3Object;

@Slf4j
public class S3MediaStorage implements MediaStorage {

  private static final String APPLICATION_X_DIRECTORY = "application/x-directory";
  private final S3 s3;
  private final S3v2 s3v2;

  public S3MediaStorage() {
    this(Regions.getCurrentRegion().getName());
  }

  public S3MediaStorage(final String region) {
    this.s3 = new S3();
    this.s3v2 = new S3v2(region);
  }

  private static AmazonS3URI toAmazonUri(final MediaUri mediaUri) {
    final URI uri = mediaUri.toUri();
    if (uri.getScheme().equals("s3a")) {
      return new AmazonS3URI(URI.create(uri.toString().replaceFirst("s3a", "s3")));
    }
    return new AmazonS3URI(uri);
  }

  @Override
  public long getContentLength(final MediaUri uri) {
    final AmazonS3URI amazonUri = toAmazonUri(uri);
    return s3v2.getContentLength(amazonUri.getBucket(), amazonUri.getKey());
  }

  @Override
  public MediaUri copy(final Path source, final MediaUri target) {
    final AmazonS3URI targetUri = toAmazonUri(target);
    return MediaUri.from(s3v2.upload(source.toFile(), targetUri.getBucket(), targetUri.getKey()));
  }

  @Override
  public MediaUri copy(final MediaUri source, final MediaUri target) {
    final AmazonS3URI targetUri = toAmazonUri(target);
    final AmazonS3URI sourceUri = toAmazonUri(source);
    return MediaUri.from(s3v2.copy(sourceUri.getBucket(), targetUri.getBucket(), sourceUri.getKey()));
  }

  @Override
  public MediaUri copyEncrypted(final Path source, final MediaUri target, final String customerMasterKey,
      final StsToken stsToken) {
    final AmazonS3URI amazonUri = toAmazonUri(target);
    return MediaUri
        .from(
            s3.uploadEncryptedWithKmsKey(source.toFile(), amazonUri.getBucket(), amazonUri.getKey(), customerMasterKey,
                stsToken));
  }

  @Override
  public MediaUri uploadAndCreatePath(final byte[] content, final String contentType, final MediaUri target,
      final AclPermission permission) {
    final AmazonS3URI amazonUri = toAmazonUri(target);
    return MediaUri
        .from(s3v2.upload(content, contentType, amazonUri.getBucket(), amazonUri.getKey(), convert(permission)));
  }

  @Override
  public MediaUri uploadAndCreatePath(final byte[] content, final String contentType, final MediaUri target) {
    final AmazonS3URI amazonUri = toAmazonUri(target);
    return MediaUri
        .from(s3v2.upload(content, contentType, amazonUri.getBucket(), amazonUri.getKey(), null));
  }

  @Override
  public void download(final MediaUri source, final Path target) {
    try {
      try (final MediaInputStream inputStream = newInputStream(source)) {
        final long bytes = Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Copied {} to {} with {} bytes", source, target, bytes);
      }
    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public MediaInputStream newInputStream(final MediaUri uri) {
    final AmazonS3URI amazonUri = toAmazonUri(uri);
    return new S3MediaInputStream(s3v2.getObject(amazonUri.getBucket(), amazonUri.getKey()));
  }

  @Override
  public MediaInputStream newInputStream(final MediaUri uri, final long start, final long end) {
    final AmazonS3URI amazonUri = toAmazonUri(uri);
    return new S3MediaInputStream(
        s3v2.getObject(amazonUri.getBucket(), amazonUri.getKey(), start, end));
  }

  @Override
  public MediaInputStream newDecryptedInputStream(final MediaUri uri, final String customerMasterKey,
      final StsToken stsToken) {
    final AmazonS3URI amazonUri = toAmazonUri(uri);
    return new S3MediaInputStream(
        s3.getDecryptedObjectWithKmsKey(amazonUri.getBucket(), amazonUri.getKey(), customerMasterKey, stsToken));
  }

  @Override
  public boolean exists(final MediaUri uri) {
    final AmazonS3URI amazonUri = toAmazonUri(uri);
    log.info("Checking if exists Bucket {}, Key {}", amazonUri.getBucket(), amazonUri.getKey());
    return s3v2.isDirectory(amazonUri.getBucket(), amazonUri.getKey())
        || s3v2.objectExists(amazonUri.getBucket(), amazonUri.getKey());
  }

  @Override
  public boolean exists(final MediaUri uri, final String customerMasterKey, final StsToken stsToken) {
    final AmazonS3URI amazonUri = toAmazonUri(uri);
    log.info("Checking if exists Bucket {}, Key {}", amazonUri.getBucket(), amazonUri.getKey());
    return s3.isDirectory(amazonUri.getBucket(), amazonUri.getKey(), customerMasterKey, stsToken) || s3
        .objectExists(amazonUri.getBucket(), amazonUri.getKey(), customerMasterKey, stsToken);
  }

  @Override
  public boolean isDirectory(final MediaUri uri) {
    final AmazonS3URI amazonUri = toAmazonUri(uri);
    return s3v2.isDirectory(amazonUri.getBucket(), amazonUri.getKey());
  }

  @Override
  public boolean isDirectory(final MediaUri uri, final String customerMasterKey, final StsToken stsToken) {
    final AmazonS3URI amazonUri = toAmazonUri(uri);
    return s3.isDirectory(amazonUri.getBucket(), amazonUri.getKey(), customerMasterKey, stsToken);
  }

  /**
   * This needs to be replaced with the S3.isDirectory method, or the other way around we shouldn't have two
   * implementations. For fear of breaking things I will leave it now.
   */
  private boolean isDirectory(final ObjectMetadata metadata) {
    return metadata.getContentType().equals(APPLICATION_X_DIRECTORY);
  }

  @Override
  public MediaUri newHttpUploadUrl(final MediaUri uri, final String md5, final Instant expiration) {
    final AmazonS3URI amazonUri = toAmazonUri(uri);
    return MediaUri.from(s3v2.generatePreSignedUrl(amazonUri.getBucket(), amazonUri.getKey(), md5, expiration));
  }

  @Override
  public Collection<MediaUri> listContent(final MediaUri directory) {
    final AmazonS3URI amazonUri = toAmazonUri(directory);
    return s3v2.getObjectList(amazonUri.getBucket(), amazonUri.getKey())
        .contents()
        .stream()
        .map(obj -> MediaUri.from(s3v2.getUrl(amazonUri.getBucket(), obj.key())))
        .filter(obj -> !isDirectory(obj))
        .collect(Collectors.toList());
  }

  @Override
  public Collection<MediaUri> listEncryptedContent(MediaUri directory, String customerMasterKey, StsToken stsToken) {
    final AmazonS3URI amazonUri = toAmazonUri(directory);
    return s3.getObjectList(amazonUri.getBucket(), amazonUri.getKey(), customerMasterKey, stsToken)
        .getObjectSummaries()
        .stream()
        .filter(summary -> !isDirectory(
            s3.getObjectMetadata(summary.getBucketName(), summary.getKey(), customerMasterKey, stsToken)))
        .map(summary -> s3.getUrl(summary.getBucketName(), summary.getKey(), customerMasterKey, stsToken))
        .map(MediaUri::from)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteEncrypted(final MediaUri mediaUri, final String customerMasterKey, final StsToken stsToken) {
    final AmazonS3URI amazonUri = toAmazonUri(mediaUri);
    log.info("Deleting encrypted artifact on S3 with Bucket {} and Key {} for CMK {} ", amazonUri.getBucket(),
        amazonUri.getKey(), customerMasterKey);
    if (s3.isDirectory(amazonUri.getBucket(), amazonUri.getKey(), customerMasterKey, stsToken)) {
      final List<S3ObjectSummary> objects = s3
          .getObjectList(amazonUri.getBucket(), amazonUri.getKey(), customerMasterKey, stsToken)
          .getObjectSummaries();
      for (S3ObjectSummary objectSummary : objects) {
        this.deleteEncrypted(buildMediaUri(objectSummary.getBucketName(), objectSummary.getKey()), customerMasterKey,
            stsToken);
      }
    } else {
      s3.delete(amazonUri.getBucket(), amazonUri.getKey(), customerMasterKey, stsToken);
    }
  }

  @Override
  public void delete(final MediaUri mediaUri) {
    final AmazonS3URI amazonUri = toAmazonUri(mediaUri);
    log.info("Deleting artifact on S3 with Bucket {} and Key {} ", amazonUri.getBucket(), amazonUri.getKey());
    if (s3v2.isDirectory(amazonUri.getBucket(), amazonUri.getKey())) {
      final List<S3Object> objects = s3v2.getObjectList(amazonUri.getBucket(), amazonUri.getKey())
          .contents();
      for (final S3Object object : objects) {
        this.delete(buildMediaUri(amazonUri.getBucket(), object.key()));
      }
    } else {
      s3v2.delete(amazonUri.getBucket(), amazonUri.getKey());
    }
  }

  @Override
  public MediaUri buildMediaUri(final String bucket, final String object) {
    return MediaUri.from("s3://" + bucket, object);
  }

  private ObjectCannedACL convert(final AclPermission permission) {
    switch (permission) {
      case PROTECTED_READ:
        return ObjectCannedACL.BUCKET_OWNER_READ;
      case PROTECTED_READ_WRITE:
        return ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL;
      case PUBLIC_READ:
        return ObjectCannedACL.PUBLIC_READ;
      case PUBLIC_READ_WRITE:
        return ObjectCannedACL.PUBLIC_READ_WRITE;
      default:
        throw new RuntimeException("Unknown permission " + permission);
    }
  }
}
