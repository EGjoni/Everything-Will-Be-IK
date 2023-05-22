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

import IK.doubleIK.solver.ShadowSkeleton;
import IK.doubleIK.solver.SkeletonState;
import IK.doubleIK.solver.SkeletonState.BoneState;
import IK.doubleIK.solver.SkeletonState.ConstraintState;
import IK.doubleIK.solver.SkeletonState.TargetState;
import IK.doubleIK.solver.SkeletonState.TransformState;
import asj.LoadManager;
import asj.SaveManager;
import asj.Saveable;
import asj.data.JSONObject;
import data.EWBIKLoader;
import data.EWBIKSaver;
import math.doubleV.AbstractAxes;
import math.doubleV.AbstractBasis;
import math.doubleV.Rot;
import math.doubleV.SGVec_3d;
import math.doubleV.Vec3d;
import math.doubleV.sgRayd;

/**
 * An Armature is a hierarchical collection of Bones. Bones must be descendants
 * of an Armature in order for the IKSolver to run on them.
 * 
 * @author Eron Gjoni
 */
public abstract class AbstractArmature implements Saveable {

	private int IKIterations = 30;
	protected AbstractAxes localAxes;
	protected AbstractAxes tempWorkingAxes;
	protected ArrayList<AbstractBone> bones = new ArrayList<AbstractBone>();
	protected HashMap<String, AbstractBone> tagBoneMap = new HashMap<String, AbstractBone>();
	
	protected AbstractBone rootBone;
	protected HashMap<AbstractBone, Integer> traversalIndex;
	protected HashMap<AbstractBone, Integer> returnfulIndex;
	// public StrandedArmature strandedArmature;
	protected String tag;

	
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
		fauxParent = rootBone.localAxes().getGlobalCopy();

		return rootBone;
	}

	private <V extends Vec3d<?>> AbstractBone createRootBone(V tipHeading, V rollHeading, String inputTag,
			double boneHeight, AbstractBone.frameType coordinateType) {
		initializeRootBone(this, tipHeading, rollHeading, inputTag, boneHeight, coordinateType);
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
		regenerateShadowSkeleton();
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
		if(this.shadowSkel !=null) {
			this.shadowSkel.setDampening(this.dampening, this.getDefaultIterations());
		}
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
		bones.add(rootBone);
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
			this.regenerateShadowSkeleton();
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
			this.regenerateShadowSkeleton();
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

	private boolean dirtySkelState = false;
	private boolean dirtyRate = false;
	SkeletonState skelState;
	
	/**
	 * a list of the bones used by the solver, in the same order they appear in the skelState after validation.
	 * This is to very quickly update the scene with the solver's results, without incurring hashmap lookup penalty. 
	 ***/
	protected AbstractBone[] skelStateBoneList = new AbstractBone[0];
	
	ShadowSkeleton shadowSkel;
	private void _regenerateShadowSkeleton() {
		skelState = new SkeletonState();
		for(AbstractBone b: bones) {
			registerBoneWithShadowSkeleton(b);
		}
		skelState.validate();
		shadowSkel = new ShadowSkeleton(skelState, this.dampening);
		skelStateBoneList = new AbstractBone[skelState.getBoneCount()];
		for(int i=0; i<bones.size(); i++) {
			BoneState bonestate = skelState.getBoneStateById(bones.get(i).getIdentityHash());
			if(bonestate != null)
				skelStateBoneList[bonestate.getIndex()] = bones.get(i);
		}
		dirtySkelState = false;
	}
	
	/**
	 * This method should be called whenever a structural change has been made to the armature prior to calling the solver.
	 * A structural change is basically any change other than a rotation/translation/scale of a bone or a target. 
	 * Structural changes include things like, 
	 *		1. reparenting / adding / removing bones. 
	 * 	2. marking a bone as an effector (aka "pinning / unpinning a bone"
	 * 	3. adding / removing a constraint on a bone.
	 * 	4. modifying a pin's fallOff to non-zero if it was zero, or zero if it was non-zero
	 * 
	 * You should NOT call this function if you have only modified a translation/rotation/scale of some transform on the armature
	 * 
	 * For skeletal modifications that are likely to effect the solver behavior but do not fall 
	 * under any of the above (generally things like changing bone stiffness, depth falloff, targetweight, etc) to intermediary values, 
	 * you should (but don't have to) call updateShadowSkelRateInfo() for maximum efficiency.
	 */
	public void regenerateShadowSkeleton() {
		this.regenerateShadowSkeleton(false);
	}
	 /**
	 * @param force by default, calling this function sets a flag notifying the solver that it needs to regenerate the shadow skeleton before
	 * attempting a solve. If you set this to "true", the shadow skeleton will be regenerated immediately. 
	 * (useful if you do solves in a separate thread from structure updates)
	 */
	public void regenerateShadowSkeleton(boolean force) {
		dirtySkelState = true;
		if(force) 
			this._regenerateShadowSkeleton();
		dirtyRate = true;
	}
	
	public void updateShadowSkelRateInfo() {
		dirtyRate = true;
	}
	
	private void _updateShadowSkelRateInfo() {
		BoneState[] bonestates = skelState.getBonesArray();
		for(int i=0; i<skelStateBoneList.length; i++) {
			AbstractBone b = skelStateBoneList[i];
			BoneState bs = bonestates[i];
			bs.setStiffness(b.getStiffness());
		}
	}
	
	private void registerBoneWithShadowSkeleton(AbstractBone bone) { 
		String parBoneId = (bone.getParent() == null) ? null : bone.getParent().getIdentityHash(); 
		Constraint constraint = bone.getConstraint();
		String constraintId = (constraint == null) ? null : constraint.getIdentityHash(); 
		AbstractIKPin target = bone.getIKPin();
		String targetId = (target == null || target.getPinWeight() == 0 || target.isEnabled() == false) ? null : target.getIdentityHash();
		skelState.addBone(
				bone.getIdentityHash(), 
				bone.localAxes().getIdentityHash(), 
				parBoneId, 
				constraintId, 
				bone.getStiffness(),
				targetId, 
				bone);
		registerAxesWithShadowSkeleton(bone.localAxes(), bone.getParent() == null);
		if(targetId != null) registerTargetWithShadowSkeleton(target);
		if(constraintId != null) registerConstraintWithShadowSkeleton(constraint);
		
	}
	private void registerTargetWithShadowSkeleton(AbstractIKPin ikPin) {
		skelState.addTarget(ikPin.getIdentityHash(), 
				ikPin.getAxes().getIdentityHash(), 
				ikPin.forBone().getIdentityHash(),
				new double[] {ikPin.getXPriority(), ikPin.getYPriority(), ikPin.getZPriority()}, 
				ikPin.getDepthFalloff(),
				ikPin.getPinWeight());
		registerAxesWithShadowSkeleton(ikPin.getAxes(), true);
	}
	private void registerConstraintWithShadowSkeleton(Constraint constraint) {
		AbstractAxes twistAxes = constraint.twistOrientationAxes() == null ? null : constraint.twistOrientationAxes();
		skelState.addConstraint(
				constraint.getIdentityHash(),
				constraint.attachedTo().getIdentityHash(),
				constraint.getPainfulness(),
				constraint.swingOrientationAxes().getIdentityHash(),
				twistAxes == null ? null : twistAxes.getIdentityHash(),
				constraint);
		registerAxesWithShadowSkeleton(constraint.swingOrientationAxes(), false);
		if(twistAxes != null)
			registerAxesWithShadowSkeleton(twistAxes, false);
		
	}
	/**
	 * @param axes
	 * @param rebase if true, this function will not provide a parent_id for these axes.
	 * This is mostly usefu l for ensuring that targetAxes are always implicitly defined in skeleton space when calling the solver.
	 * You should always set this to true when giving the axes of an IKPin, as well as when giving the axes of the root bone. 
	 * see the skelState.addTransform documentation for more info. 
	 */
	private void registerAxesWithShadowSkeleton(AbstractAxes axes, boolean unparent) {
		String parent_id  = unparent || axes.getParentAxes() == null ? null : axes.getParentAxes().getIdentityHash();
		AbstractBasis basis = getSkelStateRelativeBasis(axes, unparent);
		Vec3d<?> translate = basis.translate;
		Rot rotation =basis.rotation;
		skelState.addTransform(
				axes.getIdentityHash(), 
				new double[]{translate.getX(), translate.getY(), translate.getZ()}, 
				rotation.toArray(), 
				new double[]{1.0,1.0,1.0}, 
				parent_id, axes);
	}
	
	/**
	 * @param axes
	 * @param unparent if true, will return a COPY of the basis in Armature space, otherwise, will return a reference to axes.localMBasis
	 * @return
	 */
	private AbstractBasis getSkelStateRelativeBasis(AbstractAxes axes, boolean unparent) {
		AbstractBasis basis = axes.getLocalMBasis(); 
		if(unparent) {
			AbstractBasis resultBasis = basis.copy();
			this.localAxes().getGlobalMBasis().setToLocalOf(axes.getGlobalMBasis(), resultBasis);
			return resultBasis;
		}
		return basis;
	}
	
	private void updateskelStateTransforms() {
		BoneState[] bonestates = skelState.getBonesArray();
		for(int i=0; i<skelStateBoneList.length; i++) {
			AbstractBone b = skelStateBoneList[i];
			BoneState bs = bonestates[i];
			updateSkelStateBone(b, bs);
		}
	}
	
	private void updateSkelStateBone(AbstractBone b, BoneState bs) {
		updateSkelStateAxes(b.localAxes(), bs.getTransform(), b.getParent() == null);
		if(b.getConstraint() != null) {
			updateSkelStateConstraint(b.getConstraint(), bs.getConstraint());
		}
		TargetState ts = bs.getTarget(); 
		if(ts != null) {
			updateSkelStateTarget(b.getIKPin(), ts);
		}
	}
	
	private void updateSkelStateConstraint(Constraint c, ConstraintState cs) {
		AbstractAxes swing = c.swingOrientationAxes();
			updateSkelStateAxes(swing, cs.getSwingTransform(), false);
		AbstractAxes twist = c.twistOrientationAxes();
		if(twist != null)
			updateSkelStateAxes(twist, cs.getTwistTransform(), false);
	}	
	
	private void updateSkelStateTarget(AbstractIKPin p, TargetState ts) {
		updateSkelStateAxes(p.getAxes(), ts.getTransform(), true);
	}
	
	private void updateSkelStateAxes(AbstractAxes a, TransformState ts, boolean unparent) {
		AbstractBasis basis = getSkelStateRelativeBasis(a, unparent);
		ts.rotation= basis.rotation.toArray(); 
		ts.translation = basis.translate.get();
		if(!a.forceOrthoNormality) {
			ts.scale[0] = basis.getXHeading().mag() * ( basis.isAxisFlipped(AbstractAxes.X) ? -1d : 1d);
			ts.scale[1] = basis.getYHeading().mag() * ( basis.isAxisFlipped(AbstractAxes.Y) ? -1d : 1d); 
			ts.scale[2] = basis.getZHeading().mag() * ( basis.isAxisFlipped(AbstractAxes.Z) ? -1d : 1d);
		} else {
			ts.scale[0] = basis.isAxisFlipped(AbstractAxes.X) ? -1d : 1d;
			ts.scale[1] = basis.isAxisFlipped(AbstractAxes.Y) ? -1d : 1d; 
			ts.scale[2] = basis.isAxisFlipped(AbstractAxes.Z) ? -1d : 1d;
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
		IKSolver(bone, -1, -1);
	}

	/**
	 * automatically solves the IK system of this armature from the given bone using
	 * the given parameters.
	 * 
	 * @param bone
	 * @param iterations        number of iterations to run. Set this to -1 if you
	 *                          want to use the armature's default.
	 * @param stabilizingPasses number of stabilization passes to run. Set this to
	 *                          -1 if you want to use the armature's default.
	 */
	public void IKSolver(AbstractBone bone, int iterations, int stabilizingPasses) {
		if(dirtySkelState) 
			_regenerateShadowSkeleton();
		if(dirtyRate) {
			_updateShadowSkelRateInfo();
			shadowSkel.updateRates(iterations);
			dirtyRate = false;
		}
		performance.startPerformanceMonitor();
		this.updateskelStateTransforms();
		shadowSkel.solve(
				iterations == -1 ? this.getDefaultIterations() : iterations, 
				stabilizingPasses == -1? this.defaultStabilizingPassCount : stabilizingPasses, 
				(bonestate) -> alignBoneToSolverResult(bonestate));
		performance.solveFinished(iterations == -1 ? this.IKIterations : iterations);
	}
	
	/**for debugging purposes, does a single pullback iteration on the shadow skeleton, then updates 
	 * the bones with the results.
	 */
	public void _doSinglePullbackStep() {
		if(dirtySkelState) 
			_regenerateShadowSkeleton();
		if(dirtyRate) {
			_updateShadowSkelRateInfo();
			shadowSkel.updateRates(this.getDefaultIterations());
			dirtyRate = false;
		}
		this.updateskelStateTransforms();
		shadowSkel.alignSimAxesToBoneStates();
		shadowSkel.pullBack(this.getDefaultIterations(), true, (bonestate) -> alignBoneToSolverResult(bonestate));
	}
	
	/**for debugging purposes, does a single step to move effectors toward their targets on the shadow skeleton, then updates 
	 * the bones with the results.
	 */
	public void _doSingleTowardTargetStep() {
		if(dirtySkelState) 
			_regenerateShadowSkeleton();
		if(dirtyRate) {
			_updateShadowSkelRateInfo();
			shadowSkel.updateRates(this.getDefaultIterations());
			dirtyRate = false;
		}
		this.updateskelStateTransforms();
		shadowSkel.alignSimAxesToBoneStates();
		shadowSkel.solveToTargets(0, true, (bonestate) -> alignBoneToSolverResult(bonestate));
	}
	
	/**
	 * read back the solver results from the SkeletonState object. 
	 * The solver only ever modifies the transforms of the bones themselves
	 * so we don't need to bother with constraint or target transforms.
	 */
	private void alignBonesListToSolverResults() {
		BoneState[] bonestates = skelState.getBonesArray();
		for(int i=0; i<bonestates.length; i++) {
			alignBoneToSolverResult(bonestates[i]);
		}
	}
	
	/**
	 * read back the solver results from the given BoneState to the corresponding AbstractBone. 
	 * The solver only ever modifies things in local space, so we can just markDirty after
	 * manually updating the local transforms and defer global space updates to be triggered by 
	 * whatever else in the pipeline happens to need them.
	 */

	private void alignBoneToSolverResult(BoneState bs) {
		int bsi = bs.getIndex();
		AbstractBone currBone = skelStateBoneList[bsi];
		AbstractAxes currBoneAx = currBone.localAxes();
		TransformState ts = bs.getTransform();
		currBoneAx.getLocalMBasis().set(ts.translation, ts.rotation, ts.scale);
		currBoneAx.markDirty();
		currBone.IKUpdateNotification();
	}

	/**
	 * The solver tends to be quite stable whenever a pose is reachable (or
	 * unreachable but without excessive contortion). However, in cases of extreme
	 * unreachability (due to excessive contortion on orientation constraints), the
	 * solution might fail to stabilize, resulting in an undulating motion.
	 * 
	 * If you are porting this library, it is ESSENTIAL THAT YOU KEEP THIS PARAMETER AT 0 WHILE TESTING, 
	 * as enablling this stabilizer will fool you into thinking the solver is sort-of-working when in fact 
	 * the stabilizer is just hiding some critical failure in your implementation. 
	 * 
	 * Setting this parameter to "1" will prevent such undulations, with a
	 * negligible cost to performance. However, the bones may look like they're grinding against
	 * their constraint boundaries. 
	 * 
	 * The higher you set this parameter greater than 1, the more this grindiness may be alleviated, 
	 * however note that grindiness you experience while stress-testing is highly unlikely 
	 * to be encountered in normal operation, and values higher than 1 have a very large 
	 * performance penalty -- so you should strongly prefer to keep this value no higher than 1.
	 * 
	 * DO NOT SET THIS TO 1 IF YOU ARE PORTING AND NOTICE INSTABILITY WHEN SOLVING FOR PERFECTLY REACHABLE TARGETS. 
	 * DO NOT SET THIS TO 1 IF YOU ARE PORTING AND NOTICE INSTABILITY WHEN SOLVING FOR PERFECTLY REACHABLE TARGETS. 
	 * DO NOT SET THIS TO 1 IF YOU ARE PORTING AND NOTICE INSTABILITY WHEN SOLVING FOR PERFECTLY REACHABLE TARGETS. 
	 * DO NOT SET THIS TO 1 IF YOU ARE PORTING AND NOTICE INSTABILITY WHEN SOLVING FOR PERFECTLY REACHABLE TARGETS. 
	 * 
	 * You're encouraged to experiment with this parameter as per your use case, but
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
	
	/**
	 * currently unused, future feature for more natural posing
	 * 
	 * @param enabled
	 */
	public void setAbilityBiasing(boolean enabled) {
		abilityBiasing = enabled;
	}

	public boolean getAbilityBiasing() {
		return abilityBiasing;
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
		regenerateShadowSkeleton();
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