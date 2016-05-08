package IK;

import sceneGraph.AbstractAxes;

public interface Constraint {
	
	public void snapToLimits();
	public AbstractAxes limitingAxes();

}
