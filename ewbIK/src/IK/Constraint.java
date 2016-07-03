package IK;

import sceneGraph.AbstractAxes;

public interface Constraint {
	
	public void snapToLimits();
	public void disable(); 
	public void enable(); 
	public boolean isEnabled();
	public AbstractAxes limitingAxes();

}
