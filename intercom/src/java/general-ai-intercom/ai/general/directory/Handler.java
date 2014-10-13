/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

/**
 * Abstract base class for handlers.
 * A Handler handles a request for a resource specified by a URI.
 */
public abstract class Handler {

  /**
   * Constructs a regular handler with the specified name.
   *
   * @param name A unique handler name.
   */
  protected Handler(String name) {
    this(name, false);
  }

  /**
   * Constructs a regular or catch-all handler with the specified name.
   *
   * While regular handlers are run only on the target node of a resource request, catch-all
   * handlers are run on all requests that pass through a node, i.e. either target the node
   * itself or any decendant node reachable by the node.
   *
   * @param name A unique handler name.
   * @param catch_all True if this handler is a catch-all handler.
   */
  protected Handler(String name, boolean catch_all) {
    this.name_ = name;
    this.is_catch_all_ = catch_all;
  }

  /**
   * Returns the handler name.
   *
   * @return A unique name for the handler.
   */
  public String getName() {
    return name_;
  }

  /**
   * A subclass must override this method to specify the request handling logic.
   *
   * @param request The request to handle.
   */
  public abstract void handle(Request request);

  /**
   * Called to handle a catch-all request. This method is only called if this is a catch-all
   * handler and the calling node is not the target node of the request.
   * By default, this method calls the {@link #handle(Request)} method.
   *
   * A catch-all handler subclass may override this method if it needs to process catch-all
   * requests differently from regular requests.
   *
   * Unlike the handle method, this method provides the path remainder of the request. The path
   * remainder is the path from this node to the target node of the request. A catch-all handler
   * subclass may need to override this method if it needs access to the path remainder in order
   * to process the request.
   *
   * @param path_remainder The relative path from the calling node to the target of the request.
   * @param request The request to handle.
   */
  public void handleCatchAll(String path_remainder, Request request) {
    handle(request);
  }

  /**
   * Returns whether this handler is a catch-all handler.
   *
   * @return Whether this handler is a catch-all handler.
   */
  public boolean isCatchAll() {
    return is_catch_all_;
  }

  private String name_;  // name of handler
  private boolean is_catch_all_;  // true if this handler is a catch-all handler
}
