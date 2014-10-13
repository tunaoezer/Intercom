/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

import ai.general.net.Uri;

/**
 * A PathWalker is used to traverse directory paths.
 * The PathWalker splits a directory path into an array of nodes and keeps track of
 * the current node. It can be used to move up and down in the node hierarchy.
 */
public class PathWalker {

  /**
   * Constructs a PathWalker from a directory path.
   *
   * The path may be either relative or absolute (i.e., start with a '/'). Regardless of whether
   * the pathis relative or absolute, the first level of a PathWalker always points to the current
   * directory node and has no name.
   * Thus, the paths "level1/level2" and "/level1/level2" produce both a 3 level PathWalker
   * with the first node equal to "", the second node equal to "level1" and the third node
   * equal to "level2".
   *
   * @param path The path for which the PathWalker will be constructed.
   */
  public PathWalker(String path) {
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    nodes_ = path.split("/");
    if (nodes_.length == 0) {
      nodes_ = new String[] { "" };
    }
    level_ = 0;
    ends_with_wildcard_ = nodes_.length > 1 && nodes_[nodes_.length - 1].equals("*");
    num_nodes_ = ends_with_wildcard_ ? nodes_.length - 1 : nodes_.length;
    leaf_level_ = num_nodes_ - 1;
  }

  /**
   * Constructs a PathWalker from the path component of a URI.
   *
   * @param uri The URI for which the PathWalker will be constructed.
   */
  public PathWalker(Uri uri) {
    this(uri.getPath());
  }

  /**
   * Checks whether the current node is a leaf node, i.e. there are no further named nodes in
   * the path. The path may contain a wildcard after the leaf node.
   *
   * @return True if the walker is at the lowest level.
   */
  public boolean atLeaf() {
    return level_ == leaf_level_;
  }

  /**
   * The level of the current node along the path. The level is the distance between this node
   * and the first node.
   *
   * @return The current level in the URI path hierarchy.
   */
  public int getCurrentLevel() {
    return level_;
  }

  /**
   * Name of current node along the path.
   *
   * @return The name of the current node.
   */
  public String getCurrentNodeName() {
    return nodes_[level_];
  }

  /**
   * Returns whether the paths ends with a '/*' wildcard.
   *
   * @return True if the path ends with a '/*'.
   */
  public boolean endsWithWildcard() {
    return ends_with_wildcard_;
  }

  /**
   * Moves to the next level in the node hierarchy, i.e. to the child node.
   * Does not move if the walker is already at the lowest level.
   *
   * @return False if the walker is already at the lowest level.
   */
  public boolean moveDown() {
    if (level_ == leaf_level_) {
      return false;
    }
    level_++;
    return true;
  }

  /**
   * Moves to the previous level in the node hierarchy, i.e. to the parent node.
   * Does not move if the walker is already at the root level.
   *
   * @return False if the walker is already at the root level.
   */
  public boolean moveUp() {
    if (level_ == 0) {
      return false;
    }
    level_--;
    return true;
  }

  /**
   * Total number of nodes along the path.
   *
   * @return The total number of nodes along the URI path.
   */
  public int numNodes() {
    return num_nodes_;
  }

  /**
   * Computes and returns the path from the current node to leaf node.
   * Wildcards are excluded.
   *
   * @return The remaining path to the leaf. Empty string if this is the leaf.
   */
  public String remainder() {
    if (atLeaf()) {
      return "";
    }
    StringBuilder remainder = new StringBuilder(nodes_[level_ + 1]);
    for (int i = level_ + 2; i <= leaf_level_; i++) {
      remainder.append('/');
      remainder.append(nodes_[i]);
    }
    return remainder.toString();
  }

  private final int leaf_level_;  // Lowest level (excludes wildcard).
  private final boolean ends_with_wildcard_;  // True if the path ends with '/*'.
  private int level_;  // Current position in nodes_.
  private String[] nodes_;  // The names of the nodes in the URI path.
  private final int num_nodes_;  // Number of nodes (excludes wildcard).
}
