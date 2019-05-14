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
package IK.floatIK;
import java.util.ArrayList;
import java.util.HashMap;

import IK.floatIK.Constraint;
import IK.floatIK.G;
import IK.floatIK.StrandedArmature.Strand;
import data.EWBIKLoader;
import data.EWBIKSaver;
import data.JSONObject;
import data.Saveable;
import sceneGraph.*;
import sceneGraph.math.doubleV.SGVec_3d;
import sceneGraph.math.doubleV.sgRayd;
import sceneGraph.math.floatV.*;
import sceneGraph.math.floatV.AbstractAxes;
import sceneGraph.math.floatV.Rot;
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
	protected float dampening = (float)Math.toRadians(10f);
	private boolean abilityBiasing = false;

	public float IKSolverStability = 0f; 
	PerformanceStats performance = new PerformanceStats(); 
	
	

	public AbstractArmature() {}

	public AbstractArmature(AbstractAxes inputOrigin, String name) {
		this.localAxes = inputOrigin; 
		this.tempWorkingAxes = localAxes.getGlobalCopy();
		this.tag = name;
		createRootBone(localAxes.y_().heading(), localAxes.z_().heading(), tag+" : rootBone", 1f, AbstractBone.frameType.GLOBAL);
	}



	public AbstractBone createRootBone(AbstractBone inputBone) {
		this.rootBone = inputBone;
		this.segmentedArmature = new SegmentedArmature(rootBone);
		this.strandedArmature = new StrandedArmature(rootBone);
		return rootBone;
	}

	private AbstractBone createRootBone(SGVec_3f tipHeading, SGVec_3f rollHeading, String inputTag, float boneHeight, AbstractBone.frameType coordinateType) {
		initializeRootBone(this, tipHeading, rollHeading, inputTag, boneHeight, coordinateType);
		this.segmentedArmature = new SegmentedArmature(rootBone);
		this.strandedArmature = new StrandedArmature(rootBone);
		return rootBone;
	}

	protected abstract void initializeRootBone(AbstractArmature armature, 
			SGVec_3f tipHeading, SGVec_3f rollHeading, 
			String inputTag, 
			float boneHeight, 
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

	public void setDefaultDampening(float damp) {
		this.dampening = Math.max(Math.abs(Float.MIN_VALUE), Math.abs(damp)); 
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

	public void IKSolver(AbstractBone bone, float dampening, int iterations) {
		
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
	public void ambitiousIKSolver(AbstractBone bone, float dampening, int iterations) {
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

	
	
	
	protected void solveIK(SegmentedArmature chain, float dampening, int iterations) {
		ArrayList<SegmentedArmature> pinnedChains = chain.pinnedDescendants;

		if(!chain.isBasePinned()) {
			SGVec_3f translateBy = new SGVec_3f(0,0,0);
			for(SegmentedArmature pc : pinnedChains) {
				sgRayf tipToTargetRay = new sgRayf(pc.segmentTip.getTip_(), pc.segmentTip.pinnedTo());
				translateBy.add((SGVec_3f)tipToTargetRay.heading());
			}
			translateBy.div((float)pinnedChains.size());
			segmentedArmature.segmentRoot.localAxes.translateByGlobal(translateBy);	
		}

		solveIKChainList(pinnedChains, dampening, iterations); 

		for(SegmentedArmature pc : pinnedChains) {
			for(SegmentedArmature pccs: pc.childSegments) {
				solveIK(pccs, dampening, iterations);
			}
		}

	}

	protected void solveIKChainList(ArrayList<SegmentedArmature> chains, float dampening, int iterations) {
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


	protected void solveIKChain(SegmentedArmature chain, float dampening, int iterations) {		 
		//System.out.println("\n CHAIN ITERATION \n");
		for(int i = 0 ; i<iterations; i++) {
			iterateCCD(dampening, chain); 
		}
	}



	private void iterateCCD(float dampening, SegmentedArmature chain) {
		AbstractBone tipBone = chain.segmentTip; 
		AbstractBone currentBone = tipBone;
		ArrayList<Rot> rotations = new ArrayList<Rot>();

		sgRayf currentRay = new sgRayf(); 
		sgRayf goalRay = new sgRayf(); 

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

				float angle = rotateToTarget.getAngle(); 
				SGVec_3f axis = rotateToTarget.getAxis();

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
	public void tranquilIKSolver(AbstractBone bone, float dampening, int iterations) {
		IKSolverStability = 0f;
		StrandedArmature thisStrandCollection = strandedArmature.getStrandCollectionFor(bone);
		if(thisStrandCollection != null) {
			recursivelyCallAlternateSolver(thisStrandCollection, dampening, iterations);
			for(AbstractBone b : thisStrandCollection.allBonesInStrandCollection) {
				b.IKUpdateNotification();
			}
		}	

	}

	private void recursivelyCallAlternateSolver(StrandedArmature starm, float dampening, int iterations) {		
		if(starm!= null) {
			starm.resetStabilityMeasures();
			alternateIKSolver(starm, dampening, iterations);
			float collectionStability = starm.getStability(); 
			IKSolverStability = Math.max(IKSolverStability, collectionStability);
		}
		for(StrandedArmature sa: starm.childCollections) {
			recursivelyCallAlternateSolver(sa, dampening, iterations);
		}
	}

	protected void alternateIKSolver(StrandedArmature collection, float dampening, int iterations) {
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
				SGVec_3f translateBy = new SGVec_3f(0,0,0);
				sgRayf tipToTargetRay = new sgRayf(translateBy.copy(), null);
				for(Strand s : strands) {
					tipToTargetRay.setP1(s.getStrandTip().getTip_());
					tipToTargetRay.setP2(s.getStrandTip().pinnedTo());
					translateBy.add((SGVec_3f)tipToTargetRay.heading());
				}
				translateBy.div((float)strands.size());
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



	Rot ir = new Rot(new SGVec_3f(1,1,1), 0);//strandList.get(0).rotationsMap.get(b);
	Quaternionf initialQ = new Quaternionf(ir.rotation.getQ0(), ir.rotation.getQ1(), ir.rotation.getQ2(), ir.rotation.getQ3());

	private void applyAverageWeightedRotations(StrandedArmature collection) {

		for(AbstractBone b : collection.allBonesInStrandCollection) {
			ArrayList<Strand> strandList = collection.boneStrandMap.get(b);


			float totalCount = 0;
			float wT = 0;
			float xT = 0; 
			float yT = 0; 
			float zT = 0;

			//totalDist = collection.totalPinDist;

			float totalFreedomForBone = 0f;
			for(Strand s : strandList) {

				//float distance = s.distToTarget; 					
				Rot r = s.rotationsMap.get(b);
				//r = new Rot(r.getAxis(), r.getAngle()*(distance/totalDist));
				Quaternionf current = G.getSingleCoveredQuaternionf(
						new Quaternionf(r.rotation.getQ0(), 
								r.rotation.getQ1(),
								r.rotation.getQ2(), 
								r.rotation.getQ3()), 
						initialQ);

				/*wT += current.getQ0();
				xT += current.getQ1();
				yT += current.getQ2();
				zT += current.getQ3();

				totalCount ++;*/

				float freedomForBoneThisStrand = getAbilityBiasing() ? s.getRemainingFreedomAtBone(b) : 1f; 
				float weight = 1f/freedomForBoneThisStrand;
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

	private void iterateCCDStrand(Strand chain, float dampening) {

		//SGVec_3f strandTip = chain.strandTip.getTip();
		SGVec_3f strandTipPin = chain.getStrandTip().pinnedTo();
		sgRayf currentRay = new sgRayf(new SGVec_3f(0,0,0), null);
		sgRayf goalRay = new sgRayf(new SGVec_3f(0,0,0), null);

		SGVec_3f origXHead = new SGVec_3f(0,0,0);
		SGVec_3f origYHead = new SGVec_3f(0,0,0);
		SGVec_3f postXHead = new SGVec_3f(0,0,0);
		SGVec_3f postYHead = new SGVec_3f(0,0,0);

		for(AbstractBone currentBone : chain.bones) {

			currentRay.setP1(currentBone.getBase_()); 
			currentRay.setP2(chain.getStrandTip().getTip_()); 

			goalRay.setP1(currentRay.p1());
			goalRay.setP2(strandTipPin); 

			origXHead = (SGVec_3f) currentBone.localAxes().x_().heading();
			origYHead = (SGVec_3f) currentBone.localAxes().y_().heading();

			Rot rotateToTarget = new Rot(currentRay.heading(), goalRay.heading());//G.averageRotation(rotations);

			float angle = rotateToTarget.getAngle();

			angle = Math.min(angle, dampening);

			currentBone.rotateBy(new Rot(rotateToTarget.getAxis(), angle));   
			currentBone.snapToConstraints();   			

			postXHead = (SGVec_3f) currentBone.localAxes().x_().heading(); 
			postYHead = (SGVec_3f) currentBone.localAxes().y_().heading();

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
			float dampening, 
			int iterations,
			boolean inverseWeighting,
			boolean orientationAware, 
			boolean twistAware) {
		IKSolverStability = 0f;
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
			float dampening, 
			int iterations, 
			boolean inverseWeighting,
			boolean orientationAware, 
			boolean twistAware) {		
		if(starm!= null) {
			starm.resetStabilityMeasures();
			fabriCCDSolver(starm, dampening, iterations, inverseWeighting, orientationAware, twistAware) ;
			float collectionStability = starm.getStability(); 
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
			float dampening, 
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

		SGVec_3f base = new SGVec_3f(); 

		for(int i = 0; i< iterations; i++) {
			for(Strand s: strands) {
				s.alignSimulationAxesToBones();
				SGVec_3f fauxTip = new SGVec_3f(0f, s.getStrandTip().getBoneHeight(), 0f);
				if(orientationAware) {
					AbstractAxes strandTargetAxes = s.getStrandTip().getPinnedAxes();
					saneCCD(dampening, s, fauxTip, s.getStrandTip().getBoneHeight(), s.getStrandTip(), s.getStrandRoot(), inverseWeighting, true); 	
					saneCCD(dampening, s, base, 0f, s.getStrandTip().parent, s.getStrandRoot(), inverseWeighting, true);
					AbstractAxes simulatedTipAxes = s.simulatedLocalAxes.get(s.getStrandTip()); simulatedTipAxes.updateGlobal();
					simulatedTipAxes.alignOrientationTo(strandTargetAxes);	
					AbstractKusudama constraint =  ((AbstractKusudama)s.getStrandTip().getConstraint());
					if(constraint != null) {
						constraint.setAxesToOrientationSnap(simulatedTipAxes, s.simulatedConstraintAxes.get(s.getStrandTip()), constraint.getStrength());
					}	
				} else {
					saneCCD(dampening, s, base, s.getStrandTip().getBoneHeight(), s.getStrandTip(), s.getStrandRoot(), inverseWeighting, true);
				}			
				
				if(twistAware) 
					fabriTwist(s, s.getStrandRoot()); 				
			}
		}
		applyAverageWeightedRotationsToSimulationAxes(collection, false);
		strandedArmature.translateToAverageTipError(true);	

	}


	private void saneCCD(
			float dampening, 
			Strand chain,
			SGVec_3f pointOnTarget,
			float lengthAlongTipBone, //which part of the tip bone should be attempting to reach the target 
			AbstractBone startFrom, 
			AbstractBone upTo,
			boolean inverseWeighting,
			boolean snapLast) {
		AbstractAxes strandTargetAxes = chain.getStrandTip().getPinnedAxes();
		AbstractAxes strandTipAxes = chain.simulatedLocalAxes.get(chain.getStrandTip());
		//strandTipAxes.alignOrientationTo(strandTargetAxes);
		SGVec_3f pinLocation = strandTargetAxes.getGlobalOf(pointOnTarget);
		SGVec_3f alongBoneLocation = new SGVec_3f(0f, lengthAlongTipBone, 0f);
		AbstractBone currentBone = startFrom;
		if(currentBone != null) {
			sgRayf currentBoneDirRay = new sgRayf(new SGVec_3f(), new SGVec_3f());
			sgRayf goalRay = new sgRayf(new SGVec_3f(), new SGVec_3f());

			float boneCount = 1f;
			float bonesInChain = chain.bones.size();

			float angle; 
			float scalar;
			while(currentBone != upTo.parent) {

				AbstractAxes currentSimulatedAxes = chain.simulatedLocalAxes.get(currentBone);
				currentBoneDirRay.setP1(currentSimulatedAxes.origin_()); 
				currentBoneDirRay.setP2(strandTipAxes.getGlobalOf(alongBoneLocation));

					goalRay.setP1(currentBoneDirRay.p1());
					goalRay.setP2(pinLocation); 

					Rot rotateToTarget = new Rot(
							currentBoneDirRay.heading(), 
							goalRay.heading()
							);
					
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
									chain.simulatedConstraintAxes.get(currentBone), 
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
		AbstractAxes childAxes = chain.simulatedLocalAxes.get(childBone);
		AbstractAxes childAxesCopy = childAxes.getGlobalCopy();
		AbstractBone currentBone = childBone.getParent();
		AbstractAxes childInverseConstraints = chain.simulatedConstraintAxes.get(chain.getStrandTip()); 
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


		AbstractAxes tempChildAxes = chain.getStrandTipTracerAxes().getGlobalCopy();
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
				if(constraint != null) {
					constraint.snapToTwistLimits(simulatedCurrentAxes, chain.simulatedConstraintAxes.get(currentBone));
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

		for(AbstractBone b : collection.allBonesInStrandCollection) {

			//float totalDist = 0f;
			ArrayList<Strand> strandList = collection.boneStrandMap.get(b);

			/*for(Strand s : strandList) { 
				s.updateDistToTarget();
				totalDist += s.distToTarget;
			}*/

			float totalCount = 0;
			float wT = 0;
			float xT = 0; 
			float yT = 0; 
			float zT = 0;

			//totalDist = collection.totalPinDist;
			float totalFreedomForBone = 0f; 			
			for(Strand s : strandList) {
				//float distance = s.distToTarget; 					
				AbstractAxes simulatedLocal = s.simulatedLocalAxes.get(b);
				//simulatedLocal.updateGlobal();
				Rot r = simulatedLocal.localMBasis.rotation;
				//r = new Rot(r.getAxis(), r.getAngle()*(distance/totalDist));
				Quaternionf current = G.getSingleCoveredQuaternionf(
						new Quaternionf(r.rotation.getQ0(), 
								r.rotation.getQ1(),
								r.rotation.getQ2(), 
								r.rotation.getQ3()), 
						initialQ);

				float freedomForBoneThisStrand = getAbilityBiasing() ? s.getRemainingFreedomAtBone(b) : 1f; 
				float weight = 1f/freedomForBoneThisStrand;
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
			Rot avg = new Rot(
					wT/totalFreedomForBone, 
					xT/totalFreedomForBone, 
					yT/totalFreedomForBone, 
					zT/totalFreedomForBone, true);
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
	float debugMag = 5f; 
	SGVec_3f lastTargetPos = new SGVec_3f(); 


	public Rot getDampenedRotationBetween(Rot from, Rot to, float clamp) {
		Rot result = to.applyTo(from.revert());
		SGVec_3f axis = result.getAxis(); 
		float aangle =result.getAngle(); 
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

	public float getDampening() {
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
		saveJSON.setFloat("dampening", this.getDampening());
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
		this.setDefaultDampening(j.getFloat("defaultDampening"));
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