/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

import ai.general.directory.test.TestHandler;
import ai.general.directory.test.TestUtilities;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for {@link Directory} class.
 */
public class DirectoryTest {

  /**
   * Tests creation of paths.
   */
  @Test
  public void createPath() {
    Directory directory = Directory.Instance;
    Assert.assertNotNull(directory.getNode("/"));
    Assert.assertTrue(directory.pathExists("/"));
    Assert.assertNull(directory.getNode("/directory/create"));
    Assert.assertNull(directory.getNode("/directory/create/a/b/c1"));
    Assert.assertFalse(directory.pathExists("/directory/create"));

    Assert.assertTrue(directory.createPath("/directory/create/a/b/c1"));
    Assert.assertNotNull(directory.getNode("/directory/create"));
    Assert.assertNotNull(directory.getNode("/directory/create/a"));
    Assert.assertNotNull(directory.getNode("/directory/create/a/b"));
    Assert.assertNotNull(directory.getNode("/directory/create/a/b/c1"));
    Assert.assertTrue(directory.pathExists("/directory/create"));

    Assert.assertNull(directory.getNode("/directory/create/a/b/c2"));
    Assert.assertTrue(directory.createPath("/directory/create/a/b/c2"));
    Assert.assertNotNull(directory.getNode("/directory/create/a/b/c2"));
  }

  /**
   * Tests removal of paths.
   */
  @Test
  public void removePath() {
    Directory directory = Directory.Instance;
    Assert.assertNull(directory.getNode("/directory/remove"));
    Assert.assertTrue(directory.createPath("/directory/remove/rm1/rm2/rm3a/rm4a/rm5a"));
    Assert.assertTrue(directory.createPath("/directory/remove/rm1/rm2/rm3b/rm4b/rm5b"));
    Assert.assertNotNull(directory.getNode("/directory/remove/rm1/rm2/rm3a/rm4a/rm5a"));
    Assert.assertNotNull(directory.getNode("/directory/remove/rm1/rm2/rm3b/rm4b/rm5b"));
    Assert.assertTrue(directory.pathExists("/directory/remove/rm1/rm2/rm3a/rm4a/rm5a"));

    Assert.assertTrue(directory.removePath("/directory/remove/rm1/rm2/rm3a"));
    Assert.assertFalse(directory.pathExists("/directory/remove/rm1/rm2/rm3a/rm4a/rm5a"));
    Assert.assertNull(directory.getNode("/directory/remove/rm1/rm2/rm3a"));
    Assert.assertNull(directory.getNode("/directory/remove/rm1/rm2/rm3a/rm4a/rm5a"));
    Assert.assertNotNull(directory.getNode("/directory/remove/rm1/rm2/rm3b/rm4b/rm5b"));

    // root path cannot be removed
    Assert.assertFalse(directory.removePath(""));
    Assert.assertFalse(directory.removePath("/"));
  }

  /**
   * Tests execution of handlers.
   */
  @Test
  public void handle() {
    Directory directory = Directory.Instance;
    String base_directory = "/directory";
    String home1 = base_directory + "/home1";
    String home2 = base_directory + "/home2";
    Assert.assertNull(directory.getNode(home1 + "/handle"));
    Assert.assertTrue(directory.createPath(home1 + "/a/b1/c1"));
    Assert.assertTrue(directory.createPath(home1 + "/a/b2/c2"));
    Assert.assertTrue(directory.createPath(home2 + "/a/b1/c1"));
    Assert.assertTrue(directory.createPath(home2 + "/a/b2/c2"));

    TestHandler handler_home1_b1 = new TestHandler("handler_b1", true);  // catch-all
    TestHandler handler_home1_c1 = new TestHandler("handler_c1");  // regular
    TestHandler handler_home1_c2 = new TestHandler("handler_c2");  // regular
    Assert.assertTrue(directory.addHandler(home1 + "/a/b1", handler_home1_b1));
    Assert.assertTrue(directory.addHandler(home1 + "/a/b1/c1", handler_home1_c1));
    Assert.assertTrue(directory.addHandler(home1 + "/a/b2/c2", handler_home1_c2));
    TestHandler handler_home2_b1 = new TestHandler("handler_b1", true);  // catch-all
    TestHandler handler_home2_c1 = new TestHandler("handler_c1");  // regular
    TestHandler handler_home2_c2 = new TestHandler("handler_c2");  // regular
    Assert.assertTrue(directory.addHandler(home2 + "/a/b1", handler_home2_b1));
    Assert.assertTrue(directory.addHandler(home2 + "/a/b1/c1", handler_home2_c1));
    Assert.assertTrue(directory.addHandler(home2 + "/a/b2/c2", handler_home2_c2));

    Assert.assertTrue(directory.hasHandler(home1 + "/a/b1", "handler_b1"));
    Assert.assertTrue(directory.hasHandler(home2 + "/a/b1", "handler_b1"));

    // Runs b1 and c1 handlers.
    Request request = TestUtilities.createRequest("/a/b1/c1", "[1]");
    // run on home1
    assertThat(directory.handle(home1, request), is(2));
    assertThat(handler_home1_b1.getArgument(0), is("[1]"));
    assertThat(handler_home1_b1.getPathRemainder(), is("c1"));
    assertThat(handler_home1_c1.getArgument(0), is("[1]"));
    Assert.assertNull(handler_home1_c2.getArgument(0));
    Assert.assertNull(handler_home2_b1.getArgument(0));
    Assert.assertNull(handler_home2_c1.getArgument(0));
    Assert.assertNull(handler_home2_c2.getArgument(0));
    // run on home2
    assertThat(directory.handle(home2, request), is(2));
    assertThat(handler_home1_b1.getArgument(0), is("[1]"));
    assertThat(handler_home1_c1.getArgument(0), is("[1]"));
    Assert.assertNull(handler_home1_c2.getArgument(0));
    assertThat(handler_home2_b1.getArgument(0), is("[1]"));
    assertThat(handler_home2_b1.getPathRemainder(), is("c1"));
    assertThat(handler_home2_c1.getArgument(0), is("[1]"));
    Assert.assertNull(handler_home2_c2.getArgument(0));

    // Runs c2 handler.
    request = TestUtilities.createRequest("/a/b2/c2", "[2]");
    // run on home1
    assertThat(directory.handle(home1, request), is(1));
    assertThat(handler_home1_b1.getArgument(0), is("[1]"));
    assertThat(handler_home1_c1.getArgument(0), is("[1]"));
    assertThat(handler_home1_c2.getArgument(0), is("[2]"));
    assertThat(handler_home2_b1.getArgument(0), is("[1]"));
    assertThat(handler_home2_c1.getArgument(0), is("[1]"));
    Assert.assertNull(handler_home2_c2.getArgument(0));
    // run on home2
    assertThat(directory.handle(home2, request), is(1));
    assertThat(handler_home1_b1.getArgument(0), is("[1]"));
    assertThat(handler_home1_c1.getArgument(0), is("[1]"));
    assertThat(handler_home1_c2.getArgument(0), is("[2]"));
    assertThat(handler_home2_b1.getArgument(0), is("[1]"));
    assertThat(handler_home2_c1.getArgument(0), is("[1]"));
    assertThat(handler_home2_c2.getArgument(0), is("[2]"));

    // Non-existent path. Catchall handler is still run.
    request = TestUtilities.createRequest("/a/b1/c1/d1", "[3]");
    // run on home1
    assertThat(directory.handle(home1, request), is(1));
    assertThat(handler_home1_b1.getArgument(0), is("[3]"));
    assertThat(handler_home1_b1.getPathRemainder(), is("c1/d1"));
    assertThat(handler_home1_c1.getArgument(0), is("[1]"));
    assertThat(handler_home1_c2.getArgument(0), is("[2]"));
    assertThat(handler_home2_b1.getArgument(0), is("[1]"));
    assertThat(handler_home2_c1.getArgument(0), is("[1]"));
    assertThat(handler_home2_c2.getArgument(0), is("[2]"));
    // run on home2
    assertThat(directory.handle(home2, request), is(1));
    assertThat(handler_home1_b1.getArgument(0), is("[3]"));
    assertThat(handler_home1_c1.getArgument(0), is("[1]"));
    assertThat(handler_home1_c2.getArgument(0), is("[2]"));
    assertThat(handler_home2_b1.getArgument(0), is("[3]"));
    assertThat(handler_home2_b1.getPathRemainder(), is("c1/d1"));
    assertThat(handler_home2_c1.getArgument(0), is("[1]"));
    assertThat(handler_home2_c2.getArgument(0), is("[2]"));

    // Removed handler is not run anymore.
    Assert.assertTrue(directory.removeHandler(home1 + "/a/b1", "handler_b1"));
    request = TestUtilities.createRequest("/a/b1/c1", "[4]");
    assertThat(directory.handle(home1, request), is(1));
    assertThat(handler_home1_b1.getArgument(0), is("[3]"));
    assertThat(handler_home1_c1.getArgument(0), is("[4]"));
    assertThat(handler_home1_c2.getArgument(0), is("[2]"));
    // not removed from home2
    assertThat(directory.handle(home2, request), is(2));
    assertThat(handler_home2_b1.getArgument(0), is("[4]"));
    assertThat(handler_home2_c1.getArgument(0), is("[4]"));
    assertThat(handler_home2_c2.getArgument(0), is("[2]"));

    // Non-existent home directory
    request = TestUtilities.createRequest("/a/b1/c1", "[5]");
    assertThat(directory.handle(base_directory + "/nohome", request), is(0));
    assertThat(handler_home1_b1.getArgument(0), is("[3]"));
    assertThat(handler_home1_c1.getArgument(0), is("[4]"));
    assertThat(handler_home1_c2.getArgument(0), is("[2]"));
    assertThat(handler_home2_b1.getArgument(0), is("[4]"));
    assertThat(handler_home2_c1.getArgument(0), is("[4]"));
    assertThat(handler_home2_c2.getArgument(0), is("[2]"));

    // Non-absolute home directory
    request = TestUtilities.createRequest("/a/b1/c1", "[6]");
    assertThat(directory.handle("notbasolute", request), is(0));
    assertThat(handler_home1_b1.getArgument(0), is("[3]"));
    assertThat(handler_home1_c1.getArgument(0), is("[4]"));
    assertThat(handler_home1_c2.getArgument(0), is("[2]"));
    assertThat(handler_home2_b1.getArgument(0), is("[4]"));
    assertThat(handler_home2_c1.getArgument(0), is("[4]"));
    assertThat(handler_home2_c2.getArgument(0), is("[2]"));

    // Path removal results in handler removal.
    Assert.assertTrue(directory.removePath(home1 + "/a/b1"));
    request = TestUtilities.createRequest("/a/b1/c1", "[7]");
    assertThat(directory.handle(home1, request), is(0));
    assertThat(handler_home1_b1.getArgument(0), is("[3]"));
    assertThat(handler_home1_c1.getArgument(0), is("[4]"));
    assertThat(handler_home1_c2.getArgument(0), is("[2]"));
    // not removed from home2
    assertThat(directory.handle(home2, request), is(2));
    assertThat(handler_home2_b1.getArgument(0), is("[7]"));
    assertThat(handler_home2_c1.getArgument(0), is("[7]"));
    assertThat(handler_home2_c2.getArgument(0), is("[2]"));
  }

  /**
   * Tests linking and unlinking.
   * Includes handler testing. This test adds handlers to the underlying nodes. The
   * {@link #linkHandlers()} test adds handlers via links.
   */
  @Test
  public void link() {
    Directory directory = Directory.Instance;
    String base_directory = "/directory/link";
    Assert.assertTrue(directory.createPath(base_directory + "/main/b"));
    Assert.assertTrue(directory.createPath(base_directory + "/main/c"));
    Assert.assertTrue(directory.createPath(base_directory + "/a1"));
    Assert.assertTrue(directory.createPath(base_directory + "/a2"));
    TestHandler handler_b = new TestHandler("handler_b", true);  // catch-all
    TestHandler handler_c = new TestHandler("handler_c");  // regular
    Assert.assertTrue(directory.addHandler(base_directory + "/main/b", handler_b));
    Assert.assertTrue(directory.addHandler(base_directory + "/main/c", handler_c));
    Request request_a1 = TestUtilities.createRequest("/a1/b/c", "a1");
    Request request_a2a = TestUtilities.createRequest("/a2/b/c", "a2a");
    Request request_a2b = TestUtilities.createRequest("/a2/b/c", "a2b");
    Request request_a2c = TestUtilities.createRequest("/a2/c", "a2c");
    assertThat(directory.handle(base_directory, request_a1), is(0));
    Assert.assertNull(handler_b.getArgument(0));
    Assert.assertNull(handler_c.getArgument(0));

    // Link main/c to main/b and main/b to a1
    Assert.assertTrue(directory.link(base_directory + "/main/b",
                                     base_directory + "/main/c"));
    Assert.assertTrue(directory.pathExists(base_directory + "/main/b"));
    Assert.assertTrue(directory.pathExists(base_directory + "/main/c"));
    Assert.assertTrue(directory.pathExists(base_directory + "/main/b/c"));
    Assert.assertTrue(directory.link(base_directory + "/a1",
                                     base_directory + "/main/b"));
    Assert.assertTrue(directory.pathExists(base_directory + "/a1/b/c"));
    assertThat(directory.handle(base_directory, request_a1), is(2));
    assertThat(handler_b.getArgument(0), is("a1"));
    assertThat(handler_b.getPathRemainder(), is("c"));
    assertThat(handler_c.getArgument(0), is("a1"));

    // Link linked a1/b to a2
    Assert.assertTrue(directory.link(base_directory + "/a2", base_directory + "/a1/b"));
    Assert.assertTrue(directory.pathExists(base_directory + "/a2/b/c"));
    assertThat(directory.handle(base_directory, request_a2a), is(2));
    assertThat(handler_b.getArgument(0), is("a2a"));
    assertThat(handler_b.getPathRemainder(), is("c"));
    assertThat(handler_c.getArgument(0), is("a2a"));

    // Unlink a1/b and run handler via a2
    Assert.assertTrue(directory.unlink(base_directory + "/a1",
                                       base_directory + "/main/b"));
    Assert.assertFalse(directory.pathExists(base_directory + "/a1/b/c"));
    Assert.assertTrue(directory.pathExists(base_directory + "/a2/b/c"));
    assertThat(directory.handle(base_directory, request_a1), is(0));
    assertThat(handler_b.getArgument(0), is("a2a"));
    assertThat(handler_c.getArgument(0), is("a2a"));
    assertThat(directory.handle(base_directory, request_a2b), is(2));
    assertThat(handler_b.getArgument(0), is("a2b"));
    assertThat(handler_b.getPathRemainder(), is("c"));
    assertThat(handler_c.getArgument(0), is("a2b"));

    // Establish direct link a2/c bypassing b
    Assert.assertTrue(directory.link(base_directory + "/a2",
                                     base_directory + "/a2/b/c"));
    Assert.assertTrue(directory.pathExists(base_directory + "/a2/c"));
    assertThat(directory.handle(base_directory, request_a2c), is(1));
    assertThat(handler_b.getArgument(0), is("a2b"));
    assertThat(handler_c.getArgument(0), is("a2c"));

    // Unlinking main/c from main/b affects a2, but not any direct links
    assertThat(directory.handle(base_directory, request_a2a), is(2));
    assertThat(handler_c.getArgument(0), is("a2a"));
    Assert.assertTrue(directory.unlink(base_directory + "/main/b",
                                       base_directory + "/main/c"));
    Assert.assertFalse(directory.pathExists(base_directory + "/main/b/c"));
    Assert.assertFalse(directory.pathExists(base_directory + "/a2/b/c"));
    Assert.assertTrue(directory.pathExists(base_directory + "/a2/c"));
    assertThat(directory.handle(base_directory, request_a2c), is(1));
    assertThat(handler_c.getArgument(0), is("a2c"));

    // Cyclic links
    Assert.assertFalse(directory.link(base_directory + "/a2", base_directory + "/a2"));
    Assert.assertFalse(directory.link(base_directory + "/a2/c", base_directory + "/a2"));

    // Non-exitent path
    Assert.assertFalse(directory.link(base_directory + "/a2", base_directory + "/b"));
  }

  /**
   * Tests behavior of handlers added through links.
   */
  @Test
  public void linkHandlers() {
    Directory directory = Directory.Instance;
    String base_directory = "/directory/linkHandlers";
    Assert.assertTrue(directory.createPath(base_directory + "/main/target"));
    Assert.assertTrue(directory.createPath(base_directory + "/link"));
    Assert.assertTrue(directory.createPath(base_directory + "/link_link"));
    TestHandler link_handler = new TestHandler("link", true);  // catch-all
    TestHandler link_link_handler = new TestHandler("link_link", true);  // catch-ll
    
    // direct link to target
    Assert.assertTrue(directory.link(base_directory + "/link",
                                     base_directory + "/main/target"));
    // indirect link to target
    Assert.assertTrue(directory.link(base_directory + "/link_link",
                                     base_directory + "/link/target"));

    // add handlers via links
    Assert.assertTrue(directory.addHandler(base_directory + "/link/target", link_handler));
    Assert.assertTrue(directory.addHandler(base_directory + "/link_link/target",
                                           link_link_handler));

    // request via main path
    Request request = TestUtilities.createRequest("/main/target", "direct");
    assertThat(directory.handle(base_directory, request), is(1));  // 0 real + 1 virtual
    assertThat(link_handler.getArgument(0), is("direct"));
    assertThat(link_link_handler.getArgument(0), is("direct"));

    // request via link
    request = TestUtilities.createRequest("/link/target", "indirect");
    assertThat(directory.handle(base_directory, request), is(1));  // 0 real + 1 virtual
    assertThat(link_handler.getArgument(0), is("indirect"));
    assertThat(link_link_handler.getArgument(0), is("indirect"));

    // request via linked link
    request = TestUtilities.createRequest("/link_link/target", "super indirect");
    assertThat(directory.handle(base_directory, request), is(1));  // 0 real + 1 virtual
    assertThat(link_handler.getArgument(0), is("super indirect"));
    assertThat(link_link_handler.getArgument(0), is("super indirect"));

    // catch-all via main path
    request = TestUtilities.createRequest("/main/target/all", "everything");
    assertThat(directory.handle(base_directory, request), is(1));  // 0 real + 1 virtual
    assertThat(link_handler.getArgument(0), is("everything"));
    assertThat(link_link_handler.getArgument(0), is("everything"));

    // catch-all via linked link
    request = TestUtilities.createRequest("/link_link/target/all", "anyway");
    assertThat(directory.handle(base_directory, request), is(1));  // 0 real + 1 virtual
    assertThat(link_handler.getArgument(0), is("anyway"));
    assertThat(link_link_handler.getArgument(0), is("anyway"));

    // unlinking removes handlers
    Assert.assertTrue(directory.unlink(base_directory + "/link",
                                       base_directory + "/main/target"));
    request = TestUtilities.createRequest("/main/target", "unlinked");
    link_handler.clearArguments();
    link_link_handler.clearArguments();
    assertThat(directory.handle(base_directory, request), is(0));
    Assert.assertNull(link_handler.getArgument(0));
    Assert.assertNull(link_link_handler.getArgument(0));

    // restablishing links requires adding handlers again and linking linked links again
    Assert.assertTrue(directory.link(base_directory + "/link",
                                     base_directory + "/main/target"));
    // must first unlink old link since it is stale
    Assert.assertTrue(directory.unlink(base_directory + "/link_link",
                                       base_directory + "/link/target"));
    Assert.assertTrue(directory.link(base_directory + "/link_link",
                                     base_directory + "/link/target"));
    request = TestUtilities.createRequest("/main/target", "re-linked");
    assertThat(directory.handle(base_directory, request), is(0));  // 0 real + 0 virtual
    Assert.assertNull(link_handler.getArgument(0));
    Assert.assertNull(link_link_handler.getArgument(0));
    Assert.assertTrue(directory.addHandler(base_directory + "/link/target", link_handler));
    Assert.assertTrue(directory.addHandler(base_directory + "/link_link/target",
                                           link_link_handler));
    assertThat(directory.handle(base_directory, request), is(1));  // 0 real + 1 virtual
    assertThat(link_handler.getArgument(0), is("re-linked"));
    assertThat(link_link_handler.getArgument(0), is("re-linked"));

    // unlink linked link
    Assert.assertTrue(directory.unlink(base_directory + "/link_link",
                                       base_directory + "/link/target"));
    request = TestUtilities.createRequest("/main/target", "unchained");
    link_handler.clearArguments();
    link_link_handler.clearArguments();
    assertThat(directory.handle(base_directory, request), is(1));  // 0 real + 1 virtual
    assertThat(link_handler.getArgument(0), is("unchained"));
    Assert.assertNull(link_link_handler.getArgument(0));

    // ensure that linked link cannot get relinked after lower link has been unlinked
    // first unlink and relink link
    Assert.assertTrue(directory.unlink(base_directory + "/link",
                                       base_directory + "/main/target"));
    Assert.assertTrue(directory.link(base_directory + "/link",
                                     base_directory + "/main/target"));
    // link link_link
    Assert.assertTrue(directory.link(base_directory + "/link_link",
                                     base_directory + "/link/target"));
    // now unlink link, this creates a stale link linked by link_link
    Assert.assertTrue(directory.unlink(base_directory + "/link",
                                       base_directory + "/main/target"));
    // now add a handler to link_link and ensure that stale link does not become active again
    Assert.assertTrue(directory.addHandler(base_directory + "/link_link/target",
                                           link_link_handler));
    request = TestUtilities.createRequest("/main/target", "stale link");
    link_handler.clearArguments();
    link_link_handler.clearArguments();
    assertThat(directory.handle(base_directory, request), is(0));  // 0 real + 0 virtual
    Assert.assertNull(link_handler.getArgument(0));
    Assert.assertNull(link_link_handler.getArgument(0));
  }
}
