public class Rotor implements CustomEvents {
  Axes axes;
  Axes originalAxes; 
  Axes tempAxes; 
  
  Rot lastRotation;
  DVector tempHeading = null;
  DVector[] circle = new DVector[100];
  double lastDrawScale = -1;
  boolean enabled = false;

  DVector[] XYHandleScreenCoords = new DVector[100];
  DVector[] XZHandleScreenCoords = new DVector[100];
  DVector[] YZHandleScreenCoords = new DVector[100];
  
  DVector[] XYHandle = new DVector[100];
  DVector[] XZHandle = new DVector[100];
  DVector[] YZHandle = new DVector[100];
 
 
  final int XY = 0; 
  final int XZ = 1;
  final int YZ = 2;

  public boolean xyDrag = false; 
  public boolean xzDrag = false; 
  public boolean yzDrag = false; 
  public boolean autoRotate = true;
  
  boolean XYHovered = false; 
  boolean XZHovered = false; 
  boolean YZHovered = false; 
  
  Method callback = null;
  Object caller = null;
  Interaction attachedTo = null;
  
  Runnable relevanceCallback;
  

  public Rotor() {
    
  }

 public Rotor(Axes inputAxes, boolean autoRot) {
    this.axes = (Axes)inputAxes.getAbsoluteCopy();
    this.originalAxes = inputAxes;
    makeCircles();
    this.autoRotate = autoRot;
    //app.registerMouseEvent(this);
    //eventListeningObjects.put(this, this);
    eventListeningObjects.add(this);
  }
  
  public Rotor(Axes inputAxes, boolean autoRot, Interaction attachedToObject) {
    this.axes = (Axes)inputAxes.getAbsoluteCopy();
    this.originalAxes = inputAxes;
    makeCircles();
    this.autoRotate = autoRot;
    this.attachedTo = attachedToObject;
    //app.registerMouseEvent(this);
    //eventListeningObjects.put(this, this);
    eventListeningObjects.add(this);
    
  }
  
  /*public Rotor(Axes inputAxes, Axes inRelativeToAxes, boolean autoRot) {
    this.axes = new Axes(inputAxes.x, inputAxes.y, inputAxes.z);
    this.originalAxes = inputAxes;
    makeCircles();
    this.autoRotate = autoRot;
    this.relativeToAxes = inRelativeToAxes;
    //app.registerMouseEvent(this);
    //eventListeningObjects.put(this, this);
    eventListeningObjects.add(this);
  }*/
  
  public Rotor(Axes inputAxes, boolean autoRot, Method inCallback, Object inCaller) {
    this.axes = new Axes(inputAxes.x(), inputAxes.y(), inputAxes.z());
    this.originalAxes = inputAxes;
    makeCircles();
    this.autoRotate = autoRot;
    this.callback = inCallback;
    this.caller = inCaller;
    //app.registerMouseEvent(this);
    //eventListeningObjects.put(this, this);
    eventListeningObjects.add(this);
  }
  
  public void keyPressed(){} 
  public void keyReleased(){}
     
  public void mousePressed() {
    //println(attachedTo + " " + attachedTo.isRotorRelevant());
    if(attachedTo == null || attachedTo.isRotorRelevant()) {
      testClick();  
    }
  }
  
  int lastFrame = frameCount;
  
  public void mouseDragged() {
    if(attachedTo == null || attachedTo.isRotorRelevant()) {
      if((enabled || attachedTo.isRotorRelevant())) { 
        //if(lastFrame < frameCount) 
        clickDrag();  
        lastFrame = frameCount;
        attachedTo.interactionUpdated(ROTATION);
      }
    }
  }
  
  public void mouseReleased() {
    if(attachedTo == null || attachedTo.isRotorRelevant()) {
      if(enabled || attachedTo.isRotorRelevant()) {
        mouseRelease();  
        attachedTo.interactionUpdated(ROTATION);
      }
    }
  }
  
  public void updateHandleScreenCoords() {
    Axes drawAxes = originalAxes;
    for(int i = 0; i<circle.length; i++) {
      XYHandle[i].normalize();
      XYHandleScreenCoords[i] = screenXY(drawAxes.getGlobalOf(DVector.mult(XYHandle[i], lastDrawScale)));
    }  
    for(int i = 0; i<circle.length; i++) {
      XZHandle[i].normalize();
      XZHandleScreenCoords[i] = screenXY(drawAxes.getGlobalOf(DVector.mult(XZHandle[i], lastDrawScale)));
    }  
    for(int i = 0; i<circle.length; i++) {
      YZHandle[i].normalize();
      YZHandleScreenCoords[i] = screenXY(drawAxes.getGlobalOf(DVector.mult(YZHandle[i], lastDrawScale)));
    }  
  }
  
  public void testMouseOver() {
    if(!xyDrag && !xzDrag && !yzDrag) {
    DVector mouse = new DVector(mouseX, mouseY);
    XYHovered = false; 
    XZHovered = false; 
    YZHovered = false;
    for(int i = 0; i<XYHandleScreenCoords.length; i++) {
        if(mouse.dist(XYHandleScreenCoords[i]) < 5) {
          XYHovered = true;
          break;
        }
    }
    if(!XYHovered) {
      for(int i = 0; i<XZHandleScreenCoords.length; i++) {
        if(mouse.dist(XZHandleScreenCoords[i]) < 5) {
          XZHovered = true;
          break;
        }
      }
    }    
    if(!XYHovered && !XZHovered) {
      for(int i = 0; i<YZHandleScreenCoords.length; i++) {
        if(mouse.dist(YZHandleScreenCoords[i]) < 5) {
          YZHovered = true;
          break;
        }
      }
    }
    }
    
  }

  public void makeCircles() {
    DVector radius = new DVector(1, 0);
    for (int i = 0; i<circle.length; i++) {
      radius.rotate(TAU/circle.length);
      XYHandle[i] = new DVector(radius.x, radius.y, 0);
      XZHandle[i] = new DVector(radius.x, 0, radius.y); 
      YZHandle[i] = new DVector(0, radius.x, radius.y);
    }
  }  

  public int testClick() {
    axes = (Axes)originalAxes.getAbsolute();
    if (enabled || attachedTo.isRotorRelevant()) {
      //double scale = lastDrawScale;
      DVector mouseXY = mousePlaneIntersect(axes.origin(), axes.x().p2, axes.y().p2);
      DVector mouseXZ = mousePlaneIntersect(axes.origin(), axes.x().p2, axes.z().p2);
      DVector mouseYZ = mousePlaneIntersect(axes.origin(), axes.z().p2, axes.y().p2);

      //println("rotorClick");
      if (tempHeading == null) {
        //double XYs = Math.abs(axes.getLocalOf(mouseXY).mag() - scale);
        //double XZs = Math.abs(axes.getLocalOf(mouseXZ).mag() - scale*1.05);
        //double YZs = Math.abs(axes.getLocalOf(mouseYZ).mag() - scale*1.1);
        if(XYHovered) {//if (XYs < XZs && XYs < YZs && XYs < scale*.2) {
          tempAxes = new Axes(axes.x(), axes.y(), axes.z());
          tempHeading = DVector.sub(mouseXY, axes.origin());
          xyDrag = true;
          return XY;
        } 
        if(XZHovered) { //if (XZs < XYs && XZs < YZs && XZs < scale*.2) {
          tempAxes = new Axes(axes.x(), axes.y(), axes.z());
          tempHeading = DVector.sub(mouseXZ, axes.origin());
          xzDrag = true;
          return XZ;
        } 
        if(YZHovered) { //if ( YZs< XZ && YZs < XY && YZs < scale*.2) {
          tempAxes = new Axes(axes.x(), axes.y(), axes.z());
          tempHeading = DVector.sub(mouseYZ, axes.origin());
          yzDrag = true;
          return YZ;
        } else return -1;
      } else { 
        return -1;
      }
    } else return -1;
  }
  
  /*public int relativeTestClick() {
    
    Axes relativeAxes = originalAxes.relativeTo(relativeToAxes);
    
    DVector mouseXY = mousePlaneIntersect(relativeAxes.origin, relativeAxes.x.p2, relativeAxes.y.p2);
    DVector mouseXZ = mousePlaneIntersect(relativeAxes.origin, relativeAxes.x.p2, relativeAxes.z.p2);
    DVector mouseYZ = mousePlaneIntersect(relativeAxes.origin, relativeAxes.z.p2, relativeAxes.y.p2);
    
    if(XYHovered) { 
          tempAxes = new Axes(relativeAxes.x, relativeAxes.y, relativeAxes.z);
          tempHeading = DVector.sub(mouseXY, relativeAxes.origin);
          xyDrag = true;
          return XY;
        } else if (XZHovered) {
          tempAxes = new Axes(relativeAxes.x, relativeAxes.y, relativeAxes.z);
          tempHeading = DVector.sub(mouseXZ, relativeAxes.origin);
          xzDrag = true;
          return XZ;
        } else if (YZHovered) {
          tempAxes = new Axes(relativeAxes.x,relativeAxes.y, relativeAxes.z);
          tempHeading = DVector.sub(mouseYZ, relativeAxes.origin);
          yzDrag = true;
          return YZ;
        } else return -1;
    
  }*/

  public int clickDrag() {
    /*println("clickDragging");
    println(xyDrag);
    println(xzDrag);
    println(yzDrag);
    println(callback);*/
    
    /*if (autoRotate) {
       println("rotating");
       strokeWeight(2);
       axes.drawMe();
       strokeWeight(5);
       originalAxes.drawMe();
       originalAxes.rotateTo(new Rot(originalAxes.y.heading(), axes.y.heading())); 
       originalAxes.rotateTo(new Rot(originalAxes.x.heading(), axes.x.heading()));
       originalAxes.translateTo(axes.origin);
    }*/
    if (xyDrag) {
      DVector mouseXY = DVector.sub(mousePlaneIntersect(tempAxes.origin(), tempAxes.x().p2, tempAxes.y().p2), tempAxes.origin());      
      axes.lorigin = tempAxes.origin().copy();
      axes.lx = tempAxes.lx.copy(); 
      axes.ly = tempAxes.ly.copy(); 
      axes.lz = tempAxes.lz.copy();
      Rot rotate = new Rot(tempHeading, mouseXY);
      lastRotation = rotate;
      int dir = (rotate.getAxis().dot(tempAxes.z().heading()) < 0) ? -1 : 1; 
      if(autoRotate) {
          originalAxes.rotateAboutZ(dir*rotate.getAngle());
          tempHeading = mouseXY;
      } else {
        if(callback != null) {
          println("not null");
          tempHeading = mouseXY;
          try {
          Object[] parameters = new Object[1];
          parameters[0] = rotate;
          callback.invoke(caller, parameters);
          } catch (Exception e) {
            println("exception");
          }
        }
        axes.rotateTo(new Rot(DVector.mult(rotate.getAxis(), 1), rotate.getAngle()));
      }
      return XY;
    } else if (xzDrag) {
      DVector mouseXZ = DVector.sub(mousePlaneIntersect(tempAxes.origin(), tempAxes.z().p2, tempAxes.x().p2), tempAxes.origin());
      axes.lorigin = tempAxes.lorigin.copy();
      axes.lx = tempAxes.lx.copy(); 
      axes.ly = tempAxes.ly.copy(); 
      axes.lz = tempAxes.lz.copy();
      Rot rotate = new Rot(tempHeading, mouseXZ);
      lastRotation = rotate;
       int dir = (rotate.getAxis().dot(tempAxes.ly.heading()) < 0) ? -1 : 1; 
      if(autoRotate) {
          originalAxes.rotateAboutY(dir*rotate.getAngle());
          tempHeading = mouseXZ;
      } else {
        axes.rotateTo(new Rot(DVector.mult(rotate.getAxis(), 1), rotate.getAngle()));
      }
      return XZ;
    } else if (yzDrag) {
      DVector mouseYZ = DVector.sub(mousePlaneIntersect(tempAxes.origin(), tempAxes.z().p2, tempAxes.y().p2), tempAxes.origin());
      axes.lorigin = tempAxes.lorigin.copy();
      axes.lx = tempAxes.x().copy(); 
      axes.ly = tempAxes.y().copy(); 
      axes.lz = tempAxes.z().copy();
      Rot rotate = new Rot(tempHeading, mouseYZ);
      lastRotation = rotate;
      int dir = (rotate.getAxis().dot(tempAxes.x().heading()) < 0) ? -1 : 1; 
      if(autoRotate) {
          originalAxes.rotateAboutX(dir*rotate.getAngle());
          tempHeading = mouseYZ;
      } else {
        axes.rotateTo(new Rot(DVector.mult(rotate.getAxis(), 1), rotate.getAngle()));
      }
      return YZ;
    } else {
      return -1;
    }
    
  }

  public void mouseRelease() {
    axes = new Axes(originalAxes.x(), originalAxes.y(), originalAxes.z());
    tempAxes = new Axes(axes.x(), axes.y(), axes.z()); 
    tempHeading = null; 
    makeCircles();

    xyDrag = false;
    xzDrag = false;
    yzDrag = false;
  }
  
  public void drawMeIfEnabled(float scale, float strokeWeight) {
    if(enabled || attachedTo.isRotorRelevant()) drawMe(scale, strokeWeight);  
  }

  public void drawMe(float scale, float strokeWeight) {
    lastDrawScale = scale;
    Axes drawAxes = originalAxes;
    updateHandleScreenCoords();
    testMouseOver(); 
    
    
    println("Rot draw");
    stroke(0, 0, 255);
    strokeWeight(strokeWeight); 
    if (XYHovered) { 
      stroke(100, 100, 255);
      strokeWeight(strokeWeight*1.5f);
    }
    noFill();
    beginShape();
    for (int i = 0; i<circle.length; i++) {
      DVector point = drawAxes.getGlobalOf(DVector.mult(XYHandle[i], scale));
      vertex(point.x, point.y, point.z);
    }
    endShape();


    stroke(255, 0, 0);
    strokeWeight(strokeWeight);
    if (XZHovered) { 
      stroke(255, 100, 100);
      strokeWeight(strokeWeight*1.5f);
    }
    beginShape();
    for (int i = 0; i<circle.length; i++) {
      DVector point = drawAxes.getGlobalOf(DVector.mult(XZHandle[i], scale));
      
      vertex(point.x, point.y, point.z);
    }
    endShape();

    stroke(0, 255, 0);
    strokeWeight(strokeWeight);
    if (YZHovered) {
      stroke(100, 255, 100);
      strokeWeight(strokeWeight*1.5f);
    }
    beginShape();
    for (int i = 0; i<circle.length; i++) {      
      DVector point = drawAxes.getGlobalOf(DVector.mult(YZHandle[i], scale));
      vertex(point.x, point.y, point.z);
    }
    endShape();
  }

  
}


public class Translator implements CustomEvents {
  Axes originalAxes; 
  Interaction attachedTo = null;
  boolean enabled = false;
  
  DVector xHandle, yHandle, zHandle, centerHandle; 
  DVector xH, yH, zH;
  
  final int X = 0; 
  final int Y = 1;
  final int Z = 2;
  
  int selected = -1;
  DVector selectedFrom;
  
  public Translator(Axes inputAxes, Interaction attachedObject) {
    this.originalAxes = inputAxes;
    eventListeningObjects.add(this);
    this.attachedTo = attachedObject;
  }
  
  
  public void updateScreenCoords() {
    updateScreenCoords(0.5);
  }
  
  public void updateScreenCoords(double scale) {
    
    xHandle = screenXY(DVector.add(new DVector(scale,0,0), originalAxes.origin())); 
    yHandle = screenXY(DVector.add(new DVector(0,scale,0), originalAxes.origin())); 
    zHandle = screenXY(DVector.add(new DVector(0,0,scale), originalAxes.origin())); 
    
    xH = DVector.add(new DVector(scale,0,0), originalAxes.origin()); 
    yH = DVector.add(new DVector(0,scale,0), originalAxes.origin()); 
    zH = DVector.add(new DVector(0,0,scale), originalAxes.origin()); 
    
    centerHandle = screenXY(originalAxes.origin());
  }
  
  public void drawMeIfEnabled(double scale) {
    if(attachedTo.isTranslatorRelevant()) drawMe(scale);  
  }
  
  public void drawMe(){
    drawMe(0.5);  
  }
  public void drawMe(double scale) {
    updateScreenCoords(scale);
    
    strokeWeight(40);
    stroke(0); 
    point(originalAxes.origin());
    strokeWeight(35);
    stroke(255); 
    point(originalAxes.origin());
        
    strokeWeight(2);
    stroke(0,255,0);
    line(xH, originalAxes.origin()); 
    stroke(255,0,0);
    line(yH, originalAxes.origin()); 
    stroke(0, 0, 255);
    line(zH, originalAxes.origin()); 
    
    strokeWeight(35);
    stroke(0,255,0);
    point(xH);
    stroke(255,0,0);
    point(yH);
    stroke(0,0,255);
    point(zH);
    
    
  }
  
  public void selectHandle() {
    double xDistance = -1;
    double yDistance = -1;
    double zDistance = -1;
    
    DVector mouse = new DVector(mouseX, mouseY);
    
    int toSelect = -1;
    
    if(mouse.dist(xHandle) <20) {
        xDistance = xH.dist(new DVector(cameraPosition)); 
        toSelect = X;
        selectedFrom = xH.copy();
    }
    if(mouse.dist(yHandle) <20) {
        yDistance = yH.dist(new DVector(cameraPosition)); 
        if(xDistance == -1 || yDistance < xDistance) { 
          xDistance = -1;
          toSelect = Y;
          selectedFrom = yH.copy();
      }
    }
    if(mouse.dist(zHandle) <20) {
        zDistance = zH.dist(new DVector(cameraPosition));  
        if((xDistance == -1 && yDistance == -1)) { 
          selectedFrom = zH.copy();
          toSelect = Z;
        }
        else if((yDistance != -1 && zDistance < yDistance)) { 
          selectedFrom = zH.copy();
          toSelect = Z;
        }
        else if((xDistance != -1 && zDistance < xDistance)) { 
          selectedFrom = zH.copy();
          toSelect = Z;
        }
    }
    
    
    selected = toSelect;    
  }
  
  public void dragHandle() {
    
    
    switch(selected) {
       case X: { 
         DVector mouse3D = cameraPlaneIntersectThrough(new DVector(mouseX, mouseY), selectedFrom, new DVector(cameraPosition));
         
         DVector moveTo = new DVector(mouse3D.x, xH.y, xH.z);
         
         Ray translate = new Ray(xH, moveTo);
         originalAxes.translateByGlobal(translate.heading());
         break;
       } 
       case Y: {
         DVector mouse3D = cameraPlaneIntersectThrough(new DVector(mouseX, mouseY), selectedFrom, new DVector(cameraPosition));
         
         DVector moveTo = new DVector(yH.x, mouse3D.y, yH.z);
         
         Ray translate = new Ray(yH, moveTo);
         originalAxes.translateByGlobal(translate.heading());
         break;
       }
       case Z: {
         DVector mouse3D = cameraPlaneIntersectThrough(new DVector(mouseX, mouseY), selectedFrom, new DVector(cameraPosition));
         
         DVector moveTo = new DVector(zH.x, zH.y, mouse3D.z);
         
         Ray translate = new Ray(zH, moveTo);
         originalAxes.translateByGlobal(translate.heading());
         break;
       }
    }
  }
  public void keyPressed(){} 
  public void keyReleased(){}
  
  public void mousePressed() {
    if(attachedTo.isTranslatorRelevant()) selectHandle();    
  }
  public void mouseDragged() {
    if(attachedTo.isTranslatorRelevant()) dragHandle(); 
  }
  public void mouseReleased() {
    if(attachedTo.isTranslatorRelevant()) {
      selected = -1;  
    }
  }
  
}

interface CustomEvents {
  public void mousePressed();
  public void mouseReleased();
  public void mouseDragged();
  public void keyPressed(); 
  public void keyReleased();
}

interface Interaction {
  public boolean isRotorRelevant();
  public boolean isTranslatorRelevant();
  public void interactionUpdated(int interactionType);
  
}

public void drawGreatArc(DVector A, DVector B, Axes frame, double granularity, boolean fullLine) {
  Rot rotation = new Rot(A, B); 
  DVector axis = rotation.getAxis();
  double angle = rotation.getAngle();
  angle /= granularity;
  rotation = new Rot(axis, angle);

  DVector point = A.copy(); 
  DVector lastPoint = A.copy();
  for (int i = 0; i< granularity; i++) {
    point = rotation.applyTo(lastPoint);
    if(fullLine) line(frame.getGlobalOf(lastPoint), frame.getGlobalOf(point));
    else point(frame.getGlobalOf(lastPoint));
    lastPoint = point;
  }
}

public void drawGreatArc(DVector A, DVector B, Axes frame, double granularity) {
  drawGreatArc(A, B, frame, granularity, true);  
}

public void drawMinorArc(double radius, double magnitude, DVector axis, DVector A, DVector B, double granularity, Axes frame, Kusudama constraints, boolean hideInBounds) {
  //DVector tempPlane = new DVector(random(1), random(1), random(1)).normalize();
  //println("axis, "+axis+ "A, " + A+" B, " + B+ " frame, "+frame + " kusudama, "+constraints);
  DVector normal = (axis.copy().normalize()).cross(A.copy());
  DVector radialPoint = new Rot(normal, radius-radians(0.05)).applyTo(axis);
  DVector lastRadialPoint = radialPoint.copy();
  Rot circle = new Rot(axis, DVector.angleBetween(A, B)/granularity);
  boolean[] inBounds = {false};
 // println("arcDraw");
  for(int i = 0; i<=granularity; i++) {
    inBounds[0] = false;
    strokeWeight(2);
    stroke(255);
    radialPoint = circle.applyTo(radialPoint);
    constraints.pointInLimits(frame.getGlobalOf(radialPoint), inBounds);
    if(!inBounds[0]) {
    DVector radDraw = DVector.mult(radialPoint.copy().normalize(), magnitude);
    DVector lastDraw = DVector.mult(lastRadialPoint.copy().normalize(), magnitude);
    line(frame.getGlobalOf(lastDraw), frame.getGlobalOf(radDraw));
    noStroke();
    fill(0, 0, 0, 50); 
    beginShape();
      vertex(frame.origin()); 
      vertex(frame.getGlobalOf(lastDraw));
      vertex(frame.getGlobalOf(radDraw));
    endShape();
    }
    lastRadialPoint = radialPoint.copy();
  }
  
}


public void tempBoneToCurrentArmature(DVector mouse) {
  if(currentlySelectedArmature == -1) {   
  } else {
    if(currentlySelectedBone!=null) {
      currentlySelectedBone.drawTempChild(mouse);//, camera.y().heading());
    }
  }
}



public void resetCam() {
  camera(new DVector(cameraPosition), camera.origin(), camera.y().heading());
  cameraPosition = camera.z().getScaledTo(orbitRadius).toPVec();
}

public DVector mousePlaneIntersect(DVector planePoint1, DVector planePoint2, DVector planePoint3) {
  Ray camToViewingPlaneIntersect = new Ray(new DVector(cameraPosition), coordinatesOn3DViewingPlane(new DVector(mouseX, mouseY)));  
  return(ewbIK.G.intersectTest(camToViewingPlaneIntersect, planePoint1, planePoint2, planePoint3));
}

public DVector pointPlaneIntersect(DVector planePoint1, DVector planePoint2, DVector planePoint3, DVector inputPoint) {
  Ray camToViewingPlaneIntersect = new Ray(new DVector(cameraPosition), coordinatesOn3DViewingPlane(inputPoint));  
  return(ewbIK.G.intersectTest(camToViewingPlaneIntersect, planePoint1, planePoint2, planePoint3));
}

public DVector cameraPlaneIntersectThrough(DVector point, DVector planeThrough) {
  Ray tempCamXonPlane = new Ray(planeThrough, null);
  tempCamXonPlane.heading(camera.x().heading());
  Ray tempCamYonPlane = new Ray(planeThrough, null);
  tempCamYonPlane.heading(camera.y().heading());
  Ray camToViewingPlaneIntersect = new Ray(new DVector(cameraPosition), coordinatesOn3DViewingPlane(point));  

  return(ewbIK.G.intersectTest(camToViewingPlaneIntersect, tempCamXonPlane.p2, tempCamYonPlane.p2, planeThrough)); 
}

public DVector cameraPlaneIntersectThrough(DVector point, DVector planeThrough,  DVector cameraPosition) {
  DVector adjustedPlaneThrough = planeThrough.copy();
  if(planeThrough.x ==0 && planeThrough.y == 0 && planeThrough.z == 0) {
      adjustedPlaneThrough.y = 0.000001f;
  }
  DVector planeX = cameraPosition.cross(adjustedPlaneThrough); 
  DVector planeY = cameraPosition.cross(planeX);
  
  
  
  Ray tempCamXonPlane = new Ray(planeThrough, null);
  tempCamXonPlane.heading(planeX);
  Ray tempCamYonPlane = new Ray(planeThrough, null);
  tempCamYonPlane.heading(planeY);
  Ray camToViewingPlaneIntersect = new Ray(cameraPosition, coordinatesOn3DViewingPlane(point));  

  return(ewbIK.G.intersectTest(camToViewingPlaneIntersect, tempCamXonPlane.p2, tempCamYonPlane.p2, adjustedPlaneThrough)); 
}

public DVector cameraPlaneIntersectThrough(DVector point, DVector planeThrough, Axes camAxes, DVector inputCameraPosition) {
  DVector adjustedPlaneThrough = planeThrough.copy();
  if(planeThrough.x ==0 && planeThrough.y == 0 && planeThrough.z == 0) {
      adjustedPlaneThrough.y = 0.000001f;
  }
  DVector planeX = inputCameraPosition.cross(adjustedPlaneThrough); 
  DVector planeY = inputCameraPosition.cross(planeX);
  
  
  
  Ray tempCamXonPlane = new Ray(planeThrough, null);
  tempCamXonPlane.heading(planeX);
  Ray tempCamYonPlane = new Ray(planeThrough, null);
  tempCamYonPlane.heading(planeY);
  Ray camToViewingPlaneIntersect = new Ray(inputCameraPosition, coordinatesOn3DViewingPlane(point, camAxes));  

  return(ewbIK.G.intersectTest(camToViewingPlaneIntersect, tempCamXonPlane.p2, tempCamYonPlane.p2, adjustedPlaneThrough)); 
}

public DVector coordinatesOn3DViewingPlane(DVector point, Axes camAxes){
  Ray viewDistance = new Ray(camAxes.z().p2, camAxes.origin());  
  viewDistance.mag(max(width, height)); //TODO make dynamic for zoomable views??
  
  Ray tempXrayFromCenter = new Ray(viewDistance.p2, null); 
  tempXrayFromCenter.heading(camAxes.x().heading());
  
  Ray tempYrayFromCenter = new Ray(viewDistance.p2, null); 
  tempYrayFromCenter.heading(camAxes.y().heading());
  
  double yScalar = screenY(tempYrayFromCenter.p2) - height/2; 
  double xScalar = screenX(tempXrayFromCenter.p2) - width/2;
  
  double yProj = (point.y - height/2)/yScalar;
  double xProj = (point.x - width/2)/xScalar;
  
  tempXrayFromCenter.scale(xProj);
  tempYrayFromCenter.scale(yProj);
  
  return DVector.add(viewDistance.p2, DVector.add(tempXrayFromCenter.heading(), tempYrayFromCenter.heading()));
  
}

public DVector coordinatesOn3DViewingPlane(DVector point){
  Ray viewDistance = new Ray(camera.z().p2, camera.origin());  
  viewDistance.mag(max(width, height)); //TODO make dynamic for zoomable views??
  
  Ray tempXrayFromCenter = new Ray(viewDistance.p2, null); 
  tempXrayFromCenter.heading(camera.x().heading());
  
  Ray tempYrayFromCenter = new Ray(viewDistance.p2, null); 
  tempYrayFromCenter.heading(camera.y().heading());
  
  double yScalar = screenY(tempYrayFromCenter.p2) - height/2; 
  double xScalar = screenX(tempXrayFromCenter.p2) - width/2;
  
  double yProj = (point.y - height/2)/yScalar;
  double xProj = (point.x - width/2)/xScalar;
  
  tempXrayFromCenter.scale(xProj);
  tempYrayFromCenter.scale(yProj);
  
  return DVector.add(viewDistance.p2, DVector.add(tempXrayFromCenter.heading(), tempYrayFromCenter.heading()));
  
}
public DVector screenXY(DVector point) {
  return DVector.mult(new DVector(screenX(point), screenY(point)), 1); 
}

public PVector screenXY(PVector point) {
  return PVector.mult(new PVector(screenX(point), screenY(point)), 1); 
}

public float screenX(PVector point) {
 return screenX((float)point.x, (float)point.y, (float)point.z); 
}

public double screenX(DVector point) {
 return screenX((float)point.x, (float)point.y, (float)point.z); 
}

public float screenY(PVector point) {
 return screenY((float)point.x, (float)point.y, (float)point.z); 
}

public double screenY(DVector point) {
 return screenY((float)point.x, (float)point.y, (float)point.z); 
}


public DVector screenXY(DVector point, PGraphics buff) {
  return DVector.mult(new DVector(screenX(point, buff), screenY(point, buff)), 1); 
}

public double screenX(DVector point, PGraphics buff) {
 return buff.screenX((float)point.x, (float)point.y, (float)point.z); 
}

public double screenY(DVector point, PGraphics buff) {
 return buff.screenY((float)point.x, (float)point.y, (float)point.z); 
}

public void camera(DVector location, DVector lookAt, DVector up) {
  camera((float)location.x, (float)location.y, (float)location.z, (float)lookAt.x, (float)lookAt.y, (float)lookAt.z, (float)up.x, (float)up.y, (float)up.z);  
}

public void camera(DVector location, DVector lookAt, DVector up, PGraphics buffer) {
  buffer.camera((float)location.x, (float)location.y, (float)location.z, (float)lookAt.x, (float)lookAt.y, (float)lookAt.z, (float)up.x, (float)up.y, (float)up.z);  
}

public void point(DVector p) {
  point((float)p.x, (float)p.y, (float)p.z);  
}

public void point(DVector p, PGraphics context) {
  context.point((float)p.x, (float)p.y, (float)p.z);  
}

public void point(double x, double y, double z) {
  point((float) x, (float) y, (float) z);  
}

public void point(double x, double y, double z, PGraphics context) {
  context.point((float) x, (float) y, (float) z);  
}

public void line(Ray r) {
  line(r.p1, r.p2);   
  
}

public void line(DVector p1, DVector p2) {
  line((float)p1.x, (float)p1.y, (float)p1.z, (float)p2.x, (float)p2.y, (float)p2.z);
}

public void line(DVector p1, DVector p2, PGraphics context) {
  context.line((float)p1.x, (float)p1.y, (float)p1.z, (float)p2.x, (float)p2.y, (float)p2.z);
}

public void vertex(DVector p) {
  vertex((float)p.x, (float)p.y, (float)p.z);  
}

public void vertex(double x, double y, double z) {
  vertex((float) x, (float) y, (float) z);  
}

public void vertex(double x, double y, double z, PGraphics context) {
  context.vertex((float) x, (float) y, (float) z);  
}

public int pixelAt(int x, int y) {
  
 int result = y*width; 
 result += x; 
 
 if(y>=height || x >= width || x < 0 || y<0) return -1; 
 else return result;
}

public void drawFloorGrid(){
  DVector xza;
  DVector xzb;
  for(int i=0; i<100; i++) {
    stroke(0);
    strokeWeight(0.05f);
    xza = new DVector(-500 + i*10, 0, -500); 
    xzb = new DVector(-500 + i*10, 0, 500); 
    line(xza, xzb); 
    xza = new DVector(-500, 0, -500 + i*10); 
    xzb = new DVector(500, 0, -500 + i*10); 
    line(xza, xzb); 
  }
}


public class Camera implements CustomEvents {
  PVector position;
  Axes orbitAxes;
  double thisOrbitRadius;
  double FOV;
  
  public Camera(PVector cameraPosition, Axes inputCameraAxes, double cameraOrbitRadius, double inputZoom) {
    this.position = cameraPosition;
    this.orbitAxes = inputCameraAxes;
    this.thisOrbitRadius = cameraOrbitRadius;
    this.FOV = inputZoom;
    
    eventListeningObjects.add(this);
  }
  
  public void updateGlobalValuesForScene() { 
    //THIS IS KINDA HACKY AND LAZY AND I SHOULD GO THROUGH AND MAKE EACH 
    //OBJECT THAT NEEDS THE INFO GET IT BY ACCESSING THE CURRENT CAMERA OBJECT
    //BUT WHATEVER I'M SHORT ON TIME
    cameraPosition = this.position; 
    camera = this.orbitAxes;
    orbitRadius = thisOrbitRadius;
    zoom = this.FOV;
    
    
  }
  
  public void frame() {
    camera = this.orbitAxes;     
    zoom = this.FOV;
    orbitRadius = this.thisOrbitRadius;
    cameraPosition = this.position;
    
    camera(new DVector(this.position), this.orbitAxes.origin(), this.orbitAxes.y().heading());
    perspective((float)FOV, PApplet.parseFloat(width)/PApplet.parseFloat(height), 4, 10000);
    
  }
  
  DVector pMouseSpace; 
  
  public void panCamera() {
    
    DVector pMouse = new DVector(pmouseX, pmouseY);
    DVector mouse = new DVector(mouseX, mouseY);
    //DVector newMouseSpace = cameraPlaneIntersectThrough(mouse, this.orbitAxes.origin(), new DVector(this.position)); 
    
    //Ray translate = new Ray(pMouseSpace, newMouseSpace);
    //println(translate.heading() + "----------- " + this.orbitAxes.origin());
    //this.orbitAxes.translateByGlobal(translate.heading());
    //this.position = this.orbitAxes.z().getScaledTo(thisOrbitRadius);
    double camXdelt = (pMouse.x - mouse.x)/(thisOrbitRadius*(FOV));
    double camYdelt = (pMouse.y - mouse.y)/(thisOrbitRadius*(FOV));
    //DVector camYheading = orbitAxes.y().heading();
    //DVector camXheading = orbitAxes.z().heading();
  
    DVector camXscale = DVector.mult(orbitAxes.x().heading(), camXdelt);
    DVector camYscale = DVector.mult(orbitAxes.y().heading(), camYdelt);
  
    orbitAxes.translateByGlobal(camXscale);
    orbitAxes.translateByGlobal(camYscale);
    this.position = orbitAxes.z().getScaledTo(thisOrbitRadius).toPVec();
  }

  public void orbitCamera() {
    DVector mouse = new DVector(mouseX, mouseY);
    DVector pMouse = new DVector(pmouseX, pmouseY);
    double yturn = ((pMouse.x - mouse.x)/width)*PI;
    double xturn = ((pMouse.y - mouse.y)/height)*PI;
    yturn *= Math.max(0, 1-Math.abs(mouse.y - center.y)/center.y);
    xturn *= Math.max(0, 1-Math.abs(mouse.x - center.x)/center.x);
    pMouse.sub(center);
    mouse.sub(center);
    double zturn = pMouse.heading() - mouse.heading(); 
  
    if (Math.abs(pMouse.heading() - mouse.heading()) > 1) { 
      zturn = 0;
    }
    double diagDist = DVector.dist(new DVector(0, 0), new DVector(center.x, center.y)); 
    mouse.add(center);
    zturn *= mouse.dist(center)/diagDist;
    rotCamX(-xturn); 
    rotCamY(yturn);
    rotCamZ(zturn);
}

public void zoomCamera() {
  DVector pMouse = new DVector(pmouseX, pmouseY);
  double addZoom = -1*(((mouseX - pMouse.x) + ( mouseY - pMouse.y))/width);
 // println(this.FOV + (addZoom*(this.FOV/2f)));
  this.FOV+= (this.FOV + (addZoom*(this.FOV)) > radians(1.1f))? (addZoom*(this.FOV)) : 0;
}

public void dollyCamera(double iterations) {
  //println("dollying to : " + ( this.thisOrbitRadius + iterations*this.FOV));
  this.thisOrbitRadius += iterations*degrees((float)this.FOV*2);
  this.position = this.orbitAxes.z().getScaledTo(this.thisOrbitRadius).toPVec();
}
  
  
  public void rotCamX(double angle) {
    this.orbitAxes.rotateAboutX(angle);
    this.position = this.orbitAxes.z().getScaledTo(this.thisOrbitRadius).toPVec();
  }

  public void rotCamZ(double angle) {
    this.orbitAxes.rotateAboutZ(angle);
    this.position = this.orbitAxes.z().getScaledTo(this.thisOrbitRadius).toPVec();
  }

  public void rotCamY(double angle){
    this.orbitAxes.rotateAboutY(angle);
    this.position = this.orbitAxes.z().getScaledTo(this.thisOrbitRadius).toPVec();
  }
  
  DVector mousePressScreenLocation = null;
  DVector mousePressSpaceLocation = null;
  DVector cameraPositionAtMousePress = null; 
  Axes orbitAxesAtMousePress = null; 
  Double FOVatMousePress = null;
  Double orbitRadiusAtMousePress = null;
  
  public void keyPressed(){} 
  public void keyReleased(){}
  
  public void mousePressed(){
    //println(this);
    if(cameraMode && currentCamera==this) {
     // println("making");
      this.mousePressScreenLocation = new DVector(mouseX, mouseY);
      this.mousePressSpaceLocation = cameraPlaneIntersectThrough(new DVector(mouseX, mouseY), this.orbitAxes.origin(), new DVector(this.position));
      this.cameraPositionAtMousePress = new DVector(this.position.copy());
      this.orbitAxesAtMousePress = (Axes)this.orbitAxes.getAbsoluteCopy();
      this.FOVatMousePress = this.FOV;
      this.orbitRadiusAtMousePress = this.thisOrbitRadius;
    }
  }
  public void mouseDragged(){
    if(dragEventRelevantToCamera()) {      
      switch(mouseButton) {
        case RIGHT: panCamera(); 
                    break;
        case LEFT: orbitCamera();
                    break;
        case CENTER: zoomCamera();
        
      }
    }  
  }
  public void mouseReleased(){
      this.mousePressScreenLocation = null;
      this.mousePressSpaceLocation = null;
      this.cameraPositionAtMousePress = null;
      this.orbitAxesAtMousePress = null;
      this.FOVatMousePress = null;
      this.orbitRadiusAtMousePress = null;
      this.pMouseSpace = null;
  }
  
  public boolean dragEventRelevantToCamera(){
    if(currentCamera == this &&
      cameraMode == true &&
      mousePressScreenLocation != null &&
      cameraPositionAtMousePress != null &&
      orbitAxesAtMousePress != null && 
      FOVatMousePress != null &&
      orbitRadiusAtMousePress != null) {
       return true; 
    } else return false; 
  }
  
  public DVector viewingPlaneIntersect(DVector pointThrough) {
    DVector result;
    double distToPlane = new DVector(this.position).dist(pointThrough);
    double xIntercept = Math.tan(((double)(mouseX-(width/2))/(double)(height))*(FOV))*distToPlane; 
    double yIntercept = Math.tan(((double)(mouseY-(height/2))/(double)(height))*(FOV))*distToPlane; 
    
    DVector xVec = new DVector(1,0);
    xVec.rotate(Math.tan((FOV/2)*((double)(mouseX- ((double)width/2f))/((double)height/2f))));
    xVec = new DVector(xVec.y,0, xVec.x); 
    
    DVector yVec = new DVector(1,0);
    yVec.rotate(Math.tan((FOV/2)*((double)(mouseY- ((double)height/2f))/((double)height/2f))));
    yVec = new DVector(0, yVec.y, yVec.x); 
    
    
    
    result = this.orbitAxes.getGlobalOf(new DVector(xIntercept, yIntercept, 0));
    //println("----" + result);
    return result;
  }
}