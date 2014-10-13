/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

import java.util.Collection;

/**
 * Base class for nodes. A node represents a resource or group of resources identified via a URI
 * and provides a mechanism to handle requests directed to those resources.
 *
 * A node is addressable via a URI and is arranged in an acyclic directed graph. A node can have
 * child nodes and multiple parent nodes.
 *
 * A node can be associated with one or more {@link Handler} instances that process requests to
 * the resources represented by the node or its child nodes.
 */
public abstract class Node {

  /**
   * Adds the specified handler to this node's handler list.
   * A NodeException is thrown if a handler with the same name already exists.
   *
   * If the handler indicates that it is a catch-all handler, it will also be added to
   * the catch-all list. A handler cannot change its catch-all status after it has been
   * added.
   *
   * @param handler The handler to add.
   * @throws NodeException if the handler cannot be added to this node.
   */
  public abstract void addHandler(Handler handler) throws NodeException;

  /**
   * Provides access to child nodes.
   *
   * @param name Name of the child node.
   * @return The child node or null if there is no child node with the specified name.
   */
  public abstract Node getChild(String name);

  /**
   * Provides access to child nodes.
   *
   * @return The child nodes mounted at this node.
   */
  public abstract Collection<Node> getChildren();

  /**
   * Node name.
   *
   * @return The name of this node.
   */
  public abstract String getName();

  /**
   * Handles the specified request.
   *
   * This method traverses the node hierarchy until the target node is found. Along the way
   * any catch-all handlers at each node level are executed. At the target node all handlers
   * are exeucted.
   *
   * The request URI is evaluated with respect to this node. The target node of the request must
   * be reachable by this node.
   *
   * If the target node does not exist, no regular request handlers will be executed, but
   * catch-all handlers along the path are always executed.
   *
   * @param request The request to handle.
   * @return The total number of handlers executed.
   */
  public abstract int handle(Request request);

  /**
   * Checks whether this node has the specified child.
   *
   * @param name Name of the child node.
   * @return True if this node has a direct child with the specified name.
   */
  public abstract boolean hasChild(String name);

  /**
   * Checks whether this node has the specified handler.
   *
   * @param name Name of handler.
   * @return True if the Node has a handler with the specified name.
   */
  public abstract boolean hasHandler(String name);

  /**
   * Checks whether a node is reachable from this node.
   *
   * @return True if there is a path from this node to the specified node.
   */
  public abstract boolean isReachable(Node node);

  /**
   * Mounts the specified node under this node. The mounted node becomes a child of this node.
   *
   * A NodeException is thrown if another node with the same name is already mounted or
   * mounting the node would create a cyclic relationship.
   *
   * @param child Node to be mounted under this node.
   * @throws NodeException if the child node cannot be mounted under this node.
   */
  public abstract void mount(Node child) throws NodeException;

  /**
   * Returns the number of mounted children.
   *
   * @return The number of child nodes mounted at this node.
   */
  public abstract int numChildren();

  /**
   * Removes the handler with the specified name from the list of handlers if
   * such a handler was previously added.
   *
   * @param name Name of handler to remove.
   * @return True if the handler was removed.
   */
  public abstract boolean removeHandler(String name);

  /**
   * Unmounts the previously mounted child node.
   * If the node is not mounted, results in a no-op.
   *
   * @param child Child node to be unmounted.
   * @return True if the node was unmounted.
   */
  public abstract boolean unmount(Node child);

  /**
   * Helper method to walk down a directory path while handling a request.
   *
   * @param request The request to handle.
   * @param path_walker PathWalker to help find the target node.
   * @return The total number of handlers executed.
   */
  protected abstract int handle(Request request, PathWalker path_walker);
}
