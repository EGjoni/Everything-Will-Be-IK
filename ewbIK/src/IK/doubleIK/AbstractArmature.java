/**

Copyright (c) 2019 Eron Gjoni

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
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;

import IK.doubleIK.StrandedArmature.Strand;
import data.EWBIKLoader;
import data.EWBIKSaver;
import data.JSONObject;
import data.Saveable;
import sceneGraph.*;
import sceneGraph.math.doubleV.*;
import sceneGraph.math.doubleV.AbstractAxes;
import sceneGraph.math.doubleV.Rot;
/**
 * @author Eron Gjoni
 *
 */

public abstract class AbstractArmature implements Saveable {


	/**This solver aims to minimize the total distance to the target pins, however if the pins
	 * are impossible to reach AND the armature is under-constrained, this solver can become unstable. 
	 * 
	 * That said, it is quite stable when the chain armature is sufficiently constrained. 
	 * That is -- it is stable if you constrain all bones affected by the solver with Kusudamas such that 
	 * full 360 degree rotations on any axis are disallowed.*/
	public final static int AMBITIOUS = 0; 

	/** 
	 * Highly stable even when under-constrained, and also when over-constrained (by Kusudamas)
	 *
	 * Converges on a solution satisfying all pins and constraints where possible. 
	 * converges on something that looks like a polite but not especially strenuous
	 * effort at reaching the target pins when impossible. 
	 * 
	 * This solver is good if you care more about stability than minimizing total distance to
	 * target pins.  
	 * 
	 * IKchain is solved starting from the current bone, and traveling rootward until a pinned bone 
	 * is encountered. If the entire armature only contains one pin, the entire armature gets translated
	 * to meet the requirements of that pin. */
	public final static int TRANQUIL = 1; 

	/**
	 * Applies alternating solutions of AMBITIOUS and TRANQUIL solvers. 
	 * This is about twice as slow for very little improvemet, and should be avoided unless 
	 * you're trying to do fancy error correction stuff. 
	 */
	public final static int MIXED = 2; 


	/**
	 * The orientation aware solver attempts to account for target orientation, and not just position. 
	 * This solver tends to give the best quality when it works, however, it is slower than the other solvers, 
	 * and should be used with care, as it is unstable when trying to solve impossible requests. In other words 
	 * you should only use this when you are reasonably sure your targets are reachable. 
	 * 
	 * This solver allows some versatility in terms of quality / stability / performance tradeoffs. In particular
	 * at the cost of quality, you may gain some speed and stability by disabling either of 
	 * setSatifiesOrientation() or setSatisfiedTwist(). 
	 * 
	 * Disabling both causes this solver to be roughly equivalent to the tranquil solver.
	 * 
	 * This solver's poses will only look natural if the armature is naturally constrained. 
	 */
	public final static int ORIENTATIONAWARE = 4;

	protected AbstractAxes localAxes;
	protected AbstractAxes tempWorkingAxes;
	protected ArrayList<AbstractBone> bones = new ArrayList<AbstractBone>();
	protected HashMap<String, AbstractBone> boneMap = new HashMap<String, AbstractBone>();
	protected AbstractBone rootBone;
	public SegmentedArmature segmentedArmature;
	public StrandedArmature strandedArmature;
	protected String tag;

	protected int IKType = ORIENTATIONAWARE; 
	protected int IKIterations = 15;
	protected double dampening = Math.toRadians(10d);
	private boolean abilityBiasing = false;

	public double IKSolverStability = 0d; 
	PerformanceStats performance = new PerformanceStats(); 



	public AbstractArmature() {}

	public AbstractArmature(AbstractAxes inputOrigin, String name) {
		this.localAxes = inputOrigin; 
		this.tempWorkingAxes = localAxes.getGlobalCopy();
		this.tag = name;
		createRootBone(localAxes.y_().heading(), localAxes.z_().heading(), tag+" : rootBone", 1d, AbstractBone.frameType.GLOBAL);

	}



	public AbstractBone createRootBone(AbstractBone inputBone) {
		this.rootBone = inputBone;
		this.segmentedArmature = new SegmentedArmature(rootBone);
		this.strandedArmature = new StrandedArmature(rootBone);
		fauxParent = rootBone.localAxes().getGlobalCopy();
		errorBaseAxes = rootBone.localAxes().getGlobalCopy();
		errorTipAxes = errorBaseAxes.getGlobalCopy();
		return rootBone;
	}

	private AbstractBone createRootBone(SGVec_3d tipHeading, SGVec_3d rollHeading, String inputTag, double boneHeight, AbstractBone.frameType coordinateType) {
		initializeRootBone(this, tipHeading, rollHeading, inputTag, boneHeight, coordinateType);
		this.segmentedArmature = new SegmentedArmature(rootBone);
		this.strandedArmature = new StrandedArmature(rootBone);
		fauxParent = rootBone.localAxes().getGlobalCopy();
		errorBaseAxes = rootBone.localAxes().getGlobalCopy();
		errorTipAxes = errorBaseAxes.getGlobalCopy();
		return rootBone;
	}

	protected abstract void initializeRootBone(AbstractArmature armature, 
			SGVec_3d tipHeading, SGVec_3d rollHeading, 
			String inputTag, 
			double boneHeight, 
			AbstractBone.frameType coordinateType);

	public void setDefaultIterations(int iter) {
		this.IKIterations = iter;
	}


	/**
	 * MIXED MODE APPLIES ONE ITERATION OF 
	 * TRANQUIL TO GET A STABLE STARTING TEMPLATE
	 * AND ONE ITERATION OF AMBITIOUS TO GET 
	 * STRONG MATCH. HOWEVER, THIS IS NECESSARILY 
	 * SLOWER THAN JUST USING ONE OR THE OTHER
	 */

	public void setDefaultIKType(int type) {
		IKType = type;
		if(IKType < 0 || IKType > 4) IKType = 4;
	}

	public void setDefaultDampening(double damp) {
		this.dampening = Math.max(Math.abs(Double.MIN_VALUE), Math.abs(damp)); 
	}

	/**
	 * @return the rootBone of this armature.
	 */
	public AbstractBone getRootBone() {
		return rootBone;
	}

	/**
	 * (warning, this function is untested)
	 * @return all bones belonging to this armature.
	 */
	public ArrayList<AbstractBone> getBoneList() {
		this.bones.clear();
		rootBone.addDescendantsToArmature();
		return bones;
	}

	/**
	 * The armature maintains an internal hashmap of bone name's and their corresponding
	 * bone objects. This method should be called by any bone object if ever its 
	 * name is changed.
	 * @param bone 
	 * @param previousTag
	 * @param newTag
	 */
	protected void updateBoneTag(AbstractBone bone, String previousTag, String newTag) {
		boneMap.remove(previousTag);
		boneMap.put(newTag, bone);
	}

	/**
	 * this method should be called by any newly created bone object if the armature is
	 * to know it exists. 
	 * @param bone
	 */
	public void addToBoneList(AbstractBone abstractBone) {
		if(!bones.contains(abstractBone)) {
			bones.add(abstractBone);
			boneMap.put(abstractBone.tag, abstractBone);
		}
	}

	/**
	 * this method should be called by any newly deleted bone object if the armature is
	 * to know it no longer exists
	 */
	public void removeFromBoneList(AbstractBone abstractBone) {
		if(bones.contains(abstractBone)) {
			bones.remove(abstractBone);
			boneMap.remove(abstractBone);
			this.updateArmatureSegments();
		}
	}

	/**
	 * 
	 * @param tag the tag of the bone object you wish to retrieve
	 * @return the bone object corresponding to this tag
	 */

	public AbstractBone getBoneTagged(String tag) {
		return boneMap.get(tag);	
	}

	/**
	 * 
	 * @return the user specified tag String for this armature.
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * @param A user specified tag string for this armature.
	 */
	public void setTag(String newTag) {
		this.tag = newTag;
	}


	protected boolean satisfyTwist = true;
	protected boolean satisfyOrientation = true;
	protected boolean inverseWeighted = false;

	/**
	 * This option is is only applicable to the orientation aware solver its default value is "true".  
	 * @return if true, performs an additional bi-directional pass on the chain to account for the desired
	 * twist of the target pin. (for example, if a character is attempting to look behind itself, the but the 
	 * constraints on the neck bone disallow it, this will cause the spine to twist and accomodate)
	 */	

	public boolean isSatisfyTwist() {
		return satisfyTwist; 
	}

	/**
	 * This value is only relevant to the orientation aware solver.
	 * @return if true, performs an additional pass on the chain attempting to 
	 * account for the desired pin direction. For example, if the target pin is attempting to make
	 * a character look down, but the constraints on the characters neck disallow it, this will allow 
	 * the spine to compensate by straightening up and outward to give the neck bone more leeway.
	 */

	public boolean isSatisfyOrientation() {
		return satisfyOrientation; 
	}


	/**
	 * This option is is only applicable to the orientation aware solver its default value is "true".  
	 * @param satisfyTwist if true, performs an additional bi-directional pass on the chain to account for the desired
	 * twist of the target pin. (for example, if a character is attempting to look behind itself, the but the 
	 * constraints on the neck bone disallow it, this will cause the spine to twist and accomodate)
	 */	
	public void setSatifyTwist(boolean satisfyTwist) {
		this.satisfyTwist = satisfyTwist; 
	}


	/**
	 * This option is is only applicable to the orientation aware solver its default value is "true".  
	 * @param satisfyTwist if true, performs an additional pass on the chain attempting to 
	 * account for the desired pin direction. For example, if the target pin is attempting to make
	 * a character look down, but the constraints on the characters neck disallow it, this will allow 
	 * the spine to compensate by straightening up and outward to give the neck bone more leeway.
	 */

	public void setSatisfyOrientation(boolean satisfyOrientation) {
		this.satisfyOrientation = satisfyOrientation;
	}


	/**
	 * @param inverseWeighted  if true, will apply an additional rotation penalty on the
	 * peripheral bones near a target so as to result in more natural poses with less need for dampening. 
	 */
	public void setInverseWeighted(boolean inverseWeighted) {
		this.inverseWeighted = inverseWeighted;
	}

	public boolean isInverseWeighted() {
		return this.inverseWeighted;
	}

	/**
	 * this method should be called whenever a bone 
	 * in this armature has been pinned or unpinned.
	 * 
	 * for the most part, the abstract classes call this when necessary. 
	 * But if you are extending classes more than you would reasonably expect
	 * this library to reasonably expect and getting weird results, you might try calling 
	 * this method after making any substantial structural changes to the armature.
	 */
	public void updateArmatureSegments() {
		segmentedArmature.updateSegmentedArmature();
		strandedArmature.updateStrandedArmature();
	}

	/**
	 * If you have created some sort of save / load system 
	 * for your armatures which might make it difficult to notify the armature
	 * when a pin has been enabled on a bone, you can call this function after 
	 * all bones and pins have been instantiated and associated with one another 
	 * to index all of the pins on the armature. 
	 */
	public void refreshArmaturePins() {
		AbstractBone rootBone = this.getRootBone();
		ArrayList<AbstractBone> pinnedBones = new ArrayList<>(); 
		rootBone.addSelfIfPinned(pinnedBones);

		for(AbstractBone b : pinnedBones) {
			b.notifyAncestorsOfPin(false);
			updateArmatureSegments();
		}
	}


	private void insertIntoSortedChainList(ArrayList<SegmentedArmature> chainList, SegmentedArmature sa) {
		if(chainList.indexOf(sa) == -1) {
			int insertAt = -1;
			for(int i =0; i<chainList.size(); i++) {
				if(chainList.get(i).distanceToRoot < sa.distanceToRoot) {
					insertAt = i;
					break;
				}
			}
			if(insertAt == -1) chainList.add(sa);
			else chainList.add(insertAt, sa);
		}
	}


	/**
	 * automatically solves the IK system of this armature from the
	 * given bone using the armature's default IK parameters. 
	 * 
	 * You can specify these using the setDefaultIterations() setDefaultIKType() and setDefaultDampening() methods.
	 * The library comes with some defaults already set, so you can more or less use this method out of the box if 
	 * you're just testing things out. 
	 * @param bone
	 */
	public void IKSolver(AbstractBone bone) {
		IKSolver(bone, dampening, IKIterations);
	}

	public void IKSolver(AbstractBone bone, double dampening, int iterations) {

		performance.resetPerformanceStat();
		performance.startPerformanceMonitor();
		if(this.IKType == AMBITIOUS) {
			ambitiousIKSolver(bone, dampening, iterations);
		} else if(this.IKType == TRANQUIL) {
			tranquilIKSolver(bone, dampening, iterations);
		} else if(this.IKType == MIXED) {
			tranquilIKSolver(bone, dampening, iterations);
			ambitiousIKSolver(bone, dampening, iterations);
		} else if(this.IKType == ORIENTATIONAWARE) {
			//orientationAwareSolver(bone, dampening, iterations, isInverseWeighted(), isSatisfyOrientation(), isSatisfyTwist());
			improvedSolver(bone, dampening, iterations);
		}
		performance.solveFinished();
		performance.printStats();
	}

	/**
	 * Same as other ambitiousIKSolver method, but uses the armature's default iteration and dampening. 
	 * @param bone
	 */
	public void ambitiousIKSolver(AbstractBone bone) {
		ambitiousIKSolver(bone, dampening, IKIterations);
	}


	/**
	 * This solver aims to minimize the total distance to the target pins, however if the pins
	 * are impossible to reach AND the armature is under-constrained, this solver can become unstable. 
	 * 
	 * That said, it is quite stable when the chain armature is sufficiently constrained. 
	 * That is -- it is stable if you constrain all bones affected by the solver with Kusudamas such that 
	 * full 360 degree rotations on any axis are disallowed.
	 * 
	 * @param bone the bone from which to begin solving the IK system. Pinned ancestors of this bone
	 * will not be modifed. Pinned children will however, will also have this solver called on them, recursively.  
	 * @param dampening the maximum number of radians any bone is allowed to rotate per iteration.
	 * a lower number gives more evenly distributed results, but may take a larger number of iterations 
	 * before it converges on a solution. if you don't know what you want, try starting with 0.1.
	 * @param iterations the number of times to run the IK loop before returning an answer. 
	 */
	public void ambitiousIKSolver(AbstractBone bone, double dampening, int iterations) {
		SegmentedArmature thisChain = segmentedArmature.getChainFor(bone); 
		if(thisChain != null) {
			SegmentedArmature startFrom = thisChain.getPinnedRootChainFromHere();
			//if(startFrom != null) {
			for(int i = 0; i < iterations; i++) {
				if(startFrom != null /*&& (startFrom.basePinned || startFrom.tipPinned)*/) 
					solveIK(startFrom, dampening, 1);//iterations);
				else if (thisChain != null && thisChain.isTipPinned() || thisChain.pinnedDescendants.size() > 0)
					solveIK(segmentedArmature, dampening, 1);//iterations);
			}
			//}
		}// if there's only one pin, the armature automatically gets translated
		//to meet its requirement as an inherent part of the algorithm anyway, and I can't imagine any instance where it is not 
		//acceptable to do so. so, doing so.
	}




	protected void solveIK(SegmentedArmature chain, double dampening, int iterations) {
		ArrayList<SegmentedArmature> pinnedChains = chain.pinnedDescendants;

		if(!chain.isBasePinned()) {
			SGVec_3d translateBy = new SGVec_3d(0,0,0);
			for(SegmentedArmature pc : pinnedChains) {
				sgRayd tipToTargetRay = new sgRayd(pc.segmentTip.getTip_(), pc.segmentTip.pinnedTo());
				translateBy.add((SGVec_3d)tipToTargetRay.heading());
			}
			translateBy.div((double)pinnedChains.size());
			segmentedArmature.segmentRoot.localAxes.translateByGlobal(translateBy);	
		}

		solveIKChainList(pinnedChains, dampening, iterations); 

		for(SegmentedArmature pc : pinnedChains) {
			for(SegmentedArmature pccs: pc.childSegments) {
				solveIK(pccs, dampening, iterations);
			}
		}

	}

	protected void solveIKChainList(ArrayList<SegmentedArmature> chains, double dampening, int iterations) {
		ArrayList<SegmentedArmature> parentChains = new ArrayList<SegmentedArmature>();

		for(SegmentedArmature c : chains) {
			solveIKChain(c, dampening, iterations);
			if(c.getParentSegment() != null && !c.getParentSegment().isTipPinned() && !c.isBasePinned()) {
				insertIntoSortedChainList(parentChains, c.getParentSegment());
			}
		}

		if(parentChains.size() != 0) {
			solveIKChainList(parentChains,dampening, iterations);
		}
		for(SegmentedArmature sa : parentChains) {
			for(AbstractBone b : sa.boneRotationMap.keySet()) {
				b.IKUpdateNotification();
			}
		}
	}


	protected void solveIKChain(SegmentedArmature chain, double dampening, int iterations) {		 
		//System.out.println("\n CHAIN ITERATION \n");
		for(int i = 0 ; i<iterations; i++) {
			iterateCCD(dampening, chain); 
		}
	}



	private void iterateCCD(double dampening, SegmentedArmature chain) {
		AbstractBone tipBone = chain.segmentTip; 
		AbstractBone currentBone = tipBone;
		ArrayList<Rot> rotations = new ArrayList<Rot>();

		sgRayd currentRay = new sgRayd(); 
		sgRayd goalRay = new sgRayd(); 

		//tempWorkingAxes.alignGlobalsTo(tipBone.localAxes());
		//tempWorkingAxes.translateTo(tipBone.getTip());

		for(int i=0; i<=chain.chainLength; i++) {
			//first check to see if the IK system is allowed to modify the orientation of this bone
			if(!currentBone.getIKOrientationLock()) {
				rotations.clear();
				/*if(currentBone.constraints != null) { 
				currentBone.constraints.limitingAxes.globalCoords = currentBone.constraints.limitingAxes.relativeTo(currentBone.constraints.limitingAxes.parent.globalCoords);
			}*/

				for(SegmentedArmature pinnedTarget : chain.pinnedDescendants) {
					currentRay.setP1(currentBone.getBase_()); 
					currentRay.setP2(pinnedTarget.segmentTip.getTip_());

					goalRay.setP1(currentBone.getBase_()); 
					goalRay.setP2(pinnedTarget.segmentTip.pinnedTo());
					rotations.add(new Rot(currentRay.heading(), goalRay.heading()));
				}  


				Rot rotateToTarget = G.averageRotation(rotations);

				double angle = rotateToTarget.getAngle(); 
				SGVec_3d axis = rotateToTarget.getAxis();

				angle = Math.min(angle, dampening);
				currentBone.rotateBy(new Rot(axis, angle));   
			}

			currentBone.snapToConstraints();   
			currentBone = currentBone.parent;

		}
	}

	/**
	 * Same as other tranquilIKSolver method, but uses the armature's default iteration and dampening. 
	 * @param bone
	 */
	public void tranquilIKSolver(AbstractBone bone) {
		tranquilIKSolver(bone, dampening, IKIterations);
	}


	/** 
	 * Highly stable even when under-constrained, and also when over-constrained (by Kusudamas)
	 *
	 * Converges on a solution satisfying all pins and constraints where possible. 
	 * converges on something that looks like a polite but not especially strenuous
	 * effort at reaching the target pins when impossible. 
	 * 
	 * This solver is good if you care more about stability than minimizing total distance to
	 * target pins.  
	 * 
	 * IKchain is solved starting from the current bone, and traveling rootward until a pinned bone 
	 * is encountered. If the entire armature only contains one pin, the entire armature gets translated
	 * to meet the requirements of that pin. 
	 * 
	 * @param bone the bone from which to begin solving the IK system. Pinned ancestors of this bone
	 * will not be modifed. Pinned children will however have this solver called on them -- recursively.  
	 * @param dampening the maximum number of radians any bone is allowed to rotate per iteration.
	 * a lower number gives more evenly distributed results, but may take a larger number of iterations 
	 * before it converges on a solution. if you don't know what you want, try starting with 0.1.
	 * @param iterations the number of times to run the IK loop before returning an answer. 
	 */
	public void tranquilIKSolver(AbstractBone bone, double dampening, int iterations) {
		IKSolverStability = 0d;
		StrandedArmature thisStrandCollection = strandedArmature.getStrandCollectionFor(bone);
		if(thisStrandCollection != null) {
			recursivelyCallAlternateSolver(thisStrandCollection, dampening, iterations);
			for(AbstractBone b : thisStrandCollection.allBonesInStrandCollection) {
				b.IKUpdateNotification();
			}
		}	

	}

	private void recursivelyCallAlternateSolver(StrandedArmature starm, double dampening, int iterations) {		
		if(starm!= null) {
			starm.resetStabilityMeasures();
			alternateIKSolver(starm, dampening, iterations);
			double collectionStability = starm.getStability(); 
			IKSolverStability = Math.max(IKSolverStability, collectionStability);
		}
		for(StrandedArmature sa: starm.childCollections) {
			recursivelyCallAlternateSolver(sa, dampening, iterations);
		}
	}

	protected void alternateIKSolver(StrandedArmature collection, double dampening, int iterations) {
		ArrayList<Strand> strands = collection.strands;
		/**
		 * Go through each strand, and do an iteration of CCD on it. 
		 * For each strand, add to its rotationHashMap for each bone the rotational difference
		 * between its current orientation, and its original orientation 
		 * then reset the bone orientations of all bones in the collections back to their original orientations;
		 * (as an optimization, only reset the orientation if the bone is mapped to multiple strands in boneStrandsMap);
		 */		

		for(int i =0; i<iterations; i++) {
			if(!collection.isBasePinned()) {
				SGVec_3d translateBy = new SGVec_3d(0,0,0);
				sgRayd tipToTargetRay = new sgRayd(translateBy.copy(), null);
				for(Strand s : strands) {
					tipToTargetRay.setP1(s.getStrandTip().getTip_());
					tipToTargetRay.setP2(s.getStrandTip().pinnedTo());
					translateBy.add((SGVec_3d)tipToTargetRay.heading());
				}
				translateBy.div((double)strands.size());
				segmentedArmature.segmentRoot.localAxes.translateByGlobal(translateBy);	
			}	

			for(Strand s: strands) {
				/*
				 * Kinda hacky to have iterateCCDStrand 
				 * actually reach into the strand and make changes on the fly, 
				 * but I'm shooting for speed over readability,
				 * and this allows me to avoid copying a bunch of axes objects.
				 */
				iterateCCDStrand(s, dampening);
				s.revertRotations();
				//s.updateDistToTarget();
				//collection.alignMultiStrandBonesToOriginalAxes();				
			}
			applyAverageWeightedRotations(collection);
			collection.updateStabilityEstimates();
			//collection.refreshOriginalAxesMap();
		}

	}



	Rot ir = new Rot(new SGVec_3d(1,1,1), 0);//strandList.get(0).rotationsMap.get(b);
	Quaternion initialQ = new Quaternion(ir.rotation.getQ0(), ir.rotation.getQ1(), ir.rotation.getQ2(), ir.rotation.getQ3());

	private void applyAverageWeightedRotations(StrandedArmature collection) {

		for(AbstractBone b : collection.allBonesInStrandCollection) {
			ArrayList<Strand> strandList = collection.boneStrandMap.get(b);


			double totalCount = 0;
			double wT = 0;
			double xT = 0; 
			double yT = 0; 
			double zT = 0;

			//totalDist = collection.totalPinDist;

			double totalFreedomForBone = 0d;
			for(Strand s : strandList) {

				//double distance = s.distToTarget; 					
				Rot r = s.rotationsMap.get(b);
				//r = new Rot(r.getAxis(), r.getAngle()*(distance/totalDist));
				Quaternion current = G.getSingleCoveredQuaternion(
						new Quaternion(r.rotation.getQ0(), 
								r.rotation.getQ1(),
								r.rotation.getQ2(), 
								r.rotation.getQ3()), 
						initialQ);

				/*wT += current.getQ0();
				xT += current.getQ1();
				yT += current.getQ2();
				zT += current.getQ3();

				totalCount ++;*/

				double freedomForBoneThisStrand = getAbilityBiasing() ? s.getRemainingFreedomAtBone(b) : 1d; 
				double weight = 1d/freedomForBoneThisStrand;
				totalFreedomForBone += weight;
				//totalFreedomForBone += freedomForBoneThisStrand;				
				wT += (current.getQ0() * weight);
				xT += (current.getQ1() * weight);
				yT += (current.getQ2() * weight);
				zT += (current.getQ3() * weight);
				//totalDist += distance;
			}
			Rot avg = new Rot(wT/totalFreedomForBone, xT/totalFreedomForBone, yT/totalFreedomForBone, zT/totalFreedomForBone, true);
			b.rotateBy(avg);

			//TODO DEBUG: TEST WHAT HAPPENS IF I ENABLE SNAPPING TO CONSTRAINTS HERE

			//b.snapToConstraints();
			collection.setDeltaMeasureForBone(b, avg.getAngle());
		}
	}	

	private void iterateCCDStrand(Strand chain, double dampening) {

		//SGVec_3d strandTip = chain.strandTip.getTip();
		SGVec_3d strandTipPin = chain.getStrandTip().pinnedTo();
		sgRayd currentRay = new sgRayd(new SGVec_3d(0,0,0), null);
		sgRayd goalRay = new sgRayd(new SGVec_3d(0,0,0), null);

		SGVec_3d origXHead = new SGVec_3d(0,0,0);
		SGVec_3d origYHead = new SGVec_3d(0,0,0);
		SGVec_3d postXHead = new SGVec_3d(0,0,0);
		SGVec_3d postYHead = new SGVec_3d(0,0,0);

		for(AbstractBone currentBone : chain.bones) {

			currentRay.setP1(currentBone.getBase_()); 
			currentRay.setP2(chain.getStrandTip().getTip_()); 

			goalRay.setP1(currentRay.p1());
			goalRay.setP2(strandTipPin); 

			origXHead = (SGVec_3d) currentBone.localAxes().x_().heading();
			origYHead = (SGVec_3d) currentBone.localAxes().y_().heading();

			Rot rotateToTarget = new Rot(currentRay.heading(), goalRay.heading());//G.averageRotation(rotations);

			double angle = rotateToTarget.getAngle();

			angle = Math.min(angle, dampening);

			currentBone.rotateBy(new Rot(rotateToTarget.getAxis(), angle));   
			currentBone.snapToConstraints();   			

			postXHead = (SGVec_3d) currentBone.localAxes().x_().heading(); 
			postYHead = (SGVec_3d) currentBone.localAxes().y_().heading();

			Rot totalRotation = new Rot(origXHead, origYHead, postXHead, postYHead);

			chain.rotationsMap.put(currentBone, totalRotation);

			//currentBone = currentBone.parent;
		}
	}

	/**
	 * 
	 * @return a reference to the Axes serving as this Armature's coordinate system. 
	 */
	public AbstractAxes localAxes() {
		return this.localAxes;
	}


	public void orientationAwareSolver(
			AbstractBone bone, 
			double dampening, 
			int iterations,
			boolean inverseWeighting,
			boolean orientationAware, 
			boolean twistAware) {
		IKSolverStability = 0d;
		StrandedArmature thisStrandCollection = strandedArmature.getStrandCollectionFor(bone);
		if(thisStrandCollection != null && thisStrandCollection.strands.size() > 0) {
			thisStrandCollection.alignSimulationAxesToBones();
			int outerIterations = Math.max(1, iterations/thisStrandCollection.strands.size());
			int innerIterations = iterations / outerIterations;
			innerIterations += Math.max(0, (iterations - (outerIterations*innerIterations))); 
			for(int i =0; i<iterations; i++) {
				if(thisStrandCollection != null) {
					recursivelyCallFabriCCDSolver(thisStrandCollection, dampening, 1, inverseWeighting, orientationAware, true);		
				}	
			}		
			thisStrandCollection.alignBonesToSimulationAxes();

			recursivelyNotifyBonesOfCompletedIKSolution(thisStrandCollection);

		}
	}

	private void recursivelyNotifyBonesOfCompletedIKSolution(StrandedArmature startFrom) {
		for(AbstractBone b : startFrom.allBonesInStrandCollection) {
			b.IKUpdateNotification();
		} 
		for(StrandedArmature childStrandCollection  : startFrom.childCollections)
			recursivelyNotifyBonesOfCompletedIKSolution(childStrandCollection);
	}

	private void recursivelyCallFabriCCDSolver(
			StrandedArmature starm, 
			double dampening, 
			int iterations, 
			boolean inverseWeighting,
			boolean orientationAware, 
			boolean twistAware) {		
		if(starm!= null) {
			starm.resetStabilityMeasures();
			fabriCCDSolver(starm, dampening, iterations, inverseWeighting, orientationAware, twistAware) ;
			double collectionStability = starm.getStability(); 
			IKSolverStability = Math.max(IKSolverStability, collectionStability);
		}
		for(StrandedArmature sa: starm.childCollections) {
			recursivelyCallFabriCCDSolver(sa, dampening, iterations, inverseWeighting, orientationAware, twistAware);
		}


	}

	/**
	 * @param collection
	 * @param dampening
	 * @param iterations
	 * @param inverseWeighting if true, will apply an additional rotation penalty on the
	 * peripheral bones near a target so as to result in more natural poses with less need for dampening. 
	 * @param orientationAware if true, performs an additional pass on the chain attempting to 
	 * account for the desired pin directions. (for example, if the target pin is attempting to make
	 * a character look down, but the constraints on the characters neck disallow it, this will allow 
	 * the spine to compensate by straightening up and outward to give the neck bone more leeway)
	 * @param twistAware if true, performs an additional bi-directional pass on the chain to account for the desired
	 * twist of the target pin. (for example, if a character is attempting to look behind itself, the but the 
	 * constraints on the neck bone disallow it, this will cause the spine to twist and accomodate)
	 */
	protected void fabriCCDSolver(
			StrandedArmature collection, 
			double dampening, 
			int iterations,
			boolean inverseWeighting,
			boolean orientationAware,
			boolean twistAware) {
		collection.updateSimulatedStrandRootParentAxes();
		ArrayList<Strand> strands = collection.strands;
		/**
		 * Go through each strand, and do an iteration of CCD on it. 
		 * For each strand, add to its rotationHashMap for each bone the rotational difference
		 * between its current orientation, and its original orientation 
		 * then reset the bone orientations of all bones in the collections back to their original orientations;
		 * (as an optimization, only reset the orientation if the bone is mapped to multiple strands in boneStrandsMap);
		 */		

		SGVec_3d base = new SGVec_3d(); 

		for(int i = 0; i< iterations; i++) {
			for(Strand s: strands) {
				s.alignSimulationAxesToBones();
				SGVec_3d fauxTip = new SGVec_3d(0d, s.getStrandTip().getBoneHeight(), 0d);				
				AbstractAxes strandTargetAxes = s.getStrandTip().getPinnedAxes();				
				AbstractAxes simulatedTipAxes = s.simulatedLocalAxes.get(s.getStrandTip())[0]; simulatedTipAxes.updateGlobal();
				AbstractKusudama constraintTip =  ((AbstractKusudama)s.getStrandTip().getConstraint());
				simulatedTipAxes.alignOrientationTo(strandTargetAxes);	
				if(orientationAware) {				
					saneCCD(dampening, s, fauxTip, s.getStrandTip().getBoneHeight(), s.getStrandTip(), s.getStrandRoot(), inverseWeighting, true); 				
					//simulatedTipAxes.alignOrientationTo(strandTargetAxes);					
					if(twistAware) 
						fabriTwist(s, s.getStrandRoot());
					if(constraintTip != null) 
						constraintTip.setAxesToOrientationSnap(simulatedTipAxes, s.simulatedConstraintAxes.get(s.getStrandTip())[0], constraintTip.getStrength());					
					saneCCD(dampening, s, base, 0d, s.getStrandTip().parent, s.getStrandRoot(), inverseWeighting, true);			

				} else {
					saneCCD(dampening, s, base, s.getStrandTip().getBoneHeight(), s.getStrandTip(), s.getStrandRoot(), inverseWeighting, true);
				}			

				if(twistAware) 
					fabriTwist(s, s.getStrandRoot());

				if(constraintTip != null) 
					constraintTip.setAxesToOrientationSnap(simulatedTipAxes, s.simulatedConstraintAxes.get(s.getStrandTip())[0], constraintTip.getStrength());

			}
			applyAverageWeightedRotationsToSimulationAxes(collection, true);

		}
		strandedArmature.translateToAverageTipError(true);	
		//applyAverageWeightedRotationsToSimulationAxes(collection, true);
		//strandedArmature.translateToAverageTipError(true);			
	}


	public void improvedSolver(AbstractBone startFrom, double dampening, int iterations) {
		SegmentedArmature armature = segmentedArmature.getChainFor(startFrom);		
		if(debug) iterations = 1;
		if(armature != null) {
			while(!armature.isBasePinned()) {
				if(armature.getParentSegment() == null) 
					break;
				armature = armature.getParentSegment(); 
			}
			for(int i = 0; i<iterations; i++) {
				armature.alignSimulationAxesToBones();
				if(armature.getParentSegment() == null && armature.segmentTip.getPinnedAxes() != null) {
					SGVec_3d baseTranslate = armature.segmentTip.getPinnedAxes().origin_().subCopy(
																				armature.simulatedLocalAxes.get(armature.segmentTip).origin_());
					armature.simulatedLocalAxes.get(armature.segmentRoot).translateByGlobal(baseTranslate);
					armature.simulatedConstraintAxes.get(armature.segmentRoot).translateByGlobal(baseTranslate);
				}
				
				recursivelyCallImprovedSolver(armature, dampening, iterations);
				
				//TODO: make this use temporary simulated boneAxes until all iterations have completed. 
				armature.recursivelyAlignBonesToSimAxesFrom(armature.segmentRoot);
			}
		}
	}

	/**given a segmented armature, solves each chain from its pinned 
	 * tips down to its pinned root. Then call itself again on any segmented armatures 
	 * which are children of the pinned tips.
	 * @param armature
	 */
	public void recursivelyCallImprovedSolver(SegmentedArmature armature, double dampening, int iterations) {
		if(armature.childSegments == null && !armature.isTipPinned()) {
			return; 
		} else if(!armature.isTipPinned()) {
			for(SegmentedArmature c: armature.childSegments) {
				recursivelyCallImprovedSolver(c, dampening, iterations);
			}
		}
		planarCCD(armature, dampening,  true, true, true, false);
	}

	/*
	 * New Approach idea (still implementing)
	 * In most cases, stability in  unsolvable situations is preferable to completeness over all solvable ones. 
	 * So it is better to have one poorly convergent solution than many divergent solutions. 
	 * 
	 *  Since we want orientation awareness, we can treat twist, direction, and location as separate effector types we are trying to 
	 *  satisfy. Ultimately though, we want to minimize the disagreement between these three solution types before averaging.
	 *  So it won't do to solvethem all from the same base solution. It would (probably) be much more stable to start one solution 
	 *  from the rest post of the previous solution, and THEN average them. So our input simulatedAxes and our output simulatedAxes 
	 *  are not necessarily going to be the same. Their target Axes however, are. 
	 *  
	 * All bones in the collection are already oredered by their depth from the root, so it should be safe to reference strands per bone. 
	 * So our procedure is 
	 * 	for, each bone  
	 * 		get the average rotation and position of the strand tips across all strands.  (should average be nlerp or axis components)?  
	 * 		get the average rotation and position of the strand target across all strands. (should average be nlerp or axis components)?
	 * 		solve bone to get average strand tip base-position to average strand target position. [vector -> vector solution]
	 * 		store the solution
	 * 		exit loop.
	 * 	for each bone 
	 * 		get the average rotation and position of the strand tips as per the previous solution across all strands.
	 * 		get the average rotation and position of the strand target across all strands.
	 * 		solve bone to get average strand tip from base->y to average strand target position from base->y. [plane -> plane solution (use custom precedent-respecting version?)]
	 * 		store the solution in new array element.
	 * 		exit loop. 
	 * 	for each bone
	 * 		get the average rotation and position of the strand tips as per the previous solution across all strands .
	 * 		get the average rotation and position of the strand target across all strands.
	 * 		solve bone to bring average strand tip base->z to average strand target position base->z. [plane -> plane solution (use custom precedent-respecting version?)]
	 * 		store the solution in new array element.
	 * 		exit loop. 
	 *		(optional?) for each bone
	 * 		get the average rotation and position of the strand tips as per the previous solution across all strands .
	 * 		get the average rotation and position of the strand target across all strands.
	 * 		solve bone to bring average strand tip base->x to average strand target position base->x. [plane -> plane solution (use custom precedent-respecting version?)]
	 * 		store the solution in new array element.  
	 *		for each bone 
	 *			average all of the solutions 
	 *
	 *		Actually, StrangedArmature is probably overkill for this approach. 
	 */

	boolean debug = true;
	AbstractBone lastDebugBone = null; 

	/**
	 * Performs ccd attempting if possible (depending on input) to rotate one triangle onto another instead of one ray onto another.
	 * see {@link IK.doubleIK.AbstractArmature.contextualPlanarRotation} for more information about the rotation procedure. 
	 */
	private void planarCCD(
			SegmentedArmature chain, 
			double dampening, 
			boolean orientationAware, 
			boolean twistAware, 
			boolean useRedundancy, // might improve convergence and stability in some situations, but slower
			boolean inverseWeighting) {

		debug =false;
		double passCount = 0; 
		short modeCode = 0; 
		if(orientationAware) {
			passCount++;
			modeCode +=1; 
		}
		if(twistAware) {
			passCount++;
			modeCode += 2; 
		}
		if(useRedundancy) {
			passCount++;
			modeCode +=4;
		}
		//lastDebugBone = null;
		AbstractBone startFrom = debug && lastDebugBone != null ? lastDebugBone :  chain.segmentTip;		
		AbstractBone stopAfter = chain.segmentRoot;
		


		AbstractBone currentBone = startFrom;
		//sgRayd[] averageTargetRays = chain.getAverageTargetOrientationAcrossAllPinnedBones(true);
		//System.out.println(averageTargetRays[0].p1());
		if(currentBone == chain.segmentTip && chain.isTipPinned()) {
			if(debug && chain.simulatedLocalAxes.size() < 2) {

			} else { 
				AbstractAxes currentBoneSimulatedAxes = chain.simulatedLocalAxes.get(currentBone); 
				currentBoneSimulatedAxes.updateGlobal();
				/*Rot dirRot = currentBoneSimulatedAxes.globalMBasis.rotation.applyInverseTo(currentBone.getPinnedAxes().globalMBasis.rotation);
			currentBoneSimulatedAxes.rotateBy(currentBone.getPinnedAxes().globalMBasis.rotation);*/	
				AbstractAxes pinAxes = currentBone.getPinnedAxes();
				pinAxes.updateGlobal();
				MRotation pinRotation = pinAxes.globalMBasis.rotation.rotation;
				Rot toPinRotation = new Rot(currentBoneSimulatedAxes.globalMBasis.rotation.rotation.getInverse().multiply(pinRotation));
				Rot clampedToPinRot = new Rot(toPinRotation.getAxis(), MathUtils.clamp(toPinRotation.getAngle(), -dampening, dampening));
				currentBoneSimulatedAxes.rotateBy(clampedToPinRot);
				currentBoneSimulatedAxes.updateGlobal();
				//currentBoneSimulatedAxes.alignOrientationTo(currentBone.getPinnedAxes());
				currentBone.setAxesToSnapped(currentBoneSimulatedAxes,  chain.simulatedConstraintAxes.get(currentBone));
				currentBoneSimulatedAxes.updateGlobal();
				currentBone = currentBone.getParent();
				/*if(debug) {
					lastDebugBone = null;//currentBone;
					currentBone = null;
				}*/
			}
		}

		if(debug && chain.simulatedLocalAxes.size() < 2) {

		} else {
			System.out.print("---------");
			while(currentBone != null && passCount > 0) {			
				chain.updateAverageRotationToPinnedDescendants(currentBone, modeCode, dampening);
				
				/*AbstractAxes currentBoneSimulatedAxes = chain.simulatedLocalAxes.get(currentBone); 
				AbstractAxes currentBoneConstraintAxes = chain.simulatedConstraintAxes.get(currentBone);
				double accumulatedq0 = 0d; 
				double accumulatedq1 = 0d; 
				double accumulatedq2 = 0d; 
				double accumulatedq3 = 0d;
				for(int mode = 0 ; mode < 3; mode++) {
					boolean skipRound = (mode==0 && ! orientationAware) || (mode==1 && !twistAware) || (mode ==2 &&  !useRedundancy);    
					if(!skipRound) {
						//if(currentBone == chain.segmentTip.parent) 
							//System.out.print((mode == 0 ? "Y" : mode == 1 ? "Z" : "X")+": ");
						int rayIndex = (mode+1)%3;
						sgRayd relevantTargetRay = averageTargetRays[rayIndex];
						sgRayd[] averageTipRays = chain.getAverageTipOrientationAcrossAllPinnedBones(true);
						double tipMag = averageTipRays[rayIndex].mag();
						double targetMag = averageTargetRays[rayIndex].mag();
						sgRayd relevantTipRay = averageTipRays[rayIndex];					

						Rot dirRot = contextualPlanarRotation(
								currentBoneSimulatedAxes.origin_(), 
								relevantTipRay.p1(), relevantTipRay.p2(),
								relevantTargetRay.p1(), relevantTargetRay.p2());


						Rot dampenedRot = new Rot(dirRot.getAxis(), MathUtils.clamp(dirRot.getAngle(), -dampening, dampening));
						currentBoneSimulatedAxes.rotateBy(dampenedRot);

						currentBone.setAxesToSnapped(currentBoneSimulatedAxes, currentBoneConstraintAxes);
						currentBoneSimulatedAxes.updateGlobal(); 

						Quaternion singleCovered =//G.getSingleCoveredQuaternion(
								G.getQuaternion(currentBoneSimulatedAxes.localMBasis.rotation);//, 
						//G.getQuaternion(currentBoneSimulatedAxes.localMBasis.rotation)
						//);

						accumulatedq0 += singleCovered.getQ0();
						accumulatedq1 += singleCovered.getQ1();
						accumulatedq2 += singleCovered.getQ2();
						accumulatedq3 += singleCovered.getQ3();

						currentBoneSimulatedAxes.alignGlobalsTo(currentBone.localAxes());
						currentBoneSimulatedAxes.markDirty(); currentBoneSimulatedAxes.updateGlobal();
					}	
				}
				Rot averagedLocalRot = new Rot(
						accumulatedq0 /passCount,
						accumulatedq1 /passCount,
						accumulatedq2 /passCount,
						accumulatedq3 /passCount,
						true);

				currentBoneSimulatedAxes.alignGlobalsTo(currentBone.localAxes());
				currentBoneSimulatedAxes.markDirty(); currentBoneSimulatedAxes.updateGlobal();
				currentBoneSimulatedAxes.localMBasis.rotateTo(averagedLocalRot);
				currentBoneSimulatedAxes.markDirty(); currentBoneSimulatedAxes.updateGlobal();*/

				if(currentBone == stopAfter) currentBone = null;
				else currentBone = currentBone.getParent();

				if(debug) { 
					lastDebugBone = currentBone; 
					break;
				}
			}
		}
	}



	/**
	 * triangles are rotated with edge precedence, meaning, instead of optimizing so that the perpendicular 
	 *bisectors of two triangles align, one edge of each triangle is defined as mandatory, and rotation 
	 *is performed with that edge as the axis of rotation to align the rest of the triangle. This
	 *planar step is optional. If a null parameter is given for any pair of edges, only the non-null edges are accounted for. 
	 *If any two vectors defining a triangle have (roughly) zero magnitude, a non-planar rotation 
	 *is performed.
	 *Additionally, the rotation is scaled in proportion to the distances between its start and target 
	 *vectors. So, for example, if the distance between startEdge1 and startEdge2 is 0.5, then the resulting rotation is halved. 
	 *This is to better account for degenerate situations, where naive averaging of multiple rotation matrices can cause 
	 *basis vectors to cancel each other out. These cancellations occur due to opposing rotations, but manifest as reflections, 
	 *so we simply treat reflections as encoding opposing rotations. This can probably be formalized, but it's not important. 
	 *
	 *If some combination of invalid inputs is given which cannot be accounted for, the identity rotation is returned (no rotation)
	 * 
	 * @param sharedOrigin a point shared by both triangles
	 * @param startEdge1 the first adjacent edge on the start triangle (emanating from the origin), 
	 * this takes precedence in the rotation and will be aligned to target edge no so long as it has greater than zero magnitude 
	 * relative to the origin
	 * @param startEdge2  the second adjacent edge on the start triangle. If null, planar rotation will not be performed. If startEdge1
	 * is of zero magnitude relative to the origin, then this is used as the only edge to rotate, (onto targetEdge2). 
	 * @param targetEdge1 the first target edge (takes precedence), 
	 * @param targetEdge2 the second target edge (takes precedence).
	 */
	protected static Rot contextualPlanarRotation(
			SGVec_3d sharedOrigin, 
			SGVec_3d startEdge1, 	SGVec_3d startEdge2, 
			SGVec_3d targetEdge1, SGVec_3d targetEdge2
			) {
		Rot result;
		SGVec_3d precedenceStartEdge, precedenceTargetEdge;
		boolean skipMinor = false;
		if(startEdge1 == null ) {
			precedenceStartEdge = startEdge2;
			precedenceTargetEdge = targetEdge2;
			skipMinor=true;
		} else {
			precedenceStartEdge = startEdge1; 
			precedenceTargetEdge = targetEdge1;
		}

		if(precedenceStartEdge == null || precedenceTargetEdge == null) 
			return new Rot();
		else {
			precedenceStartEdge = precedenceStartEdge.subCopy(sharedOrigin);  
			precedenceTargetEdge = precedenceTargetEdge.subCopy(sharedOrigin);
			if(precedenceStartEdge.magSq() <= MathUtils.DOUBLE_ROUNDING_ERROR 
					|| precedenceTargetEdge.magSq() <= MathUtils.DOUBLE_ROUNDING_ERROR) {
				if(skipMinor) {
					return new Rot(); 
				} else {
					precedenceStartEdge = startEdge2.subCopy(sharedOrigin);
					precedenceTargetEdge = targetEdge2.subCopy(sharedOrigin);
					if(precedenceStartEdge.magSq() <= MathUtils.DOUBLE_ROUNDING_ERROR ||
							precedenceTargetEdge.magSq() <= MathUtils.DOUBLE_ROUNDING_ERROR)
						return new Rot(); 
					else {
						skipMinor = true;
					}
				}
			}
		}
		result = new Rot(precedenceStartEdge, precedenceTargetEdge);
		if(skipMinor) 
			return result; 
		else {
			SGVec_3d minorStartEdge = startEdge2.subCopy(sharedOrigin);
			SGVec_3d minorTargetEdge = targetEdge2.subCopy(sharedOrigin);
			if(minorStartEdge.magSq() <= MathUtils.DOUBLE_ROUNDING_ERROR ||
					minorTargetEdge.magSq() <= MathUtils.DOUBLE_ROUNDING_ERROR) 
				return result; 
			else {
				SGVec_3d minorStartEdgeRotated = result.applyToCopy(minorStartEdge); 
				//minorStartEdge.normalize(); minorTargetEdge.normalize();
				SGVec_3d minorStartProjected = minorStartEdgeRotated.getPlaneProjectionOf(precedenceTargetEdge);
				SGVec_3d minorTargetProjected = minorTargetEdge.getPlaneProjectionOf(precedenceTargetEdge);
				if(minorStartProjected.magSq() <= MathUtils.DOUBLE_ROUNDING_ERROR || 
						minorTargetProjected.magSq() <= MathUtils.DOUBLE_ROUNDING_ERROR) {
					return result; 
				} else {

					//Rot minorRotation = new Rot(minorStartEdge, minorTargetEdge); 
					//result = minorRotation.applyTo(result);

					/**
					 * When the projected minor vectors are very small, we approach a degenerate situation, 
					 * however, we can't just discard them, because this would cause our solutions to jump 
					 * when we leave the degenerate threshold. Instead, we between the vector to vector rotation 
					 * and the plane to plane rotation by apparent usefulness of the plane-plane rotation. 
					 * 
					 * Don't be fooled by the elegant-looking weighting formula. 
					 * I have no clue if this is actually a meaningful way to be weighing the influence 
					 * and have yet to try to determine if a truly meaningful one. 
					 */

					double maxStartEdgeLengthSQ = minorStartEdge.subCopy(precedenceTargetEdge).magSq();
					double maxTargetEdgeLengthSQ = minorTargetEdge.subCopy(precedenceTargetEdge).magSq();

					double pythagoreanWeight = (minorStartProjected.magSq()/maxStartEdgeLengthSQ) + (minorTargetProjected.magSq() /maxTargetEdgeLengthSQ); // 1.414213562373095d; 
					pythagoreanWeight = Math.sqrt(pythagoreanWeight/2d);
					//double pythagoreanSlow = (minorStartProjected.mag() / minorStartEdge.mag()) + (minorTargetProjected.mag() / minorTargetEdge.mag());
					//pythagoreanSlow = Math.sqrt(pythagoreanSlow);
					//System.out.println("weight: " + pythagoreanWeight);
					Rot minorRotation = new Rot(minorStartProjected, minorTargetProjected); 
					Rot rolledRot = minorRotation.applyTo(result); 
					result = new Rot(Rot.slerp(pythagoreanWeight, result.rotation, rolledRot.rotation));
					//result = new Rot(precedenceStartEdge, minorStartEdge, precedenceTargetEdge, minorTargetEdge);
					return result;
				}
			}					
		}		
	}


	private void saneCCD(
			double dampening, 
			Strand chain,
			SGVec_3d pointOnTarget,
			double lengthAlongTipBone, //which part of the tip bone should be attempting to reach the target 
			AbstractBone startFrom, 
			AbstractBone upTo,
			boolean inverseWeighting,
			boolean snapLast) {
		AbstractAxes strandTargetAxes = chain.getStrandTip().getPinnedAxes();
		AbstractAxes strandTipAxes = chain.simulatedLocalAxes.get(chain.getStrandTip())[0];
		//strandTipAxes.alignOrientationTo(strandTargetAxes);
		SGVec_3d pinLocation = strandTargetAxes.getGlobalOf(pointOnTarget);
		SGVec_3d alongBoneLocation = new SGVec_3d(0d, lengthAlongTipBone, 0d);
		AbstractBone currentBone = startFrom;
		if(currentBone != null) {
			sgRayd currentBoneDirRay = new sgRayd(new SGVec_3d(), new SGVec_3d());
			sgRayd goalRay = new sgRayd(new SGVec_3d(), new SGVec_3d());

			double boneCount = 1d;
			double bonesInChain = chain.bones.size();

			double angle; 
			double scalar;
			while(currentBone != upTo.parent) {

				AbstractAxes currentSimulatedAxes = chain.simulatedLocalAxes.get(currentBone)[0];
				currentBoneDirRay.setP1(currentSimulatedAxes.origin_()); 
				currentBoneDirRay.setP2(strandTipAxes.getGlobalOf(alongBoneLocation));

				goalRay.setP1(currentBoneDirRay.p1());
				goalRay.setP2(pinLocation); 

				Rot rotateToTarget = new Rot(currentBoneDirRay.heading(), goalRay.heading());

				angle = rotateToTarget.getAngle();
				scalar = angle < 0 ? -1 : 1;
				angle = Math.abs(dampening) > Math.abs(angle) ? angle : dampening * scalar;
				angle *= (1f-currentBone.getStiffness());
				if(inverseWeighting)
					angle *= (boneCount/bonesInChain);

				if(!currentBone.getIKOrientationLock()) {
					currentSimulatedAxes.rotateBy(new Rot(rotateToTarget.getAxis(), angle));
					if(currentBone.getConstraint() != null) {
						currentSimulatedAxes.updateGlobal();
						Constraint constraint =  currentBone.getConstraint();
						((AbstractKusudama)currentBone.getConstraint()).setAxesToOrientationSnap(
								currentSimulatedAxes, 
								chain.simulatedConstraintAxes.get(currentBone)[0], 
								((AbstractKusudama)constraint).getStrength());							
					}
				}
				currentBone = currentBone.parent;
				boneCount++;			
			}


		}
	}


	/**
	 * performs a sequence of inverse snap-to-twists from the chain-tip to @param upTo, and 
	 * then another sequence of forward snap to twists from @param upTo to the chain tip. 
	 */
	private void fabriTwist(Strand chain, AbstractBone upTo) {		
		AbstractBone childBone = chain.getStrandTip();
		AbstractAxes childAxes = chain.simulatedLocalAxes.get(childBone)[0];
		AbstractAxes childAxesCopy = childAxes.getGlobalCopy();
		AbstractBone currentBone = childBone.getParent();
		AbstractAxes childInverseConstraints = chain.simulatedConstraintAxes.get(chain.getStrandTip())[0]; 
		if(childInverseConstraints != null) childInverseConstraints = childInverseConstraints.getGlobalCopy();
		AbstractAxes currentAxesCopy = childAxes.getGlobalCopy();
		AbstractKusudama childConstraint = ((AbstractKusudama)childBone.getConstraint());

		//backwad pass (from tip to root)
		while(childBone != upTo) {
			AbstractAxes currentAxes = chain.simulatedLocalAxes.get(currentBone)[0];
			currentAxesCopy.alignGlobalsTo(currentAxes);
			if(childConstraint != null && !currentBone.getIKOrientationLock()) {
				((AbstractKusudama)childBone.getConstraint()).setAxesToInverseLimitingAxes(
						chain.simulatedLocalAxes.get(currentBone)[0], 
						childAxes, 
						chain.simulatedConstraintAxes.get(childBone)[0], 
						childInverseConstraints);
				if(childConstraint.axiallyConstrained) {
					childConstraint.snapToInvertedLimit(
							childAxesCopy, null, currentAxes, childInverseConstraints, currentAxesCopy, true, true);
					currentAxes.alignGlobalsTo(currentAxesCopy);
				}
			}
			childAxes.alignGlobalsTo(childAxesCopy);
			childAxes.updateGlobal();
			childAxes = currentAxes; 	
			childAxesCopy.alignGlobalsTo(childAxes);
			childAxes.updateGlobal();
			childBone = currentBone;
			currentBone = currentBone.parent;
			childConstraint = ((AbstractKusudama)childBone.getConstraint());
		}


		AbstractAxes tempChildAxes = chain.getStrandTip().getPinnedAxes().getGlobalCopy();
		AbstractAxes simulatedChildAxes = null;
		int i= chain.bones.size()-1; 
		while(i >= 0) {
			currentBone = chain.bones.get(i);
			childBone = null; 
			if(i > 0) childBone = chain.bones.get(i-1);

			if(currentBone.parent != null) {
				AbstractAxes simulatedCurrentAxes = chain.simulatedLocalAxes.get(currentBone)[0];
				if(childBone != null) {
					simulatedChildAxes = chain.simulatedLocalAxes.get(childBone)[0];
					tempChildAxes.alignGlobalsTo(simulatedChildAxes);					
				} 	

				AbstractKusudama constraint = ((AbstractKusudama)currentBone.getConstraint());
				if(constraint != null) {
					constraint.setAxesToSnapped(simulatedCurrentAxes, chain.simulatedConstraintAxes.get(currentBone)[0]);
					//constraint.snapToTwistLimits(simulatedCurrentAxes, chain.simulatedConstraintAxes.get(currentBone));
				}
				if(childBone != null) { 
					simulatedChildAxes.alignGlobalsTo(tempChildAxes);
					simulatedChildAxes.updateGlobal();
				}

			}
			i--;
		}
	}





	/**
	 * TODO: weigh each rotation by total error it introduces to all pins. So that
	 * rotations which selfishly increase error get less of a voice.
	 * @param collection
	 * @param snap
	 */
	private void applyAverageWeightedRotationsToSimulationAxes(StrandedArmature collection, boolean snap) {
		Quaternion neighborhood =new Quaternion(MRotation.IDENTITY.getQ0(), MRotation.IDENTITY.getQ1(), MRotation.IDENTITY.getQ2(), MRotation.IDENTITY.getQ3()); 
		for(AbstractBone b : collection.allBonesInStrandCollection) {

			//double totalDist = 0d;
			ArrayList<Strand> strandList = collection.boneStrandMap.get(b);

			/*for(Strand s : strandList) { 
				s.updateDistToTarget();
				totalDist += s.distToTarget;
			}*/

			double totalCount = 0;
			double wT = 0;
			double xT = 0; 
			double yT = 0; 
			double zT = 0;

			/*Quaternion neighborhood =new Quaternion(
					b.majorRotationAxes.localMBasis.rotation.rotation.getQ0(),
					b.majorRotationAxes.localMBasis.rotation.rotation.getQ1(),
					b.majorRotationAxes.localMBasis.rotation.rotation.getQ2(),
					b.majorRotationAxes.localMBasis.rotation.rotation.getQ3());*/
			//totalDist = collection.totalPinDist;
			double totalFreedomForBone = 0d; 			
			for(Strand s : strandList) {
				//double distance = s.distToTarget; 					
				AbstractAxes simulatedLocal = s.simulatedLocalAxes.get(b)[0];
				//simulatedLocal.updateGlobal();
				Rot r = simulatedLocal.localMBasis.rotation;
				//double errorPenalty = measurePositionalTipErrorForRotAppliedTo(r, b.localAxes(), strandList); 
				//System.out.println("error size : " + errorPenalty);
				//r = new Rot(r.getAxis(), r.getAngle()*(distance/totalDist));
				Rot limitAxesRot = s.simulatedConstraintAxes.get(b)[0].localMBasis.rotation;

				Quaternion current = G.getSingleCoveredQuaternion(
						new Quaternion(r.rotation.getQ0(), 
								r.rotation.getQ1(),
								r.rotation.getQ2(), 
								r.rotation.getQ3()), 
						neighborhood
						);

				double freedomForBoneThisStrand = getAbilityBiasing() ? s.getRemainingFreedomAtBone(b) : 1d; 
				double weight = 1d/freedomForBoneThisStrand;
				totalFreedomForBone += weight;
				//totalFreedomForBone += freedomForBoneThisStrand;				
				wT += (current.getQ0() * weight);
				xT += (current.getQ1() * weight);
				yT += (current.getQ2() * weight);
				zT += (current.getQ3() * weight);
				//if(strandList.size() > 1) System.out.print(Math.toDegrees(r.getAngle()) + ", ");
				//totalDist += distance;
			}
			//if(strandList.size() > 1) System.out.println();
			Rot avg = new Rot(wT/totalFreedomForBone, xT/totalFreedomForBone, yT/totalFreedomForBone, zT/totalFreedomForBone, true);
			//Rot avg = new Rot(wT/totalCount, xT/totalCount, yT/totalCount, zT/totalCount, true);


			//b.localAxes().globalMBasis.rotateTo(avg);
			AbstractAxes composedAxes = collection.averageSimulatedAxes.get(b);
			//composedAxes.alignGlobalsTo();
			//composedAxes.updateGlobal();
			composedAxes.localMBasis.adoptValues(b.localAxes().localMBasis);
			composedAxes.localMBasis.rotateTo(avg); 
			composedAxes.localMBasis.translateTo(b.localAxes().localMBasis.translate);
			//composedAxes.localMBasis.adoptValues((b.localAxes().getParentAxes().getLocalOf(composedAxes.globalMBasis)));
			composedAxes.markDirty();
			composedAxes.updateGlobal();



			for(Strand s : strandList) {
				if(snap) {
					b.setAxesToSnapped(composedAxes, s.simulatedConstraintAxes.get(b)[0]);
				}
				composedAxes.markDirty();
				composedAxes.updateGlobal();
				AbstractAxes strandAxes = s.simulatedLocalAxes.get(b)[0];
				strandAxes.localMBasis.adoptValues(composedAxes.localMBasis);
				strandAxes.markDirty();
				strandAxes.updateGlobal();			
			}
			//b.rotateBy(avg);

			//TODO DEBUG: TEST WHAT HAPPENS IF I ENABLE SNAPPING TO CONSTRAINTS HERE

			//b.snapToConstraints();
			//collection.setDeltaMeasureForBone(b, avg.getAngle());
		}
	}	

	AbstractAxes fauxParent;
	AbstractAxes errorBaseAxes;
	AbstractAxes  errorTipAxes;
	public double measurePositionalTipErrorForRotAppliedTo(
			Rot rotation, 
			AbstractAxes appliedTo, 
			ArrayList<Strand> strands) {

		if(appliedTo.getParentAxes() != null)
			fauxParent.alignGlobalsTo(appliedTo.getParentAxes());
		errorBaseAxes.setParent(fauxParent);
		errorBaseAxes.alignGlobalsTo(appliedTo);
		errorTipAxes.setParent(errorBaseAxes);

		double errorSize = 0d; 

		for(Strand s: strands) {
			errorBaseAxes.alignGlobalsTo(appliedTo);
			errorTipAxes.alignGlobalsTo(s.parentStrandCollection.averageSimulatedAxes.get(s.getStrandTip()));
			AbstractAxes targetAxes = s.getStrandTip().getPinnedAxes();
			errorBaseAxes.localMBasis.rotateTo(rotation);
			errorSize += errorTipAxes.origin_().dist(targetAxes.origin_());
		}		

		return errorSize;
	}

	//debug code -- use to set a minimum distance an effector must move
	// in order to trigger a chain iteration 
	double debugMag = 5f; 
	SGVec_3d lastTargetPos = new SGVec_3d(); 


	public Rot getDampenedRotationBetween(Rot from, Rot to, double clamp) {
		Rot result = to.applyTo(from.revert());
		SGVec_3d axis = result.getAxis(); 
		double aangle =result.getAngle(); 
		aangle = Math.min(aangle, dampening);
		Rot rotTo = new Rot(axis, aangle);
		return rotTo;
	}


	public void setAbilityBiasing(boolean enabled) {
		abilityBiasing = enabled;
	}

	public boolean getAbilityBiasing() {
		return abilityBiasing;
	}


	/**
	 * returns the rotation that would bring the right-handed orthonormal axes of a into alignment with b
	 * @param a
	 * @param b
	 * @return
	 */
	public Rot getRotationBetween(AbstractAxes a, AbstractAxes b) {

		return new Rot(a.orientation_X_(), a.orientation_Y_(), b.orientation_X_(), b.orientation_Y_());
	}



	public int getDefaultIterations() {
		return IKIterations;
	}

	public int getDefaultSolverType() {
		return IKType;
	}

	public double getDampening() {
		return dampening;
	}



	public class PerformanceStats {
		int totalIterationsThisSolverCall = 0; 
		long averageSolutionTime = 0;
		int solutionCount = 0; 
		float iterationsPerSecond = 0f; 


		long startTime = 0;
		public void startPerformanceMonitor() {
			totalIterationsThisSolverCall = 0;
			startTime = System.currentTimeMillis();
		}

		public void solveFinished() {
			long thisSolutionTime = System.currentTimeMillis() - startTime; 
			averageSolutionTime *= solutionCount; 
			solutionCount++;
			averageSolutionTime += thisSolutionTime; 
			averageSolutionTime = (long)((float)averageSolutionTime / (float)solutionCount);
		}

		public void resetPerformanceStat() {
			startTime = 0;
			totalIterationsThisSolverCall = 0; 
			averageSolutionTime = 0;
			solutionCount = 0; 
			iterationsPerSecond = 0f; 
		}

		public void printStats() {
			System.out.println(
					"average solution time: " + (averageSolutionTime)+  "ms \n");
		}

	}

	@Override
	public void makeSaveable() {
		EWBIKSaver.addToSaveState(this);
		this.localAxes().makeSaveable(); 
		this.rootBone.makeSaveable();
	}

	@Override
	public JSONObject getSaveJSON() {
		JSONObject saveJSON = new JSONObject(); 
		saveJSON.setString("identityHash", this.getIdentityHash());
		saveJSON.setString("localAxes", localAxes().getIdentityHash()); 
		saveJSON.setString("rootBone", getRootBone().getIdentityHash());
		saveJSON.setInt("defaultIterations", getDefaultIterations()); 
		saveJSON.setDouble("dampening", this.getDampening());
		saveJSON.setInt("defaultSolver", this.getDefaultSolverType());
		saveJSON.setBoolean("inverseWeighted", this.isInverseWeighted());
		saveJSON.setBoolean("satisfyOrientation", this.isSatisfyOrientation());
		saveJSON.setBoolean("satisfyTwist",  this.isSatisfyTwist());
		saveJSON.setString("tag", this.getTag());
		return saveJSON;
	}


	public void loadFromJSONObject(JSONObject j) {
		this.localAxes = EWBIKLoader.getObjectFor(localAxes().getClass(), j, j.getString("localAxes"));
		this.rootBone = EWBIKLoader.getObjectFor(getRootBone().getClass(), j, j.getString("rootBone"));
		this.setDefaultIterations(j.getInt("defaultIterations"));
		this.setDefaultDampening(j.getDouble("defaultDampening"));
		this.setDefaultIKType(j.getInt("defaultSolver"));	
		this.setSatisfyOrientation(j.getBoolean("satisfyOrientation"));
		this.setSatifyTwist(j.getBoolean("satisfyTwist"));
		this.setInverseWeighted(j.getBoolean("inverseWeighted"));
		this.tag = j.getString("tag");
	}


	@Override
	public void notifyOfSaveIntent() {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOfSaveCompletion() {
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