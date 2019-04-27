
import IK.*;
import sceneGraph.*;

Armature simpleArmature;
Bone  rootBone, initialBone, 
      secondBone, thirdBone, 
      fourthBone, fifthBone, 
      bFourthBone, bFifthBone, 
      bSixthBone;


void setup() {
  size(1200, 900, P3D);
  simpleArmature = new Armature("example");
  simpleArmature.localAxes().translateTo(new DVector(0, 200, 0));
  simpleArmature.localAxes().rotateAboutZ(PI);
  
  initializeBones(); 
  setBoneConstraints();
}

  void draw() {
    background(160, 100, 100);
    mouse.x =  mouseX - (width/2); mouse.y = mouseY - (height/2);
    camera(cameraPosition, lookAt, up);
    ortho(-width/2, width/2, -height/2, height/2, -10000, 10000);
    drawBones();  

    if(mousePressed) {
      bSixthBone.setPin(mouse);      
      //simpleArmature.ambitiousIKSolver(rootBone, 0.1, 20);
      simpleArmature.tranquilIKSolver(bSixthBone, 0.1, 20);
    }
  }


public void initializeBones() {
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
}

public void setBoneConstraints() {    
  
    Kusudama fourthConstraint = new Kusudama(fourthBone);
    fourthConstraint.addLimitConeAtIndex(0, new DVector(.5, 1, 0), .2d);
    fourthConstraint.addLimitConeAtIndex(1, new DVector(-.5, 1, 0), PI/5);
    fourthConstraint.setAxialLimits(0.1,2d);
    fourthConstraint.enable();
    fourthBone.addConstraint(fourthConstraint);
    
    Kusudama fifthConstraint = new Kusudama(fifthBone);
    fifthConstraint.addLimitConeAtIndex(0, new DVector(.5, 1, 0), PI/7);
    fifthConstraint.addLimitConeAtIndex(1, new DVector(-.5, 1, 0), PI);
    fifthConstraint.setAxialLimits(0.1,1.5d);
    fifthConstraint.enable();
    fifthBone.addConstraint(fifthConstraint); 
    
    Kusudama bFourthConstraint = new Kusudama(bFourthBone);
    bFourthConstraint.addLimitConeAtIndex(0, new DVector(.5, 1, 0), .5d);
    bFourthConstraint.setAxialLimits(0.1,2d);
    bFourthConstraint.enable();
    bFourthBone.addConstraint(bFourthConstraint);
    
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
    double[] angleArr = bone.getXZYAngle();
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