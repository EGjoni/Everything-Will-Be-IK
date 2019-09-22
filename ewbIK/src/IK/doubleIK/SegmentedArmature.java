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
package IK.doubleIK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

import sceneGraph.math.doubleV.AbstractAxes;
import sceneGraph.math.doubleV.Basis;
import sceneGraph.math.doubleV.MRotation;
import sceneGraph.math.doubleV.MathUtils;
import sceneGraph.math.doubleV.Matrix3d;
import sceneGraph.math.doubleV.Quaternion;
import sceneGraph.math.doubleV.Rot;
import sceneGraph.math.doubleV.SGVec_3d;
import sceneGraph.math.doubleV.sgRayd;

/**
 * @author Eron Gjoni
 *
 */
public class SegmentedArmature {
	public AbstractBone segmentRoot; 
	public AbstractBone segmentTip;
	public AbstractAxes debugTipAxes;
	public AbstractAxes debugTargetAxes;
	public ArrayList<SegmentedArmature> childSegments = new ArrayList<SegmentedArmature>();
	public ArrayList<SegmentedArmature> pinnedDescendants = new ArrayList<SegmentedArmature>();

	HashMap<SegmentedArmature, ArrayList<AbstractBone>> strandMap = new HashMap<SegmentedArmature, ArrayList<AbstractBone>>();
	HashMap<AbstractBone, AbstractAxes> simulatedLocalAxes = new HashMap<AbstractBone, AbstractAxes>();
	HashMap<AbstractBone, AbstractAxes> simulatedConstraintAxes = new HashMap<AbstractBone, AbstractAxes>();
	public HashMap<AbstractBone, ArrayList<Rot>> boneRotationMap = new HashMap<AbstractBone, ArrayList<Rot>>();
	WeakHashMap<Rot, SegmentedArmature> rotationStrandMap = new WeakHashMap<Rot, SegmentedArmature>();
	ArrayList<AbstractBone> strandsBoneList = new ArrayList<AbstractBone>();

	private SegmentedArmature parentSegment = null;
	private boolean basePinned = false; 
	private boolean tipPinned = false;
	private boolean processed  = false; 
	public int distanceToRoot = 0;

	public int chainLength = 0;
	boolean includeInIK = true;

	private int subTargetCounts = 0; 

	public SegmentedArmature(AbstractBone rootBone) {
		segmentRoot = armatureRootBone(rootBone);
		generateArmatureSegments();
		ensureAxesHeirarchy();
	}

	public SegmentedArmature(SegmentedArmature inputParentSegment, AbstractBone inputSegmentRoot) {
		this.segmentRoot = inputSegmentRoot;
		this.setParentSegment(inputParentSegment);
		this.distanceToRoot = this.getParentSegment().distanceToRoot+1;
		generateArmatureSegments();  
	}

	private void generateArmatureSegments() {
		childSegments.clear();
		//pinnedDescendants.clear();
		setTipPinned(false);
		if(segmentRoot.getParent() != null && segmentRoot.getParent().isPinned()) 
			this.setBasePinned(true);
		else 
			this.setBasePinned(false); 

		AbstractBone tempSegmentTip = this.segmentRoot;
		this.chainLength = -1;
		while(true) {
			this.chainLength++;
			ArrayList<AbstractBone> childrenWithPinnedDescendants = tempSegmentTip.returnChildrenWithPinnedDescendants();

			if(childrenWithPinnedDescendants.size() > 1 || (tempSegmentTip.isPinned())) {
				if(tempSegmentTip.isPinned()) setTipPinned(true); 
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
		updateTotalSubTargets();
	}

	/**
	 * calculates the total number of bases the immediate effectors emanating from this 
	 * segment reach for (based on modecode set in the IKPin) 
	 */
	private void updateTotalSubTargets() {
		subTargetCounts = 0;
		for(int i=0; i< pinnedDescendants.size(); i++) {
			SegmentedArmature s = pinnedDescendants.get(i);
			AbstractIKPin pin = s.segmentTip.getIKPin(); 
			int pinTargets = pin.getSubtargetCount();
			subTargetCounts += pinTargets; 
		}
	}

	/**
	 * Should only be called from the rootmost strand. 
	 * ensures the proper axes parent relationships
	 *for simulatedAxes throughout the SegmentedArmature .  
	 */
	private void ensureAxesHeirarchy() {
		SegmentedArmature rootStrand = this; 
		while(rootStrand.parentSegment != null) {
			rootStrand = rootStrand.parentSegment;
		}
		recursivelyEnsureAxesHeirarchyFor(rootStrand.segmentRoot, rootStrand.segmentRoot.parentArmature.localAxes());		
	}

	private void recursivelyEnsureAxesHeirarchyFor(AbstractBone b, AbstractAxes parentTo) {
		SegmentedArmature chain = getChainFor(b); 
		if(chain != null) {
			AbstractAxes simLocalAxes =  chain.simulatedLocalAxes.get(b);
			AbstractAxes simConstraintAxes = chain.simulatedConstraintAxes.get(b);
			simLocalAxes.setParent(parentTo);
			simConstraintAxes.setParent(parentTo);
			for(AbstractBone c : b.getChildren()) {
				chain.recursivelyEnsureAxesHeirarchyFor(c, simLocalAxes);
			}
		}
	}

	public void updateSegmentedArmature() {
		if(this.getParentSegment() != null) {
			this.getParentSegment().updateSegmentedArmature();
		} else { 
			generateArmatureSegments();
			ensureAxesHeirarchy();
		}
	}

	public void generateStrandMaps(){
		for(AbstractAxes a : simulatedConstraintAxes.values()) {
			a.emancipate();
		}
		for(AbstractAxes a : simulatedLocalAxes.values()) {
			a.emancipate();
		}

		simulatedLocalAxes.clear();
		simulatedConstraintAxes.clear();
		boneRotationMap.clear();
		strandMap.clear(); 
		strandsBoneList.clear();

		AbstractBone currentBone = segmentTip; 
		AbstractBone stopOn = segmentRoot;
		while(currentBone != null) {
			AbstractAxes ax = simulatedLocalAxes.get(currentBone);
			if(ax == null) { 
				simulatedLocalAxes.put(currentBone, currentBone.localAxes().getGlobalCopy());
				simulatedConstraintAxes.put(currentBone, currentBone.getMajorRotationAxes().getGlobalCopy());
				boneRotationMap.put(currentBone, new ArrayList<Rot>());
				strandsBoneList.add(0, currentBone);
			}

			if(currentBone == stopOn) break;
			currentBone = currentBone.getParent();

		}

		//strandsBoneList.addAll(boneRotationMap.keySet());
	}

	public ArrayList<AbstractBone> getStrandFromTip(AbstractBone pinnedBone) {
		ArrayList<AbstractBone> result = new ArrayList<AbstractBone>();

		if(pinnedBone.isPinned()) {
			result.add(pinnedBone);
			AbstractBone currBone = pinnedBone.getParent();
			//note to self -- try removing the currbone.parent != null condition
			while(currBone != null && currBone.getParent() != null) {
				result.add(currBone);
				if(currBone.getParent().isPinned()) {
					break;
				}
				currBone = currBone.getParent();
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
		if(this.isTipPinned()) {
			innerPinnedChains.add(this); 
		} else { 
			for(SegmentedArmature childSegment : childSegments) {				
				innerPinnedChains.addAll(childSegment.returnSegmentPinnedNodes());
			}
		}
		return innerPinnedChains;
	}


	public double getManualMSD(SGVec_3d[] locTips, SGVec_3d[] locTargets, double[] weights ) {
		double manualRMSD = 0d; 
		double wsum = 0d;
		for(int i=0; i<locTargets.length; i++) {
			double xd = locTargets[i].x - locTips[i].x;
			double yd = locTargets[i].y - locTips[i].y;
			double zd = locTargets[i].z - locTips[i].z;			
			double magsq = weights[i]* (xd*xd + yd*yd + zd*zd);
			manualRMSD += magsq;
			wsum += weights[i];
		}	
		manualRMSD /= wsum;//(double) locTargets.length; 
		//manualRMSD = MathUtils.sqrt(manualRMSD);

		return manualRMSD;
	}

	
	/**
	 * 
	 * @param forBone
	 * @param dampening
	 * @param translate set to true if you wish to allow translation in addition to rotation of the bone (should only be used for unpinned root bones)
	 * @param stabilizationPasses If you know that your armature isn't likely to succumb to instability in unsolvable configurations, leave this value set to 0. 
	 * If you value stability in extreme situations more than computational speed, then increase this value. A value of 1 will be completely stable, and just as fast 
	 * as a value of 0, however, it might result in small levels of robotic looking jerk. The higher the value, the less jerk there will be (but at potentially significant computation cost).
	 */
	public void updateOptimalRotationToPinnedDescendants(
			AbstractBone forBone, 
			double dampening,
			boolean translate,
			int stabilizationPasses) {

		int targetCount = (subTargetCounts*2)-this.pinnedDescendants.size();
		SGVec_3d[] localizedTargetHeadings = new SGVec_3d[targetCount]; 
		SGVec_3d [] localizedTipHeadings = new SGVec_3d[targetCount]; 
		double[] weights = new double[targetCount];
		AbstractAxes thisBoneAxes = simulatedLocalAxes.get(forBone);
		AbstractAxes bestOrientationAxes = thisBoneAxes.getGlobalCopy();
		if(forBone.getParent() == null || targetCount == 1) 
			stabilizationPasses = 0;

		
		updateTargetHeadings(localizedTargetHeadings, weights, thisBoneAxes);
		upateTipHeadings(localizedTipHeadings, thisBoneAxes);		


		double bestRMSD = 0d; 
		QCP qcpConvergenceCheck = new QCP(MathUtils.DOUBLE_ROUNDING_ERROR, MathUtils.DOUBLE_ROUNDING_ERROR);
		double newRMSD = 999999d;
		
		if(stabilizationPasses > 0)
			bestRMSD = getManualMSD(localizedTipHeadings, localizedTargetHeadings, weights);
		for(int i=0; i<stabilizationPasses + 1; i++) {
			updateOptimalRotationToPinnedDescendants(forBone, thisBoneAxes, dampening, translate, localizedTipHeadings, localizedTargetHeadings, weights, qcpConvergenceCheck);	
			if(stabilizationPasses > 0) {
				
				upateTipHeadings(localizedTipHeadings, thisBoneAxes);		
				newRMSD = getManualMSD(localizedTipHeadings, localizedTargetHeadings, weights);
				
				if(bestRMSD > newRMSD) {
					bestOrientationAxes.alignGlobalsTo(thisBoneAxes);
					bestRMSD = newRMSD;					
					break;				
				}
			}
		}
		if(stabilizationPasses > 0) { 
			//System.out.println("retried " + (int)(((tryDampen -1d) /4d)));
			thisBoneAxes.alignGlobalsTo(bestOrientationAxes);
			thisBoneAxes.markDirty(); 
		}

	}

	private void updateOptimalRotationToPinnedDescendants(
			AbstractBone forBone, 
			AbstractAxes thisBoneAxes,
			double dampening,
			boolean translate,
			SGVec_3d[] localizedTipHeadings,
			SGVec_3d[] localizedTargetHeadings, 
			double[] weights,
			QCP qcpOrientationAligner) {

		qcpOrientationAligner.setMaxIterations(5);
		Rot qcpRot = qcpOrientationAligner.weightedSuperpose(localizedTipHeadings, localizedTargetHeadings, weights, translate);
		SGVec_3d translateBy = qcpOrientationAligner.getTranslation();
		double predamp = 1d-forBone.getStiffness();
		dampening = forBone.getParent() == null ? MathUtils.PI : predamp * dampening;
		Rot dampened = new Rot(qcpRot.getAxis(),  MathUtils.clamp(qcpRot.getAngle(), -dampening, dampening));
		thisBoneAxes.rotateBy(dampened);
		AbstractAxes simAxes = simulatedConstraintAxes.get(forBone);
		forBone.setAxesToSnapped(thisBoneAxes,simAxes);
		simAxes.translateByGlobal(translateBy);
		thisBoneAxes.translateByGlobal(translateBy); 
			
	}

	public double getRMSD(
			SGVec_3d[] localizedTargetHeadings, 
			SGVec_3d[] localizedTipHeadings, 
			double[] weights, 
			AbstractAxes thisBoneAxes, 
			boolean translate,
			QCP qcpObj) {
		if(translate) 
			updateTargetHeadings(localizedTargetHeadings, weights, thisBoneAxes);
		upateTipHeadings(localizedTipHeadings, thisBoneAxes);

		qcpObj.setMaxIterations(5);
		qcpObj.set(localizedTipHeadings, localizedTargetHeadings, weights, translate);

		double snappedRMSD = qcpObj.getRmsd();
		return snappedRMSD;		
	}

	public void updateTargetHeadings(SGVec_3d[] localizedTargetHeadings, double[] weights, AbstractAxes thisBoneAxes) {		
		int subTargetsSoFar = 0;
		for(int i=0; i< pinnedDescendants.size(); i++) {
			SegmentedArmature s = pinnedDescendants.get(i);
			AbstractIKPin pin = s.segmentTip.getIKPin();
			AbstractAxes targetAxes = s.segmentTip.getPinnedAxes();
			targetAxes.updateGlobal();
			localizedTargetHeadings[subTargetsSoFar] = SGVec_3d.sub(targetAxes.origin_(), thisBoneAxes.origin_());
			weights[subTargetsSoFar] = pin.getPinWeight();
			subTargetsSoFar++;
			int modeCode = pin.getModeCode();

			if((modeCode & 1<<0) != 0) {
				sgRayd xTarget = targetAxes.x_norm_();
				localizedTargetHeadings[subTargetsSoFar] = SGVec_3d.sub(xTarget.p2(), thisBoneAxes.origin_());
				localizedTargetHeadings[subTargetsSoFar+1] = SGVec_3d.sub(xTarget.getRayScaledBy(-1d).p2(), thisBoneAxes.origin_());
				weights[subTargetsSoFar] = pin.getPinWeight() * pin.getXPriority();
				weights[subTargetsSoFar+1] = weights[subTargetsSoFar];
				subTargetsSoFar+=2;
			}
			if((modeCode & 1<<1) != 0) {
				sgRayd yTarget = targetAxes.y_norm_();
				localizedTargetHeadings[subTargetsSoFar] = SGVec_3d.sub(yTarget.p2(), thisBoneAxes.origin_());
				localizedTargetHeadings[subTargetsSoFar+1] = SGVec_3d.sub(yTarget.getRayScaledBy(-1d).p2(), thisBoneAxes.origin_());
				weights[subTargetsSoFar] = pin.getPinWeight() * pin.getYPriority();
				weights[subTargetsSoFar+1] = weights[subTargetsSoFar];
				subTargetsSoFar+=2;
			}
			if((modeCode & 1<<2) != 0) {
				sgRayd zTarget = targetAxes.z_norm_();
				localizedTargetHeadings[subTargetsSoFar] = SGVec_3d.sub(zTarget.p2(), thisBoneAxes.origin_());	
				localizedTargetHeadings[subTargetsSoFar+1] = SGVec_3d.sub(zTarget.getRayScaledBy(-1d).p2(), thisBoneAxes.origin_());
				weights[subTargetsSoFar] = pin.getPinWeight() * pin.getZPriority();
				weights[subTargetsSoFar+1] = weights[subTargetsSoFar];
				subTargetsSoFar+=2;
			}
		}
	}

	public void upateTipHeadings(SGVec_3d[] localizedTipHeadings, AbstractAxes thisBoneAxes) {
		int subTargetsSoFar = 0;
		for(int i=0; i< pinnedDescendants.size(); i++) {
			SegmentedArmature s = pinnedDescendants.get(i);
			AbstractAxes tipAxes = s.simulatedLocalAxes.get(s.segmentTip);
			tipAxes.updateGlobal();

			localizedTipHeadings[subTargetsSoFar] =  SGVec_3d.sub(tipAxes.origin_(), thisBoneAxes.origin_());
			subTargetsSoFar++;

			AbstractIKPin pin = s.segmentTip.getIKPin();
			int modeCode = pin.getModeCode();

			if((modeCode & 1<<0) != 0) {
				sgRayd xTip = tipAxes.x_norm_();
				localizedTipHeadings[subTargetsSoFar] = SGVec_3d.sub(xTip.p2(), thisBoneAxes.origin_());
				localizedTipHeadings[subTargetsSoFar+1] = SGVec_3d.sub(xTip.getRayScaledBy(-1d).p2(), thisBoneAxes.origin_());
				subTargetsSoFar+=2;
			}
			if((modeCode & 1<<1) != 0) {
				sgRayd yTip = tipAxes.y_norm_();
				localizedTipHeadings[subTargetsSoFar] = SGVec_3d.sub(yTip.p2(), thisBoneAxes.origin_());
				localizedTipHeadings[subTargetsSoFar+1] = SGVec_3d.sub(yTip.getRayScaledBy(-1d).p2(), thisBoneAxes.origin_());
				subTargetsSoFar+=2;
			}
			if((modeCode & 1<<2) != 0) {
				sgRayd zTip = tipAxes.z_norm_();
				localizedTipHeadings[subTargetsSoFar] = SGVec_3d.sub(zTip.p2(), thisBoneAxes.origin_());
				localizedTipHeadings[subTargetsSoFar+1] = SGVec_3d.sub(zTip.getRayScaledBy(-1d).p2(), thisBoneAxes.origin_());
				subTargetsSoFar+=2;
			}
		}
	}

	/**
	 * 
	 * @param chainMember
	 * @return returns the segment chain (pinned or unpinned, doesn't matter) to which the inputBone belongs. 
	 */
	public SegmentedArmature getChainFor(AbstractBone chainMember) {
		//AbstractBone candidate = this.segmentTip; 
		SegmentedArmature result = null; 
		if(this.strandsBoneList.contains(chainMember)) 
			return this;
		if(this.parentSegment != null) 
			result = this.parentSegment.getAncestorSegmentContaining(chainMember);
		if(result == null)
			result = getChildSegmentContaining(chainMember);
		return result;
	}	
	public SegmentedArmature getChildSegmentContaining(AbstractBone b) {
		if(strandsBoneList.contains(b)) { 
			return this;
		} else {
			for(SegmentedArmature s : childSegments) {
				SegmentedArmature childContaining = s.getChildSegmentContaining(b);
				if(childContaining != null) 
					return childContaining;
			}
		}
		return null;
	}

	public SegmentedArmature getAncestorSegmentContaining(AbstractBone b) {
		if(strandsBoneList.contains(b)) 
			return this;
		else if(this.parentSegment != null) 
			return this.parentSegment.getAncestorSegmentContaining(b);
		else 
			return null;
	}

	/**
	 * this function travels rootward through the chain hierarchy until it reaches a chain whose base is pinned.
	 * @return returns the first chain encountered with a pinned base. Or, null if it reaches an unpinned armature root.
	 */
	public SegmentedArmature getPinnedRootChainFromHere() {

		SegmentedArmature currentChain = this;
		while(true && currentChain !=null) {
			if(currentChain.isBasePinned()) return currentChain;
			else currentChain = currentChain.getParentSegment();
		}

		return currentChain;

	}

	public AbstractBone armatureRootBone(AbstractBone rootBone2) {
		AbstractBone rootBone = rootBone2;
		while(rootBone.getParent() != null) {
			rootBone = rootBone.getParent();
		} 
		return rootBone;
	}

	public boolean isTipPinned() {
		return tipPinned;
	}

	public void setTipPinned(boolean tipPinned) {
		this.tipPinned = tipPinned;
	}

	public boolean isBasePinned() {
		return basePinned;
	}

	public void setBasePinned(boolean basePinned) {
		this.basePinned = basePinned;
	}

	public SegmentedArmature getParentSegment() {
		return parentSegment;
	}

	public void setParentSegment(SegmentedArmature parentSegment) {
		this.parentSegment = parentSegment;
	}

	/**
	 * aligns all simulation axes from this root of this chain  up until the pinned tips
	 * of any child chains  with the constraint an local axes of their corresponding bone. 
	 */

	public void alignSimulationAxesToBones() {
		if(!this.isBasePinned() && this.getParentSegment() != null) {
			this.getParentSegment().alignSimulationAxesToBones();
		} else {
			recursivelyAlignSimAxesOutwardFrom(segmentRoot);
		}
	}

	public void recursivelyAlignSimAxesOutwardFrom(AbstractBone b) {
		SegmentedArmature bChain = getChildSegmentContaining(b);
		if(bChain != null) {
			AbstractAxes bAxes = bChain.simulatedLocalAxes.get(b); 
			AbstractAxes cAxes = bChain.simulatedConstraintAxes.get(b);
			bAxes.alignGlobalsTo(b.localAxes());
			bAxes.markDirty(); bAxes.updateGlobal();			
			cAxes.alignGlobalsTo(b.getMajorRotationAxes());
			cAxes.markDirty(); cAxes.updateGlobal();
			for(AbstractBone bc: b.getChildren()) {
				bChain.recursivelyAlignSimAxesOutwardFrom(bc);	
			}			
		}
	}



	public void recursivelyAlignSimAxesRootwardFrom(AbstractBone b) {
		if(b!= null) {
			SegmentedArmature bChain = getChainFor(b);			
			AbstractBone parent = b.getParent(); 
			AbstractAxes bAxes = bChain.simulatedLocalAxes.get(b); 
			AbstractAxes cAxes = bChain.simulatedConstraintAxes.get(b);
			bChain.simAligned = true;
			bAxes.alignGlobalsTo(b.localAxes());
			bAxes.markDirty(); bAxes.updateGlobal();			
			cAxes.alignGlobalsTo(b.getMajorRotationAxes());
			cAxes.markDirty(); cAxes.updateGlobal();			
			if(parent != null) {
				SegmentedArmature bParentChain = getChainFor(parent);
				if(bParentChain != bChain && bParentChain.simAligned) {
					return; // the parent chain doesn't need aligning, it is safe to just update these simAxes
				}
				recursivelyAlignSimAxesRootwardFrom(parent); 
			}			
			if(bAxes == null) {
				int debug = 0;
			}
			if(Double.isNaN(bAxes.globalMBasis.rotation.getAngle())) {
				int debug = 0;
			}
		}	
	}

	/**aligns this bone and all relevant childBones to their coresponding simulatedAxes (if any) in the SegmentedArmature
	 * @param b bone to start from
	 */
	public void recursivelyAlignBonesToSimAxesFrom(AbstractBone b) {
		SegmentedArmature chain =getChainFor(b);		
		if(chain != null) {			
			AbstractAxes simulatedLocalAxes = chain.simulatedLocalAxes.get(b);
			if(b.getParent() == null) {
				b.localAxes().alignGlobalsTo(simulatedLocalAxes);
			} else {
				b.localAxes().localMBasis.rotateTo(simulatedLocalAxes.localMBasis.rotation);
				b.localAxes().markDirty(); b.localAxes().updateGlobal();
			}
			for(AbstractBone bc: b.getChildren()) {
				recursivelyAlignBonesToSimAxesFrom(bc);	
			}			
			chain.simAligned = false;
			chain.processed = false;
		} else {
			int debug = 0;
		}

	}

	/**
	 * populates the given arraylist with the rootmost unprocessed chains of this segmented armature 
	 * and its descenedants up until their pinned tips. 
	 * @param segments
	 */
	public void getRootMostUnprocessedChains(ArrayList<SegmentedArmature> segments) {
		if(!this.processed) {
			segments.add(this);
		} else {
			if(this.tipPinned) 
				return; 
			for(SegmentedArmature c: childSegments) {
				c.getRootMostUnprocessedChains(segments);
			}
		}
	}



	public void setProcessed(boolean b) {
		this.processed = b;
		if(processed == false) {
			for(SegmentedArmature c : childSegments) {
				c.setProcessed(false);
			}
		}
	}



	private boolean simAligned = false;



}
