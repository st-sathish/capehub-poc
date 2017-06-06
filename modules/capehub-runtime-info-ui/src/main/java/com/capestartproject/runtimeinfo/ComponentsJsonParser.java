package com.capestartproject.runtimeinfo;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * Parses a text dump of components.json reports
 */
public final class ComponentsJsonParser {

  /** Matches IPv4 addresses */
  public static final String IP_REGEX = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

  /**
   * Private constructor to disallow construction of this utility class.
   */
  private ComponentsJsonParser() {
  }

  /**
   * Main method
   *
   * @param args
   *          a string array containing the program args. The first arg should be the file path to the json.
   */
  public static void main(String[] args) {
    try {
      File f = new File(args[0]);
      FileInputStream fis = new FileInputStream(f);
      DataInputStream in = new DataInputStream(fis);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      JSONParser parser = new JSONParser();

      Set<String> adminServers = new HashSet<String>();

      String strLine;
      while ((strLine = br.readLine()) != null) {
        JSONObject json;
        try {
          json = (JSONObject) parser.parse(strLine);
        } catch (Exception e) {
          // this was malformed
          continue;
        }
        String adminServer = (String) json.get("admin");
        adminServer = adminServer.substring(adminServer.indexOf("/") + 2);
        if (adminServer.contains(":")) {
          adminServer = adminServer.substring(0, adminServer.lastIndexOf(":"));
        }
        if (adminServer.matches(IP_REGEX)) {
          adminServer = InetAddress.getByName(adminServer).getHostName();
        }
        adminServers.add(adminServer);
      }
      in.close();

      System.out.println("Admin Servers (" + adminServers.size() + ") :");
      for (String admin : adminServers) {
        System.out.println("\t" + admin);
      }

    } catch (Exception e) {
      System.out.println(e);
    }

  }
}
