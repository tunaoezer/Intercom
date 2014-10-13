/* General AI - Networking
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

import ai.general.directory.Directory;
import ai.general.directory.test.TestBean;
import ai.general.net.wamp.WampConnection;
import ai.general.net.wamp.WampConnectionTest;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for {@link RemoteMethod} and {@link RemoteMethodCall} classes.
 */
public class RemoteMethodTest {

  private static final String kHostname = "general.ai";

  /**
   * CallThread executes calls on a separate thread allowing testing of concurrent behavior.
   */
  private static class CallThread extends Thread {

    /**
     * Constructs a CallThread.
     *
     * @param method The method to call.
     * @param synchronized True if the call blocks, false for asynchronous calls.
     * @param method_arguments Arguments passed to the called method.
     */
    public CallThread(RemoteMethod<TestBean> method,
                      boolean synchronize,
                      Object ... method_arguments) {
      this.method_ = method;
      this.synchronize_ = synchronize;
      this.method_arguments_ = method_arguments;
      method_call_ = null;
      result_ = null;
      thread_completion_time_millis_ = 0;
    }

    /**
     * The method call is only available for asynchronous calls or synchronous call that produce
     * an error.
     *
     * @return The remote method call object or null if none is available.
     */
    public RemoteMethodCall<TestBean> getMethodCall() {
      return method_call_;
    }

    /**
     * The result is only available for synchronous calls that do not return an error.
     *
     * @return The remote method call result or null if none is available.
     */
    public TestBean getResult() {
      return result_;
    }

    /**
     * Returns the time when this thread has completed.
     *
     * @return The time when this thread has completed.
     */
    public long getThreadCompletionTimeMillis() {
      return thread_completion_time_millis_;
    }

    /**
     * Initiates the method call using the either a synchronous or asynchronous call.
     * Records the time when this thread has completed.
     */
    @Override
    public void run() {
      if (synchronize_) {
        try {
          result_ = method_.call(method_arguments_);
        } catch (RemoteMethodCallException e) {
          @SuppressWarnings("unchecked") RemoteMethodCall<TestBean> method_call =
            (RemoteMethodCall<TestBean>) e.getMethodCall();
          method_call_ = method_call;
        }
        // Give processor chance to complete.
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {}
      } else {
        method_call_ = method_.callAsync(method_arguments_);
      }
      thread_completion_time_millis_ = System.currentTimeMillis();
      synchronized (this) {
        notifyAll();
      }
    }

    private RemoteMethod<TestBean> method_;
    private Object[] method_arguments_;
    private RemoteMethodCall<TestBean> method_call_;
    private TestBean result_;  ///< for synchronous calls
    private boolean synchronize_;
    private long thread_completion_time_millis_;
  }

  /**
   * CallProcessor processes calls on a separate thread allowing testing of concurrent behavior.
   * CallProcessor waits until the client has output. Once the client has produced output,
   * the output is supplied to the server and the reply is then immediatly supplied to the client.
   */
  private static class CallProcessor extends Thread {

    /**
     * Constructs a CallProcessor.
     *
     * @param client Client side connection.
     * @param client_sender Sender from client to server.
     * @param server Server side connection.
     * @param server_sender Sender from server to client.
     */
    public CallProcessor(WampConnection client,
                         WampConnectionTest.TestSender client_sender,
                         WampConnection server,
                         WampConnectionTest.TestSender server_sender) {
      this.client_ = client;
      this.client_sender_ = client_sender;
      this.server_ = server;
      this.server_sender_ = server_sender;
      thread_completion_time_millis_ = 0;
    }

    /**
     * Returns the time when this thread has completed.
     *
     * @return The time when this thread has completed.
     */
    public long getThreadCompletionTimeMillis() {
      return thread_completion_time_millis_;
    }

    /**
     * Waits for output on client_sender and then processes the output.
     * Records the thread completion time when processing is complete.
     */
    @Override
    public void run() {
      try {
        while (client_sender_.getOutput() == null) {
          Thread.sleep(1);
        }
      } catch (InterruptedException e) {}
      Assert.assertTrue(server_.process(client_sender_.getOutput()));
      Assert.assertTrue(client_.process(server_sender_.getOutput()));
      thread_completion_time_millis_ = System.currentTimeMillis();
      synchronized (this) {
        notifyAll();
      }
    }

    private WampConnection client_;
    private WampConnectionTest.TestSender client_sender_;
    private WampConnection server_;
    private WampConnectionTest.TestSender server_sender_;
    private long thread_completion_time_millis_;
  }

  /**
   * Implements a remote method for testing purposes.
   * The method sleeps for the specified time and then creates and returns a TestBean with the
   * specified parameters.
   *
   * @param sleep_time_millis Time to sleep in milliseconds.
   * @param number The TestBean number value.
   * @param real The TestBean real value.
   * @param text The TestBean text value.
   * @return A TestBean with the specified parameters.
   */
  public static TestBean method(int sleep_time_millis, int number, double real, String text) {
    try {
      Thread.sleep(sleep_time_millis);
    } catch (InterruptedException e) {}
    return new TestBean(number, real, text);
  }

  /**
   * Implements a remote method that throws an exception.
   *
   * @param description The exception description.
   * @param details The exception details.
   * @return Never returns. Return type defined for compatibility with method().
   * @throws RpcException always for testing purposes.
   */
  public static TestBean error(String description, TestBean details) throws RpcException {
    throw new RpcException(description, details);
  }

  /**
   * Tests asynchronous call behavior.
   */
  @Test
  public void callAsync() {
    final String kHomePath = "/remote_method/async";
    final String kMethod = "/method";
    final String kUserAccount = "async_user";
    Directory directory = Directory.Instance;
    Assert.assertTrue(directory.createPath(kHomePath + kMethod));
    try {
      Assert.assertTrue(directory.addHandler(
          kHomePath + kMethod,
          new MethodHandler(kMethod,
                            false,
                            null,
                            getClass().getDeclaredMethod("method",
                                                         int.class,
                                                         int.class,
                                                         double.class,
                                                         String.class))));
    } catch (NoSuchMethodException e) {
      Assert.fail(e.toString());
    }

    Uri uri = new Uri("ws", kHostname, "/remote_method_test");
    WampConnectionTest.TestSender client_sender = new WampConnectionTest.TestSender();
    WampConnectionTest.TestSender server_sender = new WampConnectionTest.TestSender();
    WampConnection client = new WampConnection(uri, kUserAccount, "", client_sender);
    WampConnection server = new WampConnection(uri, kUserAccount, kHomePath, server_sender);
    server.welcome("test-session-" + kUserAccount);
    client.process(server_sender.getOutput());

    RemoteMethod<TestBean> remote_method =
      new RemoteMethod<TestBean>(client, kMethod, TestBean.class);
    CallProcessor processor = new CallProcessor(client, client_sender, server, server_sender);
    CallThread call_thread = new CallThread(remote_method, false, 100, -123, 3.14159, "async");
    processor.start();
    call_thread.start();
    synchronized (call_thread) {
      while (call_thread.isAlive()) {
        try {
          call_thread.wait();
        } catch (InterruptedException e) {}
      }
    }
    synchronized (processor) {
      while (processor.isAlive()) {
        try {
          processor.wait();
        } catch (InterruptedException e) {}
      }
    }
    RemoteMethodCall<TestBean> method_call = call_thread.getMethodCall();
    Assert.assertNotNull(method_call);
    assertThat(method_call.getState(), is(RemoteMethodCall.State.Completed));
    Assert.assertTrue(method_call.isSuccessful());
    TestBean result = method_call.getResult();
    Assert.assertNotNull(result);
    assertThat(result.getNumber(), is(-123));
    assertThat(result.getReal(), is(3.14159));
    assertThat(result.getText(), is("async"));
    if (call_thread.getThreadCompletionTimeMillis() > processor.getThreadCompletionTimeMillis()) {
      Assert.fail("Call thread completion time: " + call_thread.getThreadCompletionTimeMillis() +
                  ", Processor thread completion time: " +
                  processor.getThreadCompletionTimeMillis());
    }

    client.close();
    server.close();
  }

  /**
   * Tests synchronous call behavior.
   */
  @Test
  public void callSync() {
    final String kHomePath = "/remote_method/sync";
    final String kMethod = "/method";
    final String kUserAccount = "sync_user";
    Directory directory = Directory.Instance;
    Assert.assertTrue(directory.createPath(kHomePath + kMethod));
    try {
      Assert.assertTrue(directory.addHandler(
          kHomePath + kMethod,
          new MethodHandler(kMethod,
                            false,
                            null,
                            getClass().getDeclaredMethod("method",
                                                         int.class,
                                                         int.class,
                                                         double.class,
                                                         String.class))));
    } catch (NoSuchMethodException e) {
      Assert.fail(e.toString());
    }

    Uri uri = new Uri("ws", kHostname, "/remote_method_test");
    WampConnectionTest.TestSender client_sender = new WampConnectionTest.TestSender();
    WampConnectionTest.TestSender server_sender = new WampConnectionTest.TestSender();
    WampConnection client = new WampConnection(uri, kUserAccount, "", client_sender);
    WampConnection server = new WampConnection(uri, kUserAccount, kHomePath, server_sender);
    server.welcome("test-session-" + kUserAccount);
    client.process(server_sender.getOutput());

    RemoteMethod<TestBean> remote_method =
      new RemoteMethod<TestBean>(client, kMethod, TestBean.class);
    CallProcessor processor = new CallProcessor(client, client_sender, server, server_sender);
    CallThread call_thread = new CallThread(remote_method, true, 100, 123, -3.14159, "sync");
    processor.start();
    call_thread.start();
    synchronized (call_thread) {
      while (call_thread.isAlive()) {
        try {
          call_thread.wait();
        } catch (InterruptedException e) {}
      }
    }
    synchronized (processor) {
      while (processor.isAlive()) {
        try {
          processor.wait();
        } catch (InterruptedException e) {}
      }
    }
    TestBean result = call_thread.getResult();
    Assert.assertNotNull(result);
    assertThat(result.getNumber(), is(123));
    assertThat(result.getReal(), is(-3.14159));
    assertThat(result.getText(), is("sync"));
    if (call_thread.getThreadCompletionTimeMillis() < processor.getThreadCompletionTimeMillis()) {
      Assert.fail("Call thread completion time: " + call_thread.getThreadCompletionTimeMillis() +
                  ", Processor thread completion time: " +
                  processor.getThreadCompletionTimeMillis());
    }

    client.close();
    server.close();
  }

  /**
   * Tests handling of call errors.
   */
  @Test
  public void callError() {
    final String kHomePath = "/remote_method/error";
    final String kMethod = "/error";
    final String kUserAccount = "error_user";
    Directory directory = Directory.Instance;
    Assert.assertTrue(directory.createPath(kHomePath + kMethod));
    try {
      Assert.assertTrue(directory.addHandler(
          kHomePath + kMethod,
          new MethodHandler(kMethod,
                            false,
                            null,
                            getClass().getDeclaredMethod("error",
                                                         String.class,
                                                         TestBean.class))));
    } catch (NoSuchMethodException e) {
      Assert.fail(e.toString());
    }

    Uri uri = new Uri("ws", kHostname, "/remote_method_test");
    WampConnectionTest.TestSender client_sender = new WampConnectionTest.TestSender();
    WampConnectionTest.TestSender server_sender = new WampConnectionTest.TestSender();
    WampConnection client = new WampConnection(uri, kUserAccount, "", client_sender);
    WampConnection server = new WampConnection(uri, kUserAccount, kHomePath, server_sender);
    server.welcome("test-session-" + kUserAccount);
    client.process(server_sender.getOutput());

    RemoteMethod<TestBean> remote_method =
      new RemoteMethod<TestBean>(client, kMethod, TestBean.class);
    CallProcessor processor = new CallProcessor(client, client_sender, server, server_sender);
    TestBean bean = new TestBean(1010, 0.1, "details");
    CallThread call_thread = new CallThread(remote_method, true, "remote method test", bean);
    processor.start();
    call_thread.start();
    synchronized (call_thread) {
      while (call_thread.isAlive()) {
        try {
          call_thread.wait();
        } catch (InterruptedException e) {}
      }
    }
    synchronized (processor) {
      while (processor.isAlive()) {
        try {
          processor.wait();
        } catch (InterruptedException e) {}
      }
    }
    RemoteMethodCall<TestBean> method_call = call_thread.getMethodCall();
    Assert.assertNotNull(method_call);
    assertThat(method_call.getState(), is(RemoteMethodCall.State.Completed));
    Assert.assertFalse(method_call.isSuccessful());
    assertThat(method_call.getErrorUri().toString(),
               is("wamp://error_user@general.ai" + kMethod + "#logic_error"));
    assertThat(method_call.getErrorDescription(), is("remote method test"));
    ObjectMapper mapper = new ObjectMapper();
    assertThat(mapper.convertValue(method_call.getErrorDetails(), TestBean.class), is(bean));
    if (call_thread.getThreadCompletionTimeMillis() < processor.getThreadCompletionTimeMillis()) {
      Assert.fail("Call thread completion time: " + call_thread.getThreadCompletionTimeMillis() +
                  ", Processor thread completion time: " +
                  processor.getThreadCompletionTimeMillis());
    }

    client.close();
    server.close();
  }
}
