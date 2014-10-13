/* General AI - Directory Test Support
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory.test;

import ai.general.directory.Handler;
import ai.general.directory.Request;

import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Generic test handler used by test classes.
 * Generic test handler expects arguments of type Object and allows conversion to another type.
 */
public class GenericTestHandler extends Handler {

  /**
   * Constructs a regular GenericTestHandler with the specified name.
   *
   * @param name Name of handler.
   */
  public GenericTestHandler(String name) {
    this(name, false);
  }

  /**
   * Constructs a regular or catch-all GenericTestHandler with the specified name.
   *
   * @param name Name of handler.
   * @param catch_all True if this a catch-all handler.
   */
  public GenericTestHandler(String name, boolean catch_all) {
    super(name, catch_all);
    arguments_ = null;
    path_remainder_ = null;
    object_mapper_ = new ObjectMapper();
  }

  /**
   * Sets the request arugments to null.
   */
  public void clearArguments() {
    arguments_ = null;
  }

  /**
   * Returns the request argument at specified index.
   * Returns null if no request arguments have been captured or there is no request argument
   * at the specified index.
   *
   * @param index The request argument index.
   * @return The request argument at the specified index or null.
   */
  public Object getArgument(int index) {
    if (arguments_ == null || arguments_.size() <= index) {
      return null;
    }
    return arguments_.toArray()[index];
  }

  /**
   * Returns all request arguments captured while handling the request or null if no arguments
   * have been captured. Arguments will only be captured when the request is handled by this
   * request handler.
   *
   * @return The request arguments or null.
   */
  public Collection<Object> getArguments() {
    return arguments_;
  }

  /**
   * Returns the request argument at the specified index after conversion into the type of the
   * type_template argument.
   *
   * The returned value has the same type as the type_template parameter.
   *
   * Returns null if no request arguments have been captured or there is no request argument
   * at the specified index.
   *
   * @param index The request argument index.
   * @param type_template The type into which to convert the returned argument.
   * @return The converted request argument at the specified index or null.
   */
  public Object getArgumentAs(int index, Object type_template) {
    Object argument = getArgument(index);
    if (argument == null) {
      return null;
    }
    return object_mapper_.convertValue(argument, type_template.getClass());
  }

  /**
   * If this handler was called for a catch-all request, returns the captured path remainder.
   * Returns null, if this request was not called as part of a catch-all request.
   * The path remainder is the path from the calling node to the target node of the request.
   *
   * @return The path remainder or null.
   */
  public String getPathRemainder() {
    return path_remainder_;
  }

  /**
   * Handles a request. Obtains the request arguments from request.
   *
   * @param request Request to handle.
   */
  @Override
  public void handle(Request request) {
    arguments_ = request.getArguments();
    path_remainder_ = null;
  }

  /**
   * Handles catch-all requests. Stores the path remainder and calls {@link #handle(Request)}.
   *
   * @param path_remainder The relative path from the calling node to the target of the request.
   * @param request The request to handle.
   */
  @Override
  public void handleCatchAll(String path_remainder, Request request) {
    handle(request);
    this.path_remainder_ = path_remainder;
  }

  private Collection<Object> arguments_;  // captured request arguments
  private ObjectMapper object_mapper_;  // used to convert request arguments into other types
  private String path_remainder_;  // if the handler was called to handle a catch-all request
}
