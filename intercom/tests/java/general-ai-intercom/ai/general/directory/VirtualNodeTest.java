/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

import ai.general.directory.test.TestHandler;
import ai.general.directory.test.TestUtilities;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for {@link VirtualNode} class.
 */
public class VirtualNodeTest {

  /**
   * Tests that basic node operations are relayed to the underlying node.
   */
  @Test
  public void operations() {
    DirectoryNode real = new DirectoryNode("real");
    VirtualNode virtual = new VirtualNode(real, "virtual");
    VirtualNode super_virtual = new VirtualNode(virtual, "super");
    DirectoryNode child = new DirectoryNode("child");

    assertThat(virtual.getName(), is("real"));
    assertThat(super_virtual.getName(), is("real"));
    try {
      super_virtual.mount(child);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e.toString());
    }
    assertThat(super_virtual.numChildren(), is(1));
    Assert.assertTrue(super_virtual.hasChild("child"));
    Assert.assertTrue(super_virtual.getChild("child") instanceof VirtualNode);
    Assert.assertTrue(super_virtual.isReachable(child));

    assertThat(virtual.numChildren(), is(1));
    Assert.assertTrue(virtual.hasChild("child"));
    Assert.assertTrue(virtual.getChild("child") instanceof VirtualNode);
    Assert.assertTrue(virtual.isReachable(child));

    assertThat(real.numChildren(), is(1));
    Assert.assertTrue(real.hasChild("child"));
    assertThat(real.getChild("child"), is((Node) child));
    Assert.assertTrue(real.isReachable(child));

    super_virtual.unmount(child);
    assertThat(super_virtual.numChildren(), is(0));
    assertThat(real.numChildren(), is(0));    

    // Virtual nodes cannot reach themsevles.
    Assert.assertFalse(virtual.isReachable(virtual));
  }

  /**
   * Tests behavior of handlers with virtual nodes.
   */
  @Test
  public void handle() {
    DirectoryNode real = new DirectoryNode("real");
    VirtualNode virtual = new VirtualNode(real, "virtual");
    VirtualNode super_virtual = new VirtualNode(virtual, "super");
    TestHandler handler = new TestHandler("handler");
    TestHandler catchall = new TestHandler("catchall", true);
    try {
      super_virtual.addHandler(handler);
      super_virtual.addHandler(catchall);
    } catch (NodeException e) {
      Assert.fail("Unexpected exception: " + e.toString());
    }
    Assert.assertTrue(super_virtual.hasHandler("handler"));
    Assert.assertTrue(super_virtual.hasHandler("catchall"));
    Assert.assertFalse(virtual.hasHandler("handler"));
    Assert.assertFalse(virtual.hasHandler("catchall"));
    Assert.assertFalse(real.hasHandler("handler"));
    Assert.assertFalse(real.hasHandler("catchall"));

    Request request = TestUtilities.createRequest("/", "imaginary");
    // Note that the number of run handlers is 1 since the VirtualNode appears as one
    // handler.
    assertThat(real.handle(request), is(1));
    assertThat(handler.getArgument(0), is("imaginary"));
    assertThat(catchall.getArgument(0), is("imaginary"));

    request = TestUtilities.createRequest("/", "indirect");
    assertThat(virtual.handle(request), is(1));
    assertThat(handler.getArgument(0), is("indirect"));
    assertThat(catchall.getArgument(0), is("indirect"));

    handler.clearArguments();
    catchall.clearArguments();
    request = TestUtilities.createRequest("/", "super_indirect");
    assertThat(super_virtual.handle(request), is(1));
    assertThat(handler.getArgument(0), is("super_indirect"));
    assertThat(catchall.getArgument(0), is("super_indirect"));

    handler.clearArguments();
    catchall.clearArguments();
    request = TestUtilities.createRequest("/child", "everything");
    assertThat(real.handle(request), is(1));
    Assert.assertNull(handler.getArgument(0));
    assertThat(catchall.getArgument(0), is("everything"));
    assertThat(catchall.getPathRemainder(), is("child"));

    virtual.deactivate();
    assertThat(real.handle(request), is(0));
  }
}
