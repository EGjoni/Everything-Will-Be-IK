package IK;

import java.util.*;
import sceneGraph.*;
import sceneGraph.math.SGVec_3d;

public class StrandedArmature
{
	
	/**
	 * Note: This code is doing a lot of stuff conceptually in relatively few lines 
	 * and might be difficult to wrap your head around.  I've opted to make it concise, 
	 * because making it verbose did not make it any easier to digest. If you're going to edit it,
	 * you might want to take some time to graph things out.
	 */
    public ArrayList<Strand> strands;
    public ArrayList<StrandedArmature> childCollections;
    public ArrayList<AbstractBone> allBonesInStrandCollection;
    public HashMap<AbstractBone, ArrayList<Strand>> boneStrandMap;
    public HashMap<AbstractBone, Double> angleDeltaMap;
    public HashMap<AbstractBone, AbstractAxes> averageSimulatedAxes;
    Double lastLargestAngleDelta;
    Double amountIncreasing;
    Double amountDecreasing;
    int increasingSampleCount;
    int decreasingSampleCount;
    protected StrandedArmature parentStrandedAmature;
    public Strand parentStrand;
    public double totalPinDist;
    public int distanceToRoot;
    public int chainLength;
    boolean includeInIK;
    boolean basePinned;
    AbstractBone strandRoot;
    AbstractAxes simulatedStrandRootParentAxes;
    

    
    public StrandedArmature( AbstractBone rootBone) {
        this.strands = new ArrayList<Strand>();
        this.childCollections = new ArrayList<StrandedArmature>();
        this.allBonesInStrandCollection = new ArrayList<AbstractBone>();
        this.boneStrandMap = new HashMap<AbstractBone, ArrayList<Strand>>();
        this.angleDeltaMap = new HashMap<AbstractBone, Double>();
        this.averageSimulatedAxes = new HashMap<AbstractBone, AbstractAxes>();
        this.lastLargestAngleDelta = null;
        this.amountIncreasing = 0.0;
        this.amountDecreasing = 0.0;
        this.increasingSampleCount = 0;
        this.decreasingSampleCount = 0;
        this.parentStrandedAmature = null;
        this.parentStrand = null;
        this.totalPinDist = 0.0;
        this.distanceToRoot = 0;
        this.chainLength = 0;
        this.includeInIK = true;
        this.basePinned = false;
        this.simulatedStrandRootParentAxes = null;
        this.strandRoot = this.armatureRootBone(rootBone);
        this.allBonesInStrandCollection.add(this.strandRoot);
        this.generateStrandHierarchy();
    }
    
    public StrandedArmature( Strand inputParentStrand,  AbstractBone inputStrandRoot) {
        this.strands = new ArrayList<Strand>();
        this.childCollections = new ArrayList<StrandedArmature>();
        this.allBonesInStrandCollection = new ArrayList<AbstractBone>();
        this.boneStrandMap = new HashMap<AbstractBone, ArrayList<Strand>>();
        this.angleDeltaMap = new HashMap<AbstractBone, Double>();
        this.averageSimulatedAxes = new HashMap<AbstractBone, AbstractAxes>();
        this.lastLargestAngleDelta = null;
        this.amountIncreasing = 0.0;
        this.amountDecreasing = 0.0;
        this.increasingSampleCount = 0;
        this.decreasingSampleCount = 0;
        this.parentStrandedAmature = null;
        this.parentStrand = null;
        this.totalPinDist = 0.0;
        this.distanceToRoot = 0;
        this.chainLength = 0;
        this.includeInIK = true;
        this.basePinned = false;
        this.simulatedStrandRootParentAxes = null;
        this.strandRoot = inputStrandRoot;
        this.parentStrandedAmature = inputParentStrand.parentStrandCollection;
        this.parentStrand = inputParentStrand;
        this.distanceToRoot = this.parentStrandedAmature.distanceToRoot + 1;
        this.basePinned = (inputParentStrand != null && inputParentStrand.strandTip.isPinned());
        this.allBonesInStrandCollection.add(this.strandRoot);
        this.updateSimulatedStrandRootParentAxes();
        this.generateStrandHierarchy();
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
            		this.simulatedStrandRootParentAxes =  this.parentStrandedAmature.averageSimulatedAxes.get(this.strandRoot.parent);
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
                    this.simulatedStrandRootParentAxes.alignGlobalsTo(this.strandRoot.localAxes.getParentAxes());
                    this.simulatedStrandRootParentAxes.updateGlobal();
                }
                else {
                	AbstractAxes parentAverage  = (AbstractAxes)this.parentStrand.parentStrandCollection.averageSimulatedAxes.get(this.parentStrand.strandTip); 
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
        this.amountIncreasing = 0.0;
        this.amountDecreasing = 0.0;
        this.increasingSampleCount = 0;
        this.decreasingSampleCount = 0;
    }
    
    public void updateStabilityEstimates() {
         double thisLargestDelta = this.getLargestAngleChange();
        if (this.lastLargestAngleDelta != null) {
             double change = thisLargestDelta - this.lastLargestAngleDelta;
            if (change < 0.0) {
                this.amountDecreasing += -1.0 * change;
                ++this.increasingSampleCount;
            }
            else {
                this.amountIncreasing += change;
                ++this.decreasingSampleCount;
            }
        }
        this.lastLargestAngleDelta = thisLargestDelta;
    }
    
    private double getLargestAngleChange() {
        double largestDelta = 0.0;
        for ( AbstractBone b : this.allBonesInStrandCollection) {
             Double delta = Math.abs(this.angleDeltaMap.get(b));
            if (delta != null && delta > largestDelta) {
                largestDelta = delta;
            }
        }
        return largestDelta;
    }
    
    public void setDeltaMeasureForBone( AbstractBone b,  double angle) {
        this.angleDeltaMap.put(b, angle);
    }
    
    public double getStability() {
        if (this.increasingSampleCount == 0) {
            return 0.0;
        }
        if (this.decreasingSampleCount == 0) {
            return 1.0;
        }
        return this.increasingSampleCount / (this.increasingSampleCount + this.decreasingSampleCount);
    }
    
    public void averageSimulatedAxesOrientations() {
    }
    
    public void updateTotalPinDist() {
        this.totalPinDist = 0.0;
        for ( Strand s : this.strands) {
            this.totalPinDist += s.strandTip.getTip_().dist(s.strandTip.getPinPosition());
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
        else if (!this.basePinned) {
            addTo.addAll(this.strands);
        }
    }
    
    public AbstractBone armatureRootBone( AbstractBone rootBone2) {
        AbstractBone rootBone3;
        for (rootBone3 = rootBone2; rootBone3.parent != null; rootBone3 = rootBone3.parent) {}
        return rootBone3;
    }
    
    public void translateToAverageTipError( boolean onlyIfUnpinned) {
        if (!onlyIfUnpinned || !this.basePinned) {
             SGVec_3d totalDiff = new SGVec_3d(0.0, 0.0, 0.0);
             AbstractAxes translationAxes = this.averageSimulatedAxes.get(this.strandRoot);
            AbstractAxes tipBoneAxes = null;
            AbstractBone tipBone = null;
            SGVec_3d target = null;
            SGVec_3d tracer = null;
            for ( Strand s : this.strands) {
                tipBone = s.strandTip;
                tipBoneAxes = this.averageSimulatedAxes.get(tipBone);
                target = tipBone.getPinnedAxes().origin_().copy();
                tracer = tipBoneAxes.origin_().copy();
                totalDiff.add(SGVec_3d.sub(target, tracer));
            }
             double count = this.strands.size();
             SGVec_3d averageDiff = SGVec_3d.div(totalDiff, count);
            // IKVector baseExpected = IKVector.add(averageDiff, translationAxes.origin());
          //   IKVector tracerExpected = IKVector.add(tipBoneAxes.getGlobalOf(new IKVector(0.0, tipBone.getBoneHeight() / 2.0, 0.0)), averageDiff);
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
    
    public class DisconnectedBoneException extends Exception
    {
        public DisconnectedBoneException( String message) {
            super("Error: BoneExample is not connected to armature. Ignoring strand");
        }
    }
    
    public class Strand
    {
        AbstractAxes strandTipTracerAxes;
        AbstractBone strandTip;
        AbstractBone strandRoot;
        public boolean rootParentPinned;
        StrandedArmature parentStrandCollection;
        ArrayList<AbstractBone> bones;
        HashMap<AbstractBone, Rot> rotationsMap;
        HashMap<AbstractBone, Rot> tempRotationsMap;
        HashMap<AbstractBone, AbstractAxes> simulatedLocalAxes;
        HashMap<AbstractBone, AbstractAxes> simulatedConstraintAxes;
        HashMap<AbstractBone, Double> remainingFreedomAtBoneMap ; 
        double distToTarget;
        AbstractAxes strandTempAxes;
        
        public Strand( StrandedArmature par,  AbstractBone tip,  AbstractBone root) throws DisconnectedBoneException {
            this.rootParentPinned = true;
            this.bones = new ArrayList<AbstractBone>();
            this.rotationsMap = new HashMap<AbstractBone, Rot>();
            this.tempRotationsMap = new HashMap<AbstractBone, Rot>();
            this.simulatedLocalAxes = new HashMap<AbstractBone, AbstractAxes>();
            this.simulatedConstraintAxes = new HashMap<AbstractBone, AbstractAxes>();
            this.remainingFreedomAtBoneMap = new HashMap<>();
            this.distToTarget = 0.0;
            this.parentStrandCollection = par;
            this.strandTip = tip;
            this.strandRoot = root;
            this.strandTipTracerAxes = this.strandTip.localAxes().getGlobalCopy();
            if (this.strandRoot.parent != null && this.strandRoot.parent.isPinned()) {
                this.rootParentPinned = true;
            }
            else {
                this.rootParentPinned = false;
            }
            this.populateStrandBonesData();
            (this.strandTempAxes = this.simulatedLocalAxes.get(this.strandTip).getGlobalCopy()).setParent(this.strandTipTracerAxes);
        }
        
        public ArrayList<StrandedArmature> generateChildStrandCollection() {
        	/**This isn't wet code. It's accounting for the fact that there might be a pin
			 * at a multi-bone juncture. Which means each bone emerging from the juncture 
			 * should also be its own strandedArmature.
			 */
             ArrayList<StrandedArmature> result = new ArrayList<StrandedArmature>();
             ArrayList<AbstractBone> childrenWithPinnedDescendents = this.strandTip.returnChildrenWithPinnedDescendants();
            for ( AbstractBone b : childrenWithPinnedDescendents) {
                result.add(new StrandedArmature(this, b));
            }
            return result;
        }
        
        
        /**aligns the tracer axes for the strand tip to the bone,
		 * then returns the axes. 
		 */
        public AbstractAxes getStrandTipTracerAxes() {
            this.strandTipTracerAxes.alignGlobalsTo(
            		this.simulatedLocalAxes.get(this.strandTip));
             //IKVector boneCenter = IKVector.div(IKVector.add(this.simulatedLocalAxes.get(this.strandTip).origin(), this.simulatedLocalAxes.get(this.strandTip).y().getScaledTo(this.strandTip.getBoneHeight())), 2.0);
            //this.strandTipTracerAxes.translateTo(boneCenter);
            return this.strandTipTracerAxes;
        }
        
        public double getTotalErrorAcrossStrandsIfRotatedTo(Rot rotationTo, AbstractBone boneToRotate) {
        	double result = 0d;
        	strandTempAxes.alignGlobalsTo(simulatedLocalAxes.get(boneToRotate));
        	
        	
        	return result; 
        }
        
        public void updateDistToTarget() {
            this.distToTarget = this.strandTip.getTip_().dist(this.strandTip.pinnedTo());
        }
        
        public void revertRotations() {
            for ( AbstractBone b : this.bones) {
                b.localAxes().rotateBy(this.rotationsMap.get(b).revert());
            }
        }
        
        public void revertSimulatedLocalAxes() {
            for ( AbstractBone b : this.bones) {
                this.simulatedLocalAxes.get(b).alignGlobalsTo(b.localAxes());
            }
        }
        
        private void populateStrandBonesData() throws DisconnectedBoneException {
            AbstractBone currentBone = this.strandTip;
            AbstractBone previousChild = null;
            while (previousChild != this.strandRoot) {
                this.populateBoneData(currentBone, previousChild);
                this.simulatedLocalAxes.put(currentBone, currentBone.localAxes().getGlobalCopy());
                if(currentBone != this.strandRoot) 
                	this.simulatedConstraintAxes.put(currentBone, currentBone.getConstraint().limitingAxes().getGlobalCopy());
                previousChild = currentBone;
                currentBone = currentBone.parent;
                if(previousChild == this.strandRoot) {
                	currentBone = previousChild;
                	break;
                } else if (currentBone == null) {
                    throw new DisconnectedBoneException("");
                }
            }
            //this.populateBoneData(currentBone, previousChild);
            this.simulatedLocalAxes.put(currentBone, currentBone.localAxes().getGlobalCopy());
            if (currentBone.parent != null) {
                this.simulatedConstraintAxes.put(currentBone, currentBone.getConstraint().limitingAxes().getGlobalCopy());
            }
            for (currentBone = this.strandTip; currentBone != this.strandRoot; currentBone = currentBone.parent) {
                this.simulatedLocalAxes.get(currentBone).setParent(this.simulatedLocalAxes.get(currentBone.parent));
                this.simulatedConstraintAxes.get(currentBone).setParent(this.simulatedLocalAxes.get(currentBone.parent));
            }
            if (currentBone.parent != null) {
                this.simulatedLocalAxes.get(currentBone).setParent(this.parentStrandCollection.simulatedStrandRootParentAxes);
                AbstractAxes offStrandSimulatedParentAxes = parentStrandedAmature.averageSimulatedAxes.get(this.strandRoot.parent);
                this.simulatedConstraintAxes.get(currentBone).setParent(offStrandSimulatedParentAxes);//this.simulatedLocalAxes.get(currentBone).getParentAxes());
            }
            else {
            	if(currentBone == this.strandRoot && currentBone == this.strandTip) 
            	    this.simulatedLocalAxes.put(currentBone, currentBone.localAxes().getGlobalCopy());
                this.simulatedLocalAxes.get(currentBone).setParent(currentBone.parentArmature.localAxes());
               
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
            		this.remainingFreedomAtBoneMap.put(bone, 1d + remainingFreedomAtBoneMap.get(child));
            	}
            } else {
            	this.remainingFreedomAtBoneMap.put(bone, 1d);
            }
        }
        
        public double getRemainingFreedomAtBone(AbstractBone b) {
        	return remainingFreedomAtBoneMap.get(b);
        }
        
        public void alignSimulationAxesToBones() {
            this.parentStrandCollection.updateSimulatedStrandRootParentAxes();
            for (int i = this.bones.size() - 1; i >= 0; --i) {
                 AbstractBone currentBone = this.bones.get(i);
                 AbstractAxes currentSimAxes = this.simulatedLocalAxes.get(currentBone);
                if (currentSimAxes.getParentAxes() != null) {
                    currentSimAxes.emancipate();
                }
                 AbstractAxes currentLocal = this.parentStrandCollection.averageSimulatedAxes.get(currentBone);
                currentSimAxes.alignGlobalsTo(currentLocal);
                currentSimAxes.updateGlobal();
            }
            for (int i = 0; i < this.bones.size(); ++i) {
                 AbstractBone currentBone = this.bones.get(i);
                 AbstractAxes currentSimAxes = this.simulatedLocalAxes.get(currentBone);
                 AbstractBone parentBone = currentBone.getParent();
                if (parentBone != null && this.simulatedLocalAxes.get(parentBone) != null) {
                    currentSimAxes.setParent((AbstractAxes)this.simulatedLocalAxes.get(parentBone));
                }
                else {
                    currentSimAxes.setParent(this.parentStrandCollection.simulatedStrandRootParentAxes);
                }
            }
            for (int i = 0; i < this.bones.size(); ++i) {
                 AbstractBone currentBone = this.bones.get(i);
                if (this.simulatedConstraintAxes.get(currentBone) != null) {
                     AbstractAxes constraintAxes = this.simulatedConstraintAxes.get(currentBone);
                    constraintAxes.localMBasis.adoptValues(currentBone.getConstraint().limitingAxes().localMBasis);
                    constraintAxes.markDirty();
                    constraintAxes.updateGlobal();
                }
            }
        }
    }
}
