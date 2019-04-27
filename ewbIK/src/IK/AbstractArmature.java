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
	/**
	 * MIXED MODE APPLIES ONE ITERATION OF 
	 * TRANQUIL TO GET A STABLE STARTING TEMPLATE
	 * AND ONE ITERATION OF AMBITIOUS TO GET 
	 * STRONG MATCH. HOWEVER, THIS IS NECESSARILY 
	 * SLOWER THAN JUST USING ONE OR THE OTHER
	 */
	public final static int AMBITIOUS = 0, TRANQUIL = 1, MIXED = 2, ORIENTATIONAWARE = 4;

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
			fabriCCDIKSolver(bone, dampening, iterations);
		} else if(this.IKType == ORIENTATIONAWARE) {
			fabriCCDIKSolver(bone, dampening, iterations);
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
					currentRay.p1 = currentBone.getBase_(); 
					currentRay.p2 = pinnedTarget.segmentTip.getTip_();

					goalRay.p1 = currentBone.getBase_(); 
					goalRay.p2 = pinnedTarget.segmentTip.pinnedTo();
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



		/* EXPERIMENTAL : Solves an IK system by treating every child effector as maintaining its own chain relative to the parent
		 * effector, this results in a superposition of rotations on any bone with a descendent branching out to multiple end effectors.
		 * the average of these rotations is taken as the solution.
		 * This algorithm starts from the current bone, and travels rootward until a pinned bone 
		 * is encountered. If the entire armature only contains one pin, the entire armature gets translated
		 * to meet the requirements of that pin. 
		 */
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
					tipToTargetRay.p1 = s.strandTip.getTip_();
					tipToTargetRay.p2 = s.strandTip.pinnedTo();
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

			currentRay.p1 = currentBone.getBase_(); 
			currentRay.p2 = chain.strandTip.getTip_(); 

			goalRay.p1 = currentRay.p1;
			goalRay.p2 = strandTipPin; 

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

	/*private ArrayList<AbstractBone> getStrandFromTip(AbstractBone pinnedBone) {
		ArrayList<AbstractBone> result = new ArrayList<AbstractBone>();

		if(pinnedBone.isPinned()) {
			result.add(pinnedBone);
			AbstractBone currBone = pinnedBone.parent;
			while(currBone.parent != null) {
				result.add(currBone);
				if(currBone.parent.isPinned()) {
					break;
				}
				currBone = currBone.parent;
			}			
		}

		return result;
	}*/

	/**
	 * 
	 * @return a reference to the Axes serving as this Armature's coordinate system. 
	 */
	public AbstractAxes localAxes() {
		return this.localAxes;
	}



	/** 
	 * Experimental. 
	 * 
	 * AmbitiousIKSolver and TranquilIKSolver only try to satisfy end-effector position. 
	 * This solver attempts to satisfy end effector orientation as well.
	 * 
	 * As an example of the difference between this solver and the other two -- with this 
	 * solver, you can cause a humanoid rig's palm to remain flat against a spot on wall. Whereas 
	 * with the other two solvers, doing this would require multiple pins orthogonal bones. While 
	 * this method should offer results similar to using multiple pins, it has the advantage
	 * of allowing you to specify how much you care about orientation vs position.
	 * 
	 * 
	 * Attempts to satisfy even orientational targets of end effectors. 
	 * 
	 * 
	 * 
	 * @param bone the bone from which to begin solving the IK system. Pinned ancestors of this bone
	 * will not be modifed. Pinned children will however have this solver called on them -- recursively.  
	 * @param dampening the maximum number of radians any bone is allowed to rotate per iteration.
	 * a lower number gives more evenly distributed results, but may take a larger number of iterations 
	 * before it converges on a solution. if you don't know what you want, try starting with 0.1.
	 * @param iterations the number of times to run the IK loop before returning an answer. 
	 */
	public void orientationAwareIKSolver(AbstractBone bone, double dampening, int iterations) {
		IKSolverStability = 0d;
		StrandedArmature thisStrandCollection = strandedArmature.getStrandCollectionFor(bone);
		if(thisStrandCollection != null) {
			recursivelyCallOrientationSolver(thisStrandCollection, dampening, iterations);
		}
	}


	private void recursivelyCallOrientationSolver(StrandedArmature starm, double dampening, int iterations) {		
		if(starm!= null) {
			starm.resetStabilityMeasures();
			orientationIKSolver(starm, dampening, iterations);
			double collectionStability = starm.getStability(); 
			IKSolverStability = Math.max(IKSolverStability, collectionStability);
		}
		for(StrandedArmature sa: starm.childCollections) {
			recursivelyCallOrientationSolver(sa, dampening, iterations);
		}
	}

	protected void orientationIKSolver(StrandedArmature collection, double dampening, int iterations) {
		ArrayList<Strand> strands = collection.strands;
		/**
		 * Go through each strand, and do an iteration of CCD on it. 
		 * For each strand, add to its rotationHashMap for each bone the rotational difference
		 * between its current orientation, and its original orientation 
		 * then reset the bone orientations of all bones in the collections back to their original orientations;
		 * (as an optimization, only reset the orientation if the bone is mapped to multiple strands in boneStrandsMap);
		 */		

		for(int i =0; i<iterations*10; i++) {
			//System.out.println("iteration: " + i);
			if(!collection.basePinned) {
				SGVec_3d translateBy = new SGVec_3d(0,0,0);
				sgRay tipToTargetRay = new sgRay(translateBy.copy(), null);
				for(Strand s : strands) {
					tipToTargetRay.p1 = s.strandTip.getTip_();
					tipToTargetRay.p2 = s.strandTip.pinnedTo();
					translateBy.add((SGVec_3d)tipToTargetRay.heading());
				}
				translateBy.div((double)strands.size());
				strandedArmature.strandRoot.localAxes.translateByGlobal(translateBy);	
			}	

			for(Strand s: strands) {
				resetRotationMap(s);
				/*
				 * Kinda hacky to have iterateCCDStrand 
				 * actually reach into the strand and make changes on the fly, 
				 * but I'm shooting for speed over readability,
				 * and this allows me to avoid copying a bunch of axes objects.
				 */
				iterateOrentationAwareCCDStrand(s, dampening, POSITION);
				s.revertRotations();
				iterateOrentationAwareCCDStrand(s, dampening, TWIST);
				s.revertRotations();
				iterateOrentationAwareCCDStrand(s, dampening, DIRECTION);
				s.revertRotations();

				averageStrandTempRotations(s);
				//s.updateDistToTarget();
				//collection.alignMultiStrandBonesToOriginalAxes();		

				/*for(AbstractBone currentBone : s.bones) {
					currentBone.rotateBy(s.rotationsMap.get(currentBone));

				}*/

			}
			applyAverageWeightedRotations(collection);
			//collection.updateStabilityEstimates();
			//collection.refreshOriginalAxesMap();
		}


	}



	public static final int POSITION = 0, DIRECTION = 1, TWIST = 2;

	/**
	 * 
	 * @param chain
	 * @param dampening
	 * @param align one of TWIST, DIRECTION, or POSITION. This parameter determines 
	 * which of the strand tip's alignment targets this iteration will aim for.
	 */
	private void iterateOrentationAwareCCDStrand(Strand chain, double dampening, int align) {

		//SGVec_3d strandTip = chain.strandTip.getTip();
		//SGVec_3d strandTipPin = chain.strandTip.pinnedTo();
		AbstractAxes targetAxes = chain.strandTip.getPinnedAxes();
		//AbstractAxes tracerAxes = chain.getStrandTipTracerAxes();
		sgRay currentRay = new sgRay(new SGVec_3d(0,0,0), null);
		sgRay goalRay = new sgRay(new SGVec_3d(0,0,0), null);

		SGVec_3d strandTarget = getAppropriateTargetPosition(targetAxes, align);

		SGVec_3d origXHead = new SGVec_3d(0,0,0);
		SGVec_3d origYHead = new SGVec_3d(0,0,0);
		SGVec_3d postXHead = new SGVec_3d(0,0,0);
		SGVec_3d postYHead = new SGVec_3d(0,0,0);

		for(AbstractBone currentBone : chain.bones) {

			currentRay.p1 = currentBone.getBase_(); 
			currentRay.p2 = getAppropriateTargetPosition(chain.getStrandTipTracerAxes(), align);

			goalRay.p1 = currentRay.p1;
			goalRay.p2 = strandTarget; 

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

			Quaternion totalRotationQ = G.getSingleCoveredQuaternion(
					new Quaternion(totalRotation.rotation.getQ0(), 
							totalRotation.rotation.getQ1(),
							totalRotation.rotation.getQ2(), 
							totalRotation.rotation.getQ3()), 
					initialQ);

			Rot existingTempRotation = chain.tempRotationsMap.get(currentBone);

			Quaternion existingRotationQ = G.getSingleCoveredQuaternion(
					new Quaternion(existingTempRotation.rotation.getQ0(), 
							existingTempRotation.rotation.getQ1(),
							existingTempRotation.rotation.getQ2(), 
							existingTempRotation.rotation.getQ3()), 
					initialQ);


			double cq0 = existingRotationQ.getQ0();
			double cq1 = existingRotationQ.getQ1(); 
			double cq2 = existingRotationQ.getQ2();
			double cq3 = existingRotationQ.getQ3();

			cq0 += totalRotationQ.getQ0();
			cq1 += totalRotationQ.getQ1();
			cq2 += totalRotationQ.getQ2();
			cq3 += totalRotationQ.getQ3();

			existingTempRotation.rotation.set(cq0, cq1, cq2, cq3, false);



			//currentBone = currentBone.parent;
		}
	}


	/**
	 * returns either the origin, x tip, or y-tip of the input Axes 
	 * depending on the value of align
	 * @param from the axes to get the relevant point from
	 * @param align one of POSITION, DIRECTION, or TWIST
	 * @return  either the origin, x tip, or y-tip of the input Axes 
	 * depending on the value of align
	 */
	private SGVec_3d getAppropriateTargetPosition(AbstractAxes from, int align) {
		if(align == TWIST) 
			return (SGVec_3d) from.x_().p2.copy();
		else if(align == DIRECTION) 
			return (SGVec_3d) from.y_().p2.copy();
		else 
			return (SGVec_3d) from.origin_(); 	
	}


	private void resetRotationMap(Strand s) {
		Rot defaultRot = new Rot(MRotation.IDENTITY);
		for(AbstractBone currentBone : s.bones) { 
			Rot mappedRot = s.tempRotationsMap.get(currentBone); 
			if(mappedRot != null) 
				mappedRot.set((MRotation)null);
			else {
				s.tempRotationsMap.put(currentBone, defaultRot.copy());
			}
		}
	}



	private void averageStrandTempRotations(Strand s) {
		for(AbstractBone currentBone : s.bones) { 
			Rot mappedTempRot = s.tempRotationsMap.get(currentBone);
			//r = new Rot(r.getAxis(), r.getAngle()*(distance/totalDist));
			Quaternion current = G.getSingleCoveredQuaternion(
					new Quaternion(mappedTempRot.rotation.getQ0(), 
							mappedTempRot.rotation.getQ1(),
							mappedTempRot.rotation.getQ2(), 
							mappedTempRot.rotation.getQ3()), 
					initialQ);

			Rot mappedRot = s.rotationsMap.get(currentBone); 

			mappedRot.rotation.set(current.getQ0()/3d, current.getQ1()/3d, current.getQ2()/3d, current.getQ3()/3d, true);
			/*xT += current.getQ1();
			yT += current.getQ2();
			zT += current.getQ3();

			totalCount ++;*/			
		}

	}



	/**
	 *FabriCCD outline-- 
	 *
	 * 
	 * basic idea: 
	 * 	take the tip of each strand and position and orient it in accordance with its end effector.
	 * 		start currentBone sequence
	 *		take the parent bone of the currentBone, point it to the currentBone's base, then translate its tip to that base.
	 *		snap the currentBone to its constraints.
	 *		call the REPAIR TIP function 
	 *			the exact repair tip procedure should be modular so I can experiment -- but some ideas include 
	 *				1. simply rotate from the base of the parent bone in the strand tip -> strand target direction (CCD like). 
	 *				2. do another Forward Fabrik pass from this configuration. 
	 *     do a backward fabrik iteration
	 */


	public void fabriCCDIKSolver(AbstractBone bone, double dampening, int iterations) {
		IKSolverStability = 0d;
		StrandedArmature thisStrandCollection = strandedArmature.getStrandCollectionFor(bone);
		if(thisStrandCollection != null && thisStrandCollection.strands.size() > 0) {
			//System.out.println(bone.getPinnedAxes());


			//ArrayList<Strand> innerMostStrands = new ArrayList<>();
			//this.strandedArmature.getInnerMostStrands(innerMostStrands);

			thisStrandCollection.alignSimulationAxesToBones();
			//System.out.println("######## ITERATING #############"); 
			long totalTime = System.currentTimeMillis(); 
			int outerIterations = Math.max(1, iterations/thisStrandCollection.strands.size());
			int innerIterations = iterations / outerIterations;
			innerIterations += Math.max(0, (iterations - (outerIterations*innerIterations))); 
			for(int i =0; i<iterations; i++) {
				if(thisStrandCollection != null) {
					//System.out.println("#### ITERATION " + i + " BEGIN ###");
					recursivelyCallFabriCCDSolver(thisStrandCollection, dampening, 1);		

					//System.out.println("averaging difference       -----------------    ");
				}	
			}		
			//System.out.println("~~~~~~~~~~~~~~~TOTAL TIME = " + (System.currentTimeMillis() - totalTime));
			thisStrandCollection.alignBonesToSimulationAxes();

			recursivelyNotifyBonesOfCompletedIKSolution(thisStrandCollection);
			/*if(strandedArmature.strandRoot.localAxes().localMBasis.getComposedMatrix().m00 == Double.NaN) {
				System.out.println("shit");
			}*/


		}
	}

	private void recursivelyNotifyBonesOfCompletedIKSolution(StrandedArmature startFrom) {
		for(AbstractBone b : startFrom.allBonesInStrandCollection) {
			b.IKUpdateNotification();
		} 
		for(StrandedArmature childStrandCollection  : startFrom.childCollections)
			recursivelyNotifyBonesOfCompletedIKSolution(childStrandCollection);
	}

	private void recursivelyCallFabriCCDSolver(StrandedArmature starm, double dampening, int iterations) {		
		if(starm!= null) {
			long startTime = System.currentTimeMillis(); 
			starm.resetStabilityMeasures();
			fabriCCDSolver(starm, dampening, iterations) ;
			double collectionStability = starm.getStability(); 
			IKSolverStability = Math.max(IKSolverStability, collectionStability);
			//System.out.println("Segment time = " + (System.currentTimeMillis() - startTime));
		}
		for(StrandedArmature sa: starm.childCollections) {
			recursivelyCallFabriCCDSolver(sa, dampening, iterations);
		}


	}


	protected void fabriCCDSolver(StrandedArmature collection, double dampening, int iterations) {
		collection.updateSimulatedStrandRootParentAxes();
		ArrayList<Strand> strands = collection.strands;
		/**
		 * Go through each strand, and do an iteration of CCD on it. 
		 * For each strand, add to its rotationHashMap for each bone the rotational difference
		 * between its current orientation, and its original orientation 
		 * then reset the bone orientations of all bones in the collections back to their original orientations;
		 * (as an optimization, only reset the orientation if the bone is mapped to multiple strands in boneStrandsMap);
		 */		



		/*if(!collection.basePinned) {
			SGVec_3d translateBy = new SGVec_3d(0,0,0);
			sgRay tipToTargetRay = new sgRay(translateBy.copy(), null);
			for(Strand s : strands) {
				tipToTargetRay.p1 = s.simulatedLocalAxes.get(s.strandTip).y().getScaledTo(s.strandTip.getBoneHeight());
				tipToTargetRay.p2 = s.strandTip.getPinnedAxes().origin();
				translateBy.add(tipToTargetRay.heading());
			}
			translateBy.div((double)strands.size());
			for(Strand s : strands) {
				s.simulatedLocalAxes.get(s.strandRoot).translateByGlobal(translateBy);	
				//segmentedArmature.segmentRoot.localAxes.translateByGlobal(translateBy);	
			}
		}	else {
			SGVec_3d parentTip = collection.parentStrand.simulatedLocalAxes.get(collection.parentStrand.strandTip).getGlobalOf(new SGVec_3d(0, collection.parentStrand.strandTip.getBoneHeight(), 0));
			for(Strand s: collection.strands) {
				s.simulatedLocalAxes.get(s.strandRoot).translateTo(parentTip);
			}
		}*/

		//Strand s = collection.strands.get(0);
		for(int i = 0; i< iterations; i++) {
			for(Strand s: strands) {
				s.alignSimulationAxesToBones();

				//iterateFabriCCDStrand(s, dampening);
				saneCCDRectify(dampening, s, s.strandRoot, true);
			}
			//CCDRectify(Math.toRadians(1), s, s.strandRoot, true);

		}
		applyAverageWeightedRotationsToSimulationAxes(collection, false);
		strandedArmature.translateToAverageTipError(true);	



	}




	/**
	 * does not solve for the strand tip. Instead, solves for all ancestors of the strand tip such that
	 * the target is where the pinned BoneExample's base would be if the chain were solvable. Then as a final step reorients
	 * the pinned bone to match the target orientation as closely as possible
	 * @param dampening
	 * @param chain
	 * @param upTo
	 * @param snapLast
	 */
	private void saneCCDRectify(double dampening, Strand chain, AbstractBone upTo, boolean snapLast) {
		AbstractAxes strandTargetAxes = chain.strandTip.getPinnedAxes();
		//System.out.println(chain.strandTip.getPinnedAxes().y_norm_());
		AbstractAxes strandTipAxes = chain.simulatedLocalAxes.get(chain.strandTip);
		strandTipAxes.alignOrientationTo(strandTargetAxes);
		SGVec_3d pinnedBase = strandTargetAxes.origin_(); //.getGlobalOf(new SGVec_3d(0, 0, 0));
		AbstractBone currentBone = chain.strandTip.parent;
		if(currentBone != null) {
			sgRay currentBoneDirRay = new sgRay(new SGVec_3d(), new SGVec_3d());
			sgRay currentORTHORay = new sgRay(new SGVec_3d(), new SGVec_3d());
			sgRay goalRay = new sgRay(new SGVec_3d(), new SGVec_3d());
			sgRay goalORTHORay = new sgRay(new SGVec_3d(), new SGVec_3d());

			double boneCount = 1d;
			double bonesInChain = chain.bones.size();

			double angle; 
			double scalar;
			while(currentBone != upTo.parent) {
				
					AbstractAxes currentSimulatedAxes = chain.simulatedLocalAxes.get(currentBone);
					currentBoneDirRay.p1 = currentSimulatedAxes.origin_(); 
					currentBoneDirRay.p2 = strandTipAxes.origin_();//strandTipAxes.getGlobalOf(new SGVec_3d(0, chain.strandTip.getBoneHeight()/2d, 0));

					goalRay.p1 = currentBoneDirRay.p1;
					goalRay.p2 = pinnedBase; 

					Rot rotateToTarget = new Rot(currentBoneDirRay.heading(), goalRay.heading());//G.averageRotation(rotations);
					//Rot rotateToTarget = new Rot(tracerX.heading(), tracerO.heading(), goalX.heading(), goalO.heading());
					angle = rotateToTarget.getAngle();
					scalar = angle < 0 ? -1 : 1;
					angle = Math.abs(dampening) > Math.abs(angle) ? angle : dampening * scalar;
					angle *= (1f-currentBone.getStiffness());
					//angle *= (boneCount/bonesInChain);
					//System.out.println("--------");
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
				//constraint.setAxesToSnapped(simulatedTipAxes, chain.simulatedConstraintAxes.get(chain.strandTip));
			}
			fabriTwist(chain, upTo);
		}
	}


	/**
	 * performs a sequence of inverse snap to twists from the chaintip to @param upTo, and 
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
					//((AbstractKusudama)childBone.getConstraint()).snapToTwistLimits(currentAxes, childInverseConstraints);
					//childConstraint.snapToInvertedTwistLimit(childAxesCopy, currentAxesCopy, null, childInverseConstraints, currentAxesCopy);

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
				//	AbstractAxes simulatedParentAxes = chain.simulatedLocalAxes.get(currentBone.parent);


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

			/*if(strandList.size() > 1) {
				System.out.print("bone " + System.identityHashCode(b) + " rotations are: ");
				if(((GiftedApprentice_JME.Elements.Armatures.Bone)b).isSelected()) {
					//System.out.println("let's see how many strands this bone is part of.");
					System.out.println("*****");
				}
			}*/


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

				/*wT += current.getQ0();
				xT += current.getQ1();
				yT += current.getQ2();
				zT += current.getQ3();
				totalCount ++;*/
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
			//for
			//b.rotateBy(avg);

			//TODO DEBUG: TEST WHAT HAPPENS IF I ENABLE SNAPPING TO CONSTRAINTS HERE

			//b.snapToConstraints();
			collection.setDeltaMeasureForBone(b, avg.getAngle());
		}

	}	


	private void applyAverageWeightedRotationsToSimulationAxes_fabrik(StrandedArmature collection, boolean snap) {

		int i =0; 
		for(AbstractBone b : collection.allBonesInStrandCollection) {
			ArrayList<Strand> strandList = collection.boneStrandMap.get(b);
			double totalCount = 0;
			double wT = 0;
			double xT = 0; 
			double yT = 0; 
			double zT = 0;


			if( i == 0) {
				SGVec_3d pos =new SGVec_3d(); 
				for(Strand s : strandList) {
					pos.add(s.simulatedLocalAxes.get(b).origin_()); 
				}
				pos.div(strandList.size());
				AbstractAxes avgax = collection.averageSimulatedAxes.get(b);
				avgax.translateTo(pos);
				avgax.updateGlobal();
				avgax.updateGlobal();
			}

			SGVec_3d avgPos = new SGVec_3d(); 
			double totalFreedomForBone = 0d; 			
			for(Strand s : strandList) {
				AbstractAxes simulatedLocal = s.simulatedLocalAxes.get(b);
				Rot r = simulatedLocal.localMBasis.rotation;
				Quaternion current = G.getSingleCoveredQuaternion(
						new Quaternion(r.rotation.getQ0(), 
								r.rotation.getQ1(),
								r.rotation.getQ2(), 
								r.rotation.getQ3()), 
						initialQ);

				double freedomForBoneThisStrand = getAbilityBiasing() ? s.getRemainingFreedomAtBone(b) : 1d; 
				double weight = 1d/freedomForBoneThisStrand;
				totalFreedomForBone += weight;
				wT += (current.getQ0() * weight);
				xT += (current.getQ1() * weight);
				yT += (current.getQ2() * weight);
				zT += (current.getQ3() * weight);				
				avgPos.add(SGVec_3d.mult(simulatedLocal.localMBasis.translate, weight));
			}
			Rot avg = new Rot(wT/totalFreedomForBone, xT/totalFreedomForBone, yT/totalFreedomForBone, zT/totalFreedomForBone, true);
			AbstractAxes composedAxes = collection.averageSimulatedAxes.get(b);
			composedAxes.localMBasis.rotateTo(avg); 
			if(i != 0)
				composedAxes.localMBasis.translateTo(b.localAxes().localMBasis.translate);
			composedAxes.markDirty();
			composedAxes.updateGlobal();
			collection.setDeltaMeasureForBone(b, avg.getAngle());
			i++;
		}
	}	

	//debug code -- use to set a minimum distance an effector must move
	// in order to trigger a chain iteration 
	double debugMag = 5f; 
	SGVec_3d lastTargetPos = new SGVec_3d(); 

	/**
	 * Experimental: 
	 * 
	 * like Fabrik solver but orientation aware and applies bone constraints 
	 * bidirectionally. Such that on the forward pass, bones are constrained with respect to the 
	 * inverse constraints of their children, and on the backward's pass bones are constrained with 
	 * respect to the constraints of their parent. 
	 * 
	 * For now, this uses a strangedArmature instead of a segmentedArmature. 
	 * Which means the flow should go as follows: 
	 * 
	 * 1. Declare our rootTarget to be the tip of the rootBone of the dragged pin's parent. 
	 * 	a. If the root isn't pinned, then compute it dynamically as the average of the roots of each strand after a forward pass. 
	 * 2. Declare our innerForwardTargets to be the pins emanating from this root.  
	 * 3. Do a forward pass on all Strands between the rootTarget and innerForwardTargets.
	 * 	a. recompute the root if necessary. 
	 * 4. Do a backward pass on all Strands in this StrandedArmature (with forward constraints) down to the rootTarget.
	 * 6. Take the tip of the bone whose pin was being dragged, and set it as the root target.  
	 * 7. Do a forward and backward pass on any strands emanating from it. 
	 * 8. Do that recursively on any strands emanating from /their/ pinned tips
	 *     
	 */
	private void biConstrainedFabrikSolver(AbstractBone bone, double dampening, int iterations) {
		IKSolverStability = 0d;
		StrandedArmature thisStrandCollection = strandedArmature.getStrandCollectionFor(bone);
		if(thisStrandCollection != null && thisStrandCollection.strands.size() > 0) {				
			strandedArmature.alignSimulationAxesToBones();
			SGVec_3d averageBasePosition = new SGVec_3d();
			SGVec_3d backwardTarget = null; 
			if(thisStrandCollection.basePinned && thisStrandCollection.strandRoot.parent != null) 
				backwardTarget = thisStrandCollection.strandRoot.parent.getTip_();
			for(int i =0; i<iterations; i++) {
				if(thisStrandCollection != null) {	
					averageBasePosition = 	iterateStrandsForwardFromTipsToRoot(thisStrandCollection, dampening);			
				}	
				if(!thisStrandCollection.basePinned) 
					backwardTarget = averageBasePosition;
				iterateStrandsBackwardFromRootToTips(thisStrandCollection, backwardTarget);
			}		
			applyAverageWeightedRotationsToSimulationAxes_fabrik(thisStrandCollection, false);
			strandedArmature.alignBonesToSimulationAxes();			
		}	
	}

	/**
	 * begins at the outermost pinned strands of the input stranded armature and does a single outward pulling fabrik iteration with inverted constraints 
	 * on each until reaching the pinned root, then calls itself again on any pinned parent strands until the full armature's root is reached. 
	 * 
	 * note that this should iterate bone-wise, so that the rotation as per each strand is stored for each bone, and then snapped. 
	 * This way, child bones know the correct direction to head in if their parent was in superposition on the forward pass  
	 * @param starm
	 * @return averageBasePosition the averaged position of the bases of each strand after the forward pass 
	 */
	private SGVec_3d  iterateStrandsForwardFromTipsToRoot(StrandedArmature starm, double dampening) {
		SGVec_3d basePosition = new SGVec_3d(); 
		for(Strand s : starm.strands) {
			basePosition.add(
					forwardIterateFabrikStrand(s, s.strandRoot));			
		}
		return basePosition.div(starm.strands.size());
	}

	/**beings at the innermost strandroot of the input stranded armature and does a single inward pulling fabrik iteration with 
	 * true constraints on each bone emerging from the strand-root.   
	 * 
	 * @param starm
	 */
	private void iterateStrandsBackwardFromRootToTips(StrandedArmature starm, SGVec_3d backwardTarget) {
		for(Strand s : starm.strands) {
			backwardIterateFabrikStrand(s, backwardTarget);			
		}
	}

	private void backwardIterateFabrikStrand(Strand chain, SGVec_3d backwardRootTarget) {
		AbstractAxes currentAxes = chain.simulatedLocalAxes.get(chain.strandRoot);
		AbstractAxes currentAxesCopy = currentAxes.getGlobalCopy();
		AbstractAxes childAxesCopy = chain.simulatedLocalAxes.get(chain.bones.get(1)).getGlobalCopy();
		AbstractBone currentBone = chain.strandRoot;
		SGVec_3d boneTip = childAxesCopy.origin_(); 
		SGVec_3d backwardTarget = backwardRootTarget.copy(); 
		int i= chain.bones.size()-1; 
		while(i >= 0) {
			currentBone = chain.bones.get(i);
			if(i>0) { 
				childAxesCopy.alignGlobalsTo(chain.simulatedLocalAxes.get(chain.bones.get(i-1)));
				boneTip = childAxesCopy.origin_();
			} else {
				boneTip = chain.strandTip.getPinnedAxes().origin_(); 
			}

			if(i< chain.bones.size()-1) {				
				AbstractBone parentBone =  currentBone.getParent();
				AbstractAxes parentAxes = chain.simulatedLocalAxes.get(parentBone);
				backwardTarget = parentAxes.y_().getScaledTo(parentBone.getBoneHeight());
			}

			currentAxes = chain.simulatedLocalAxes.get(currentBone);
			if(!currentBone.getIKOrientationLock()) {
				sgRay backwardBoneRay = new sgRay(boneTip, currentAxes.origin_());
				sgRay backwardTargetRay = new sgRay(boneTip, backwardTarget);
				Rot pointToParent = new Rot(backwardBoneRay.heading(), backwardTargetRay.heading()); 
				currentAxes.rotateBy(pointToParent);
			}
			currentAxes.translateTo(backwardTarget);
			currentBone.setAxesToSnapped(currentAxes, chain.simulatedConstraintAxes.get(currentBone));

			if(i>0)  
				chain.simulatedLocalAxes.get(chain.bones.get(i-1)).alignGlobalsTo(childAxesCopy);

			i--;
		}
	}

	private SGVec_3d forwardIterateFabrikStrand(Strand chain, AbstractBone upTo) {
		AbstractBone childBone = null; 
		AbstractAxes pinAxes = chain.strandTip.getPinnedAxes();
		AbstractAxes tipTracerAxes = chain.getStrandTipTracerAxes(); 
		tipTracerAxes.updateGlobal();
		AbstractAxes tempAxes = chain.strandTempAxes;
		AbstractAxes simulatedTipAxes = chain.simulatedLocalAxes.get(chain.strandTip);
		//align the strandTempAxes with the strandTip perfectly, 
		//since they are parented to the tracer, they will be correctly oriented 
		//once we set the tracer to realign with the pinnedAxes
		tempAxes.alignGlobalsTo(simulatedTipAxes);
		tempAxes.setParent(tipTracerAxes);
		//set the tracer to realign with the pinned axes. 
		tipTracerAxes.updateGlobal(); pinAxes.updateGlobal();
		Rot dampenedRotAlign = getDampenedRotationBetween(tipTracerAxes.globalMBasis.rotation, pinAxes.globalMBasis.rotation, dampening);
		//tipTracerAxes.rotateBy(dampenedRotAlign); 
		tipTracerAxes.updateGlobal();
		tipTracerAxes.alignGlobalsTo(pinAxes);
		//tipTracerAxes.y().getScaledTo(pinAxes);
		//tipTracerAxes.translateTo(pinAxes.origin());
		tempAxes.updateGlobal();

		//now that we know the orientation and location of our strand tip, 
		//we set our simulated boneAxes for the tip to have the same location and orientation. 

		AbstractAxes targetAxesCopy = tempAxes.getGlobalCopy();
		simulatedTipAxes.alignGlobalsTo(tempAxes);
		simulatedTipAxes.updateGlobal();
		AbstractAxes tempChildAxesCopy = simulatedTipAxes.getGlobalCopy();
		AbstractAxes tempCurrentAxesCopy = tempChildAxesCopy.getGlobalCopy();
		childBone = chain.strandTip;
		AbstractBone currentBone = childBone.parent;		
		AbstractAxes childInverseConstraints = chain.simulatedConstraintAxes.get(chain.strandTip).getGlobalCopy();
		AbstractKusudama childConstraint = ((AbstractKusudama)childBone.getConstraint());
		if(childConstraint != null) {
			((AbstractKusudama)childBone.getConstraint()).setAxesToInverseLimitingAxes(
					chain.simulatedLocalAxes.get(currentBone), 
					simulatedTipAxes, 
					chain.simulatedConstraintAxes.get(childBone), 
					childInverseConstraints);
		}


		SGVec_3d currentTipLocal = new SGVec_3d(0,1,0);
		int index = 0; 

		while(childBone != upTo) {

			if(currentBone != chain.strandTip) {
				//System.out.println("----");
				AbstractAxes simulatedCurrentAxes = chain.simulatedLocalAxes.get(currentBone);
				currentTipLocal.x = 0; currentTipLocal.z = 0;
				currentTipLocal.y = currentBone.getBoneHeight();
				AbstractAxes simulatedChildAxes = chain.simulatedLocalAxes.get(childBone);
				tempChildAxesCopy.alignGlobalsTo(simulatedChildAxes);
				tempChildAxesCopy.updateGlobal();
				double targetRadius = simulatedCurrentAxes.origin_().dist(tempChildAxesCopy.origin_());
				if(!currentBone.getIKOrientationLock()) {
					sgRay currentBoneRay = new sgRay(simulatedCurrentAxes.origin_().copy(), simulatedCurrentAxes.getGlobalOf(currentTipLocal));
					sgRay toChildBase = new sgRay(simulatedCurrentAxes.origin_().copy(), simulatedChildAxes.origin_().copy());
					Rot pointToChild = new Rot(currentBoneRay.heading(), toChildBase.heading());
					SGVec_3d axis = pointToChild.getAxis(); double dampenedAngle = Math.min(pointToChild.getAngle(), dampening); 
					Rot dampenedPointToChild = new Rot(axis, dampenedAngle);
					simulatedCurrentAxes.rotateBy(pointToChild);
				}

				SGVec_3d originTarget = SGVec_3d.sub(tempChildAxesCopy.origin_(), simulatedCurrentAxes.getGlobalOf(currentTipLocal)); 
				simulatedCurrentAxes.translateTo(SGVec_3d.add(originTarget, simulatedCurrentAxes.origin_()));
				simulatedCurrentAxes.updateGlobal();							

				childConstraint = ((AbstractKusudama)childBone.getConstraint());
				if(childConstraint!= null) {
					tempCurrentAxesCopy.alignGlobalsTo(simulatedCurrentAxes);
					childConstraint.snapToInvertedLimit(tempChildAxesCopy, null, simulatedCurrentAxes, childInverseConstraints, tempCurrentAxesCopy, true, true);
					simulatedCurrentAxes.alignGlobalsTo(tempCurrentAxesCopy);
				}
				simulatedChildAxes.alignGlobalsTo(tempChildAxesCopy);
				simulatedChildAxes.updateGlobal();

				AbstractAxes thisSimulatedConstraint = chain.simulatedConstraintAxes.get(currentBone);
				if(thisSimulatedConstraint != null) {
					AbstractAxes simulatedParentAxes = chain.simulatedLocalAxes.get(currentBone.parent); 
					if(simulatedParentAxes == null) simulatedParentAxes = currentBone.getParent().localAxes(); 
					try {
						((AbstractKusudama)currentBone.getConstraint()).setAxesToInverseLimitingAxes(
								simulatedParentAxes, 
								simulatedCurrentAxes, 
								thisSimulatedConstraint, 
								childInverseConstraints);
					} catch(Exception e) {
						System.out.println("?");
					}
				}

				Rot thisTipTargetDelta = getRotationBetween(chain.simulatedLocalAxes.get(chain.strandTip), targetAxesCopy);
				SGVec_3d thisTipPosDelta = SGVec_3d.sub(chain.simulatedLocalAxes.get(chain.strandTip).origin_(), targetAxesCopy.origin_());

				childBone = currentBone;
				currentBone = currentBone.parent;
				index++; 
			}
		}
		return chain.simulatedLocalAxes.get(upTo).origin_();
	}

	public Rot getDampenedRotationBetween(Rot from, Rot to, double clamp) {
		Rot result = to.applyTo(from.revert());
		SGVec_3d axis = result.getAxis(); 
		double aangle =result.getAngle(); 
		aangle = Math.min(aangle, dampening);
		Rot rotTo = new Rot(axis, aangle);
		return rotTo;
	}

	private void iterateFabriCCDStrand(Strand chain, double dampening) {
		debugMag = 5f;
		SGVec_3d newPin = chain.strandTip.getPinnedAxes().origin_();
		double newSize = SGVec_3d.sub(newPin, lastTargetPos).mag(); 
		//if(newSize > debugMag) { 
		//lastTargetPos.set(newPin);
		forwardIterateFabricCCDStrand(chain, dampening);

		//backwardIterateFabricCCDStrand(chain, dampening);
		//}

	}

	private void forwardIterateFabricCCDStrand(Strand chain, double dampening) {

		AbstractBone childBone = null; 
		AbstractAxes pinAxes = chain.strandTip.getPinnedAxes();

		//align the tracer axes to be oriented with the tip bone, and originate about its center
		AbstractAxes tipTracerAxes = chain.getStrandTipTracerAxes(); 
		tipTracerAxes.updateGlobal();
		AbstractAxes tempAxes = chain.strandTempAxes;

		//align the strandTempAxes to be aligned with the strandTip perfectly, 
		//since they are parented to the tracer, they will be correctly oriented 
		//once we set the tracer to realign with the pinnedAxes
		chain.strandTempAxes.alignGlobalsTo(chain.simulatedLocalAxes.get(chain.strandTip));

		//set the tracer to realign with the pinned axes. 
		chain.strandTempAxes.updateGlobal(); pinAxes.updateGlobal();
		Rot dampenedRotAlign = getDampenedRotationBetween(chain.strandTipTracerAxes.globalMBasis.rotation, pinAxes.globalMBasis.rotation, dampening);
		chain.strandTipTracerAxes.rotateBy(dampenedRotAlign); 
		chain.strandTipTracerAxes.translateTo(pinAxes.origin_());
		chain.strandTempAxes.updateGlobal();


		//now that we know the orientation and location of our strand tip, 
		//we set our simulated boneAxes for the tip to have the same location and orientation. 
		AbstractAxes simulatedTipAxes = chain.simulatedLocalAxes.get(chain.strandTip);
		AbstractAxes targetAxesCopy = chain.strandTempAxes.getGlobalCopy();
		//Rot tipTargetDelta = getRotationBetween(chain.simulatedLocalAxes.get(chain.strandTip), targetAxes);
		double lastRotDelta = 0d;//tipTargetDelta.getAngle(); 
		SGVec_3d lastTipPosDelta = new SGVec_3d();//SGVec_3d.sub(chain.simulatedLocalAxes.get(chain.strandTip).origin(), targetAxes.origin());
		simulatedTipAxes.alignGlobalsTo(chain.strandTempAxes);
		simulatedTipAxes.updateGlobal();
		AbstractAxes tempChildAxesCopy = targetAxesCopy.getGlobalCopy();

		childBone = chain.strandTip;
		AbstractBone currentBone = childBone.parent;




		SGVec_3d currentTipLocal = new SGVec_3d(0,1,0);
		int index = 0; 
		//System.out.println("##############");
		while(childBone != chain.strandRoot) {

			if(currentBone != chain.strandTip) {
				//System.out.println("----");
				AbstractAxes simulatedCurrentAxes = chain.simulatedLocalAxes.get(currentBone);
				currentTipLocal.y = currentBone.getBoneHeight();
				AbstractAxes simulatedChildAxes = chain.simulatedLocalAxes.get(childBone);
				tempChildAxesCopy.alignGlobalsTo(simulatedChildAxes);
				tempChildAxesCopy.updateGlobal();
				if(!currentBone.getIKOrientationLock()) {
					sgRay currentBoneRay = new sgRay(simulatedCurrentAxes.origin_().copy(), simulatedCurrentAxes.getGlobalOf(currentTipLocal));
					sgRay toChildBase = new sgRay(simulatedCurrentAxes.origin_().copy(), simulatedChildAxes.origin_().copy());

					Rot pointToChild = new Rot(currentBoneRay.heading(), toChildBase.heading());
					simulatedCurrentAxes.rotateBy(pointToChild);

					simulatedChildAxes.alignGlobalsTo(tempChildAxesCopy);
					simulatedChildAxes.updateGlobal();
				}

				SGVec_3d originTarget = SGVec_3d.sub(tempChildAxesCopy.origin_(), simulatedCurrentAxes.getGlobalOf(currentTipLocal)); 
				simulatedCurrentAxes.translateTo(SGVec_3d.add(originTarget, simulatedCurrentAxes.origin_()));
				simulatedCurrentAxes.updateGlobal();				

				simulatedChildAxes.alignGlobalsTo(tempChildAxesCopy);
				//childBone.setAxesToSnapped(simulatedChildAxes, chain.simulatedConstraintAxes.get(childBone));
				simulatedChildAxes.updateGlobal();			

				Rot thisTipTargetDelta = getRotationBetween(chain.simulatedLocalAxes.get(chain.strandTip), targetAxesCopy);
				SGVec_3d thisTipPosDelta = SGVec_3d.sub(chain.simulatedLocalAxes.get(chain.strandTip).origin_(), targetAxesCopy.origin_());
				/*if(Math.abs(thisTipTargetDelta.getAngle()) > Math.abs(lastRotDelta) || (thisTipPosDelta.mag() > lastTipPosDelta.mag()*2 && thisTipPosDelta.mag() > chain.strandTip.boneHeight/3d)) {
					//System.out.println("rectifying angle " + thisTipTargetDelta.getAngle());
					//System.out.println("rectifying pos, prevMag :  " + (float)lastTipPosDelta.mag() + " thisMag : " + (float)thisTipPosDelta.mag());
					rectifyChain(chain, currentBone); 
					thisTipTargetDelta = getRotationBetween(chain.simulatedLocalAxes.get(chain.strandTip), targetAxes);
					thisTipPosDelta = SGVec_3d.sub(chain.simulatedLocalAxes.get(chain.strandTip).origin(), targetAxes.origin());
				} else {
					//if(index == 1) {
					//System.out.println("not rectifying because");
					//System.out.println(Math.abs(thisTipTargetDelta.getAngle()) + " < " + lastRotDelta);
					//}
				}*/

				lastRotDelta = Math.abs(thisTipTargetDelta.getAngle()); //System.out.println(lastRotDelta);
				lastTipPosDelta = thisTipPosDelta;

				childBone = currentBone;
				currentBone = currentBone.parent;
				index++; 
			}
		}
	}


	public void rectifyChain(Strand chain, AbstractBone upTo) {
		/**debug code, split this out into its own "simpleRectify" function**/


		//CCDRectify(Math.toRadians(170), chain, upTo, false);

		AbstractAxes rectificationAxes = chain.simulatedLocalAxes.get(chain.strandTip).getGlobalCopy();
		rectificationAxes.translateTo(SGVec_3d.div(SGVec_3d.add(rectificationAxes.origin_(), rectificationAxes.y_().getScaledTo(chain.strandTip.getBoneHeight())), 2d));
		AbstractAxes previousParent = chain.simulatedLocalAxes.get(upTo).getParentAxes();
		chain.simulatedLocalAxes.get(upTo).emancipate();
		chain.simulatedLocalAxes.get(upTo).setParent(rectificationAxes);
		Rot toPinned = new Rot(rectificationAxes.x_().heading(), rectificationAxes.y_().heading(), chain.strandTip.getPinnedAxes().x_().heading(), chain.strandTip.getPinnedAxes().y_().heading());
		double angle = toPinned.getAngle();
		SGVec_3d axis = toPinned.getAxis();
		toPinned = new Rot(axis, Math.max(angle,Math.toRadians(5)));
		rectificationAxes.rotateBy(toPinned);
		rectificationAxes.translateTo(chain.strandTip.getPinnedAxes().origin_());
		//rectificationAxes.alignGlobalsTo(chain.strandTip.getPinnedAxes());
		chain.simulatedLocalAxes.get(upTo).emancipate();
		chain.simulatedLocalAxes.get(upTo).setParent(previousParent);
		//CCDRectify(Math.toRadians(10), chain, upTo, false);

		/*for(int i = 0; i < 5; i++) {
			CCDRectify(Math.toRadians(10), chain, upTo);
		}*/
	}


	private void backwardIterateFabricCCDStrand(Strand chain, double dampening) {
		//AbstractAxes rootAxes = chain.simulatedLocalAxes.get(chain.strandRoot);
		AbstractAxes tempChildAxes = chain.strandTipTracerAxes.getGlobalCopy();
		for(int i = chain.bones.size()-1; i >= 0; i--) {
			AbstractBone currentBone = chain.bones.get(i);
			AbstractBone childBone = null; 
			if(i > 0) childBone = chain.bones.get(i-1);

			if(currentBone.parent != null) {
				AbstractAxes simulatedCurrentAxes = chain.simulatedLocalAxes.get(currentBone);
				AbstractAxes simulatedParentAxes = chain.simulatedLocalAxes.get(currentBone.parent);
				AbstractAxes simulatedChildAxes = chain.simulatedLocalAxes.get(childBone);

				if(childBone != null) {
					tempChildAxes.alignGlobalsTo(simulatedChildAxes);
					if(childBone.parent != currentBone) {
						System.out.println("MISMATCH!!");

					}
				} else {
					simulatedChildAxes = chain.strandTip.getPinnedAxes();
					tempChildAxes.alignGlobalsTo(simulatedChildAxes);
				}

				SGVec_3d currentTip = simulatedChildAxes.origin_();

				if(simulatedParentAxes == null) simulatedParentAxes = simulatedCurrentAxes.getParentAxes();
				if(simulatedParentAxes == null) simulatedParentAxes = currentBone.parent.localAxes();
				sgRay currentReverseDirection = new sgRay(currentTip, simulatedCurrentAxes.origin_());
				//sgRay alternateCurrentDirection = new sgRay(simulatedChildAxes.origin(), simulatedCurrentAxes.origin());
				sgRay toParentTip = new sgRay(currentReverseDirection.p1, simulatedParentAxes.y_().getScaledTo(currentBone.parent.boneHeight));
				if(!currentBone.getIKOrientationLock()) {
					Rot pointAwayFromParent = new Rot(currentReverseDirection.heading(), toParentTip.heading());
					simulatedCurrentAxes.rotateBy(pointAwayFromParent);
					simulatedCurrentAxes.updateGlobal();
				}
				//SGVec_3d translateToParent = SGVec_3d.sub(simulatedParentAxes.y().getScaledTo(currentBone.parent.getBoneHeight()), simulatedCurrentAxes.origin());
				simulatedCurrentAxes.translateTo(toParentTip.p2);
				//currentBone.setAxesToSnapped(simulatedCurrentAxes, chain.simulatedConstraintAxes.get(currentBone));

				if(childBone != null) { 
					simulatedChildAxes.alignGlobalsTo(tempChildAxes);
					simulatedChildAxes.updateGlobal();
				}
			}
		}

		//chain.simulatedLocalAxes.get(chain.strandTip).translateTo(chain.simulatedLocalAxes.get(chain.strandTip.parent).y().getScaledTo(chain.strandTip.parent.getBoneHeight()));

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
