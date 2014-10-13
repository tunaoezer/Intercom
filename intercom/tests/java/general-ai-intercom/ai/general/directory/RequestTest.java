/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

import ai.general.net.Uri;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for the {@link Request} class.
 */
public class RequestTest {

  private static final String kTestUri =
    "wamp://testuser@general.ai/robot/future@hope.org/base/velocity?" +
    "type=publish&callid=1234567890&=invalid&=&extra&ok=";
  private static final String kJsonData = "[true]";
  private static final String kPathUri = "/a/b/c";
  private static final String kQueryUri = "/?x";

  /**
   * Tests all getters.
   */
  @Test
  public void getters() {
    Request request = new Request(new Uri(kTestUri), kJsonData);
    request.addArgument("argument");
    request.addArguments(new Object[] {"array", 1});
    request.addArgument(new Object[] {"array", 1});
    assertThat(request.getUri().getUser(), is("testuser"));
    assertThat(request.getRequestType(), is(Request.RequestType.Publish));
    assertThat(request.numArguments(), is(5));
    assertThat(request.getArgument(0).toString(), is(kJsonData));
    assertThat(request.getArgument(1).toString(), is("argument"));
    assertThat(request.getArgument(2).toString(), is("array"));
    assertThat((int) request.getArgument(3), is(1));
    Object[] array_argument = (Object[]) request.getArgument(4);
    assertThat(array_argument.length, is(2));
    assertThat(array_argument[0].toString(), is("array"));
    assertThat((int) array_argument[1], is(1));
    assertThat(request.getUri().hasParameter("callid"), is(true));
    assertThat(request.getUri().hasParameter("invalid"), is(false));
    assertThat(request.getUri().hasParameter("extra"), is(true));
    assertThat(request.getUri().hasParameter("ok"), is(true));
    assertThat(request.getUri().getParameter("callid"), is("1234567890"));
    assertThat(request.getUri().getParameter("extra"), is(""));
    assertThat(request.getUri().getParameter("ok"), is(""));

    request = new Request(new Uri(kPathUri), kJsonData);
    assertThat(request.getUri().getUser(), is(""));
    assertThat(request.getRequestType(), is(Request.RequestType.Unspecified));

    request = new Request(new Uri(kQueryUri), kJsonData);
    assertThat(request.getUri().hasParameter("x"), is(true));
    assertThat(request.getUri().getParameter("x"), is(""));
  }

  /**
   * Tests adding results and setting errors.
   */
  @Test
  public void result() {
    Request request = new Request(new Uri(kTestUri), kJsonData);
    Result result = request.getResult();
    result.addValue("result string");
    result.addValue(123);
    assertThat(result.numValues(), is(2));
    assertThat((String) result.getValue(0), is("result string"));
    assertThat((int) result.getValue(1), is(123));

    result.addError(new Result.Error("first", 0.123));
    result.addError(new Result.Error("second", 987));
    Assert.assertTrue(result.hasErrors());
    assertThat(result.numErrors(), is(2));
    assertThat(result.getError(0).getDescription(), is("first"));
    assertThat((double) result.getError(0).getDetails(), is(0.123));
    assertThat(result.getError(1).getDescription(), is("second"));
    assertThat((int) result.getError(1).getDetails(), is(987));
  }
}
