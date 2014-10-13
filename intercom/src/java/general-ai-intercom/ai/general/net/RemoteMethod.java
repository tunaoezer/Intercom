/* General AI - Networking
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

/**
 * Represents a remote method that can be called via a {@link Connection}.
 *
 * RemoteMethod offers methods for making an RPC call synchronously or asynchronously.
 *
 * The generic argument TReturnType specifies the return type of the RemoteMethod.
 * If the return type is void, Void can be specified as TReturnType.
 */
public class RemoteMethod<TReturnType> {

  /**
   * Creates a RemoteMethod instance. A RemoteMethod instance can be used to make multiple calls
   * to the same remote method.
   *
   * @param connection The Connection with which the call will be made.
   * @param method_path The URI of the remote method.
   * @param return_type The method return type, must match TReturnType.
   */
  public RemoteMethod(Connection connection, String method_path, Class<TReturnType> return_type) {
    this.connection_ = connection;
    this.method_path_ = method_path;
    this.return_type_ = return_type;
  }

  /**
   * Makes a synchronous method call and waits until completion of the call or a timeout occurs.
   *
   * If a timeout or error occurs, this method return a {@link RemoteMethodCall} object via a
   * {@link RemoteMethodCallException} that can be used to determine the cause of error or
   * continue waiting if the exception was thrown due to a timeout.
   *
   * This method waits for the default timeout specified by the {@link RemoteMethodCall} class.
   *
   * @param arguments The method arguments.
   * @return The return value of the method.
   * @throws RemoteMethodCallException if a timeout or other error has occurred.
   */
  public TReturnType call(Object ... arguments) throws RemoteMethodCallException {
    RemoteMethodCall<TReturnType> method_call =
      new RemoteMethodCall<TReturnType>(connection_, method_path_, return_type_);
    if (method_call.call(arguments)) {
      if (method_call.isSuccessful()) {
        return method_call.getResult();
      } else {
        throw new RemoteMethodCallException(method_call,
                                            RemoteMethodCallException.Reason.RemoteError);
      }
    } else {
      switch (method_call.getState()) {
        case InProgress:
          throw new RemoteMethodCallException(method_call,
                                              RemoteMethodCallException.Reason.Timeout);
      default:
          throw new RemoteMethodCallException(method_call,
                                              RemoteMethodCallException.Reason.CallError);
      }
    }
  }

  /**
   * Makes an asynchronous remote method call and immediately returns.
   *
   * The returned {@link RemoteMethodCall} object can be used to track the progress of the call
   * and obtain the final result.
   *
   * @param arguments The method arguments.
   * @return The {@link RemoteMethodCall} object which represents the call.
   */
  public RemoteMethodCall<TReturnType> callAsync(Object ... arguments) {
    RemoteMethodCall<TReturnType> method_call =
      new RemoteMethodCall<TReturnType>(connection_, method_path_, return_type_);
    method_call.callAsync(arguments);
    return method_call;
  }

  private Connection connection_;  // Connection with which the RPC is made.
  private String method_path_;  // The directory path of the method.
  private Class<TReturnType> return_type_;  // The return type of the remote method.
}
