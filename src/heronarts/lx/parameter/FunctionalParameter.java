/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx.parameter;

import heronarts.lx.LXComponent;

/**
 * An LXParameter that has a value computed by a function, which may combine the
 * values of other parameters, or call some function, etc.
 */
public abstract class FunctionalParameter implements LXParameter {

  private final String label;

  private LXComponent component;
  private String path;

  protected FunctionalParameter() {
    this("FUNC-PARAM");
  }

  protected FunctionalParameter(String label) {
    this.label = label;
  }

  public String getDescription() {
    return null;
  }

  @Override
  public LXParameter setComponent(LXComponent component, String path) {
    if (component == null || path == null) {
      throw new IllegalArgumentException("May not set null component or path");
    }
    if (this.component != null || this.path != null) {
      throw new IllegalStateException("Component already set on this modulator: " + this);
    }
    this.component = component;
    this.path = path;
    return this;
  }

  @Override
  public LXComponent getComponent() {
    return this.component;
  }

  @Override
  public String getPath() {
    return this.path;
  }

  @Override
  public Polarity getPolarity() {
    return Polarity.UNIPOLAR;
  }

  @Override
  public Formatter getFormatter() {
    return getUnits();
  }

  @Override
  public Units getUnits() {
    return Units.NONE;
  }

  @Override
  public void dispose() {}

  /**
   * Does nothing, subclass may override.
   */
  public FunctionalParameter reset() {
    return this;
  }

  /**
   * Not supported for this parameter type unless subclass overrides.
   *
   * @param value The value
   */
  public LXParameter setValue(double value) {
    throw new UnsupportedOperationException(
        "FunctionalParameter does not support setValue()");
  }

  /**
   * Retrieves the value of the parameter, subclass must implement.
   *
   * @return Parameter value
   */
  public abstract double getValue();

  /**
   * Utility helper function to get the value of the parameter as a float.
   *
   * @return Parameter value as float
   */
  public float getValuef() {
    return (float) getValue();
  }

  /**
   * Gets the label for this parameter
   *
   * @return Label of parameter
   */
  public final String getLabel() {
    return this.label;
  }

}
