package IK.doubleIK;

import java.util.ArrayList;

import data.EWBIKLoader;
import data.EWBIKSaver;
import data.JSONObject;
import data.LoadManager;
import data.SaveManager;
import data.Saveable;
import sceneGraph.math.doubleV.AbstractAxes;
import sceneGraph.math.doubleV.SGVec_3d;

public abstract class AbstractIKPin implements Saveable {
	
	protected boolean isEnabled; 
	protected AbstractAxes axes;
	protected AbstractBone forBone;
	protected AbstractIKPin parentPin; 		
	protected ArrayList<AbstractIKPin> childPins = new ArrayList<>();
	double pinWeight  = 1;
	short modeCode = 3; 
	int subTargetCount = 3; 	
	static final short XDir = 1, YDir = 2, ZDir = 4;
	protected double xPriority =1d , yPriority =1d, zPriority = 0d;
	
	public AbstractIKPin() {}
	
	public AbstractIKPin(AbstractAxes inAxes, boolean enabled, AbstractBone bone) {
		this.isEnabled = enabled; 
		this.axes = inAxes;
		this.forBone = bone;
		setTargetPriorities(xPriority, yPriority, zPriority);
	}
	
	public AbstractIKPin(AbstractAxes inAxes, AbstractBone bone) {
		this.axes = inAxes;
		this.forBone = bone;
		this.isEnabled = false;
		setTargetPriorities(xPriority, yPriority, zPriority);
	}
	
	public boolean isEnabled() {
		return isEnabled; 
	}
	
	public void toggle() {
		if(this.isEnabled()) disable(); 
		else this.enable();
	}
	
	public void enable() {
		this.isEnabled = true; 
	}
	
	public void disable() {
		this.isEnabled = false;
	}
	
	/**
	 * Sets  the priority of the orientation bases which effectors reaching for this target will and won't align with. 
	 * If all are set to 0, then the target is treated as a simple position target. 
	 * It's usually better to set at least on of these three values to 0, as giving a nonzero value to all three is most often redundant. 
	 * 
	 *  This values this function sets are only considered by the orientation aware solver. 
	 *  
	 * @param position
	 * @param xPriority set to a positive value (recommended between 0 and 1) if you want the bone's x basis to point in the same direction as this target's x basis (by this library's convention the x basis corresponds to a limb's twist) 
	 * @param yPriority set to a positive value (recommended between 0 and 1)  if you want the bone's y basis to point in the same direction as this target's y basis (by this library's convention the y basis corresponds to a limb's direction) 
	 * @param zPriority set to a positive value (recommended between 0 and 1)  if you want the bone's z basis to point in the same direction as this target's z basis (by this library's convention the z basis corresponds to a limb's twist) 
	 */
	public void setTargetPriorities(double xPriority, double yPriority, double zPriority) {
		boolean xDir = xPriority > 0 ? true : false;
		boolean yDir = yPriority > 0 ? true : false;
		boolean zDir = zPriority > 0 ? true : false;
		modeCode =0; 
		if(xDir) modeCode += XDir; 
		if(yDir) modeCode += YDir; 
		if(zDir) modeCode += ZDir;
		
		subTargetCount = 1;
		if((modeCode &1) != 0) subTargetCount++;   
		if((modeCode &2) != 0) subTargetCount++;   
		if((modeCode &4) != 0) subTargetCount++; 
		
		this.xPriority = xPriority;
		this.yPriority = yPriority;
		this.zPriority = zPriority;
	}
	
	/**
	 * @return the number of bases an effector to this target will attempt to align on.
	 */
	public int getSubtargetCount() {
		return subTargetCount;
	}
	
	public short getModeCode() {
		return modeCode;
	}
	
	/**
	 * @return the priority of this pin's x axis; 
	 */
	public double getXPriority() {
		return this.xPriority;
	}
	
	/**
	 * @return the priority of this pin's y axis; 
	 */
	public double getYPriority() {
		return this.yPriority;
	}
	
	/**
	 * @return the priority of this pin's z axis; 
	 */
	public double getZPriority() {
		return this.zPriority;
	}
	
	public AbstractAxes getAxes() {
		return axes; 
	}

	/**
	 * translates and rotates the pin to match the position 
	 * and orientation of the input Axes. The orientation 
	 * is only relevant for orientation aware solvers.
	 * @param inAxes
	 */
	public void alignToAxes(AbstractAxes inAxes) {
		this.axes.alignGlobalsTo(inAxes);
		//Rot rotation = new Rot(axes.x().heading(), axes.y().heading(), inAxes.x().heading(), inAxes.y().heading());
	}
	
	/**
	 * translates the pin to the location specified in global coordinates
	 * @param location
	 */
	public void translateTo_(SGVec_3d location) {
		this.axes.translateTo(location);
	}
	
	/**
	 * translates the pin to the location specified in local coordinates 
	 * (relative to any other Axes objects the pin may be parented to)
	 * @param location
	 */
	public void translateBy_(SGVec_3d location) {
		this.axes.translateTo(location);
	}
	
	/**
	 * @return the pin locationin global coordinates
	 */
	public SGVec_3d getLocation_() {
		return axes.origin_();
	}
	
	
	
	
	public AbstractBone forBone() {
		return this.forBone;
		
	}

	/**
	 * called when this pin is being removed entirely from the Armature. (as opposed to just being disabled)
	 */
	public void removalNotification() {
		for(AbstractIKPin cp : childPins) 
			cp.setParentPin(getParentPin());
	}
	
	
	public void setParentPin(AbstractIKPin parent) {
		if(this.parentPin != null) {
			this.parentPin.removeChildPin(this);
		}
		//set the parent to the global axes if the user
		//tries to set the pin to be its own parent

		if(parent == this || parent == null) {
			this.axes.setParent(null);			
		} else if (parent != null){
			this.axes.setParent(parent.axes);	
			parent.addChildPin(this);
			this.parentPin = parent; 
		}
	}	
	


	public void solveIKForThisAndChildren() {
		
		try { 
			for(AbstractIKPin childPin : childPins) {
				childPin.solveIKForThisAndChildren();
			}
		this.forBone.solveIKFromHere(); 
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}


	public void removeChildPin(AbstractIKPin child) {
		childPins.remove(child);
	}

	public void addChildPin(AbstractIKPin newChild) {
		if(newChild.isAncestorOf(this)) {
			this.setParentPin(newChild.getParentPin());
		}
		if(!childPins.contains(newChild)) childPins.add(newChild);
	}

	public AbstractIKPin getParentPin() {
		return this.parentPin;
	}
	
	public boolean isAncestorOf(AbstractIKPin potentialDescendent) {
		boolean result = false;
		AbstractIKPin cursor = potentialDescendent.getParentPin();
		while(cursor != null) {
			if(cursor == this) {
				result = true; 
				break; 
			} else {
				cursor = cursor.parentPin; 
			}
		}		
		return result; 
	}
	
	
	public double getPinWeight() {
		return pinWeight;
	}
	
	
	/**
	 * Currently only works with tranquil solver. 
	 * 
	 * @param weight any positive number representing how much the IK solver
	 * should prefer to satisfy this pin over competing pins. For example, setting 
	 * one pin's weight to 90 and a competing pins weight to 10 will mean the IK solver 
	 * will prefer to satisfy the pin with a weight of 90 by as much as 9 times over satisfying 
	 * the pin with a weight of 10. 
	 * 
	 * 
	 */
	public void setPinWeight(double weight) {
		this.pinWeight = weight;
	}
	
	@Override
	public void makeSaveable(SaveManager saveManager) {
		saveManager.addToSaveState(this);
		getAxes().makeSaveable(saveManager);
	}
	
	@Override
	public JSONObject getSaveJSON(SaveManager saveManager) {
		JSONObject saveJSON = new JSONObject(); 
		saveJSON.setString("identityHash", this.getIdentityHash());
		saveJSON.setString("axes", getAxes().getIdentityHash()); 
		saveJSON.setString("forBone", forBone.getIdentityHash());
		saveJSON.setBoolean("isEnabled", this.isEnabled());
		saveJSON.setDouble("pinWeight", this.pinWeight);
		return saveJSON;
	}
	
	
	public void loadFromJSONObject(JSONObject j, LoadManager l) {
		this.axes = (AbstractAxes) l.getObjectFromClassMaps(AbstractAxes.class, j.getString("axes"));
		this.isEnabled = j.getBoolean("isEnabled"); 
		this.pinWeight = j.getDouble("pinWeight");
		this.forBone = (AbstractBone) l.getObjectFromClassMaps(AbstractBone.class, j.getString("forBone")); 
		
	}

	
	@Override
	public void notifyOfSaveIntent(SaveManager saveManager) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyOfSaveCompletion(SaveManager saveManager) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isLoading() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setLoading(boolean loading) {
		// TODO Auto-generated method stub
		
	}
}
