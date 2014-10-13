/* General AI - Networking
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

/**
 * Exception that can be thrown by RPC methods.
 *
 * While RPC methods can throw any exception, RpcException allows specification of the exception
 * description and details which are returned to the caller.
 */
public class RpcException extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs an RpcException. If details are specified, they must be serializable to
   * JSON.
   *
   * @param description Required error description.
   * @param details Optional error details. May be null if no details are provided.
   */
  public RpcException(String description, Object details) {
    super(description);
    this.details_ = details;
  }

  /**
   * Any error details if details have been provided. Details are serialized to JSON.
   *
   * @return Error details if provided or null.
   */
  public Object getDetails() {
    return details_;
  }

  private Object details_;  // Details that are serialzed to JSON.
}
