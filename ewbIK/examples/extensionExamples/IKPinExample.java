package extensionExamples;

import IK.AbstractBone;
import IK.AbstractIKPin;
import sceneGraph.AbstractAxes;

public class IKPinExample extends AbstractIKPin{
	
	public IKPinExample(AxesExample inAxes, boolean enabled, BoneExample bone) {
		super(inAxes, enabled, bone);
	}
	
	public IKPinExample(AxesExample inAxes, BoneExample bone) {
		super(inAxes, bone);
	}
	
	
	
	///WRAPPER FUNCTIONS. Basically just ctrl+f and replace these with the appropriate class names and 
	//any conversion functions you modified in AxesExample and you should be good to go. 
	public ExampleVector getLocation() {
		return AxesExample.toExampleVector(super.getLocation_());
	}
	

	public void translateTo(ExampleVector v) {
		 super.translateTo_(AxesExample.toSGVec(v));
	}
	
	public void translateBy(ExampleVector v) {
		 super.translateBy_(AxesExample.toSGVec(v));
	}

}
