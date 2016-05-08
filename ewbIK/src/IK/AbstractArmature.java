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
import java.util.Collection;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.apache.commons.math3.complex.Quaternion;

import IK.StrandedArmature.Strand;
import processing.core.PApplet;
import sceneGraph.*;
/**
 * @author Eron Gjoni
 *
 */

public abstract class AbstractArmature {
	protected AbstractAxes localAxes;
	protected ArrayList<AbstractBone> bones = new ArrayList<AbstractBone>();
	protected HashMap<String, AbstractBone> boneMap = new HashMap<String, AbstractBone>();
	protected AbstractBone rootBone;
	public SegmentedArmature segmentedArmature;
	public StrandedArmature strandedArmature;
	protected String tag;

	public double IKSolverStability = 0d; 


	public AbstractArmature(AbstractAxes inputOrigin, String name) {
		this.localAxes = inputOrigin; 
		this.tag = name;
		createRootBone(localAxes.y().heading(), localAxes.z().heading(), tag+" : rootBone", 1d, AbstractBone.frameType.GLOBAL);
	}

	public AbstractArmature(PApplet app, AbstractAxes inputOrigin, String name) {
		this.localAxes = inputOrigin; 
		this.tag = name;
		createRootBone(app, localAxes.y().heading(), localAxes.z().heading(), tag+" : rootBone", 1d, AbstractBone.frameType.GLOBAL);
	}

	public AbstractArmature(PApplet app, AbstractAxes inputOrigin, String name, boolean empty) {
		this.localAxes = inputOrigin;
		this.tag = name; 
	}

	public AbstractBone createRootBone(AbstractBone inputBone) {
		this.rootBone = inputBone;
		this.segmentedArmature = new SegmentedArmature(rootBone);
		this.strandedArmature = new StrandedArmature(rootBone);
		return rootBone;
	}

	private AbstractBone createRootBone(DVector tipHeading, DVector rollHeading, String inputTag, double boneHeight, AbstractBone.frameType coordinateType) {
		initializeRootBone(this, tipHeading, rollHeading, inputTag, boneHeight, coordinateType);
		this.segmentedArmature = new SegmentedArmature(rootBone);
		this.strandedArmature = new StrandedArmature(rootBone);
		return rootBone;
	}

	private AbstractBone createRootBone(Object app, DVector tipHeading, DVector rollHeading, String inputTag, double boneHeight, AbstractBone.frameType coordinateType) {
		initializeRootBone(app, this, tipHeading, rollHeading, inputTag, boneHeight, coordinateType);
		this.segmentedArmature = new SegmentedArmature(rootBone);
		this.strandedArmature = new StrandedArmature(rootBone);
		return rootBone;
	}

	protected abstract void initializeRootBone(AbstractArmature armature, 
			DVector tipHeading, DVector rollHeading, 
			String inputTag, 
			double boneHeight, 
			AbstractBone.frameType coordinateType);

	protected abstract void initializeRootBone(Object app, AbstractArmature armature, 
			DVector tipHeading, DVector rollHeading, 
			String inputTag, 
			double boneHeight, 
			AbstractBone.frameType coordinateType);

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
	protected void addToBoneList(AbstractBone abstractBone) {
		if(!bones.contains(abstractBone)) {
			bones.add(abstractBone);
			boneMap.put(abstractBone.tag, abstractBone);
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
	 */
	protected void updateArmatureSegments() {
		segmentedArmature.updateSegmentedArmature();
		strandedArmature.updateStrandedArmature();
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
	 * This solver aims to minimize the total distance to the target pins, however if the pins
	 * are impossible to reach AND the armature is under-constrained, this solver can become unstable. 
	 * 
	 * That said, it is quite stable when the chain armature is sufficiently constrained. 
	 * That is -- if you constrain all bones affected by the solver with Kusudamas such that 
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
			for(int i = 0; i < iterations; i++) {
				if(startFrom != null /*&& (startFrom.basePinned || startFrom.tipPinned)*/) 
					solveIK(startFrom, dampening, 1);//iterations);
				else if (thisChain.tipPinned)
					solveIK(segmentedArmature, dampening, 1);//iterations);
			}
		}// if there's only one pin, the armature automatically gets translated
		//to meet its requirement as an inherent part of the algorithm anyway, and I can't imagine any instance where it is not 
		//acceptable to do so. so, doing so.
	}

	protected void solveIK(SegmentedArmature chain, double dampening, int iterations) {
		ArrayList<SegmentedArmature> pinnedChains = chain.pinnedDescendants;

		if(!chain.basePinned) {
			DVector translateBy = new DVector(0,0,0);
			for(SegmentedArmature pc : pinnedChains) {
				Ray tipToTargetRay = new Ray(pc.segmentTip.getTip(), pc.segmentTip.pinnedTo());
				translateBy.add(tipToTargetRay.heading());
			}
			translateBy.mult(1d/pinnedChains.size());
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

	}


	protected void solveIKChain(SegmentedArmature chain, double dampening, int iterations) {		  
		for(int i = 0 ; i<iterations; i++) {
			iterateCCD(dampening, chain); 
		}
	}

	private void iterateCCD(double dampening, SegmentedArmature chain) {
		AbstractBone currentBone = chain.segmentTip; 
		ArrayList<Rot> rotations = new ArrayList<Rot>();

		for(int i=0; i<=chain.chainLength; i++) {

			rotations.clear();
			/*if(currentBone.constraints != null) { 
				currentBone.constraints.limitingAxes.globalCoords = currentBone.constraints.limitingAxes.relativeTo(currentBone.constraints.limitingAxes.parent.globalCoords);
			}*/

			for(SegmentedArmature pinnedTarget : chain.pinnedDescendants) {
				Ray currentRay = new Ray(currentBone.getBase(), pinnedTarget.segmentTip.getTip()); 
				Ray goalRay = new Ray(currentBone.getBase(), pinnedTarget.segmentTip.pinnedTo()); 
				rotations.add(new Rot(currentRay.heading(), goalRay.heading()));
			}  

			Rot rotateToTarget = G.averageRotation(rotations);

			double angle = rotateToTarget.getAngle(); 
			DVector axis = rotateToTarget.getAxis();

			angle = Math.min(angle, dampening);
			currentBone.localAxes.rotateTo(new Rot(axis, angle));   

			currentBone.snapToConstraints();   
			currentBone = currentBone.parent;

		}
	}


	/** 
	 * Highly stable even when under-constrained, and also when over-constrained (by Kusudamas)
	 *
	 * Converges on a solution satisfying all pins and constraints where possible. 
	 * converges on something that looks like a polite but not especially strenuous
	 * effort toward reaching the target pins when impossible. 
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
		 * (as an optimization, only reset the orientation of the bone is mapped to multiple strands in boneStrandsMap;
		 */		

		for(int i =0; i<iterations; i++) {
			if(!collection.basePinned) {
				DVector translateBy = new DVector(0,0,0);
				for(Strand s : strands) {
					Ray tipToTargetRay = new Ray(s.strandTip.getTip(), s.strandTip.pinnedTo());
					translateBy.add(tipToTargetRay.heading());
				}
				translateBy.mult(1d/(double)strands.size());
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

			Rot ir = new Rot(new DVector(1,1,1), 0);//strandList.get(0).rotationsMap.get(b);
			Quaternion initialQ = new Quaternion(ir.rotation.getQ0(), ir.rotation.getQ1(), ir.rotation.getQ2(), ir.rotation.getQ3());
			for(Strand s : strandList) {

				double distance = s.distToTarget; 					
				Rot r = s.rotationsMap.get(b);
				//r = new Rot(r.getAxis(), r.getAngle()*(distance/totalDist));
				Quaternion current = G.getSingleCoveredQuaternion(
						new Quaternion(r.rotation.getQ0(), 
								r.rotation.getQ1(),
								r.rotation.getQ2(), 
								r.rotation.getQ3()), 
						initialQ);

				wT += current.getQ0();
				xT += current.getQ1();
				yT += current.getQ2();
				zT += current.getQ3();

				totalCount ++;
				//totalDist += distance;
			}
			Rot avg = new Rot(wT/totalCount, xT/totalCount, yT/totalCount, zT/totalCount, true);
			b.localAxes().rotateTo(avg);
			//b.snapToConstraints();
			collection.setDeltaMeasureForBone(b, avg.getAngle());
		}
	}

	private void iterateCCDStrand(Strand chain, double dampening) {

		//DVector strandTip = chain.strandTip.getTip();
		DVector strandTipPin = chain.strandTip.pinnedTo();
		Ray currentRay = new Ray(new DVector(0,0,0), null);
		Ray goalRay = new Ray(new DVector(0,0,0), null);

		DVector origXHead = new DVector(0,0,0);
		DVector origYHead = new DVector(0,0,0);
		DVector postXHead = new DVector(0,0,0);
		DVector postYHead = new DVector(0,0,0);

		for(AbstractBone currentBone : chain.bones) {

			currentRay.p1 = currentBone.getBase(); 
			currentRay.p2 = chain.strandTip.getTip(); 

			goalRay.p1 = currentRay.p1;
			goalRay.p2 = strandTipPin; 

			origXHead = currentBone.localAxes().x().heading();
			origYHead = currentBone.localAxes().y().heading();

			Rot rotateToTarget = new Rot(currentRay.heading(), goalRay.heading());//G.averageRotation(rotations);

			double angle = rotateToTarget.getAngle();

			angle = Math.min(angle, dampening);

			currentBone.localAxes.rotateTo(new Rot(rotateToTarget.getAxis(), angle));   
			currentBone.snapToConstraints();   			

			postXHead = currentBone.localAxes().x().heading(); 
			postYHead = currentBone.localAxes().y().heading();

			Rot totalRotation = new Rot(origXHead, origYHead, postXHead, postYHead);

			chain.rotationsMap.put(currentBone, totalRotation);

			//currentBone = currentBone.parent;
		}
	}

	private ArrayList<AbstractBone> getStrandFromTip(AbstractBone pinnedBone) {
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
	}

	/**
	 * 
	 * @return a reference to the Axes serving as this Armature's coordinate system. 
	 */
	public AbstractAxes localAxes() {
		return this.localAxes;
	}




}
