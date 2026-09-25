package com.smarttransit.smart_transit.user;

import com.smarttransit.smart_transit.common.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A traveller saved in a user's address book so they can be reused across bookings. */
@Entity
@Table(name = "passengers", indexes = @Index(name = "idx_passengers_user", columnList = "user_id"))
public class Passenger extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 120)
	private String fullName;

	@Column(nullable = false)
	private int age;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private Gender gender;

	@Column(length = 20)
	private String mobile;

	protected Passenger() {
	}

	public Passenger(User user, String fullName, int age, Gender gender, String mobile) {
		this.user = user;
		update(fullName, age, gender, mobile);
	}

	public void update(String fullName, int age, Gender gender, String mobile) {
		this.fullName = fullName.trim();
		this.age = age;
		this.gender = gender;
		this.mobile = mobile;
	}

	public User getUser() { return user; }

	public String getFullName() { return fullName; }

	public int getAge() { return age; }

	public Gender getGender() { return gender; }

	public String getMobile() { return mobile; }
}
