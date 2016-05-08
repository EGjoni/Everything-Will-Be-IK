package IK;

import sceneGraph.AbstractAxes;
import sceneGraph.DVector;
import sceneGraph.Rot;

public class IKPin extends AbstractIKPin{
	
	public IKPin(AbstractAxes inAxes, boolean enabled, AbstractBone bone) {
		super(inAxes, enabled, bone);
	}
	
	public IKPin(AbstractAxes inAxes, AbstractBone bone) {
		super(inAxes, bone);
	}

}
