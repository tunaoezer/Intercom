/* General AI - Networking
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

/**
 * Defines the interface for an RPC callback.
 * RPC callbacks are used to communicate the aysnchronous result of an RPC to the caller.
 *
 * For each RPC, exactly one of onSuccess() or onError() will be called when the RPC completes.
 */
public interface RpcCallback {

  /**
   * Called on an RPC error.
   *
   * @param error_uri The returned error URI. Maybe null if the URI is invalid.
   * @param error_description Description of the error.
   * @param error_details Optional error details. Null if no details were provided.
   */
  void onError(Uri error_uri, String error_description, Object error_details);

  /**
   * Called on successful execution of the RPC method.
   *
   * @param result The result of the RPC method or null if the method did not return a value.
   */
  void onSuccess(Object result);
}
