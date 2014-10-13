/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Represents a directory of nodes. A directory node represents a resource that is identified via
 * a URI. A directory is an acyclic graph of nodes representing a collection of resources.
 *
 * Directory nodes are addressable with a directory path that represents the sequence of nodes
 * from the root node to the target node. A particular node can be addressable with more than
 * one path.
 *
 * Directory can be used to create, delete and link nodes. Directory can be also used to
 * add, remove and execute node handlers.
 *
 * Linking and unlinking of nodes can be used to grant temporary access to a subset of nodes
 * Linking creates a virtual overlay that is removed by unlinking. This ensures that the access
 * is cleanly removed when the node is unlinked automatically removing any handlers that were
 * associated with the link.
 *
 * Directory is a singleton class.
 */
public class Directory {

  /**
   * Singleton instance.
   */
  public static final Directory Instance = new Directory();

  /**
   * Directory is singleton.
   * The singleton instance can be obtained via {@link #Instance}.
   */
  private Directory() {
    root_ = new DirectoryNode("");
  }

  /**
   * Adds the specified handler to the node at the specified path.
   *
   * The handler will not be added if the path does not exist or a handler with the same
   * name was already added.
   * The path must be absolute. Wildcards are ignored.
   *
   * @param path Path at which to add the handler.
   * @param handler The handler to add.
   * @return True if the handler was added.
   */
  public boolean addHandler(String path, Handler handler) {
    log.trace("add handler: {} => {}", path, handler.getName());
    Node node = getNode(path);
    if (node == null) {
      return false;
    }
    try {
      node.addHandler(handler);
    } catch (NodeException e) {
      log.catching(Level.TRACE, e);
      return false;
    }
    return true;
  }

  /**
   * Creates the specified path. Creates and adds any necessary nodes along the path.
   *
   * The path must be an absolute path starting at the root, e.g. "/node1/node2/node3".
   * Wildcards are ignored.
   *
   * Results in a no-op if the path already exists.
   *
   * @param path The path to add to the directory.
   * @return True if the path was created.
   */
  public boolean createPath(String path) {
    log.entry(path);
    PathWalker path_walker = new PathWalker(path);
    if (path_walker.numNodes() < 2 || path_walker.getCurrentNodeName().length() > 0) {
      return false;
    }
    Node node = root_;
    while (path_walker.moveDown()) {
      if (node.hasChild(path_walker.getCurrentNodeName())) {
        node = node.getChild(path_walker.getCurrentNodeName());
      } else {
        Node child = new DirectoryNode(path_walker.getCurrentNodeName());
        try {
          node.mount(child);
        } catch (NodeException e) {
          // createPath() checks for duplicate names and does not add cycles by construction.
          log.catching(Level.DEBUG, e);
          return false;
        }
        node = child;
      }
    }
    log.exit();
    return true;
  }

  /**
   * Returns the node at the specified path.
   * The path must be must be absolute (i.e., start with '/') and is interpreted relative to the
   * root of the directory.
   * If the path ends with a wildcard returns the node just before the wildcard.
   *
   * @return The node at the specified path or null if no such path exists.
   */
  public Node getNode(String path) {
    PathWalker path_walker = new PathWalker(path);
    if (path_walker.numNodes() < 1 || path_walker.getCurrentNodeName().length() > 0) {
      return null;
    }
    Node node = root_;
    while (path_walker.moveDown()) {
      if (node.hasChild(path_walker.getCurrentNodeName())) {
        node = node.getChild(path_walker.getCurrentNodeName());
      } else {
        log.trace("node not found: {}", path);
        // Path does not exist.
        return null;
      }
    }
    return node;
  }

  /**
   * Handles a request. This method invokes handle on the specified base directory node.
   *
   * The target node path is the concatenation of the base path and the request URI path.
   * All regular handlers at the target node and catch-all handlers between the base node and
   * target node are executed.
   *
   * The request URI is interpreted relative to the base path.
   * The request URI cannot refer to a node that is not reachable from the base path.
   *
   * @param base_path The base directory node with respect to which the request will be executed.
   * @param request The request to handle.
   * @return The total number of handlers executed.
   */
  public int handle(String base_path, Request request) {
    log.trace("handle: {} => {}", base_path, request.getUri().toString());
    Node base = getNode(base_path);
    if (base == null) {
      return 0;
    }
    return base.handle(request);
  }

  /**
   * Checks whether a handler at the specified path exists.
   * Returns false if the path does not exist.
   *
   * @param path Absolute path to node.
   * @param handler_name Name of handler.
   * @return True if the node exists and has a handler with the specified name.
   */
  public boolean hasHandler(String path, String handler_name) {
    Node node = getNode(path);
    if (node == null) {
      return false;
    }
    return node.hasHandler(handler_name);
  }

  /**
   * Creats a link from the 'from' node to the 'to' node.
   *
   * Once the link has been created, the 'to' node and all of its subnodes become reachable
   * via the 'from' node.
   *
   * Both the 'from' and 'to' nodes must exist.
   * The 'from' node must not already have a child node with the same name as the 'to' node.
   * The 'from' and 'to' paths must not refer to the same node or create a cyclic link.
   *
   * Results in a no-op if the nodes do not exist, are already linked, or the 'from' node already
   * has a child with the same name as the 'to' node.
   *
   * Links are created using VirtualNodes. This creats an overlay layer on top of the actual
   * directory node. Handlers added to the linked node do not affect the actual target node. They
   * only affect the virtual node that represents the linked target node.
   *
   * All handlers added via the link are automatically removed when the node is unlinked.
   *
   * @param from The absolute path to the node to which to add the link.
   * @param to The absolute of the target node of the link.
   * @return True if the link has been created.
   */
  public boolean link(String from, String to) {
    log.trace("link: {} -> {}", from, to);
    Node from_node = getNode(from);
    if (from_node == null) {
      return false;
    }
    Node to_node = getNode(to);
    if (to_node == null) {
      return false;
    }
    String virtual_handler_name = "virtual:" + from;
    if (to_node.hasHandler(virtual_handler_name)) {
      return false;
    }
    try {
      VirtualNode virtual_node = new VirtualNode(to_node, "virtual:" + from);
      from_node.mount(virtual_node);
      return true;
    } catch (NodeException e) {
      log.catching(Level.TRACE, e);
      return false;
    }
  }

  /**
   * Checks whether the specified path exists.
   *
   * @return True if a node is defined for the specified path.
   */
  public boolean pathExists(String path) {
    return getNode(path) != null;
  }

  /**
   * Removes the handler with the specified name from the node at the specified path.
   *
   * Results in a no-op if the path does not exist or there is no such handler.
   * The path must be absolute. Wildcards are ignored.
   *
   * @param path Path from which to remove the handler.
   * @param handler_name The name of the handler to remove.
   * @return True if the handler was removed.
   */
  public boolean removeHandler(String path, String handler_name) {
    log.trace("remove handler: {} => {}", path, handler_name);
    Node node = getNode(path);
    if (node == null) {
      return false;
    }
    return node.removeHandler(handler_name);
  }

  /**
   * Removes the specified path from the directory. This is achieved by unmounting the last
   * node of the path. For example, if the path "/a/b/c" is removed, node "c" is unmounted.
   * After the operation, the path "/a/b" still exists, but "/a/b/c" or "/a/b/c/d" will
   * have been removed. This operation does not delete any the nodes if the nodes have been
   * also mounted under other nodes.
   *
   * The path must be absolute. Wildcards are ignored.
   * Results in a no-op if the path does not exist.
   * The root node cannot be removed.
   *
   * @param path The path to be removed from the directory.
   * @return True if the path was removed.
   */
  public boolean removePath(String path) {
    log.entry(path);
    PathWalker path_walker = new PathWalker(path);
    if (path_walker.numNodes() < 2 || path_walker.getCurrentNodeName().length() > 0) {
      return false;
    }
    Node parent = root_;
    Node node = root_;
    while (path_walker.moveDown()) {
      if (node.hasChild(path_walker.getCurrentNodeName())) {
        parent = node;
        node = node.getChild(path_walker.getCurrentNodeName());
      } else {
        // Path does not exist.
        return false;
      }
    }
    log.exit();
    return parent.unmount(node);
  }

  /**
   * Unlinks the 'to' node from the 'from' node. This is the reverse of
   * {@link #link(String, String)}.
   *
   * This method will remove the 'to' node from the 'from' node even if the 'to' node was not
   * added by {@link #link(String, String)}. After this operation, the 'to' node will not be
   * reachable by 'from'. Children of 'to' may still be reachable if there are direct links to
   * those children.
   *
   * Unlinking removes the virtual overlay created by linking. This causes any handlers that were
   * added to the 'to' node or any of its decendants using the link to be automatically removed.
   *
   * After this operation the 'to' node and its children may be deleted if they are not mounted
   * onto another node.
   * Results in a no-op if the 'from' node have a child named 'to'.
   *
   * @param from The absolute to the node to which the link to be removed was added.
   * @param to The absolute path of the target node of the link to be removed.
   * @return True if the link was removed.
   */
  public boolean unlink(String from, String to) {
    log.trace("unlink: {} -> {}", from, to);
    Node from_node = getNode(from);
    if (from_node == null) {
      return false;
    }
    Node to_node = getNode(to);
    if (to_node == null) {
      return false;
    }
    Node virtual_node = from_node.getChild(to_node.getName());
    if (virtual_node instanceof VirtualNode) {
      ((VirtualNode) virtual_node).deactivate();
    }
    return from_node.unmount(virtual_node);
  }

  private static Logger log = LogManager.getLogger();

  // Represents the root of the directory.
  private Node root_;
}
