package IK.doubleIK.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

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

public class ArmatureSegment {
	private ShadowSkeleton shadowSkel;
	public AbstractAxes[] simTransforms;
	public AbstractArmature forArmature;
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
	public double previousDeviation = Double.POSITIVE_INFINITY;
	QCP qcpConverger = new QCP(MathUtils.DOUBLE_ROUNDING_ERROR, MathUtils.DOUBLE_ROUNDING_ERROR);
	public WorkingBone wb_segmentTip;


	public ArmatureSegment(ShadowSkeleton shadowSkel, BoneState startingFrom, ArmatureSegment parentSegment, boolean isRootPined) {
		this.shadowSkel = shadowSkel;
		this.isRootPinned = isRootPined;
		this.simTransforms = shadowSkel.simTransforms;
		this.forArmature = shadowSkel.forArmature;
		buildSegment(startingFrom);
		buildReverseTraversalArray();
		for(ArmatureSegment cs : childSegments) {
			cs.createHeadingArrays();
		}
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
			strandBones.add(currentWB);
			TargetState target = currentBS.getTarget();
			if(target != null || startingFrom.getChildCount() > 1) {
				if(target != null) 
					segEffectors.add(currentWB);
				for(int i=0; i<startingFrom.getChildCount(); i++) {
					if(target.getDepthFallOff() <= 0.0) {
						childSgmts.add(new ArmatureSegment(shadowSkel, startingFrom.getChild(i), this, true));
						finished = true;
						this.wb_segmentTip = currentWB;
					}
					else {
						ArmatureSegment subseg = new ArmatureSegment(shadowSkel, startingFrom.getChild(i), this, false);
						subSegments.add(subseg);
						subSegments.addAll(subseg.subSegments);
						Collections.addAll(segEffectors, subseg.pinnedBones);
					}
				}
			} else if (startingFrom.getChildCount() == 1) {
				currentBS = startingFrom.getChild(0);
			}
		} 
		this.subSegments = subSgmts; 
		this.pinnedBones = segEffectors.toArray(this.pinnedBones); 
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

		this.reversedTraversalArray = reverseTraversalArray.toArray(this.reversedTraversalArray);
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
		} else {
			TargetState target = this.wb_segmentTip.targetState;
			if (target != null) {
				ArrayList<Double> innerWeightArray = new ArrayList<Double>();
				weightArray.add(innerWeightArray);
				byte modeCode = target.getModeCode();
				innerWeightArray.add(target.getWeight() * currentFalloff);
				double maxPinWeight = target.getMaxPriority();			
				if (maxPinWeight == 0d)
					maxPinWeight = 1d;

				if ((modeCode & AbstractIKPin.XDir) != 0) {
					double subTargetWeight = target.getWeight() * (target.getPriority(AbstractIKPin.XDir) / maxPinWeight) * currentFalloff;
					innerWeightArray.add(subTargetWeight);
					innerWeightArray.add(subTargetWeight);
				}
				if ((modeCode & AbstractIKPin.YDir) != 0) {
					double subTargetWeight = target.getWeight() * (target.getPriority(AbstractIKPin.YDir) / maxPinWeight) * currentFalloff;
					innerWeightArray.add(subTargetWeight);
					innerWeightArray.add(subTargetWeight);
				}
				if ((modeCode & AbstractIKPin.ZDir) != 0) {
					double subTargetWeight = target.getWeight() * (target.getPriority(AbstractIKPin.ZDir) / maxPinWeight) * currentFalloff;
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
	 * 
	 * @author Eron Gjoni
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
		double cosHalfReturnfullnessDampened_iterated[];
		double halfReturnfullnessDampened_iterated[];
		boolean springy = false;

		/**hypothetical QCP-based constraint logic
		 * 1. do a regular solve.
		 * 2. determine which tangent cones 
		 */

		double conePreferenceWeights[];
		/**
		 * EXPERIMENTAL: 
		 * all of these vectors point in the same direction (the one the bone is pointing in),
		 * each of them corresponds to a tangent cone which the bone should prefer to avoid.
		 */
		SGVec_3d boneTipVectors[];
		/**
		 * EXPERIMENTAL: 
		 * each of these correspond either to tangent cone the bone should prefer to avoid,
		 * or a limit cone the bone should prefer to be in the region of
		 * 
		 * order is: 
		 * controlpoint, tan1, tan2, ... controlpoint
		 */
		SGVec_3d conePreferenceVectors[]; 

		SGVec_3d limitConeLocalDirectionCache[];
		double cosineRadiiCache[];

		SGVec_3d tipHeadings_internalconcat[];
		SGVec_3d targetHeadings_internalconcat[];
		double weights_internalconcat[];

		public WorkingBone(BoneState toSimulate, ArmatureSegment chain) {
			forBone = toSimulate;
			cnstrntstate = forBone.getConstraint();			
			simLocalAxes = simTransforms[forBone.getTransform().getIndex()];
			onChain = chain;
			if(forBone.getTarget() != null) {
				this.targetState = forBone.getTarget();
				this.simTargetAxes = simTransforms[targetState.getTransform().getIndex()];
			}
			double predamp = 1d - forBone.getStiffness();
			double defaultDampening = forArmature.getDampening();
			double dampening = forBone.getParent() == null ? MathUtils.PI : predamp * defaultDampening;
			cosHalfDampen = Math.cos(dampening / 2d);
			if(cnstrntstate != null) {
				constraint = cnstrntstate.getDirectReference();
				simConstraintSwingAxes = simTransforms[cnstrntstate.getSwingTransform().getIndex()];
				simConstraintTwistAxes = simTransforms[cnstrntstate.getTwistTransform().getIndex()];
				AbstractKusudama k = ((AbstractKusudama) cnstrntstate.getDirectReference());
				if (k != null && k.getPainfulness() > 0d) {
					springy = true;
					populateReturnDampeningIterationArray(k);
				} else {
					springy = false;
				}
				if(k!= null) {
					ArrayList<? extends AbstractLimitCone> abl = k.getLimitCones();
					int coneCount = (abl.size()*3)-2;
					conePreferenceWeights = new double[coneCount];
					boneTipVectors = new SGVec_3d[coneCount];
					conePreferenceVectors = new SGVec_3d[coneCount];
					cosineRadiiCache = new double[coneCount];
					limitConeLocalDirectionCache = new SGVec_3d[coneCount];
					for (int i=0, j=0; i<abl.size(); i++, j+=3) {
						AbstractLimitCone lc = abl.get(i);
						boneTipVectors[j] = new SGVec_3d();
						conePreferenceVectors[j] = new SGVec_3d();
						limitConeLocalDirectionCache[j] = (SGVec_3d) lc.getControlPoint().multCopy(-1d);
						cosineRadiiCache[j] = abl.get(i).getRadiusCosine();
						if(lc.getTangentCircleCenterNext1(0) != null && j < coneCount-1) {
							boneTipVectors[j+1] = new SGVec_3d();
							boneTipVectors[j+2] = new SGVec_3d();
							conePreferenceVectors[j+1] = new SGVec_3d();
							conePreferenceVectors[j+2] = new SGVec_3d();
							limitConeLocalDirectionCache[j+1] = (SGVec_3d) lc.getTangentCircleCenterNext1(0).copy();
							cosineRadiiCache[j+1] = 1d-lc.getTangentCircleRadiusNextCos(0);
							limitConeLocalDirectionCache[j+2] = (SGVec_3d) lc.getTangentCircleCenterNext2(0).copy();
							cosineRadiiCache[j+2] = 1d-lc.getTangentCircleRadiusNextCos(0);				
						}
					}
				}
			}
		}

		public void updateConePreference_concatenatedArrays(Vec3d<?>[] localizedTipHeadings, Vec3d<?>[] localizedTargetHeadings, double[] weights) {
			simLocalAxes.updateGlobal();
			simConstraintSwingAxes.updateGlobal();
			SGVec_3d boneDir = (SGVec_3d) simLocalAxes.y_().heading().normalize();
			int i=0;
			for(; i<localizedTargetHeadings.length; i++) {
				weights_internalconcat[i] = weights[i];
				tipHeadings_internalconcat[i].set(localizedTipHeadings[i]);
				targetHeadings_internalconcat[i].set(localizedTargetHeadings[i]);
			}
			if(tipHeadings_internalconcat[tipHeadings_internalconcat.length-1] == null) {
				for(int j = i, cp=0; cp<conePreferenceVectors.length; j++, cp++) {
					tipHeadings_internalconcat[j] = new SGVec_3d();
					targetHeadings_internalconcat[j] = new SGVec_3d();
				}
			}
			for(int j=0; i<targetHeadings_internalconcat.length; i++, j++) {
				//boneTipVectors[i].set(boneDir);
				//conePreferenceVectors[i].set(simConstraintAxes.getGlobalOf(limitConeLocalDirectionCache[i]));
				tipHeadings_internalconcat[i].set(boneDir);
				targetHeadings_internalconcat[i].set(simConstraintSwingAxes.getGlobalOf(limitConeLocalDirectionCache[j]).sub(simConstraintSwingAxes.origin_()));
				weights_internalconcat[i] = 0d*cosineRadiiCache[j];
			}
		}

		public void fastUpdateOptimalRotationToPinnedDescendants(double dampening, boolean translate) {

			simLocalAxes.updateGlobal();

			double newDampening = -1;
			if (translate == true) {
				newDampening = MathUtils.PI2;
			}
			updateTargetHeadings(onChain.boneCenteredTargetHeadings, onChain.weights);			
			Rot prevOrientation = new Rot(simLocalAxes.getLocalMBasis().rotation.rotation);
			boolean gotCloser = true;
			for (int i = 0; i <= forArmature.defaultStabilizingPassCount; i++) {
				updateTipHeadings(onChain.boneCenteredTipHeadings, true);
				updateConePreference_concatenatedArrays(onChain.boneCenteredTipHeadings, onChain.boneCenteredTargetHeadings, onChain.weights);
				updateOptimalRotationToPinnedDescendants(newDampening, translate, tipHeadings_internalconcat, targetHeadings_internalconcat, weights_internalconcat);
				/*updateOptimalRotationToPinnedDescendants(newDampening, translate, onChain.boneCenteredTipHeadings,
						onChain.boneCenteredTargetHeadings, weights);*/
				if (forArmature.defaultStabilizingPassCount > 0) {
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

		private void updateOptimalRotationToPinnedDescendants(double dampening, boolean translate,
				SGVec_3d[] localizedTipHeadings, SGVec_3d[] localizedTargetHeadings, double[] weights) {

			Rot qcpRot = onChain.qcpConverger.weightedSuperpose(localizedTipHeadings, localizedTargetHeadings, weights,
					translate);


			SGVec_3d translateBy = onChain.qcpConverger.getTranslation();
			double boneDamp = cosHalfDampen;
			// sb.forBone.getIKPin().modeCode = 6;
			if (dampening != -1) {
				boneDamp = dampening;
				qcpRot.rotation.clampToAngle(boneDamp);
			} else {
				qcpRot.rotation.clampToQuadranceAngle(boneDamp);
			}
			simLocalAxes.rotateBy(qcpRot);
			simLocalAxes.updateGlobal();

			if(constraint != null) {
				constraint.setAxesToSnapped(simLocalAxes, simConstraintSwingAxes, simConstraintTwistAxes);
				if(translate) {
					simLocalAxes.translateByGlobal(translateBy);
					simConstraintSwingAxes.translateByGlobal(translateBy);
					simConstraintTwistAxes.translateByGlobal(translateBy);
				}
			}
		}

		/*public void pullBackTowardAllowableRegion(int iteration, int totalIterations) {
			if (springy) {
				double coshalfDamp;
				double halfDamp;
				if (totalIterations != forArmature.getDefaultIterations()) {
					halfDamp = computeIterateReturnfulness((double) iteration, (double) totalIterations,
							forBone.getConstraint());
					coshalfDamp = Math.cos(halfDamp / 2d);
				} else {
					halfDamp = halfReturnfullnessDampened_iterated[iteration];
					coshalfDamp = cosHalfReturnfullnessDampened_iterated[iteration];
				}
				forBone.setAxesToReturnfulled(simLocalAxes, simConstraintAxes, coshalfDamp, halfDamp);
				onChain.previousDeviation = Double.POSITIVE_INFINITY;
			}
		}*/

		public void updateTargetHeadings(Vec3d<?>[] localizedTargetHeadings, double[] weights) {
			int hdx = 0;
			if(targetHeadings_internalconcat == null || targetHeadings_internalconcat.length != localizedTargetHeadings.length) {
				int pad = boneTipVectors == null ? 0 : boneTipVectors.length;
				targetHeadings_internalconcat = new SGVec_3d[localizedTargetHeadings.length + pad];
				tipHeadings_internalconcat = new SGVec_3d[localizedTargetHeadings.length + pad];
				weights_internalconcat = new double[weights.length + pad];
				for (int i = 0; i < localizedTargetHeadings.length; i++) {
					targetHeadings_internalconcat[i] = new SGVec_3d();
					tipHeadings_internalconcat[i] = new SGVec_3d();
				}
			}
			for (int i = 0; i < onChain.pinnedBones.length; i++) {
				WorkingBone sb = onChain.pinnedBones[i];
				AbstractAxes targetAxes = sb.simTargetAxes;
				targetAxes.updateGlobal();
				Vec3d<?> origin = simLocalAxes.origin_();
				localizedTargetHeadings[hdx].set(targetAxes.origin_()).sub(origin);
				byte modeCode = sb.targetState.getModeCode();
				hdx++;

				if ((modeCode & AbstractIKPin.XDir) != 0) {
					sgRayd xTarget = targetAxes.x_().getRayScaledBy(weights[hdx]);
					localizedTargetHeadings[hdx].set(xTarget.p2()).sub(origin);
					xTarget.setToInvertedTip(localizedTargetHeadings[hdx + 1]).sub(origin);
					hdx += 2;
				}
				if ((modeCode & AbstractIKPin.YDir) != 0) {
					sgRayd yTarget = targetAxes.y_().getRayScaledBy(weights[hdx]);
					localizedTargetHeadings[hdx] = Vec3d.sub(yTarget.p2(), origin);
					yTarget.setToInvertedTip(localizedTargetHeadings[hdx + 1]).sub(origin);
					hdx += 2;
				}
				if ((modeCode & AbstractIKPin.ZDir) != 0) {
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

				if ((modeCode & AbstractIKPin.XDir) != 0) {
					sgRayd xTip = tipAxes.x_().getRayScaledBy(scaleBy);
					localizedTipHeadings[hdx].set(xTip.p2()).sub(origin);
					xTip.setToInvertedTip(localizedTipHeadings[hdx + 1]).sub(origin);
					hdx += 2;
				}
				if ((modeCode & AbstractIKPin.YDir) != 0) {
					sgRayd yTip = tipAxes.y_().getRayScaledBy(scaleBy);
					localizedTipHeadings[hdx].set(yTip.p2()).sub(origin);
					yTip.setToInvertedTip(localizedTipHeadings[hdx + 1]).sub(origin);
					hdx += 2;
				}
				if ((modeCode & AbstractIKPin.ZDir) != 0) {
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
			double defaultDampening = forArmature.getDampening();
			double dampening = forBone.getParent() == null ? MathUtils.PI : predamp * defaultDampening;
			cosHalfDampen = Math.cos(dampening / 2d);
			if(constraint != null) {
				AbstractKusudama k = ((AbstractKusudama) constraint);
				if (k.getPainfulness() >= 0d) {
					returnDamp = Math.max(defaultDampening / 2d, k.getPainfulness() / 2d);
					cosHalfReturnDamp = Math.cos(returnDamp / 2d);
					springy = true;
					populateReturnDampeningIterationArray(k);
				} else {
					springy = false;
				}
			}
		}

		public void populateReturnDampeningIterationArray(AbstractKusudama k) {
			double iterations = forArmature.getDefaultIterations();
			halfReturnfullnessDampened_iterated = new double[(int) iterations];
			cosHalfReturnfullnessDampened_iterated = new double[(int) iterations];
			for (double i = 0; i < iterations; i++) {
				int a = 0;
				double iterationsClamp = computeIterateReturnfulness(i, forArmature.getDefaultIterations(),
						k);
				double cosIterationReturnClamp = Math.cos(iterationsClamp / 2d);
				halfReturnfullnessDampened_iterated[(int) i] = iterationsClamp;
				cosHalfReturnfullnessDampened_iterated[(int) i] = cosIterationReturnClamp;
			}
		}

		public double computeIterateReturnfulness(double iteration, double totalIteration, Constraint k) {
			double predamp = 1d - forBone.getStiffness();
			double defaultDampening = forArmature.getDampening();
			double dampening = forBone.getParent() == null ? MathUtils.PI : predamp * defaultDampening;
			double returnfullness = k.getPainfulness();
			double falloff = 0.2;
			double totalItr = totalIteration;
			double iterationspow = 1d + Math.pow(totalItr, falloff * totalItr * returnfullness);
			double iterationScalar = ((iterationspow) - Math.pow(iteration, falloff * totalItr * returnfullness))
					/ (iterationspow);
			double iterationReturnClamp = iterationScalar * returnfullness * dampening;
			return iterationReturnClamp;
		}
	}
}