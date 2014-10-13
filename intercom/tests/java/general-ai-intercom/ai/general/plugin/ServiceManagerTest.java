/* General AI - Plugin Support
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.plugin;

import ai.general.directory.Directory;
import ai.general.directory.Request;
import ai.general.directory.Result;
import ai.general.directory.test.TestBean;
import ai.general.directory.test.TestUtilities;
import ai.general.plugin.annotation.RpcMethod;
import ai.general.plugin.annotation.Subscribe;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for {@link ServiceManager} class and related annotations.
 */
public class ServiceManagerTest {

  /**
   * Test service that declares a Subscribe and an RPC method.
   */
  public static class TestService {

    public TestService() {
      bean_ = null;
    }

    /**
     * Test RPC method that combines bean1 and bean2 and returns a combined bean.
     * Numbers are added and strings are concatenated.
     *
     * @param bean1 First bean to combine.
     * @param bean2 Second bean to combine.
     * @return The combined bean.
     */
    @RpcMethod("methods/combine")
    public TestBean combine(TestBean bean1, TestBean bean2) {
      return new TestBean(bean1.getNumber() + bean2.getNumber(),
                          bean1.getReal() + bean2.getReal(),
                          bean1.getText() + bean2.getText());
    }

    /**
     * @return The bean from the last event or null.
     */
    public TestBean getBean() {
      return bean_;
    }

    /**
     * Test subscription that accepts and stores a bean.
     *
     * @param bean Event data.
     */
    @Subscribe("events/bean")
    public void onEvent(TestBean bean) {
      this.bean_ = bean;
    }

    private TestBean bean_;
  }

  /**
   * Tests the subscribe annotation.
   */
  @Test
  public void subscribe() {
    try {
      final String kServiceName = "test_subscribe_service";
      final String kHomePath = "/service_manager/subscribe/";
      TestService service = new TestService();
      final String kHandlerName = kServiceName + ":Publish:" +
        service.getClass().getDeclaredMethod("onEvent", TestBean.class).toString();

      ServiceManager service_manager = ServiceManager.Instance;
      Assert.assertNull(service_manager.getService(kServiceName));
      ServiceDefinition service_def = service_manager.addService(kServiceName, service, kHomePath);
      Assert.assertNotNull(service_def);
      assertThat(service_def.getName(), is(kServiceName));
      assertThat(service_def.getHomePath(), is(kHomePath));
      Assert.assertTrue(service_def.getService() == service);
      Assert.assertTrue(service_manager.getServiceDefinition(kServiceName) == service_def);
      Assert.assertTrue(service_manager.getService(kServiceName) == service);

      Directory directory = Directory.Instance;
      Assert.assertTrue(directory.pathExists(kHomePath + "events/bean"));
      Assert.assertTrue(directory.hasHandler(kHomePath + "events/bean", kHandlerName));

      TestBean bean = new TestBean(987, 1.23, "subscribe");
      Request request = TestUtilities.createRequest(
          "wamp://test@general.ai/events/bean?type=publish", bean);
      assertThat(directory.handle(kHomePath, request), is(1));
      Assert.assertNotNull(service.getBean());
      assertThat(service.getBean(), is(bean));

      service_manager.removeService(kServiceName);
      Assert.assertFalse(directory.hasHandler(kHomePath + "events/bean", kHandlerName));
    } catch (NoSuchMethodException e) {
      Assert.fail(e.toString());
    }
  }

  /**
   * Tests the RpcMethod annotation.
   */
  @Test
  public void rpcMethod() {
    try {
      final String kServiceName = "test_rpc_service";
      final String kHomePath = "/service_manager/rpc/";
      TestService service = new TestService();
      final String kHandlerName = kServiceName + ":Call:" +
        service.getClass().getDeclaredMethod("combine", TestBean.class, TestBean.class).toString();

      ServiceManager service_manager = ServiceManager.Instance;
      Assert.assertNull(service_manager.getService(kServiceName));
      ServiceDefinition service_def =
        service_manager.addService(kServiceName, service, kHomePath);
      Assert.assertNotNull(service_def);
      assertThat(service_def.getName(), is(kServiceName));
      assertThat(service_def.getHomePath(), is(kHomePath));
      Assert.assertTrue(service_def.getService() == service);
      Assert.assertTrue(service_manager.getServiceDefinition(kServiceName) == service_def);
      Assert.assertTrue(service_manager.getService(kServiceName) == service);

      Directory directory = Directory.Instance;
      Assert.assertTrue(directory.pathExists(kHomePath + "methods/combine"));
      Assert.assertTrue(directory.hasHandler(kHomePath + "methods/combine", kHandlerName));

      TestBean[] args = {
        new TestBean(101, -2.3, "rpc"),
        new TestBean(1010, 2.3, "Method")
      };
      TestBean expected_result = service.combine(args[0], args[1]);
      Request request = TestUtilities.createRequest(
          "wamp://test@general.ai/methods/combine?type=call", (Object[]) args);
      assertThat(directory.handle(kHomePath, request), is(1));
      Result result = request.getResult();
      assertThat(result.numValues(), is(1));
      assertThat((TestBean) result.getValue(0), is(expected_result));

      service_manager.removeService(kServiceName);
      Assert.assertFalse(directory.hasHandler(kHomePath + "methods/combine", kHandlerName));
    } catch (NoSuchMethodException e) {
      Assert.fail(e.toString());
    }
  }
}
