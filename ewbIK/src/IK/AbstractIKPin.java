package IK;

import sceneGraph.AbstractAxes;
import sceneGraph.DVector;
import sceneGraph.Rot;

public abstract class AbstractIKPin {
	
	protected boolean isEnabled; 
	protected AbstractAxes axes;
	protected AbstractBone forBone;
	
	public AbstractIKPin(AbstractAxes inAxes, boolean enabled, AbstractBone bone) {
		this.isEnabled = enabled; 
		this.axes = inAxes;
		this.forBone = bone;
	}
	
	public AbstractIKPin(AbstractAxes inAxes, AbstractBone bone) {
		this.axes = inAxes;
		this.forBone = bone;
		this.isEnabled = false;
	}
	
	public boolean isEnabled() {
		return isEnabled; 
	}
	
	public void toggle() {
		this.isEnabled = !this.isEnabled;
	}
	
	public void enable() {
		this.isEnabled = true; 
	}
	
	public void disable() {
		this.isEnabled = false;
	}
	
	public DVector getLocation() {
		return axes.origin();
	}
	
	public AbstractAxes getAxes() {
		return axes; 
	}
	
	public void alignToAxes(AbstractAxes inAxes) {
		this.axes.translateTo(inAxes.origin());
		Rot rotation = new Rot(axes.x().heading(), axes.y().heading(), inAxes.x().heading(), inAxes.y().heading());
	}
	
	public void translateTo(DVector location) {
		this.axes.translateTo(location);
	}
	
	public AbstractBone forBone() {
		return this.forBone;
		
	}
	

}
