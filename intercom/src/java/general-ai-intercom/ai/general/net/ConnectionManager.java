/* General AI - Networking
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

import ai.general.plugin.PluginManager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * The ConnectionManager manages all {@link Connection} instances.
 *
 * Connection instances add themselves to the ConnectionManager when they are created and they
 * remove themselves when they are closed.
 *
 * The ConnectionManager interacts with the {@link PluginManager} to notify services of new or
 * closed connections.
 *
 * ConnectionManager is a singleton class.
 */
public class ConnectionManager {

  /**
   * Singleton instance.
   */
  public static final ConnectionManager Instance = new ConnectionManager();

  /**
   * ConnectionManager is a singleton.
   * The singleton instance can be obtained via {@link #Instance}.
   */
  private ConnectionManager() {
    connections_ = new HashSet<Connection>();
  }

  /**
   * Adds a ready connection to the ConnectionManager.
   *
   * This method must be called by a subclass of {@link Connection} when the connection is ready.
   * It should not be called by other classes.
   * The added connection must be ready. This method throws an IllegalArgumentException if the
   * connection is not ready.
   *
   * A connection instance can be added only once. Duplicate additions are ignored. Each connection
   * added by this method must be removed with a call to {@link #remove(Connection)} when the
   * connection is closed.
   *
   * The ConnectionManager automatically notifes the {@link PluginManager} of the new connection,
   * which connects all enabled services to the new connection.
   *
   * @param connection The connection to add to the connection manager.
   * @throws IllegalArgumentException if the connection is not connected.
   */
  public synchronized void add(Connection connection) {
    if (!connection.isReady()) {
      throw new IllegalArgumentException("Connection must be ready.");
    }
    if (!connections_.contains(connection)) {
      connections_.add(connection);
      PluginManager.Instance.connect(connection);
    }
  }

  /**
   * Returns a read-only collection of all connections.
   *
   * @return A collection of all connections.
   */
  public Collection<Connection> getConnections() {
    return Collections.unmodifiableSet(connections_);
  }

  /**
   * Returns the number of connections.
   *
   * @return The number of connections.
   */
  public int numConnections() {
    return connections_.size();
  }

  /**
   * Removes a connected connection from the ConnectionManager.
   *
   * This method must be called by a subclass of {@link Connection} when the connection is closed.
   * It should not be called by other classes.
   *
   * The ConnectionManager automatically notifes the {@link PluginManager} that the connection has
   * been closed, which disconnect all enabled services from the connection.
   *
   * @param connection The connection to remove from the connection manager.
   */
  public synchronized void remove(Connection connection) {
    if (connections_.remove(connection)) {
      PluginManager.Instance.disconnect(connection);
    }
  }

  private HashSet<Connection> connections_;  // All connections.
}
