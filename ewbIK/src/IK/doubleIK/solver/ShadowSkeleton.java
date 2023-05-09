package IK.doubleIK.solver;

import java.util.ArrayList;
import java.util.Collections;

import IK.doubleIK.AbstractArmature;
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
	
	private void buildSimTransformsHierarchy() {
		int transformCount = skelState.getTransformCount();
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
		this.rootSegment = new ArmatureSegment(this, rootBone, null, false);
	}
	
	private void buildTraversalArray() {
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
	
}
