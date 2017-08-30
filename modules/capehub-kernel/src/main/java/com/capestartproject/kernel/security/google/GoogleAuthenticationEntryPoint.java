package com.capestartproject.kernel.security.google;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * @author CS39
 *
 */
public class GoogleAuthenticationEntryPoint implements AuthenticationEntryPoint {

	/** The logger */
	private static final Logger logger = LoggerFactory.getLogger(GoogleAuthenticationEntryPoint.class);

	/*
	 * Authentication entry point for google login (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.security.web.AuthenticationEntryPoint#commence(javax.
	 * servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse,
	 * org.springframework.security.core.AuthenticationException)
	 */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException arg2)
			throws IOException, ServletException {
		final String redirectURI = "http://localhost:8080/oauth2callback";
		StringBuilder sb = new StringBuilder()
				.append("https://accounts.google.com/o/oauth2/auth?")
				.append("scope=email")
				.append("&")
				.append("redirect_uri=" + redirectURI)
				.append("&")
				.append("response_type=code")
				.append("&")
				.append("client_id=176374462623-85npnc3n9rj3tssi9f2nfeod6f99ktsr.apps.googleusercontent.com")
				.append("&").append("approval_prompt=force")
				.append("&")
				.append("hd=capestart.com");
		logger.debug("Google Authentication entry url {}", sb.toString());
		response.sendRedirect(sb.toString());
	}

}
