package com.capestartproject.userdirectory.google.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.capestartproject.userdirectory.google.security.utils.GoogleClientUtils;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.Directory.Users.Get;

/**
 * @author S T
 *
 */
public class GoogleAuthenticationProvider implements AuthenticationProvider {

	/** The logger */
	private static final Logger logger = LoggerFactory.getLogger(GoogleAuthenticationProvider.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.security.authentication.AuthenticationProvider#
	 * authenticate(org.springframework.security.core.Authentication)
	 */
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String userName = authentication.getName().trim();
		String password = authentication.getCredentials().toString().trim();
		Authentication auth = null;
		try {
			Directory directory = GoogleClientUtils.getDirectoryService();
			Get get = directory.users().get(userName);
			HttpResponse response = get.buildHttpRequest().execute();
		} catch (IOException e) {
			logger.error("Google Authentication Exception {}", e.getMessage());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.security.authentication.AuthenticationProvider#
	 * supports(java.lang.Class)
	 */
	@Override
	public boolean supports(Class<?> authentication) {
		return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
	}

}
