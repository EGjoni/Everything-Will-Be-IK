/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package sceneGraph.math;

import sceneGraph.math.doubleV.MathUtils;

/** Takes a linear value in the range of 0-1 and outputs a (usually) non-linear, interpolated value.
 * @author Nathan Sweet */
public abstract class Interpolation {
	/** @param a Alpha value between 0 and 1. */
	abstract public double apply (double a);

	/** @param a Alpha value between 0 and 1. */
	public double apply (double start, double end, double a) {
		return start + (end - start) * apply(a);
	}

	//

	static public final Interpolation linear = new Interpolation() {
		public double apply (double a) {
			return a;
		}
	};

	//

	/** Aka "smoothstep". */
	static public final Interpolation smooth = new Interpolation() {
		public double apply (double a) {
			return a * a * (3 - 2 * a);
		}
	};
	static public final Interpolation smooth2 = new Interpolation() {
		public double apply (double a) {
			a = a * a * (3 - 2 * a);
			return a * a * (3 - 2 * a);
		}
	};

	/** By Ken Perlin. */
	static public final Interpolation smoother = new Interpolation() {
		public double apply (double a) {
			return MathUtils.clamp(a * a * a * (a * (a * 6 - 15) + 10), 0, 1);
		}
	};
	static public final Interpolation fade = smoother;

	//

	static public final Pow pow2 = new Pow(2);
	/** Slow, then fast. */
	static public final PowIn pow2In = new PowIn(2);
	/** Fast, then slow. */
	static public final PowOut pow2Out = new PowOut(2);
	static public final Interpolation pow2InInverse = new Interpolation() {
		public double apply (double a) {
			return (double)Math.sqrt(a);
		}
	};
	static public final Interpolation pow2OutInverse = new Interpolation() {
		public double apply (double a) {
			return 1 - (double)Math.sqrt(-(a - 1));
		}
	};

	static public final Pow pow3 = new Pow(3);
	static public final PowIn pow3In = new PowIn(3);
	static public final PowOut pow3Out = new PowOut(3);
	static public final Interpolation pow3InInverse = new Interpolation() {
		public double apply (double a) {
			return (double)Math.cbrt(a);
		}
	};
	static public final Interpolation pow3OutInverse = new Interpolation() {
		public double apply (double a) {
			return 1 - (double)Math.cbrt(-(a - 1));
		}
	};

	static public final Pow pow4 = new Pow(4);
	static public final PowIn pow4In = new PowIn(4);
	static public final PowOut pow4Out = new PowOut(4);

	static public final Pow pow5 = new Pow(5);
	static public final PowIn pow5In = new PowIn(5);
	static public final PowOut pow5Out = new PowOut(5);

	static public final Interpolation sine = new Interpolation() {
		public double apply (double a) {
			return (1 - MathUtils.cos(a * MathUtils.PI)) / 2;
		}
	};

	static public final Interpolation sineIn = new Interpolation() {
		public double apply (double a) {
			return 1 - MathUtils.cos(a * MathUtils.PI / 2);
		}
	};

	static public final Interpolation sineOut = new Interpolation() {
		public double apply (double a) {
			return MathUtils.sin(a * MathUtils.PI / 2);
		}
	};

	static public final Exp exp10 = new Exp(2, 10);
	static public final ExpIn exp10In = new ExpIn(2, 10);
	static public final ExpOut exp10Out = new ExpOut(2, 10);

	static public final Exp exp5 = new Exp(2, 5);
	static public final ExpIn exp5In = new ExpIn(2, 5);
	static public final ExpOut exp5Out = new ExpOut(2, 5);

	static public final Interpolation circle = new Interpolation() {
		public double apply (double a) {
			if (a <= 0.5f) {
				a *= 2;
				return (1 - (double)Math.sqrt(1 - a * a)) / 2;
			}
			a--;
			a *= 2;
			return ((double)Math.sqrt(1 - a * a) + 1) / 2;
		}
	};

	static public final Interpolation circleIn = new Interpolation() {
		public double apply (double a) {
			return 1 - (double)Math.sqrt(1 - a * a);
		}
	};

	static public final Interpolation circleOut = new Interpolation() {
		public double apply (double a) {
			a--;
			return (double)Math.sqrt(1 - a * a);
		}
	};

	static public final Elastic elastic = new Elastic(2, 10, 7, 1);
	static public final ElasticIn elasticIn = new ElasticIn(2, 10, 6, 1);
	static public final ElasticOut elasticOut = new ElasticOut(2, 10, 7, 1);

	static public final Swing swing = new Swing(1.5f);
	static public final SwingIn swingIn = new SwingIn(2f);
	static public final SwingOut swingOut = new SwingOut(2f);

	static public final Bounce bounce = new Bounce(4);
	static public final BounceIn bounceIn = new BounceIn(4);
	static public final BounceOut bounceOut = new BounceOut(4);

	//

	static public class Pow extends Interpolation {
		final int power;

		public Pow (int power) {
			this.power = power;
		}

		public double apply (double a) {
			if (a <= 0.5f) return (double)Math.pow(a * 2, power) / 2;
			return (double)Math.pow((a - 1) * 2, power) / (power % 2 == 0 ? -2 : 2) + 1;
		}
	}

	static public class PowIn extends Pow {
		public PowIn (int power) {
			super(power);
		}

		public double apply (double a) {
			return (double)Math.pow(a, power);
		}
	}

	static public class PowOut extends Pow {
		public PowOut (int power) {
			super(power);
		}

		public double apply (double a) {
			return (double)Math.pow(a - 1, power) * (power % 2 == 0 ? -1 : 1) + 1;
		}
	}

	//

	static public class Exp extends Interpolation {
		final double value, power, min, scale;

		public Exp (double value, double power) {
			this.value = value;
			this.power = power;
			min = (double)Math.pow(value, -power);
			scale = 1 / (1 - min);
		}

		public double apply (double a) {
			if (a <= 0.5f) return ((double)Math.pow(value, power * (a * 2 - 1)) - min) * scale / 2;
			return (2 - ((double)Math.pow(value, -power * (a * 2 - 1)) - min) * scale) / 2;
		}
	};

	static public class ExpIn extends Exp {
		public ExpIn (double value, double power) {
			super(value, power);
		}

		public double apply (double a) {
			return ((double)Math.pow(value, power * (a - 1)) - min) * scale;
		}
	}

	static public class ExpOut extends Exp {
		public ExpOut (double value, double power) {
			super(value, power);
		}

		public double apply (double a) {
			return 1 - ((double)Math.pow(value, -power * a) - min) * scale;
		}
	}

	//

	static public class Elastic extends Interpolation {
		final double value, power, scale, bounces;

		public Elastic (double value, double power, int bounces, double scale) {
			this.value = value;
			this.power = power;
			this.scale = scale;
			this.bounces = bounces * MathUtils.PI * (bounces % 2 == 0 ? 1 : -1);
		}

		public double apply (double a) {
			if (a <= 0.5f) {
				a *= 2;
				return (double)Math.pow(value, power * (a - 1)) * MathUtils.sin(a * bounces) * scale / 2;
			}
			a = 1 - a;
			a *= 2;
			return 1 - (double)Math.pow(value, power * (a - 1)) * MathUtils.sin((a) * bounces) * scale / 2;
		}
	}

	static public class ElasticIn extends Elastic {
		public ElasticIn (double value, double power, int bounces, double scale) {
			super(value, power, bounces, scale);
		}

		public double apply (double a) {
			if (a >= 0.99) return 1;
			return (double)Math.pow(value, power * (a - 1)) * MathUtils.sin(a * bounces) * scale;
		}
	}

	static public class ElasticOut extends Elastic {
		public ElasticOut (double value, double power, int bounces, double scale) {
			super(value, power, bounces, scale);
		}

		public double apply (double a) {
			a = 1 - a;
			return (1 - (double)Math.pow(value, power * (a - 1)) * MathUtils.sin(a * bounces) * scale);
		}
	}

	//

	static public class Bounce extends BounceOut {
		public Bounce (double[] widths, double[] heights) {
			super(widths, heights);
		}

		public Bounce (int bounces) {
			super(bounces);
		}

		private double out (double a) {
			double test = a + widths[0] / 2;
			if (test < widths[0]) return test / (widths[0] / 2) - 1;
			return super.apply(a);
		}

		public double apply (double a) {
			if (a <= 0.5f) return (1 - out(1 - a * 2)) / 2;
			return out(a * 2 - 1) / 2 + 0.5f;
		}
	}

	static public class BounceOut extends Interpolation {
		final double[] widths, heights;

		public BounceOut (double[] widths, double[] heights) {
			if (widths.length != heights.length)
				throw new IllegalArgumentException("Must be the same number of widths and heights.");
			this.widths = widths;
			this.heights = heights;
		}

		public BounceOut (int bounces) {
			if (bounces < 2 || bounces > 5) throw new IllegalArgumentException("bounces cannot be < 2 or > 5: " + bounces);
			widths = new double[bounces];
			heights = new double[bounces];
			heights[0] = 1;
			switch (bounces) {
			case 2:
				widths[0] = 0.6f;
				widths[1] = 0.4f;
				heights[1] = 0.33f;
				break;
			case 3:
				widths[0] = 0.4f;
				widths[1] = 0.4f;
				widths[2] = 0.2f;
				heights[1] = 0.33f;
				heights[2] = 0.1f;
				break;
			case 4:
				widths[0] = 0.34f;
				widths[1] = 0.34f;
				widths[2] = 0.2f;
				widths[3] = 0.15f;
				heights[1] = 0.26f;
				heights[2] = 0.11f;
				heights[3] = 0.03f;
				break;
			case 5:
				widths[0] = 0.3f;
				widths[1] = 0.3f;
				widths[2] = 0.2f;
				widths[3] = 0.1f;
				widths[4] = 0.1f;
				heights[1] = 0.45f;
				heights[2] = 0.3f;
				heights[3] = 0.15f;
				heights[4] = 0.06f;
				break;
			}
			widths[0] *= 2;
		}

		public double apply (double a) {
			a += widths[0] / 2;
			double width = 0, height = 0;
			for (int i = 0, n = widths.length; i < n; i++) {
				width = widths[i];
				if (a <= width) {
					height = heights[i];
					break;
				}
				a -= width;
			}
			a /= width;
			double z = 4 / width * height * a;
			return 1 - (z - z * a) * width;
		}
	}

	static public class BounceIn extends BounceOut {
		public BounceIn (double[] widths, double[] heights) {
			super(widths, heights);
		}

		public BounceIn (int bounces) {
			super(bounces);
		}

		public double apply (double a) {
			return 1 - super.apply(1 - a);
		}
	}

	//

	static public class Swing extends Interpolation {
		private final double scale;

		public Swing (double scale) {
			this.scale = scale * 2;
		}

		public double apply (double a) {
			if (a <= 0.5f) {
				a *= 2;
				return a * a * ((scale + 1) * a - scale) / 2;
			}
			a--;
			a *= 2;
			return a * a * ((scale + 1) * a + scale) / 2 + 1;
		}
	}

	static public class SwingOut extends Interpolation {
		private final double scale;

		public SwingOut (double scale) {
			this.scale = scale;
		}

		public double apply (double a) {
			a--;
			return a * a * ((scale + 1) * a + scale) + 1;
		}
	}

	static public class SwingIn extends Interpolation {
		private final double scale;

		public SwingIn (double scale) {
			this.scale = scale;
		}

		public double apply (double a) {
			return a * a * ((scale + 1) * a - scale);
		}
	}
}
