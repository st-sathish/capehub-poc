/**
 * 
 */
package com.capestartproject.kernel.security.google;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.filter.GenericFilterBean;

/**
 * @author CS39 Filter to handle google redirect uri after successfull login
 */
public class GoogleAuthenticationFilter extends GenericFilterBean {

	/** The logger */
	private static final Logger logger = LoggerFactory.getLogger(GoogleAuthenticationFilter.class);

	/** Authentication Manager */
	private AuthenticationManager authenticationManager;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 * javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		// get request parameter code after successfully authenticated
		if (null != request.getParameter("code")) {
			final String redirectURI = "http://localhost:8080/oauth2callback";
			StringBuilder sb = new StringBuilder().append("code=" + request.getParameter("code")).append("&")
					.append("client_id=176374462623-85npnc3n9rj3tssi9f2nfeod6f99ktsr.apps.googleusercontent.com")
					.append("&").append("client_secret=JgfwE8GwlaHSuphUGd3pVrX8").append("&")
					.append("redirect_uri=" + redirectURI)
					.append("&").append("grant_type=authorization_code");

			// post parameters
			URL url = new URL("https://accounts.google.com/o/oauth2/token");
			URLConnection urlConn = url.openConnection();
			urlConn.setDoOutput(true);
			OutputStreamWriter writer = new OutputStreamWriter(urlConn.getOutputStream());
			writer.write(sb.toString());
			writer.flush();

			// get output in outputString
			String line, outputString = "";
			BufferedReader reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			while ((line = reader.readLine()) != null) {
				outputString += line;
			}
			System.out.println(outputString);

			// get Access Token
			JSONParser parser = new JSONParser();
			JSONObject json;
			try {
				json = (JSONObject) parser.parse(outputString);
				String access_token = json.get("access_token").toString();
				System.out.println(access_token);

				// get User Info
				url = new URL("https://www.googleapis.com/oauth2/v2/userinfo");
				urlConn = url.openConnection();
				urlConn.setRequestProperty("Authorization", "Bearer " + access_token);
				urlConn.setRequestProperty("Content-Type", "application/json");
				outputString = "";
				reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
				while ((line = reader.readLine()) != null) {
					outputString += line;
				}
				System.out.println("User details");
				System.out.println(outputString);

				// get admin User Info
				url = new URL(
						"https://www.googleapis.com/admin/directory/v1/users/sathish.st@capestart.com");

				System.out.println(url.toString());
				urlConn = url.openConnection();
				urlConn.setRequestProperty("Authorization", "Bearer " + access_token);
				urlConn.setRequestProperty("Content-Type", "application/json");
				outputString = "";
				reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
				while ((line = reader.readLine()) != null) {
					outputString += line;
				}
				System.out.println("Admin User details");
				System.out.println(outputString);

				((HttpServletResponse) response).sendRedirect("/index.html");
				return;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		chain.doFilter(request, response);
	}

	/**
	 * Set authentication manager
	 * 
	 * @param authenticationManager
	 */
	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}
}
