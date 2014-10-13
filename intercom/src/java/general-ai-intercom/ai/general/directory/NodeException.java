/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

/**
 * Represents exceptions in node operations.
 */
public class NodeException extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * List of NodeException reasons.
   */
  public enum Reason {
    /** Operation would create a recursive cycle. */
    Cyclic,

    /** Name already exists. */
    DuplicateName,
  }

  /**
   * Constructs a NodeException for the specified reason.
   *
   * @param reason The reason of the exception.
   */
  public NodeException(Reason reason) {
    super(reason.name());
    this.reason_ = reason;
  }

  /**
   * Exception reason.
   *
   * @return The reason of the exception.
   */
  public Reason getReason() {
    return reason_;
  }

  private Reason reason_;  // Exception reason.
}
