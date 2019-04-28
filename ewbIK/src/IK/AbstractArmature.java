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
package IK;
import java.util.ArrayList;
import java.util.HashMap;

import IK.StrandedArmature.Strand;
import sceneGraph.*;
import sceneGraph.math.Quaternion;
import sceneGraph.math.SGVec_3d;
import sceneGraph.math.SGVec_3d;
/**
 * @author Eron Gjoni
 *
 */

public abstract class AbstractArmature {
	
	
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
		return rootBone;
	}

	private AbstractBone createRootBone(SGVec_3d tipHeading, SGVec_3d rollHeading, String inputTag, double boneHeight, AbstractBone.frameType coordinateType) {
		initializeRootBone(this, tipHeading, rollHeading, inputTag, boneHeight, coordinateType);
		this.segmentedArmature = new SegmentedArmature(rootBone);
		this.strandedArmature = new StrandedArmature(rootBone);
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
	protected boolean inverseWeighted = true;
	
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
		
		//performance.resetPerformanceStat();
		performance.startPerformanceMonitor();
		if(this.IKType == AMBITIOUS) {
			ambitiousIKSolver(bone, dampening, iterations);
		} else if(this.IKType == TRANQUIL) {
			tranquilIKSolver(bone, dampening, iterations);
		} else if(this.IKType == MIXED) {
			tranquilIKSolver(bone, dampening, iterations);
			ambitiousIKSolver(bone, dampening, iterations);
		} else if(this.IKType == ORIENTATIONAWARE) {
			orientationAwareSolver(bone, dampening, iterations, isInverseWeighted(), isSatisfyOrientation(), isSatisfyTwist());
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
				else if (thisChain != null && thisChain.tipPinned || thisChain.pinnedDescendants.size() > 0)
					solveIK(segmentedArmature, dampening, 1);//iterations);
			}
			//}
		}// if there's only one pin, the armature automatically gets translated
		//to meet its requirement as an inherent part of the algorithm anyway, and I can't imagine any instance where it is not 
		//acceptable to do so. so, doing so.
	}

	
	
	
	protected void solveIK(SegmentedArmature chain, double dampening, int iterations) {
		ArrayList<SegmentedArmature> pinnedChains = chain.pinnedDescendants;

		if(!chain.basePinned) {
			SGVec_3d translateBy = new SGVec_3d(0,0,0);
			for(SegmentedArmature pc : pinnedChains) {
				sgRay tipToTargetRay = new sgRay(pc.segmentTip.getTip_(), pc.segmentTip.pinnedTo());
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
			if(c.parentSegment != null && !c.parentSegment.tipPinned && !c.basePinned) {
				insertIntoSortedChainList(parentChains, c.parentSegment);
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

		sgRay currentRay = new sgRay(); 
		sgRay goalRay = new sgRay(); 

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
			if(!collection.basePinned) {
				SGVec_3d translateBy = new SGVec_3d(0,0,0);
				sgRay tipToTargetRay = new sgRay(translateBy.copy(), null);
				for(Strand s : strands) {
					tipToTargetRay.setP1(s.strandTip.getTip_());
					tipToTargetRay.setP2(s.strandTip.pinnedTo());
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
		SGVec_3d strandTipPin = chain.strandTip.pinnedTo();
		sgRay currentRay = new sgRay(new SGVec_3d(0,0,0), null);
		sgRay goalRay = new sgRay(new SGVec_3d(0,0,0), null);

		SGVec_3d origXHead = new SGVec_3d(0,0,0);
		SGVec_3d origYHead = new SGVec_3d(0,0,0);
		SGVec_3d postXHead = new SGVec_3d(0,0,0);
		SGVec_3d postYHead = new SGVec_3d(0,0,0);

		for(AbstractBone currentBone : chain.bones) {

			currentRay.setP1(currentBone.getBase_()); 
			currentRay.setP2(chain.strandTip.getTip_()); 

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
					recursivelyCallFabriCCDSolver(thisStrandCollection, dampening, 1, inverseWeighting, orientationAware, twistAware);		
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
			fabriCCDSolver(starm, dampening, iterations, inverseWeighting, true, true) ;
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
			boolean twistAware ) {
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
				SGVec_3d fauxTip = new SGVec_3d(0d, s.strandTip.getBoneHeight(), 0d);
				if(orientationAware) 
					saneCCD(dampening, s, fauxTip, s.strandTip, s.strandRoot, inverseWeighting, true); 	
				saneCCD(dampening, s, base, s.strandTip.parent, s.strandRoot, inverseWeighting, true);							
				if(twistAware) 
					fabriTwist(s, s.strandRoot); 				
			}
		}
		applyAverageWeightedRotationsToSimulationAxes(collection, false);
		strandedArmature.translateToAverageTipError(true);	

	}


	private void saneCCD(
			double dampening, 
			Strand chain,
			SGVec_3d pointOnTarget,
			AbstractBone startFrom, 
			AbstractBone upTo,
			boolean inverseWeighting,
			boolean snapLast) {
		AbstractAxes strandTargetAxes = chain.strandTip.getPinnedAxes();
		AbstractAxes strandTipAxes = chain.simulatedLocalAxes.get(chain.strandTip);
		strandTipAxes.alignOrientationTo(strandTargetAxes);
		SGVec_3d pinLocation = strandTargetAxes.getGlobalOf(pointOnTarget);
		AbstractBone currentBone = startFrom;
		if(currentBone != null) {
			sgRay currentBoneDirRay = new sgRay(new SGVec_3d(), new SGVec_3d());
			sgRay goalRay = new sgRay(new SGVec_3d(), new SGVec_3d());

			double boneCount = 1d;
			double bonesInChain = chain.bones.size();

			double angle; 
			double scalar;
			while(currentBone != upTo.parent) {
				
					AbstractAxes currentSimulatedAxes = chain.simulatedLocalAxes.get(currentBone);
					currentBoneDirRay.setP1(currentSimulatedAxes.origin_()); 
					currentBoneDirRay.setP2(strandTipAxes.getGlobalOf(pointOnTarget));

					goalRay.setP1(currentBoneDirRay.p1());
					goalRay.setP2(pinLocation); 

					Rot rotateToTarget = new Rot(currentBoneDirRay.heading(), goalRay.heading());
					
					angle = rotateToTarget.getAngle();
					scalar = angle < 0 ? -1 : 1;
					angle = Math.abs(dampening) > Math.abs(angle) ? angle : dampening * scalar;
					angle *= (1f-currentBone.getStiffness());
					if(inverseWeighted)
						angle *= (boneCount/bonesInChain);
					
					if(!currentBone.getIKOrientationLock()) {
						currentSimulatedAxes.rotateBy(new Rot(rotateToTarget.getAxis(), angle));
						if(currentBone.getConstraint() != null) {
							currentSimulatedAxes.updateGlobal();
							Constraint constraint =  currentBone.getConstraint();
							((AbstractKusudama)currentBone.getConstraint()).setAxesToOrientationSnap(
									currentSimulatedAxes, 
									chain.simulatedConstraintAxes.get(currentBone), 
									((AbstractKusudama)constraint).getStrength());							
						}
					}
				currentBone = currentBone.parent;
				boneCount++;			
			}

			AbstractAxes simulatedTipAxes = chain.simulatedLocalAxes.get(chain.strandTip); simulatedTipAxes.updateGlobal();

			simulatedTipAxes.alignOrientationTo(strandTargetAxes);	
			AbstractKusudama constraint =  ((AbstractKusudama)chain.strandTip.getConstraint());
			if(constraint != null) {
				constraint.setAxesToOrientationSnap(simulatedTipAxes, chain.simulatedConstraintAxes.get(chain.strandTip), constraint.getStrength());
			}			
		}
	}


	/**
	 * performs a sequence of inverse snap-to-twists from the chain-tip to @param upTo, and 
	 * then another sequence of forward snap to twists from @param upTo to the chain tip. 
	 */
	private void fabriTwist(Strand chain, AbstractBone upTo) {		
		AbstractBone childBone = chain.strandTip;
		AbstractAxes childAxes = chain.simulatedLocalAxes.get(childBone);
		AbstractAxes childAxesCopy = childAxes.getGlobalCopy();
		AbstractBone currentBone = childBone.getParent();
		AbstractAxes childInverseConstraints = chain.simulatedConstraintAxes.get(chain.strandTip); 
		if(childInverseConstraints != null) childInverseConstraints = childInverseConstraints.getGlobalCopy();
		AbstractAxes currentAxesCopy = childAxes.getGlobalCopy();
		AbstractKusudama childConstraint = ((AbstractKusudama)childBone.getConstraint());

		//backwad pass (from tip to root)
		while(childBone != upTo) {
			AbstractAxes currentAxes = chain.simulatedLocalAxes.get(currentBone);
			currentAxesCopy.alignGlobalsTo(currentAxes);
			if(childConstraint != null && !currentBone.getIKOrientationLock()) {
				((AbstractKusudama)childBone.getConstraint()).setAxesToInverseLimitingAxes(
						chain.simulatedLocalAxes.get(currentBone), 
						childAxes, 
						chain.simulatedConstraintAxes.get(childBone), 
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


		AbstractAxes tempChildAxes = chain.strandTipTracerAxes.getGlobalCopy();
		AbstractAxes simulatedChildAxes = null;
		int i= chain.bones.size()-1; 
		while(i >= 0) {
			currentBone = chain.bones.get(i);
			childBone = null; 
			if(i > 0) childBone = chain.bones.get(i-1);

			if(currentBone.parent != null) {
				AbstractAxes simulatedCurrentAxes = chain.simulatedLocalAxes.get(currentBone);
				if(childBone != null) {
					simulatedChildAxes = chain.simulatedLocalAxes.get(childBone);
					tempChildAxes.alignGlobalsTo(simulatedChildAxes);					
				} 	

				AbstractKusudama constraint = ((AbstractKusudama)currentBone.getConstraint());
				constraint.snapToTwistLimits(simulatedCurrentAxes, chain.simulatedConstraintAxes.get(currentBone));

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

			//totalDist = collection.totalPinDist;
			double totalFreedomForBone = 0d; 			
			for(Strand s : strandList) {
				//double distance = s.distToTarget; 					
				AbstractAxes simulatedLocal = s.simulatedLocalAxes.get(b);
				//simulatedLocal.updateGlobal();
				Rot r = simulatedLocal.localMBasis.rotation;
				//r = new Rot(r.getAxis(), r.getAngle()*(distance/totalDist));
				Quaternion current = G.getSingleCoveredQuaternion(
						new Quaternion(r.rotation.getQ0(), 
								r.rotation.getQ1(),
								r.rotation.getQ2(), 
								r.rotation.getQ3()), 
						initialQ);

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
				AbstractAxes strandAxes = s.simulatedLocalAxes.get(b);
				strandAxes.localMBasis.adoptValues(composedAxes.localMBasis);
				strandAxes.markDirty();
				strandAxes.updateGlobal();
				if(snap) {
					b.setAxesToSnapped(strandAxes, s.simulatedConstraintAxes.get(b));
				}
			}
			//b.rotateBy(avg);

			//TODO DEBUG: TEST WHAT HAPPENS IF I ENABLE SNAPPING TO CONSTRAINTS HERE

			//b.snapToConstraints();
			collection.setDeltaMeasureForBone(b, avg.getAngle());
		}

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


}
