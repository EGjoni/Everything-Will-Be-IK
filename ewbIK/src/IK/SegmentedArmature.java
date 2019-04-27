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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

import sceneGraph.AbstractAxes;
import sceneGraph.Rot;

/**
 * @author Eron Gjoni
 *
 */
public class SegmentedArmature {
	public AbstractBone segmentRoot; 
	public AbstractBone segmentTip;
	public ArrayList<SegmentedArmature> childSegments = new ArrayList<SegmentedArmature>();
	public ArrayList<SegmentedArmature> pinnedDescendants = new ArrayList<SegmentedArmature>();
	
	HashMap<SegmentedArmature, ArrayList<AbstractBone>> strandMap = new HashMap<SegmentedArmature, ArrayList<AbstractBone>>();
	HashMap<AbstractBone, AbstractAxes> originalOrientations = new HashMap<AbstractBone, AbstractAxes>();
	HashMap<AbstractBone, ArrayList<Rot>> boneRotationMap = new HashMap<AbstractBone, ArrayList<Rot>>();
	WeakHashMap<Rot, SegmentedArmature> rotationStrandMap = new WeakHashMap<Rot, SegmentedArmature>();
	ArrayList<AbstractBone> strandsBoneList = new ArrayList<AbstractBone>();

	protected SegmentedArmature parentSegment = null;
	boolean basePinned = false; 
	boolean tipPinned = false;
	public int distanceToRoot = 0;

	public int chainLength = 0;
	boolean includeInIK = true;

	public SegmentedArmature(AbstractBone rootBone) {
		segmentRoot = armatureRootBone(rootBone);
		generateArmatureSegments();
	}

	public SegmentedArmature(SegmentedArmature inputParentSegment, AbstractBone inputSegmentRoot) {
		this.segmentRoot = inputSegmentRoot;
		this.parentSegment = inputParentSegment;
		this.distanceToRoot = this.parentSegment.distanceToRoot+1;
		generateArmatureSegments();  
	}

	private void generateArmatureSegments() {
		childSegments.clear();
		//pinnedDescendants.clear();
		tipPinned = false;
		if(segmentRoot.parent != null && segmentRoot.parent.isPinned()) 
			this.basePinned = true;
		else 
			this.basePinned = false; 

		AbstractBone tempSegmentTip = this.segmentRoot;
		this.chainLength = -1;
		while(true) {
			this.chainLength++;
			ArrayList<AbstractBone> childrenWithPinnedDescendants = tempSegmentTip.returnChildrenWithPinnedDescendants();
			
			if(childrenWithPinnedDescendants.size() > 1 || (tempSegmentTip.isPinned())) {
				if(tempSegmentTip.isPinned()) tipPinned = true; 
				//else tipPinned = false;
				this.segmentTip = tempSegmentTip; 
				
				for(AbstractBone childBone: childrenWithPinnedDescendants) {
					this.childSegments.add(new SegmentedArmature(this, childBone));
				}
				
				break;
			} else if (childrenWithPinnedDescendants.size() == 1) {
				tempSegmentTip = childrenWithPinnedDescendants.get(0);
			} else {
				this.segmentTip = tempSegmentTip; 
				break;
			}
		}
		updatePinnedDescendants();	    
		generateStrandMaps(); 
	}
	
	public void updateSegmentedArmature() {
		if(this.parentSegment != null) {
			this.parentSegment.updateSegmentedArmature();
		} else { 
			generateArmatureSegments();
		}
	}
	
	public void generateStrandMaps(){
		originalOrientations.clear();
		boneRotationMap.clear();
		strandMap.clear(); 
		strandsBoneList.clear();
		
		for(SegmentedArmature sa : pinnedDescendants) {			
			ArrayList<AbstractBone> strandBoneList = getStrandFromTip(sa.segmentTip);
			strandMap.put(sa, strandBoneList);
			
			for(AbstractBone ab : strandBoneList) {
				AbstractAxes ax = originalOrientations.get(ab);
				if(ax == null) { 
					originalOrientations.put(ab, ab.localAxes().attachedCopy(false));
					boneRotationMap.put(ab, new ArrayList<Rot>());
				}
			}
		}
		
		strandsBoneList.addAll(boneRotationMap.keySet());
	}
	
	public ArrayList<AbstractBone> getStrandFromTip(AbstractBone pinnedBone) {
		ArrayList<AbstractBone> result = new ArrayList<AbstractBone>();

		if(pinnedBone.isPinned()) {
			result.add(pinnedBone);
			AbstractBone currBone = pinnedBone.parent;
			//note to self -- try removing the currbone.parent != null condition
			while(currBone != null && currBone.parent != null) {
				result.add(currBone);
				if(currBone.parent.isPinned()) {
					break;
				}
				currBone = currBone.parent;
			}			
		}

		return result;
	}

	public void updatePinnedDescendants() {    
		pinnedDescendants.clear();
		pinnedDescendants = this.returnSegmentPinnedNodes();
	}

	public ArrayList<SegmentedArmature> returnSegmentPinnedNodes() {
		ArrayList<SegmentedArmature> innerPinnedChains = new ArrayList<SegmentedArmature>();
		if(this.tipPinned) {
			innerPinnedChains.add(this); 
		} else { 
			for(SegmentedArmature childSegment : childSegments) {
				innerPinnedChains.addAll(childSegment.returnSegmentPinnedNodes());
			}
		}
		return innerPinnedChains;
	}


	/**
	 * 
	 * @param chainMember
	 * @return returns the segment chain (pinned or unpinned, doesn't matter) to which the inputBone belongs. 
	 */
	public SegmentedArmature getChainFor(AbstractBone chainMember) {
		AbstractBone candidate = this.segmentTip; 
		while(true) {
			if(candidate == chainMember) return this;
			if(/*candidate == segmentRoot ||*/ candidate.parent == null) {
				break;
			}		
			candidate = candidate.parent;
		}
		SegmentedArmature result = null;
		for(SegmentedArmature children : childSegments) {
			result = children.getChainFor(chainMember); 
			if(result != null) break;
		}
		return result;
	}

	/**
	 * this function travels rootward through the chain hierarchy until it reaches a chain whose base is pinned.
	 * @return returns the first chain encountered with a pinned base. Or, null if it reaches an unpinned armature root.
	 */
	public SegmentedArmature getPinnedRootChainFromHere() {

		SegmentedArmature currentChain = this;
		while(true && currentChain !=null) {
			if(currentChain.basePinned) return currentChain;
			else currentChain = currentChain.parentSegment;
		}

		return currentChain;

	}

	public AbstractBone armatureRootBone(AbstractBone rootBone2) {
		AbstractBone rootBone = rootBone2;
		while(rootBone.parent != null) {
			rootBone = rootBone.parent;
		} 
		return rootBone;
	}

}
