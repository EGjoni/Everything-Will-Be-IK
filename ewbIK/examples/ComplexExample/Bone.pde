   public class pArmature extends ewbIK.AbstractArmature implements CustomEvents, Interaction { //<>// //<>//

    public pArmature(Axes origin, String name) {
      super(origin, name);   
    }

    @Override
    public void initializeRootBone(AbstractArmature armature, DVector tipHeading, DVector rollHeading, String inputTag,
        double boneHeight, Bone.frameType coordinateType) {
      println("creating rootBone"); 
      rootBone = new pBone(this, tipHeading, rollHeading, inputTag, boneHeight, coordinateType);
      println("rootBone created: " + rootBone);
      println("via getter: " + this.getRootBone()); 

    }

    public void mousePressed(){};
    public void mouseReleased(){};
    public void mouseDragged(){};
    public void keyPressed(){}; 
    public void keyReleased(){};
    public boolean isRotorRelevant(){
      return false;  
    };
    public boolean isTranslatorRelevant(){
      return false; 
    };

    public void interactionUpdated(int interactionType){};
  }


 
 
 public class pBone extends ewbIK.AbstractBone implements CustomEvents, Interaction {  //<>// //<>// //<>// //<>//

    boolean isSelected = false; 
    boolean draggingTip = false;
    boolean isVisible = true;

    Rotor boneRotor;

    SelectionBone selectionBone;
    SegmentedArmature segmentedArmature = null; 

    public pBone (pBone par, //parent pBone
        DVector tipHeading, //the orienational heading of this pBone (global vs relative coords specified in coordinateType)
        DVector rollHeading, //axial rotation heading of the pBone (it's z-axis) 
        String inputTag,   //some user specified name for the pBone, if desired 
        double inputBoneHeight, //bone length 
        frameType coordinateType              
        ) throws NullParentForBoneException {
      super(par, tipHeading, rollHeading, inputTag, inputBoneHeight, coordinateType);

      selectionBone = new SelectionBone(this);
      refreshBoneAppearance();
      boneRotor = new Rotor((Axes)localAxes, true, this);

      eventListeningObjects.add(this);
    }

    public pBone (pBone par, //parent bone
        DVector tipHeading, //the orienational heading of this bone (global vs relative coords specified in coordinateType)
        DVector rollHeading, //axial rotation heading of the bone (it's z-axis) 
        String inputTag,   //some user specified name for the bone, if desired 
        frameType coordinateType              
        ) throws NullParentForBoneException {
      super(par, tipHeading, rollHeading, inputTag, coordinateType);

      selectionBone = new SelectionBone(this);
      refreshBoneAppearance();
      boneRotor = new Rotor((Axes)localAxes, true, this);

      eventListeningObjects.add(this);
    }

    public pBone (pArmature parArma, 
        DVector tipHeading, 
        DVector rollHeading, 
        String inputTag, 
        double inputBoneHeight, 
        frameType coordinateType) 
            throws NullParentForBoneException {
      super(parArma, tipHeading, rollHeading, inputTag, inputBoneHeight, coordinateType);  

      selectionBone = new SelectionBone(this);
      refreshBoneAppearance();
      boneRotor = new Rotor((Axes)localAxes, true, this);

      eventListeningObjects.add(this);
    }

    public pBone (pArmature parArma, 
        DVector tipHeading, 
        DVector rollHeading, 
        String inputTag,  
        frameType coordinateType) 
            throws NullParentForBoneException {
      super(parArma, tipHeading, rollHeading, inputTag, coordinateType);        

      selectionBone = new SelectionBone(this);
      refreshBoneAppearance();
      boneRotor = new Rotor((Axes)localAxes, true, this);
      eventListeningObjects.add(this);
    }

    public void generateAxes(DVector origin, DVector x, DVector y, DVector z) {
      this.localAxes = new Axes(origin, x, y, z);
      localAxes.orthogonalize();
    }

    public boolean pinned() {
      return this.pinned;  
    }

    public pBone parent() {
      return (pBone)this.parent;  
    }

    public DVector pinnedTo() {
      return (DVector)this.pinnedTo;  
    }


    public void keyPressed(){} 
    public void keyReleased(){

      if(key == 'p' && editMode == BONE && this.isSelected && !cameraMode) {
        this.togglePin();
      }
    }



    public void mousePressed(){
      DVector mouse = new DVector(mouseX, mouseY);
      if(editMode == BONE && !cameraMode && mouseButton == LEFT && !mouseOverUI) {
        if(this.pinnedTo != null && this.pinned) {
          double distToMouse = mouse.dist(screenXY(this.getTip()));

          if(distToMouse < 10) {
            this.draggingTip = true; 
          }
        }
      }
      if(editMode == BONELIMIT && !cameraMode && mouseButton == RIGHT && this.isSelected && !mouseOverUI) {
        if(constraints == null) { 
          constraints = new Kusudama(this);
          ((Kusudama)constraints).isVisible = true;
        }
      }

    }
    public void mouseDragged(){
      DVector mouse = new DVector(mouseX, mouseY);
      if(editMode == BONE &&  this.isSelected && !cameraMode) {      
        if(mouseButton == RIGHT  && (
        this.constraints == null ||
          ((Kusudama)this.constraints).currentlySelectedConstraintPoint == null)) {
          drawTempChild(mouse);
        }
      } 
      if(editMode == BONE && !cameraMode && mouseButton == LEFT) {
        //println(this.pinnedTo + "   " + this.pinned +  "    " + this.draggingTip);
        if(this.pinnedTo != null && this.pinned && this.draggingTip) {
          this.setPin(cameraPlaneIntersectThrough(mouse, this.pinnedTo()));
          //this.parentArmature.segmentedArmature().returnSegmentTerminalNodes();
          this.parentArmature.solveIK(this, Math.toRadians(5d), 5);
        }
      }
    }
    public void mouseReleased(){
      if(editMode == BONE  && this.isSelected && !cameraMode) {
        if(mouseButton == RIGHT  && (
            this.constraints == null ||
              ((Kusudama)this.constraints).currentlySelectedConstraintPoint == null)) {
          childFromTempChild(new DVector(mouseX, mouseY));
        }
      }
      this.draggingTip = false; 
      currentlySelectedTip = null;

    }

    public boolean isRotorRelevant() {
      return (editMode == BONE && !cameraMode && mouseButton == LEFT && this.isSelected);
    }

    public boolean isTranslatorRelevant() {
      return false;
    }

    public void interactionUpdated(int interactionType) {
      if(interactionType == ROTATION) {
        //boolean[] inBounds = {true};     
        this.snapToConstraints();  
        this.boneRotor.tempAxes = (Axes)this.localAxes.getAbsoluteCopy();
        this.boneRotor.axes = (Axes)this.localAxes.getAbsoluteCopy();

      }
    }   


    public pBone deselect() {
      if(this.constraints == null || ((Kusudama)this.constraints).wasDragging == false){
        this.isSelected = false; 
        if(this.constraints != null) ((Kusudama)this.constraints).isVisible = false;
      } 
      return null;
    }

    public void select() {
      this.isSelected = true; 
      currentlySelectedBone = this;
      if(this.constraints != null) {
        ((Kusudama)constraints).isVisible = true;         
        constraintTypes.getItem(1).setState(constraints.isAxiallyConstrained());
        constraintTypes.getItem(0).setState(constraints.isOrientationallyConstrained());
      } else {
        constraintTypes.getItem(1).setState(false);
        constraintTypes.getItem(0).setState(false);
      }
    }

    public void deselectChildBones() {
      //recursively go through child bones until a selected one is found.
      isSelected = false;
      for (AbstractBone child : children) {
        ((pBone)child).deselectChildBones();
      }
    }

    public ArrayList<? extends AbstractBone> children( ){
      return this.children;  
    }

    public ArrayList<? extends AbstractBone>  freeChildren( ){
      return this.freeChildren;  
    }

    public ArrayList<?extends AbstractBone>  effectoredChildren( ){
      return this.effectoredChildren;  
    }

    public void drawMe() {
      //println(this);
      if (this.isSelected) {
        // strokeWeight(255);
        //this.snapToConstraints();
        boneRotor.enabled = true;
        boneRotor.drawMe((float)boneHeight/2, 2);
      } else boneRotor.enabled = false;
      strokeWeight(1);

      strokeWeight(1);

      strokeWeight(1);
      stroke(0);


      //if(workView) {




      Axes localAbsAxes = (Axes)this.localAxes;//.getAbsoluteCopy();
      //localAbsAxes.drawMe();

      DVector origin = this.getBase(); 
      DVector tip = this.getTip();
      stroke(0);
      if (this.isSelected) stroke(255);
      line(origin, 
          localAbsAxes.getGlobalOf(new DVector(0.1f*boneHeight, 0.1f*boneHeight, 0)));
      line(tip, 
          localAbsAxes.getGlobalOf(new DVector(0.1f*boneHeight, 0.1f*boneHeight, 0)));

      line(origin, 
          localAbsAxes.getGlobalOf(new DVector(-0.1f*boneHeight, 0.1f*boneHeight, 0)));
      line(tip, 
          localAbsAxes.getGlobalOf(new DVector(-0.1f*boneHeight, 0.1f*boneHeight, 0)));

      line(origin, 
          localAbsAxes.getGlobalOf(new DVector(0, 0.1f*boneHeight, -0.1f*boneHeight)));
      line(tip, 
          localAbsAxes.getGlobalOf(new DVector(0, 0.1f*boneHeight, -0.1f*boneHeight)));


      line(origin, 
          localAbsAxes.getGlobalOf(new DVector(0, 0.1f*boneHeight, 0.1f*boneHeight)));
      line(tip, 
          localAbsAxes.getGlobalOf(new DVector(0, 0.1f*boneHeight, 0.1f*boneHeight)));   

      strokeWeight(5); 
      point(this.localAxes.origin());

      if (this.pinned) {
        strokeWeight(21); 
        stroke(0);
        point(this.getTip());
        strokeWeight(18); 
        stroke(255, 255, 255);
        point(this.getTip()); 
        strokeWeight(15); 
        stroke(230, 0, 0);
        point(this.getTip());

        stroke(25);
        stroke(150,150,10);
        point(this.pinnedTo);
        strokeWeight(3); 
        line(this.getTip(), this.pinnedTo);
      }

      if(constraints != null ) {
        ((Kusudama)constraints).drawMe();  
      }
    }

    public void drawChildBones(ArrayList<SelectionBone> boneSelectionArray) {
      //recursively go through child bones until a selected one is found.
      drawMe();
      selectionBone.updateShapes();
      boneSelectionArray.add(selectionBone);
      for (AbstractBone child : this.children) {
        ((pBone)child).drawChildBones(boneSelectionArray);
      }
    }


    public void drawTempChild(DVector mouse) {  
      stroke(0);
      strokeWeight(10);
      line(this.getTip(), cameraPlaneIntersectThrough(mouse, this.getTip()));
    }

    public void childFromTempChild(DVector mouse) {
      new pBone(this,  cameraPlaneIntersectThrough(mouse, this.getTip()), this.localAxes().z().p2,  null, pBone.frameType.GLOBAL);
    }

    @Override
    public Axes localAxes() {
      return (Axes)this.localAxes;
    }

    private void refreshBoneAppearance() {
      DVector tempTip = this.getTip();
      tempTip.sub(this.getBase());
    }


    public void drawPinned() {
      stroke(150, 150, 0);
      strokeWeight(25); 
      if (this.pinned) {
        point(this.pinnedTo);
      }

      for (AbstractBone effectoredChild : effectoredChildren) {
        ((pBone)effectoredChild).drawPinned();
      }
    } 

  }

public class SelectionBone implements  CustomEvents {
    ArrayList<java.awt.Polygon> display = new ArrayList<java.awt.Polygon>(); 
    pBone bone; 

    public SelectionBone(pBone inBone) {
      this.bone = inBone;
      eventListeningObjects.add(this);
    }

    public void updateShapes() {
      display.clear();   
      java.awt.Polygon p1 = new java.awt.Polygon();
      java.awt.Polygon p2 = new java.awt.Polygon();
      java.awt.Polygon p3 = new java.awt.Polygon(); 

      DVector base = bone.getBase(); 
      DVector tip = bone.getTip();
      Axes localAbsAxes = bone.localAxes();
      DVector x1 = localAbsAxes.getGlobalOf(new DVector(-0.1f*bone.getBoneHeight(), 0.1f*bone.getBoneHeight(), 0)); 
      DVector x2 = localAbsAxes.getGlobalOf(new DVector(0.1f*bone.getBoneHeight(), 0.1f*bone.getBoneHeight(), 0)); 
      DVector z1 = localAbsAxes.getGlobalOf(new DVector(0, 0.1f*bone.getBoneHeight(), 0.1f*bone.getBoneHeight()));
      DVector z2 = localAbsAxes.getGlobalOf(new DVector(0, 0.1f*bone.getBoneHeight(), -0.1f*bone.getBoneHeight()));


      //bone XY-Plane
      p1.addPoint((int)screenX(base), (int)screenY(base));
      p1.addPoint((int)screenX(x1), (int)screenY(x1)); 
      p1.addPoint((int)screenX(tip), (int)screenY(tip));
      p1.addPoint((int)screenX(x2), (int)screenY(x2)); 

      //bone YZ Plane
      p2.addPoint((int)screenX(base), (int)screenY(base));
      p2.addPoint((int)screenX(z1), (int)screenY(z1)); 
      p2.addPoint((int)screenX(tip), (int)screenY(tip));
      p2.addPoint((int)screenX(z2), (int)screenY(z2)); 

      //bone XZ Plane
      p3.addPoint((int)screenX(z1), (int)screenY(z1));
      p3.addPoint((int)screenX(x1), (int)screenY(x1)); 
      p3.addPoint((int)screenX(z2), (int)screenY(z2));
      p3.addPoint((int)screenX(x2), (int)screenY(x2)); 

      display.add(p1); 
      display.add(p2);
      display.add(p3);
    }


    public void mousePressed(){    
    };
    public void mouseReleased(){
      if(mouseButton == LEFT && !cameraMode && !mouseOverUI) {
        //println("clickTestery "); 
        boolean contains =false; 
        for(java.awt.Polygon boneDisplay: display) {
          if(boneDisplay.contains(mouseX, mouseY)) {
            contains = true; 
          }
        }


        if(contains) { 

          if(editMode == BONE && 
            (bone.constraints == null || ((Kusudama)bone.constraints).currentlySelectedConstraintPoint == null)) { 
              bone.select(); 
          }

        } else {
          if(editMode == BONE &&
           (bone.constraints == null || ((Kusudama)bone.constraints).currentlySelectedConstraintPoint == null)){
            //println("bone.constraints: " +bone.constraints); 
            if((bone.constraints != null))
              //println("currentlySelectedConstraintPoint:  " + ((Kusudama)bone.constraints).currentlySelectedConstraintPoint);
            bone.deselect();
          }
            
        }

      }
    }
    public void mouseDragged(){};
    public void keyPressed(){}; 
    public void keyReleased(){};
  }
  
  
  

  