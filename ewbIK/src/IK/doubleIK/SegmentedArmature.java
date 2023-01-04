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
package IK.doubleIK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

import IK.PerfTimer;
import math.doubleV.AbstractAxes;
import math.doubleV.MathUtils;
import math.doubleV.QCP;
import math.doubleV.Rot;
import math.doubleV.SGVec_3d;
import math.doubleV.Vec3d;
import math.doubleV.sgRayd;
import asj.LoadManager;
import asj.SaveManager;
import asj.Saveable;
import asj.data.JSONObject;

/**
 * @author Eron Gjoni
 *
 */
public class SegmentedArmature {
	public AbstractBone segmentRoot;
	public AbstractBone segmentTip;
	public WorkingBone wb_segmentRoot;

	// private int subTargetCounts = 0;
	public ArrayList<SegmentedArmature> childSegments = new ArrayList<SegmentedArmature>();
	public ArrayList<SegmentedArmature> pinnedDescendants = new ArrayList<SegmentedArmature>();
	WorkingBone[] pinnedBones;

	HashMap<AbstractBone, WorkingBone> simulatedBones = new HashMap<>();
	ArrayList<AbstractBone> segmentBoneList = new ArrayList<AbstractBone>();
	QCP qcpConverger = new QCP(MathUtils.DOUBLE_ROUNDING_ERROR, MathUtils.DOUBLE_ROUNDING_ERROR);
	private SegmentedArmature parentSegment = null;
	private boolean basePinned = false;
	private boolean tipPinned = false;
	private boolean processed = false;
	public int distanceToRoot = 0;

	public int chainLength = 0;
	boolean includeInIK = true;
	int pinDepth = 1;

	public AbstractAxes debugTipAxes;
	public AbstractAxes debugTargetAxes;

	SGVec_3d[] boneCenteredTargetHeadings;
	SGVec_3d[] boneCenteredTipHeadings;
	SGVec_3d[] uniform_boneCenteredTipHeadings;
	double[] weights;

	public SegmentedArmature(AbstractBone rootBone) {
		segmentRoot = armatureRootBone(rootBone);
		generateArmatureSegments();
		ensureAxesHeirarchy();
	}

	public SegmentedArmature(SegmentedArmature inputParentSegment, AbstractBone inputSegmentRoot) {
		this.segmentRoot = inputSegmentRoot;
		this.setParentSegment(inputParentSegment);
		this.distanceToRoot = this.getParentSegment().distanceToRoot + 1;
		generateArmatureSegments();
	}

	private void generateArmatureSegments() {
		childSegments.clear();
		// pinnedDescendants.clear();
		setTipPinned(false);
		if (segmentRoot.getParent() != null && segmentRoot.getParent().isPinned())
			this.setBasePinned(true);
		else
			this.setBasePinned(false);

		AbstractBone tempSegmentTip = this.segmentRoot;
		this.chainLength = -1;
		while (true) {
			this.chainLength++;
			ArrayList<AbstractBone> childrenWithPinnedDescendants = tempSegmentTip
					.returnChildrenWithPinnedDescendants();

			if (childrenWithPinnedDescendants.size() > 1 || (tempSegmentTip.isPinned())) {
				if (tempSegmentTip.isPinned())
					setTipPinned(true);
				// else tipPinned = false;
				this.segmentTip = tempSegmentTip;

				for (AbstractBone childBone : childrenWithPinnedDescendants) {
					this.childSegments.add(new SegmentedArmature(this, childBone));
				}

				break;
			} else if (childrenWithPinnedDescendants.size() == 1) {
				tempSegmentTip = childrenWithPinnedDescendants.get(0);
			} else {
				this.segmentTip = tempSegmentTip;
				break;
			}
		}
		updatePinnedDescendants();
		generateSegmentMaps();
		this.wb_segmentRoot = simulatedBones.get(this.segmentRoot);
	}

	/**
	 * calculates the total number of bases the immediate effectors emanating from
	 * this segment reach for (based on modecode set in the IKPin)
	 */
	/*
	 * private void updateTotalSubTargets() { subTargetCounts = 0; for(int i=0; i<
	 * pinnedDescendants.size(); i++) { SegmentedArmature s =
	 * pinnedDescendants.get(i); AbstractIKPin pin = s.segmentTip.getIKPin(); int
	 * pinTargets = pin.getSubtargetCount(); subTargetCounts += pinTargets; } }
	 */

	static void recursivelyCreateHeadingArraysFor(SegmentedArmature s) {
		s.createHeadingArrays();
		for (SegmentedArmature c : s.childSegments) {
			recursivelyCreateHeadingArraysFor(c);
		}
	}

	void createHeadingArrays() {
		ArrayList<ArrayList<Double>> penaltyArray = new ArrayList<ArrayList<Double>>();
		ArrayList<WorkingBone> pinSequence = new ArrayList<>();
		recursivelyCreatePenaltyArray(this, penaltyArray, pinSequence, 1d);
		pinnedBones = new WorkingBone[pinSequence.size()];
		int totalHeadings = 0;
		for (ArrayList<Double> a : penaltyArray) {
			totalHeadings += a.size();
		}
		for (int i = 0; i < pinSequence.size(); i++) {
			pinnedBones[i] = pinSequence.get(i);
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

	void recursivelyCreatePenaltyArray(SegmentedArmature from, 
			ArrayList<ArrayList<Double>> weightArray, 
			ArrayList<WorkingBone> pinSequence, 
			double currentFalloff) {
		if(currentFalloff == 0) {
			return;
		} else {
			AbstractIKPin pin = from.segmentTip.getIKPin();
			if (pin != null) {
				ArrayList<Double> innerWeightArray = new ArrayList<Double>();
				weightArray.add(innerWeightArray);
				byte modeCode = pin.getModeCode();
				innerWeightArray.add(pin.getPinWeight() * currentFalloff);
				double maxPinWeight = 0d;
				if ((modeCode & AbstractIKPin.XDir) != 0)
					maxPinWeight = MathUtils.max(maxPinWeight, pin.getXPriority());
				if ((modeCode & AbstractIKPin.YDir) != 0)
					maxPinWeight = MathUtils.max(maxPinWeight, pin.getYPriority());
				if ((modeCode & AbstractIKPin.ZDir) != 0)
					maxPinWeight = MathUtils.max(maxPinWeight, pin.getZPriority());

				if (maxPinWeight == 0d)
					maxPinWeight = 1d;

				maxPinWeight = 1d;

				if ((modeCode & AbstractIKPin.XDir) != 0) {
					double subTargetWeight = pin.getPinWeight() * (pin.getXPriority() / maxPinWeight) * currentFalloff;
					innerWeightArray.add(subTargetWeight);
					innerWeightArray.add(subTargetWeight);
				}
				if ((modeCode & AbstractIKPin.YDir) != 0) {
					double subTargetWeight = pin.getPinWeight() * (pin.getYPriority() / maxPinWeight) * currentFalloff;
					innerWeightArray.add(subTargetWeight);
					innerWeightArray.add(subTargetWeight);
				}
				if ((modeCode & AbstractIKPin.ZDir) != 0) {
					double subTargetWeight = pin.getPinWeight() * (pin.getZPriority() / maxPinWeight) * currentFalloff;
					innerWeightArray.add(subTargetWeight);
					innerWeightArray.add(subTargetWeight);
				}
				pinSequence.add(
						pin.forBone().parentArmature.boneSegmentMap.get(
								pin.forBone()
						).simulatedBones.get(pin.forBone())); 
			}
			double thisFalloff = pin == null ? 1d : pin.getDepthFalloff();
			for (SegmentedArmature s : from.childSegments) {
				recursivelyCreatePenaltyArray(s, weightArray, pinSequence, currentFalloff * thisFalloff);
			}

		}
	}

	/**
	 * Should only be called from the rootmost strand. ensures the proper axes
	 * parent relationships for simulatedAxes throughout the SegmentedArmature .
	 */
	private void ensureAxesHeirarchy() {
		SegmentedArmature rootStrand = this;
		while (rootStrand.parentSegment != null) {
			rootStrand = rootStrand.parentSegment;
		}
		recursivelyEnsureAxesHeirarchyFor(rootStrand.segmentRoot, rootStrand.segmentRoot.parentArmature.localAxes());
	}

	private void recursivelyEnsureAxesHeirarchyFor(AbstractBone b, AbstractAxes parentTo) {
		SegmentedArmature chain = getChainFor(b);
		if (chain != null) {
			WorkingBone sb = chain.simulatedBones.get(b);
			sb.simLocalAxes.setParent(parentTo);
			sb.simConstraintAxes.setParent(parentTo);
			for (AbstractBone c : b.getChildren()) {
				chain.recursivelyEnsureAxesHeirarchyFor(c, sb.simLocalAxes);
			}
		}
	}

	public void updateSegmentedArmature() {
		if (this.getParentSegment() != null) {
			this.getParentSegment().updateSegmentedArmature();
		} else {
			generateArmatureSegments();
			ensureAxesHeirarchy();
		}
	}

	public void generateSegmentMaps() {
		for (WorkingBone b : simulatedBones.values()) {
			b.simConstraintAxes.emancipate();
			b.simLocalAxes.emancipate();
		}
		simulatedBones.clear();
		segmentBoneList.clear();

		AbstractBone currentBone = segmentTip;
		AbstractBone stopOn = segmentRoot;
		while (currentBone != null) {
			WorkingBone sb = simulatedBones.get(currentBone);
			if (sb == null) {
				simulatedBones.put(currentBone, new WorkingBone(currentBone, this));
				segmentBoneList.add(0, currentBone);
			}

			if (currentBone == stopOn)
				break;
			currentBone = currentBone.getParent();

		}

		// strandsBoneList.addAll(boneRotationMap.keySet());
	}

	public ArrayList<AbstractBone> getStrandFromTip(AbstractBone pinnedBone) {
		ArrayList<AbstractBone> result = new ArrayList<AbstractBone>();

		if (pinnedBone.isPinned()) {
			result.add(pinnedBone);
			AbstractBone currBone = pinnedBone.getParent();
			// note to self -- try removing the currbone.parent != null condition
			while (currBone != null && currBone.getParent() != null) {
				result.add(currBone);
				if (currBone.getParent().isPinned()) {
					break;
				}
				currBone = currBone.getParent();
			}
		}

		return result;
	}

	public void updatePinnedDescendants() {
		pinnedDescendants.clear();
		pinnedDescendants = this.returnSegmentPinnedNodes();
	}

	public ArrayList<SegmentedArmature> returnSegmentPinnedNodes() {
		ArrayList<SegmentedArmature> innerPinnedChains = new ArrayList<>();
		if (this.isTipPinned()) {
			innerPinnedChains.add(this);
		} else {
			for (SegmentedArmature childSegment : childSegments) {
				innerPinnedChains.addAll(childSegment.returnSegmentPinnedNodes());
			}
		}
		return innerPinnedChains;
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
	 * 
	 * @param forBone
	 * @param dampening
	 * @param translate           set to true if you wish to allow translation in
	 *                            addition to rotation of the bone (should only be
	 *                            used for unpinned root bones)
	 * @param stabilizationPasses If you know that your armature isn't likely to
	 *                            succumb to instability in unsolvable
	 *                            configurations, leave this value set to 0. If you
	 *                            value stability in extreme situations more than
	 *                            computational speed, then increase this value. A
	 *                            value of 1 will be completely stable, and just as
	 *                            fast as a value of 0, however, it might result in
	 *                            small levels of robotic looking jerk. The higher
	 *                            the value, the less jerk there will be (but at
	 *                            potentially significant computation cost).
	 */
	public void updateOptimalRotationToPinnedDescendants(AbstractBone forBone, double dampening, boolean translate,
			int stabilizationPasses, int iteration, double totalIterations) {

		if (forBone.getParent() == null || boneCenteredTargetHeadings.length == 1)
			stabilizationPasses = 0;

		if (stabilizationPasses > 0)
			stableUpdateOptimalRotationToPinnedDescendants(forBone, dampening, translate, stabilizationPasses,
					iteration, totalIterations);
		else
			fastUpdateOptimalRotationToPinnedDescendants(forBone, dampening, translate, iteration, totalIterations);

	}

	private void fastUpdateOptimalRotationToPinnedDescendants(AbstractBone forBone, double dampening, boolean translate,
			int iteration, double totalIterations) {

		WorkingBone sb = simulatedBones.get(forBone);
		AbstractAxes thisBoneAxes = sb.simLocalAxes;
		thisBoneAxes.updateGlobal();

		double newDampening = -1;
		if (translate == true) {
			newDampening = Math.PI;
		}
		updateTargetHeadings(boneCenteredTargetHeadings, weights, thisBoneAxes);
		upateTipHeadings(boneCenteredTipHeadings, thisBoneAxes);
		QCP qcpConvergenceCheck = new QCP(MathUtils.DOUBLE_ROUNDING_ERROR, MathUtils.DOUBLE_ROUNDING_ERROR);
		updateOptimalRotationToPinnedDescendants(sb, newDampening, translate, boneCenteredTipHeadings,
				boneCenteredTargetHeadings, weights, qcpConvergenceCheck, iteration, totalIterations);
		thisBoneAxes.markDirty();
	}

	/**
	 * partially returns the bone back toward the center / straight skeleton of its
	 * allowable region does so no more than by some fraction of its most recent
	 * rotation
	 * 
	 * @param sb
	 */
	private void returnToCenter(WorkingBone sb) {

	}

	private void updateOptimalRotationToPinnedDescendants(WorkingBone sb, double dampening, boolean translate,
			SGVec_3d[] localizedTipHeadings, SGVec_3d[] localizedTargetHeadings, double[] weights,
			QCP qcpOrientationAligner, int iteration, double totalIterations) {

		qcpOrientationAligner.setMaxIterations(5);
		Rot qcpRot = qcpOrientationAligner.weightedSuperpose(localizedTipHeadings, localizedTargetHeadings, weights,
				translate);

		SGVec_3d translateBy = qcpOrientationAligner.getTranslation();
		double boneDamp = sb.cosHalfDampen;
		// sb.forBone.getIKPin().modeCode = 6;
		if (dampening != -1) {
			boneDamp = dampening;
			qcpRot.rotation.clampToAngle(boneDamp);
		} else {
			qcpRot.rotation.clampToQuadranceAngle(boneDamp);
		}
		sb.simLocalAxes.rotateBy(qcpRot);

		sb.simLocalAxes.updateGlobal();

		sb.forBone.setAxesToSnapped(sb.simLocalAxes, sb.simConstraintAxes);
		sb.simLocalAxes.translateByGlobal(translateBy);
		sb.simConstraintAxes.translateByGlobal(translateBy);
	}

	private void stableUpdateOptimalRotationToPinnedDescendants(AbstractBone forBone, double dampening,
			boolean translate, int stabilizationPasses, int iteration, double totalIterations) {

		WorkingBone sb = simulatedBones.get(forBone);
		AbstractAxes thisBoneAxes = sb.simLocalAxes;
		thisBoneAxes.updateGlobal();

		Rot bestOrientation = new Rot(thisBoneAxes.getGlobalMBasis().rotation.rotation);
		double newDampening = -1;
		if (translate == true) {
			newDampening = Math.PI;
		}
		updateTargetHeadings(boneCenteredTargetHeadings, weights, thisBoneAxes);
		upateTipHeadings(boneCenteredTipHeadings, thisBoneAxes);

		double bestRMSD = 0d;
		QCP qcpConvergenceCheck = new QCP(MathUtils.DOUBLE_ROUNDING_ERROR, MathUtils.DOUBLE_ROUNDING_ERROR);
		double newRMSD = 999999d;
		bestRMSD = getManualMSD(boneCenteredTipHeadings, boneCenteredTargetHeadings, weights);

		for (int i = 0; i < stabilizationPasses + 1; i++) {
			updateOptimalRotationToPinnedDescendants(sb, newDampening, translate, boneCenteredTipHeadings,
					boneCenteredTargetHeadings, weights, qcpConvergenceCheck, iteration, totalIterations);

			upateTipHeadings(boneCenteredTipHeadings, thisBoneAxes);
			newRMSD = getManualMSD(boneCenteredTipHeadings, boneCenteredTargetHeadings, weights);

			if (bestRMSD >= newRMSD) {
				if (sb.springy) {
					if (dampening != -1 || totalIterations != sb.forBone.parentArmature.getDefaultIterations()) {
						double returnfullness = ((AbstractKusudama) sb.forBone.getConstraint()).getPainfulness();
						double dampenedAngle = sb.forBone.getStiffness() * dampening * returnfullness;
						double totaliterationssq = totalIterations * totalIterations;
						double scaledDampenedAngle = dampenedAngle
								* ((totaliterationssq - (iteration * iteration)) / totaliterationssq);
						double cosHalfAngle = Math.cos(0.5 * scaledDampenedAngle);
						sb.forBone.setAxesToReturnfulled(sb.simLocalAxes, sb.simConstraintAxes, cosHalfAngle,
								scaledDampenedAngle);
					} else {
						sb.forBone.setAxesToReturnfulled(sb.simLocalAxes, sb.simConstraintAxes,
								sb.cosHalfReturnfullnessDampened_iterated[iteration],
								sb.halfReturnfullnessDampened_iterated[iteration]);
					}
					upateTipHeadings(boneCenteredTipHeadings, thisBoneAxes);
					newRMSD = getManualMSD(boneCenteredTipHeadings, boneCenteredTargetHeadings, weights);
				}
				bestOrientation.set(thisBoneAxes.getGlobalMBasis().rotation.rotation);
				bestRMSD = newRMSD;
				break;
			}
		}
		thisBoneAxes.setGlobalOrientationTo(bestOrientation);
		thisBoneAxes.markDirty();
	}

	// AbstractAxes tempAxes = null;

	public void updateTargetHeadings(Vec3d<?>[] localizedTargetHeadings, double[] weights, AbstractAxes thisBoneAxes) {

		int hdx = 0;
		for (int i = 0; i < pinnedBones.length; i++) {
			WorkingBone sb = pinnedBones[i];
			AbstractIKPin pin = sb.forBone.getIKPin();
			AbstractAxes targetAxes = pin.forBone.getPinnedAxes();
			targetAxes.updateGlobal();
			Vec3d<?> origin = thisBoneAxes.origin_();
			localizedTargetHeadings[hdx].set(targetAxes.origin_()).sub(origin);
			byte modeCode = pin.getModeCode();
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

	public void upateTipHeadings(Vec3d<?>[] localizedTipHeadings, AbstractAxes thisBoneAxes) {
		int hdx = 0;

		for (int i = 0; i < pinnedBones.length; i++) {
			WorkingBone sb = pinnedBones[i];
			AbstractIKPin pin = sb.forBone.getIKPin();
			AbstractAxes tipAxes = sb.simLocalAxes;
			tipAxes.updateGlobal();
			Vec3d<?> origin = thisBoneAxes.origin_();
			byte modeCode = pin.getModeCode();

			AbstractAxes targetAxes = pin.forBone.getPinnedAxes();
			targetAxes.updateGlobal();
			localizedTipHeadings[hdx].set(tipAxes.origin_()).sub(origin);
			double scaleBy = Math.max(1d, thisBoneAxes.origin_().dist(targetAxes.origin_()));
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

	/**
	 * 
	 * @param chainMember
	 * @return returns the segment chain (pinned or unpinned, doesn't matter) to
	 *         which the inputBone belongs.
	 */
	public SegmentedArmature getChainFor(AbstractBone chainMember) {
		// AbstractBone candidate = this.segmentTip;
		SegmentedArmature result = null;
		if (this.segmentBoneList.contains(chainMember))
			return this;
		if (this.parentSegment != null)
			result = this.parentSegment.getAncestorSegmentContaining(chainMember);
		if (result == null)
			result = getChildSegmentContaining(chainMember);
		return result;
	}

	public SegmentedArmature getChildSegmentContaining(AbstractBone b) {
		if (segmentBoneList.contains(b)) {
			return this;
		} else {
			for (SegmentedArmature s : childSegments) {
				SegmentedArmature childContaining = s.getChildSegmentContaining(b);
				if (childContaining != null)
					return childContaining;
			}
		}
		return null;
	}

	public SegmentedArmature getAncestorSegmentContaining(AbstractBone b) {
		if (segmentBoneList.contains(b))
			return this;
		else if (this.parentSegment != null)
			return this.parentSegment.getAncestorSegmentContaining(b);
		else
			return null;
	}

	/**
	 * this function travels rootward through the chain hierarchy until it reaches a
	 * chain whose base is pinned.
	 * 
	 * @return returns the first chain encountered with a pinned base. Or, null if
	 *         it reaches an unpinned armature root.
	 */
	public SegmentedArmature getPinnedRootChainFromHere() {

		SegmentedArmature currentChain = this;
		while (true && currentChain != null) {
			if (currentChain.isBasePinned())
				return currentChain;
			else
				currentChain = currentChain.getParentSegment();
		}

		return currentChain;

	}

	public AbstractBone armatureRootBone(AbstractBone rootBone2) {
		AbstractBone rootBone = rootBone2;
		while (rootBone.getParent() != null) {
			rootBone = rootBone.getParent();
		}
		return rootBone;
	}

	public boolean isTipPinned() {
		return tipPinned;
	}

	public void setTipPinned(boolean tipPinned) {
		this.tipPinned = tipPinned;
	}

	public boolean isBasePinned() {
		return basePinned;
	}

	public void setBasePinned(boolean basePinned) {
		this.basePinned = basePinned;
	}

	public SegmentedArmature getParentSegment() {
		return parentSegment;
	}

	public void setParentSegment(SegmentedArmature parentSegment) {
		this.parentSegment = parentSegment;
	}

	/**
	 * aligns all simulation axes from this root of this chain up until the pinned
	 * tips of any child chains with the constraint an local axes of their
	 * corresponding bone.
	 */

	public void alignSimulationAxesToBones() {
		if (!this.isBasePinned() && this.getParentSegment() != null) {
			this.getParentSegment().alignSimulationAxesToBones();
		} else {
			recursivelyAlignSimAxesOutwardFrom(segmentRoot, true);
		}
	}

	public void recursivelyAlignSimAxesOutwardFrom(AbstractBone b, boolean forceGlobal) {
		SegmentedArmature bChain = getChildSegmentContaining(b);
		if (bChain != null) {
			WorkingBone sb = bChain.simulatedBones.get(b);
			AbstractAxes bAxes = sb.simLocalAxes;
			AbstractAxes cAxes = sb.simConstraintAxes;
			if (forceGlobal) {
				bAxes.alignGlobalsTo(b.localAxes());
				bAxes.markDirty();
				bAxes.updateGlobal();
				cAxes.alignGlobalsTo(b.getMajorRotationAxes());
				cAxes.markDirty();
				cAxes.updateGlobal();
			} else {
				bAxes.alignLocalsTo(b.localAxes());
				cAxes.alignLocalsTo(b.getMajorRotationAxes());
			}
			for (AbstractBone bc : b.getChildren()) {
				bChain.recursivelyAlignSimAxesOutwardFrom(bc, false);
			}
		}
	}

	public void recursivelyAlignSimAxesRootwardFrom(AbstractBone b) {
		if (b != null) {
			SegmentedArmature bChain = b.parentArmature.boneSegmentMap.get(b); // getChainFor(b);
			AbstractBone parent = b.getParent();
			WorkingBone sb = bChain.simulatedBones.get(b);
			AbstractAxes bAxes = sb.simLocalAxes;
			AbstractAxes cAxes = sb.simConstraintAxes;
			bChain.simAligned = true;
			bAxes.alignGlobalsTo(b.localAxes());
			bAxes.markDirty();
			bAxes.updateGlobal();
			cAxes.alignGlobalsTo(b.getMajorRotationAxes());
			cAxes.markDirty();
			cAxes.updateGlobal();
			if (parent != null) {
				SegmentedArmature bParentChain = b.parentArmature.boneSegmentMap.get(parent);// getChainFor(parent);
				if (bParentChain != bChain && bParentChain.simAligned) {
					return; // the parent chain doesn't need aligning, it is safe to just update these
							// simAxes
				}
				recursivelyAlignSimAxesRootwardFrom(parent);
			}
			if (bAxes == null) {
				int debug = 0;
			}
			if (Double.isNaN(bAxes.globalMBasis.rotation.getAngle())) {
				int debug = 0;
			}
		}
	}

	/**
	 * aligns this bone and all relevant childBones to their coresponding
	 * simulatedAxes (if any) in the SegmentedArmature
	 * 
	 * @param b bone to start from
	 */
	public void recursivelyAlignBonesToSimAxesFrom(AbstractBone b) {
		SegmentedArmature chain = b.parentArmature.boneSegmentMap.get(b); // getChainFor(b);
		if (chain != null) {
			WorkingBone sb = chain.simulatedBones.get(b);
			AbstractAxes simulatedLocalAxes = sb.simLocalAxes;
			if (b.parent != null) {
				b.localAxes().localMBasis.rotateTo(simulatedLocalAxes.localMBasis.rotation);
				b.localAxes().markDirty();
				b.localAxes().updateGlobal();
			} else {
				b.localAxes().alignLocalsTo(simulatedLocalAxes);
			}
			/*
			 * if(b.getParent() == null) { b.localAxes().alignGlobalsTo(simulatedLocalAxes);
			 * } else {
			 * b.localAxes().localMBasis.rotateTo(simulatedLocalAxes.localMBasis.rotation);
			 * b.localAxes().markDirty(); b.localAxes().updateGlobal(); }
			 */
			for (AbstractBone bc : b.getChildren()) {
				recursivelyAlignBonesToSimAxesFrom(bc);
			}
			chain.simAligned = false;
			chain.processed = false;
		} else {
			int debug = 0;
		}

	}

	/**
	 * populates the given arraylist with the rootmost unprocessed chains of this
	 * segmented armature and its descenedants up until their pinned tips.
	 * 
	 * @param segments
	 */
	public void getRootMostUnprocessedChains(ArrayList<SegmentedArmature> segments) {
		if (!this.processed) {
			segments.add(this);
		} else {
			if (this.tipPinned)
				return;
			for (SegmentedArmature c : childSegments) {
				c.getRootMostUnprocessedChains(segments);
			}
		}
	}

	public void setProcessed(boolean b) {
		this.processed = b;
		if (processed == false) {
			for (SegmentedArmature c : childSegments) {
				c.setProcessed(false);
			}
		}
	}

	private double previousDeviation = Double.POSITIVE_INFINITY;
	private boolean simAligned = false;

	/**
	 * Holds working information for the given bone.
	 * 
	 * @author Eron Gjoni
	 */
	class WorkingBone {
		AbstractBone forBone;
		AbstractAxes simLocalAxes;
		AbstractAxes simConstraintAxes;
		SegmentedArmature onChain;
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

		public WorkingBone(AbstractBone toSimulate, SegmentedArmature chain) {
			forBone = toSimulate;
			simLocalAxes = forBone.localAxes().getGlobalCopy();
			simConstraintAxes = forBone.getMajorRotationAxes().getGlobalCopy();
			onChain = chain;
			double predamp = 1d - forBone.getStiffness();
			double defaultDampening = forBone.parentArmature.dampening;
			double dampening = forBone.getParent() == null ? MathUtils.PI : predamp * defaultDampening;
			cosHalfDampen = Math.cos(dampening / 2d);
			AbstractKusudama k = ((AbstractKusudama) forBone.getConstraint());
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
		
		public void updateConePreference_concatenatedArrays(Vec3d<?>[] localizedTipHeadings, Vec3d<?>[] localizedTargetHeadings, double[] weights) {
			simLocalAxes.updateGlobal();
			simConstraintAxes.updateGlobal();
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
				targetHeadings_internalconcat[i].set(simConstraintAxes.getGlobalOf(limitConeLocalDirectionCache[j]).sub(simConstraintAxes.origin_()));
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
			for (int i = 0; i <= forBone.parentArmature.defaultStabilizingPassCount; i++) {
				updateTipHeadings(onChain.boneCenteredTipHeadings, true);
				updateConePreference_concatenatedArrays(onChain.boneCenteredTipHeadings, onChain.boneCenteredTargetHeadings, onChain.weights);
				updateOptimalRotationToPinnedDescendants(newDampening, translate, tipHeadings_internalconcat, targetHeadings_internalconcat, weights_internalconcat);
				/*updateOptimalRotationToPinnedDescendants(newDampening, translate, onChain.boneCenteredTipHeadings,
						onChain.boneCenteredTargetHeadings, weights);*/
				if (forBone.parentArmature.defaultStabilizingPassCount > 0) {
					updateTipHeadings(onChain.uniform_boneCenteredTipHeadings, false);
					double currentmsd = getManualMSD(uniform_boneCenteredTipHeadings, boneCenteredTargetHeadings,
							weights);
					if (currentmsd <= previousDeviation * 1.000001d) {
						previousDeviation = currentmsd;
						gotCloser = true;
						break;
					} else gotCloser = false;
					
				}
			}
			if(!gotCloser)
				simLocalAxes.setLocalOrientationTo(prevOrientation);
				
			if (onChain.wb_segmentRoot == this) 
				previousDeviation = Double.POSITIVE_INFINITY;
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

			forBone.setAxesToSnapped(simLocalAxes, simConstraintAxes);
			simLocalAxes.translateByGlobal(translateBy);
			simConstraintAxes.translateByGlobal(translateBy);
		}

		public void pullBackTowardAllowableRegion(int iteration, int totalIterations) {
			if (springy) {
				double coshalfDamp;
				double halfDamp;
				if (totalIterations != forBone.parentArmature.getDefaultIterations()) {
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
		}

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
			for (int i = 0; i < pinnedBones.length; i++) {
				WorkingBone sb = pinnedBones[i];
				AbstractIKPin pin = sb.forBone.getIKPin();
				AbstractAxes targetAxes = pin.forBone.getPinnedAxes();
				targetAxes.updateGlobal();
				Vec3d<?> origin = simLocalAxes.origin_();
				localizedTargetHeadings[hdx].set(targetAxes.origin_()).sub(origin);
				byte modeCode = pin.getModeCode();
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

			for (int i = 0; i < pinnedBones.length; i++) {
				WorkingBone sb = pinnedBones[i];
				AbstractIKPin pin = sb.forBone.getIKPin();
				AbstractAxes tipAxes = sb.simLocalAxes;
				tipAxes.updateGlobal();
				Vec3d<?> origin = simLocalAxes.origin_();
				byte modeCode = pin.getModeCode();

				AbstractAxes targetAxes = pin.forBone.getPinnedAxes();
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
			double defaultDampening = forBone.parentArmature.dampening;
			double dampening = forBone.getParent() == null ? MathUtils.PI : predamp * defaultDampening;
			cosHalfDampen = Math.cos(dampening / 2d);
			AbstractKusudama k = ((AbstractKusudama) forBone.getConstraint());
			if (k != null && k.getPainfulness() >= 0d) {
				returnDamp = Math.max(defaultDampening / 2d, k.getPainfulness() / 2d);
				cosHalfReturnDamp = Math.cos(returnDamp / 2d);
				springy = true;
				populateReturnDampeningIterationArray(k);
			} else {
				springy = false;
			}
		}

		public void populateReturnDampeningIterationArray(AbstractKusudama k) {
			double iterations = forBone.parentArmature.getDefaultIterations();
			halfReturnfullnessDampened_iterated = new double[(int) iterations];
			cosHalfReturnfullnessDampened_iterated = new double[(int) iterations];
			for (double i = 0; i < iterations; i++) {
				int a = 0;
				double iterationsClamp = computeIterateReturnfulness(i, forBone.parentArmature.getDefaultIterations(),
						k);
				double cosIterationReturnClamp = Math.cos(iterationsClamp / 2d);
				halfReturnfullnessDampened_iterated[(int) i] = iterationsClamp;
				cosHalfReturnfullnessDampened_iterated[(int) i] = cosIterationReturnClamp;
			}
		}

		public double computeIterateReturnfulness(double iteration, double totalIteration, Constraint k) {
			double predamp = 1d - forBone.getStiffness();
			double defaultDampening = forBone.parentArmature.dampening;
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
