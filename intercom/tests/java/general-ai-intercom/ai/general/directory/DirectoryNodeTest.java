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
 * Tests for {@link DirectoryNode} class.
 */
public class DirectoryNodeTest {

  /**
   * Tests mounting and unmounting of nodes.
   */
  @Test
  public void mount() {
    Node node_a = new DirectoryNode("node_a");
    assertThat(node_a.getName(), is("node_a"));
    Node node_b1 = new DirectoryNode("node_b1");
    assertThat(node_a.isReachable(node_a), is(true));
    assertThat(node_a.isReachable(node_b1), is(false));
    assertThat(node_b1.isReachable(node_a), is(false));

    // simple mount: a->b1
    try {
      node_a.mount(node_b1);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }
    assertThat(node_a.numChildren(), is(1));
    assertThat(node_a.hasChild("node_b1"), is(true));
    assertThat(node_a.getChild("node_b1"), is(node_b1));
    assertThat(node_a.isReachable(node_b1), is(true));
    assertThat(node_b1.isReachable(node_a), is(false));

    // duplicate mount
    try {
      node_a.mount(node_b1);
      Assert.fail("Expected exception: DuplicateName.");
      return;
    } catch (NodeException e) {
      assertThat(e.getReason(), is(NodeException.Reason.DuplicateName));
    }

    // mount a second node: a->b2
    Node node_b2 = new DirectoryNode("node_b2");
    try {
      node_a.mount(node_b2);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }
    assertThat(node_a.numChildren(), is(2));
    assertThat(node_a.hasChild("node_b2"), is(true));
    assertThat(node_a.isReachable(node_b2), is(true));
    assertThat(node_b1.isReachable(node_b2), is(false));

    // mount more nodes: b1->c1->d1, b2->c2
    Node node_c1 = new DirectoryNode("node_c1");
    Node node_d1 = new DirectoryNode("node_d1");
    Node node_c2 = new DirectoryNode("node_c2");
    try {
      node_c1.mount(node_d1);
      node_b1.mount(node_c1);
      node_b2.mount(node_c2);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }
    assertThat(node_a.numChildren(), is(2));
    assertThat(node_a.hasChild("node_d1"), is(false));
    assertThat(node_a.isReachable(node_c1), is(true));
    assertThat(node_a.isReachable(node_d1), is(true));
    assertThat(node_a.isReachable(node_c2), is(true));
    assertThat(node_c2.isReachable(node_d1), is(false));

    // unmount
    Assert.assertTrue(node_a.unmount(node_b1));
    assertThat(node_a.numChildren(), is(1));
    assertThat(node_a.hasChild("node_b1"), is(false));
    assertThat(node_a.isReachable(node_d1), is(false));

    // re-mount at different location: a->b2->c2->b1->c1->d1
    try {
      node_c2.mount(node_b1);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }
    assertThat(node_a.isReachable(node_d1), is(true));

    // cyclic mount
    try {
      node_d1.mount(node_a);
      Assert.fail("Expected exception: Cyclic.");
      return;
    } catch (NodeException e) {
      assertThat(e.getReason(), is(NodeException.Reason.Cyclic));
    }

    // multiple paths: add a->d1
    try {
      node_a.mount(node_d1);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }
    assertThat(node_a.numChildren(), is(2));
    assertThat(node_a.hasChild("node_d1"), is(true));
  }

  /**
   * Tests execution of handlers.
   */
  @Test
  public void handle() {
    Node root = new DirectoryNode("");
    Node node_a1 = new DirectoryNode("a1");
    Node node_a1b1 = new DirectoryNode("b1");
    Node node_a1b2 = new DirectoryNode("b2");
    Node node_a1b1c1 = new DirectoryNode("c1");
    Node node_a2 = new DirectoryNode("a2");
    Node node_a2b1 = new DirectoryNode("b1");
    Node node_a2b2 = new DirectoryNode("b2");
    Node node_a2b1c1 = new DirectoryNode("c1");
    try {
      root.mount(node_a1);
      root.mount(node_a2);
      node_a1.mount(node_a1b1);
      node_a1.mount(node_a1b2);
      node_a1b1.mount(node_a1b1c1);
      node_a2.mount(node_a2b1);
      node_a2.mount(node_a2b2);
      node_a2b1.mount(node_a2b1c1);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }

    TestHandler test_handler_a1 = new TestHandler("handler-a1");
    TestHandler test_handler_a1b1_1 = new TestHandler("handler-a1b1-1");
    TestHandler test_handler_a1b1_2 = new TestHandler("handler-a1b1-2");
    TestHandler test_handler_a1b2 = new TestHandler("handler-a1b2");
    TestHandler test_handler_a1b1c1 = new TestHandler("handler-a1b1c1");
    TestHandler test_handler_a2b1c1 = new TestHandler("handler-a2b1c2");

    // add handlers
    try {
      node_a1.addHandler(test_handler_a1);
      node_a1b1.addHandler(test_handler_a1b1_1);
      node_a1b1.addHandler(test_handler_a1b1_2);
      node_a1b2.addHandler(test_handler_a1b2);
      node_a1b1c1.addHandler(test_handler_a1b1c1);
      node_a2b1c1.addHandler(test_handler_a2b1c1);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }

    // duplicate handler name
    try {
      node_a1.addHandler(test_handler_a1);
      Assert.fail("Expected exception: DuplicateName.");
      return;
    } catch (NodeException e) {
      assertThat(e.getReason(), is(NodeException.Reason.DuplicateName));
    }
    Assert.assertTrue(node_a1.hasHandler(test_handler_a1.getName()));

    // simple request to /a1/b1/c1 with full URI
    Request request = TestUtilities.createRequest(
        "wamp://user@domain/a1/b1/c1?type=publish#fragment", "[1]");
    assertThat(root.handle(request), is(1));
    Assert.assertNull(test_handler_a1.getArgument(0));
    Assert.assertNull(test_handler_a1b1_1.getArgument(0));
    Assert.assertNull(test_handler_a1b1_2.getArgument(0));
    Assert.assertNull(test_handler_a1b2.getArgument(0));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[1]"));
    Assert.assertNull(test_handler_a2b1c1.getArgument(0));

    // request to /a1/b1, multiple handlers
    request = TestUtilities.createRequest("wamp://user@domain/a1/b1", "[2]");
    assertThat(root.handle(request), is(2));
    Assert.assertNull(test_handler_a1.getArgument(0));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    Assert.assertNull(test_handler_a1b2.getArgument(0));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[1]"));
    Assert.assertNull(test_handler_a2b1c1.getArgument(0));

    // request to /a1/b2, with extra query parameter and fragment
    request = TestUtilities.createRequest(
        "wamp://user@domain/a1/b2?type=publish&param=true#fragment", "[3]");
    assertThat(root.handle(request), is(1));
    Assert.assertNull(test_handler_a1.getArgument(0));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[1]"));
    Assert.assertNull(test_handler_a2b1c1.getArgument(0));

    // request to /a1, no query or fragment
    request = TestUtilities.createRequest("wamp://user@domain/a1", "[4]");
    assertThat(root.handle(request), is(1));
    assertThat(test_handler_a1.getArgument(0), is("[4]"));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[1]"));
    Assert.assertNull(test_handler_a2b1c1.getArgument(0));

    // request to /a2, no handler
    request = TestUtilities.createRequest("wamp://user@domain/a2", "[5]");
    assertThat(root.handle(request), is(0));
    assertThat(test_handler_a1.getArgument(0), is("[4]"));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[1]"));
    Assert.assertNull(test_handler_a2b1c1.getArgument(0));

    // request to /a2/b1/c1
    request = TestUtilities.createRequest("wamp://user@domain/a2/b1/c1", "[6]");
    assertThat(root.handle(request), is(1));
    assertThat(test_handler_a1.getArgument(0), is("[4]"));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[1]"));
    assertThat(test_handler_a2b1c1.getArgument(0), is("[6]"));

    // request to /a2/b1/c1, with bare URI
    request = TestUtilities.createRequest("/a2/b1/c1", "[7]");
    assertThat(root.handle(request), is(1));
    assertThat(test_handler_a1.getArgument(0), is("[4]"));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[1]"));
    assertThat(test_handler_a2b1c1.getArgument(0), is("[7]"));

    // request to /a1/b1/c1 with non-absolute path
    request = TestUtilities.createRequest("a1/b1/c1", "[8]");
    assertThat(root.handle(request), is(1));
    assertThat(test_handler_a1.getArgument(0), is("[4]"));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[8]"));
    assertThat(test_handler_a2b1c1.getArgument(0), is("[7]"));

    // request to non-existent node
    request = TestUtilities.createRequest("wamp://user@domain/a1/b1/c1/d1", "[9]");
    assertThat(root.handle(request), is(0));
    assertThat(test_handler_a1.getArgument(0), is("[4]"));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[8]"));
    assertThat(test_handler_a2b1c1.getArgument(0), is("[7]"));

    // remove handler and verify via request
    Assert.assertTrue(node_a1b1.hasHandler(test_handler_a1b1_2.getName()));
    Assert.assertTrue(node_a1b1.removeHandler(test_handler_a1b1_2.getName()));
    Assert.assertFalse(node_a1b1.hasHandler(test_handler_a1b1_2.getName()));
    request = TestUtilities.createRequest("wamp://user@domain/a1/b1", "[10]");
    assertThat(root.handle(request), is(1));
    assertThat(test_handler_a1.getArgument(0), is("[4]"));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[10]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[8]"));
    assertThat(test_handler_a2b1c1.getArgument(0), is("[7]"));

    // request from non-root nodes
    request = TestUtilities.createRequest("wamp://user@domain/b1/c1", "[11]");
    // from /a1
    assertThat(node_a1.handle(request), is(1));
    assertThat(test_handler_a1.getArgument(0), is("[4]"));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[10]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[11]"));
    assertThat(test_handler_a2b1c1.getArgument(0), is("[7]"));

    // same request from /a2
    assertThat(node_a2.handle(request), is(1));
    assertThat(test_handler_a1.getArgument(0), is("[4]"));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[10]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[11]"));
    assertThat(test_handler_a2b1c1.getArgument(0), is("[11]"));

    // bare request from non-root nodes
    request = TestUtilities.createRequest("/c1", "[12]");
    // from /a1/b1
    assertThat(node_a1b1.handle(request), is(1));
    assertThat(test_handler_a1.getArgument(0), is("[4]"));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[10]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[12]"));
    assertThat(test_handler_a2b1c1.getArgument(0), is("[11]"));

    // same request from /a2/b1
    assertThat(node_a2b1.handle(request), is(1));
    assertThat(test_handler_a1.getArgument(0), is("[4]"));
    assertThat(test_handler_a1b1_1.getArgument(0), is("[10]"));
    assertThat(test_handler_a1b1_2.getArgument(0), is("[2]"));
    assertThat(test_handler_a1b2.getArgument(0), is("[3]"));
    assertThat(test_handler_a1b1c1.getArgument(0), is("[12]"));
    assertThat(test_handler_a2b1c1.getArgument(0), is("[12]"));
  }

  /**
   * Tests execution of catch-all handlers.
   */
  @Test
  public void handleCatchall() {
    Node root = new DirectoryNode("");
    Node node_a = new DirectoryNode("a");
    Node node_b1 = new DirectoryNode("b1");
    Node node_b2 = new DirectoryNode("b2");
    Node node_c = new DirectoryNode("c");
    try {
      root.mount(node_a);
      node_a.mount(node_b1);
      node_a.mount(node_b2);
      node_b1.mount(node_c);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }
    TestHandler test_handler_a = new TestHandler("handler-a", true);  // catch-all
    TestHandler test_handler_b1_1 = new TestHandler("handler-b1-1", true);  // catch-all
    TestHandler test_handler_b1_2 = new TestHandler("handler-b1-2");  // regular
    TestHandler test_handler_b2 = new TestHandler("handler-b2");  // regular
    TestHandler test_handler_c = new TestHandler("handler-c", true);  // catch-all
    try {
      node_a.addHandler(test_handler_a);
      node_b1.addHandler(test_handler_b1_1);
      node_b1.addHandler(test_handler_b1_2);
      node_b2.addHandler(test_handler_b2);
      node_c.addHandler(test_handler_c);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }

    // catch-all test (a and b1-1)
    Request request =
      TestUtilities.createRequest("wamp://user@domain/a/b1/c?type=publish", "[123]");
    assertThat(root.handle(request), is(3));
    assertThat(test_handler_a.getArgument(0), is("[123]"));
    assertThat(test_handler_a.getPathRemainder(), is("b1/c"));
    assertThat(test_handler_b1_1.getArgument(0), is("[123]"));
    assertThat(test_handler_b1_1.getPathRemainder(), is("c"));
    Assert.assertNull(test_handler_b1_2.getArgument(0));
    Assert.assertNull(test_handler_b2.getArgument(0));
    assertThat(test_handler_c.getArgument(0), is("[123]"));
    Assert.assertNull(test_handler_c.getPathRemainder());

    // catch-all on non-root node (b1-1)
    request = TestUtilities.createRequest("wamp://user@domain/c", "[321]");
    assertThat(node_b1.handle(request), is(2));
    assertThat(test_handler_a.getArgument(0), is("[123]"));
    assertThat(test_handler_b1_1.getArgument(0), is("[321]"));
    assertThat(test_handler_b1_1.getPathRemainder(), is("c"));
    Assert.assertNull(test_handler_b1_2.getArgument(0));
    Assert.assertNull(test_handler_b2.getArgument(0));
    assertThat(test_handler_c.getArgument(0), is("[321]"));
    Assert.assertNull(test_handler_c.getPathRemainder());

    // catch-all for non-existent path (a only)
    request = TestUtilities.createRequest("wamp://user@domain/a/b2/c", "[456]");
    assertThat(root.handle(request), is(1));
    assertThat(test_handler_a.getArgument(0), is("[456]"));
    assertThat(test_handler_a.getPathRemainder(), is("b2/c"));
    assertThat(test_handler_b1_1.getArgument(0), is("[321]"));
    Assert.assertNull(test_handler_b1_2.getArgument(0));
    Assert.assertNull(test_handler_b2.getArgument(0));
    assertThat(test_handler_c.getArgument(0), is("[321]"));

    // execution of all handlers on target node (catch-all and regular)
    request = TestUtilities.createRequest("wamp://user@domain/a/b1", "[789]");
    assertThat(root.handle(request), is(3));
    assertThat(test_handler_a.getArgument(0), is("[789]"));
    assertThat(test_handler_a.getPathRemainder(), is("b1"));
    assertThat(test_handler_b1_1.getArgument(0), is("[789]"));
    Assert.assertNull(test_handler_b1_1.getPathRemainder());
    assertThat(test_handler_b1_2.getArgument(0), is("[789]"));
    Assert.assertNull(test_handler_b1_2.getPathRemainder());
    Assert.assertNull(test_handler_b2.getArgument(0));
    assertThat(test_handler_c.getArgument(0), is("[321]"));

    // removing catch-all handlers
    Assert.assertTrue(node_b1.removeHandler(test_handler_b1_1.getName()));
    Assert.assertTrue(node_c.removeHandler(test_handler_c.getName()));
    test_handler_b1_1 = new TestHandler(test_handler_b1_1.getName(), false);
    test_handler_c = new TestHandler(test_handler_c.getName(), false);
    try {
      node_b1.addHandler(test_handler_b1_1);
      node_c.addHandler(test_handler_c);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }
    // a is catch-all, and c is target
    request = TestUtilities.createRequest("wamp://user@domain/a/b1/c", "[true]");
    assertThat(root.handle(request), is(2));
    assertThat(test_handler_a.getArgument(0), is("[true]"));
    Assert.assertNull(test_handler_b1_1.getArgument(0));
    Assert.assertNull(test_handler_b1_1.getPathRemainder());
    assertThat(test_handler_b1_2.getArgument(0), is("[789]"));
    Assert.assertNull(test_handler_b2.getArgument(0));
    assertThat(test_handler_c.getArgument(0), is("[true]"));
    Assert.assertNull(test_handler_c.getPathRemainder());
  }

  /**
   * Tests linking of a nodes by mounting a sub-hierarchy onto multiple nodes.
   * Includes tests for handlers.
   */
  @Test
  public void link() {
    Node root = new DirectoryNode("");
    Node node_a1 = new DirectoryNode("a1");
    Node node_a2 = new DirectoryNode("a2");
    Node node_b = new DirectoryNode("b");
    Node node_c = new DirectoryNode("c");
    try {
      root.mount(node_a1);
      root.mount(node_a2);
      node_a1.mount(node_b);
      node_b.mount(node_c);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }
    TestHandler test_handler_a1 = new TestHandler("handler-a1", true);  // catch-all
    TestHandler test_handler_a2 = new TestHandler("handler-a2", true);  // catch-all
    TestHandler test_handler_c = new TestHandler("handler-c");  // regular
    try {
      node_a1.addHandler(test_handler_a1);
      node_a2.addHandler(test_handler_a2);
      node_c.addHandler(test_handler_c);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }

    // direct
    Request request = TestUtilities.createRequest("/a1/b/c", "[1]");
    assertThat(root.handle(request), is(2));
    assertThat(test_handler_a1.getArgument(0), is("[1]"));
    Assert.assertNull(test_handler_a2.getArgument(0));
    assertThat(test_handler_c.getArgument(0), is("[1]"));

    // not linked
    request = TestUtilities.createRequest("/a2/b/c", "[2]");
    assertThat(root.handle(request), is(1));
    assertThat(test_handler_a1.getArgument(0), is("[1]"));
    assertThat(test_handler_a2.getArgument(0), is("[2]"));
    assertThat(test_handler_c.getArgument(0), is("[1]"));

    // link
    try {
      node_a2.mount(node_b);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e);
      return;
    }
    Assert.assertTrue(node_a2.isReachable(node_c));
    request = TestUtilities.createRequest("/a2/b/c", "[3]");
    assertThat(root.handle(request), is(2));
    assertThat(test_handler_a1.getArgument(0), is("[1]"));
    assertThat(test_handler_a2.getArgument(0), is("[3]"));
    assertThat(test_handler_a2.getPathRemainder(), is("b/c"));
    assertThat(test_handler_c.getArgument(0), is("[3]"));

    // unlink
    Assert.assertTrue(node_a2.unmount(node_b));
    Assert.assertFalse(node_a2.isReachable(node_c));
    request = TestUtilities.createRequest("/a2/b/c", "[4]");
    assertThat(root.handle(request), is(1));
    assertThat(test_handler_a1.getArgument(0), is("[1]"));
    assertThat(test_handler_a2.getArgument(0), is("[4]"));
    assertThat(test_handler_c.getArgument(0), is("[3]"));
  }
}
