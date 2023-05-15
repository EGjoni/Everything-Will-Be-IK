package IK.doubleIK.solver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

import IK.doubleIK.AbstractIKPin;
import IK.doubleIK.AbstractKusudama;
import IK.doubleIK.Constraint;
import math.doubleV.MathUtils;

/**
 * This class is used as an intermediary between the IK solver and whatever Armature system you happen to be using.
 * Upon initializing the solver, you must provide it with a prebuilt SkeletonState object which indicates
 * the transformations and parent child relationships between the bones in your armature.
 * 
 * The IKSolver will build its internal representation of the skeleton structure from this SkeletonState object.
 * Once the solver has finished solving, it will update the SkeletonState transforms with its solution, so that you
 * may read them back into your armature.
 * 
 * You are free to change the SkeletonState transforms before calling the solver again, and the solver will
 * operate beginning with the new transforms. 
 * 
 * IMPORTANT: you should NOT make any structural changes (where a structural change is defined as anything other 
 * than modifications to the transforms of a bone or constraint)  to the SkeletonState object after registering it with the solver.
 * If you make a structural  change, you MUST reregister the SkeletonState object with the solver. 
 * 
 * @author Eron Gjoni
 *
 */
public class SkeletonState {
	private ArrayList<BoneState> bonesList = new ArrayList<>();
	private ArrayList<TransformState> transformsList = new ArrayList<>();
	private ArrayList<TargetState> targetsList = new ArrayList<>();
	private ArrayList<ConstraintState> constraintsList = new ArrayList<>();
	private HashMap<String, BoneState> boneMap = new HashMap<>();
	private HashMap<String, TransformState> transformMap = new HashMap<>();
	private HashMap<String, TargetState> targetMap = new HashMap<>();
	private HashMap<String, ConstraintState>constraintMap = new HashMap<>();
	private BoneState[] bones = null;
	private TargetState[] targets = null;
	private ConstraintState[] constraints = null;
	private TransformState[] transforms = null;
	private BoneState rootBoneState = null;
	private boolean assumeValid = false;

	public SkeletonState() {
	}
	/**
	 * @param assumeValid If your usecase is such that you very frequently intend to change the structure of the skeleton, you can avoid the performance penalty
	 * of validation checks by providing `true` as an argument to this constructor. However, make sure you at least test your logic with this value set to its default of `false` 
	 * before aiming for optimizations
	 */
	public SkeletonState(boolean assumeValid) {
		this.assumeValid = assumeValid;
	}
	
	/**
	 * @return the array of TransformState objects. (full access instead of a getter for a crucial .000001% increase in speed)
	 */
	public TransformState[] getTransformsArray() {return this.transforms;}
	/**
	 * @return the array of BoneState objects. (full access instead of a getter for a crucial .000001% increase in speed)
	 */
	public BoneState getBoneState(int index ) {return this.bones[index];}
	public BoneState getBoneStateById(String id) {return this.boneMap.get(id);}
	public int getBoneCount() {return this.bones.length;}
	public TransformState getTransformState(int index ) {return this.transforms[index];}
	public int getTransformCount() {return this.transforms.length;}	
	public BoneState[] getBonesArray() {return this.bones;}
	public ConstraintState getConstraintState(int index) {return this.constraints[index];}
	public int getConstraintCount() {return this.constraints.length;}
	public TargetState getTargetState(int index) {return this.targets[index];}
	public int getTargetCount() {return this.targets.length;}

	public void validate() {
		for(BoneState bs : boneMap.values()) {
			if(bs.parent_id == null) {
				if(rootBoneState != null) {
					throw new RuntimeException("A skeleton may only have 1 root bone, you attempted to initialize bone of id `"+bs.id+"' as an implicit root (no parent bone),"
							+ "when bone with id '"+rootBoneState.id+"' is already determined as being the implicit root");
				}
				rootBoneState = bs;
			}
		}
		for(BoneState bs : boneMap.values()) {
			bs.validate();
			bonesList.add(bs);
		}
		for(TransformState ts : transformMap.values()) {
			ts.validate();
			transformsList.add(ts);
		}
		for(TargetState ts: targetMap.values()) {
			ts.validate();
			targetsList.add(ts); 
		}
		for(ConstraintState cs: constraintMap.values()) {
			cs.validate();
			constraintsList.add(cs);
		}		
		this.optimize();
		this.prune();
	}
	/**
	 * removes any bones/transforms/constraints that the solver would ignore, then 
	 * reindex and reconnects everything
	 */
	private void prune() {
		int leafCount = 1;
		while(leafCount > 0) {
			ArrayList<BoneState> leafBones = new ArrayList<>(); 
			for(BoneState bs : boneMap.values()) {
				if(bs.getTempChildCount() == 0 && bs.target_id == null) leafBones.add(bs);
			}
			leafCount = leafBones.size();
			for(BoneState leaf: leafBones) {
				BoneState currentLeaf =  leaf;
				while(currentLeaf != null && currentLeaf.target_id == null) {
					if(currentLeaf.getTempChildCount() == 0) {
						currentLeaf.prune();
						currentLeaf = currentLeaf.getParent();
					} else {
						break;
					}
				}
			}
		}
		this.optimize();
	}
	
	/**
	 * @param id a string by which to identify this bone
	 * @param transform_id the id string of the transform defining this bone's translation, rotation, and scale relative to its parent bone (or relative to the skeleton, if this is the root bone)
	 * @param parent_id null if this bone is a root bone, otherwise, the id string of this bone's parent (the bone's parent does not need to have been pre-registered with
	 * the SkeletonState, but will need to be eventually registered prior to calling SkeletonState.validate())
	 * @param constraint_id OPTIONAL id string of the cocnstraint on this bone, or null if no constraint
	 * @param stiffness aka friction. OPTIONAL Value from 1-0 indicating how slowly this bone moves. 1 means it never moves, 0 means it moves as much as the dampening parameter allows
	 * @param target_id OPTIONAL null if this bone is not an effector, otherwise, the string 
	 */
	public BoneState addBone(String id, String transform_id, String parent_id, String constraint_id, double stiffness, String target_id) {
		BoneState result = new BoneState(id, transform_id, parent_id, target_id, constraint_id, stiffness);
		boneMap.put(id, result);
		return result;
	}
	
	public BoneState addBone(String id, String transform_id, String parent_id, String target_id, String constraint_id) {
		BoneState result = new BoneState(id, transform_id, parent_id, target_id, constraint_id, 0.0);
		boneMap.put(id, result);
		return result;
	}
	/**
	 * @param id string by which to identify this constraint
	 * @param forBone_id the id string of the bone this constraint is constraining
	 * @param swingOrientationTransform_id
	 * @param twistOrientationTransform_id OPTIONAL only relevant for ball and socket-type region constraints (like Kusudamas, splines, limit cones, etc). 
	 * 			A transform specifying the orientation of the twist basis to be used in swing twist decomposition when constraining this bone.
	 * @param directReference REQUIRED, a reference to the actual Constraint instance so the solver can call its snapToLimitingAxes function.
	 */
	public ConstraintState addConstraint(String id, String forBone_id, double painfulness, String swingOrientationTransform_id, String twistOrientationTransform_id, Constraint directReference) {
		ConstraintState con = new ConstraintState(id, forBone_id, painfulness, swingOrientationTransform_id, twistOrientationTransform_id, directReference);
		constraintMap.put(id, con);
		return con;
	}
	public ConstraintState addConstraint(String id, String forBone_id, double painfulness, String swingOrientationTransform_id, Constraint directReference) {
		return this.addConstraint(id, forBone_id, painfulness, swingOrientationTransform_id, null, directReference);
	}
	/**
	 * @param id a string by which to identify this bone
	 * @param transform_id the id string of the transform defining this target's translation, rotation, and scale. 
	 * Note, this transform MUST be provided in a space relative to the skeleton transform. 
	 * The skeleton transform is treated as the identity by the solver. So if your actual target is defined as being in a space outside of the skeleton, 
	 * or is not a direct child of the skeleton transform, you must convert it into this space whenever updating the SkeletonState. 
	 * The solver never changes anything about the targets, so you need not worry about reading this value back from the SkeletonState, or converting its value
	 * back into your desired space.
	 * @param forBoneid the id string of the effector bone this is a target for
	 * @param priorities the orientation priorities for this target. For more information see the setTargetPriorities documentation in AbstractIKPin
	 * @param setDepthFalloff the depthFallOff for this target. value 0-1, with 0 indicating that no effectors 
	 * downstream of this once are visible to ancestors of this target's effector, and 1 indicating that all effectors 
	 * immediately downstream of this one are solved for with equal priority to this one by ancestors bones 
	 * of this target's effector. For more information, see the depthFallOff documentation in AbstractIKPin
	 * @param weight
	 */
	public TargetState addTarget(String id, String transform_id, String forBoneid, double[] priorities, double depthFalloff, double weight) {
		TargetState result = new TargetState(id, transform_id, forBoneid, priorities, depthFalloff, weight); 
		targetMap.put(id, result); 
		return result;
	}	
	public TargetState addTarget(String id, String transform_id, String forBoneid) {
		return this.addTarget(id, transform_id, forBoneid, new double[] {1.0, 1.0, 0.0}, 0.0, 1.0);
	}
	/**
	 * @param id a string by which to identify this transform
	 * @param translation an array of THREE numbers corresponding to the translation of this transform  in the space of its parent transform
	 * @param rotation an array of FOUR numbers corresponding to the rotation of this transform in the space of its parent bone's transform. 
	 * The four numbers should be a Hamilton quaternion representation (not JPL, important!), in the form [W, X, Y, Z], where W is the scalar quaternion component
	 * @param scale an array of THREE numbers corresponding to scale of the X, Y, and Z components of this transform. The convention is that a value of [1,1,1] indicates
	 * a right-handed transform, whereas something like [-1, 1, 1] would indicate a left-handed transform (with the x axis inverted relative to the parent x-axis)
	 * @param a string indicating the parent transform this transform is defined the in the space of, or null defined relative to the identity transform. 
	 * (the identity transform in this is defined as the parent of the root bone's transform so as to maximize numerical precision)
	 * @param directReference optional. To allow for efficiently referencing the actual  transform to which this TransformState corresponds without invoking a hash lookup,
	 * you can provide a reference to it here which you can read back
	 * */ 
	public void addTransform(String id, double[] translation, double[] rotation, double[] scale, String parent_id, Object directReference) {
		TransformState existingTransform = transformMap.get(id);
		if(existingTransform != null) {
			if(existingTransform.directReference == directReference) {
				existingTransform.update(translation, rotation, scale); 
			}
			else if(!Arrays.equals(existingTransform.translation, translation) 
					|| !Arrays.equals(existingTransform.rotation, rotation)
					|| !Arrays.equals(existingTransform.scale, scale)
					|| !parent_id.equals(existingTransform.parent_id)) {
				throw new RuntimeException("Transform with id '"+id+"' already exists and has contents which are not equivalent to the new transform being provided");
			}
		} else {
			TransformState transformState = new TransformState(id, translation, rotation, scale, parent_id, directReference);
			transformMap.put(id, transformState);
		}		
	}
	
	/**
	 * returns the rootmost BoneState of this SkeletonState
	 */
	public BoneState getRootBonestate() {
		return this.rootBoneState;
	}

	private void optimize() {
		//the following four lines create dense contiguous arrays by filtering out null elements in the corrsesponding source ArrayList
		bones = bonesList.stream().filter(Objects::nonNull).toArray(BoneState[]::new);
		transforms = transformsList.stream().filter(Objects::nonNull).toArray(TransformState[]::new);
		constraints = constraintsList.stream().filter(Objects::nonNull).toArray(ConstraintState[]::new);
		targets = targetsList.stream().filter(Objects::nonNull).toArray(TargetState[]::new);
		
		for(int i=0; i<bones.length; i++) bones[i].clearChildList();
		
		for(int i=0; i<bones.length; i++) {
			BoneState bs = bones[i];
			bs.setIndex(i);
		}
		for(int i=0; i<transforms.length; i++) {
			TransformState ts = transforms[i];
			ts.setIndex(i);
		}
		for(int i=0; i<targets.length; i++) {
			TargetState ts = targets[i];
			ts.setIndex(i);
		}
		for(int i=0; i<constraints.length; i++) {
			ConstraintState cs = constraints[i];
			cs.setIndex(i);
		}
		for(BoneState bs : bones) bs.optimize();
		for(TransformState ts : transforms) ts.optimize();
		for(TargetState ts : targets) ts.optimize();
		for(ConstraintState cs : constraints) cs.optimize();
	}


	public class BoneState {
		String id = null;
		String parent_id = null;
		String transform_id = null;
		String target_id = null;
		String constraint_id = null;
		private double stiffness = 0.0;
		private HashMap<String, Integer> childMap = new HashMap<>();
		private int index;
		private int parentIdx = -1;
		private int[] childIndices = null;
		private int transformIdx = -1;
		private int constraintIdx = -1;
		private int targetIdx = -1;

		private BoneState(String id, String transform_id, String parent_id, String target_id, String constraint_id,  double stiffness) {
			this.id = id;
			this.parent_id = parent_id;
			this.transform_id  = transform_id;
			this.target_id = target_id; 
			this.constraint_id = constraint_id; 
			this.stiffness = stiffness;
		}		
		public TransformState getTransform() {
			return transforms[this.transformIdx];
		}
		public TargetState getTarget() {
			if(this.targetIdx ==-1) return null;
			else return targets[this.targetIdx];
		}
		public double getStiffness() {
			return stiffness;
		}
		public BoneState getParent() {
			if(this.parentIdx >= 0)
				return bones[this.parentIdx];
			return null;
		}
		public BoneState getChild(String id) {
			return bones[this.childMap.get(id)];
		}
		public BoneState getChild(int index) {
			return bones[this.childIndices[index]];
		}
		public void clearChildList() {
			this.childMap.clear();
			this.childIndices = new int[] {};
		}
		public int getChildCount() {
			return childIndices.length;
		}
		
		private int getTempChildCount() {
			return this.childMap.values().size();
		}
		public ConstraintState getConstraint() {
			if(this.constraintIdx >= 0)
				return constraints[this.constraintIdx];
			return null;
		}
		
		private void prune() {
			bonesList.set(this.index, null);
			boneMap.remove(this.id);
			this.getTransform().prune();
			if(this.getConstraint() != null) this.getConstraint().prune();
			if(this.parent_id != null) {
				boneMap.get(this.parent_id).childMap.remove(this.id);
			}
			if(rootBoneState == this)
				rootBoneState = null;
		}		
		private void setIndex(int index) {
			this.index = index;
			if(this.parent_id != null) {
				BoneState parentBone = boneMap.get(this.parent_id);
				parentBone.addChild(this.id, this.index);
			}
		}
		private void  addChild(String id, int childIndex) {
			this.childMap.put(id, childIndex);
		}
		private void optimize() {
			Integer[] tempChildren = childMap.values().toArray(new Integer[0]);
			this.childIndices = new int[tempChildren.length];
			int j = 0;
			for(int i=0; i<tempChildren.length; i++) childIndices[i] = tempChildren[i];
			if(this.parent_id != null)
				this.parentIdx = boneMap.get(this.parent_id).index;
			this.transformIdx = transformMap.get(this.transform_id).getIndex();
			if(this.constraint_id != null)
				this.constraintIdx = constraintMap.get(this.constraint_id).getIndex();
			if(this.target_id != null)
				this.targetIdx = targetMap.get(this.target_id).getIndex();
		}

		private void validate() {
			if(assumeValid) return;
			TransformState transform = transformMap.get(this.transform_id); 
			if(transform == null) //check that the bone has a transform
				throw new RuntimeException("Bone '"+this.id+"' references transform with id '"+this.transform_id+"', but '"+this.transform_id+"' has not been registered with the SkeletonState.");
			if(this.parent_id != null) { //if this isn't a root bone, ensure the following
				BoneState parent = boneMap.get(this.parent_id);
				if(parent == null) //check that the bone listed as its parent has been registered.
					throw new RuntimeException("Bone '"+this.id+"' references parent bone with id '"+this.parent_id+"', but '"+this.parent_id+"' has not been registered with the SkeletonState.");
				TransformState parentBonesTransform = transformMap.get(parent.transform_id);
				TransformState transformsParent = transformMap.get(transform.parent_id);
				if(parentBonesTransform != transformsParent) { //check that the parent transform of this bones transform 
					// is the same as the transform as the ransform of the bone's parent
					throw new RuntimeException("Bone '"+this.id+"' has listed bone with id '"+this.parent_id+"' as its parent, which has a transform_id of '"+parent.transform_id+
							"' but the parent transform of this bone's transform is listed as "+transform.parent_id+"'. A bone's transform must have the parent bone's transform as its parent");
				}
				//avoid grandfather paradoxes
				BoneState ancestor = parent;
				while(ancestor != null) {
					if(ancestor == this) {
						throw new RuntimeException("Bone '"+this.id+"' is listed as being both a descendant and an ancestor of itself.");
					}
					if(ancestor.parent_id == null) 
						ancestor = null;
					else {
						BoneState curr = ancestor;
						ancestor = boneMap.get(ancestor.parent_id);
						if(ancestor == null) {
							throw new RuntimeException("bone with id `"+curr.id+"` lists its parent bone as having id `"+curr.parent_id+"', but no such bone has been registered with the SkeletonState");  
						}
					}
				}
			} else {
				if(this.constraint_id != null) {
					throw new RuntimeException("Bone '"+this.id+"' has been determined to be a root bone. However, root bones may not be constrained."
							+ "If you wish to constrain the root bone anyway, please insert a fake unconstrained root bone prior to this bone. Give that bone's transform values equal to this bone's, and set this "
							+ "bone's transformsto identity. ");
				}
			}	      		
			if(this.constraint_id != null) { //if this bone has a constraint, ensure the following:
				ConstraintState constraint = constraintMap.get(this.constraint_id) ;
				if(constraint == null)//check that the constraint has been registered
					throw new RuntimeException("Bone '"+this.id+"' claims to be constrained by '"+this.constraint_id+"', but no such constraint has been registered with this SkeletonState");
				if(!constraint.forBone_id.equals(this.id)) {
					throw new RuntimeException("Bone '"+this.id+"' claims to be constrained by '"+constraint.id+"', but constraint of id '"+constraint.id+"' claims to be constraining bone with id '"+constraint.forBone_id+"'");
				}
			}
		}
		public int getIndex() {
			return index;
		}
		public String getIdString() {
			return this.id;
		}
		public void setStiffness(double stiffness) {
			this.stiffness = stiffness;
		}
		
	}	

	public class TransformState {
		String id;
		public double[] translation;
		public double[] rotation;
		public double[] scale;
		String parent_id;
		Object directReference = null;
		private int index;
		private int parentIdx = -1;
		private int[] childIndices = null;
		private ArrayList<Integer> childIdxsList = new ArrayList<>();
		
		private TransformState(String id, double[] translation, double[] rotation, double[] scale, String parent_id, Object directReference) {
			this.id = id;
			this.translation = translation;
			this.rotation = rotation;
			this.scale = scale;
			this.parent_id = parent_id;
			this.directReference = directReference;
		}
		public void update(double[] translation, double[] rotation, double[] scale) {
			this.translation = translation;
			this.rotation = rotation;
			this.scale = scale;			
		}
		
		public void prune() {
			transformsList.set(this.index, null);
			transformMap.remove(this.id);
		}
		public int getIndex() {
			return this.index;
		}
		public int getParentIndex() {
			return this.parentIdx;
		}
		public TransformState getParentTransform() {
			return transforms[this.parentIdx];
		}
		private void setIndex(int index) {
			this.index = index;
			TransformState parTransform = transformMap.get(this.parent_id);
			if(parTransform != null) {
				parTransform.addChild(this.index);
			}
		}
		public void  addChild(int childIndex) {
			this.childIdxsList.add(childIndex);
		}
		public void optimize() {
			this.childIndices = new int[childIdxsList.size()];
			for(int i=0; i<childIdxsList.size(); i++) childIndices[i] = childIdxsList.get(i);
			TransformState ax = transformMap.get(this.parent_id);
			if(this.parent_id != null)
				this.parentIdx = transformMap.get(this.parent_id).getIndex();
		}
		public void validate() {
			if(assumeValid) return;
			if(this.parent_id == null) return;

			TransformState parent = transformMap.get(parent_id);
			if(parent == null)
				throw new RuntimeException("Transform '"+this.id+"' lists '" +this.parent_id+"' as its parent, but no transform with id '"+this.parent_id+"' has been registered with this SkeletonState"); 
			TransformState ancestor = parent;
			while(ancestor != null) {
				if(ancestor == this) {
					throw new RuntimeException("Transform '"+this.id+"' is listed as being both a descendant and an ancestor of itself.");
				}
				if(ancestor.parent_id == null) 
					ancestor = null;
				else {
					TransformState curr = ancestor;
					ancestor = transformMap.get(ancestor.parent_id);
					if(ancestor == null) {
						throw new RuntimeException("Transform with id `"+curr.id+"` lists its parent transform as having id `"+curr.parent_id+"', but no such transform has been registered with the SkeletonState");  
					}
				}
			}
		}
		public String getIdString() {
			return this.id;
		}
	}

	public class TargetState {		
		public static final int XDir = 0, YDir=2, ZDir=4;
		String id;
		String transform_id;
		String forBone_id;
		byte modeCode;
		double[] priorities;
		double depthFalloff;
		double weight;
		int index;
		int transformIdx;
		int forBoneIdx;

		private TargetState(String id, String transform_id, String forBoneid, double[] priorities, double depthFalloff, double weight) {
			this.init(id, transform_id, forBoneid, priorities, depthFalloff, weight);
		}
		private void init(String id, String transform_id, String forBoneid, double[] priorities, double depthFalloff, double weight) {
			this.id = id;
			this.forBone_id= forBoneid;
			this.transform_id = transform_id;
			this.modeCode = 0;
			this.priorities = priorities;
			this.depthFalloff = depthFalloff;
			this.weight = weight;
			boolean xDir = this.priorities[0] > 0 ? true : false;
			boolean yDir = this.priorities[1]> 0 ? true : false;
			boolean zDir = this.priorities[2] > 0 ? true : false;
			modeCode =0; 
			if(xDir) modeCode += 1; 
			if(yDir) modeCode += 2; 
			if(zDir) modeCode += 4;  
		}

		private void setIndex(int index) {
			this.index = index;
		}
		private void optimize() {
			this.forBoneIdx = boneMap.get(this.forBone_id).getIndex();
			this.transformIdx = transformMap.get(this.transform_id).getIndex();
		}
		public TransformState getTransform() {
			return transforms[this.transformIdx];
		}
		private void validate() {
			TransformState transform = transformMap.get(this.transform_id);
			if(transform == null)
				throw new RuntimeException("Target with id '"+this.id+"' lists its transform as having id '"+this.transform_id+"', but no such transform has been registered with this StateSkeleton");
			if(transform.parent_id != null)
				throw new RuntimeException("Target with id '"+this.id+"' lists its transform as having a parent transform. "
						+ "However, target transforms are not allowed to have a parent, as they must be given in the space of the skeleton transform. "
						+ "Please provide a transform object that has been converted into skeleton space and has no parent.");
		}
		public String getIdString() {
			return this.id;
		}
		public byte getModeCode() {
			return this.modeCode;
		}
		public double getDepthFallOff() {
			return this.depthFalloff;
		}
		public double getWeight() {
			return this.weight;
		}
		
		public int getIndex() {
			return this.index;
		}
		
		/**
		 * @param basisDirection
		 * @return  the priority of the requested AbstractIKPin.XDir, AbstractIKPin.YDir, or AbstractIKPin.ZDir
		 */
		public double getPriority(int basisDirection) {
			return priorities[basisDirection/2];
		}
		
		/**@return the priority of whichever direction has the largest priority.*/ 
		public double getMaxPriority() {
			double maxPinWeight = 0;
			if ((modeCode & AbstractIKPin.XDir) != 0)
				maxPinWeight = MathUtils.max(maxPinWeight, this.getPriority(AbstractIKPin.XDir));
			if ((modeCode & AbstractIKPin.YDir) != 0)
				maxPinWeight = MathUtils.max(maxPinWeight, this.getPriority(AbstractIKPin.YDir));
			if ((modeCode & AbstractIKPin.ZDir) != 0)
				maxPinWeight = MathUtils.max(maxPinWeight, this.getPriority(AbstractIKPin.ZDir));
			return maxPinWeight;
		}
		
	}

	public class ConstraintState {
		String id = null;
		String forBone_id = null;
		String swingOrientationTransform_id = null;
		String twistOrientationTransform_id = null;
		Constraint directReference;
		private int index;
		private int swingTranform_idx = -1;
		private int twistTransform_idx = -1;
		public double painfulness = 0;

		private ConstraintState(String id, String forBone_id, double painfulness, String swingOrientationTransform_id, String twistOrientationTransform_id, Constraint directReference) {
			this.forBone_id = forBone_id;
			this.swingOrientationTransform_id = swingOrientationTransform_id;
			this.twistOrientationTransform_id = twistOrientationTransform_id;
			this.directReference = directReference;
			this.painfulness = painfulness;
		}
		public void prune() {
			if(this.getTwistTransform() != null) this.getTwistTransform().prune();
			this.getSwingTransform().prune();
			constraintMap.remove(this.id);
			constraintsList.set(this.index, null);
		}
		public TransformState getSwingTransform() {
			return transforms[this.swingTranform_idx];
		}
		public TransformState getTwistTransform() {
			if(this.twistTransform_idx == -1) return null;
			return transforms[this.twistTransform_idx];
		}		
		private void setIndex(int index) {
			this.index = index;
		}
		public int getIndex() {
			return this.index;
		}
		
		public double getPainfulness() {
			return this.painfulness;
		}
		private void optimize() {
			if(this.twistOrientationTransform_id != null) {
				TransformState twistTransform = transformMap.get(this.twistOrientationTransform_id);
				this.twistTransform_idx = twistTransform.getIndex();
			}
			TransformState swingTranform  = transformMap.get(this.swingOrientationTransform_id); 
			this.swingTranform_idx = swingTranform.getIndex();
		}
		private void validate() {
			if(assumeValid) return;
			BoneState forBone = boneMap.get(this.forBone_id) ;
			if(forBone == null)//check that the constraint has been registered
				throw new RuntimeException("Constraint '"+this.id+"' claims to constrain bone '"+forBone_id+"', but no such bone has been registered with this SkeletonState");
			if(this.swingOrientationTransform_id == null) {
				throw new RuntimeException("Constraint with id '"+this.id+"' claims to have no swing transform, but this transform is required. "
						+"You may provide an identity transform if you wish to indicate that the constraint's swing space is equivalent to the parent bone's default space"); 
			}
			TransformState constraintSwing = transformMap.get(this.swingOrientationTransform_id);
			if(constraintSwing == null) {
				throw new RuntimeException("Constraint with id '"+this.id+"' claims to have a swingOrientationTransform with id'"+this.swingOrientationTransform_id+"', but no such transform has been registered with this SkeletonState'");
			}
			if(this.twistOrientationTransform_id != null)  {
				TransformState constraintTwist = transformMap.get(this.twistOrientationTransform_id);
				if(constraintTwist == null) {
					throw new RuntimeException("Constraint with id '"+this.id+"' claims to have a twist transform with id'"+this.twistOrientationTransform_id+"', but no such transform has been registered with this SkeletonState'");
				}
			}
		}
		public String getIdString() {
			return this.id;
		}
		public Constraint getDirectReference() {
			return this.directReference;
		}
	}
}
