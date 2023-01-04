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
package IK.doubleIK;

import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JInternalFrame;

import IK.doubleIK.SegmentedArmature;
import IK.doubleIK.SegmentedArmature.WorkingBone;
import asj.LoadManager;
import asj.SaveManager;
import asj.Saveable;
import asj.data.JSONObject;
import data.EWBIKLoader;
import data.EWBIKSaver;
import math.doubleV.AbstractAxes;
import math.doubleV.Rot;
import math.doubleV.SGVec_3d;
import math.doubleV.Vec3d;

/**
 * An Armature is a hierarchical collection of Bones. Bones must be descendants
 * of an Armature in order for the IKSolver to run on them.
 * 
 * @author Eron Gjoni
 */
public abstract class AbstractArmature implements Saveable {

	protected AbstractAxes localAxes;
	protected AbstractAxes tempWorkingAxes;
	protected ArrayList<AbstractBone> bones = new ArrayList<AbstractBone>();
	protected HashMap<String, AbstractBone> tagBoneMap = new HashMap<String, AbstractBone>();
	protected HashMap<AbstractBone, SegmentedArmature> boneSegmentMap = new HashMap<AbstractBone, SegmentedArmature>();
	protected AbstractBone rootBone;
	protected WorkingBone[] traversalArray;
	protected WorkingBone[] returnfulArray;
	protected HashMap<AbstractBone, Integer> traversalIndex;
	protected HashMap<AbstractBone, Integer> returnfulIndex;
	public SegmentedArmature segmentedArmature;
	// public StrandedArmature strandedArmature;
	protected String tag;

	// protected int IKType = ORIENTATIONAWARE;
	protected int IKIterations = 15;
	protected double dampening = Math.toRadians(5d);
	private boolean abilityBiasing = false;

	public double IKSolverStability = 0d;
	PerformanceStats performance = new PerformanceStats();

	public int defaultStabilizingPassCount = 1;

	AbstractAxes fauxParent;

	public AbstractArmature() {
	}

	/**
	 * Initialize an Armature with a default root bone matching the given
	 * parameters.. The rootBone's length will be 1.
	 * 
	 * @param inputOrigin Desired location and orientation of the rootBone.
	 * @param name        A human readable name for this armature
	 */
	public AbstractArmature(AbstractAxes inputOrigin, String name) {

		this.localAxes = (AbstractAxes) inputOrigin;
		this.tempWorkingAxes = localAxes.getGlobalCopy();
		this.tag = name;
		createRootBone(localAxes.y_().heading(), localAxes.z_().heading(), tag + " : rootBone", 1d,
				AbstractBone.frameType.GLOBAL);
	}

	/**
	 * Set the inputBone as this Armature's Root Bone.
	 * 
	 * @param inputBone
	 * @return
	 */
	public AbstractBone createRootBone(AbstractBone inputBone) {
		this.rootBone = inputBone;
		this.segmentedArmature = new SegmentedArmature(rootBone);
		fauxParent = rootBone.localAxes().getGlobalCopy();

		return rootBone;
	}

	private <V extends Vec3d<?>> AbstractBone createRootBone(V tipHeading, V rollHeading, String inputTag,
			double boneHeight, AbstractBone.frameType coordinateType) {
		initializeRootBone(this, tipHeading, rollHeading, inputTag, boneHeight, coordinateType);
		this.segmentedArmature = new SegmentedArmature(rootBone);
		fauxParent = rootBone.localAxes().getGlobalCopy();

		return rootBone;
	}

	protected abstract void initializeRootBone(AbstractArmature armature, Vec3d<?> tipHeading, Vec3d<?> rollHeading,
			String inputTag, double boneHeight, AbstractBone.frameType coordinateType);

	/**
	 * The default number of iterations to run over this armature whenever
	 * IKSolver() is called. The higher this value, the more likely the Armature is
	 * to have converged on a solution when by the time it returns. However, it will
	 * take longer to return (linear cost)
	 * 
	 * @param iter
	 */
	public void setDefaultIterations(int iter) {
		this.IKIterations = iter;
		updateArmatureSegments();
	}

	/**
	 * The default maximum number of radians a bone is allowed to rotate per solver
	 * iteration. The lower this value, the more natural the pose results. However,
	 * this will the number of iterations the solver requires to converge.
	 * 
	 * !!THIS IS AN EXPENSIVE OPERATION. This updates the entire armature's cache of
	 * precomputed quadrance angles. The cache makes things faster in general, but
	 * if you need to dynamically change the dampening during a call to IKSolver,
	 * use the IKSolver(bone, dampening, iterations, stabilizationPasses) function,
	 * which clamps rotations on the fly.
	 * 
	 * @param damp
	 */
	public void setDefaultDampening(double damp) {
		this.dampening = Math.min(Math.PI * 3d, Math.max(Math.abs(Double.MIN_VALUE), Math.abs(damp)));
		updateArmatureSegments();
	}

	/**
	 * @return the rootBone of this armature.
	 */
	public AbstractBone getRootBone() {
		return rootBone;
	}

	/**
	 * (warning, this function is untested)
	 * 
	 * @return all bones belonging to this armature.
	 */
	public ArrayList<? extends AbstractBone> getBoneList() {
		this.bones.clear();
		rootBone.addDescendantsToArmature();
		return bones;
	}

	/**
	 * The armature maintains an internal hashmap of bone name's and their
	 * corresponding bone objects. This method should be called by any bone object
	 * if ever its name is changed.
	 * 
	 * @param bone
	 * @param previousTag
	 * @param newTag
	 */
	protected void updateBoneTag(AbstractBone bone, String previousTag, String newTag) {
		tagBoneMap.remove(previousTag);
		tagBoneMap.put(newTag, bone);
	}

	/**
	 * this method should be called by any newly created bone object if the armature
	 * is to know it exists.
	 * 
	 * @param bone
	 */
	protected void addToBoneList(AbstractBone abstractBone) {
		if (!bones.contains(abstractBone)) {
			bones.add(abstractBone);
			tagBoneMap.put(abstractBone.getTag(), abstractBone);
		}
	}

	/**
	 * this method should be called by any newly deleted bone object if the armature
	 * is to know it no longer exists
	 */
	protected void removeFromBoneList(AbstractBone abstractBone) {
		if (bones.contains(abstractBone)) {
			bones.remove(abstractBone);
			tagBoneMap.remove(abstractBone);
			this.updateArmatureSegments();
		}
	}

	/**
	 * 
	 * @param tag the tag of the bone object you wish to retrieve
	 * @return the bone object corresponding to this tag
	 */

	public AbstractBone getBoneTagged(String tag) {
		return tagBoneMap.get(tag);
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

	/*
	 * @param inverseWeighted if true, will apply an additional rotation penalty on
	 * the peripheral bones near a target so as to result in more natural poses with
	 * less need for dampening.
	 */
	/*
	 * public void setInverseWeighted(boolean inverseWeighted) {
	 * this.inverseWeighted = inverseWeighted; }
	 * 
	 * public boolean isInverseWeighted() { return this.inverseWeighted; }
	 */

	/**
	 * this method should be called whenever a bone in this armature has been pinned
	 * or unpinned.
	 * 
	 * for the most part, the abstract classes call this when necessary. But if you
	 * are extending classes more than you would reasonably expect this library to
	 * reasonably expect and getting weird results, you might try calling this
	 * method after making any substantial structural changes to the armature.
	 */
	public void updateArmatureSegments() {
		segmentedArmature.updateSegmentedArmature();
		boneSegmentMap.clear();
		recursivelyUpdateBoneSegmentMapFrom(segmentedArmature);
		SegmentedArmature.recursivelyCreateHeadingArraysFor(segmentedArmature);
		WorkingBone[][] built = buildTraversalArrayFromGroups(segmentedArmature);
		traversalArray = built[0];
		returnfulArray = built[1];
		traversalIndex = new HashMap<AbstractBone, Integer>();
		returnfulIndex = new HashMap<AbstractBone, Integer>();
		for (int i = 0; i < traversalArray.length; i++) {
			traversalIndex.put(traversalArray[i].forBone, i);
		}
		for (int i = 0; i < returnfulArray.length; i++) {
			returnfulIndex.put(returnfulArray[i].forBone, i);
		}
	}

	private void recursivelyUpdateBoneSegmentMapFrom(SegmentedArmature startFrom) {
		for (AbstractBone b : startFrom.segmentBoneList) {
			boneSegmentMap.put(b, startFrom);
		}
		for (SegmentedArmature c : startFrom.childSegments) {
			recursivelyUpdateBoneSegmentMapFrom(c);
		}
	}

	/**
	 * If you have created some sort of save / load system for your armatures which
	 * might make it difficult to notify the armature when a pin has been enabled on
	 * a bone, you can call this function after all bones and pins have been
	 * instantiated and associated with one another to index all of the pins on the
	 * armature.
	 */
	public void refreshArmaturePins() {
		AbstractBone rootBone = this.getRootBone();
		ArrayList<AbstractBone> pinnedBones = new ArrayList<>();
		rootBone.addSelfIfPinned(pinnedBones);

		for (AbstractBone b : pinnedBones) {
			b.notifyAncestorsOfPin(false);
			updateArmatureSegments();
		}
	}

	/**
	 * automatically solves the IK system of this armature from the given bone using
	 * the armature's default IK parameters.
	 * 
	 * You can specify these using the setDefaultIterations() setDefaultIKType() and
	 * setDefaultDampening() methods. The library comes with some defaults already
	 * set, so you can more or less use this method out of the box if you're just
	 * testing things out.
	 * 
	 * @param bone
	 */
	public void IKSolver(AbstractBone bone) {
		IKSolver(bone, -1, -1, -1);
	}

	/**
	 * automatically solves the IK system of this armature from the given bone using
	 * the given parameters.
	 * 
	 * @param bone
	 * @param dampening         dampening angle in radians. Set this to -1 if you
	 *                          want to use the armature's default.
	 * @param iterations        number of iterations to run. Set this to -1 if you
	 *                          want to use the armature's default.
	 * @param stabilizingPasses number of stabilization passes to run. Set this to
	 *                          -1 if you want to use the armature's default.
	 */
	public void IKSolver(AbstractBone bone, double dampening, int iterations, int stabilizingPasses) {
		performance.startPerformanceMonitor();
		flatTraveseSolver(bone, dampening, iterations, stabilizingPasses);// (bone, dampening, iterations);
		performance.solveFinished(iterations == -1 ? this.IKIterations : iterations);
	}

	/**
	 * The solver tends to be quite stable whenever a pose is reachable (or
	 * unreachable but without excessive contortion). However, in cases of extreme
	 * unreachability (due to excessive contortion on orientation constraints), the
	 * solution might fail to stabilize, resulting in an undulating motion.
	 * 
	 * Setting this parameter to "1" will prevent such undulations, with a
	 * negligible cost to performance. Setting this parameter to a value higher than
	 * 1 will offer minor benefits in pose quality in situations that would
	 * otherwise be prone to instability, however, it will do so at a significant
	 * performance cost.
	 * 
	 * You're encourage to experiment with this parameter as per your use case, but
	 * you may find the following guiding principles helpful:
	 * <ul>
	 * <li>If your armature doesn't have any constraints, then leave this parameter
	 * set to 0.</li>
	 * <li>If your armature doesn't make use of orientation aware pins (x,y,and,z
	 * direction pin priorities are set to 0) the leave this parameter set to 0.
	 * </li>
	 * <li>If your armature makes use of orientation aware pins and orientation
	 * constraints, then set this parameter to 1</li>
	 * <li>If your armature makes use of orientation aware pins and orientation
	 * constraints, but speed is of the highest possible priority, then set this
	 * parameter to 0</li>
	 * </ul>
	 * 
	 * @param passCount
	 */
	public void setDefaultStabilizingPassCount(int passCount) {
		defaultStabilizingPassCount = passCount;
	}

	/**
	 * 
	 * @return a reference to the Axes serving as this Armature's coordinate system.
	 */
	public AbstractAxes localAxes() {
		return this.localAxes;
	}
	
	private void iterativelyNotifyBonesOfCompletedIKSolution(int startFrom, int endOn) { 
		for(int i=startFrom; i>=endOn; i--) {
			traversalArray[i].forBone.IKUpdateNotification();
		}
	}
	private void recursivelyNotifyBonesOfCompletedIKSolution(SegmentedArmature startFrom) {
		for (AbstractBone b : startFrom.segmentBoneList) {
			b.IKUpdateNotification();
		}
		for (SegmentedArmature s : startFrom.childSegments) {
			recursivelyNotifyBonesOfCompletedIKSolution(s);
		}
	}

	/**
	 * @param startFrom
	 * @param dampening
	 * @param iterations
	 */

	public void flatTraveseSolver(AbstractBone startFrom, double dampening, int iterations, int stabilizationPasses) {
		int endOnIndex = traversalArray.length - 1;
		//int returnfullEndOnIndex = returnfulArray.length > 0 ? returnfulIndex.get(startFrom);  
		int tipIndex = 0;
		SegmentedArmature forSegment = segmentedArmature;
		iterations = iterations == -1 ? IKIterations : iterations;
		double totalIterations = iterations;
		stabilizationPasses = stabilizationPasses == -1 ? this.defaultStabilizingPassCount : stabilizationPasses;
		if (startFrom != null) {
			forSegment = boneSegmentMap.get(startFrom);
			AbstractBone endOnBone = forSegment.segmentRoot;
			endOnIndex = traversalIndex.get(endOnBone);
		}

		iterativelyAlignSimAxesToBones(traversalArray, endOnIndex);

		for (int i = 0; i < iterations; i++) {
			for (int j = 0; j <= endOnIndex; j++) {
				traversalArray[j].fastUpdateOptimalRotationToPinnedDescendants(dampening,
						j == endOnIndex && endOnIndex == traversalArray.length - 1);
			}
			/*if(i < totalIterations - 1) {
				for (int j = 0; j <= endOnIndex; j++) {
					traversalArray[j].pullBackTowardAllowableRegion(i, iterations);
				}
			}*/
		}

		iterativelyAlignBonesToSimAxesFrom(traversalArray, endOnIndex);
		iterativelyNotifyBonesOfCompletedIKSolution(tipIndex, endOnIndex);
	}

	/**
	 * @param startFrom
	 * @param dampening
	 * @param iterations
	 */

	/*public void iteratedImprovedSolver(AbstractBone startFrom, double dampening, int iterations,
			int stabilizationPasses) {
		SegmentedArmature armature = boneSegmentMap.get(startFrom);

		if (armature != null) {
			SegmentedArmature pinnedRootChain = armature.getPinnedRootChainFromHere();
			armature = pinnedRootChain == null ? armature.getAncestorSegmentContaining(rootBone) : pinnedRootChain;
			if (armature != null && armature.pinnedDescendants.size() > 0) {
				armature.alignSimulationAxesToBones();

				iterations = iterations == -1 ? IKIterations : iterations;
				double totalIterations = iterations;
				// dampening = dampening == -1? this.dampening : dampening;
				stabilizationPasses = stabilizationPasses == -1 ? this.defaultStabilizingPassCount
						: stabilizationPasses;
				if (!armature.isBasePinned() && armature.getParentSegment() == null) {
					// alignSegmentTipOrientationsFor(armature, dampening);
					//
					// this condition translates the rootmost chain to a location that maximizes the
					// other chain's ability to reach their target.
					// If this translation behavior isn't desired, simply set the rootmost bone to
					// be an effector, in which case, the root bone will always end up
					// aligning to the specific transformation specified by its target.
					//
					
					armature.updateOptimalRotationToPinnedDescendants(armature.segmentRoot, Math.PI * 2, true,
							stabilizationPasses, 0, totalIterations);
					// armature.setProcessed(false);
					for (int i = 0; i < iterations; i++) {
						for (SegmentedArmature c : armature.childSegments) {
							groupedRecursiveSegmentSolver(c, dampening, stabilizationPasses, i, totalIterations);
						}
						// outwardRecursiveSegmentSolver(armature, dampening);
						// alignSegmentTipOrientationsFor(armature, dampening);
					}

				} else {
					for (int i = 0; i < iterations; i++) {
						groupedRecursiveSegmentSolver(armature, dampening, stabilizationPasses, i, totalIterations);
						// outwardRecursiveSegmentSolver(armature, dampening);
						// alignSegmentTipOrientationsFor(armature, dampening);
					}
				}
				armature.recursivelyAlignBonesToSimAxesFrom(armature.segmentRoot);
				recursivelyNotifyBonesOfCompletedIKSolution(armature);
			}
		}
	}*/

	/**returns a two element array of WorkingBone arrays in the order which they should be traversed in
	 * the 0th element is for trying to reach targets, the 1st element is for trying to reach comfort.
	 */
	private WorkingBone[][] buildTraversalArrayFromGroups(SegmentedArmature startFrom) {
		ArrayList<WorkingBone> boneList = new ArrayList<WorkingBone>();
		ArrayList<WorkingBone> returnfulList = new ArrayList<WorkingBone>();
		buildTraversalArrayFromSegments(startFrom, boneList, returnfulList);
		WorkingBone[] boneListResult = new WorkingBone[boneList.size()];
		WorkingBone[] returnfulResult = new WorkingBone[returnfulList.size()];
		WorkingBone[][] result = {boneList.toArray(boneListResult), returnfulList.toArray(returnfulResult)};
		return result;
	}

	private void buildTraversalArrayFromSegments(SegmentedArmature startFrom, ArrayList<WorkingBone> boneList, ArrayList<WorkingBone> returnfulList) {
		for (SegmentedArmature a : startFrom.pinnedDescendants) {
			for (SegmentedArmature c : a.childSegments) {
				buildTraversalArrayFromSegments(c, boneList, returnfulList);
			}
		}
		buildTraversalArrayFromChains(startFrom, boneList, returnfulList);
	}

	private void buildTraversalArrayFromChains(SegmentedArmature chain, ArrayList<WorkingBone> boneList, ArrayList<WorkingBone> returnfulList) {
		if (chain.childSegments == null && !chain.isTipPinned()) {
			return;
		} else if (!chain.isTipPinned()) {
			for (SegmentedArmature c : chain.childSegments) {
				buildTraversalArrayFromChains(c, boneList, returnfulList);
			}
		}
		pushSegmentBonesToTraversalArray(chain, boneList, returnfulList);
	}

	private void pushSegmentBonesToTraversalArray(SegmentedArmature chain, ArrayList<WorkingBone> boneList, ArrayList<WorkingBone> returnfulList) {
		AbstractBone startFrom = debug && lastDebugBone != null ? lastDebugBone : chain.segmentTip;
		AbstractBone stopAfter = chain.segmentRoot;

		AbstractBone currentBone = startFrom;
		while (currentBone != null) {
			boneList.add(chain.simulatedBones.get(currentBone));
			if(currentBone.getConstraint() != null)
				if(currentBone.getConstraint().getPainfulness() > 0) {
					returnfulList.add(chain.simulatedBones.get(currentBone));
				}
			if (currentBone == stopAfter)
				currentBone = null;
			else
				currentBone = currentBone.getParent();
		}
	}

	public void groupedRecursiveSegmentSolver(SegmentedArmature startFrom, double dampening, int stabilizationPasses,
			int iteration, double totalIterations) {
		recursiveSegmentSolver(startFrom, dampening, stabilizationPasses, iteration, totalIterations);
		for (SegmentedArmature a : startFrom.pinnedDescendants) {
			for (SegmentedArmature c : a.childSegments) {
				// alignSegmentTipOrientationsFor(startFrom, dampening);
				groupedRecursiveSegmentSolver(c, dampening, stabilizationPasses, iteration, totalIterations);
			}
		}
		// alignSegmentTipOrientationsFor(startFrom, dampening);
	}

	/**
	 * aligns this bone and all relevant childBones to their coresponding
	 * simulatedAxes (if any) in the SegmentedArmature
	 * 
	 * @param b bone to start from
	 */
	public void iterativelyAlignBonesToSimAxesFrom(WorkingBone[] bonelist, int from) {
		// SegmentedArmature chain = b.parentArmature.boneSegmentMap.get(b);
		// //getChainFor(b);

		for (int i = from; i >= 0; i--) {
			WorkingBone sb = bonelist[i];
			AbstractAxes simulatedLocalAxes = sb.simLocalAxes;
			AbstractBone b = sb.forBone;
			/*if (b.parent != null) {
				// TODO: test robustness / efficiency of avoiding global update
				b.localAxes().localMBasis.rotateTo(simulatedLocalAxes.localMBasis.rotation);
				b.localAxes().markDirty();
				b.localAxes().updateGlobal();
			} else {*/
				b.localAxes().alignLocalsTo(simulatedLocalAxes);
			//}
		}
	}

	/**
	 * align the WorkingBone SimulationAxes to the boneAxes outward from the given
	 * bone index
	 **/
	public void iterativelyAlignSimAxesToBones(WorkingBone[] bonelist, int from) {

		// branching outside of loop in hopes of tiny performance gains

		for (int i = from; i >= 0; i--) {
			WorkingBone sb = bonelist[i];
			/*
			 * if (!sb.onChain.isBasePinned()) { sbAxes.alignGlobalsTo(b.localAxes());
			 * sbAxes.markDirty(); sbAxes.updateGlobal();
			 * cAxes.alignGlobalsTo(b.getMajorRotationAxes()); cAxes.markDirty();
			 * cAxes.updateGlobal(); } else {&=
			 */
			sb.simLocalAxes.alignLocalsTo(sb.forBone.localAxes());
			sb.simConstraintAxes.alignLocalsTo(sb.forBone.getMajorRotationAxes());
			// }
		}

	}

	/**
	 * given a segmented armature, solves each chain from its pinned tips down to
	 * its pinned root.
	 * 
	 * @param armature
	 */
	public void recursiveSegmentSolver(SegmentedArmature armature, double dampening, int stabilizationPasses,
			int iteration, double totalIterations) {
		if (armature.childSegments == null && !armature.isTipPinned()) {
			return;
		} else if (!armature.isTipPinned()) {
			for (SegmentedArmature c : armature.childSegments) {
				recursiveSegmentSolver(c, dampening, stabilizationPasses, iteration, totalIterations);
				// c.setProcessed(true);
			}
		}
		QCPSolver(armature, dampening, false, stabilizationPasses, iteration, totalIterations);
	}

	boolean debug = true;
	AbstractBone lastDebugBone = null;

	private void QCPSolver(SegmentedArmature chain, double dampening, boolean inverseWeighting, int stabilizationPasses,
			int iteration, double totalIterations) {

		debug = false;

		// lastDebugBone = null;
		AbstractBone startFrom = debug && lastDebugBone != null ? lastDebugBone : chain.segmentTip;
		AbstractBone stopAfter = chain.segmentRoot;

		AbstractBone currentBone = startFrom;
		if (debug && chain.simulatedBones.size() < 2) {

		} else {
			/*
			 * if(chain.isTipPinned() && chain.segmentTip.getIKPin().getDepthFalloff() ==
			 * 0d) alignSegmentTipOrientationsFor(chain, dampening);
			 */
			// System.out.print("---------");
			while (currentBone != null) {
				if (!currentBone.getIKOrientationLock()) {
					chain.updateOptimalRotationToPinnedDescendants(currentBone, dampening, false, stabilizationPasses,
							iteration, totalIterations);
				}
				if (currentBone == stopAfter)
					currentBone = null;
				else
					currentBone = currentBone.getParent();

				if (debug) {
					lastDebugBone = currentBone;
					break;
				}
			}

		}
	}

	void rootwardlyUpdateFalloffCacheFrom(AbstractBone forBone) {
		SegmentedArmature current = boneSegmentMap.get(forBone);
		while (current != null) {
			current.createHeadingArrays();
			current = current.getParentSegment();
		}
	}

	// debug code -- use to set a minimum distance an effector must move
	// in order to trigger a chain iteration
	double debugMag = 5f;
	SGVec_3d lastTargetPos = new SGVec_3d();

	/**
	 * currently unused
	 * 
	 * @param enabled
	 */
	public void setAbilityBiasing(boolean enabled) {
		abilityBiasing = enabled;
	}

	public boolean getAbilityBiasing() {
		return abilityBiasing;
	}

	/**
	 * returns the rotation that would bring the right-handed orthonormal axes of a
	 * into alignment with b
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public Rot getRotationBetween(AbstractAxes a, AbstractAxes b) {
		return new Rot(a.x_().heading(), a.y_().heading(), b.x_().heading(), b.y_().heading());
	}

	public int getDefaultIterations() {
		return IKIterations;
	}

	public double getDampening() {
		return dampening;
	}

	boolean monitorPerformance = true;

	public void setPerformanceMonitor(boolean state) {
		monitorPerformance = state;
	}

	public class PerformanceStats {
		int timedCalls = 0;
		int benchmarkWindow = 60;
		int iterationCount = 0;
		float averageSolutionTime = 0;
		float averageIterationTime = 0;
		int solutionCount = 0;
		float iterationsPerSecond = 0f;
		long totalSolutionTime = 0;

		long startTime = 0;

		public void startPerformanceMonitor() {
			monitorPerformance = true;
			if (monitorPerformance) {
				if (timedCalls > benchmarkWindow) {
					performance.resetPerformanceStat();
				}
				startTime = System.nanoTime();
			}
		}

		public void solveFinished(int iterations) {
			if (monitorPerformance) {
				totalSolutionTime += System.nanoTime() - startTime;
				// averageSolutionTime *= solutionCount;
				solutionCount++;
				iterationCount += iterations;

				if (timedCalls > benchmarkWindow) {
					timedCalls = 0;
					performance.printStats();
				}
				timedCalls++;
			}
		}

		public void resetPerformanceStat() {
			startTime = 0;
			iterationCount = 0;
			averageSolutionTime = 0;
			solutionCount = 0;
			iterationsPerSecond = 0f;
			totalSolutionTime = 0;
			averageIterationTime = 0;
		}

		public void printStats() {
			averageSolutionTime = (float) (totalSolutionTime / solutionCount) / 1000000f;
			averageIterationTime = (float) (totalSolutionTime / iterationCount) / 1000000f;
			System.out.println("solution time average: ");
			System.out.println("per call = " + (averageSolutionTime) + "ms");
			System.out.println("per iteration = " + (averageIterationTime) + "ms \n");
		}

	}

	@Override
	public void makeSaveable(SaveManager saveManager) {
		saveManager.addToSaveState(this);
		if (this.localAxes().getParentAxes() != null)
			this.localAxes().getParentAxes().makeSaveable(saveManager);
		else
			this.localAxes().makeSaveable(saveManager);
		this.rootBone.makeSaveable(saveManager);
	}

	@Override
	public JSONObject getSaveJSON(SaveManager saveManager) {
		JSONObject saveJSON = new JSONObject();
		saveJSON.setString("identityHash", this.getIdentityHash());
		saveJSON.setString("localAxes", localAxes().getIdentityHash());
		saveJSON.setString("rootBone", getRootBone().getIdentityHash());
		saveJSON.setInt("defaultIterations", getDefaultIterations());
		saveJSON.setDouble("dampening", this.getDampening());
		// saveJSON.setBoolean("inverseWeighted", this.isInverseWeighted());
		saveJSON.setString("tag", this.getTag());
		return saveJSON;
	}

	public void loadFromJSONObject(JSONObject j, LoadManager l) {
		try {
			this.localAxes = l.getObjectFor(AbstractAxes.class, j, "localAxes");
			this.rootBone = l.getObjectFor(AbstractBone.class, j, "rootBone");
			if (j.hasKey("defaultIterations"))
				this.IKIterations = j.getInt("defaultIterations");
			if (j.hasKey("dampening"))
				this.dampening = j.getDouble("dampening");
			this.tag = j.getString("tag");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void notifyOfSaveIntent(SaveManager saveManager) {
		this.makeSaveable(saveManager);
	}

	@Override
	public void notifyOfSaveCompletion(SaveManager saveManager) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOfLoadCompletion() {
		this.createRootBone(rootBone);
		refreshArmaturePins();
		updateArmatureSegments();
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