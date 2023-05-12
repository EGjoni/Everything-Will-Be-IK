package IK.doubleIK.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;

import IK.doubleIK.AbstractArmature;
import IK.doubleIK.AbstractBone;
import IK.doubleIK.SegmentedArmature;
import IK.doubleIK.solver.ArmatureSegment.WorkingBone;
import IK.doubleIK.solver.SkeletonState.BoneState;
import IK.doubleIK.solver.SkeletonState.TransformState;
import math.doubleV.AbstractAxes;

/**
 * builds maintains an IKSolver specific representation of the Armature topology
 * @author rufsketch1
 */
public class ShadowSkeleton {
	AbstractArmature forArmature = null;
	AbstractAxes[] simTransforms = null;
	AbstractAxes shadowSpace = null;
	WorkingBone[] traversalArray;
	ArmatureSegment rootSegment = null;
	SkeletonState skelState =null;
	

	/**
	 * builds a shadowSkeleton from the given SkeletonState object
	 * */
	public ShadowSkeleton(SkeletonState skelState, AbstractArmature forArmature) {
		this.forArmature = forArmature;
		this.skelState = skelState;
		this.buildSimTransformsHierarchy();
		this.buildArmaturSegmentHierarchy();
		this.buildTraversalArray();
	}
	
	/**
	 * @param notifier a (potentially threaded) function to call every time the solver has updated the transforms for a given bone. 
	 * Called once per solve, per bone. NOT once per iteration.
	 * This can be used to take advantage of parallelism, so that you can update your Bone transforms while this is still writing into the skelState TransformState list
	 */
	public void solve(double dampening, int iterations, int stabilizationPasses, Consumer<BoneState> notifier) {
		int endOnIndex = traversalArray.length - 1;
		//int returnfullEndOnIndex = returnfulArray.length > 0 ? returnfulIndex.get(startFrom);  
		int tipIndex = 0;
		ArmatureSegment forSegment = rootSegment;
		iterations = iterations == -1 ? forArmature.getDefaultIterations() : iterations;
		double totalIterations = iterations;
		stabilizationPasses = stabilizationPasses == -1 ? forArmature.defaultStabilizingPassCount : stabilizationPasses;

		alignSimAxesToBoneStates();

		for (int i = 0; i < iterations; i++) {
			//if(i < totalIterations - 1) {
				for (int j = 0; j <= endOnIndex; j++) {
					traversalArray[j].pullBackTowardAllowableRegion(i, iterations);
				}
			//}
			for (int j = 0; j <= endOnIndex; j++) {
				traversalArray[j].fastUpdateOptimalRotationToPinnedDescendants(dampening,
						j == endOnIndex && endOnIndex == traversalArray.length - 1);
			}		
		}

		if(notifier == null) alignBoneStatesToSimAxes();
		else alignBoneStatesToSimAxes(notifier);
		//iterativelyNotifyBonesOfCompletedIKSolution(tipIndex, endOnIndex);
	}
	private void alignSimAxesToBoneStates() {
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
		this.shadowSpace = forArmature.localAxes().freeCopy();
		shadowSpace.toIdentity();
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
		for(ArmatureSegment segment: segmentTraversalArray) {
			Collections.addAll(reversedTraversalArray, segment.reversedTraversalArray);
		}
		this.traversalArray = new WorkingBone[reversedTraversalArray.size()];
		int j = 0;
		for(int i = reversedTraversalArray.size()-1 ; i>=0; i--) {
			this.traversalArray[j] = reversedTraversalArray.get(i);
			j++;
		}
	}

	public void updateRates() {
		for (int j = 0; j < traversalArray.length; j++) {
			traversalArray[j].updateCosDampening();
		}
	}
	
}
