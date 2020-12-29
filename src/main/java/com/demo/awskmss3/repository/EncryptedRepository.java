package com.demo.awskmss3.repository;

import com.demo.awskmss3.domain.EncryptedEntity;

public interface EncryptedRepository<T extends EncryptedEntity> {

  <E extends T> E encryptAndSave(final E entity);

}
