/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Implements a directory node. A directory node represents a resource or group of resources
 * identified via a URI and provides a mechanism to handle requests directed to those resources.
 *
 * DirectoryNode is thread-safe.
 */
public class DirectoryNode extends Node {

  /**
   * Constructs a DirectoryNode with the specified name. The node name is used to refer to this
   * node in paths and URI's.
   *
   * @param name The name that is used to identify this node.
   */
  public DirectoryNode(String name) {
    this.name_ = name;
    children_ = new HashMap<String, Node>();
    handlers_ = new HashMap<String, Handler>();
    catch_all_handlers_ = new ArrayList<Handler>();
  }

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
  @Override
  public synchronized void addHandler(Handler handler) throws NodeException {
    if (handlers_.containsKey(handler.getName())) {
      throw new NodeException(NodeException.Reason.DuplicateName);
    }
    handlers_.put(handler.getName(), handler);
    if (handler.isCatchAll()) {
      catch_all_handlers_.add(handler);
    }
  }

  /**
   * Provides access to child nodes.
   *
   * @param name Name of the child node.
   * @return The child node or null if there is no child node with the specified name.
   */
  @Override
  public Node getChild(String name) {
    return children_.get(name);
  }

  /**
   * Provides access to child nodes.
   *
   * @return The child nodes mounted at this node.
   */
  @Override
  public Collection<Node> getChildren() {
    return Collections.unmodifiableCollection(children_.values());
  }

  /**
   * Node name.
   *
   * @return The name of this node.
   */
  @Override
  public String getName() {
    return name_;
  }

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
  @Override
  public int handle(Request request) {
    return handle(request, new PathWalker(request.getUri()));
  }

  /**
   * Checks whether this node has the specified child.
   *
   * @param name Name of the child node.
   * @return True if this node has a direct child with the specified name.
   */
  @Override
  public boolean hasChild(String name) {
    return children_.containsKey(name);
  }

  /**
   * Checks whether this node has the specified handler.
   *
   * @param name Name of handler.
   * @return True if the Node has a handler with the specified name.
   */
  @Override
  public boolean hasHandler(String name) {
    return handlers_.containsKey(name);
  }

  /**
   * Checks whether a node is reachable from this node.
   *
   * @return True if there is a path from this node to the specified node.
   */
  @Override
  public boolean isReachable(Node node) {
    if (node == this) {
      return true;
    }
    for (Node child : children_.values()) {
      if (child.isReachable(node)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Mounts the specified node under this node. The mounted node becomes a child of this node.
   *
   * A NodeException is thrown if another node with the same name is already mounted or
   * mounting the node would create a cyclic relationship.
   *
   * @param child Node to be mounted under this node.
   * @throws NodeException if the child node cannot be mounted under this node.
   */
  @Override
  public synchronized void mount(Node child) throws NodeException {
    if (children_.containsKey(child.getName())) {
      throw new NodeException(NodeException.Reason.DuplicateName);
    }
    if (child.isReachable(this)) {
      throw new NodeException(NodeException.Reason.Cyclic);
    }
    children_.put(child.getName(), child);
  }

  /**
   * Returns the number of mounted children.
   *
   * @return The number of child nodes mounted at this node.
   */
  @Override
  public int numChildren() {
    return children_.size();
  }

  /**
   * Removes the handler with the specified name from the list of handlers if
   * such a handler was previously added.
   *
   * @param name Name of handler to remove.
   * @return True if the handler was removed.
   */
  @Override
  public synchronized boolean removeHandler(String name) {
    Handler handler = handlers_.get(name);
    if (handler == null) {
      return false;
    }
    catch_all_handlers_.remove(handler);
    return handlers_.remove(name) != null;
  }

  /**
   * Unmounts the previously mounted child node.
   * If the node is not mounted, results in a no-op.
   *
   * @param child Child node to be unmounted.
   * @return True if the node was unmounted.
   */
  @Override
  public synchronized boolean unmount(Node child) {
    return children_.remove(child.getName()) != null;
  }

  /**
   * Helper method to walk down a directory path while handling a request.
   *
   * @param request The request to handle.
   * @param path_walker PathWalker to help find the target node.
   * @return The total number of handlers executed.
   */
  @Override
  protected int handle(Request request, PathWalker path_walker) {
    log.entry();
    int executed_handler_count = 0;
    if (path_walker.atLeaf()) {
      for (Handler handler : handlers_.values()) {
        handler.handle(request);
        executed_handler_count++;
      }
    } else {
      if (catch_all_handlers_.size() > 0) {
        String path_remainder = path_walker.remainder();
        for (Handler handler : catch_all_handlers_) {
          handler.handleCatchAll(path_remainder, request);
          executed_handler_count++;
        }
      }
      path_walker.moveDown();
      Node child = children_.get(path_walker.getCurrentNodeName());
      if (child != null) {
        executed_handler_count += child.handle(request, path_walker);
      } else {
        log.trace("target node not found: {}", request.getUri().toString());
      }
    }
    log.exit();
    return executed_handler_count;
  }

  private static Logger log = LogManager.getLogger();

  // Catch-all handlers handle requests that target this node or a decendant of this node.
  // Catch-all handlers are also a members of handlers_.
  private ArrayList<Handler> catch_all_handlers_;

  // Child nodes indexed by their name.
  private HashMap<String, Node> children_;

  // List of handlers associated with this node.
  private HashMap<String, Handler> handlers_;

  // Name of this node. The name is used in directory paths.
  private String name_;
}
