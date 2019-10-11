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

import IK.PerfTimer;
import math.doubleV.AbstractAxes;
import math.doubleV.MathUtils;
import math.doubleV.QCP;
import math.doubleV.Rot;
import math.doubleV.SGVec_3d;
import math.doubleV.Vec3d;
import math.doubleV.sgRayd;
import asj.LoadManager;
import asj.SaveManager;
import asj.Saveable;
import asj.data.JSONObject;

/**
 * @author Eron Gjoni
 *
 */
public class SegmentedArmature {
	public AbstractBone segmentRoot; 
	public AbstractBone segmentTip;

	//private int subTargetCounts = 0; 
	public ArrayList<SegmentedArmature> childSegments = new ArrayList<SegmentedArmature>();
	public ArrayList<SegmentedArmature> pinnedDescendants = new ArrayList<SegmentedArmature>();
	WorkingBone[] pinnedBones;	

	HashMap<AbstractBone, WorkingBone> simulatedBones = new HashMap<>();
	ArrayList<AbstractBone> segmentBoneList = new ArrayList<AbstractBone>();

	private SegmentedArmature parentSegment = null;
	private boolean basePinned = false; 
	private boolean tipPinned = false;
	private boolean processed  = false; 
	public int distanceToRoot = 0;

	public int chainLength = 0;
	boolean includeInIK = true;
	int pinDepth = 1; 



	public AbstractAxes debugTipAxes;
	public AbstractAxes debugTargetAxes;

	SGVec_3d[] localizedTargetHeadings; 
	SGVec_3d [] localizedTipHeadings;
	double[] weights;

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
		generateSegmentMaps(); 
	}


	/**
	 * calculates the total number of bases the immediate effectors emanating from this 
	 * segment reach for (based on modecode set in the IKPin) 
	 */
	/*private void updateTotalSubTargets() {
		subTargetCounts = 0;
		for(int i=0; i< pinnedDescendants.size(); i++) {
			SegmentedArmature s = pinnedDescendants.get(i);
			AbstractIKPin pin = s.segmentTip.getIKPin(); 
			int pinTargets = pin.getSubtargetCount();
			subTargetCounts += pinTargets; 
		}
	}*/

	static void recursivelyCreateHeadingArraysFor(SegmentedArmature s) {
		s.createHeadingArrays();
		for(SegmentedArmature c : s.childSegments) {
			recursivelyCreateHeadingArraysFor(c);
		}
	}


	void createHeadingArrays( ) {
		ArrayList<ArrayList<Double>> penaltyArray = new ArrayList<ArrayList<Double>>();
		ArrayList<WorkingBone> pinSequence = new ArrayList<>();		
		recursivelyCreatePenaltyArray(this, penaltyArray, pinSequence, 1d);
		pinnedBones = new WorkingBone[pinSequence.size()];	
		int totalHeadings = 0;
		for(ArrayList<Double> a : penaltyArray) {			
			totalHeadings += a.size();
		}
		for(int i =0; i< pinSequence.size(); i++) {
			pinnedBones[i] = pinSequence.get(i);
		}
		localizedTargetHeadings = new SGVec_3d[totalHeadings]; 
		localizedTipHeadings = new SGVec_3d[totalHeadings]; 
		weights = new double[totalHeadings];
		int currentHeading = 0;
		for(ArrayList<Double> a : penaltyArray) {
			for(Double ad : a) {
				weights[currentHeading] = ad;
				localizedTargetHeadings[currentHeading] = new SGVec_3d();
				localizedTipHeadings[currentHeading] = new SGVec_3d();
				currentHeading++;
			}
		}	
	}

	void recursivelyCreatePenaltyArray(SegmentedArmature from, ArrayList<ArrayList<Double>> weightArray, ArrayList<WorkingBone> pinSequence, double currentFalloff) {
		if(currentFalloff == 0) {
			return;
		} else {
			AbstractIKPin pin = from.segmentTip.getIKPin(); 
			if(pin != null) {
				ArrayList<Double> innerWeightArray = new ArrayList<Double>();
				weightArray.add(innerWeightArray);
				byte modeCode = pin.getModeCode();
				innerWeightArray.add(pin.getPinWeight()*currentFalloff);
				double maxPinWeight = 0d;
				if((modeCode & AbstractIKPin.XDir) != 0)
					maxPinWeight = MathUtils.max(maxPinWeight, pin.getXPriority());					
				if((modeCode & AbstractIKPin.YDir) != 0) 
					maxPinWeight = MathUtils.max(maxPinWeight, pin.getYPriority());				
				if((modeCode & AbstractIKPin.ZDir) != 0)
					maxPinWeight = MathUtils.max(maxPinWeight, pin.getZPriority());

				if(maxPinWeight == 0d) maxPinWeight = 1d;

				maxPinWeight = 1d;

				if((modeCode & AbstractIKPin.XDir) != 0) {
					double subTargetWeight = pin.getPinWeight() * (pin.getXPriority()/maxPinWeight)*currentFalloff;
					innerWeightArray.add(subTargetWeight);
					innerWeightArray.add(subTargetWeight);
				}
				if((modeCode & AbstractIKPin.YDir) != 0) {
					double subTargetWeight = pin.getPinWeight() * (pin.getYPriority()/maxPinWeight)*currentFalloff;
					innerWeightArray.add(subTargetWeight);
					innerWeightArray.add(subTargetWeight);
				}
				if((modeCode & AbstractIKPin.ZDir) != 0) {
					double subTargetWeight = pin.getPinWeight() * (pin.getZPriority()/maxPinWeight)*currentFalloff;
					innerWeightArray.add(subTargetWeight);
					innerWeightArray.add(subTargetWeight);
				}
				pinSequence.add(pin.forBone().parentArmature.boneSegmentMap.get(pin.forBone()).simulatedBones.get(pin.forBone())); 
			}
			double thisFalloff = pin == null ? 1d : pin.getDepthFalloff();
			for(SegmentedArmature s : from.childSegments) { 				
				recursivelyCreatePenaltyArray(s, weightArray, pinSequence, currentFalloff*thisFalloff);
			}

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
			WorkingBone sb = chain.simulatedBones.get(b);
			sb.simLocalAxes.setParent(parentTo);
			sb.simConstraintAxes.setParent(parentTo);
			for(AbstractBone c : b.getChildren()) {
				chain.recursivelyEnsureAxesHeirarchyFor(c, sb.simLocalAxes);
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

	public void generateSegmentMaps(){
		for(WorkingBone b: simulatedBones.values()) {
			b.simConstraintAxes.emancipate();
			b.simLocalAxes.emancipate();
		}
		simulatedBones.clear();
		segmentBoneList.clear();

		AbstractBone currentBone = segmentTip; 
		AbstractBone stopOn = segmentRoot;
		while(currentBone != null) {
			WorkingBone sb = simulatedBones.get(currentBone);
			if(sb == null) { 
				simulatedBones.put(currentBone, new WorkingBone(currentBone));
				segmentBoneList.add(0, currentBone);
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
		ArrayList<SegmentedArmature> innerPinnedChains = new ArrayList<>();
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

		WorkingBone sb = simulatedBones.get(forBone);
		AbstractAxes thisBoneAxes = sb.simLocalAxes;
		thisBoneAxes.updateGlobal();

		Rot bestOrientation = new Rot(thisBoneAxes.getGlobalMBasis().rotation.rotation);
		double newDampening = -1; 
		if(forBone.getParent() == null || localizedTargetHeadings.length == 1) 
			stabilizationPasses = 0;
		if(translate == true) {
			newDampening = Math.PI;
		}

		if(sb.springy) {
			if(dampening != -1) {
				double returnfullness = ((AbstractKusudama)forBone.getConstraint()).getReturnfullness();
				double cosHalfAngle = Math.cos(forBone.getStiffness()*dampening*0.5d*returnfullness);		
				forBone.setAxesToReturnfulled(thisBoneAxes, sb.simConstraintAxes, cosHalfAngle);
			} else {
				forBone.setAxesToReturnfulled(thisBoneAxes, sb.simConstraintAxes, sb.cosHalfReturnFullnessDampened);
			}
		}


		updateTargetHeadings(localizedTargetHeadings, weights, thisBoneAxes);
		upateTipHeadings(localizedTipHeadings, thisBoneAxes);		

		double bestRMSD = 0d; 
		QCP qcpConvergenceCheck = new QCP(MathUtils.DOUBLE_ROUNDING_ERROR, MathUtils.DOUBLE_ROUNDING_ERROR);
		double newRMSD = 999999d;

		if(stabilizationPasses > 0)
			bestRMSD = getManualMSD(localizedTipHeadings, localizedTargetHeadings, weights);


		for(int i=0; i<stabilizationPasses + 1; i++) {
			updateOptimalRotationToPinnedDescendants(
					sb, newDampening, 
					translate, 
					localizedTipHeadings, 
					localizedTargetHeadings, 
					weights, 
					qcpConvergenceCheck);	
			if(stabilizationPasses > 0) {
				//newDampening = dampening == -1 ? sb.forBone.parentArmature.dampening 
				upateTipHeadings(localizedTipHeadings, thisBoneAxes);		
				newRMSD = getManualMSD(localizedTipHeadings, localizedTargetHeadings, weights);

				if(bestRMSD >= newRMSD) {
					bestOrientation.set(thisBoneAxes.getGlobalMBasis().rotation.rotation);
					bestRMSD = newRMSD;		
					//if(i>0) 
					//System.out.println("inner retired after " + i + " attempts.");
					break;				
				}
			} else {
				//System.out.println("retired after " + i + " attempts.");
				break;
			}
		}
		if(stabilizationPasses > 0) { 
			//System.out.println("retried " + (int)(((tryDampen -1d) /4d)));
			thisBoneAxes.setGlobalOrientationTo(bestOrientation);
			thisBoneAxes.markDirty(); 
		}
	}

	//AbstractAxes tempAxes = null;

	private void updateOptimalRotationToPinnedDescendants( 
			WorkingBone sb,
			double dampening,
			boolean translate,
			SGVec_3d[] localizedTipHeadings,
			SGVec_3d[] localizedTargetHeadings, 
			double[] weights,
			QCP qcpOrientationAligner) {

		qcpOrientationAligner.setMaxIterations(10);		
		Rot qcpRot =  qcpOrientationAligner.weightedSuperpose(localizedTipHeadings, localizedTargetHeadings, weights, translate);

		SGVec_3d translateBy = qcpOrientationAligner.getTranslation();
		double boneDamp = sb.cosHalfDampen; 
		if(dampening != -1) {
			boneDamp = dampening;
			qcpRot.rotation.clampToAngle(boneDamp);
		}else {
			qcpRot.rotation.clampToQuadranceAngle(boneDamp);
		}	
		sb.simLocalAxes.rotateBy(qcpRot);
		sb.forBone.setAxesToSnapped(sb.simLocalAxes, sb.simConstraintAxes, boneDamp);
		sb.simLocalAxes.translateByGlobal(translateBy);
		sb.simConstraintAxes.translateByGlobal(translateBy);
	}


	public void updateTargetHeadings(Vec3d<?>[] localizedTargetHeadings, double[] weights, AbstractAxes thisBoneAxes) {		

		int hdx = 0;
		for(int i =0; i<pinnedBones.length; i++) {
			WorkingBone sb = pinnedBones[i];
			AbstractIKPin pin = sb.forBone.getIKPin();
			AbstractAxes targetAxes = pin.forBone.getPinnedAxes();
			targetAxes.updateGlobal();
			Vec3d<?> origin  = thisBoneAxes.origin_();
			localizedTargetHeadings[hdx].set(targetAxes.origin_()).sub( origin);
			byte  modeCode = pin.getModeCode();
			hdx++;

			if((modeCode & AbstractIKPin.XDir) != 0) {
				sgRayd xTarget = targetAxes.x_();
				localizedTargetHeadings[hdx].set(xTarget.p2()).sub(origin);
				xTarget.setToInvertedTip(localizedTargetHeadings[hdx+1]).sub(origin);
				hdx +=2;
			}
			if((modeCode & AbstractIKPin.YDir) != 0) {
				sgRayd yTarget = targetAxes.y_();
				localizedTargetHeadings[hdx] =  Vec3d.sub(yTarget.p2(), origin);
				yTarget.setToInvertedTip(localizedTargetHeadings[hdx+1]).sub(origin);
				hdx +=2;
			}
			if((modeCode & AbstractIKPin.ZDir) != 0) {
				sgRayd zTarget = targetAxes.z_();
				localizedTargetHeadings[hdx] =  Vec3d.sub(zTarget.p2(), origin);
				zTarget.setToInvertedTip(localizedTargetHeadings[hdx+1]).sub(origin);
				hdx +=2;
			}			
		}		

	}

	public void upateTipHeadings(Vec3d<?>[] localizedTipHeadings, AbstractAxes thisBoneAxes) {
		int hdx = 0;
		for(int i =0; i<pinnedBones.length; i++) {
			WorkingBone sb = pinnedBones[i];
			AbstractIKPin pin = sb.forBone.getIKPin();
			AbstractAxes tipAxes = sb.simLocalAxes;
			tipAxes.updateGlobal();
			Vec3d<?> origin  = thisBoneAxes.origin_();
			localizedTipHeadings[hdx].set(tipAxes.origin_()).sub( origin);
			byte  modeCode = pin.getModeCode();
			hdx++;

			if((modeCode & AbstractIKPin.XDir) != 0) {
				sgRayd xTip = tipAxes.x_();
				localizedTipHeadings[hdx].set(xTip.p2()).sub(origin);
				xTip.setToInvertedTip(localizedTipHeadings[hdx+1]).sub(origin);
				hdx+=2;
			}
			if((modeCode & AbstractIKPin.YDir) != 0) {
				sgRayd yTip = tipAxes.y_();
				localizedTipHeadings[hdx].set(yTip.p2()).sub(origin);
				yTip.setToInvertedTip(localizedTipHeadings[hdx+1]).sub(origin);
				hdx+=2;
			}
			if((modeCode & AbstractIKPin.ZDir) != 0) {
				sgRayd zTip = tipAxes.z_();
				localizedTipHeadings[hdx].set(zTip.p2()).sub(origin);
				zTip.setToInvertedTip(localizedTipHeadings[hdx+1]).sub(origin);
				hdx+=2;
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
		if(this.segmentBoneList.contains(chainMember)) 
			return this;
		if(this.parentSegment != null) 
			result = this.parentSegment.getAncestorSegmentContaining(chainMember);
		if(result == null)
			result = getChildSegmentContaining(chainMember);
		return result;
	}	
	public SegmentedArmature getChildSegmentContaining(AbstractBone b) {
		if(segmentBoneList.contains(b)) { 
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
		if(segmentBoneList.contains(b)) 
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
			WorkingBone sb = bChain.simulatedBones.get(b);
			AbstractAxes bAxes = sb.simLocalAxes;
			AbstractAxes cAxes = sb.simConstraintAxes;
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
			SegmentedArmature bChain = b.parentArmature.boneSegmentMap.get(b); // getChainFor(b);			
			AbstractBone parent = b.getParent(); 
			WorkingBone sb = bChain.simulatedBones.get(b);
			AbstractAxes bAxes = sb.simLocalAxes;
			AbstractAxes cAxes = sb.simConstraintAxes;
			bChain.simAligned = true;
			bAxes.alignGlobalsTo(b.localAxes());
			bAxes.markDirty(); bAxes.updateGlobal();			
			cAxes.alignGlobalsTo(b.getMajorRotationAxes());
			cAxes.markDirty(); cAxes.updateGlobal();			
			if(parent != null) {
				SegmentedArmature bParentChain =  b.parentArmature.boneSegmentMap.get(parent);//getChainFor(parent);
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
		SegmentedArmature chain = b.parentArmature.boneSegmentMap.get(b); //getChainFor(b);
		if(chain != null) {			
			WorkingBone sb = chain.simulatedBones.get(b);
			AbstractAxes simulatedLocalAxes = sb.simLocalAxes;
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

	/**
	 * Holds working information for the given bone. 
	 * @author rufsketch1
	 */
	class WorkingBone {
		AbstractBone forBone;
		AbstractAxes simLocalAxes;
		AbstractAxes simConstraintAxes;
		double cosHalfDampen = 0d; 
		double cosHalfReturnFullnessDampened = 0d;
		boolean springy = false;

		public WorkingBone(AbstractBone toSimulate) {
			forBone = toSimulate;
			simLocalAxes =  forBone.localAxes().getGlobalCopy();
			simConstraintAxes = forBone.getMajorRotationAxes().getGlobalCopy();
			double predamp = 1d-forBone.getStiffness();
			double defaultDampening = forBone.parentArmature.dampening;
			double dampening = forBone.getParent() == null ? MathUtils.PI : predamp * defaultDampening;					
			cosHalfDampen = Math.cos(dampening/ 2d);
			AbstractKusudama k = ((AbstractKusudama)forBone.getConstraint());
			if( k != null && k.getReturnfullness() != 0d) {
				springy = true;
				cosHalfReturnFullnessDampened = Math.cos(((dampening*k.getReturnfullness())/2d));
			} else {
				springy = false;
			}
		}

		public void updateCosDampening() {
			double predamp = 1d-forBone.getStiffness();
			double defaultDampening = forBone.parentArmature.dampening;
			double dampening = forBone.getParent() == null ? MathUtils.PI : predamp * defaultDampening;					
			cosHalfDampen = Math.cos(dampening/ 2d);
			AbstractKusudama k = ((AbstractKusudama)forBone.getConstraint());
			if( k != null && k.getReturnfullness() != 0d) {
				springy = true;
				cosHalfReturnFullnessDampened = Math.cos(((dampening*k.getReturnfullness())/2d));
			} else {
				springy = false;
			}
		}
	}



}
