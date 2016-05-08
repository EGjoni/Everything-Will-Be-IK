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
import IK.AbstractBone.frameType;
import sceneGraph.*;

public class Armature extends AbstractArmature{

	/**
	 * Note, this class is a concrete implementation of the abstract class AbstractArmature. Please refer to the {@link AbstractArmature AbstractArmature docs.} 
	 * @param name A label for this armature.  
	 */	
	public Armature(String name) {		
		super(new Axes(new DVector(0,0,0), new DVector(1,0,0), new DVector(0,1,0), new DVector(0,0,1)), name);
		DVector test = new DVector(0,0,0);
	}
	
	public Armature(AbstractAxes inputOrigin, String name) {
		super(inputOrigin, name);
	}

	@Override
	protected void initializeRootBone(AbstractArmature armature, DVector tipHeading, DVector rollHeading, String inputTag,
			double boneHeight, frameType coordinateType) {
		this.rootBone = new Bone(this, tipHeading, rollHeading, inputTag, boneHeight, coordinateType);
		
	}
	
	@Override
	protected void initializeRootBone(Object app, AbstractArmature armature, DVector tipHeading, DVector rollHeading,
			String inputTag, double boneHeight, frameType coordinateType) {
		// TODO Auto-generated method stub
		
	}
	
	@Override 
	public Bone getRootBone() {
		return (Bone)rootBone;
	}
	
	@Override
	public Bone getBoneTagged(String tag) {
		return (Bone)boneMap.get(tag);	
	}

	

}
