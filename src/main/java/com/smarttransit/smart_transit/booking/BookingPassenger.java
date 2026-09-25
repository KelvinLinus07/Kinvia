package com.smarttransit.smart_transit.booking;

import com.smarttransit.smart_transit.common.AuditedEntity;
import com.smarttransit.smart_transit.user.Gender;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A traveller's details frozen at booking time, independent of later edits to the saved passenger list. */
@Entity
@Table(name = "booking_passengers", indexes = @Index(name = "idx_booking_passengers_booking", columnList = "booking_id"))
public class BookingPassenger extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id", nullable = false)
	private Booking booking;

	@Column(nullable = false, length = 120)
	private String fullName;

	@Column(nullable = false)
	private int age;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private Gender gender;

	@Column(length = 20)
	private String mobile;

	protected BookingPassenger() {
	}

	BookingPassenger(Booking booking, String fullName, int age, Gender gender, String mobile) {
		this.booking = booking;
		this.fullName = fullName.trim();
		this.age = age;
		this.gender = gender;
		this.mobile = mobile;
	}

	public Booking getBooking() { return booking; }

	public String getFullName() { return fullName; }

	public int getAge() { return age; }

	public Gender getGender() { return gender; }

	public String getMobile() { return mobile; }
}
