package com.smarttransit.smart_transit.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.smarttransit.smart_transit.user.Role;

/** Restricts an endpoint (or every endpoint of a controller) to the listed roles. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface RequiresRole {

	Role[] value();
}
