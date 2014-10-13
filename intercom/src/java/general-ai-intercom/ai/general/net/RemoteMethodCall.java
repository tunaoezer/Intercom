/* General AI - Networking
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a remote method call.
 *
 * RemoteMethodCall implements the {@link RpcCallback} interface and keeps track of the state of
 * the method call. When a call completes, it stores the result of the call.
 *
 * RemoteMethodCall provides methods for synchronization and allows waiting for the call to
 * complete.
 *
 * Each RemoteMethodCall instance is associated with exactly one call and can only be used for that
 * one call.
 *
 * If the remote method returns void, TReturnType may be Void. The call result will be null.
 */
public class RemoteMethodCall<TReturnType> implements RpcCallback {

  /** Default synchronous method call timeout in milliseconds. */
  public static final long kDefaultCallTimeoutMillis = 120000;

  /**
   * Indicates the stage of the RemoteMethodCall.
   */
  public enum State {
    /** response has been received */
    Completed,

    /** call has not been made */
    Initialized,

    /** call has been made, awaiting response */
    InProgress,
  }

  /**
   * Creates a RemoteMethodCall instance for a single RPC call.
   *
   * @param connection The Connection with which the call will be made.
   * @param method_path The URI of the remote method.
   * @param return_type The method return type, must match TReturnType.
   */
  public RemoteMethodCall(Connection connection,
                          String method_path,
                          Class<TReturnType> return_type) {
    this.connection_ = connection;
    this.method_path_ = method_path;
    this.return_type_ = return_type;
    state_ = State.Initialized;
    call_timeout_millis_ = kDefaultCallTimeoutMillis;
    json_parser_ = new ObjectMapper();
    successful_ = false;
    result_ = null;
    error_uri_ = null;
    error_description_ = null;
    error_details_ = null;
  }

  /**
   * The synchronous call timeout in milliseconds.
   *
   * @return The call timeout in milliseconds used by synchronous calls.
   */
  public long getCallTimeoutMillis() {
    return call_timeout_millis_;
  }

  /**
   * If the call completed with an error, provides the error description.
   *
   * @return The error description if the method call returned with an error.
   */
  public String getErrorDescription() {
    return error_description_;
  }

  /**
   * If the call completed with an error and details were provided, provides the error details.
   *
   * @return Any provided error details if the method call returned with an error or null.
   */
  public Object getErrorDetails() {
    return error_details_;
  }

  /**
   * If the call completed with an error, provides the error URI.
   *
   * @return The error URI if the method call returned with an error.
   */
  public Uri getErrorUri() {
    return error_uri_;
  }

  /**
   * If the remote method did not return a result, returns null.
   *
   * @return The result of the method call once the call has returned or null.
   */
  public TReturnType getResult() {
    return result_;
  }

  /**
   * The current state in the life cycle of the call.
   *
   * @return The current state of the remote method call.
   */
  public State getState() {
    return state_;
  }

  /**
   * Makes a synchronous call to the remote method. This method blocks until the remote method
   * returns or a timeout as specified by {@link #setCallTimeoutMillis(long)} occurs.
   *
   * If this method returns early due to a timeout, the caller may continue waiting by calling
   * {@link #waitUntilCompletion(long)}.
   *
   * @param arguments The method arguments.
   * @return True if the call was made and completed within the timeout.
   */
  public boolean call(Object ... arguments) {
    if (!callAsync(arguments)) {
      return false;
    }
    return waitUntilCompletion(call_timeout_millis_);
  }

  /**
   * Makes an asynchronous remote method call and immediately returns.
   * Once the call is completed, the result can be obtained from this object.
   *
   * A call can be only made in the Initialized state. Each RemoteMethodCall instance can be
   * used only for one RPC call.
   *
   * @param arguments The method arguments.
   * @return True if the call was made.
   */
  public boolean callAsync(Object ... arguments) {
    if (state_ != State.Initialized) {
      return false;
    }
    state_ = State.InProgress;
    return connection_.call(method_path_, this, arguments);
  }

  /**
   * Checks whether the call completed with no errors.
   *
   * @return True if the method call completed and did not return an error.
   */
  public boolean isSuccessful() {
    return successful_;
  }

  /**
   * RpcCallback implementation. Called on an RPC error.
   *
   * @param error_uri The returned error URI. Maybe null if the URI is invalid.
   * @param error_description Description of the error.
   * @param error_details Optional error details. Null if no details were provided.
   */
  @Override
  public synchronized void onError(Uri error_uri, String error_description, Object error_details) {
    state_ = State.Completed;
    successful_ = false;
    this.error_uri_ = error_uri;
    this.error_description_ = error_description;
    this.error_details_ = error_details;
    notifyAll();
  }

  /**
   * RpcCallback implementation. Called by the network thread on successful execution of the RPC
   * method.
   *
   * @param result The result of the RPC method or null if the method did not return a value.
   */
  @Override
  public synchronized void onSuccess(Object result) {
    state_ = State.Completed;
    successful_ = true;
    if (result != null) {
      result_ = json_parser_.convertValue(result, return_type_);
    }
    notifyAll();
  }

  /**
   * Changes the synchronous call timeout.
   *
   * @param timeout_millis The new call timeout in milliseconds to be used by synchronous calls.
   */
  public void setCallTimeoutMillis(long timeout_millis) {
    call_timeout_millis_ = timeout_millis;
  }

  /**
   * Waits for the remote method call to complete.
   *
   * If the call does not complete within the specified timeout, aborts waiting and returns false.
   * A timeout does not cancel the method call and a client can call this method again to continue
   * waiting.
   *
   * Immediately returns if the call has already completed or has not been initiated.
   *
   * waitUntilCompletion must not be called by the network thread.
   * waitUntilCompletion can be called by multiple threads at the same time. In such a case, all
   * threads will wait until the call has completed.
   *
   * The timeout value is approximate and the exact wait time depends on the Java VM. This method
   * may return before the timeout expires if the Java VM wakes up this thread early.
   *
   * @param timeout_millis Wait timeout in milliseconds.
   * @return True if the call has completed.
   */
  public boolean waitUntilCompletion(long timeout_millis) {
    if (state_ == State.Completed) {
      return true;
    }
    if (state_ != State.InProgress) {
      return false;
    }
    synchronized (this) {
      try {
        wait(timeout_millis);
      } catch (InterruptedException e) {}
      return state_ == State.Completed;
    }
  }

  private long call_timeout_millis_;  // The RPC timeout in milliseconds.
  private Connection connection_;  // Connection with which the RPC is made.
  private String error_description_;  // Error description.
  private Object error_details_;  // Optional error details.
  private Uri error_uri_;  // Returned error URI if the RPC was not successful.
  private ObjectMapper json_parser_;  // JSON parser.
  private String method_path_;  // The directory path of the method.
  private TReturnType result_;  // The return value of the RPC method.
  private Class<TReturnType> return_type_;  // The return type of the RPC method.
  private State state_;  // The current state of the RPC.
  private boolean successful_;  // True if the RPC was successful.
}
