/* General AI - Networking
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

/**
 * Represents exceptions that can occur during a remote method call.
 */
public class RemoteMethodCallException extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * List of RemoteMethodCallException reasons.
   */
  public enum Reason {
    /** An error occurred while trying to make the remote method call. */
    CallError,

    /** The remote method returned an error. */
    RemoteError,

    /** The method did not return within the specified timeout. */
    Timeout,
  }

  /**
   * Constructs a RemoteMethodCallException for the specified reason and RPC call.
   *
   * @param method_call The RemoteCallCall that caused the exception.
   * @param reason The reason of the exception.
   */
  public RemoteMethodCallException(RemoteMethodCall<?> method_call, Reason reason) {
    super(reason.name());
    this.method_call_ = method_call;
    this.reason_ = reason;
  }

  /**
   * The {@link RemoteMethodCall} which caused the exception.
   *
   * The RemoteMethodCall object provides an error URI ({@link RemoteMethodCall#getErrorUri()})
   * and description ({@link RemoteMethodCall#getErrorDescription()}) and may also provide
   * further details about the error ({@link RemoteMethodCall#getErrorDetails()}).
   *
   * @return The remote method call associated with the exception.
   */
  public RemoteMethodCall<?> getMethodCall() {
    return method_call_;
  }

  /**
   * Exception reason.
   *
   * @return The reason of the exception.
   */
  public Reason getReason() {
    return reason_;
  }

  private RemoteMethodCall<?> method_call_;  // The RPC call that caused the exception.
  private Reason reason_;  // The exception reason.
}
