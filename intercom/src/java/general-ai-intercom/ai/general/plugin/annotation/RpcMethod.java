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
 * Allows a method to define itself as a remote method implementation.
 *
 * Remote methods receive and handle RPC calls. Methods with this annotation will be registered
 * to receive any RPC's directed to the specified URI path. The URI should be a relative path
 * with respect to the home path specified for the instance with which the method is associated.
 * The instance home path is specified when the the instance is added to the
 * {@link ai.general.plugin.ServiceManager}. This allows fine grained access control.
 *
 * For example, to declare an RPC method for the URI /home_path/methods/my_func, use:
 * <pre>
 * {@code @RpcMethod("methods/my_func")}
 * </pre>
 *
 * The /home_path/ portion of the path is specified for the whole instance when it is added to
 * ServiceManager.
 *
 * Methods with this annotation must be designed as RPC call methods. RPC call methods must be
 * public and can have any number of parameters and may return values, but the parameters must
 * be a primitive type, a bean, or an array or collection of such type. RPC call methods may throw
 * exceptions. If exceptions are thrown, it is recommended that an instance of
 * {@link ai.general.net.RpcException} is thrown.
 *
 * The class in which the method is defined must be public. If the method is defined in a nested
 * class both the nested class and its outer class must be public.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcMethod {
  /**
   * The URI path of the remote method.
   * The path is relative to the URI home path of the instance associated with the annotated
   * method.
   */
  String value();
}
