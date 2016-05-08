/*

Copyright (c) 2015 Eron Gjoni

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and 
associated documentation files (the "Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 

*/

package IK;
import sceneGraph.*;

public class Kusudama extends AbstractKusudama {

	/**
	 * Kusudamas are a sequential collection of reach cones, forming a path by their tangents. <br><br>
	 *  
	 * A reach cone is essentially a cone bounding the rotation of a ball-and-socket joint.
	 * A reach cone is defined as a vector pointing in the direction which the cone is opening,
	 * and a radius (in radians) representing how much the cone is opening up. 
	 * <br><br>
	 * You can think of a Kusudama (taken from the Japanese word for "ball with a bunch of cones sticking out of it") as a ball with 
	 * with a bunch of reach-cones sticking out of it. Except that these reach cones are arranged sequentially, and a smooth path is 
	 * automatically inferred leading from one cone to the next.  
	 * 
	 * @param forBone the bone this kusudama will be attached to.
	 */
	public Kusudama(Bone forBone) {
		super(forBone);
	}

	@Override
	public void generateAxes(Ray x, Ray y, Ray z) {
		limitingAxes = new Axes(x, y, z);
	}

	/**
	 * {@inheritDoc}
	 **/
	@Override
	public void addLimitConeAtIndex(int insertAt, DVector newPoint, double radius) {
		// TODO Auto-generated method stub
		if(insertAt == -1) {
			limitCones.add(new LimitCone(newPoint, radius, this));
		} else {
			limitCones.add(insertAt, new LimitCone(newPoint, radius, this));
		}
		
		super.addLimitConeAtIndex(insertAt, newPoint, radius);
	}
	
	/**
	 * {@inheritDoc}
	 **/
	public DVector pointInLimits(DVector inPoint, boolean[] inBounds) {
		return super.pointInLimits(inPoint, inBounds);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void snapToLimits() { 
		super.snapToLimits();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAxialLimits(double minAngle, double maxAngle) {
		super.setAxialLimits(minAngle, maxAngle);
	}
	
	
}
