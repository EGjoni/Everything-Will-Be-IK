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

package extensionExamples;
import IK.AbstractKusudama;
import IK.AbstractLimitCone;
import sceneGraph.math.SGVec_3d;

public class KusudamaExample extends AbstractKusudama {

	/**
	 * Kusudamas are a sequential collection of reach cones, forming a path by their tangents. <br><br>
	 *  
	 * A reach cone is essentially a cone bounding the rotation of a ball-and-socket joint.
	 * A reach cone is defined as a vector pointing in the direction which the cone is opening,
	 * and a radius (in radians) representing how much the cone is opening up. 
	 * <br><br>
	 * You can think of a KusudamaExample (taken from the Japanese word for "ball with a bunch of cones sticking out of it") as a ball with 
	 * with a bunch of reach-cones sticking out of it. Except that these reach cones are arranged sequentially, and a smooth path is 
	 * automatically inferred leading from one cone to the next.  
	 * 
	 * @param forBone the bone this kusudama will be attached to.
	 */
	public KusudamaExample(BoneExample forBone) {
		super(forBone);
	}


	/**
	 * {@inheritDoc}
	 **/
	@Override
	public AbstractLimitCone createLimitConeForIndex(int insertAt, SGVec_3d newPoint, double radius) {
		return new LimitCone(AxesExample.toExampleVector(newPoint), radius, this);		
	}
	

	public boolean isInLimits(ExampleVector inPoint) {
		return super.isInLimits_(
				AxesExample.toSGVec(inPoint));
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
