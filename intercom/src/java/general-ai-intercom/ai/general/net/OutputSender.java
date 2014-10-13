/* General AI - Networking
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

import java.nio.ByteBuffer;

/**
 * Sender interface for sending output to a remote endpoint.
 * The particular implementation depends on the specific choice of communication protocol.
 */
public interface OutputSender {

  /**
   * Sends a binary message to the remote endpoint.
   *
   * @param data Binary data to send.
   * @return True if the data was successfully sent.
   */
  boolean sendBinary(ByteBuffer data);

  /**
   * Sends a text message to the remote endpoint.
   *
   * @param text Text message to send.
   * @return True if the message was successfully sent.
   */
  boolean sendText(String text);
}
