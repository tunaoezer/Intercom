/* General AI - WAMP Server and Client
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net.wamp;

import ai.general.directory.Directory;
import ai.general.directory.Handler;
import ai.general.directory.Request;
import ai.general.directory.Result;
import ai.general.directory.test.GenericTestHandler;
import ai.general.directory.test.TestBean;
import ai.general.directory.test.TestHandler;
import ai.general.net.OutputSender;
import ai.general.net.RpcCallback;
import ai.general.net.Uri;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for {@link WampConnection} class.
 */
public class WampConnectionTest {

  private static final String kHostname = "general.ai";

  /**
   * Output sender for testing purposes.
   */
  public static class TestSender implements OutputSender {

    /**
     * Constructs a TestSender with no receiver or output.
     */
    public TestSender() {
      receiver_ = null;
      output_ = null;
    }

    /**
     * Returns the last output or null.
     *
     * @return Last output from WampConnection or null.
     */
    public String getOutput() {
      return output_;
    }

    /**
     * Not implemented.
     *
     * @param data The data to send.
     * @return false
     */
    @Override
    public boolean sendBinary(ByteBuffer data) {
      return false;
    }

    /**
     * Records the output of the connection and sends it to the receiver.
     *
     * Some tests do not set a receiver but manually send the message. This may be necessary to
     * test behavior with multiple threads.
     * In order to ensure that all tests pass, this method returns true if no receiver is set.
     *
     * @param text The text to send.
     * @return True if the message was successfully processed by the receiver.
     */
    @Override
    public boolean sendText(String text) {
      output_ = text;
      if (receiver_ != null) {
        return receiver_.process(output_);
      }
      return true;
    }

    /**
     * Sets the receiver of the output sent by this TestSender.
     *
     * @param receiver WampConnection that will receive output send to this OutputSender.
     */
    public void setReceiver(WampConnection receiver) {
      this.receiver_ = receiver;
    }

    private String output_;  // last output
    private WampConnection receiver_;  // output destination
  }

  /**
   * Defines a bi-directional WAMP connection between a client and server to test round trips.
   */
  public static class TestConnection {

    /**
     * Constructs a TestConnection for the specified user account. This constructor creates
     * standard user home directory paths for this test and uses the name of this test as the
     * endpoint.
     *
     * @param user_account The user account associated with the client-server connection.
     */
    public TestConnection(String user_account) {
      this("/wamp_connection_test",
           user_account,
           WampConnectionTest.clientHomePath(user_account),
           WampConnectionTest.serverHomePath(user_account));
    }

    /**
     * Constructs a TestConnection for the specified user account and directory home paths for
     * the user account on the server and client side.
     *
     * This method sets different endpoint paths for the client and server connection.
     * Typically, the endpoint paths would be the same on the server and client side. However,
     * since the tests run the server and client in the same process using the same directory,
     * the connections cannot be easily distinguished if the same endpoint path is used for both
     * connections.
     * The suffix '/server' is added to the server endpoint. The suffix '/client' is added to the
     * client endpoint.
     *
     * This constructor can be used by other tests that make use of the TestConnection class.
     *
     * @param endpoint The absolute URI path of the endpoint associated with the connection.
     * @param user_account The user account associated with the client-server connection.
     * @param client_user_home_path The user home directory path on the client side.
     * @param server_user_home_path The user home directory path on the server side.
     */
    public TestConnection(String endpoint,
                          String user_account,
                          String client_user_home_path,
                          String server_user_home_path) {
      this.user_account_ = user_account;
      client_out_ = new TestSender();
      server_out_ = new TestSender();
      client_ = new WampConnection(new Uri("ws", kHostname, endpoint + "/client"),
                                   user_account,
                                   client_user_home_path,
                                   client_out_);
      server_ = new WampConnection(new Uri("ws", kHostname, endpoint + "/server"),
                                   user_account,
                                   server_user_home_path,
                                   server_out_);
      client_out_.setReceiver(server_);
      server_out_.setReceiver(client_);
    }

    /**
     * Returns the client side connection.
     *
     * @return The client side connection.
     */
    public WampConnection client() {
      return client_;
    }

    /**
     * Closes the test connection.
     */
    public void close() {
      client_.close();
      server_.close();
    }

    /**
     * Returns the last client output.
     *
     * @return The last client output.
     */
    public String getClientOutput() {
      return client_out_.getOutput();
    }

    /**
     * Returns the last server output.
     *
     * @return The last server output.
     */
    public String getServerOutput() {
      return server_out_.getOutput();
    }

    /**
     * Runs the WAMP handshake.
     */
    public void open() {
      server_.welcome("test-session-" + user_account_);
    }

    /**
     * Returns the server side connection.
     *
     * @return The server side connection.
     */
    public WampConnection server() {
      return server_;
    }

    private WampConnection client_;
    private TestSender client_out_;
    private WampConnection server_;
    private TestSender server_out_;
    private String user_account_;
  }

  /**
   * Encapsulates all data structures associated with a test user.
   * To use a TestUser, a test must call the {@link #begin()} method at the beginning and the
   * {@link #end()} method at the end.
   */
  private static class TestUser {

    /**
     * Creates a TestUser for the specified user account.
     *
     * @param account The user account name.
     */
    public TestUser(String account) {
      this.account_ = account;
      client_home_ = WampConnectionTest.clientHomePath(account);
      server_home_ = WampConnectionTest.serverHomePath(account);
      connection_ = new TestConnection(account_);
      handlers_ = new HashMap<String, GenericTestHandler>();
    }

    /**
     * Sets up a test user.
     * This method creates topic paths on the client side and grants access to topics to the
     * user on the server side.
     *
     * This method must be called at the beginning of a test.
     */
    public void begin() {
      String root_home = serverHomePath("root");
      WampConnectionTest.createTopicPaths(client_home_, handlers_);
      Assert.assertTrue(Directory.Instance.createPath(server_home_ + "/topics"));
      grantAccess(root_home, server_home_, "category1");
      grantAccess(root_home, server_home_, "category2");
      grantAccess(root_home, server_home_, "category3");
      connection_.open();
    }

    /**
     * Closes the test connection.
     * This method must be called at the end of a test.
     */
    public void end() {
      connection_.close();
    }

    /**
     * Returns the user account.
     *
     * @return The user account.
     */
    public String getAccount() {
      return account_;
    }

    /**
     * Returns the home directory path on the client side.
     *
     * @return The home directory path on the client side.
     */
    public String getClientHomePath() {
      return client_home_;
    }

    /**
     * Returns the test connection.
     *
     * @return The test connection.
     */
    public TestConnection getConnection() {
      return connection_;
    }

    /**
     * Returns the handler with the specified name.
     *
     * @param name The handler name.
     * @return The handler with the specified name.
     */
    public GenericTestHandler getHandler(String name) {
      return handlers_.get(name);
    }

    /**
     * Returns the home directory path on the server side.
     *
     * @return The home directory path on the server side.
     */
    public String getServerHomePath() {
      return server_home_;
    }

    private String account_;  // user account
    private String client_home_;  // home directory at client side
    private TestConnection connection_;  // test connection
    private HashMap<String, GenericTestHandler> handlers_;  // active test handlers, by name
    private String server_home_;  // home directory at server side
  }

  /**
   * Test handler that handles RPC requests.
   */
  private static class RpcHandler extends Handler {
    public static final String kMethod1 = "/rpc/method1";
    public static final String kMethod2 = "/rpc/method2";
    public static final String kCatchall = "/rpc/catchall";
    public static final String kCatchallMethod2 = "/rpc/catchall/method2";
    public static final String kCallError = "/rpc/call_error";
    public static final String kCallErrorWithDetails = "/rpc/call_error_with_details";
    public static final String kPrefixMethod = "/rpc/prefix";  // for prefix testing

    /**
     * Constructs an RPC handler for the method with the specified name.
     *
     * @param method_name The RPC method name.
     */
    public RpcHandler(String method_name) {
      super(method_name, method_name.endsWith(kCatchall));
    }

    /**
     * Handles requests.
     *
     * @param request The request to handle.
     */
    @Override
    public void handle(Request request) {
      if (request.getRequestType() != Request.RequestType.Call) {
        return;
      }
      if (request.getUri().getPath().equals(getName())) {
        switch (getName()) {
          case kMethod1: break;  // nothing to do for method1
          case kMethod2:
          case kCatchallMethod2:
          case kPrefixMethod:
            callMethod2(request);
            break;
          case kCallError: callError(request); break;
          case kCallErrorWithDetails: callErrorWithDetails(request); break;
          default: break;
        }
      }
    }

    /**
     * Handles catch-all requests.
     *
     * @param path_remainder The relative path from the calling node to the target of the request.
     * @param request The request to handle.
     */
    @Override
    public void handleCatchAll(String path_remainder, Request request) {
      if (request.getRequestType() != Request.RequestType.Call) {
        return;
      }
      if (request.getUri().getPath().equals(kCatchallMethod2)) {
        callMethod2All(request);
      }
    }

    /**
     * Adds an error with no details to the request.
     *
     * @param request The request to handle.
     */
    private void callError(Request request) {
      request.getResult().addError(new Result.Error("test error", null));
    }

    /**
     * Adds an error with details to the request.
     *
     * @param request The request to handle.
     */
    private void callErrorWithDetails(Request request) {
      request.getResult().addError(new Result.Error("test error with details", 1001));
    }

    /**
     * Helper method to call {@link #method2(int, int, int)} using request arguments.
     *
     * @param request The request to handle.
     */
    private void callMethod2(Request request) {
      Object[] args = request.getArguments().toArray();
      if (args.length != 3) {
        return;
      }
      request.getResult().addValue(method2((int) args[0], (int) args[1], (int) args[2]));
    }

    /**
     * Helper method to call {@link #method2All(int, int, int)} using request arguments.
     *
     * @param request The request to handle.
     */
    private void callMethod2All(Request request) {
      Object[] args = request.getArguments().toArray();
      if (args.length != 3) {
        return;
      }
      request.getResult().addValue(method2All((int) args[0], (int) args[1], (int) args[2]));
    }

    /**
     * Takes 3 integer arguments and returns their sum.
     *
     * @param x First integer.
     * @param y Second integer.
     * @param z Third integer.
     */
    private int method2(int x, int y, int z) {
      return x + y + z;
    }

    /**
     * Takes 3 integer arguments and returns their product.
     *
     * @param x First integer.
     * @param y Second integer.
     * @param z Third integer.
     */
    private int method2All(int x, int y, int z) {
      return x * y * z;
    }
  }

  /**
   * Test callback that stores the success or error results.
   */
  private static class TestCallback implements RpcCallback {

    /** Specifies whether the callback result is a successful callback or error callback. */
    public enum CallbackType {
      /// Callback has not completed yet.
      kNone,
      /// Callback result indicates success.
      kSuccess,
      /// Callback result indicates error.
      kError
    }

    /**
     * Creates a default TestCallback.
     */
    public TestCallback() {
      clear();
    }

    /**
     * Resets the values of the TestCallback.
     */
    public void clear() {
      callback_type_ = CallbackType.kNone;
      result_ = null;
      error_uri_ = null;
      error_description_ = null;
      error_details_ = null;
    }

    /**
     * Returns the callback type.
     *
     * @return The callback type.
     */
    public CallbackType getCallbackType() {
      return callback_type_;
    }

    /**
     * Returns the callback error description if the callback was not successful.
     *
     * @return The error description.
     */
    public String getErrorDescription() {
      return error_description_;
    }

    /**
     * Returns the callback error details if the callback was not successful.
     *
     * @return The error details.
     */
    public Object getErrorDetails() {
      return error_details_;
    }

    /**
     * Returns the callback error URI if the callback was not successful.
     *
     * @return The error URI.
     */
    public Uri getErrorUri() {
      return error_uri_;
    }

    /**
     * Returns the callback return value if the callback was successful or null.
     *
     * @return The callback return value or null.
     */
    public Object getResult() {
      return result_;
    }

    /**
     * Called on an RPC error.
     *
     * @param error_uri The returned error URI. Maybe null if the URI is invalid.
     * @param error_description Description of the error.
     * @param error_details Optional error details. Null if no details were provided.
     */
    @Override
    public void onError(Uri error_uri, String error_description, Object error_details) {
      callback_type_ = CallbackType.kError;
      this.error_uri_ = error_uri;
      this.error_description_ = error_description;
      this.error_details_ = error_details;
    }

    /**
     * Called on successful execution of the RPC method.
     *
     * @param result The result of the RPC method or null if the method did not return a value.
     */
    @Override
    public void onSuccess(Object result) {
      callback_type_ = CallbackType.kSuccess;
      this.result_ = result;
    }

    private CallbackType callback_type_;
    private String error_description_;
    private Object error_details_;
    private Uri error_uri_;
    private Object result_;
  }

  /**
   * Tets welcome messages.
   */
  @Test
  public void welcome() {
    final String kUserAccount = "client1@domain.zz";
    TestConnection test = new TestConnection(kUserAccount);
    Assert.assertFalse(test.server().isReady());
    Assert.assertFalse(test.client().isReady());

    Assert.assertTrue(test.server().welcome("test-session"));
    assertThat(test.getServerOutput(),
               startsWith("[0,\"test-session\",1,\"general.ai-Intercom/"));
    assertThat(test.client().getSessionId(), is("test-session"));
    Assert.assertTrue(test.server().isReady());
    Assert.assertTrue(test.client().isReady());

    test.close();
    Assert.assertFalse(test.server().isReady());
    Assert.assertFalse(test.client().isReady());
  }

  /**
   * Tests subscribe and unsubscribe.
   */
  @Test
  public void subscribe() {
    final String kUserAccount = "tester1@domain.zz";
    final String kHandlerNameSuffix = "->" + kUserAccount + "@test-session-" + kUserAccount;
    Directory directory = Directory.Instance;
    String topic = "/subscribe/topic1";
    String all_topics = "/subscribe/*";
    String server_home = serverHomePath(kUserAccount);
    Assert.assertTrue(directory.createPath(server_home + topic));
    TestConnection connection = new TestConnection(kUserAccount);
    connection.open();

    Assert.assertTrue(connection.client().subscribe(topic));
    assertThat(connection.getClientOutput(), is(jsonArray(5, uri(kUserAccount, topic))));
    Assert.assertTrue(directory.hasHandler(server_home + topic,
                                           server_home + topic + kHandlerNameSuffix));

    Assert.assertTrue(connection.client().subscribe(all_topics));
    assertThat(connection.getClientOutput(), is(jsonArray(5, uri(kUserAccount, all_topics))));
    Assert.assertTrue(directory.hasHandler(server_home + "/subscribe",
                                           server_home + "/subscribe/*" + kHandlerNameSuffix));

    Assert.assertTrue(connection.client().unsubscribe(topic));
    assertThat(connection.getClientOutput(), is(jsonArray(6, uri(kUserAccount, topic))));
    Assert.assertFalse(directory.hasHandler(server_home + topic,
                                            server_home + topic + kHandlerNameSuffix));

    // verify that client unsubscribes on close
    connection.client().close();
    Assert.assertFalse(directory.hasHandler(server_home,
                                            server_home + "/subscribe/*" + kHandlerNameSuffix));
    connection.close();

    // verify that server unsubscribes on close
    connection.open();
    Assert.assertTrue(connection.client().subscribe(topic));
    Assert.assertTrue(directory.hasHandler(server_home + topic,
                                           server_home + topic + kHandlerNameSuffix));
    connection.server().close();
    Assert.assertFalse(directory.hasHandler(server_home + topic,
                                            server_home + topic + kHandlerNameSuffix));
    connection.close();
  }

  /**
   * Tests event messages. In this test, the events are triggered directly from the server without
   * a publish message.
   */
  @Test
  public void event() {
    final String kUserAccount = "tester1@domain.zz";
    Directory directory = Directory.Instance;
    String client_home = clientHomePath(kUserAccount);
    String all_topics = "/event";
    String topic1 = "/event/topic1";
    String topic2 = "/event/topic2";
    Assert.assertTrue(directory.createPath(client_home + topic1));
    Assert.assertTrue(directory.createPath(client_home + topic2));
    TestHandler catchall_handler = new TestHandler("catchall", true);
    TestHandler topic1_handler = new TestHandler("topic1");
    GenericTestHandler topic2_handler = new GenericTestHandler("topic2");
    Assert.assertTrue(directory.addHandler(client_home + all_topics, catchall_handler));
    Assert.assertTrue(directory.addHandler(client_home + topic1, topic1_handler));
    Assert.assertTrue(directory.addHandler(client_home + topic2, topic2_handler));
    TestConnection connection = new TestConnection(kUserAccount);
    connection.open();

    // topic 1 with catch-all
    Assert.assertTrue(connection.server().event(topic1, "test message"));
    assertThat(connection.getServerOutput(),
               is(jsonArray(8, uri(kUserAccount, topic1), "\"test message\"")));
    assertThat(catchall_handler.getArgument(0), is("test message"));
    assertThat(topic1_handler.getArgument(0), is("test message"));
    Assert.assertNull(topic2_handler.getArgument(0));

    // topic 2 with bean
    TestBean bean = new TestBean(235, 3.1415, "bean");
    Assert.assertTrue(connection.server().event(topic2, bean));
    Assert.assertNull(catchall_handler.getArgument(0));
    assertThat(topic1_handler.getArgument(0), is("test message"));
    assertThat((TestBean) topic2_handler.getArgumentAs(0, bean), is(bean));

    connection.close();
  }

  /**
   * Tests publish messages, including subscriptions.
   */
  @Test
  public void publish() {
    final String kRootHome = serverHomePath("root");
    final String user_account_1 = "publisher1@domain.zz";
    final String user_account_2 = "publisher2@domain.zz";
    final String user_account_3 = "publisher3@domain.zz";
    TestUser[] users = {
      new TestUser(user_account_1),
      new TestUser(user_account_2),
      new TestUser(user_account_3)
    };

    // main topic path on server
    createTopicPaths(kRootHome, null);

    // initialize user accounts
    for (TestUser user : users) {
      user.begin();
    }

    // Subscribe to topics, user n publishes category n + 1 and subscribes to the other categories.
    // explicit subscriptions
    Assert.assertTrue(users[0].getConnection().client().subscribe("/topics/category2/topic1"));
    Assert.assertTrue(users[0].getConnection().client().subscribe("/topics/category2/topic2"));
    Assert.assertTrue(users[0].getConnection().client().subscribe("/topics/category3/topic1"));
    Assert.assertTrue(users[0].getConnection().client().subscribe("/topics/category3/topic2"));
    Assert.assertTrue(users[1].getConnection().client().subscribe("/topics/category1/topic1"));
    Assert.assertTrue(users[1].getConnection().client().subscribe("/topics/category1/topic2"));
    // wildcard subscriptions
    Assert.assertTrue(users[1].getConnection().client().subscribe("/topics/category3/*"));
    Assert.assertTrue(users[2].getConnection().client().subscribe("/topics/category1/*"));
    Assert.assertTrue(users[2].getConnection().client().subscribe("/topics/category2/*"));

    int data = 1;
    // test publishing of messages
    testPublish(users, 0, new int[] {0, 1, 1}, "/topics/category1/topic1", data++);
    testPublish(users, 0, new int[] {0, 1, 1}, "/topics/category1/topic2", data++);
    testPublish(users, 1, new int[] {1, 0, 1}, "/topics/category2/topic1", data++);
    testPublish(users, 1, new int[] {1, 0, 1}, "/topics/category2/topic2", data++);
    testPublish(users, 2, new int[] {1, 1, 0}, "/topics/category3/topic1", data++);
    testPublish(users, 2, new int[] {1, 1, 0}, "/topics/category3/topic2", data++);

    // publish to non-existing node, should trigger catchall
    testPublish(users, 0, new int[] {-1, -1, 2}, "/topics/category1/topic3", data++);

    // explicit exlcude
    String[] exclude = { "test-session-" + user_account_2 };
    String[] eligible = {};
    testPublish(users, 0, new int[] {0, 0, 1},
                false, exclude, eligible,
                "/topics/category1/topic1", data++);
    exclude = new String[] { "test-session-" + user_account_2, "test-session-" + user_account_3 };
    testPublish(users, 0, new int[] {0, 0, 0},
                false, exclude, eligible,
                "/topics/category1/topic1", data++);

    // explicit include
    exclude = new String[] {};
    eligible = new String[] { "test-session-" + user_account_2 };
    testPublish(users, 0, new int[] {0, 1, 0},
                false, exclude, eligible,
                "/topics/category1/topic1", data++);

    // stop receiving after unsubscribing
    Assert.assertTrue(users[0].getConnection().client().unsubscribe("/topics/category2/topic1"));
    testPublish(users, 1, new int[] {0, 0, 1}, "/topics/category2/topic1", data++);
    Assert.assertTrue(users[1].getConnection().client().unsubscribe("/topics/category3/*"));
    testPublish(users, 2, new int[] {1, 0, 0}, "/topics/category3/topic2", data++);

    // subscribe to self
    Assert.assertTrue(users[0].getConnection().client().subscribe("/topics/category1/topic1"));
    testPublish(users, 0, new int[] {1, 1, 1}, "/topics/category1/topic1", data++);

    // exclude me
    testPublish(users, 0, new int[] {0, 1, 1},
                true, null, null,
                "/topics/category1/topic1", data++);

    // deny publish access
    denyAccess(kRootHome, users[0].getServerHomePath(), "category1");
    testPublish(users, 0, new int[] {0, 0, 0}, "/topics/category1/topic1", data++);

    // deny subscribe access
    denyAccess(kRootHome, users[0].getServerHomePath(), "category2");
    testPublish(users, 1, new int[] {0, 0, 1}, "/topics/category2/topic2", data++);

    for (TestUser user : users) {
      user.end();
    }
  }

  /**
   * Tests RPC calls including call results and call errors.
   */
  @Test
  public void call() {
    final String kRootHome = serverHomePath("root");
    final String kUserAccount = "caller@domain.zz";
    final String kUserHome = serverHomePath(kUserAccount);
    Directory directory = Directory.Instance;
    Assert.assertTrue(directory.createPath(kRootHome + RpcHandler.kMethod1));
    Assert.assertTrue(directory.createPath(kRootHome + RpcHandler.kMethod2));
    Assert.assertTrue(directory.createPath(kRootHome + RpcHandler.kCatchallMethod2));
    Assert.assertTrue(directory.createPath(kRootHome + RpcHandler.kCallError));
    Assert.assertTrue(directory.createPath(kRootHome + RpcHandler.kCallErrorWithDetails));

    // create method handlers
    Assert.assertTrue(directory.addHandler(kRootHome + RpcHandler.kMethod1,
                                           new RpcHandler(RpcHandler.kMethod1)));
    Assert.assertTrue(directory.addHandler(kRootHome + RpcHandler.kMethod2,
                                           new RpcHandler(RpcHandler.kMethod2)));
    Assert.assertTrue(directory.addHandler(kRootHome + RpcHandler.kCatchall,
                                           new RpcHandler(RpcHandler.kCatchall)));
    Assert.assertTrue(directory.addHandler(kRootHome + RpcHandler.kCatchallMethod2,
                                           new RpcHandler(RpcHandler.kCatchallMethod2)));
    Assert.assertTrue(directory.addHandler(kRootHome + RpcHandler.kCallError,
                                           new RpcHandler(RpcHandler.kCallError)));
    Assert.assertTrue(directory.addHandler(kRootHome + RpcHandler.kCallErrorWithDetails,
                                           new RpcHandler(RpcHandler.kCallErrorWithDetails)));

    // map methods onto user home
    Assert.assertTrue(directory.createPath(kUserHome + "/rpc"));
    Assert.assertTrue(directory.link(kUserHome + "/rpc", kRootHome + RpcHandler.kMethod1));
    Assert.assertTrue(directory.link(kUserHome + "/rpc", kRootHome + RpcHandler.kMethod2));
    Assert.assertTrue(directory.link(kUserHome + "/rpc", kRootHome + RpcHandler.kCatchall));
    Assert.assertTrue(directory.link(kUserHome + "/rpc", kRootHome + RpcHandler.kCallError));
    Assert.assertTrue(directory.link(kUserHome + "/rpc",
                                     kRootHome + RpcHandler.kCallErrorWithDetails));
    TestConnection connection = new TestConnection(kUserAccount);
    connection.open();

    // call no arguments, no result
    TestCallback callback = new TestCallback();
    Assert.assertTrue(connection.client().call(RpcHandler.kMethod1, callback));
    String[] client_output = connection.getClientOutput().split(",");
    assertThat(client_output.length, is(3));
    assertThat(client_output[0], is("[2"));
    assertThat(client_output[2], is(uri(kUserAccount, RpcHandler.kMethod1) + "]"));
    String[] server_reply = connection.getServerOutput().split(",");
    assertThat(server_reply.length, is(3));
    assertThat(server_reply[0], is("[3"));
    assertThat(server_reply[1], is(client_output[1]));
    assertThat(server_reply[2], is("null]"));
    Assert.assertTrue(connection.client().process(connection.getServerOutput()));
    assertThat(callback.getCallbackType(), is(TestCallback.CallbackType.kSuccess));
    String old_callid = client_output[1];

    // call 3 arguments, 1 result
    callback.clear();
    Assert.assertTrue(connection.client().call(RpcHandler.kMethod2, callback, 2, 3, 5));
    client_output = connection.getClientOutput().split(",");
    assertThat(client_output.length, is(6));
    assertThat(client_output[0], is("[2"));
    assertThat(client_output[1], is(not(old_callid)));
    assertThat(client_output[2], is(uri(kUserAccount, RpcHandler.kMethod2)));
    server_reply = connection.getServerOutput().split(",");
    assertThat(server_reply.length, is(3));
    assertThat(server_reply[0], is("[3"));
    assertThat(server_reply[1], is(client_output[1]));
    assertThat(server_reply[2], is("10]"));
    Assert.assertTrue(connection.client().process(connection.getServerOutput()));
    assertThat(callback.getCallbackType(), is(TestCallback.CallbackType.kSuccess));
    assertThat((int) callback.getResult(), is(10));

    // call 2 methods with catch-all
    callback.clear();
    Assert.assertTrue(connection.client().call(RpcHandler.kCatchallMethod2, callback, 2, 3, 5));
    client_output = connection.getClientOutput().split(",");
    assertThat(client_output.length, is(6));
    assertThat(client_output[2], is(uri(kUserAccount, RpcHandler.kCatchallMethod2)));
    server_reply = connection.getServerOutput().split(",");
    assertThat(server_reply.length, is(4));
    assertThat(server_reply[0], is("[3"));
    assertThat(server_reply[1], is(client_output[1]));
    assertThat(server_reply[2], is("[30"));
    assertThat(server_reply[3], is("10]]"));
    Assert.assertTrue(connection.client().process(connection.getServerOutput()));
    assertThat(callback.getCallbackType(), is(TestCallback.CallbackType.kSuccess));
    @SuppressWarnings("unchecked") ArrayList<Integer> array_result =
      (ArrayList<Integer>) callback.getResult();
    assertThat(array_result.size(), is(2));
    assertThat(array_result.get(0), is(30));
    assertThat(array_result.get(1), is(10));

    // call error
    callback.clear();
    Assert.assertTrue(connection.client().call(RpcHandler.kCallError, callback, "test"));
    client_output = connection.getClientOutput().split(",");
    server_reply = connection.getServerOutput().split(",");
    assertThat(server_reply.length, is(4));
    assertThat(server_reply[0], is("[4"));
    assertThat(server_reply[1], is(client_output[1]));
    assertThat(server_reply[2], is(
        "\"wamp://caller%40domain.zz@general.ai/rpc/call_error#logic_error\""));
    assertThat(server_reply[3], is("\"test error\"]"));
    Assert.assertTrue(connection.client().process(connection.getServerOutput()));
    assertThat(callback.getCallbackType(), is(TestCallback.CallbackType.kError));
    assertThat(callback.getErrorUri().toString(),
               is("wamp://caller%40domain.zz@general.ai/rpc/call_error#logic_error"));
    assertThat(callback.getErrorDescription(), is("test error"));
    Assert.assertNull(callback.getErrorDetails());

    // call error with details
    callback.clear();
    Assert.assertTrue(connection.client().call(RpcHandler.kCallErrorWithDetails, callback));
    client_output = connection.getClientOutput().split(",");
    server_reply = connection.getServerOutput().split(",");
    assertThat(server_reply.length, is(5));
    assertThat(server_reply[0], is("[4"));
    assertThat(server_reply[1], is(client_output[1]));
    assertThat(server_reply[2], is(
        "\"wamp://caller%40domain.zz@general.ai/rpc/call_error_with_details#logic_error\""));
    assertThat(server_reply[3], is("\"test error with details\""));
    assertThat(server_reply[4], is("1001]"));
    Assert.assertTrue(connection.client().process(connection.getServerOutput()));
    assertThat(callback.getCallbackType(), is(TestCallback.CallbackType.kError));
    assertThat(callback.getErrorUri().toString(),
               is("wamp://caller%40domain.zz@general.ai/rpc/call_error_with_details#logic_error"));
    assertThat(callback.getErrorDescription(), is("test error with details"));
    assertThat((int) callback.getErrorDetails(), is(1001));

    // deny access
    Assert.assertTrue(directory.unlink(kUserHome + "/rpc", kRootHome + RpcHandler.kMethod1));
    callback.clear();
    Assert.assertTrue(connection.client().call(RpcHandler.kMethod1, callback));
    client_output = connection.getClientOutput().split(",");
    server_reply = connection.getServerOutput().split(",");
    assertThat(server_reply.length, is(4));
    assertThat(server_reply[0], is("[4"));
    assertThat(server_reply[1], is(client_output[1]));
    assertThat(server_reply[2],
               is("\"wamp://caller%40domain.zz@general.ai/rpc/method1#rpc_error\""));
    assertThat(server_reply[3], is("\"undefined method\"]"));
    Assert.assertTrue(connection.client().process(connection.getServerOutput()));
    assertThat(callback.getCallbackType(), is(TestCallback.CallbackType.kError));
    assertThat(callback.getErrorUri().toString(),
               is("wamp://caller%40domain.zz@general.ai/rpc/method1#rpc_error"));
    assertThat(callback.getErrorDescription(), is("undefined method"));
    Assert.assertNull(callback.getErrorDetails());

    connection.close();
  }

  /**
   * Tests prefix requests.
   */
  @Test
  public void prefix() {
    final String kRootHome = serverHomePath("root");
    final String kUserAccount = "prefix@domain.zz";
    final String kUserHome = serverHomePath(kUserAccount);
    Directory directory = Directory.Instance;
    Assert.assertTrue(directory.createPath(kRootHome + RpcHandler.kPrefixMethod));
    Assert.assertTrue(directory.addHandler(kRootHome + RpcHandler.kPrefixMethod,
                                           new RpcHandler(RpcHandler.kPrefixMethod)));
    Assert.assertTrue(directory.createPath(kUserHome + "/rpc"));
    Assert.assertTrue(directory.link(kUserHome + "/rpc", kRootHome + RpcHandler.kPrefixMethod));
    TestConnection connection = new TestConnection(kUserAccount);
    connection.open();

    Uri uri = new Uri("wamp", "general.ai", "/rpc/");
    uri.setUser(kUserAccount);
    Assert.assertTrue(connection.client().prefix("curie", uri));
    assertThat(connection.getClientOutput(),
               is(jsonArray(1, "\"curie\"", "\"wamp://prefix%40domain.zz@general.ai/rpc/\"")));
    Assert.assertTrue(connection.server().process("[2,\"prefix_id\",\"curie:prefix\",100,10,1]"));
    String server_output = connection.getServerOutput();
    Assert.assertNotNull(server_output);
    assertThat(server_output, is("[3,\"prefix_id\",111]"));

    connection.close();
  }

  /**
   * Tets invalid JSON input.
   */
  @Test
  public void invalidJson() {
    final String kUserAccount = "invalid@domain.zz";
    TestConnection test = new TestConnection(kUserAccount);
    Assert.assertFalse(test.server().process("[9, 'invalid']"));
    Assert.assertFalse(test.server().process("{\"problem\": \"not wamp\"}"));
    test.close();
  }

  /**
   * Helper method that returns the client side home directory path for the specified user account.
   *
   * @param user_account The user account name.
   * @return The home directory path for the user on the client machine.
   */
  private static String clientHomePath(String user_account) {
    return "/wamp/clients/" + user_account + "/home";
  }

  /**
   * Helper method to create test topic paths given a home_path.
   *
   * If handlers is not null, creates and adds handlers to the paths and returns the created
   * handlers in the handlers parameter. The caller should provide an empty HashMap that will
   * contain all created handlers when this method returns.
   *
   * @param home_path Directory path under which to add the test topics.
   * @param handlers HashMap to populate with created test handlers.
   */
  private static void createTopicPaths(String home_path,
                                       HashMap<String, GenericTestHandler> handlers) {
    Directory directory = Directory.Instance;
    for (int n = 1; n <= 3; n++) {
      for (int i = 1; i <= 2; i++) {
        String topic_name = "/topics/category" + n + "/topic" + i;
        String topic_path = home_path + topic_name;
        Assert.assertTrue(directory.createPath(topic_path));
        if (handlers != null) {
          GenericTestHandler handler = new GenericTestHandler(topic_name);
          Assert.assertTrue(directory.addHandler(topic_path, handler));
          handlers.put(handler.getName(), handler);
        }
      }
    }
    if (handlers != null) {
      // add catch-all handler
      GenericTestHandler handler = new GenericTestHandler("/topics/*", true);
      Assert.assertTrue(directory.addHandler(home_path + "/topics", handler));
      handlers.put(handler.getName(), handler);
    }
  }

  /**
   * Helper method to deny access to a user to publish under a topic category.
   *
   * @param root_home The directory which contains the topics.
   * @param user_server_home The server side user home directory.
   * @param category The category part of the topic name.
   */
  private static void denyAccess(String root_home,
                                  String user_server_home,
                                  String category) {
    Assert.assertTrue(Directory.Instance.unlink(user_server_home + "/topics",
                                                root_home + "/topics/" + category));
  }

  /**
   * Helper method to grant access to a user to publish under a topic category.
   *
   * @param root_home The directory which contains the topics.
   * @param user_server_home The server side user home directory.
   * @param category The category part of the topic name.
   */
  private static void grantAccess(String root_home,
                                  String user_server_home,
                                  String category) {
    Assert.assertTrue(Directory.Instance.link(user_server_home + "/topics",
                                              root_home + "/topics/" + category));
  }

  /**
   * Helper method to convert a set of objects into a JSON array.
   * This method is appropriate for non-string types. For string types, use
   * {@link #jsonStringArray(String[])}.
   *
   * @param elements Set of elements.
   * @return Converts the elements into strings and combines them into a JSON array.
   */
  private static String jsonArray(Object ... elements) {
    if (elements.length == 0) {
      return "[]";
    }
    StringBuilder result = new StringBuilder("[" + elements[0].toString());
    for (int i = 1; i < elements.length; i++) {
      result.append(",");
      result.append(elements[i].toString());
    }
    result.append("]");
    return result.toString();
  }

  /**
   * Helper method to convert a set of string into a JSON string array.
   *
   * @param elements Set of strings.
   * @return JSON array with String elements.
   */
  private static String jsonStringArray(String[] elements) {
    if (elements.length == 0) {
      return "[]";
    }
    StringBuilder result = new StringBuilder("[\"" + elements[0]);
    for (int i = 1; i < elements.length; i++) {
      result.append("\",\"");
      result.append(elements[i]);
    }
    result.append("\"]");
    return result.toString();
  }

  /**
   * Helper method that returns the server side home directory path for the specified user account.
   *
   * @param user_account The user account name.
   * @return The home directory path for the user on the server.
   */
  private static String serverHomePath(String user_account) {
    return "/wamp/server/user/" + user_account + "/home";
  }

  /**
   * Helper method to call 
   * {@link #testPublish(TestUser[], int, int[], boolean, String[], String[], String, Object)}
   * with the exclude_me and exclude and eligible tests disabled.
   *
   * @param users Test users.
   * @param publisher Index of user that calls publish.
   * @param expect_received 0 = no, 1 = yes, 2 = yes (catch-all), -1 = no (handler undefined).
   * @param topic Topic URI.
   * @param data Data to publish.
   */
  private static void testPublish(TestUser[] users,
                                  int publisher,
                                  int[] expect_received,
                                  String topic,
                                  Object data) {
    testPublish(users, publisher, expect_received, false, null, null, topic, data);
  }

  /**
   * Helper method to test publishing of messages.
   *
   * This method accepts a set of users, one of whom publishes a message with the specified topic
   * and data. The users may or may not receive the message depending on their subscription
   * status. The expectation which users should receive which messages is specified why the
   * expect_received argument.
   *
   * For each user[i], there is a corresponding entry in expect_received[i]. The value of
   * expect_received[i] is interpreted for user[i] as follows:
   * -1: user is not expected to receive message and has no handlers defined for the topic
   * 0: user is not expected to receive message, but has handlers defined for the topic
   * 1: user is expected to receive message
   * 2: user is expected to receive message only through a catch-all handler
   *
   * In case 0, the user has declared handlers but has not yet subscribed to the topic or
   * unsubscribed from the topic.
   *
   * This method also allows testing of the exclude_me, exclude, and eligible features as
   * specified in the WAMP specification.
   * If exclude_me is true, the exclude_me feature is tested. If exclude_me is false and both
   * the exclude and eligible arrays are not null, the exclude and eligible feature is tested.
   *
   * @param users Test users.
   * @param publisher Index of user that calls publish.
   * @param expect_received 0 = no, 1 = yes, 2 = yes (catch-all), -1 = no (handler undefined).
   * @param exclude_me If true, exclude_me feature is tested.
   * @param exclude If exclude and eligible are not null, exclude/eligible feature is tested.
   * @param eligible If exclude and eligible are not null, exclude/eligible feature is tested.
   * @param topic Topic URI.
   * @param data Data to publish.
   */
  private static void testPublish(TestUser[] users,
                                  int publisher,
                                  int[] expect_received,
                                  boolean exclude_me,
                                  String[] exclude,
                                  String[] eligible,
                                  String topic,
                                  Object data) {
    if (exclude_me) {
      Assert.assertTrue(
          users[publisher].getConnection().client().publish(topic, data, exclude_me));
      assertThat(users[publisher].getConnection().getClientOutput(),
                 is(jsonArray(7, uri(users[publisher].getAccount(), topic), data, exclude_me)));
    } else if (exclude != null && eligible != null) {
      Assert.assertTrue(
          users[publisher].getConnection().client().publish(topic, data, exclude, eligible));
      assertThat(users[publisher].getConnection().getClientOutput(),
                 is(jsonArray(7, uri(users[publisher].getAccount(), topic), data,
                              jsonStringArray(exclude), jsonStringArray(eligible))));
    } else {
      Assert.assertTrue(users[publisher].getConnection().client().publish(topic, data));
      assertThat(users[publisher].getConnection().getClientOutput(),
                 is(jsonArray(7, uri(users[publisher].getAccount(), topic), data)));
    }
    for (int i = 0; i < users.length; i++) {
      switch (expect_received[i]) {
        case 0:  // no
          Assert.assertNull(users[i].getHandler(topic).getArgument(0));
          Assert.assertNull(users[i].getHandler("/topics/*").getArgument(0));
          users[i].getHandler(topic).clearArguments();
          break;
        case 1:  // yes
          assertThat(users[i].getConnection().getServerOutput(),
                     is(jsonArray(8, uri(users[i].getAccount(), topic), data)));
          assertThat(users[i].getHandler(topic).getArgument(0), is(data));
          assertThat(users[i].getHandler("/topics/*").getArgument(0), is(data));
          users[i].getHandler(topic).clearArguments();
          break;
        case 2:  // catch-all only, specific handler is undefined
          assertThat(users[i].getConnection().getServerOutput(),
                     is(jsonArray(8, uri(users[i].getAccount(), topic), data)));
          assertThat(users[i].getHandler("/topics/*").getArgument(0), is(data));
          break;
        case -1:  // not received and no specific handler is defined
          Assert.assertNull(users[i].getHandler("/topics/*").getArgument(0));
          break;
      }
      users[i].getHandler("/topics/*").clearArguments();
    }
  }

  /**
   * Helper method that returns a normalized URI string constructed using test parameters for the
   * specified user account and path.
   *
   * @param user_account The user account name.
   * @param path URI path.
   * @return URI string constructed with test parameters that includes the specified path.
   */
  private static String uri(String user_account, String path) {
    return "\"wamp://" + user_account.replace("@", "%40") + "@" + kHostname + path + "\"";
  }
}
