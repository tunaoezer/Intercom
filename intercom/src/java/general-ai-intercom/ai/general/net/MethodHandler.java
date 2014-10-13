/* General AI - Networking
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

import ai.general.directory.Handler;
import ai.general.directory.Request;
import ai.general.directory.Result;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Invokes a method to handle requests.
 *
 * The method is specified via a Method object. MethodHandler checks whether the request
 * data matches the method signature and converts the request data to method arguments.
 *
 * MethodHandler can handle both event methods and RPC methods. Event methods have no return
 * value and process incoming publish or event messages. RPC methods process RPC calls and
 * may return values or errors.
 *
 * An RPC method may throw an exception which is returned to the remote caller. While not
 * necessary, it is recommended that a subclass of {@link RpcException} is thrown since it allows
 * specifying all error information that can be returned to the caller.
 */
public class MethodHandler extends Handler {

  /**
   * The specified instance must be an instance of the class that defines the method or a
   * subclass of that class. It may be null for static methods.
   *
   * The method must be publicly accessible and it must be defined public in a class that is
   * public. If the method is defined in a nested class, both the nested class and the outer class
   * must be public.
   *
   * Event methods must have exactly 1 parameter that accepts the publish or event data.
   * Any return values or exceptions thrown by an event method will be ignored.
   *
   * RPC call methods may have any number of parameters, return any value and throw exceptions.
   *
   * Method parameters must hava a POJO type. This includes primitive Java types or their arrays,
   * and bean types, i.e. classes that declare getters and setters for each field.
   *
   * @param name A unique name for the MethodHandler.
   * @param catchall True if this a catch-all handler.
   * @param instance Instance associated with method. May be null for static methods.
   * @param method The method to be called.
   * @throws IllegalArgumentException if the arguments are invalid or incompatible with each other.
   */
  public MethodHandler(String name, boolean catchall, Object instance, Method method) {
    super(name, catchall);
    if (!Modifier.isStatic(method.getModifiers())) {
      if (instance == null) {
        throw new IllegalArgumentException("Non-static method requires instance.");
      }
      if (!method.getDeclaringClass().isInstance(instance)) {
        throw new IllegalArgumentException("Incompatible instance object.");
      }
    }
    this.instance_ = instance;
    this.method_ = method;
    parameter_types_ = method.getParameterTypes();
    json_parser_ = new ObjectMapper();
  }

  /**
   * Handles the request by calling the method represented by this handler.
   * Request parameters are converted to match the method signature if possible. If no conversion
   * is possible, the method is not called.
   *
   * @param request The request to handle.
   */
  public void handle(Request request) {
    log.entry(request.getUri().toString());
    try {
      Object[] raw_args = request.getArguments().toArray();
      if (raw_args.length != parameter_types_.length) {
        request.getResult().addError(
            new Result.Error("invalid number of method arguments",
                             "got " + raw_args.length + " arguments for method with " +
                             parameter_types_.length + " arguments"));
        log.exit("invalid number of arguments");
        return;
      }
      Object[] args = new Object[raw_args.length];
      for (int i = 0; i < raw_args.length; i++) {
        args[i] = json_parser_.convertValue(raw_args[i], parameter_types_[i]);
      }
      Object result = method_.invoke(instance_, args);
      if (result != null) {
        request.getResult().addValue(result);
      }
    } catch (InvocationTargetException e) {
      log.catching(Level.TRACE, e);
      Throwable cause = e.getCause();
      if (cause != null) {
        if (cause instanceof RpcException) {
          request.getResult().addError(
              new Result.Error(cause.getMessage(), ((RpcException) cause).getDetails()));
        } else {
          request.getResult().addError(
              new Result.Error(cause.getClass().getName(), cause.getMessage()));
        }
      } else {
        request.getResult().addError(
            new Result.Error("unspecified exception thrown by RPC method", null));
      }
    } catch (ReflectiveOperationException e) {
      log.catching(Level.TRACE, e);
      request.getResult().addError(
          new Result.Error("cannot call method with specified arguments", null));
    } catch (Exception e) {
      log.catching(Level.TRACE, e);
      request.getResult().addError(new Result.Error(e.getClass().getName(), e.getMessage()));
    }
    log.exit();
  }

  private static Logger log = LogManager.getLogger();

  private Object instance_;  // The object instance that is associated with the method call.
  private ObjectMapper json_parser_;  // JSON parser.
  private Method method_;  // The method to be called.
  private Class<?>[] parameter_types_;  // The parameter types of the method.
}
