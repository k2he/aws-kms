package com.demo.awskmss3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demo.awskmss3.domain.Patient;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long>, EncryptedRepository<Patient> {

	/**
	 * Encrypt the PHI fields to set the {@link Patient#encryptedBlob} and save.
	 */
	@Override
	default <E extends Patient> E encryptAndSave(final E patient) {
		patient.encrypt();
		return save(patient);
	}
}
