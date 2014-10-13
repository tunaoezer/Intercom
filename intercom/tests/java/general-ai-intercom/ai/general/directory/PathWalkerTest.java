/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

import ai.general.net.Uri;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tets for the {@link PathWalker} class.
 */
public class PathWalkerTest {

  private static final String kFullUri = "wamp://testuser@test.general.ai:8080/" +
    "general.ai/api/robot@test.general.ai/topic/category?action=test#fragment";
  private static final String kRelativeUri = "top/down";
  private static final String kOneLevel = "one-level";
  private static final String kEmptyPath = "http://root.net/";
  private static final String kWildUri = "wamp://testuser@test.general.ai/test/robots/*";
  private static final String kEverything = "*";

  /**
   * Test walking a full URI with all major components.
   */
  @Test
  public void walk() {
    PathWalker walker = null;
    walker = new PathWalker(new Uri(kFullUri));
    assertThat(walker.numNodes(), is(6));
    assertThat(walker.getCurrentLevel(), is(0));
    assertThat(walker.atLeaf(), is(false));
    assertThat(walker.endsWithWildcard(), is(false));
    assertThat(walker.getCurrentNodeName(), is(""));
    assertThat(walker.remainder(), is("general.ai/api/robot@test.general.ai/topic/category"));
    Assert.assertTrue(walker.moveDown());
    assertThat(walker.getCurrentLevel(), is(1));
    assertThat(walker.getCurrentNodeName(), is("general.ai"));
    assertThat(walker.remainder(), is("api/robot@test.general.ai/topic/category"));
    Assert.assertTrue(walker.moveDown());
    assertThat(walker.getCurrentNodeName(), is("api"));
    assertThat(walker.remainder(), is("robot@test.general.ai/topic/category"));
    Assert.assertTrue(walker.moveDown());
    assertThat(walker.getCurrentNodeName(), is("robot@test.general.ai"));
    assertThat(walker.remainder(), is("topic/category"));
    Assert.assertTrue(walker.moveDown());
    assertThat(walker.getCurrentNodeName(), is("topic"));
    assertThat(walker.remainder(), is("category"));
    Assert.assertTrue(walker.moveDown());
    assertThat(walker.getCurrentNodeName(), is("category"));
    assertThat(walker.getCurrentLevel(), is(5));
    assertThat(walker.atLeaf(), is(true));
    assertThat(walker.remainder(), is(""));
    Assert.assertFalse(walker.moveDown());
    assertThat(walker.getCurrentLevel(), is(5));
    Assert.assertTrue(walker.moveUp());
    assertThat(walker.getCurrentLevel(), is(4));
    assertThat(walker.getCurrentNodeName(), is("topic"));
    assertThat(walker.atLeaf(), is(false));
    Assert.assertTrue(walker.moveUp());
    Assert.assertTrue(walker.moveUp());
    Assert.assertTrue(walker.moveUp());
    Assert.assertTrue(walker.moveUp());
    assertThat(walker.getCurrentLevel(), is(0));
    assertThat(walker.remainder(), is("general.ai/api/robot@test.general.ai/topic/category"));
    Assert.assertFalse(walker.moveUp());
    assertThat(walker.getCurrentLevel(), is(0));
  }

  /**
   * Tests walking a URI that represents a relative path.
   */
  @Test
  public void walkRelative() {
    PathWalker walker = null;
    walker = new PathWalker(new Uri(kRelativeUri));
    assertThat(walker.numNodes(), is(3));
    assertThat(walker.getCurrentNodeName(), is(""));
    Assert.assertTrue(walker.moveDown());
    assertThat(walker.getCurrentNodeName(), is("top"));
    assertThat(walker.atLeaf(), is(false));
    assertThat(walker.remainder(), is("down"));
    Assert.assertTrue(walker.moveDown());
    assertThat(walker.getCurrentNodeName(), is("down"));
    assertThat(walker.atLeaf(), is(true));
  }
  
  /**
   * Tests walking a path with only one level.
   */
  @Test
  public void walkOneLevel() {
    PathWalker walker = new PathWalker(kOneLevel);
    assertThat(walker.numNodes(), is(2));
    assertThat(walker.getCurrentNodeName(), is(""));
    assertThat(walker.atLeaf(), is(false));
    assertThat(walker.remainder(), is(kOneLevel));
    Assert.assertTrue(walker.moveDown());
    assertThat(walker.getCurrentNodeName(), is(kOneLevel));
    assertThat(walker.atLeaf(), is(true));
    assertThat(walker.remainder(), is(""));
  }

  /**
   * Tests walking an empty URI.
   */
  @Test
  public void walkEmpty() {
    PathWalker walker = null;
    walker = new PathWalker(new Uri(kEmptyPath));
    assertThat(walker.numNodes(), is(1));
    assertThat(walker.getCurrentNodeName(), is(""));
    assertThat(walker.atLeaf(), is(true));
    assertThat(walker.getCurrentLevel(), is(0));
    Assert.assertFalse(walker.moveDown());
    assertThat(walker.getCurrentLevel(), is(0));
    Assert.assertFalse(walker.moveUp());
    assertThat(walker.getCurrentLevel(), is(0));
  }

  /**
   * Tests wildcard behavior.
   */
  @Test
  public void wildWalk() {
    PathWalker walker = null;
    walker = new PathWalker(new Uri(kWildUri));
    assertThat(walker.endsWithWildcard(), is(true));
    assertThat(walker.numNodes(), is(3));
    assertThat(walker.remainder(), is("test/robots"));
    while (walker.moveDown());
    assertThat(walker.getCurrentLevel(), is(2));
    assertThat(walker.getCurrentNodeName(), is("robots"));

    walker = new PathWalker(kEverything);
    assertThat(walker.endsWithWildcard(), is(true));
    assertThat(walker.numNodes(), is(1));
    assertThat(walker.getCurrentNodeName(), is(""));
    assertThat(walker.atLeaf(), is(true));
    assertThat(walker.remainder(), is(""));
  }
}
