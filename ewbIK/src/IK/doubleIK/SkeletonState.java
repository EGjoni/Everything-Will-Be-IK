package IK.doubleIK;

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
 * @author Eron Gjoni
 *
 */
public class SkeletonState {
	
}
