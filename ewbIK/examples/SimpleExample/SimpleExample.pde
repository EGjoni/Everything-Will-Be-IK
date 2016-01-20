/*
USE w, a, s, d KEYS TO MOVE PIN
*/


import ewbIK.*;
import pseudoScene.*;

boolean w, a, s, d;  //keyPress Variables

Armature simpleArmature;
Bone rootBone, initialBone, secondBone, thirdBone, fourthBone, fifthBone;


void setup() {
  size(800, 600, P3D);
 
  simpleArmature = new Armature("example");

  rootBone = simpleArmature.getRootBone();
  DVector roll = new DVector(0, 0, 1);

  DVector t1Tip = new DVector(0, 5, 0); 
  DVector t2Tip = new DVector(0, 9, 0);
  DVector t3Tip = new DVector(0, 12, 0);
  DVector t4Tip = new DVector(0, 17, 0); 
  DVector t5Tip = new DVector(0, 21, 0); 

  initialBone = new Bone(rootBone, t1Tip, roll, "initial", 5d, Bone.frameType.GLOBAL);
  secondBone = new Bone(initialBone, t2Tip, roll, "nextBone", 5d, Bone.frameType.GLOBAL);
  thirdBone = new Bone(secondBone, t3Tip, roll, "anotherBone", 5d, Bone.frameType.GLOBAL); 
  fourthBone = new Bone(thirdBone, t4Tip, roll, "oneMoreBone", 5d, Bone.frameType.GLOBAL);
  fifthBone = new Bone(fourthBone, t5Tip, roll, "fifthBone", 5d, Bone.frameType.GLOBAL);
  
  rootBone.enablePin();
  fifthBone.enablePin();
}

void draw() {
  background(160);
  camera(cameraPosition, lookAt, up);
 
  drawBone(initialBone);
  drawBone(secondBone);
  drawBone(thirdBone);
  drawBone(fourthBone);
  drawBone(fifthBone);
  
  stroke(255,0,0);
  strokeWeight(10);
  point(rootBone.getPin());
  point(fifthBone.getPin());
  
  if(a || d || s || w || keyPressed) {
     if(a) fifthBone.setPin(DVector.add(fifthBone.getPin(),new DVector(0.4,0,0)));
     if(d) fifthBone.setPin(DVector.add(fifthBone.getPin(),new DVector(-0.4,0,0)));
     if(s) fifthBone.setPin(DVector.add(fifthBone.getPin(),new DVector(0,-0.4,0)));
     if(w) fifthBone.setPin(DVector.add(fifthBone.getPin(),new DVector(0, 0.4, 0)));
     
     simpleArmature.solveIK(fourthBone, 0.1, 5);
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
}


public void keyPressed() {
switch(key) {
      case 'a' : a = true;
      break;
      case 'd' : d = true;
      break;
      case 's' : s= true;
      break;
      case 'w' : w= true;
    }
}

public void keyReleased() {
  switch(key) {
      case 'a' : a = false;
      break;
      case 'd' : d = false;
      break;
      case 's' : s= false;
      break;
      case 'w' : w= false;
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
PVector lookAt = new PVector(0, 10, 0);
PVector up = new PVector(0, -1, 0);

public void camera(PVector cp, PVector so, PVector up) {
  camera(cp.x, cp.y, cp.z, so.x, so.y, so.z, up.x, up.y, up.z);
}