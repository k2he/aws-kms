package com.demo.awskmss3.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.awskmss3.service.CryptoFactory;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "PATIENT")
public class Patient implements EncryptedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Lob
	@Column(nullable = false)
	@JsonIgnore
	private String encryptedBlob;

	@Transient
	private transient String name;

	@Transient
	private transient String medicalRecordNumber;

	@Transient
	private transient String ethnicity;

	@Transient
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private transient Date dateOfBirth;

	@Transient
	private transient PatientSex sex;

	@Builder
	public Patient(Long id, String name, String medicalRecordNumber, String ethnicity, Date dateOfBirth,
			PatientSex sex) {
		this.id = id;
		this.name = name;
		this.medicalRecordNumber = medicalRecordNumber;
		this.ethnicity = ethnicity;
		this.dateOfBirth = dateOfBirth;
		this.sex = sex;
	}

	@PostLoad
	@PostUpdate
	public void decrypt() {
		CryptoFactory.get().decryptAndUpdate(encryptedBlob, this);
	}

	/**
	 * Encrypt the PHI data within a record into a single blob before persisting.
	 * Note that this method can be called on {@link PrePersist}, but it'll not work
	 * with {@link PreUpdate} since {@link JpaRepository#save(Object)} will create a
	 * new object and apply changes from the object passed in the method params only
	 * for fields that are tracked by JPA, which excludes {@link Transient}.
	 */
	public void encrypt() {
		encryptedBlob = CryptoFactory.get().serializeAndEncrypt(new EncryptedPatient(this));
	}

	/**
	 * Class to generate POJO that encapsulates all the PHI fields in a record to
	 * allow encryption into a single blob.
	 */
	@Value
	private static class EncryptedPatient {

		private final String name;
		private final String medicalRecordNumber;
		private final String ethnicity;

		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
		private final Date dateOfBirth;

		private final PatientSex sex;

		/**
		 * Create an object with fields to be encrypted into a single blob.
		 */
		private EncryptedPatient(final Patient patient) {
			name = patient.getName();
			medicalRecordNumber = patient.getMedicalRecordNumber();
			ethnicity = patient.getEthnicity();
			dateOfBirth = patient.getDateOfBirth();
			sex = patient.getSex();
		}
	}
}
