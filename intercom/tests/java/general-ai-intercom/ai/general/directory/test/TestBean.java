/* General AI - Directory Test Support
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory.test;

/**
 * Serves as a test object that can be used with generic handlers.
 */
public class TestBean {

  /**
   * Constructs a default TestBean.
   */
  public TestBean() {
    this(0, 0.0, "");
  }

  /**
   * Constructs a TestBean with the specified values.
   *
   * @param number An integer.
   * @param real A double.
   * @param text A string.
   */
  public TestBean(int number, double real, String text) {
    this.number_ = number;
    this.real_ = real;
    this.text_ = text;
  }

  /**
   * Checkes whether other is a TestBean and has the same values as this TestBean.
   *
   * @param other Other TestBean to compare with this bean.
   * @return True if other is a TestBean and has the same values as this TestBean.
   */
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof TestBean)) return false;
    TestBean other_bean = (TestBean) other;
    return
      number_ == other_bean.number_ &&
      real_ == other_bean.real_ &&
      text_.equals(other_bean.text_);
  }

  /**
   * Returns the number value.
   *
   * @return The number value.
   */
  public int getNumber() {
    return number_;
  }

  /**
   * Returns the real value.
   *
   * @return The real value.
   */
  public double getReal() {
    return real_;
  }

  /**
   * Returns the text value.
   *
   * @return The text value.
   */
  public String getText() {
    return text_;
  }

  /**
   * Sets the number value.
   *
   * @param number The new number value.
   */
  public void setNumber(int number) {
    this.number_ = number;
  }

  /**
   * Sets the real value.
   *
   * @param real The new real value.
   */
  public void setReal(double real) {
    this.real_ = real;
  }

  /**
   * Returns a string representation of this TestBean.
   *
   * @return A string representation of this TestBean.
   */
  @Override
  public String toString() {
    return "TestBean={number=" + number_ + ", real=" + real_ + ", text=" + text_ + "}";
  }

  /**
   * Sets the text value.
   *
   * @param text The new text value.
   */
  public void setText(String text) {
    this.text_ = text;
  }

  private int number_;
  private double real_;
  private String text_;
}
