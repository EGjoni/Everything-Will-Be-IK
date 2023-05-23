package IK.doubleIK.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import IK.doubleIK.AbstractArmature;
import IK.doubleIK.AbstractIKPin;
import IK.doubleIK.AbstractKusudama;
import IK.doubleIK.AbstractLimitCone;
import IK.doubleIK.Constraint;
import IK.doubleIK.solver.SkeletonState.BoneState;
import IK.doubleIK.solver.SkeletonState.ConstraintState;
import IK.doubleIK.solver.SkeletonState.TargetState;
import math.doubleV.AbstractAxes;
import math.doubleV.MathUtils;
import math.doubleV.QCP;
import math.doubleV.Rot;
import math.doubleV.SGVec_3d;
import math.doubleV.Vec3d;
import math.doubleV.sgRayd;
/**
 * A segment is defined as any set of bones all solving for the same targets.
 * A segment may have subsegments, which are all bones solving for one or more strict subsets 
 * of the targets the segment solves for.
 * A segment may have child segments, which do not solve for any of the same targets as the parent segment,
 * but whose bones are all descendants of the bones in the parent segment.
 * @author Eron Gjoni
 */
public class ArmatureSegment {
	private ShadowSkeleton shadowSkel;
	public AbstractAxes[] simTransforms;
	//segments which are part of this segment
	private ArrayList<ArmatureSegment> subSegments = new ArrayList<>();
	//segments which are descendants of but not a part of this segment
	private ArrayList<ArmatureSegment> childSegments = new ArrayList<>();
	//all bones on this strand whose transforms the solver is allowed to modify
	public ArrayList<WorkingBone> solvableStrandBones = new ArrayList<>();
	//all bones on this strand
	public ArrayList<WorkingBone> allStrandBones = new ArrayList<>();
	//all bones on this segment and its subsegments  whose transforms the solver is allowed to modify
	public ArrayList<WorkingBone> solvableSegementBones = new ArrayList<>();
	//all bones on this segment and its subsegments 
	public ArrayList<WorkingBone> allSegementBones = new ArrayList<>();
	public WorkingBone[] reversedTraversalArray; 

	public WorkingBone wb_segmentRoot;
	public SGVec_3d[] boneCenteredTargetHeadings;
	public SGVec_3d[] boneCenteredTipHeadings;
	public SGVec_3d[] uniform_boneCenteredTipHeadings;
	public double[] weights;
	public WorkingBone[] pinnedBones;
	boolean isRootPinned = false;
	boolean hasPinnedAncestor = false;
	public double previousDeviation = Double.POSITIVE_INFINITY;
	QCP qcpConverger = new QCP(MathUtils.DOUBLE_ROUNDING_ERROR, MathUtils.DOUBLE_ROUNDING_ERROR);
	public WorkingBone wb_segmentTip;
	public ArmatureSegment parentSegment;

	public ArmatureSegment(ShadowSkeleton shadowSkel, BoneState startingFrom, ArmatureSegment parentSegment, boolean isRootPined) {
		this.shadowSkel = shadowSkel;			
		this.simTransforms = shadowSkel.simTransforms;
		this.parentSegment = parentSegment;
		this.isRootPinned = isRootPined;
		this.hasPinnedAncestor = (parentSegment != null && (parentSegment.isRootPinned || parentSegment.hasPinnedAncestor));
		buildSegment(startingFrom);
		if(this.isRootPinned)
			this.wb_segmentRoot.setAsSegmentRoot();
		buildReverseTraversalArray();
		createHeadingArrays();
	}
	
	public double getDampening() {
		return shadowSkel.baseDampening;
	}
	
	public void buildSegment(BoneState startingFrom) {
		ArrayList<WorkingBone> segEffectors = new ArrayList<>();
		ArrayList<WorkingBone> strandBones = new ArrayList<>();
		ArrayList<ArmatureSegment> subSgmts = new ArrayList<>();
		ArrayList<ArmatureSegment> childSgmts = new ArrayList<>();
		BoneState currentBS = startingFrom;		
		boolean finished = false;
		while(finished ==false) {
			WorkingBone currentWB = new WorkingBone(currentBS, this);
			if(currentBS == startingFrom)
				this.wb_segmentRoot = currentWB;
			strandBones.add(currentWB);
			TargetState target = currentBS.getTarget();
			if(target != null || currentBS.getChildCount() > 1) {
				if(target != null) {
					segEffectors.add(currentWB);
					if(target.getDepthFallOff() <= 0.0) {						
						this.wb_segmentTip = currentWB;
						finished = true;
					}
				}				
				if(finished) 
					for(int i=0; i<currentBS.getChildCount(); i++) 
						childSgmts.add(new ArmatureSegment(shadowSkel, currentBS.getChild(i), this, true));
				else {
					for(int i=0; i<currentBS.getChildCount(); i++) { 
						ArmatureSegment subseg = new ArmatureSegment(shadowSkel, currentBS.getChild(i), this, false);
						subSgmts.add(subseg);
						subSgmts.addAll(subseg.subSegments);
						Collections.addAll(segEffectors, subseg.pinnedBones);
					}
					finished = true;
					this.wb_segmentTip = currentWB;
				}
			} else if (currentBS.getChildCount() == 1) {
				currentBS = currentBS.getChild(0);
			} else {
				this.wb_segmentTip = currentWB;
			}
		} 
		this.subSegments = subSgmts; 
		this.pinnedBones = segEffectors.toArray(new WorkingBone[0]); 
		this.childSegments = childSgmts;
		this.solvableStrandBones =  strandBones;
	}

	public void buildReverseTraversalArray() {
		ArrayList<WorkingBone> reverseTraversalArray = new ArrayList<>();
		for(WorkingBone wb: solvableStrandBones) 
			if(wb.forBone.getStiffness() < 1.0) 
				reverseTraversalArray.add(wb);

		for(ArmatureSegment ss : subSegments)
			Collections.addAll(reverseTraversalArray, ss.reversedTraversalArray);

		this.reversedTraversalArray = reverseTraversalArray.toArray(new WorkingBone[0]);
	}

	void createHeadingArrays() {
		ArrayList<ArrayList<Double>> penaltyArray = new ArrayList<ArrayList<Double>>();
		ArrayList<WorkingBone> pinSequence = new ArrayList<>(); //TODO: remove after debugging
		recursivelyCreatePenaltyArray(penaltyArray, pinSequence, 1d);
		int totalHeadings = 0;
		for (ArrayList<Double> a : penaltyArray) {
			totalHeadings += a.size();
		}
		boneCenteredTargetHeadings = new SGVec_3d[totalHeadings];
		boneCenteredTipHeadings = new SGVec_3d[totalHeadings];
		uniform_boneCenteredTipHeadings = new SGVec_3d[totalHeadings];
		weights = new double[totalHeadings];
		int currentHeading = 0;
		for (ArrayList<Double> a : penaltyArray) {
			for (Double ad : a) {
				weights[currentHeading] = ad;
				boneCenteredTargetHeadings[currentHeading] = new SGVec_3d();
				boneCenteredTipHeadings[currentHeading] = new SGVec_3d();
				uniform_boneCenteredTipHeadings[currentHeading] = new SGVec_3d();
				currentHeading++;
			}
		}
	}

	public void recursivelyCreatePenaltyArray(
			ArrayList<ArrayList<Double>> weightArray, 
			ArrayList<WorkingBone> pinSequence, 
			double currentFalloff) {
		if(currentFalloff == 0) {
			return;
		} else  {
			TargetState target = this.wb_segmentTip.targetState;
			if (target != null) {
				ArrayList<Double> innerWeightArray = new ArrayList<Double>();
				weightArray.add(innerWeightArray);
				byte modeCode = target.getModeCode();
				innerWeightArray.add(target.getWeight() * currentFalloff);
				double maxPinWeight = target.getMaxPriority();			
				if (maxPinWeight == 0d)
					maxPinWeight = 1d;

				if ((modeCode & TargetState.XDir) != 0) {
					double subTargetWeight = target.getWeight() * (target.getPriority(TargetState.XDir) / maxPinWeight) * currentFalloff;
					innerWeightArray.add(subTargetWeight);
					innerWeightArray.add(subTargetWeight);
				}
				if ((modeCode & TargetState.YDir) != 0) {
					double subTargetWeight = target.getWeight() * (target.getPriority(TargetState.YDir) / maxPinWeight) * currentFalloff;
					innerWeightArray.add(subTargetWeight);
					innerWeightArray.add(subTargetWeight);
				}
				if ((modeCode & TargetState.ZDir) != 0) {
					double subTargetWeight = target.getWeight() * (target.getPriority(TargetState.ZDir) / maxPinWeight) * currentFalloff;
					innerWeightArray.add(subTargetWeight);
					innerWeightArray.add(subTargetWeight);
				}
				pinSequence.add(wb_segmentTip);
			}
			double thisFalloff = target == null ? 1d : target.getDepthFallOff();
			for (ArmatureSegment s : this.subSegments) {
				s.recursivelyCreatePenaltyArray(weightArray, pinSequence, currentFalloff * thisFalloff);
			}
		}
	}

	/**
	 * @return a list containing this armatureSegment and all of its descendant armatureSegment. This list does NOT contain subsegments, 
	 * it only contains childSegments , and childSegments of childSegments and so on. In other words, it only returns descendant segments which bones
	 * on this segment do NOT attempt to satisfy the effectors of. 
	 * See the variablle definition comments for distinction between a childSegment and a subSegment.
	 */
	public ArrayList<ArmatureSegment> getDescendantSegments() {
		ArrayList<ArmatureSegment> result = new ArrayList<>();
		result.add(this);
		for(ArmatureSegment child : childSegments) {
			result.addAll(child.getDescendantSegments());
		}
		return result;
	}	

	public double getManualMSD(SGVec_3d[] locTips, SGVec_3d[] locTargets, double[] weights) {
		double manualRMSD = 0d;
		double wsum = 0d;
		for (int i = 0; i < locTargets.length; i++) {
			double xd = locTargets[i].x - locTips[i].x;
			double yd = locTargets[i].y - locTips[i].y;
			double zd = locTargets[i].z - locTips[i].z;
			double magsq = weights[i] * (xd * xd + yd * yd + zd * zd);
			manualRMSD += magsq;
			wsum += weights[i];
		}
		manualRMSD /= wsum * wsum;// (double) locTargets.length;
		// manualRMSD = MathUtils.sqrt(manualRMSD);
		return manualRMSD;
	}
	
	/**
	 * Holds working information for the given bone.
	 */
	class WorkingBone {
		BoneState forBone;
		ConstraintState cnstrntstate;
		Constraint constraint;
		TargetState targetState;
		AbstractAxes simTargetAxes;
		AbstractAxes simLocalAxes;
		AbstractAxes simConstraintSwingAxes;
		AbstractAxes simConstraintTwistAxes;
		ArmatureSegment onChain;
		double cosHalfDampen = 0d;
		double cosHalfReturnDamp = 0d;
		double returnDamp = 0d;
		boolean springy = false;
		/**set to true if this WorkingBone's chain is not a subsegment, and this bone is its root*/
		boolean isSegmentRoot = false;
		boolean hasPinnedAncestor = false;
		
		public WorkingBone(BoneState toSimulate, ArmatureSegment chain) {
			forBone = toSimulate;
			cnstrntstate = forBone.getConstraint();			
			simLocalAxes = simTransforms[forBone.getTransform().getIndex()];
			onChain = chain;
			if(forBone.getTarget() != null) {
				this.targetState = forBone.getTarget();
				this.simTargetAxes = simTransforms[targetState.getTransform().getIndex()];
			}
			this.hasPinnedAncestor = onChain.hasPinnedAncestor;
			double predamp = 1d - forBone.getStiffness();
			double defaultDampening = onChain.getDampening();
			double dampening = forBone.getParent() == null ? MathUtils.PI : predamp * defaultDampening;
			cosHalfDampen = Math.cos(dampening / 2d);
			if(cnstrntstate != null) {
				constraint = cnstrntstate.getDirectReference();
				simConstraintSwingAxes = simTransforms[cnstrntstate.getSwingTransform().getIndex()];
				simConstraintTwistAxes = cnstrntstate.getTwistTransform() == null ? null : simTransforms[cnstrntstate.getTwistTransform().getIndex()];
				if (cnstrntstate.getPainfulness() > 0d) {
					springy = true;
				} else {
					springy = false;
				}
			}
			this.updateCosDampening();
		}
		
		public void setAsSegmentRoot() {
			this.isSegmentRoot = true;
		}

		public void fastUpdateOptimalRotationToPinnedDescendants(int stabilizePasses, boolean translate) {
			simLocalAxes.updateGlobal();
			updateTargetHeadings(onChain.boneCenteredTargetHeadings, onChain.weights);			
			Rot prevOrientation = new Rot(simLocalAxes.getLocalMBasis().rotation.rotation);
			boolean gotCloser = true;
			for (int i = 0; i <= stabilizePasses; i++) {
				updateTipHeadings(onChain.boneCenteredTipHeadings, true);
				updateOptimalRotationToPinnedDescendants(translate, onChain.boneCenteredTipHeadings,
						onChain.boneCenteredTargetHeadings, weights);
				if (stabilizePasses > 0) {
					updateTipHeadings(onChain.uniform_boneCenteredTipHeadings, false);
					double currentmsd = onChain.getManualMSD(onChain.uniform_boneCenteredTipHeadings, onChain.boneCenteredTargetHeadings,
							onChain.weights);
					if (currentmsd <= onChain.previousDeviation * 1.000001d) {
						onChain.previousDeviation = currentmsd;
						gotCloser = true;
						break;
					} else gotCloser = false;
				}
			}
			if(!gotCloser)
				simLocalAxes.setLocalOrientationTo(prevOrientation);

			if (onChain.wb_segmentRoot == this) 
				onChain.previousDeviation = Double.POSITIVE_INFINITY;
			simLocalAxes.markDirty();
		}

		private Rot updateOptimalRotationToPinnedDescendants(boolean translate,
				SGVec_3d[] localizedTipHeadings, SGVec_3d[] localizedTargetHeadings, double[] weights) {

			Rot qcpRot = onChain.qcpConverger.weightedSuperpose(localizedTipHeadings, localizedTargetHeadings, weights,
					translate);

			SGVec_3d translateBy = onChain.qcpConverger.getTranslation();
			double boneDamp = cosHalfDampen;
			if(!translate)
				qcpRot.rotation.clampToQuadranceAngle(boneDamp);
			simLocalAxes.rotateBy(qcpRot);
			if(translate) {
				simLocalAxes.translateByGlobal(translateBy);
				
			}		
			simLocalAxes.updateGlobal();

			if(constraint != null) {
				constraint.setAxesToSnapped(simLocalAxes, simConstraintSwingAxes, simConstraintTwistAxes);
				/* we should never hit this condition but I know someone's going to try really hard to put constraints
				 * on root bones despite multiple warnings not to so.. 
				 */
				if(translate) { 
					simConstraintSwingAxes.translateByGlobal(translateBy);
					simConstraintTwistAxes.translateByGlobal(translateBy);
				}
			}
			return qcpRot;
		}

		public void pullBackTowardAllowableRegion() {
			if (springy && constraint != null && AbstractKusudama.class.isAssignableFrom(constraint.getClass())) {
				((AbstractKusudama)constraint).setAxesToReturnfulled(simLocalAxes, simConstraintSwingAxes, simConstraintTwistAxes, cosHalfReturnDamp, returnDamp);
				onChain.previousDeviation = Double.POSITIVE_INFINITY;
			}
		}

		public void updateTargetHeadings(Vec3d<?>[] localizedTargetHeadings, double[] weights) {
			int hdx = 0;
			for (int i = 0; i < onChain.pinnedBones.length; i++) {
				WorkingBone sb = onChain.pinnedBones[i];
				AbstractAxes targetAxes = sb.simTargetAxes;
				targetAxes.updateGlobal();
				Vec3d<?> origin = simLocalAxes.origin_();
				localizedTargetHeadings[hdx].set(targetAxes.origin_()).sub(origin);
				byte modeCode = sb.targetState.getModeCode();
				hdx++;
				if ((modeCode & TargetState.XDir) != 0) {
					sgRayd xTarget = targetAxes.x_().getRayScaledBy(weights[hdx]);
					localizedTargetHeadings[hdx].set(xTarget.p2()).sub(origin);
					xTarget.setToInvertedTip(localizedTargetHeadings[hdx + 1]).sub(origin);
					hdx += 2;
				}
				if ((modeCode & TargetState.YDir) != 0) {
					sgRayd yTarget = targetAxes.y_().getRayScaledBy(weights[hdx]);
					localizedTargetHeadings[hdx] = Vec3d.sub(yTarget.p2(), origin);
					yTarget.setToInvertedTip(localizedTargetHeadings[hdx + 1]).sub(origin);
					hdx += 2;
				}
				if ((modeCode & TargetState.ZDir) != 0) {
					sgRayd zTarget = targetAxes.z_().getRayScaledBy(weights[hdx]);
					localizedTargetHeadings[hdx] = Vec3d.sub(zTarget.p2(), origin);
					zTarget.setToInvertedTip(localizedTargetHeadings[hdx + 1]).sub(origin);
					hdx += 2;
				}
			}
		}

		/**
		 * 
		 * @param localizedTipHeadings
		 * @param scale                if false, will not scale the basis vectors by
		 *                             their distance from the bone origin
		 */
		public void updateTipHeadings(Vec3d<?>[] localizedTipHeadings, boolean scale) {
			int hdx = 0;

			for (int i = 0; i < onChain.pinnedBones.length; i++) {
				WorkingBone sb = onChain.pinnedBones[i];
				AbstractAxes tipAxes = sb.simLocalAxes;
				tipAxes.updateGlobal();
				Vec3d<?> origin = simLocalAxes.origin_();
				TargetState target = sb.targetState; 
				byte modeCode = target.getModeCode();

				AbstractAxes targetAxes = sb.simTargetAxes;
				targetAxes.updateGlobal();
				localizedTipHeadings[hdx].set(tipAxes.origin_()).sub(origin);
				double scaleBy = scale ? simLocalAxes.origin_().dist(targetAxes.origin_()) : 1d;
				hdx++;

				if ((modeCode & TargetState.XDir) != 0) {
					sgRayd xTip = tipAxes.x_().getRayScaledBy(scaleBy);
					localizedTipHeadings[hdx].set(xTip.p2()).sub(origin);
					xTip.setToInvertedTip(localizedTipHeadings[hdx + 1]).sub(origin);
					hdx += 2;
				}
				if ((modeCode & TargetState.YDir) != 0) {
					sgRayd yTip = tipAxes.y_().getRayScaledBy(scaleBy);
					localizedTipHeadings[hdx].set(yTip.p2()).sub(origin);
					yTip.setToInvertedTip(localizedTipHeadings[hdx + 1]).sub(origin);
					hdx += 2;
				}
				if ((modeCode & TargetState.ZDir) != 0) {
					sgRayd zTip = tipAxes.z_().getRayScaledBy(scaleBy);
					localizedTipHeadings[hdx].set(zTip.p2()).sub(origin);
					zTip.setToInvertedTip(localizedTipHeadings[hdx + 1]).sub(origin);
					;
					hdx += 2;
				}
			}
		}

		public void updateCosDampening() {
			double predamp = 1d - forBone.getStiffness();
			double defaultDampening = onChain.getDampening();
			double dampening = forBone.getParent() == null ? MathUtils.PI : predamp * defaultDampening;
			cosHalfDampen = Math.cos(dampening / 2d);
		}
		public void updateReturnfullnessDamp(double iterations) {
			if(cnstrntstate != null) {
				/**
				 * determine maximum pullback that would still allow the solver to converge if applied once per pass 
				 */
				if (cnstrntstate.getPainfulness() >= 0d) {
					double dampening = forBone.getParent() == null ? MathUtils.PI : (1d - forBone.getStiffness()) * onChain.getDampening();
					returnDamp = (dampening - (Math.PI / (2 * ((int) Math.ceil(Math.PI / (iterations * dampening)) * iterations))))*cnstrntstate.getPainfulness();
					cosHalfReturnDamp = Math.cos(returnDamp / 2d);
					springy = true;
					//populateReturnDampeningIterationArray(k);
				} else {
					springy = false;
				}
			}
		}
	}
}