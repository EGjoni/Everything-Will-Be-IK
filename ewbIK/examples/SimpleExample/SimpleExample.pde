
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
  fifthBone.setPin(new DVector(-200, 0, 0));
  bSixthBone.enablePin();
  bSixthBone.setPin(new DVector(100, 50, 0));
  
  //solving once before the drawing phase because it looks nicer when the sketch starts
  simpleArmature.tranquilIKSolver(rootBone, 0.1, 50);
  
}

  void draw() {
    background(160, 100, 100);
    mouse.x =  mouseX - (width/2); mouse.y = mouseY - (height/2);
    camera(cameraPosition, lookAt, up);
    ortho(-width/2, width/2, -height/2, height/2, -10000, 10000);
    drawBones();  

    if(mousePressed) {
      rootBone.setPin(mouse);      
      //simpleArmature.ambitiousIKSolver(bSixthBone, 0.1, 20);
      simpleArmature.tranquilIKSolver(rootBone, 0.01, 25);
    }
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