package com.demo.awskmss3.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.util.Base64;
import com.demo.awskmss3.exception.EncryptionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

/**
 * Interface for cryptography implementation.
 */
public interface Crypto {

  ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Serialize the given object as json, then encrypt it.
   *
   * @param toEncrypt a POJO that can be serialized into json.
   * @return a Base64 encoded string of the encrypted bytes.
   */
  default String serializeAndEncrypt(@NonNull final Object toEncrypt) {
    try {
      return encryptSerialized(OBJECT_MAPPER.writeValueAsBytes(toEncrypt));
    } catch (final JsonProcessingException e) {
      throw new EncryptionException("Failed to serialize the given object for encryption.", e);
    }
  }

  /**
   * Encrypted the given serialized byte[].
   *
   * @param toEncrypt a serialized byte[] to be encrypted.
   * @return a Base64 encoded string of the encrypted bytes
   */
  default String encryptSerialized(@NonNull final byte[] toEncrypt) {
    return Base64.encodeAsString(encrypt(toEncrypt));
  }

  /**
   * Perform encryption on the provided serialized byte array.
   *
   * @param toEncrypt a serialized byte[] to be encrypted.
   * @return encrypted bytes.
   */
  byte[] encrypt(@NonNull final byte[] toEncrypt);

  /**
   * Take an input stream of plainText, encrypt it and stream out the encrypted cipher to output stream.
   *
   * @param plainIn An input stream of the plainText to be encrypted.
   * @param cipherOut An output stream to write the cipherText back out.
   */
  void encrypt(@NonNull final InputStream plainIn, @NonNull final OutputStream cipherOut);

  /**
   * Decrypt the cipherText and get a JSON and deserialize it.
   *
   * @param cipherText Base64 encoded encrypted JSON string.
   * @param toClass Class to deserialize the decrypted JSON into.
   * @return a POJO of the provided class that represents the JSON from decrypted cipherText.
   */
  default <T> T decryptAndDeserialize(final String cipherText, final Class<T> toClass) {
    final byte[] decryptedBytes = decryptBase64(cipherText);
    try {
      if (decryptedBytes != null) {
        return OBJECT_MAPPER.readValue(decryptedBytes, toClass);
      } else {
        return null;
      }
    } catch (final IOException e) {
      throw new EncryptionException("Failed to deserialized decrypted object to " + toClass.getSimpleName(), e);
    }
  }

  /**
   * Decrypt the cipherText and apply the JSON as an update to a given POJO.
   *
   * @param cipherText Base64 encoded encrypted JSON string.
   * @param object a POJO to which to apply the update.
   */
  default void decryptAndUpdate(final String cipherText, final Object object) {
    final byte[] decryptedBytes = decryptBase64(cipherText);
    try {
      if (decryptedBytes != null) {
        OBJECT_MAPPER.readerForUpdating(object).readValue(decryptedBytes);
      }
    } catch (final IOException e) {
      throw new EncryptionException(
          "Failed to update object of class " + object.getClass().getSimpleName() + " after decryption.", e);
    }
  }

  /**
   * Decrypt the given Base64 encoded string to a byte[].
   *
   * @param cipherText Base64 encoded string of the encrypted data.
   * @return a byte[] representing the serialized form of decrypted object.
   */
  default byte[] decryptBase64(final String cipherText) {
    return decrypt(Base64.decode(cipherText));
  }

  /**
   * Perform decryption on the serialized cipherBytes.
   *
   * @param cipherBytes a byte[] representing the serialized encrypted object.
   * @return a byte[] representing the decrypted object to be serialized.
   */
  byte[] decrypt(final byte[] cipherBytes);

  /**
   * Take an input stream of cipherText, decrypt it and write it back out to the output stream.
   *
   * @param cipherIn An input stream of the cipher text to be decrypted.
   * @param plainOut An output stream to collect the plain text after decryption.
   */
  void decrypt(@NonNull final InputStream cipherIn, @NonNull final OutputStream plainOut);
}

