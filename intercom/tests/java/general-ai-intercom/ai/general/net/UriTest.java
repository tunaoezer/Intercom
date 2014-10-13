/* General AI - Networking
 * Copyright (C) 2014 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for {@link Uri}.
 */
public class UriTest {

  /**
   * Tests construction of a Uri.
   */
  @Test
  public void construction() {
    Uri uri = new Uri("http", "general.ai", "/test/path");
    assertThat(uri.getProtocol(), is("http"));
    assertThat(uri.getServer(), is("general.ai"));
    assertThat(uri.getPort(), is(-1));
    assertThat(uri.getPath(), is("/test/path"));
    assertThat(uri.toString(), is("http://general.ai/test/path"));
    

    uri = new Uri("https://user@general.ai:443/secure/path?param1&param2=value2#part1");
    uri.setPort(443);
    assertThat(uri.getProtocol(), is("https"));
    assertThat(uri.getServer(), is("general.ai"));
    assertThat(uri.getPort(), is(443));
    assertThat(uri.getPath(), is("/secure/path"));
    assertThat(uri.getUser(), is("user"));
    assertThat(uri.getFragment(), is("part1"));
    Assert.assertTrue(uri.hasParameter("param1"));
    assertThat(uri.getParameter("param1"), is(""));
    Assert.assertTrue(uri.hasParameter("param2"));
    assertThat(uri.getParameter("param2"), is("value2"));
    assertThat(uri.toString(),
               is("https://user@general.ai:443/secure/path?param1&param2=value2#part1"));

    // Copy construction.
    uri = new Uri(uri);
    assertThat(uri.getProtocol(), is("https"));
    assertThat(uri.getServer(), is("general.ai"));
    assertThat(uri.getPort(), is(443));
    assertThat(uri.getPath(), is("/secure/path"));
    assertThat(uri.getUser(), is("user"));
    assertThat(uri.getFragment(), is("part1"));
    Assert.assertTrue(uri.hasParameter("param1"));
    assertThat(uri.getParameter("param1"), is(""));
    Assert.assertTrue(uri.hasParameter("param2"));
    assertThat(uri.getParameter("param2"), is("value2"));
    assertThat(uri.toString(),
               is("https://user@general.ai:443/secure/path?param1&param2=value2#part1"));
  }

  /**
   * Tests setting and getting query parameters.
   */
  @Test
  public void parameters() {
    Uri uri = new Uri("ws", "general.ai", "/test/parameters");
    uri.setParameter("param1", "");
    uri.setParameter("param2", "value2");
    Assert.assertTrue(uri.hasParameter("param1"));
    assertThat(uri.getParameter("param1"), is(""));
    Assert.assertTrue(uri.hasParameter("param2"));
    assertThat(uri.getParameter("param2"), is("value2"));
  }

  /**
   * Tests conversion to a URI.
   */
  @Test
  public void toUri() {
    Uri uri = new Uri("wss://general.ai:8443/uri/path?param=value#fragment");
    uri.setUser("user@general.ai");
    uri.setParameter("param", "new_value");
    URI java_uri = uri.toUri();
    assertThat(java_uri.getScheme(), is("wss"));
    assertThat(java_uri.getUserInfo(), is("user@general.ai"));
    assertThat(java_uri.getHost(), is("general.ai"));
    assertThat(java_uri.getPort(), is(8443));
    assertThat(java_uri.getPath(), is("/uri/path"));
    assertThat(java_uri.getQuery(), is("param=new_value"));
    assertThat(java_uri.getFragment(), is("fragment"));
  }
}
