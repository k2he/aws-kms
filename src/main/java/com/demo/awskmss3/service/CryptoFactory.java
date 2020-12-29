package com.demo.awskmss3.service;

import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * A factory for providing an implementation for {@link Crypto}.
 */
@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class CryptoFactory {

  private static CryptoFactory instance;
  private Crypto cryptoImpl;

  @Configuration
  private static class Injector {

    @Autowired
    public Injector(final CryptoFactory cryptoFactory) {
      instance = cryptoFactory;
    }
  }

  /**
   * Provides a default instance of the crypto implementation to encrypt/decrypt data.
   */
  public static Crypto get() {
    return instance.cryptoImpl;
  }

}
