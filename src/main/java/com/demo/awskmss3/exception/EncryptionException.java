package com.demo.awskmss3.exception;

/**
 * {@link EncryptionException} is thrown for any failure while performing encryption/decryption of any data.
 */
public class EncryptionException extends RuntimeException {

  private static final long serialVersionUID = 995029479036867860L;

  public EncryptionException(final String message) {
    super(message);
  }

  public EncryptionException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
