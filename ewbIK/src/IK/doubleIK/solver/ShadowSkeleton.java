package IK.doubleIK.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Consumer;

import IK.doubleIK.AbstractArmature;
import IK.doubleIK.AbstractBone;
import IK.doubleIK.solver.ArmatureSegment.WorkingBone;
import IK.doubleIK.solver.SkeletonState.BoneState;
import IK.doubleIK.solver.SkeletonState.TransformState;
import math.doubleV.AbstractAxes;
import math.doubleV.AbstractBasis;
import math.doubleV.CartesianAxes;

/**
 * builds maintains an IKSolver specific representation of the Armature topology
 * @author rufsketch1
 */
public class ShadowSkeleton {
	AbstractAxes[] simTransforms = null;
	AbstractAxes shadowSpace = null;
	WorkingBone[] traversalArray;
	HashMap<BoneState, Integer> boneWorkingBoneIndexMap = new HashMap<BoneState, Integer>();
	ArmatureSegment rootSegment = null;
	SkeletonState skelState = null;
	BoneState lastRequested = null;
	int lastRequestedEndIndex = -1;
	double baseDampening = Math.PI;
	/**
	 * builds a shadowSkeleton from the given SkeletonState object
	 * @param SkeletonState the skeletnState object to write results into / read state from 
	 * */
	public ShadowSkeleton(SkeletonState skelState, double baseDampening) {
		this.skelState = skelState;
		this.shadowSpace = new CartesianAxes();
		this.baseDampening = baseDampening;
		this.buildSimTransformsHierarchy();
		this.buildArmaturSegmentHierarchy();
		this.buildTraversalArray();
	}
	private int previousIterationRequest = 0;
	
	/**
	 * @param iterations how many iterations to run the solver
	 * @param stabilizationPasses set to 0 for maximum speed (but less stability given unreachable situations), 
	 * Set to 1 for maximum stability while following constraints. 
	 * Set to higher than 1 for negligible benefit at considerable cost. 
	 * Set to -1 to indicate constraints can be broken on the last iteration of a solver call
	 * @param solveFrom optional, if given, the solver will only solve for the segment the given bone is on (and any of its descendant segments
	 * @param notifier a (potentially threaded) function to call every time the solver has updated the transforms for a given bone. 
	 * Called once per solve, per bone. NOT once per iteration.
	 * This can be used to take advantage of parallelism, so that you can update your Bone transforms while this is still writing into the skelState TransformState list
	 */
	public void solve(int iterations, int stabilizationPasses, BoneState solveFrom, Consumer<BoneState> notifier) {
		alignSimAxesToBoneStates();
		this.pullBack(iterations, solveFrom, false, null); //start all bones closer to a known comfortable pose.
		for (int i = 0; i < iterations; i++) {
			this.solveToTargets(1, solveFrom, false, null);
		}
		this.conditionalNotify(true, notifier);
	}
	
	
	public void pullBack(int iterations, BoneState solveFrom, boolean doNotify, Consumer<BoneState> notifier) {
		int endOnIndex = getEndOnIndex(solveFrom);
		updateReturnfulnessDamps(iterations);
		for (int j = 0; j <= endOnIndex; j++) {			
			traversalArray[j].pullBackTowardAllowableRegion();
		}
		conditionalNotify(doNotify, notifier);
	}
	
	/**
	 * @param stabilizationPasses set to 0 for maximum speed (but less stability given unreachable situations), 
	 * Set to 1 for maximum stability while following constraints. 
	 * Set to higher than 1 for negligible benefit at considerable cost. 
	 * Set to -1 to indicate constraints can be broken on the last iteration of a solver call
	 * @param solveFrom optional, if given, the solver will only solve for the segment the given bone is on (and any of its descendant segments
	 * @param notifier a (potentially threaded) function to call every time the solver has updated the transforms for a given bone. 
	 * Called once per solve, per bone. NOT once per iteration.
	 * This can be used to take advantage of parallelism, so that you can update your Bone transforms while this is still writing into the skelState TransformState list
	 */
	public void solveToTargets(int stabilizationPasses, BoneState solveFrom, boolean doNotify, Consumer<BoneState> notifier) {
		int endOnIndex = getEndOnIndex(solveFrom);
		boolean translate = endOnIndex == traversalArray.length - 1;
		boolean skipConstraints = stabilizationPasses < 0;
		stabilizationPasses = Math.max(0,  stabilizationPasses);
		for (int j = 0; j <= endOnIndex; j++) {
			WorkingBone wb = traversalArray[j];
			wb.fastUpdateOptimalRotationToPinnedDescendants(stabilizationPasses, j == endOnIndex && translate, skipConstraints);
		}
		conditionalNotify(doNotify, notifier);
	}
	
	/**
	 * lazy lookup. Get the traversal array index for the root of the pinned segment the working bone corresponding to this bonestate resides on, 
	 * but only if it's different than the last one that was tracked.
	 * @param solveFrom
	 * @return
	 */
	private int getEndOnIndex(BoneState solveFrom) {
		if(solveFrom != lastRequested) {
			if(solveFrom == null) {
				lastRequestedEndIndex = traversalArray.length-1;
				lastRequested = null;
			} else {
				Integer idx = boneWorkingBoneIndexMap.get(solveFrom);
				WorkingBone wb = traversalArray[idx];
				WorkingBone root = wb.getRootSegment().wb_segmentRoot;
				lastRequestedEndIndex = (int)boneWorkingBoneIndexMap.get(root.forBone);
				lastRequested = solveFrom;
			}			
		}
		return lastRequestedEndIndex;
	}
	
	/**
	 * lazy update of returnfullness dampening if the number of iterations for this solver call is different than the previous one
	 * @param iterations
	 */
	private void updateReturnfulnessDamps(int iterations) {
		if(previousIterationRequest != iterations) {
			for (int j = 0; j < traversalArray.length; j++) {				
				traversalArray[j].updateReturnfullnessDamp(iterations);
			}
		}
		previousIterationRequest = iterations;
	}
	public void conditionalNotify(boolean doNotify, Consumer<BoneState> notifier) {
		if(doNotify) {
			if(notifier == null) alignBoneStatesToSimAxes();
			else alignBoneStatesToSimAxes(notifier);
		}	
	}
	public void alignSimAxesToBoneStates() {
		TransformState[] transforms = skelState.getTransformsArray();
		for(int i=0; i<transforms.length; i++) {
			this.simTransforms[i] .getLocalMBasis().set(transforms[i].translation, transforms[i].rotation, transforms[i].scale);
			this.simTransforms[i] ._exclusiveMarkDirty(); //we're marking the entire hierarchy dirty anyway, so avoid the recursion
		}
	}
	
	private void alignBoneStatesToSimAxes() {
		for(int i=0; i<traversalArray.length; i++) {
			WorkingBone wb = traversalArray[i];
			alignBone(wb);
		}
	}
	
	private void alignBoneStatesToSimAxes(Consumer<BoneState> notifier) {
		for(int i=0; i<traversalArray.length; i++) {
			WorkingBone wb = traversalArray[i];
			alignBone(wb);
			notifier.accept(wb.forBone);
		}
	}
	
	private void alignBone(WorkingBone wb) {
		BoneState bs = wb.forBone; 
		TransformState ts = bs.getTransform();
		ts.translation = wb.simLocalAxes.localMBasis.translate.get();
		ts.rotation = wb.simLocalAxes.localMBasis.rotation.toArray();
	}
	private void buildSimTransformsHierarchy() {
		int transformCount = skelState.getTransformCount();
		if(transformCount == 0) return;
		
		this.simTransforms = new AbstractAxes[transformCount];
		for(int i=0; i<transformCount; i++) {
			TransformState ts = skelState.getTransformState(i);
			AbstractAxes newTransform = shadowSpace.freeCopy(); 
			newTransform.getLocalMBasis().set(ts.translation, ts.rotation, ts.scale);
			this.simTransforms[i] = newTransform;
		}
		for(int i=0; i<transformCount; i++) {
			TransformState ts = skelState.getTransformState(i);
			int parTSidx = ts.getParentIndex();
			AbstractAxes simT = this.simTransforms[i];
			if(parTSidx == -1)  simT.setRelativeToParent(shadowSpace);
			else simT.setRelativeToParent(this.simTransforms[parTSidx]);
		}
	}
	
	private void buildArmaturSegmentHierarchy() {
		BoneState rootBone = skelState.getRootBonestate();
		if(rootBone == null) return;
		this.rootSegment = new ArmatureSegment(this, rootBone, null, false);
	}
	
	private void buildTraversalArray() {
		if(this.rootSegment == null) return;
		ArrayList<ArmatureSegment> segmentTraversalArray = this.rootSegment.getDescendantSegments();
		ArrayList<WorkingBone> reversedTraversalArray = new ArrayList<>();
		boneWorkingBoneIndexMap.clear();
		for(ArmatureSegment segment: segmentTraversalArray) {
			Collections.addAll(reversedTraversalArray, segment.reversedTraversalArray);
		}
		this.traversalArray = new WorkingBone[reversedTraversalArray.size()];
		int j = 0;
		
		for(int i = reversedTraversalArray.size()-1 ; i>=0; i--) {
			this.traversalArray[j] = reversedTraversalArray.get(i);
			boneWorkingBoneIndexMap.put(traversalArray[j].forBone, j);
			j++;
		}
		
		lastRequested = null;
		lastRequestedEndIndex = traversalArray.length-1;
	}
	
	public void setDampening(double dampening, double defaultIterations) {
		this.baseDampening = dampening;
		this.updateRates(defaultIterations);
	}

	public void updateRates(double iterations) {
		rootSegment.recursivelyCreateHeadingArrays();
		for (int j = 0; j < traversalArray.length; j++) {
			traversalArray[j].updateCosDampening();
			traversalArray[j].updateReturnfullnessDamp(iterations);
		}
	}
	
}
