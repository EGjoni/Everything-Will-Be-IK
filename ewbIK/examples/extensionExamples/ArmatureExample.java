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
import IK.AbstractArmature;
import IK.AbstractBone.frameType;
import sceneGraph.*;
import sceneGraph.math.SGVec_3d;

public class ArmatureExample extends AbstractArmature{

	/**
	 * Note, this class is a concrete implementation of the abstract class AbstractArmature. Please refer to the {@link AbstractArmature AbstractArmature docs.} 
	 * @param name A label for this armature.  
	 */	
	public ArmatureExample(String name) {		
		super(new AxesExample(new ExampleVector(0,0,0), new ExampleVector(1,0,0), new ExampleVector(0,1,0), new ExampleVector(0,0,1), true, null), name);
	}
	
	public ArmatureExample(AbstractAxes inputOrigin, String name) {
		super(inputOrigin, name);
	}

	@Override
	protected  void initializeRootBone(AbstractArmature armature,SGVec_3d tipHeading,SGVec_3d rollHeading, String inputTag,
			double boneHeight, frameType coordinateType) {
		this.rootBone = new BoneExample(this, 
												AxesExample.toExampleVector(tipHeading), 
												AxesExample.toExampleVector(rollHeading), 
												inputTag, 
												boneHeight, 
												coordinateType);
		
	}

	
	@Override 
	public BoneExample getRootBone() {
		return (BoneExample)rootBone;
	}
	
	@Override
	public BoneExample getBoneTagged(String tag) {
		return (BoneExample)boneMap.get(tag);	
	}

	

}
