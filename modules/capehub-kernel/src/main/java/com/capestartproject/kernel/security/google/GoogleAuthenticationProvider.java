/**
 * 
 */
package com.capestartproject.kernel.security.google;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author CS39
 *
 */
public class GoogleAuthenticationProvider implements AuthenticationProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.security.authentication.AuthenticationProvider#
	 * authenticate(org.springframework.security.core.Authentication)
	 */
	@Override
	public Authentication authenticate(Authentication arg0) throws AuthenticationException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.security.authentication.AuthenticationProvider#
	 * supports(java.lang.Class)
	 */
	@Override
	public boolean supports(Class<?> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
