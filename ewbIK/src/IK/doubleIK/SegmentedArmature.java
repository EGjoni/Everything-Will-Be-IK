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

import IK.doubleIK.StrandedArmature.Strand;
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

	public SegmentedArmature(AbstractBone rootBone) {
		debugTipAxes = rootBone.localAxes.getGlobalCopy();
		debugTipAxes.setParent(rootBone.parentArmature.localAxes());
		debugTargetAxes = rootBone.localAxes.getGlobalCopy();
		debugTargetAxes.setParent(rootBone.parentArmature.localAxes());
		segmentRoot = armatureRootBone(rootBone);
		generateArmatureSegments();
		ensureAxesHeirarchy();
	}

	public SegmentedArmature(SegmentedArmature inputParentSegment, AbstractBone inputSegmentRoot) {
		debugTipAxes = inputSegmentRoot.localAxes.getGlobalCopy();
		debugTipAxes.setParent(inputSegmentRoot.parentArmature.localAxes());
		debugTargetAxes = inputSegmentRoot.localAxes.getGlobalCopy();
		debugTargetAxes.setParent(inputSegmentRoot.parentArmature.localAxes());
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
			}

			if(currentBone == stopOn) break;
			currentBone = currentBone.getParent();

		}

		/*for(SegmentedArmature sa : pinnedDescendants) {			
			ArrayList<AbstractBone> strandBoneList = getStrandFromTip(sa.segmentTip);
			strandMap.put(sa, strandBoneList);

			for(AbstractBone ab : strandBoneList) {
				AbstractAxes ax = simulatedLocalAxes.get(ab);
				if(ax == null) { 
					simulatedLocalAxes.put(ab, ab.localAxes().getGlobalCopy());
					simulatedConstraintAxes.put(ab, ab.getMajorRotationAxes().getGlobalCopy());
					boneRotationMap.put(ab, new ArrayList<Rot>());
				}
			}
		}*/

		strandsBoneList.addAll(boneRotationMap.keySet());
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

	/**
	 * Averaging multiple rotations is a poorly defined procedure, and 
	 * can lead to instability. However, fairly stable pseudo-averages can
	 * be generated when rotations are close to one another. To this end, 
	 * this method takes advantage of the fact that dampening is set 
	 * to minimize the distance between possible rotations, further, it averages 
	 * rotations in terms of the bone's local reference frame 
	 * 
	 * @param b the bone in this to compute the average rotation of 
	 * (result will be stored in the simulatedLocalAxes variable). 
	 */
	public void updateAverageRotationToPinnedDescendants(
			AbstractBone forBone, 
			int modeCode,
			double dampening) {

		double totalWeight = 0.0000001d;
		//we start with a weighted unit quaternion 
		//of tiny influence in order to avoid division by 0 
		//in cases of no rotation. 
		double[] accumulatedQ = {totalWeight,0d, 0d, 0d};
		double[] resultQ = {0d, 0d, 0d, 0d};

		double[][] errorTracker = new double[pinnedDescendants.size()][pinnedDescendants.size()+1];
		Rot[] localRots = new Rot[errorTracker.length];
		double[] weights = new double[errorTracker.length];
		double[] errorWeight = new double[errorTracker.length];
		double bestErrorDrop = 0d;
		double worstErrorIncrease = 0d;
		double totalErrorDrop = 0d;
		double totalError = 0d;

		sgRayd[] targetHeadings =  {
				new sgRayd(new SGVec_3d(), new SGVec_3d()), 
				new sgRayd(new SGVec_3d(), new SGVec_3d()), 
				new sgRayd(new SGVec_3d(), new SGVec_3d())
		};
		sgRayd[] tipHeadings =  {
				new sgRayd(new SGVec_3d(), new SGVec_3d()), 
				new sgRayd(new SGVec_3d(), new SGVec_3d()), 
				new sgRayd(new SGVec_3d(), new SGVec_3d())
		};

		int debug =0;

		for(int i=0; i< pinnedDescendants.size(); i++) {
			SegmentedArmature s = pinnedDescendants.get(i);
			AbstractAxes targetAxes = s.segmentTip.getPinnedAxes();
			AbstractAxes tipAxes = s.simulatedLocalAxes.get(s.segmentTip);						
			targetHeadings[0].setP1(targetAxes.origin_()); targetHeadings[0].heading(targetAxes.orientation_X_());
			targetHeadings[1].setP1(targetAxes.origin_()); targetHeadings[1].heading(targetAxes.orientation_Y_());
			targetHeadings[2].setP1(targetAxes.origin_()); targetHeadings[2].heading(targetAxes.orientation_Z_());

			tipHeadings[0].setP1(tipAxes.origin_()); tipHeadings[0].heading(tipAxes.orientation_X_());
			tipHeadings[1].setP1(tipAxes.origin_()); tipHeadings[1].heading(tipAxes.orientation_Y_());
			tipHeadings[2].setP1(tipAxes.origin_()); tipHeadings[2].heading(tipAxes.orientation_Z_());

			double pinWeight = s.segmentTip.getIKPin().getPinWeight(); 			

			double preDampedAngle = addAveragedRotationToTarget(
					targetHeadings, 
					tipHeadings, 
					forBone, 
					modeCode, 
					dampening, 
					accumulatedQ,
					resultQ,
					pinWeight);			
			localRots[i] = new Rot(new MRotation(resultQ[0], resultQ[1], resultQ[2], resultQ[3]));
			totalWeight += preDampedAngle;
			//System.out.println(totalWeight);
			weights[i] = preDampedAngle;
		}
		AbstractAxes simBoneAxes = simulatedLocalAxes.get(forBone);
		AbstractAxes simConstraintAxes = simulatedConstraintAxes.get(forBone);

		//Rot averagedLocalRot = new Rot(Rot.slerp(0.5d, localRots[0].rotation, localRots[1].rotation));
		//Rot averagedLocalRot = Rot.nlerp(localRots, weights);
		Rot averagedLocalRot = Rot.instantaneousAvg(localRots, weights);
		if(averagedLocalRot == null) averagedLocalRot = new Rot();
		//Rot averagedLocalRot = localRots[0];
		//System.out.println("error iterations for : " +forBone + " {");
		Rot minErRot = averagedLocalRot;
	/*	for(double e =0; e<=100; e++) {
			double eStart = e/100d;
			double[] testWeight = {eStart, 1-eStart};
			double totalErrorAt = 0d;
			double weightedErrorAt = 0d; 
			double bestErrorDiff = -10d;
			Rot testRot = null;
			
			for(int i=pinnedDescendants.size()-1; i>= 0; i--) {
				SegmentedArmature s = pinnedDescendants.get(i);
				AbstractAxes targetAxes = s.segmentTip.getPinnedAxes();
				AbstractAxes tipAxes = s.simulatedLocalAxes.get(s.segmentTip);			
				targetHeadings[0].setP1(targetAxes.origin_()); targetHeadings[0].heading(targetAxes.orientation_X_());
				targetHeadings[1].setP1(targetAxes.origin_()); targetHeadings[1].heading(targetAxes.orientation_Y_());
				targetHeadings[2].setP1(targetAxes.origin_()); targetHeadings[2].heading(targetAxes.orientation_Z_());			
				tipHeadings[0].setP1(tipAxes.origin_()); tipHeadings[0].heading(tipAxes.orientation_X_());
				tipHeadings[1].setP1(tipAxes.origin_()); tipHeadings[1].heading(tipAxes.orientation_Y_());
				tipHeadings[2].setP1(tipAxes.origin_()); tipHeadings[2].heading(tipAxes.orientation_Z_());
				testRot = Rot.instantaneousAvg(localRots, testWeight);
				totalErrorAt += measureMinkowskiError(forBone, testRot, s, targetHeadings, tipHeadings, 1, modeCode);
				weightedErrorAt += measureMinkowskiError(forBone, averagedLocalRot, s, targetHeadings, tipHeadings, 1, modeCode);
				//errorTracker[i][0] = measureMinkowskiError(forBone, null, s, targetHeadings, tipHeadings, 1, modeCode);
			}
			double errorDiff = weightedErrorAt - totalErrorAt;
			if( errorDiff > 0d) {
				System.out.println("improvementAt "+(int)e+": " + errorDiff + "of "+ weightedErrorAt);
				if(errorDiff > bestErrorDiff) {
					bestErrorDiff = errorDiff;
					minErRot = testRot;
				}
			}
			
			
		}*/
		//System.out.println("}");

		/*new Rot(
					accumulatedQ[0] /totalWeight,
					accumulatedQ[1] /totalWeight,
					accumulatedQ[2] /totalWeight,
					accumulatedQ[3] /totalWeight,
					true);*/
		if(Double.isNaN(minErRot.getAngle())) {
			debug = 0;
		}
		/*Quaternion singleCovered = G.getSingleCoveredQuaternion(G.getQuaternion(averagedLocalRot), G.getQuaternion(forBone.parentArmature.localAxes().globalMBasis.rotation));
		averagedLocalRot = new Rot(singleCovered.getQ0(), singleCovered.getQ1(), singleCovered.getQ2(), singleCovered.getQ3(), false);*/


		Rot clampedRot = new Rot(minErRot.getAxis(), MathUtils.clamp(minErRot.getAngle(), -dampening, dampening));
		//Rot clampedRot = minErRot;
		//simBoneAxes.alignGlobalsTo(forBone.localAxes());
		simBoneAxes.markDirty(); simBoneAxes.updateGlobal();
		simBoneAxes.localMBasis.rotateBy(clampedRot);
		simBoneAxes.markDirty(); simBoneAxes.updateGlobal();
		forBone.setAxesToSnapped(simBoneAxes, simConstraintAxes);
		simBoneAxes.markDirty(); simBoneAxes.updateGlobal();

		//}

	}

	/**
	 * adds to accumulatedQ[] the quaternion values for the localMBasis rotation
	 * of the given bone after it attempts to satisfy the rotation from the appropriate given 
	 * tipHeadings to the appropriate given targetHeadings. These attempts are averaged together 
	 * before the result is weighted by tipWeight and added to accumulatedQ[].  
	 * 
	 * @param b
	 * @param orientationAware
	 * @param twistAware
	 * @param useRedundancy
	 * @param dampening
	 * @param accumulatedQ
	 * @param tipWeight
	 * 
	 * @eturn radiansRotated prior to clamping (to be used for weighting calculations. 
	 */
	public double addAveragedRotationToTarget(
			sgRayd[] targetAxesHeadings,
			sgRayd[] tipHeadings, 			
			AbstractBone forBone, 
			int modeCode,
			double dampening,
			double[] accumulatedQ, 
			double[] resultQ,
			double tipWeight) {


		AbstractAxes currentBoneSimulatedAxes = simulatedLocalAxes.get(forBone);
		AbstractAxes preSolveCurrentAxes = currentBoneSimulatedAxes.getGlobalCopy();
		AbstractAxes currentBoneConstraintAxes = simulatedConstraintAxes.get(forBone);

		//accounts for clamping's tendency to hide information about the relative magnitudes of rotations
		double clampingTotalWeight =0.0000001d; 


		double passCount = 0d;

		if((modeCode &1) != 0) passCount++;   
		if((modeCode &2) != 0) passCount++;   
		if((modeCode &4) != 0) passCount++; 

		Rot[] rotations = new Rot[(int) passCount];

		double accumulatedq0 = clampingTotalWeight;
		double accumulatedq1 = 0d;
		double accumulatedq2 = 0d;
		double accumulatedq3 = 0d;

		MRotation inverseStartRot = currentBoneSimulatedAxes.localMBasis.rotation.rotation.getInverse(); 
		currentBoneSimulatedAxes.getParentAxes().updateGlobal();
		int ridx = 0;
		for(int mode = 0 ; mode < 3; mode++) {
			boolean skipRound = true;
			
			if((modeCode & 1<<mode) != 0) skipRound = false;   
			if((modeCode &1<<mode) != 0) skipRound = false;   
			if((modeCode &1<<mode) != 0) skipRound = false;   
			  
			if(!skipRound) {
				int rayIndex = (mode+1)%3;
				sgRayd relevantTipRay = tipHeadings[rayIndex];
				sgRayd relevantTargetRay = targetAxesHeadings[rayIndex];
				Rot dirRot = AbstractArmature.contextualPlanarRotation(
						currentBoneSimulatedAxes.origin_(), 
						relevantTipRay.p1(), relevantTipRay.p2(),
						relevantTargetRay.p1(), relevantTargetRay.p2());


				Rot localizedRotDir = currentBoneSimulatedAxes.getParentAxes().globalMBasis.getLocalOfRotation(dirRot);				
				
				Quaternion q =G.getSingleCoveredQuaternion(
						G.getQuaternion(localizedRotDir), 
						G.getQuaternion(currentBoneConstraintAxes.localMBasis.rotation)
						);			
				localizedRotDir = new Rot(q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(), true);
				rotations[ridx] = localizedRotDir;
				//MRotation localizedDirRot = inverseStartRot.multiply(localizedRot.rotation);
				/*double dirAngle = localizedRotDir.getAngle();
				localizedRotDir = new Rot(localizedRotDir.getAxis(), MathUtils.clamp(dirAngle, -dampening, dampening));
				

				currentBoneSimulatedAxes.localMBasis.rotateBy(localizedRotDir); 
				currentBoneSimulatedAxes.markDirty(); currentBoneSimulatedAxes.updateGlobal();

				forBone.setAxesToSnapped(currentBoneSimulatedAxes, currentBoneConstraintAxes);
				currentBoneSimulatedAxes.updateGlobal();

				MRotation mr =inverseStartRot.multiply(currentBoneSimulatedAxes.localMBasis.rotation.rotation);//, 


				//we multiply the rotation by the full angle prior to dampening or 
				//constraining so as to preserve some notion of how *much* 
				//we tried to rotate in any given direction (information which would be hidden by the clamping. 
				//we could of course just clamp after averaging the rotations, 
				//but this would lose us the guarantees that dampening offers on the relative closeness 
				//of rotations. 
				accumulatedq0 += mr.getQ0()*dirAngle;
				accumulatedq1 += mr.getQ1()*dirAngle;
				accumulatedq2 += mr.getQ2()*dirAngle;
				accumulatedq3 += mr.getQ3()*dirAngle;

				clampingTotalWeight += dirAngle;*/
				//currentBoneSimulatedAxes.alignGlobalsTo(forBone.localAxes());
				//currentBoneSimulatedAxes.markDirty(); currentBoneSimulatedAxes.updateGlobal();
				ridx++;
			}
		}

		Rot avgRot = Rot.instantaneousAvg(rotations, null);/*new MRotation((accumulatedq0/clampingTotalWeight),
				(accumulatedq1/clampingTotalWeight),
				(accumulatedq2/clampingTotalWeight),
				(accumulatedq3/clampingTotalWeight), true);*/
		
		

		double predamp = avgRot.getAngle();
		avgRot = new Rot(avgRot.getAxis(),  MathUtils.clamp(predamp, -dampening, dampening));
		currentBoneSimulatedAxes.localMBasis.rotateBy(avgRot); 
		currentBoneSimulatedAxes.markDirty(); currentBoneSimulatedAxes.updateGlobal();
		forBone.setAxesToSnapped(currentBoneSimulatedAxes, currentBoneConstraintAxes);
		currentBoneSimulatedAxes.markDirty(); currentBoneSimulatedAxes.updateGlobal();

		Rot iAvgRotBy = new Rot(inverseStartRot.multiply(currentBoneSimulatedAxes.localMBasis.rotation.rotation));

		resultQ[0] = iAvgRotBy.rotation.getQ0();
		resultQ[1] = iAvgRotBy.rotation.getQ1(); 
		resultQ[2] = iAvgRotBy.rotation.getQ2(); 
		resultQ[3] = iAvgRotBy.rotation.getQ3(); 

		//Rot toAvgRot = new Rot(currentBoneSimulatedAxes.localMBasis.rotation.rotation.getInverse().multiply(avgRot));
		/*double preDampedAngle = avgRot.getAngle();
		Rot clampedToPinRot = new Rot(avgRot.getAxis(), MathUtils.clamp(preDampedAngle, -dampening, dampening));

		accumulatedQ[0] += tipWeight*preDampedAngle*(clampedToPinRot.rotation.getQ0());///passCount); 
		accumulatedQ[1] += tipWeight*preDampedAngle*(clampedToPinRot.rotation.getQ1());///passCount); 
		accumulatedQ[2] += tipWeight*preDampedAngle*(clampedToPinRot.rotation.getQ2());///passCount); 
		accumulatedQ[3] += tipWeight*preDampedAngle*(clampedToPinRot.rotation.getQ3());///passCount); 
		return preDampedAngle; */


		/*resultQ[0] += tipWeight*(accumulatedq0/clampingTotalWeight);///passCount); 
		resultQ[1] += tipWeight*(accumulatedq1/clampingTotalWeight);///passCount); 
		resultQ[2] += tipWeight*(accumulatedq2/clampingTotalWeight);///passCount); 
		resultQ[3] += tipWeight*(accumulatedq3/clampingTotalWeight);///passCount);*/ 

		accumulatedQ[0] += resultQ[0];
		accumulatedQ[1] += resultQ[1];
		accumulatedQ[2] += resultQ[2];
		accumulatedQ[3] += resultQ[3];

		currentBoneSimulatedAxes.alignGlobalsTo(preSolveCurrentAxes);
		currentBoneSimulatedAxes.markDirty(); currentBoneSimulatedAxes.updateGlobal();

		return predamp/iAvgRotBy.getAngle(); 
	}

	/**
	 * old error based version 
	 * 
	 * public void updateAverageRotationToPinnedDescendants(
			AbstractBone forBone, 
			int modeCode,
			double dampening) {

		double totalWeight = 0.0000001d;
		//we start with a weighted unit quaternion 
		//of tiny influence in order to avoid division by 0 
		//in cases of no rotation. 
		double[] accumulatedQ = {totalWeight,0d, 0d, 0d};
		double[] resultQ = {0d, 0d, 0d, 0d};

		double[][] errorTracker = new double[pinnedDescendants.size()][pinnedDescendants.size()+1];
		Rot[] localRots = new Rot[errorTracker.length];
		double[] errorWeight = new double[errorTracker.length];
		double bestErrorDrop = 0d;
		double worstErrorIncrease = 0d;
		double totalErrorDrop = 0d;
		double totalError = 0d;

		sgRayd[] targetHeadings =  {
				new sgRayd(new SGVec_3d(), new SGVec_3d()), 
				new sgRayd(new SGVec_3d(), new SGVec_3d()), 
				new sgRayd(new SGVec_3d(), new SGVec_3d())
		};
		sgRayd[] tipHeadings =  {
				new sgRayd(new SGVec_3d(), new SGVec_3d()), 
				new sgRayd(new SGVec_3d(), new SGVec_3d()), 
				new sgRayd(new SGVec_3d(), new SGVec_3d())
		};

		// populate the error tracker with the errors at this iteration so we can check 
		// whether each rotation contributes to a decrease or increase in the total error. 

		for(int i=pinnedDescendants.size()-1; i>=0; i--) {
			SegmentedArmature s = pinnedDescendants.get(i);
			AbstractAxes targetAxes = s.segmentTip.getPinnedAxes();
			AbstractAxes tipAxes = s.simulatedLocalAxes.get(s.segmentTip);			
			targetHeadings[0].setP1(targetAxes.origin_()); targetHeadings[0].heading(targetAxes.orientation_X_());
			targetHeadings[1].setP1(targetAxes.origin_()); targetHeadings[1].heading(targetAxes.orientation_Y_());
			targetHeadings[2].setP1(targetAxes.origin_()); targetHeadings[2].heading(targetAxes.orientation_Z_());			
			tipHeadings[0].setP1(tipAxes.origin_()); tipHeadings[0].heading(tipAxes.orientation_X_());
			tipHeadings[1].setP1(tipAxes.origin_()); tipHeadings[1].heading(tipAxes.orientation_Y_());
			tipHeadings[2].setP1(tipAxes.origin_()); tipHeadings[2].heading(tipAxes.orientation_Z_());
			//errorTracker[i][0] = measureMinkowskiError(forBone, null, s, targetHeadings, tipHeadings, 1, modeCode);
		}



		int debug =0;

		for(int i=pinnedDescendants.size()-1; i>=0; i--) {
			SegmentedArmature s = pinnedDescendants.get(i);
			AbstractAxes targetAxes = s.segmentTip.getPinnedAxes();
			AbstractAxes tipAxes = s.simulatedLocalAxes.get(s.segmentTip);						
			targetHeadings[0].setP1(targetAxes.origin_()); targetHeadings[0].heading(targetAxes.orientation_X_());
			targetHeadings[1].setP1(targetAxes.origin_()); targetHeadings[1].heading(targetAxes.orientation_Y_());
			targetHeadings[2].setP1(targetAxes.origin_()); targetHeadings[2].heading(targetAxes.orientation_Z_());

			tipHeadings[0].setP1(tipAxes.origin_()); tipHeadings[0].heading(tipAxes.orientation_X_());
			tipHeadings[1].setP1(tipAxes.origin_()); tipHeadings[1].heading(tipAxes.orientation_Y_());
			tipHeadings[2].setP1(tipAxes.origin_()); tipHeadings[2].heading(tipAxes.orientation_Z_());

			double pinWeight = s.segmentTip.getIKPin().getPinWeight(); 			

			double preDampedAngle = addAveragedRotationToTarget(
					targetHeadings, 
					tipHeadings, 
					forBone, 
					modeCode, 
					dampening, 
					accumulatedQ,
					resultQ,
					pinWeight);

			Rot locRot = new Rot(new MRotation(resultQ[0], resultQ[1], resultQ[2], resultQ[3], true));
			localRots[i] = locRot;
			double errorToAll = 0d;
			double errorDropToAll = 0d; 
			for(int j=pinnedDescendants.size()-1; j>=0; j--) {
				SegmentedArmature ss = pinnedDescendants.get(j);
				AbstractAxes targetAxes2 = ss.segmentTip.getPinnedAxes();
				AbstractAxes tipAxes2 = ss.simulatedLocalAxes.get(ss.segmentTip);			
				targetHeadings[0].setP1(targetAxes2.origin_()); targetHeadings[0].heading(targetAxes2.orientation_X_());
				targetHeadings[1].setP1(targetAxes2.origin_()); targetHeadings[1].heading(targetAxes2.orientation_Y_());
				targetHeadings[2].setP1(targetAxes2.origin_()); targetHeadings[2].heading(targetAxes2.orientation_Z_());			
				tipHeadings[0].setP1(tipAxes2.origin_()); tipHeadings[0].heading(tipAxes2.orientation_X_());
				tipHeadings[1].setP1(tipAxes2.origin_()); tipHeadings[1].heading(tipAxes2.orientation_Y_());
				tipHeadings[2].setP1(tipAxes2.origin_()); tipHeadings[2].heading(tipAxes2.orientation_Z_());
				double error = measureMinkowskiError(forBone, locRot, ss, targetHeadings, tipHeadings, 1, modeCode);// - errorTracker[i];
				double errorDrop = error - errorTracker[i][0];
				errorTracker[i][j+1] =  error;
				if(Double.isInfinite(1d/error)) {
					int debug2 = 0;
				}
				errorToAll += 1d/error;
				errorDropToAll += errorDrop;
			}
			//errorToAll = Math.min(0d,  errorToAll);
			double inverseSquareErrorToAll = errorToAll; 
			totalError+= inverseSquareErrorToAll;
			totalErrorDrop += Math.abs(errorDropToAll);
			bestErrorDrop = Math.min(errorDropToAll, bestErrorDrop);
			worstErrorIncrease = Math.max(errorDropToAll, worstErrorIncrease);
			errorWeight[i] = inverseSquareErrorToAll;
			totalWeight += pinWeight*preDampedAngle;
		}
		AbstractAxes simBoneAxes = simulatedLocalAxes.get(forBone);
		AbstractAxes simConstraintAxes = simulatedConstraintAxes.get(forBone);

		if(totalError != 0d && ! Double.isInfinite(totalError)) {
			double[] errorWeightedRot = new double[4];
			for(int i=0; i<errorWeight.length; i++) {
				double normalizedWeight = (errorWeight[i]) / totalError;
				errorWeightedRot[0] += localRots[i].rotation.getQ0()*normalizedWeight;
				errorWeightedRot[1] += localRots[i].rotation.getQ1()*normalizedWeight; 
				errorWeightedRot[2] += localRots[i].rotation.getQ2()*normalizedWeight; 
				errorWeightedRot[3] += localRots[i].rotation.getQ3()*normalizedWeight; 
			}


			Rot averagedLocalRot = new Rot(
					errorWeightedRot[0], //accumulatedQ[0] /totalWeight,
					errorWeightedRot[1], //accumulatedQ[1] /totalWeight,
					errorWeightedRot[2], //accumulatedQ[2] /totalWeight,
					errorWeightedRot[3], //accumulatedQ[3] /totalWeight,
					true);
			if(Double.isNaN(averagedLocalRot.getAngle())) {
				debug = 0;
			}
			//Quaternion singleCovered = G.getSingleCoveredQuaternion(G.getQuaternion(averagedLocalRot), G.getQuaternion(forBone.parentArmature.localAxes().globalMBasis.rotation));
		//averagedLocalRot = new Rot(singleCovered.getQ0(), singleCovered.getQ1(), singleCovered.getQ2(), singleCovered.getQ3(), false);
			Rot clampedRot = new Rot(averagedLocalRot.getAxis(), MathUtils.clamp(averagedLocalRot.getAngle(), -dampening, dampening));

			simBoneAxes.alignGlobalsTo(forBone.localAxes());
			simBoneAxes.markDirty(); simBoneAxes.updateGlobal();
			simBoneAxes.localMBasis.rotateBy(clampedRot);
			simBoneAxes.markDirty(); simBoneAxes.updateGlobal();
			forBone.setAxesToSnapped(simBoneAxes, simConstraintAxes);
			simBoneAxes.updateGlobal();

			//if(pinnedDescendants.size() > 1) {

			if(pinnedDescendants.size() > 1) {
				System.out.println("Post-Error of " + forBone + ": [");
				for(int i=0; i<errorTracker.length; i++) {
					System.out.print((float)errorTracker[i][0]+" -> [ ");
					float total = 0f;
					for(int j= 1; j<errorTracker[i].length; j++) {
						float errorChange = (float)(errorTracker[i][j] - errorTracker[i][0]);
						System.out.print(errorChange+",  ");
						total = errorChange;
					}
					System.out.println("] = " + (float)(errorWeight[i]/totalError));
				}
				System.out.println("]");
			}
		}
		//}

	}
	 * 	
	 */


	/**
	 * use minkowski distance to measure error between components from segment tip location and orientation  to target 
	 * @param fromBone
	 * @param forTip 
	 * @param localRotation if null, no rotation will be applied to the simulationAxes before checking for rotation errors.
	 * @return a positive or negative number representing how much applying this rotation to the given bone would cause 
	 * the error to increase or decrease for the given segment tip and its associated target.  
	 */
	public double measureMinkowskiError(
			AbstractBone fromBone, 
			Rot localRotation, 
			SegmentedArmature forTip,
			sgRayd[] targetHeadings, 
			sgRayd[] simTipHeadings,
			double power, int modeCode) {

		double totalDist = 0d;
		double totalRounds = 1d;
		
		AbstractAxes simBoneAxes = simulatedLocalAxes.get(fromBone); 
		AbstractAxes preTestAxes = simBoneAxes.getGlobalCopy();
		
		
		if(localRotation != null) {
			simBoneAxes.localMBasis.rotateBy(localRotation); 
			simBoneAxes.markDirty(); simBoneAxes.updateGlobal();
			AbstractAxes tipAxes2 = forTip.simulatedLocalAxes.get(forTip.segmentTip);
			tipAxes2.markDirty();
			tipAxes2.updateGlobal();
			simTipHeadings[0].setP1(tipAxes2.origin_()); simTipHeadings[0].heading(tipAxes2.orientation_X_());
			simTipHeadings[1].setP1(tipAxes2.origin_()); simTipHeadings[1].heading(tipAxes2.orientation_Y_());
			simTipHeadings[2].setP1(tipAxes2.origin_()); simTipHeadings[2].heading(tipAxes2.orientation_Z_());
		}

		totalDist =//0d; targetHeadings[0].p1().dist(simTipHeadings[0].p1());
				SGVec_3d.angleBetween(simBoneAxes.origin_().subCopy(simTipHeadings[0].p1()), simBoneAxes.origin_().subCopy(targetHeadings[0].p1()));


		for(int mode = 0 ; mode < 3; mode++) {
			boolean skipRound = true;
			switch(mode){
			case	0:  if((modeCode &1) != 0) skipRound = false;   
			case 1:   if((modeCode &2) != 0) skipRound = false;   
			case 2:   if((modeCode &4) != 0) skipRound = false;   
			}  
			if(!skipRound) {
				int rayIndex = (mode+1)%3;
				totalDist += SGVec_3d.angleBetween(simTipHeadings[rayIndex].heading(), targetHeadings[rayIndex].heading());
				totalRounds ++;
			}
		}

		if(localRotation != null) { 
			simBoneAxes.alignGlobalsTo(preTestAxes);
			simBoneAxes.updateGlobal();
		}
		return 0.001d+totalDist;//Math.pow(totalDist, 1d/totalRounds); 	
	}







	/**
	 * @return an array of three SGRay objects, representing possibly non-uniform basis of right handed chirality
	 * corresponding to the weighted average of the right handed bases of all descendent targets affecting this chain.
	 */
	public sgRayd[] getAverageTargetOrientationAcrossAllPinnedBones(
			boolean importanceWeighting) {
		sgRayd xTotal = new sgRayd(new SGVec_3d(), new SGVec_3d());
		sgRayd yTotal = new sgRayd(new SGVec_3d(), new SGVec_3d());
		sgRayd zTotal = new sgRayd(new SGVec_3d(), new SGVec_3d());
		double totalWeight = 0d;
		//Rot thisBoneAxes = neighborhoodAxes.globalMBasis.rotation;
		//Quaternion thisBoneQ = G.getQuaternion(thisBoneAxes);
		SGVec_3d  origin = new SGVec_3d();
		SGVec_3d 	xDir = new SGVec_3d();
		SGVec_3d 	yDir = new SGVec_3d();
		SGVec_3d 	zDir = new SGVec_3d(); 

		double accumulatedq0 = 0d; 
		double accumulatedq1 = 0d; 
		double accumulatedq2 = 0d; 
		double accumulatedq3 = 0d;
		AbstractAxes simulatedTipAxes = segmentTip.parentArmature.localAxes();//simulatedConstraintAxes.get(segmentTip);
		simulatedTipAxes.updateGlobal();
		Quaternion tipQ = null;//G.getQuaternion(simulatedTipAxes.globalMBasis.rotation); 
		for(SegmentedArmature s: pinnedDescendants) {
			s.simulatedLocalAxes.get(s.segmentTip).updateGlobal();
			AbstractAxes pinnedAxes = s.segmentTip.getPinnedAxes();//.globalMBasis.rotation;
			pinnedAxes.updateGlobal();
			double weight = s.segmentTip.getIKPin().getPinWeight();
			/*origin.add(pinnedAxes.origin_().multCopy(weight));*/
			/*Quaternion singleCovered = null; 
			Quaternion tempQ =new Quaternion(
							   pinnedAxes.globalMBasis.rotation.rotation.getQ0(),
					           pinnedAxes.globalMBasis.rotation.rotation.getQ1(),
					           pinnedAxes.globalMBasis.rotation.rotation.getQ2(),
					           pinnedAxes.globalMBasis.rotation.rotation.getQ3());
			if(tipQ == null) {
				tipQ = tempQ;
				singleCovered = tipQ;
			} else
				singleCovered = G.getSingleCoveredQuaternion(tempQ, tipQ);

			accumulatedq0 += singleCovered.getQ0() * weight;
			accumulatedq1 += singleCovered.getQ1() * weight;
			accumulatedq2 += singleCovered.getQ2() * weight;
			accumulatedq3 += singleCovered.getQ3() * weight;*/
			xDir.set(pinnedAxes.orientation_X_());
			yDir.set(pinnedAxes.orientation_Y_());
			zDir.set(pinnedAxes.orientation_Z_()); 

			origin.set(pinnedAxes.origin_());
			origin.mult(weight);
			xDir.mult(weight);
			yDir.mult(weight);
			zDir.mult(weight);

			xTotal.p1().add(origin); xTotal.p2().add(origin); xTotal.p2().add(xDir);
			yTotal.p1().add(origin); yTotal.p2().add(origin); yTotal.p2().add(yDir);
			zTotal.p1().add(origin); zTotal.p2().add(origin); zTotal.p2().add(zDir);
			totalWeight += weight; 
		}
		/*Rot normalizedRot = new Rot(
				accumulatedq0/totalWeight, 
				accumulatedq1/totalWeight, 
				accumulatedq2/totalWeight, 
				accumulatedq3/totalWeight,
				true);*/
		//Quaternion singleCovered = G.getSingleCoveredQuaternion(G.getQuaternion(normalizedRot), tipQ);
		//normalizedRot = new Rot(new MRotation(singleCovered.getQ0(), singleCovered.getQ1(), singleCovered.getQ2(), singleCovered.getQ3()));
		/*origin.div(totalWeight); 
		xTotal.setP1(origin); xTotal.heading(normalizedRot.applyToCopy(Basis.xBase));
		yTotal.setP1(origin); yTotal.heading(normalizedRot.applyToCopy(Basis.yBase));
		zTotal.setP1(origin); zTotal.heading(normalizedRot.applyToCopy(Basis.zBase));*/
		xTotal.p1().div(totalWeight); xTotal.p2().div(totalWeight);
		yTotal.p1().div(totalWeight); yTotal.p2().div(totalWeight);
		zTotal.p1().div(totalWeight); zTotal.p2().div(totalWeight);
		//xTotal.normalize(); yTotal.normalize(); zTotal.normalize();

		double xWeight = xTotal.mag();
		double yWeight = yTotal.mag();
		double zWeight = zTotal.mag();

		debugTargetAxes.setOrthoNormalityConstraint(false);
		/*debugTargetAxes.translateTo(xTotal.p1()); debugTargetAxes.updateGlobal();
		debugTargetAxes.globalMBasis.setXHeading(xTotal.heading(), false);
		debugTargetAxes.globalMBasis.setYHeading(yTotal.heading(), false);
		debugTargetAxes.globalMBasis.setZHeading(zTotal.heading(), true);

		previousIdealTargetOrientation = debugTargetAxes.globalMBasis.extractIdealRotation(
				debugTargetAxes.globalMBasis.getComposedMatrix(),
				previousIdealTargetOrientation,
				120, 
				false); */
		if(pinnedDescendants.size() > 1) {			
			int debug = 0;
			//System.out.println("LAST Target QUAT = " +  previousIdealTargetOrientation);
		}
		previousIdealTargetOrientation = extractIdealRotation(
				xTotal.heading(), 
				yTotal.heading(), 
				zTotal.heading(),
				previousIdealTargetOrientation.rotation,//simulatedLocalAxes.get(segmentTip).globalMBasis.rotation.rotation,//
				12, 
				false); 
		if(pinnedDescendants.size() > 1) {			
			int debug = 0;
			//System.out.println("NEW Target QUAT = " +  previousIdealTargetOrientation);
		}


		debugTargetAxes.translateTo(xTotal.p1());
		xTotal.heading(previousIdealTargetOrientation.applyToCopy(Basis.xBase));
		yTotal.heading(previousIdealTargetOrientation.applyToCopy(Basis.yBase));
		zTotal.heading(previousIdealTargetOrientation.applyToCopy(Basis.zBase));		
		debugTargetAxes.globalMBasis.setXHeading(xTotal.heading(), false);
		debugTargetAxes.globalMBasis.setYHeading(yTotal.heading(), false);
		debugTargetAxes.globalMBasis.setZHeading(zTotal.heading(), true);
		debugTargetAxes.getParentAxes().setToLocalOf(debugTargetAxes.globalMBasis, debugTargetAxes.localMBasis);
		debugTargetAxes.markDirty();

		if(pinnedDescendants.size() > 1) {			
			int debug = 0;
			//System.out.println("NEW Y = " +  yTotal.heading());
			//System.out.println("------------------");
		}


		sgRayd[] result = {
				xTotal.getRayScaledBy(xWeight), 
				yTotal.getRayScaledBy(yWeight), 
				zTotal.getRayScaledBy(zWeight)};

		return result;
	}

	Rot previousIdealTargetOrientation = new Rot(); 
	Rot previousIdealTipOrientation = new Rot(); 

	/**
	 * @param pass the pass number to use from the rotationAxes list.
	 * @return an array of three SGRay objects, representing possibly non-uniform basis of right handed chirality
	 * corresponding to the weighted average of the right handed bases of all descendent tips affecting this chain.
	 */
	public sgRayd[] getAverageTipOrientationAcrossAllPinnedBones(
			boolean importanceWeighting) {
		sgRayd xTotal = new sgRayd(new SGVec_3d(), new SGVec_3d());
		sgRayd yTotal = new sgRayd(new SGVec_3d(), new SGVec_3d());
		sgRayd zTotal = new sgRayd(new SGVec_3d(), new SGVec_3d());
		double totalWeight = 0d;
		//Rot thisBoneAxes = neighborhoodAxes.globalMBasis.rotation;
		//Quaternion thisBoneQ = G.getQuaternion(thisBoneAxes);
		SGVec_3d  origin = new SGVec_3d();
		SGVec_3d 	xDir = new SGVec_3d();
		SGVec_3d 	yDir = new SGVec_3d();
		SGVec_3d 	zDir = new SGVec_3d(); 
		Quaternion tipQ = null;
		double accumulatedq0 = 0d; 
		double accumulatedq1 = 0d; 
		double accumulatedq2 = 0d; 
		double accumulatedq3 = 0d;
		for(SegmentedArmature s: pinnedDescendants) {
			s.simulatedLocalAxes.get(s.segmentTip).updateGlobal();
			AbstractAxes tipAxes = s.simulatedLocalAxes.get(s.segmentTip);//.getPinnedAxes().globalMBasis.rotation;
			tipAxes.updateGlobal();
			AbstractAxes pinnedAxes = s.segmentTip.getPinnedAxes();//.globalMBasis.rotation;
			pinnedAxes.updateGlobal();
			double weight = s.segmentTip.getIKPin().getPinWeight();
			/*origin.add(tipAxes.origin_().multCopy(weight));*/
			/*Quaternion singleCovered = null; 
			Quaternion tempQ =new Quaternion(
					tipAxes.globalMBasis.rotation.rotation.getQ0(),
					tipAxes.globalMBasis.rotation.rotation.getQ1(),
					tipAxes.globalMBasis.rotation.rotation.getQ2(),
					tipAxes.globalMBasis.rotation.rotation.getQ3());
			if(tipQ == null) {
				tipQ = tempQ;
				singleCovered = tipQ;
			} else
				singleCovered = G.getSingleCoveredQuaternion(tempQ, tipQ);

			accumulatedq0 += singleCovered.getQ0() * weight;
			accumulatedq1 += singleCovered.getQ1() * weight;
			accumulatedq2 += singleCovered.getQ2() * weight;
			accumulatedq3 += singleCovered.getQ3() * weight;*/

			origin.set(tipAxes.origin_());
			xDir.set(tipAxes.orientation_X_());
			yDir.set(tipAxes.orientation_Y_());
			zDir.set(tipAxes.orientation_Z_()); 

			origin.mult(weight);
			xDir.mult(weight);
			yDir.mult(weight);
			zDir.mult(weight);

			xTotal.p1().add(origin); xTotal.p2().add(origin); xTotal.p2().add(xDir);
			yTotal.p1().add(origin); yTotal.p2().add(origin); yTotal.p2().add(yDir);
			zTotal.p1().add(origin); zTotal.p2().add(origin); zTotal.p2().add(zDir);
			totalWeight += weight; 
		}
		xTotal.p1().div(totalWeight); xTotal.p2().div(totalWeight);
		yTotal.p1().div(totalWeight); yTotal.p2().div(totalWeight);
		zTotal.p1().div(totalWeight); zTotal.p2().div(totalWeight);

		double xWeight = xTotal.mag();
		double yWeight = yTotal.mag();
		double zWeight = zTotal.mag();
		//xTotal.normalize(); yTotal.normalize(); zTotal.normalize();
		/*Rot normalizedRot = new Rot(
				accumulatedq0/totalWeight, 
				accumulatedq1/totalWeight, 
				accumulatedq2/totalWeight, 
				accumulatedq3/totalWeight,
				true);*/

		/*origin.div(totalWeight); 
		xTotal.setP1(origin); xTotal.heading(normalizedRot.applyToCopy(Basis.xBase));
		yTotal.setP1(origin); yTotal.heading(normalizedRot.applyToCopy(Basis.yBase));
		zTotal.setP1(origin); zTotal.heading(normalizedRot.applyToCopy(Basis.zBase));*/

		debugTipAxes.setOrthoNormalityConstraint(false);
		/*debugTipAxes.translateTo(xTotal.p1()); debugTipAxes.updateGlobal();
		debugTipAxes.globalMBasis.setXHeading(xTotal.heading(), false);
		debugTipAxes.globalMBasis.setYHeading(yTotal.heading(), false);
		debugTipAxes.globalMBasis.setZHeading(zTotal.heading(), true);*/
		if(pinnedDescendants.size() > 1) {			
			int debug = 0;

		}
		previousIdealTipOrientation = extractIdealRotation(
				xTotal.heading(), 
				yTotal.heading(), 
				zTotal.heading(),
				previousIdealTipOrientation.rotation,//simulatedLocalAxes.get(segmentTip).globalMBasis.rotation.rotation,//
				12, 
				false); 


		debugTipAxes.translateTo(xTotal.p1());
		xTotal.heading(previousIdealTipOrientation.applyToCopy(Basis.xBase));
		yTotal.heading(previousIdealTipOrientation.applyToCopy(Basis.yBase));
		zTotal.heading(previousIdealTipOrientation.applyToCopy(Basis.zBase));
		debugTipAxes.globalMBasis.setXHeading(xTotal.heading(), false);
		debugTipAxes.globalMBasis.setYHeading(yTotal.heading(), false);
		debugTipAxes.globalMBasis.setZHeading(zTotal.heading(), true);
		debugTipAxes.getParentAxes().setToLocalOf(debugTipAxes.globalMBasis, debugTipAxes.localMBasis);
		debugTipAxes.markDirty();

		sgRayd[] result = {xTotal.getRayScaledBy(xWeight), 
				yTotal.getRayScaledBy(yWeight), 
				zTotal.getRayScaledBy(zWeight)};

		return result;
	}

	public Rot extractIdealRotation(
			SGVec_3d xDir, 
			SGVec_3d yDir,
			SGVec_3d zDir, 
			MRotation referenceOrientation, 
			int maxIter,
			boolean safetyCheck) {
		MRotation q = referenceOrientation == null ? MRotation.IDENTITY : referenceOrientation.copy();
		for (int iter = 0; iter < maxIter; iter++) {
			Matrix3d R = q.getMatrix();
			SGVec_3d col0 = R.col(0).crs(xDir);
			SGVec_3d col1 = R.col(1).crs(yDir);
			SGVec_3d col2 =R.col(2).crs(zDir); 
			SGVec_3d sum = col0.addCopy(col1).add(col2);
			double mag = 	Math.abs(
					R.col(0).dot(xDir) 
					+ R.col(1).dot(yDir) 
					+ R.col(2).dot(zDir));
			SGVec_3d omega = sum.multCopy(1d/mag+1.0e-9);					
			double w = omega.mag();
			if (w < 1.0e-9)
				break;			
			q = q.multiply(new MRotation(omega.multCopy(1d/w), w));					
			q= q.normalize();
		}
		return new Rot(q);
	}



	/**
	 * 
	 * @param chainMember
	 * @return returns the segment chain (pinned or unpinned, doesn't matter) to which the inputBone belongs. 
	 */
	public SegmentedArmature getChainFor(AbstractBone chainMember) {
		//AbstractBone candidate = this.segmentTip; 
		SegmentedArmature result = null; 
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
		for(SegmentedArmature c : pinnedDescendants) {
			recursivelyAlignSimAxesRootwardFrom(c.segmentTip);
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
		if(chain != null && chain.simAligned) {			
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
	 * popultes the given arraylist with the rootmost unprocessed chains of this segmented armature 
	 * and its descnedants up until their pinned tips. 
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
