/* General AI - Plugin Annotations
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.plugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows a method to subscribe to events.
 *
 * Methods with this annotation will be subscribed to receive the events specified by the URI
 * path. The URI should be a relative path with respect to the home path specified for the
 * instance with which the method is associated. The instance home path is specified when the
 * the instance is added to the {@link ai.general.plugin.ServiceManager}. This allows
 * subscribing the method to events specific to the current instance.
 *
 * For example, to subscribe a method to the event /home_path/events/my_event, use:
 * <pre>
 * {@code @Subscribe("events/my_event")}
 * </pre>
 *
 * The /home_path/ portion of the path is specified for the whole instance when it is added to
 * ServiceManager.
 *
 * Methods with this annotation must be public and have the signature for an event method. Event
 * methods must have exactly one parameter that accepts the published event data. The type of the
 * parameter must be compatible with the type of the event data and should be a primitive type,
 * bean type or an array or collection of such a type. The method should not return any value or
 * throw exceptions.
 *
 * The class in which the method is defined must be public. If the method is defined in a nested
 * class both the nested class and its outer class must be public.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribe {
  /**
   * The URI path of the event.
   * The path is relative to the URI home path of the instance associated with the method.
   */
  String value();
}
