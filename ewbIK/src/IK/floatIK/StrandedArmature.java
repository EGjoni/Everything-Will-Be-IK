package IK.floatIK;

import java.util.*;

import sceneGraph.*;
import sceneGraph.math.floatV.AbstractAxes;
import sceneGraph.math.floatV.MRotation;
import sceneGraph.math.floatV.Quaternionf;
import sceneGraph.math.floatV.Rot;
import sceneGraph.math.floatV.SGVec_3f;

public class StrandedArmature
{

	/*
	 * Note: This code is doing a lot of stuff conceptually in relatively few lines 
	 * and might be difficult to wrap your head around.  I've opted to make it concise, 
	 * because making it verbose did not make it any easier to digest. If you're going to edit it,
	 * you might want to take some time to graph things out.
	 */
	public ArrayList<Strand> strands;
	public ArrayList<StrandedArmature> childCollections;    

	public ArrayList<AbstractBone> allBonesInStrandCollection;
	public HashMap<AbstractBone, ArrayList<Strand>> boneStrandMap;
	public HashMap<AbstractBone, Float> angleDeltaMap;
	public HashMap<AbstractBone, AbstractAxes> averageSimulatedAxes;
	Float lastLargestAngleDelta;
	Float amountIncreasing;
	Float amountDecreasing;
	int increasingSampleCount;
	int decreasingSampleCount;
	protected StrandedArmature parentStrandedAmature;
	public Strand parentStrand;
	public float totalPinDist;
	public int distanceToRoot;
	public int chainLength;
	boolean includeInIK;
	private boolean basePinned;
	AbstractBone strandRoot;
	AbstractAxes simulatedStrandRootParentAxes;



	public StrandedArmature( AbstractBone rootBone) {
		this.strands = new ArrayList<Strand>();
		this.childCollections = new ArrayList<StrandedArmature>();
		this.allBonesInStrandCollection = new ArrayList<AbstractBone>();
		this.boneStrandMap = new HashMap<AbstractBone, ArrayList<Strand>>();
		this.angleDeltaMap = new HashMap<AbstractBone, Float>();
		this.averageSimulatedAxes = new HashMap<AbstractBone, AbstractAxes>();
		this.lastLargestAngleDelta = null;
		this.amountIncreasing = 0f;
		this.amountDecreasing = 0f;
		this.increasingSampleCount = 0;
		this.decreasingSampleCount = 0;
		this.parentStrandedAmature = null;
		this.parentStrand = null;
		this.totalPinDist = 0;
		this.distanceToRoot = 0;
		this.chainLength = 0;
		this.includeInIK = true;
		this.setBasePinned(false);
		this.simulatedStrandRootParentAxes = null;
		this.strandRoot = this.armatureRootBone(rootBone);
		this.allBonesInStrandCollection.add(this.strandRoot);
		this.generateStrandHierarchy();
		sortBoneList();
	}

	public StrandedArmature( Strand inputParentStrand,  AbstractBone inputStrandRoot) {
		this.strands = new ArrayList<Strand>();
		this.childCollections = new ArrayList<StrandedArmature>();
		this.allBonesInStrandCollection = new ArrayList<AbstractBone>();
		this.boneStrandMap = new HashMap<AbstractBone, ArrayList<Strand>>();
		this.angleDeltaMap = new HashMap<AbstractBone, Float>();
		this.averageSimulatedAxes = new HashMap<AbstractBone, AbstractAxes>();
		this.lastLargestAngleDelta = null;
		this.amountIncreasing = 0f;
		this.amountDecreasing = 0f;
		this.increasingSampleCount = 0;
		this.decreasingSampleCount = 0;
		this.parentStrandedAmature = null;
		this.parentStrand = null;
		this.totalPinDist = 0;
		this.distanceToRoot = 0;
		this.chainLength = 0;
		this.includeInIK = true;
		this.setBasePinned(false);
		this.simulatedStrandRootParentAxes = null;
		this.strandRoot = inputStrandRoot;
		this.parentStrandedAmature = inputParentStrand.parentStrandCollection;
		this.parentStrand = inputParentStrand;
		this.distanceToRoot = this.parentStrandedAmature.distanceToRoot + 1;
		this.setBasePinned((inputParentStrand != null && inputParentStrand.getStrandTip().isPinned()));
		this.allBonesInStrandCollection.add(this.strandRoot);
		this.updateSimulatedStrandRootParentAxes();
		this.generateStrandHierarchy();
		sortBoneList();
	}


	/*
	 * is always sorted such that the outermost bones have the lowest index, this is 
	 * useful for error weighting when negotiating multiple effectors
	 */
	public void sortBoneList() {
		ArrayList<AbstractBone> tempList = (ArrayList<AbstractBone>) allBonesInStrandCollection.clone();
		tempList.sort((AbstractBone a, AbstractBone b) -> {
			return a.ancestorCount - b.ancestorCount;
		});
	}

	public void generateStrandHierarchy() {
		this.childCollections.clear();
		ArrayList<AbstractBone> pinnedTips = this.strandRoot.getMostImmediatelyPinnedDescendants();
		System.out.println("attempting stranndgen");
		// this.ensureAverageSimulatedBoneHeirarchy();
		for ( AbstractBone b : pinnedTips) {
			try {
				Strand s = new Strand(this, b, this.strandRoot);
				this.strands.add(s);
			}
			catch (Exception disconnectedBoneException) {
				System.out.println(disconnectedBoneException);
				disconnectedBoneException.printStackTrace(System.out);
			}
		}
		this.ensureAverageSimulatedBoneHeirarchy();

		for (Strand s : strands) {
			try {
				this.childCollections.addAll(s.generateChildStrandCollection());
			}
			catch (Exception disconnectedBoneException) {
				System.out.println(disconnectedBoneException);
			}
		}

	}

	private void ensureAverageSimulatedBoneHeirarchy() {
		for ( AbstractBone b : this.allBonesInStrandCollection) {
			if (this.averageSimulatedAxes.get(b) == null) {
				AbstractAxes simulatedAvgAxes = b.localAxes().getGlobalCopy();
				this.averageSimulatedAxes.put(b, simulatedAvgAxes);
			}
			else {
				this.averageSimulatedAxes.get(b).emancipate();
				this.averageSimulatedAxes.get(b).alignGlobalsTo(b.localAxes());
			}
		}
		for ( AbstractBone b : this.allBonesInStrandCollection) {
			if (this.averageSimulatedAxes.get(b.getParent()) != null) {
				this.averageSimulatedAxes.get(b).setParent((AbstractAxes)this.averageSimulatedAxes.get(b.getParent()));
			}
			else {
				this.averageSimulatedAxes.get(b).setParent(this.simulatedStrandRootParentAxes);
			}
		}
	}

	public void alignSimulationAxesToBones() {
		this.updateSimulatedStrandRootParentAxes();
		this.ensureAverageSimulatedBoneHeirarchy();
		for ( Strand s : this.strands) {
			s.alignSimulationAxesToBones();
		}
		for ( StrandedArmature cc : this.childCollections) {
			cc.alignSimulationAxesToBones();
		}
	}

	public void alignBonesToSimulationAxes() {
		this.strandRoot.localAxes().localMBasis.adoptValues(this.averageSimulatedAxes.get(this.strandRoot).localMBasis);

		this.strandRoot.localAxes().markDirty();
		this.strandRoot.localAxes().updateGlobal();
		for ( AbstractBone b : this.allBonesInStrandCollection) {

			AbstractAxes boneAxes =   b.localAxes();
			AbstractAxes alignTo = this.averageSimulatedAxes.get(b);
			boneAxes.localMBasis.adoptValues(alignTo.localMBasis);
			boneAxes.markDirty();
			boneAxes.updateGlobal();
		}
		for ( StrandedArmature cc : this.childCollections) {
			cc.alignBonesToSimulationAxes();
		}

	}

	public void updateSimulatedStrandRootParentAxes() {
		if (this.strandRoot.localAxes().getParentAxes() != null) {
			if (this.simulatedStrandRootParentAxes == null) {
				if(this.parentStrandedAmature != null) {
					this.simulatedStrandRootParentAxes =  this.parentStrandedAmature.averageSimulatedAxes.get(this.strandRoot.getParent());
				} else {
					this.simulatedStrandRootParentAxes = this.strandRoot.localAxes().getParentAxes().getGlobalCopy();
				}
				// this.simulatedStrandRootParentAxes.al
			}
			else {
				/* ArrayList<AxisDependancy> dependents = (ArrayList<AxisDependancy>)this.simulatedStrandRootParentAxes.dependentsRegistry.clone();
                for ( AxisDependancy ad : dependents) {
                    ad.emancipate();
                }*/
				if (this.parentStrandedAmature == null) {
					this.simulatedStrandRootParentAxes.alignGlobalsTo(this.strandRoot.localAxes().getParentAxes());
					this.simulatedStrandRootParentAxes.updateGlobal();
				}
				else {
					AbstractAxes parentAverage  = (AbstractAxes)this.parentStrand.parentStrandCollection.averageSimulatedAxes.get(this.parentStrand.getStrandTip()); 
					if(this.simulatedStrandRootParentAxes != parentAverage)
						this.simulatedStrandRootParentAxes.alignGlobalsTo(parentAverage);
					this.simulatedStrandRootParentAxes.updateGlobal();
				}
				/*for ( AxisDependancy ad : dependents) {
                    ((AbstractAxes)ad).setParent(this.simulatedStrandRootParentAxes);
                }*/
			}
		}
	}

	public StrandedArmature getStrandCollectionFor( AbstractBone bone) {
		if (this.allBonesInStrandCollection.contains(bone)) {
			return this;
		}
		StrandedArmature result = null;
		for ( StrandedArmature sa : this.childCollections) {
			result = sa.getStrandCollectionFor(bone);
			if (result != null) {
				return result;
			}
		}
		return result;
	}

	public void resetStabilityMeasures() {
		this.lastLargestAngleDelta = null;
		this.amountIncreasing = 0f;
		this.amountDecreasing = 0f;
		this.increasingSampleCount = 0;
		this.decreasingSampleCount = 0;
	}

	public void updateStabilityEstimates() {
		float thisLargestDelta = this.getLargestAngleChange();
		if (this.lastLargestAngleDelta != null) {
			float change = thisLargestDelta - this.lastLargestAngleDelta;
			if (change < 0) {
				this.amountDecreasing += -1 * change;
				++this.increasingSampleCount;
			}
			else {
				this.amountIncreasing += change;
				++this.decreasingSampleCount;
			}
		}
		this.lastLargestAngleDelta = thisLargestDelta;
	}

	private float getLargestAngleChange() {
		float largestDelta = 0;
		for ( AbstractBone b : this.allBonesInStrandCollection) {
			Float delta = Math.abs(this.angleDeltaMap.get(b));
			if (delta != null && delta > largestDelta) {
				largestDelta = delta;
			}
		}
		return largestDelta;
	}

	public void setDeltaMeasureForBone( AbstractBone b,  float angle) {
		this.angleDeltaMap.put(b, angle);
	}

	public float getStability() {
		if (this.increasingSampleCount == 0) {
			return 0;
		}
		if (this.decreasingSampleCount == 0) {
			return 1;
		}
		return this.increasingSampleCount / (this.increasingSampleCount + this.decreasingSampleCount);
	}

	public void averageSimulatedAxesOrientations() {
	}

	public void updateTotalPinDist() {
		this.totalPinDist = 0;
		for ( Strand s : this.strands) {
			this.totalPinDist += s.getStrandTip().getTip_().dist(s.getStrandTip().getPinPosition());
		}
	}

	public void updateStrandedArmature() {
		this.strands.clear();
		this.childCollections.clear();
		this.allBonesInStrandCollection.clear();
		this.boneStrandMap.clear();
		this.generateStrandHierarchy();
	}

	public void getInnerMostStrands( ArrayList<Strand> addTo) {
		if (this.parentStrandedAmature != null) {
			this.parentStrandedAmature.getInnerMostStrands(addTo);
		}
		else if (!this.isBasePinned()) {
			addTo.addAll(this.strands);
		}
	}

	public AbstractBone armatureRootBone( AbstractBone rootBone2) {
		AbstractBone rootBone3;
		for (rootBone3 = rootBone2; rootBone3.getParent() != null; rootBone3 = rootBone3.getParent()) {}
		return rootBone3;
	}

	public void translateToAverageTipError( boolean onlyIfUnpinned) {
		if (!onlyIfUnpinned || !this.isBasePinned()) {
			SGVec_3f totalDiff = new SGVec_3f(0, 0, 0);
			AbstractAxes translationAxes = this.averageSimulatedAxes.get(this.strandRoot);
			AbstractAxes tipBoneAxes = null;
			AbstractBone tipBone = null;
			SGVec_3f target = null;
			SGVec_3f tracer = null;
			for ( Strand s : this.strands) {
				tipBone = s.getStrandTip();
				tipBoneAxes = this.averageSimulatedAxes.get(tipBone);
				target = tipBone.getPinnedAxes().origin_().copy();
				tracer = tipBoneAxes.origin_().copy();
				totalDiff.add(SGVec_3f.sub(target, tracer));
			}
			float count = this.strands.size();
			SGVec_3f averageDiff = SGVec_3f.div(totalDiff, count);
			// IKVector baseExpected = IKVector.add(averageDiff, translationAxes.origin());
			//   IKVector tracerExpected = IKVector.add(tipBoneAxes.getGlobalOf(new IKVector(0, tipBone.getBoneHeight() / 2, 0.0)), averageDiff);
			translationAxes.translateByGlobal(averageDiff);
			// IKVector baseGot = translationAxes.origin();
			// IKVector tracerGot = tipBoneAxes.getGlobalOf(new IKVector(0.0, tipBone.getBoneHeight() / 2.0, 0.0));
			// IKVector targetAfter = tipBone.getPinnedAxes().origin().copy();
			//IKVector tracerAfter = tipBoneAxes.getGlobalOf(new IKVector(0.0, tipBone.getBoneHeight() / 2.0, 0.0));
			//IKVector tracerDiff = IKVector.sub(targetAfter, tracerAfter);
			// System.out.println("tracerDiff: " + averageDiff.toPVec());
			// System.out.println("tip should be: " + tipBoneAxes.getGlobalOf(new IKVector(0.0, tipBone.getBoneHeight(), 0.0)));
		}
	}

	/**
	 * @return the NLerped average rotation of all target Axes for all Strand of which input bone is a member;
	 */
	public Rot getAverageTargetOrientationAcrossAllStrandsForBone(
			AbstractBone forBone,
			boolean importanceWeighting) {
		float q0Sum = 0f, q1Sum = 0f, q2Sum= 0f, q3Sum =0f;
		float totalWeight = 0f;
		ArrayList<Strand> strandsForBone = boneStrandMap.get(forBone); 
		averageSimulatedAxes.get(forBone).updateGlobal();
		Rot thisBoneAxes = averageSimulatedAxes.get(forBone).globalMBasis.rotation;
		Quaternionf thisBoneQ = G.getQuaternionf(thisBoneAxes);
		for(Strand s : strandsForBone) {
			s.getStrandTip().getPinnedAxes().updateGlobal();
			Rot targetAxes = s.getStrandTip().getPinnedAxes().globalMBasis.rotation;
			float weight = s.getStrandTip().getIKPin().getPinWeight();
			totalWeight += weight; 			
			//since the bone we'll ultimately be rotating is the input bone, we probably want to keep its tipAxes rotations in the same neighborhood
			/**
			 * TODO: float check that this isn't idiotic.
			 */
			Quaternionf repairedAxesTargetQ = G.getSingleCoveredQuaternionf(G.getQuaternionf(targetAxes), thisBoneQ);
			q0Sum = repairedAxesTargetQ.getQ0()*weight; 
			q1Sum = repairedAxesTargetQ.getQ1()*weight; 
			q2Sum = repairedAxesTargetQ.getQ2()*weight; 
			q3Sum = repairedAxesTargetQ.getQ3()*weight;
		}
		Rot result = new Rot( 
				q0Sum/totalWeight, 
				q1Sum/totalWeight, 
				q2Sum/totalWeight, 
				q3Sum/totalWeight,
				true);    	
		return result;
	}
	
	public SGVec_3f getAverageTargetOriginAcrossAllStrandsForBone(
			AbstractBone forBone,
			boolean importanceWeighting) {
		SGVec_3f result = new SGVec_3f();
		float totalWeight = 0f;
		ArrayList<Strand> strandsForBone = boneStrandMap.get(forBone); 		
		for(Strand s : strandsForBone) {
			SGVec_3f origin = s.getStrandTip().getIKPin().getAxes().origin_();
			float weight = s.getStrandTip().getIKPin().getPinWeight();
			totalWeight += weight;
			result.x += origin.x*weight; 
			result.y += origin.y*weight;
			result.z += origin.z*weight;
		}
		result.div(totalWeight);
		return result;
	}
	
	/**
	 * @param pass the pass number to use from the rotationAxes list.
	 * @return the NLerped average rotation of all target Axes for all Strand of which input bone is a member;
	 */
	public Rot getAverageTipOrientationAcrossAllStrandsForBone(
			AbstractBone forBone,
			int pass, 
			boolean importanceWeighting) {
		float q0Sum = 0f, q1Sum = 0f, q2Sum= 0f, q3Sum =0f;
		float totalWeight = 0f;
		ArrayList<Strand> strandsForBone = boneStrandMap.get(forBone); 
		averageSimulatedAxes.get(forBone).updateGlobal();
		Rot thisBoneAxes = averageSimulatedAxes.get(forBone).globalMBasis.rotation;
		Quaternionf thisBoneQ = G.getQuaternionf(thisBoneAxes);
		for(Strand s : strandsForBone) {
			s.simulatedLocalAxes.get(s.getStrandTip())[pass].updateGlobal();
			Rot tipAxes = s.simulatedLocalAxes.get(s.getStrandTip())[pass].globalMBasis.rotation;//.getPinnedAxes().globalMBasis.rotation;
			float weight = s.getStrandTip().getIKPin().getPinWeight();
			totalWeight += weight; 
			//since the bone we'll ultimately be rotating is the input bone, we probably want to keep its tipAxes rotations in the same neighborhood
			/**
			 * TODO: float check that this isn't idiotic.
			 */
			Quaternionf repairedAxesTipQ = G.getSingleCoveredQuaternionf(G.getQuaternionf(tipAxes), thisBoneQ);
			q0Sum = repairedAxesTipQ.getQ0()*weight; 
			q1Sum = repairedAxesTipQ.getQ1()*weight; 
			q2Sum = repairedAxesTipQ.getQ2()*weight; 
			q3Sum = repairedAxesTipQ.getQ3()*weight;
		}
		Rot result = new Rot( 
				q0Sum/totalWeight, 
				q1Sum/totalWeight, 
				q2Sum/totalWeight, 
				q3Sum/totalWeight,
				true);    	
		return result;
	}
	
	public SGVec_3f getAverageTipOriginAcrossAllStrandsForBone(
			AbstractBone forBone,
			int pass, 
			boolean importanceWeighting) {
		SGVec_3f result = new SGVec_3f();
		float totalWeight = 0f;
		ArrayList<Strand> strandsForBone = boneStrandMap.get(forBone); 		
		for(Strand s : strandsForBone) {
			SGVec_3f origin = s.simulatedLocalAxes.get(s.getStrandTip())[pass].origin_();			
			float weight = s.getStrandTip().getIKPin().getPinWeight();
			totalWeight += weight;
			result.x += origin.x*weight; 
			result.y += origin.y*weight;
			result.z += origin.z*weight;
		}
		result.div(totalWeight);
		return result;
	}

	public boolean isBasePinned() {
		return basePinned;
	}

	public void setBasePinned(boolean basePinned) {
		this.basePinned = basePinned;
	}

	public class DisconnectedBoneException extends Exception
	{
		public DisconnectedBoneException( String message) {
			super("Error: BoneExample is not connected to armature. Ignoring strand");
		}
	}

	public class Strand
	{
		public static final int SimAxesPerBone = 1;
		//AbstractAxes strandTipTracerAxes;
		private AbstractBone strandTip;
		private AbstractBone strandRoot;
		public boolean rootParentPinned;
		StrandedArmature parentStrandCollection;
		public ArrayList<AbstractBone> bones;
		public HashMap<AbstractBone, Rot> rotationsMap;
		public  HashMap<AbstractBone, Rot> tempRotationsMap;
		public HashMap<AbstractBone, AbstractAxes[]> simulatedLocalAxes;
		public HashMap<AbstractBone, AbstractAxes[]> simulatedConstraintAxes;
		public HashMap<AbstractBone, Float> remainingFreedomAtBoneMap ; 
		float distToTarget;
		AbstractAxes[] strandTempAxes;

		public Strand( StrandedArmature par,  AbstractBone tip,  AbstractBone root) throws DisconnectedBoneException {
			this.rootParentPinned = true;
			this.bones = new ArrayList<AbstractBone>();
			this.rotationsMap = new HashMap<AbstractBone, Rot>();
			this.tempRotationsMap = new HashMap<AbstractBone, Rot>();
			this.simulatedLocalAxes = new HashMap<AbstractBone, AbstractAxes[]>();
			this.simulatedConstraintAxes = new HashMap<AbstractBone, AbstractAxes[]>();
			this.remainingFreedomAtBoneMap = new HashMap<>();
			this.distToTarget = 0f;
			this.parentStrandCollection = par;
			this.setStrandTip(tip);
			this.setStrandRoot(root);
			if (this.getStrandRoot().getParent() != null && this.getStrandRoot().getParent().isPinned()) {
				this.rootParentPinned = true;
			}
			else {
				this.rootParentPinned = false;
			}
			this.populateStrandBonesData();
			//  for(int i= 0; i<strandTempAxes.length; i++)
			//this.strandTempAxes[i] = this.simulatedLocalAxes.get(this.getStrandTip())[i].getGlobalCopy().setParent(this.strandTipTracerAxes[i]);

		}



		public ArrayList<StrandedArmature> generateChildStrandCollection() {
			/**This isn't wet code. It's accounting for the fact that there might be a pin
			 * at a multi-bone juncture. Which means each bone emerging from the juncture 
			 * should also be its own strandedArmature.
			 */
			ArrayList<StrandedArmature> result = new ArrayList<StrandedArmature>();
			ArrayList<AbstractBone> childrenWithPinnedDescendents = this.getStrandTip().returnChildrenWithPinnedDescendants();
			for ( AbstractBone b : childrenWithPinnedDescendents) {
				result.add(new StrandedArmature(this, b));
			}
			return result;
		}


		/*public float getTotalErrorAcrossStrandsIfRotatedTo(Rot rotationTo, AbstractBone boneToRotate) {
        	float result = 0f;
        	strandTempAxes.alignGlobalsTo(simulatedLocalAxes.get(boneToRotate));        	
        	return result; 
        }*/

		public void updateDistToTarget() {
			this.distToTarget = this.getStrandTip().getTip_().dist(this.getStrandTip().pinnedTo());
		}

		public void revertRotations() {
			for ( AbstractBone b : this.bones) {
				b.localAxes().rotateBy(this.rotationsMap.get(b).revert());
			}
		}

		public void revertSimulatedLocalAxes() {
			for ( AbstractBone b : this.bones) {
				for(int i =0; i< SimAxesPerBone; i++) {
					this.simulatedLocalAxes.get(b)[i].alignGlobalsTo(b.localAxes());
				}
			}
		}

		private void createInitialSimulatedLocalAxesListForBone(AbstractBone bone, AbstractAxes axes) {
			AbstractAxes[] list = this.simulatedLocalAxes.get(bone);//, currentBone.localAxes().getGlobalCopy());
			if(list == null) {
				list = new AbstractAxes[SimAxesPerBone ];
				simulatedLocalAxes.put(bone, list);
			}
			for(int i =0; i<SimAxesPerBone; i++) {
				list[i] = axes.getGlobalCopy();
			}
		}

		private void createInitialConstraintAxesListForBone(AbstractBone bone, AbstractAxes axes) {
			AbstractAxes[] list = this.simulatedConstraintAxes.get(bone);//, currentBone.localAxes().getGlobalCopy());
			if(list == null) {
				list = new AbstractAxes[SimAxesPerBone ];
				simulatedConstraintAxes.put(bone, list);
			}
			for(int i =0; i<SimAxesPerBone; i++) {
				list[i] = axes.getGlobalCopy();
			}
		}

		private void populateStrandBonesData() throws DisconnectedBoneException {
			AbstractBone currentBone = this.getStrandTip();
			AbstractBone previousChild = null;
			while (previousChild != this.getStrandRoot()) {
				this.populateBoneData(currentBone, previousChild);
				createInitialSimulatedLocalAxesListForBone(currentBone, currentBone.localAxes().getGlobalCopy());
				if(currentBone != this.getStrandRoot()) {
					AbstractAxes constraintAxes = currentBone.getConstraint() == null? currentBone.getMajorRotationAxes() : currentBone.getConstraint().limitingAxes();
					createInitialConstraintAxesListForBone(currentBone,  constraintAxes.getGlobalCopy());
				}
				previousChild = currentBone;
				currentBone = currentBone.getParent();
				if(previousChild == this.getStrandRoot()) {
					currentBone = previousChild;
					break;
				} else if (currentBone == null) {
					throw new DisconnectedBoneException("");
				}
			}
			//this.populateBoneData(currentBone, previousChild);
			createInitialSimulatedLocalAxesListForBone(currentBone, currentBone.localAxes().getGlobalCopy());
			if (currentBone.getParent() != null) {
				AbstractAxes constraintAxes = currentBone.getConstraint() == null? currentBone.getMajorRotationAxes() : currentBone.getConstraint().limitingAxes();
				createInitialConstraintAxesListForBone(currentBone,  constraintAxes.getGlobalCopy());
			}
			for (currentBone = this.getStrandTip(); currentBone != this.getStrandRoot(); currentBone = currentBone.getParent()) {
				for(int i= 0; i<SimAxesPerBone; i++) {
					this.simulatedLocalAxes.get(currentBone)[i].setParent(this.simulatedLocalAxes.get(currentBone.getParent())[i]);
					this.simulatedConstraintAxes.get(currentBone)[i].setParent(this.simulatedLocalAxes.get(currentBone.getParent())[i]);
				}
			}
			if (currentBone.getParent() != null) {            	
				AbstractAxes offStrandSimulatedParentAxes = parentStrandedAmature.averageSimulatedAxes.get(this.getStrandRoot().getParent());
				for(int i= 0; i<SimAxesPerBone; i++) {
					this.simulatedLocalAxes.get(currentBone)[i].setParent(this.parentStrandCollection.simulatedStrandRootParentAxes);
					this.simulatedConstraintAxes.get(currentBone)[i].setParent(offStrandSimulatedParentAxes);//this.simulatedLocalAxes.get(currentBone).getParentAxes());
				}
			}
			else {
				if(currentBone == this.getStrandRoot() && currentBone == this.getStrandTip()) 
					createInitialSimulatedLocalAxesListForBone(currentBone, currentBone.localAxes().getGlobalCopy());
				for(int i= 0; i<SimAxesPerBone; i++) {
					this.simulatedLocalAxes.get(currentBone)[i].setParent(currentBone.parentArmature.localAxes());
				}               
			}
		}

		private void populateBoneData( AbstractBone bone, AbstractBone child) {
			this.bones.add(bone);
			if (!this.parentStrandCollection.allBonesInStrandCollection.contains(bone)) {
				this.parentStrandCollection.allBonesInStrandCollection.add(bone);
			}
			if (!this.parentStrandCollection.boneStrandMap.containsKey(bone)) {
				this.parentStrandCollection.boneStrandMap.put(bone, new ArrayList<Strand>());
			}
			if (!this.parentStrandCollection.boneStrandMap.get(bone).contains(this)) {
				this.parentStrandCollection.boneStrandMap.get(bone).add(this);
			}
			if(child != null) {
				if(bone.getConstraint() != null) {
					this.remainingFreedomAtBoneMap.put(bone, bone.getConstraint().getRotationalFreedom() + remainingFreedomAtBoneMap.get(child));
				} else {
					this.remainingFreedomAtBoneMap.put(bone, 1f + remainingFreedomAtBoneMap.get(child));
				}
			} else {
				this.remainingFreedomAtBoneMap.put(bone, 1f);
			}
		}

		public float getRemainingFreedomAtBone(AbstractBone b) {
			return remainingFreedomAtBoneMap.get(b);
		}

		public void alignSimulationAxesToBones() {
			this.parentStrandCollection.updateSimulatedStrandRootParentAxes();
			for(int h =0; h<SimAxesPerBone; h++) {
				for (int i = this.bones.size() - 1; i >= 0; --i) {
					AbstractBone currentBone = this.bones.get(i);
					AbstractAxes currentSimAxes = this.simulatedLocalAxes.get(currentBone)[h];
					if (currentSimAxes.getParentAxes() != null) {
						currentSimAxes.emancipate();
					}
					AbstractAxes currentLocal = this.parentStrandCollection.averageSimulatedAxes.get(currentBone);
					currentSimAxes.alignGlobalsTo(currentLocal);
					currentSimAxes.updateGlobal();
				}
				for (int i = 0; i < this.bones.size(); ++i) {
					AbstractBone currentBone = this.bones.get(i);
					AbstractAxes currentSimAxes = this.simulatedLocalAxes.get(currentBone)[h];
					AbstractBone parentBone = currentBone.getParent();
					if (parentBone != null && this.simulatedLocalAxes.get(parentBone) != null) {
						currentSimAxes.setParent((AbstractAxes)this.simulatedLocalAxes.get(parentBone)[h]);
					}
					else {
						currentSimAxes.setParent(this.parentStrandCollection.simulatedStrandRootParentAxes);
					}
				}
				for (int i = 0; i < this.bones.size(); ++i) {
					AbstractBone currentBone = this.bones.get(i);
					if (this.simulatedConstraintAxes.get(currentBone) != null) {
						AbstractAxes simulatedConstraintAxes = this.simulatedConstraintAxes.get(currentBone)[h];
						AbstractAxes actualConstraintAxes = currentBone.getConstraint() == null? currentBone.getMajorRotationAxes() : currentBone.getConstraint().limitingAxes();

						simulatedConstraintAxes.localMBasis.adoptValues(actualConstraintAxes.localMBasis);
						simulatedConstraintAxes.markDirty();
						simulatedConstraintAxes.updateGlobal();
					}
				}
			}
		}

		public AbstractBone getStrandTip() {
			return strandTip;
		}

		public void setStrandTip(AbstractBone strandTip) {
			this.strandTip = strandTip;
		}

		public AbstractBone getStrandRoot() {
			return strandRoot;
		}

		public void setStrandRoot(AbstractBone strandRoot) {
			this.strandRoot = strandRoot;
		}
	}
}
