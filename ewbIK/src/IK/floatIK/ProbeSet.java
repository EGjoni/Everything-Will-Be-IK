package IK.floatIK;

import java.util.ArrayList;
import java.util.Comparator;

import sceneGraph.math.floatV.AbstractAxes;
import sceneGraph.math.floatV.MRotation;
import sceneGraph.math.floatV.MathUtils;
import sceneGraph.math.floatV.Rot;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.floatV.sgRayf;

public class ProbeSet {
	
	float outerAngle = MathUtils.PI/2f;
	SegmentedArmature sa = null;
	AbstractBone forBone = null; 
	
	ArrayList<Probe> sortedProbeList = new ArrayList<>();
	Probe nullProbe = null;
/**
 * Experimental: 
 * Maintains a set of rotation axes and angles for the given bone, and the errors
 * that the child effectors would incur from each rotation.
 * These errors are then used to find a weighted average rotation which 
 * should in theory converge on the smallest rotation error. 
 */
 public ProbeSet(AbstractBone forBone, SegmentedArmature sa, float dampening) {
	 this.forBone = forBone;
	 this.sa = sa;
	 nullProbe = new Probe(null, 0, sa, forBone);
	
	 this.setDampening(dampening);
	 
 }
 
 public void initializeProbes() {
	 	 
	
	 sortedProbeList.add(new Probe(new SGVec_3f(0,0,1), outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(0,1,0), outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(0,1,1), outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(1,0,0), outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(1,0,1), outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(1,1,0), outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(1,1,1), outerAngle, sa, forBone));
	 
	 sortedProbeList.add(new Probe(new SGVec_3f(0,0,1), -outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(0,1,0), -outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(0,1,1), -outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(1,0,0), -outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(1,0,1), -outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(1,1,0), -outerAngle, sa, forBone));
	 sortedProbeList.add(new Probe(new SGVec_3f(1,1,1), -outerAngle, sa, forBone));
 }
 
 
 public void setDampening(float newDamp) {
	 outerAngle = newDamp; 
	 updateProbes();
 }
 
 public void updateProbes() {
	 
	 sortedProbeList.get(0).updateProbe(new SGVec_3f(0,0,1), outerAngle);
	 sortedProbeList.get(1).updateProbe(new SGVec_3f(0,1,0), outerAngle);
	 sortedProbeList.get(2).updateProbe(new SGVec_3f(0,1,1), outerAngle);
	 sortedProbeList.get(3).updateProbe(new SGVec_3f(1,0,0), outerAngle);
	 sortedProbeList.get(4).updateProbe(new SGVec_3f(1,0,1), outerAngle);
	 sortedProbeList.get(5).updateProbe(new SGVec_3f(1,1,0), outerAngle);
	 sortedProbeList.get(6).updateProbe(new SGVec_3f(1,1,1), outerAngle);
	                 
	 sortedProbeList.get(7).updateProbe(new SGVec_3f(0,0,1), -outerAngle);
	 sortedProbeList.get(8).updateProbe(new SGVec_3f(0,1,0), -outerAngle);
	 sortedProbeList.get(9).updateProbe(new SGVec_3f(0,1,1), -outerAngle);
	 sortedProbeList.get(10).updateProbe(new SGVec_3f(1,0,0), -outerAngle);
	 sortedProbeList.get(11).updateProbe(new SGVec_3f(1,0,1), -outerAngle);
	 sortedProbeList.get(12).updateProbe(new SGVec_3f(1,1,0), -outerAngle);
	 sortedProbeList.get(13).updateProbe(new SGVec_3f(1,1,1), -outerAngle);
	 
 }
 	

 
 public Rot getMinimalErrorRotation() {
	 updateProbes();
	/**
	 * from the list of Probes, we select only the elements with rotation errors less than or equal to 
	 * the zero-rotation.  
	 */
	
	nullProbe.updateError();
	for(Probe p : sortedProbeList) {
		p.updateError();		
	}
	 
	sortedProbeList.sort(new Comparator<Probe>() {

		public int compare(Probe p1, Probe p2) {
		   

		   //ascending order
		   return (int) (p1.error- p2.error);

		   //descending order
		   //return StudentName2.compareTo(StudentName1);
	    }});
	return null;
	 
	 
 }
 
 
 public class Probe {
	 
	 public float error = 1f;
	 Rot rotation = new Rot();
	 SegmentedArmature sa = null;
	 AbstractBone forBone = null;
	 AbstractAxes originalSimBoneAxes = null;
	 
	 
	 public Probe (SGVec_3f axis, float angle, SegmentedArmature segarm, AbstractBone bone) {
		 if(angle >=0 && axis != null) {
			 rotation = new Rot(axis, angle);
		 }
		 sa = segarm;
		 forBone = bone;
		 originalSimBoneAxes = sa.simulatedLocalAxes.get(forBone);
	 }
	 
	 public void updateProbe(SGVec_3f axis, float angle) {
		 rotation.set(axis, angle);
	 }
 	 
	 
	 
	 
	 public void updateError() {
		 
		 AbstractAxes simBoneAxes = sa.simulatedLocalAxes.get(forBone);
		 originalSimBoneAxes.alignGlobalsTo(simBoneAxes);
		 
		 simBoneAxes.rotateBy(rotation);
		 
		 float totalTranslationError = 1f;
		 float totalRotationError = 1f;
		
		 for(int i=0; i< sa.pinnedDescendants.size(); i++) {
				SegmentedArmature s = sa.pinnedDescendants.get(i);
				AbstractAxes targetAxes = s.segmentTip.getPinnedAxes();
				AbstractAxes tipAxes = s.simulatedLocalAxes.get(s.segmentTip);		
				
				tipAxes.updateGlobal();
				targetAxes.updateGlobal();
				
				float distance = tipAxes.origin_().dist(targetAxes.origin_());
				Rot rotationDiff = tipAxes.globalMBasis.rotation.applyInverseTo(targetAxes.globalMBasis.rotation);
				
				totalTranslationError += distance;
				totalRotationError += rotationDiff.getAngle();
				
			}
		 
		 error = 2f / (totalRotationError + totalTranslationError);
		 simBoneAxes.alignGlobalsTo(originalSimBoneAxes);
		 
	 }
	 
 }
 
}
