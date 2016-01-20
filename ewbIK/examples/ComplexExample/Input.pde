Accordion accordion;


  public void initializeGUI() {
    cp5 = new ControlP5(this); 


    initializeBoneControls();
    //accordion.open(0);
    cp5.setAutoDraw(false);
  }


  public void controlEvent(ControlEvent theEvent) {


    if(theEvent.isFrom(mode)) {
      editMode =  (int)theEvent.getGroup().getValue(); 
      if(editMode == BONE) {
        constraintTypes.hide();  
      } else {
        constraintTypes.show();  
      }
    }

    if(theEvent.isFrom(constraintTypes)) {
      if(currentlySelectedBone != null) {
        if(currentlySelectedBone.constraints == null) {
          currentlySelectedBone.constraints = new Kusudama(currentlySelectedBone);  
          ((Kusudama)currentlySelectedBone.constraints).isVisible = true;
        } else {
          if(constraintTypes.getItem(0).getState()) currentlySelectedBone.constraints.enableAxialLimits();
          if(!constraintTypes.getItem(0).getState()) currentlySelectedBone.constraints.disableAxialLimits();

          if(constraintTypes.getItem(1).getState()) currentlySelectedBone.constraints.enableOrientationalLimits();
          if(!constraintTypes.getItem(1).getState())  currentlySelectedBone.constraints.disableOrientationalLimits();
        }
      }
    }


  }

  Group boneControls; 
  Group bone_transform_controls, bone_limit_controls;
  Button new_armature;
  Button rotate_bone;
  Button pin_bone;
  CheckBox constraintTypes;

  CheckBox visibility;

  Group modes; 

  RadioButton mode;

  Knob currentlySelected_limit_radius; 

  public void initializeBoneControls() {
    boneControls = cp5.addGroup("Bone")
        .setBackgroundColor(color(0,60))
        .setBackgroundHeight(height)
        .setWidth(200)
        .setPosition(0,0)
        .disableCollapse();


    constraintTypes = cp5.addCheckBox("checkBox")
        .setPosition(5, 200)
        .setSize(30, 20)
        .setItemsPerRow(1)
        .setSpacingColumn(50)
        .addItem("Axially Constrained", 1)
        .addItem("Orientationally Constrained", 2)
        .moveTo(boneControls);
    ;


  }


public void mousePressed(MouseEvent event) {
  if(cp5.getMouseOverList().size() < 1) {
    try{
      for(CustomEvents ce: eventListeningObjects) {
      
        ce.mousePressed();
      } 
    } catch (Exception e) {      
    }
  
  }
  
  
}

public void mouseDragged() {
  try{
  for(CustomEvents ce: eventListeningObjects) {
    ce.mouseDragged();
  }
  }
  catch (Exception e) {
      
  }
}

public void mouseReleased() {

  //CustomEvents ce;
  for(int i=0; i<eventListeningObjects.size(); i++) {
    eventListeningObjects.get(i).mouseReleased();
  }
  if (mouseButton == RIGHT ) {
    mouseRight = false;
    mouseRightDragBegin = null;

  }    
  if (mouseButton == LEFT) {
    
  }

  if (mouseButton == CENTER ) {
  }
}

public void mouseWheel(MouseEvent event) {
  float e = event.getCount();
  if (cameraMode) {
    currentCamera.dollyCamera(e);
  } 
}


public void keyPressed() {
  println(key);
  for(CustomEvents ce: eventListeningObjects) {    
      ce.keyPressed();
  } 
  
  switch (key) {
  case 'w': 
    w = true;
    break;
  case 's': //s = true;
    break;
  case 'a': 
    a = true;             
    break;
  case 'A': 
    break;
  case 'd': 
    d = true;
    break;
  /*case 't': 
    t = true;
    break;*/   
  case 'g': 
    g = true; 
    break; 
  case 'b': 
    b = true; 
    break;
  case 'q': 
    q = true; 
    break; 
  case 'e': 
    e = true; 
    break;
  case 't':    
    break;
  case 'y':
    break;
  }
  if(keyCode == 16) {
    cameraMode=true;
  }
}

public void keyReleased() {
  
  for(CustomEvents ce: eventListeningObjects) {    
      ce.keyReleased();
  } 

  switch (key) {
  case 'w': 
    w = false;
    break;
  case 's':
    break;
  case 'a': 
    a = false;
    break;
  case 'd': 
    d = false;
    break;
  case 'l': 
    if (editMode == BONE) {
      editMode = BONELIMIT;
    } else { 
      editMode = BONE;
    }
    slow = !slow;
    break;
  case 'g': 
    g = false; 
    break; 
  case 'b': 
    b = false; 
    break;
  case 'q': 
    q = false; 
    break; 
  case 'e': 
    e = false; 
    break;
    
  }
  if(keyCode == 16) {
    shift = false;
    cameraMode = false;
  }
 
  
}