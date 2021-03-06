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

package heronarts.lx.output;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXFixture;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the output stage from the LX engine to real devices.
 * Outputs may have their own brightness, be enabled/disabled, be throttled,
 * etc.
 */
public abstract class LXOutput extends LXComponent {

  static int[] fixtureToIndices(LXFixture fixture) {
    List<LXPoint> points = fixture.getPoints();
    int[] indices = new int[points.size()];
    int i = 0;
    for (LXPoint p : points) {
      indices[i++] = p.index;
    }
    return indices;
  }

  private final List<LXOutput> children = new ArrayList<LXOutput>();

  /**
   * Buffer with colors for this output, gamma-corrected
   */
  private final int[] outputColors;

  /**
   * Local array for color-conversions
   */
  private final float[] hsb = new float[3];

  /**
   * Whether the output is enabled.
   */
  public final BooleanParameter enabled =
    new BooleanParameter("Enabled", true)
    .setDescription("Whether the output is active");

  public enum Mode {
    NORMAL,
    WHITE,
    RAW,
    OFF
  };

  /**
   * Sending mode, 0 = normal, 1 = all white, 2 = all off
   */
  public final EnumParameter<Mode> mode =
    new EnumParameter<Mode>("Mode", Mode.NORMAL)
    .setDescription("Operation mode of this output");

  /**
   * Framerate throttle
   */
  public final BoundedParameter framesPerSecond =
    new BoundedParameter("FPS", 0, 300)
    .setDescription("Maximum frames per second this output will send");

  /**
   * Gamma correction level
   */
  public final DiscreteParameter gammaCorrection =
    new DiscreteParameter("Gamma", 4)
    .setDescription("Gamma correction on the output, 0 is none");

  /**
   * Brightness of the output
   */
  public final BoundedParameter brightness =
    new BoundedParameter("Brightness", 1)
    .setDescription("Level of the output");

  /**
   * Time last frame was sent at.
   */
  private long lastFrameMillis = 0;

  private final int[] allWhite;

  private final int[] allOff;

  protected LXOutput(LX lx) {
    this(lx, "Output");
  }

  protected LXOutput(LX lx, String label) {
    super(lx, label);
    this.outputColors = new int[lx.total];
    this.allWhite = new int[lx.total];
    this.allOff = new int[lx.total];
    for (int i = 0; i < lx.total; ++i) {
      this.allWhite[i] = LXColor.WHITE;
      this.allOff[i] = LXColor.BLACK;
    }
    addParameter("enabled", this.enabled);
    addParameter("mode", this.mode);
    addParameter("fps", this.framesPerSecond);
    addParameter("gamma", this.gammaCorrection);
    addParameter("brightness", this.brightness);
  }

  /**
   * Adds a child to this output, sent after color-correction
   *
   * @param child Child output
   * @return this
   */
  public LXOutput addChild(LXOutput child) {
    // TODO(mcslee): need to setParent() on the LXComponent...
    this.children.add(child);
    return this;
  }

  /**
   * Removes a child
   *
   * @param child Child output
   * @return this
   */
  public LXOutput removeChild(LXOutput child) {
    this.children.remove(child);
    return this;
  }

  /**
   * Sends data to this output, after applying throttle and color correction
   *
   * @param colors Array of color values
   * @return this
   */
  public final LXOutput send(int[] colors) {
    if (!this.enabled.isOn()) {
      return this;
    }
    long now = System.currentTimeMillis();
    double fps = this.framesPerSecond.getValue();
    if ((fps == 0) || ((now - this.lastFrameMillis) > (1000. / fps))) {
      int[] colorsToSend;

      switch (this.mode.getEnum()) {
      case WHITE:
        int white = LXColor.hsb(0, 0, 100 * this.brightness.getValuef());
        for (int i = 0; i < this.allWhite.length; ++i) {
          this.allWhite[i] = white;
        }
        colorsToSend = this.allWhite;
        break;

      case OFF:
        colorsToSend = this.allOff;
        break;

      case RAW:
        colorsToSend = colors;
        break;

      default:
      case NORMAL:
        colorsToSend = colors;
        int gamma = this.gammaCorrection.getValuei();
        double brt = this.brightness.getValuef();
        if (gamma > 0 || brt < 1) {
          int r, g, b, rgb;
          for (int i = 0; i < colorsToSend.length; ++i) {
            rgb = colorsToSend[i];
            r = (rgb >> 16) & 0xff;
            g = (rgb >> 8) & 0xff;
            b = rgb & 0xff;
            Color.RGBtoHSB(r, g, b, this.hsb);
            float scaleBrightness = this.hsb[2];
            for (int x = 0; x < gamma; ++x) {
              scaleBrightness *= this.hsb[2];
            }
            scaleBrightness *= brt;
            this.outputColors[i] = Color.HSBtoRGB(hsb[0], hsb[1], scaleBrightness);
          }
          colorsToSend = this.outputColors;
        }
        break;
      }

      this.onSend(colorsToSend);

      for (LXOutput child : this.children) {
        child.send(colorsToSend);
      }
      this.lastFrameMillis = now;
    }
    return this;
  }

  /**
   * Subclasses implement this to send the data.
   *
   * @param colors Color values
   */
  protected abstract void onSend(int[] colors);
}