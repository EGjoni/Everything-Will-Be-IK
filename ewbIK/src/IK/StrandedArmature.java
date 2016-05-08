package IK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

import sceneGraph.AbstractAxes;
import sceneGraph.Rot;

public class StrandedArmature {
	/**
	 * Note: This code is doing a lot of stuff conceptually in relatively few lines of code 
	 * and might be difficult to wrap your head around.  I've opted to make it concise, 
	 * because making it verbose did not make it any easier to digest. If you're going to edit it,
	 * you might want to take some time to graph things out.
	 */

	public ArrayList<Strand> strands = new ArrayList<Strand>();
	public ArrayList<StrandedArmature> childCollections = new ArrayList<StrandedArmature>();

	public ArrayList<AbstractBone> allBonesInStrandCollection = new ArrayList<AbstractBone>();
	public HashMap<AbstractBone, ArrayList<Strand>> boneStrandMap = new HashMap<AbstractBone, ArrayList<Strand>>();
	public HashMap<AbstractBone, Double> angleDeltaMap = new HashMap<AbstractBone, Double>(); 
	
	Double lastLargestAngleDelta = null;
	Double amountIncreasing = 0d;
	Double amountDecreasing = 0d;
	int increasingSampleCount = 0;
	int decreasingSampleCount = 0;
	//HashMap<AbstractBone, AbstractAxes> originalOrientations = new HashMap<AbstractBone, AbstractAxes>();

	protected Strand parentStrand = null;

	public double totalPinDist = 0;
	public int distanceToRoot = 0;

	public int chainLength = 0;
	boolean includeInIK = true;
	boolean basePinned = false;

	AbstractBone strandRoot;
	

	public StrandedArmature(AbstractBone rootBone) {
		this.strandRoot = armatureRootBone(rootBone);
		generateStrandHierarchy();
	}

	public StrandedArmature(Strand inputParentStrand, AbstractBone inputStrandRoot) {
		this.strandRoot = inputStrandRoot;
		this.parentStrand= inputParentStrand;
		this.distanceToRoot = this.parentStrand.parentStrandCollection.distanceToRoot+1;
		this.basePinned = parentStrand == null? false : parentStrand.strandTip.isPinned();
		generateStrandHierarchy();  
	}

	public void generateStrandHierarchy() {
		ArrayList<AbstractBone> pinnedTips = strandRoot.getMostImmediatelyPinnedDescendants();
		for(AbstractBone b : pinnedTips) {
			Strand s = new Strand(this, b, strandRoot);
			strands.add(s);
			childCollections.addAll(s.generateChildStrandCollection());
		}
		//refreshOriginalAxesMap();
	}

	public StrandedArmature getStrandCollectionFor(AbstractBone bone) {
		if(this.allBonesInStrandCollection.contains(bone)) {
			return this;
		} else {
			StrandedArmature result = null;
			for(StrandedArmature sa : childCollections) {
				result = sa.getStrandCollectionFor(bone);
				if(result != null) {
					return result;
				}
			}
			return result;
		}
	}
	
	public void resetStabilityMeasures() {
		lastLargestAngleDelta = null;
		amountIncreasing = 0d; 
		amountDecreasing = 0d;
		increasingSampleCount = 0;
		decreasingSampleCount = 0;
	}
	
	public void updateStabilityEstimates() {
		
		double thisLargestDelta = getLargestAngleChange();
		if(lastLargestAngleDelta != null) {
			double change = thisLargestDelta - lastLargestAngleDelta; 
			if(change < 0) {
				amountDecreasing += -1* change; 
				increasingSampleCount ++; 
			} else {
				amountIncreasing += change;
				decreasingSampleCount ++;
			}
		}
		
		lastLargestAngleDelta = thisLargestDelta; 
		
	}
	
	private double getLargestAngleChange() {
		double largestDelta = 0;
		for(AbstractBone b : allBonesInStrandCollection) {
			Double delta = Math.abs(angleDeltaMap.get(b)); 
			if(delta != null && delta > largestDelta) {
				largestDelta = delta;
			}
		}
		return largestDelta;
	}
	
	public void setDeltaMeasureForBone(AbstractBone b, double angle) {
		angleDeltaMap.put(b, angle);
	}
	
	public double getStability() {
		/*if(amountIncreasing == 0) return 0;
		else if(amountDecreasing == 0) return 1; 
		else {
			return amountIncreasing / (amountIncreasing + amountDecreasing);
		}*/
		
		if(increasingSampleCount == 0 ) return 0; 
		else if (decreasingSampleCount  == 0) return 1; 
		else {
			return (double)increasingSampleCount / ((double)increasingSampleCount + (double)decreasingSampleCount);
		}
	}

	/*public void alignMultiStrandBonesToOriginalAxes() {
		for(AbstractBone b: allBonesInStrandCollection) {
			if(boneStrandMap.get(b).size() > 1) {
				b.localAxes().alignLocalsTo(originalOrientations.get(b));
			}
		}
	}*/

	/*public void alignBonesToOriginalAxes() {
		for(AbstractBone b: allBonesInStrandCollection) {
			originalOrientations.put(b, b.localAxes().attachedCopy(false));
		}
	}*/

	/*public void refreshOriginalAxesMap() {
		for(AbstractBone b: allBonesInStrandCollection) {
			originalOrientations.put(b, b.localAxes().attachedCopy(false));
		}
	}*/


	
	public void updateTotalPinDist() {
		totalPinDist = 0d;
		for(Strand s: strands) {
			totalPinDist += s.strandTip.getTip().dist(s.strandTip.getPinPosition());
		}
	}

	public void updateStrandedArmature() {
		strands.clear(); 
		childCollections.clear();
		allBonesInStrandCollection.clear();
		this.boneStrandMap.clear();
		for(StrandedArmature cs : childCollections) {
			cs.updateStrandedArmature();
		}
		generateStrandHierarchy();
	}

	public AbstractBone armatureRootBone(AbstractBone rootBone2) {
		AbstractBone rootBone = rootBone2;
		while(rootBone.parent != null) {
			rootBone = rootBone.parent;
		} 
		return rootBone;
	}


	public class Strand {
		AbstractBone strandTip; 
		AbstractBone strandRoot;
		public boolean rootParentPinned = true;
		StrandedArmature parentStrandCollection; 

		ArrayList<AbstractBone> bones = new ArrayList<AbstractBone>(); //always arranged such that the tip 
		//is the first element, and the root is the last
		HashMap<AbstractBone, Rot> rotationsMap = new HashMap<AbstractBone, Rot>();
		double distToTarget = 0;

		public Strand(StrandedArmature par, AbstractBone tip, AbstractBone root) {
			this.parentStrandCollection = par;
			this.strandTip = tip;
			this.strandRoot = root; 
			if(strandRoot.parent != null && strandRoot.parent.isPinned())
				rootParentPinned = true;
			else rootParentPinned = false; 		
			populateStrandBonesData();
		}

		public ArrayList<StrandedArmature> generateChildStrandCollection() {
			/**This isn't wet code. It's accounting for the fact that there might be a pin
			 * at a multi-bone juncture. Which means each bone emerging from the juncture 
			 * should also be its own strandedArmature.
			 */
			ArrayList<StrandedArmature> result = new ArrayList<StrandedArmature>();
			ArrayList<AbstractBone> childrenWithPinnedDescendents = strandTip.returnChildrenWithPinnedDescendants();
			for(AbstractBone b : childrenWithPinnedDescendents) {
				result.add(new StrandedArmature(this, b));			
			}

			return result;			
		}

		/**
		 * updates the rotationsMap such that it contains the rotation
		 * required to get each bone from its originalOrientation to its currentOrientation;
		 */
		/*
		public void updateRotationsMap() {
			for(AbstractBone b : bones) {
				AbstractAxes orig = parentStrandCollection.originalOrientations.get(b);
				rotationsMap.put(b, new Rot(orig.x().heading(), orig.y().heading(), 
						b.localAxes().x().heading(), b.localAxes().y().heading()));
				
			}
		}*/
		
		public void updateDistToTarget() {
			distToTarget = this.strandTip.getTip().dist(this.strandTip.pinnedTo());
		}
		
		public void revertRotations() {
			for(AbstractBone b : bones) {
				b.localAxes().rotateTo(rotationsMap.get(b).rotation.revert());
			}
		}

		private void populateStrandBonesData() {
			AbstractBone currentBone = strandTip;
			while(currentBone != strandRoot) {
				populateBoneData(currentBone);
				currentBone = currentBone.parent;
			}
			populateBoneData(currentBone);
		}	

		private void populateBoneData(AbstractBone bone) {
			bones.add(bone); 
			if(!this.parentStrandCollection.allBonesInStrandCollection.contains(bone)) {
				this.parentStrandCollection.allBonesInStrandCollection.add(bone);
				this.parentStrandCollection.boneStrandMap.put(bone, new ArrayList<Strand>());

			}
			if(!this.parentStrandCollection.boneStrandMap.get(bone).contains(this)) {
				this.parentStrandCollection.boneStrandMap.get(bone).add(this);
			}
		}		
	}
}
