/* General AI - Directory
 * Copyright (C) 2014 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents the result of a processing a {@link Request}.
 *
 * A Result consist of a set of result values and may contain a set of errors. The specific details
 * of the result values are application specific. Result values are stored as objects in Result.
 * Errors may also contains application specific details stored as objects.
 *
 * Result values are serialized into JSON during transmission. Thus, result values must have a POJO
 * type. This includes primitive types, classes that allow access to their instance variables
 * via get and set methods, and arrays and collections of these types.
 */
public class Result {

  /**
   * Represents an error result.
   */
  public static class Error {

    /**
     * Constructs an error result with the specified parameters.
     * If error details are provided, they must be serializable to JSON. In particular, the
     * error details must be of a POJO type, which include primitive types, bean like objects and
     * arrays or collections of POJO types.
     *
     * @param description A short description of the error. Mandatory.
     * @param details Optional application specific error details or null.
     */
    public Error(String description, Object details) {
      this.description_ = description;
      this.details_ = details;
    }

    /**
     * Returns a brief error description.
     *
     * @return Error description.
     */
    public String getDescription() {
      return description_;
    }

    /**
     * Returns optional application specific error details. Returns null if no details have been
     * specified.
     *
     * @return Details or null.
     */
    public Object getDetails() {
      return details_;
    }

    private String description_;  // Mandatory error description.
    private Object details_;  // Optional error details.
  }

  /**
   * Constructs an empty result.
   */
  public Result() {
    errors_ = new ArrayList<Error>();
    values_ = new ArrayList<Object>();
  }

  /**
   * Adds the specified error to the result.
   *
   * @param error The result error.
   */
  public void addError(Error error) {
    errors_.add(error);
  }

  /**
   * Adds a result value.
   *
   * @param value The result value.
   */
  public void addValue(Object value) {
    values_.add(value);
  }

  /**
   * Returns the result error at specified index.
   * Returns null if there is no result error at the specified index.
   *
   * @param index The result error index.
   * @return The result error at the specified index or null.
   */
  public Error getError(int index) {
    if (errors_.size() <= index) {
      return null;
    }
    return errors_.get(index);
  }

  /**
   * Returns all result errors.
   *
   * @return The result errors.
   */
  public Collection<Error> getErrors() {
    return errors_;
  }

  /**
   * Returns the result value at specified index.
   * Returns null if there is no result value at the specified index.
   *
   * @param index The result value index.
   * @return The result value at the specified index or null.
   */
  public Object getValue(int index) {
    if (values_.size() <= index) {
      return null;
    }
    return values_.get(index);
  }

  /**
   * Returns all result values.
   *
   * @return The results.
   */
  public Collection<Object> getValues() {
    return values_;
  }

  /**
   * Returns true if the result has any errors.
   *
   * @return True if the result has any errors.
   */
  public boolean hasErrors() {
    return errors_.size() > 0;
  }

  /**
   * Returns the number of result errors.
   *
   * @return Number of result errors.
   */
  public int numErrors() {
    return errors_.size();
  }

  /**
   * Returns the number of result values.
   *
   * @return Number of result values.
   */
  public int numValues() {
    return values_.size();
  }

  private ArrayList<Error> errors_;  // Result errors.
  private ArrayList<Object> values_;  // Result values.
}
