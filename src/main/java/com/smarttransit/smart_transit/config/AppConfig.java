package com.smarttransit.smart_transit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import com.smarttransit.smart_transit.security.AuthInterceptor;
import com.smarttransit.smart_transit.security.CurrentUserArgumentResolver;

import java.util.List;

@Configuration
public class AppConfig implements WebMvcConfigurer {

	private final KinviaProperties properties;
	private final AuthInterceptor authInterceptor;
	private final CurrentUserArgumentResolver currentUserResolver;

	public AppConfig(KinviaProperties properties, AuthInterceptor authInterceptor,
			CurrentUserArgumentResolver currentUserResolver) {
		this.properties = properties;
		this.authInterceptor = authInterceptor;
		this.currentUserResolver = currentUserResolver;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserResolver);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*");
	}
}
