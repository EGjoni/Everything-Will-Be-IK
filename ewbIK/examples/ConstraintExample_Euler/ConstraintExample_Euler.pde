
/**
WARNING!! YOU SHOULD AVOID USING EULER CONSTRAINTS!!!!

Euler Angles are convenient programmatically, however, they suffer from
singularity issues, and in this particular implementation,
have the simplest possible implementation, which is liable to cause instability.
You should avoid using them if you can help it. 

The recommended constraint to use is Kusudama, it is easy to reason about, extremely flexible,
and demonstrates excellent stability.

This is mostly here as a placeholder until I make a larger example on
creating ustom constraints
*/
import IK.*;
import sceneGraph.*;

Armature simpleArmature;
Bone  rootBone, initialBone, 
      secondBone, thirdBone, 
      fourthBone, fifthBone, 
      bFourthBone, bFifthBone, 
      bSixthBone;


void setup() {
  size(800, 600, P3D);
  simpleArmature = new Armature("example");
  simpleArmature.localAxes().translateTo(new DVector(0, 200, 0));
  simpleArmature.localAxes().rotateAboutZ(PI);
  
  rootBone = simpleArmature.getRootBone();
  
  initialBone = new Bone(rootBone, "initial", 74d);
  secondBone = new Bone(initialBone, "nextBone", 86d);
  thirdBone = new Bone(secondBone, "anotherBone", 98d); 
  fourthBone = new Bone(thirdBone, "oneMoreBone", 70d);
  fifthBone = new Bone(fourthBone, "fifthBone", 80d);  
  
  bFourthBone = new Bone(thirdBone, "branchBone", 80d);
  bFifthBone = new Bone(bFourthBone, "nextBranch", 70d);
  bSixthBone = new Bone(bFifthBone, "leaf", 80d);
    
  secondBone.rotAboutFrameZ(.4d);
  thirdBone.rotAboutFrameZ(.4d);
  
  bFourthBone.rotAboutFrameZ(-.5d);
  bFifthBone.rotAboutFrameZ(-1d);
  bSixthBone.rotAboutFrameZ(-.5d);
  initialBone.rotAboutFrameX(.01d);  
  
  rootBone.enablePin();  
  fifthBone.enablePin();
  bSixthBone.enablePin();
  
  setBoneLimits();
}

  void draw() {
    background(160, 100, 100);
    mouse.x =  mouseX - (width/2); mouse.y = mouseY - (height/2);
    camera(cameraPosition, lookAt, up);
    ortho(-width/2, width/2, -height/2, height/2, -10000, 10000);
        

    if(mousePressed) {
      fifthBone.setPin(mouse);
          
      /*for this example, we are using unstable Euler constraints, so, 
      it doesn't really matter which solver we settle on. Things will still probably get wonky*/
      simpleArmature.ambitiousIKSolver(bSixthBone, 0.1, 50);
      //simpleArmature.tranquilIKSolver(bSixthBone, .1, 50);
    }   
    
     drawBones(); 
  }

public void setBoneLimits() {
    EulerLimits firstConstraint = new EulerLimits(initialBone);
    firstConstraint.setXLimits(-1d, 0d);
    firstConstraint.setYLimits(-1d, 1d);
    firstConstraint.setZLimits(-1d, 1d);
    initialBone.addConstraint(firstConstraint);
    
    EulerLimits secondConstraint = new EulerLimits(secondBone);
    secondConstraint.setXYZLimits(-1d, 0d, -1d, 1d, -1d, 1d);
    secondBone.addConstraint(secondConstraint);
    
    EulerLimits thirdConstraint = new EulerLimits(thirdBone); 
    thirdConstraint.setXYZLimits(-1d, 0d, -1d, 1d, -1d, 1d);
    thirdBone.addConstraint(thirdConstraint);
    
    EulerLimits fourthConstraint = new EulerLimits(fourthBone); 
    fourthConstraint.setXYZLimits(-1d, 0d, -1d, 1d, -1d, 1d);
    fourthBone.addConstraint(fourthConstraint);
    
    EulerLimits fifthConstraint = new EulerLimits(fifthBone); 
    fifthConstraint.setXYZLimits(-1d, 0d, -1d, 1d, -1d, 1d);
    fifthBone.addConstraint(fifthConstraint);
  }
  
    
  DVector mouse = new DVector(0,0,0);


  public void drawBones() {
    ArrayList<?> boneList = simpleArmature.getBoneList();
    for(Object b : boneList) {
      if( b != rootBone)
        drawBone(((Bone)b));
    }    
  }
  
  public void drawBone(Bone bone) {
    stroke(0);
    strokeWeight(2);
    line(bone.getBase(), bone.getTip());
    strokeWeight(7);
    point(bone.getBase());
    strokeWeight(8);
    stroke(255);
    point(bone.getTip());
    if(bone.isPinned()) {
      strokeWeight(10); 
      stroke(255,0,0); 
      point(bone.pinnedTo());
    }
    
    String boneAngles = "";
    try {
    double[] angleArr = bone.getXYZAngle();
     boneAngles += "  ( " + degrees((float)angleArr[0]) + ",   " + degrees((float)angleArr[1]) + ",   " + degrees((float)angleArr[2]) + "  )";
    fill(0);
    text(boneAngles,(float)bone.getBase().x, (float)bone.getBase().y); 
    } catch (Exception e) {
    }
  
    
  }

public void printXY(DVector pd) {
 PVector p = pd.toPVec();
  println(screenX(p.x, p.y, p.z)
    +", " + screenY(p.x, p.y, p.z));
}
public void line(DVector p1d, DVector p2d) {
  PVector p1 = p1d.toPVec();
  PVector p2 = p2d.toPVec();
  line(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z);
}

public void point(DVector pd) {
  PVector p = pd.toPVec();
  point(p.x, p.y, p.z);
}

PVector cameraPosition = new PVector(0, 0, 70); 
PVector lookAt = new PVector(0, 0, 0);
PVector up = new PVector(0, 1, 0);

public void camera(PVector cp, PVector so, PVector up) {
  camera(cp.x, cp.y, cp.z, so.x, so.y, so.z, up.x, up.y, up.z);
}