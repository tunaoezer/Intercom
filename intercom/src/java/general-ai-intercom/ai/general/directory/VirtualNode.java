/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * VirtualNodes add an overlay layer on top of a directory hierarchy.
 *
 * A VirtualNode represents another {@link Node} object. Most operations on the VirtualNode
 * are relayed to the underlying Node object. Thus, a VirtualNode appears mostly like its
 * underlying node from outside.
 *
 * VirtualNode maintains its own set of handlers. Handlers are added to the set of handlers
 * of the VirtualNode rather than the underlying node.
 *
 * VirtualNodes do not have any children. An attempt to mount a node to a VirtualNode mounts
 * it to the underlying node.
 *
 * A VirtualNode functions like a handler and adds itself as a handler to its underlying Node.
 * Any request that target the underlying node are also automatically relayed to all handlers
 * of the VirtualNode.
 *
 * Typically, a VirtualNode will be used in conjunction with linking. Rather than linking
 * a node directly, a VirtualNode can be linked that represents the linked node.
 * This ensures that adding and removing handlers only affects the VirtualNode rather than
 * the underlying node. When the node is unlinked, the handlers are automatically removed
 * with the VirtualNode. Such behavior is necessary in order to provide correct access
 * control with user accounts.
 */
public class VirtualNode extends Node {

  /**
   * Implements a handler for the VirtualNode. Relays requests to the handlers of the
   * VirtualNode.
   */
  private class VirtualNodeHandler extends Handler {

    /**
     * Constructs a VirtualNodeHandler with the specified name.
     * VirtualNodeHandlers are always catch-all handlers.
     *
     * @param name A unique handler name.
     */
    public VirtualNodeHandler(String name) {
      super(name, true);
    }

    /**
     * Handles the request.
     *
     * @param request The request to handle.
     */
    @Override
    public void handle(Request request) {
      for (Handler handler : handlers_.values()) {
        handler.handle(request);
      }
    }

    /**
     * Handles catch-all requests.
     *
     * @param path_remainder The relative path from the calling node to the target of the request.
     * @param request The request to handle.
     */
    @Override
    public void handleCatchAll(String path_remainder, Request request) {
      for (Handler handler : catch_all_handlers_) {
        handler.handleCatchAll(path_remainder, request);
      }
    }
  }

  /**
   * Creates a virtual node that represents the specified node.
   * A virtual node handler with the specified handler name is added to the node. The handler name
   * must be unique for the underlying node.
   *
   * @param node The underlying node.
   * @param handler_name The name of the virtual node handler to be added to the underlying node.
   */
  public VirtualNode(Node node, String handler_name) {
    this.node_ = node;
    active_ = true;
    children_ = new HashMap<String, VirtualNode>();
    handlers_ = new HashMap<String, Handler>();
    catch_all_handlers_ = new ArrayList<Handler>();
    virtual_handler_ = new VirtualNodeHandler(handler_name);
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
  public void addHandler(Handler handler) throws NodeException {
    if (handlers_.containsKey(handler.getName())) {
      throw new NodeException(NodeException.Reason.DuplicateName);
    }
    handlers_.put(handler.getName(), handler);
    if (active_ && handlers_.size() == 1) {
      node_.addHandler(virtual_handler_);
    }
    if (handler.isCatchAll()) {
      catch_all_handlers_.add(handler);
    }
  }

  /**
   * Deactivates the virtual node. When a virtual node is deactivated, its virtual node handler
   * is removed from the underlying node and requests to the underlying node will not be relayed
   * anymore to the virtual node. Once deactivated, a virtual node cannot be re-activated anymore.
   *
   * Typically, a virtual node is deactivated just before it is discarded. It is necessary to
   * explicitly deactivate a virtual node, since otherwise stale links may persist after the
   * virtual node is discarded causing unintended relay of requests to the virtual node.
   * 
   * This method must be called to properly remove an overlay layer when a node is unlinked from
   * another node. The virtual node overlay cannot be destroyed until this method is called.
   *
   * Deactivating a virtual node ensures that secondary links to this virtual node do not cause a
   * stale link to be re-established. Unintended re-establishment of a stale link via a secondary
   * link to a virtual node can pose a security vulnerability.
   *
   * @return True if the virtual node handler was removed from the underlying node.
   */
  public boolean deactivate() {
    active_ = false;
    for (VirtualNode child : children_.values()) {
      child.deactivate();
    }
    return node_.removeHandler(virtual_handler_.getName());
  }

  /**
   * Provides access to child nodes.
   *
   * @param name Name of the child node.
   * @return The child node or null if there is no child node with the specified name.
   */
  @Override
  public Node getChild(String name) {
    if (children_.containsKey(name)) {
      return children_.get(name);
    } else {
      Node child = node_.getChild(name);
      if (child == null) {
        return null;
      }
      VirtualNode virtual_child =
        new VirtualNode(child, virtual_handler_.getName() + ":" + child.getName());
      children_.put(name, virtual_child);
      return virtual_child;
    }
  }

  /**
   * Provides access to child nodes.
   *
   * @return The child nodes mounted at this node.
   */
  @Override
  public Collection<Node> getChildren() {
    return node_.getChildren();
  }

  /**
   * Node name.
   *
   * @return The name of this node.
   */
  @Override
  public String getName() {
    return node_.getName();
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
    return node_.handle(request);
  }

  /**
   * Checks whether this node has the specified child.
   *
   * @param name Name of the child node.
   * @return True if this node has a direct child with the specified name.
   */
  @Override
  public boolean hasChild(String name) {
    return node_.hasChild(name);
  }

  /**
   * This method returns true if the virtual node has the specified handler.
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
    return node_.isReachable(node);
  }

  /**
   * Mounts the specified node under this node. The mounted node becomes a child of the underlying
   * node.
   *
   * A NodeException is thrown if another node with the same name is already mounted or
   * mounting the node would create a cyclic relationship.
   *
   * @param child Node to be mounted under this node.
   * @throws NodeException if the child node cannot be mounted under this node.
   */
  @Override
  public void mount(Node child) throws NodeException {
    node_.mount(child);
  }

  /**
   * Returns the number of mounted children.
   *
   * @return The number of child nodes mounted at this node.
   */
  @Override
  public int numChildren() {
    return node_.numChildren();
  }

  /**
   * Removes the handler with the specified name from the list of handlers if such a handler was
   * previously added.
   *
   * @param name Name of handler to remove.
   * @return True if the handler was removed.
   */
  @Override
  public boolean removeHandler(String name) {
    Handler handler = handlers_.get(name);
    if (handler == null) {
      return false;
    }
    catch_all_handlers_.remove(handler);
    if (handlers_.remove(name) == null) {
      return false;
    }
    if (handlers_.isEmpty()) {
      node_.removeHandler(virtual_handler_.getName());
    }
    return true;
  }

  /**
   * Unmounts the previously mounted child node. The node is unmounted from the underlying node.
   * If the node is not mounted, results in a no-op.
   *
   * @param child Node to be mounted under this node.
   * @return True if the node was unmounted.
   */
  @Override
  public boolean unmount(Node child) {
    return node_.unmount(child);
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
    return node_.handle(request, path_walker);
  }

  // True if this node serves as an active overlay on top of the underlying node, i.e. requests
  // to the underlying node are relayed to this node.
  private boolean active_;

  // Catch-all handlers associated with this virtual node.
  private ArrayList<Handler> catch_all_handlers_;

  // Virtual nodes representing the children of the underlying node indexed by their name.
  // The child nodes are not true children but used to extend the overlay. A virtual node
  // has only virtual children that represent the actual children of the underlying node. A
  // virtual node cannot have independent children.
  private HashMap<String, VirtualNode> children_;

  // Handler added to the underlying node.
  private VirtualNodeHandler virtual_handler_;

  // List of handlers associated with this virtual node.
  private HashMap<String, Handler> handlers_;

  // Underlying node.
  private Node node_;
}
