/*  Hold Shift and click drag to move the camera around. 
*  -click on a bone to select it. 
*  -rightclick and drag to add a new bone to a selected bone. 
*  -left click on a bone's rotor to rotate the bone.
*  -hit the 'p' key to pin or unpin a selected bone for the IK solver.
*  -drag the red pin to it, the ik solver will solve for the position. 
*  
*  -click *ENABLE AXIAL CONSTRAINTS* to enable axial constraints on a selected bone.
*  -click *ENABLE ORIENTATIONAL CONSTRAINTS* to enable orientational constraints on a selectedBone. 
*  -click and drag on any of the controlpoints of a constraint to modify them. 
*  -while a constraint is selected, right click to add a new reach cone to the Kusudama. 
*  -currently, there is some odd behavior with reach-cones extending further than 180 degrees, please avoid using them.
*
*  The user interface is shit. Sorry about that. The IK system is quite stable, if you are experiencing jumpiness,
*  make sure you aren't accidentally dragging a rotor or a constraint while simultaneously dragging and IK pin. 
*  
*  If you find any bugs with the library, or think something needs explaining, please email me at rufsketch1@gmail.com. 
*
*  Don't panic. Everything WIll Be IK.
*/

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;


import ewbIK.*;
import pseudoScene.*;
import controlP5.*;
ControlP5 cp5;


int buttonValue = 1;

int myColor = color(255, 0, 0);


PApplet app;
//PGraphics vecBuff;
public static final int SCREEN_SPACE=0;
public static final int FULL_SPACE=1;

DVector center;
double zoom = radians(3.5);
ArrayList<pArmature> armatures = new ArrayList<pArmature>();
ArrayList<SelectionBone> boneSelectionArray = new ArrayList<SelectionBone>();
int currentlySelectedArmature = 0; 

//state variables which may likely get removed//
boolean slow = true;
boolean w,a,s,d,q,e,t; 
boolean r,g,b;
boolean shift = false;
double orbitRadius = 500;

final int ROTATION = 300;
final int TRANSLATION = 301;

Axes camera = new Axes(new DVector(0,0,0), new DVector(1,0,0), new DVector(0,1,0), new DVector(0,0,1));
PVector cameraPosition = ((DVector)camera.z().getScaledTo(orbitRadius)).toPVec();
Axes globalAxes = new Axes(new DVector(0,0,0), new DVector(1,0,0), new DVector(0,1,0), new DVector(0,0,1));

boolean cameraMode = false;

Bone currentlySelectedTip = null;

boolean mouseLeft = false;
boolean mouseRight = false;
DVector mouseRightDragBegin = null;
DVector mouseLeftDragBegin = null;
DVector mouseCenterDragBegin = null;

boolean extrapolatedView = false; 
boolean silView = false;

float xrate = 0.035f; 
float yrate = 0.035f;

final int BONE = 0; 
final int BONELIMIT = 1; 
final int BONE_ROTATE = 13;
final int BONE_SCALE = 14;

ArrayList<CustomEvents> eventListeningObjects = new ArrayList<CustomEvents>();

int editMode = 0;

Camera currentCamera;
pArmature defaultArmature;
pBone currentlySelectedBone; 
boolean mouseOverUI = false;

public void setup() {
  
  size(1600, 900, P3D);
  //frameRate(60);
  center = new DVector(width/2, height/2);
  pseudoScene.DVector test = new pseudoScene.DVector(0,0,0);
  app = this;
  camera.rotateAboutZ(PI);
  currentCamera = new Camera(cameraPosition, camera, orbitRadius, zoom);
  currentCamera.frame();
  
  initializeGUI(); 
  
  defaultArmature = new pArmature(globalAxes, "default");
  //defaultArmature.createRootBone(
  pBone rootBone = (pBone) defaultArmature.getRootBone();
  println("via external getter: " + defaultArmature.getRootBone());
  new pBone(rootBone, new DVector(0, .4,0), new DVector(0,0,1), "initial", 5d, pBone.frameType.GLOBAL);
  /*newBone = new pBone(newBone, new DVector(0, 0.4,0), new DVector(0,0,1), "second", 4d, pBone.frameType.GLOBAL);*/
  armatures.add(defaultArmature);
  
}
DVector mouse3D;

public void draw() {
  background(160);
  hint(ENABLE_DEPTH_SORT);
  
  currentCamera.frame();
  if(mouseX < 200) mouseOverUI = true; else mouseOverUI = false;
  mouse3D = cameraPlaneIntersectThrough(new DVector(mouseX, mouseY), armatures.get(0).getRootBone().getBase(), new DVector(cameraPosition)); 
  
  drawFloorGrid();
  
  //println("drawing bones");
  drawBonesForArmature();

  //((pBone)defaultArmature.getRootBone()).drawChildBones(boneSelectionArray);
  hint(DISABLE_DEPTH_SORT);
  
  //println(defaultArmature.getBoneList());
  
  drawGUI();  
  
}
  
  

public void drawGUI() {
  
  hint(DISABLE_DEPTH_TEST);
  camera(0, 0, width, 0, 0, 0, 0, 1, 0);
  ortho(-width/2, width/2, -height/2, height/2, 1, 10000);
  camera(); 
  
  cp5.draw();
  hint(ENABLE_DEPTH_TEST);
  currentCamera.frame();
}


public void drawBonesForArmature() {
  boneSelectionArray.clear();
  //println(armatures.get(0).getBoneList().size());
  //for(AbstractBone root: armatures.get(0).getBoneList()) {
    //println(root);
    ((pBone)armatures.get(0).getRootBone()).drawChildBones(boneSelectionArray);  
  //}
}