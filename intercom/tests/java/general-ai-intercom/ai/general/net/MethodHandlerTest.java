/* General AI - Networking
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

import ai.general.directory.Result;
import ai.general.directory.Request;
import ai.general.directory.test.TestBean;
import ai.general.directory.test.TestUtilities;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for the {@link MethodHandler} class.
 */
public class MethodHandlerTest {

  /**
   * TestObject defines test methods that are called via a MethodHandler.
   */
  private static class TestObject {

    /**
     * Constructs a default test object.
     */
    public TestObject() {
      clear();
    }

    /**
     * Test method that returns the product of number and real.
     *
     * @param number An integer.
     * @param real A real.
     * @return number * real.
     */
    public double call1(int number, double real) {
      this.number_ = number;
      this.real_ = real;
      return number * real;
    }

    /**
     * Test method that returns the sum of the squares of number and real.
     *
     * @param number An integer.
     * @param real A real.
     * @return number^2 + real^2.
     */
    public double call2(int number, double real) {
      this.number_ = number;
      this.real_ = real;
      return number * number + real * real;
    }

    /**
     * Test method that sets the number of each bean in the bean array to its index + 1 and
     * returns the updated array.
     *
     * @param bean_array Array of TestBeans.
     * @return Updated array of TestBeans.
     */
    public TestBean[] callBeanArray(TestBean[] bean_array) {
      this.bean_array_ = bean_array;
      for (int i = 0; i < bean_array.length; i++) {
        bean_array[i].setNumber(i + 1);
      }
      return bean_array;
    }

    /**
     * Test method that throws an exception with the provided description and details if fail is
     * true.
     *
     * @param fail If true, throws an exception.
     * @param description Exception description.
     * @param details Exception details represented by a TestBean.
     * @return 1
     * @throws RpcException if fail is true.
     */
    public int callError(boolean fail, String description, TestBean details) throws RpcException {
      if (fail) {
        throw new RpcException(description, details);
      }
      return 1;
    }

    /**
     * Resets the variables of TestObject to default values.
     */
    public void clear() {
      number_ = 0;
      real_ = 0.0;
      bean_ = null;
      bean_array_ = null;
    }

    /**
     * Returns the TestBean captured from a call to one of the test methods.
     *
     * @return Captured TestBean.
     */
    public TestBean getBean() {
      return bean_;
    }

    /**
     * Returns the TestBean array captured from a call to one of the test methods.
     *
     * @return Captured TestBean array.
     */
    public TestBean[] getBeanArray() {
      return bean_array_;
    }

    /**
     * Returns the number captured from a call to one of the test methods.
     *
     * @return Captured number.
     */
    public int getNumber() {
      return number_;
    }

    /**
     * Returns the real captured from a call to one of the test methods.
     *
     * @return Captured real.
     */
    public double getReal() {
      return real_;
    }

    /**
     * Sets the TestBean.
     *
     * @param bean Updated TestBean value.
     */
    public void setBean(TestBean bean) {
      this.bean_ = bean;
    }

    /**
     * Sets the TestBean array.
     *
     * @param bean_array Updated TestBean array.
     */
    public void setBeanArray(TestBean[] bean_array) {
      this.bean_array_ = bean_array;
    }

    /**
     * Sets the number.
     *
     * @param number Updated number.
     */
    public void setNumber(int number) {
      this.number_ = number;
    }

    private TestBean bean_;
    private TestBean[] bean_array_;
    private int number_;
    private double real_;
  }

  /**
   * Tests handling of publish requests.
   */
  @Test
  public void publish() {
    try {
      // publish integer
      TestObject test = new TestObject();
      MethodHandler handler =
        new MethodHandler("setNumber",
                          false,
                          test,
                          test.getClass().getDeclaredMethod("setNumber", int.class));
      Request request =
        TestUtilities.createRequest("wamp://general.ai/setNumber?type=publish", 123);
      handler.handle(request);
      assertThat(test.getNumber(), is(123));

      // publish object
      test.clear();
      ObjectMapper json_parser = new ObjectMapper();
      Object data = json_parser.readValue("{\"real\":3.14159, \"text\":\"pi\"}", Object.class);
      handler =
        new MethodHandler("setBean",
                          false,
                          test,
                          test.getClass().getDeclaredMethod("setBean", TestBean.class));
      request = TestUtilities.createRequest("wamp://general.ai/setBean?type=publish", data);
      handler.handle(request);
      Assert.assertNotNull(test.getBean());
      assertThat(test.getBean().getReal(), is(3.14159));
      assertThat(test.getBean().getText(), is("pi"));

      // publish object array
      test.clear();
      Object array = json_parser.readValue("[{\"real\":1.41421, \"text\":\"sqrt(2)\"}," +
                                            "{\"real\":1.7321, \"text\":\"sqrt(3)\"}]",
                                           Object.class);
      handler =
        new MethodHandler("setBeanArray",
                          false,
                          test,
                          test.getClass().getDeclaredMethod("setBeanArray", TestBean[].class));
      request = TestUtilities.createRequest("wamp://general.ai/setBeanArray?type=publish", array);
      handler.handle(request);
      Assert.assertNotNull(test.getBeanArray());
      assertThat(test.getBeanArray().length, is(2));
      Assert.assertNotNull(test.getBeanArray()[0]);
      assertThat(test.getBeanArray()[0].getReal(), is(1.41421));
      assertThat(test.getBeanArray()[0].getText(), is("sqrt(2)"));
      Assert.assertNotNull(test.getBeanArray()[1]);
      assertThat(test.getBeanArray()[1].getReal(), is(1.7321));
      assertThat(test.getBeanArray()[1].getText(), is("sqrt(3)"));
    } catch (Exception e) {
      Assert.fail(e.toString());
    }
  }

  /**
   * Tests handling of call requests with no error.
   */
  @Test
  public void call() {
    try {
      // call 2 argument method with primitives types
      ObjectMapper json_parser = new ObjectMapper();
      Object[] data = json_parser.readValue("[2, 3.14]", Object[].class);
      TestObject test = new TestObject();
      MethodHandler handler =
        new MethodHandler("call1",
                          false,
                          test,
                          test.getClass().getDeclaredMethod("call1", int.class, double.class));
      Request request = TestUtilities.createRequest("wamp://general.ai/call?type=call", data);
      handler.handle(request);
      Result result = request.getResult();
      Assert.assertFalse(result.hasErrors());
      assertThat(test.getNumber(), is(2));
      assertThat(test.getReal(), is(3.14));
      assertThat(result.numValues(), is(1));
      assertThat((double) result.getValue(0), is(6.28));

      // second call adds more results
      handler =
        new MethodHandler("call2",
                          false,
                          test,
                          test.getClass().getDeclaredMethod("call2", int.class, double.class));
      handler.handle(request);
      result = request.getResult();
      assertThat(result.numValues(), is(2));
      assertThat((double) result.getValue(0), is(6.28));
      assertThat((double) result.getValue(1), is(13.8596));

      // call Object array with Object array return value
      test.clear();
      Object[] array = {
        json_parser.readValue("[{\"real\":0.5, \"text\":\"1/2\"}," +
                               "{\"real\":0.25, \"text\":\"1/4\"}]",
                              json_parser.getTypeFactory().constructCollectionType(
                                  ArrayList.class, Object.class))
      };
      handler =
        new MethodHandler("callBeanArray",
                          false,
                          test,
                          test.getClass().getDeclaredMethod("callBeanArray", TestBean[].class));
      request = TestUtilities.createRequest("wamp://general.ai/callBeanArray?type=call", array);
      handler.handle(request);
      result = request.getResult();
      Assert.assertFalse(result.hasErrors());
      Assert.assertNotNull(test.getBeanArray());
      assertThat(test.getBeanArray().length, is(2));
      Assert.assertNotNull(test.getBeanArray()[0]);
      assertThat(test.getBeanArray()[0].getReal(), is(0.5));
      assertThat(test.getBeanArray()[0].getText(), is("1/2"));
      Assert.assertNotNull(test.getBeanArray()[1]);
      assertThat(test.getBeanArray()[1].getReal(), is(0.25));
      assertThat(test.getBeanArray()[1].getText(), is("1/4"));
      assertThat(result.numValues(), is(1));
      TestBean[] result_beans = (TestBean[]) result.getValue(0);
      assertThat(result_beans.length, is(2));
      assertThat(result_beans[0].getNumber(), is(1));
      assertThat(result_beans[0].getReal(), is(0.5));
      assertThat(result_beans[0].getText(), is("1/2"));
      assertThat(result_beans[1].getNumber(), is(2));
      assertThat(result_beans[1].getReal(), is(0.25));
      assertThat(result_beans[1].getText(), is("1/4"));
    } catch (Exception e) {
      Assert.fail(e.toString());
    }
  }

  /**
   * Tests handling of call requests with error.
   */
  @Test
  public void callError() {
    try {
      ObjectMapper json_parser = new ObjectMapper();
      Object[] args = json_parser.readValue("[true, \"MethodHandlerTest\", " +
                                            "{\"real\":1, \"text\":\"details\"}]", Object[].class);
      TestObject test = new TestObject();
      MethodHandler handler =
        new MethodHandler("callError",
                          false,
                          test,
                          test.getClass().getDeclaredMethod(
                              "callError", boolean.class, String.class, TestBean.class));
      Request request = TestUtilities.createRequest("wamp://general.ai/callError?type=call", args);
      handler.handle(request);
      Result result = request.getResult();
      assertThat(result.numValues(), is(0));
      Assert.assertTrue(result.hasErrors());
      Result.Error error = result.getError(0);
      assertThat(error.getDescription(), is("MethodHandlerTest"));
      assertThat(((TestBean) error.getDetails()).getReal(), is(1.0));
      assertThat(((TestBean) error.getDetails()).getText(), is("details"));

      // call method with incompatible arguments
      // incorrect type
      args = json_parser.readValue("[\"text\", 1]", Object[].class);
      handler =
        new MethodHandler("call1",
                          false,
                          test,
                          test.getClass().getDeclaredMethod("call1", int.class, double.class));
      request = TestUtilities.createRequest("wamp://general.ai/call?type=call", args);
      handler.handle(request);
      result = request.getResult();
      assertThat(result.numValues(), is(0));
      Assert.assertTrue(result.hasErrors());
      error = result.getError(0);
      assertThat(error.getDescription(), is("java.lang.IllegalArgumentException"));
      Assert.assertTrue(((String) error.getDetails()).startsWith(
          "Can not construct instance of java.lang.Integer from String value 'text'"));

      // incorrect number of arguments
      args = json_parser.readValue("[1, 2, 3]", Object[].class);
      handler =
        new MethodHandler("call1",
                          false,
                          test,
                          test.getClass().getDeclaredMethod("call1", int.class, double.class));
      request = TestUtilities.createRequest("wamp://general.ai/call?type=call", args);
      handler.handle(request);
      result = request.getResult();
      assertThat(result.numValues(), is(0));
      Assert.assertTrue(result.hasErrors());
      error = result.getError(0);
      assertThat(error.getDescription(), is("invalid number of method arguments"));
      assertThat((String) error.getDetails(), is("got 3 arguments for method with 2 arguments"));
    } catch (Exception e) {
      Assert.fail(e.toString());
    }
  }
}
