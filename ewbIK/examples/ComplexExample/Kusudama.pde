  public class Kusudama extends ewbIK.AbstractKusudama implements CustomEvents {


    pLimitCone currentlySelectedConstraintPoint;   

    boolean draggingMinHandle =false;
    boolean draggingMaxHandle = false; 
    boolean wasDragging = false;
    
    public boolean isVisible = false;
    
    public DVector minAxialAngleLimitHandle, maxAxialAngleLimitHandle;



    public Kusudama(pBone forBone) {
      super(forBone);
      eventListeningObjects.add(this);  
      

      limitCones.add(new pLimitCone(this.limitingAxes.getLocalOf(forBone.localAxes().getGlobalOf(new DVector(0, 1, 0))), radians(25), this));
      limitCones.add(new pLimitCone(this.limitingAxes.getLocalOf(forBone.localAxes().getGlobalOf(new DVector(1, 1, 0))), radians(25), this));

      for(AbstractLimitCone ll : limitCones) {
        ll.controlPoint.normalize();
        ll.controlPoint.mult(attachedTo.getBoneHeight());
      }
      this.setAxialLimits(radians(-179), radians(180));

      updateTangentRadii();

    }
    
    @Override 
    public void enableAxialLimits() {
      this.axiallyConstrained = true;
      updateAxialLimitHandles();
    }
    
    public void updateAxialLimitHandles() {
      DVector zVec = new DVector(0,0,attachedTo.getBoneHeight());
      DVector yVec = new DVector(0,1,0);
      Rot minRot = new Rot(yVec, minAxialAngle());
      Rot maxRot = new Rot(yVec, maxAxialAngle());
      
      minAxialAngleLimitHandle = minRot.applyTo(zVec);
      maxAxialAngleLimitHandle = maxRot.applyTo(zVec);
      
      Rot visualAlignment = new Rot(yVec, limitingAxes.getLocalOf(attachedTo.localAxes().y().p2));
      minAxialAngleLimitHandle = visualAlignment.applyTo(minAxialAngleLimitHandle);
      maxAxialAngleLimitHandle = visualAlignment.applyTo(maxAxialAngleLimitHandle);
    }
    
    public void updateAnglesFromLimitHandles(DVector minAngle, DVector maxAngle) {
      DVector limitFrameLocalAxesTip = limitingAxes.getLocalOf(attachedTo.localAxes().y().p2);
      DVector limitFrameLocalAxesMinAngle = minAngle.copy();//limitingAxes.getLocalOf(attachedTo.localAxes().getGlobalOf(minAngle));
      DVector limitFrameLocalAxesMaxAngle = maxAngle.copy();//limitingAxes.getLocalOf(attachedTo.localAxes().getGlobalOf(maxAngle));
      
      
      DVector yHeading = new DVector(0,1,0); 
      DVector zHeading = new DVector(0,0,1);
      
      Rot yAlign = new Rot(limitFrameLocalAxesTip, yHeading);
      
      DVector limitingAxesAlignedMinHandle = yAlign.applyTo(limitFrameLocalAxesMinAngle); 
      DVector limitingAxesAlignedMaxHandle = yAlign.applyTo(limitFrameLocalAxesMaxAngle);
      
      limitingAxesAlignedMinHandle.y = 0;
      limitingAxesAlignedMaxHandle.y = 0;
      
      Rot zToMin = new Rot(zHeading, limitingAxesAlignedMinHandle);
      Rot zToMax = new Rot(zHeading, limitingAxesAlignedMaxHandle);
      
      double tempMinAngle = zToMin.getAngle();
      double tempMaxAngle = zToMax.getAngle(); 
      
      if(zToMin.getAxis().y < 0) {
        tempMinAngle = PI*2 - (tempMinAngle);
      }
      if(zToMax.getAxis().y < 0) {
        tempMaxAngle = PI*2 - (tempMaxAngle);
      }
      
      this.setAxialLimits(tempMinAngle, tempMaxAngle);
      
    }


    @Override
    public void generateAxes(Ray x, Ray y, Ray z) {
      this.limitingAxes = new Axes(x, y, z);
    }

    @Override
    public void addLimitConeAtIndex(int insertAt, DVector newPoint, double radius) {
      println("INSERTING AT " + insertAt);
      if(insertAt == -1) {
        pLimitCone tc = new pLimitCone(newPoint, radius, this);
        tc.controlPoint.normalize();
        tc.controlPoint.mult(attachedTo.getBoneHeight());
        limitCones.add(tc);
        updateTangentRadii();
        tc.select();
        tc.draggingCenter = true;
        currentlySelectedConstraintPoint = tc;
      } else {
        pLimitCone tc = new pLimitCone(newPoint, radius, this);
        tc.controlPoint.normalize();
        tc.controlPoint.mult(attachedTo.getBoneHeight());
        limitCones.add(insertAt, tc);
        updateTangentRadii();
        tc.select();
        tc.draggingCenter = true;
        currentlySelectedConstraintPoint = tc;
      }
      }

    public void addLimitConeFromMouse() {
      DVector camDVec = new DVector(cameraPosition);
      Ray mouse = new Ray(camDVec, 
          cameraPlaneIntersectThrough(new DVector(mouseX, mouseY), limitingAxes.origin(), new DVector(cameraPosition)));


      DVector sphereIntersect1 = new DVector(); 
      DVector sphereIntersect2 = new DVector();
      int intersectionCount = G.raySphereIntersection(limitingAxes.getLocalOf(mouse), attachedTo.getBoneHeight(), sphereIntersect1, sphereIntersect2);  
      DVector newPoint = sphereIntersect1; 
      if(intersectionCount == 2 && camDVec.dist(sphereIntersect1) > camDVec.dist(sphereIntersect2)) {
        newPoint = sphereIntersect2; 
      } else if (intersectionCount == 0) {
        newPoint = mouse.closestPointTo(limitingAxes.origin());
      }


      double shortestDistance = -1;
      int indexOfClosest = -1; 

      for (int i = 0; i < limitCones.size()-1; i++) {
        DVector point = G.closestPointOnGreatArc(limitCones.get(i).controlPoint, limitCones.get(i+1).controlPoint, newPoint);  
        //((LimitCone)limitCones.get(i)).deselect();
        if (DVector.angleBetween(point, newPoint) < shortestDistance || shortestDistance == -1) {
          indexOfClosest = i; 
          shortestDistance = DVector.angleBetween(point, newPoint);
        }
      }

      int insertAt = indexOfClosest+1;
      if (DVector.angleBetween(limitCones.get(limitCones.size()-1).controlPoint, newPoint) < radians(0.05f)) {

        insertAt = limitCones.size()-1;
      } else if (DVector.angleBetween(limitCones.get(0).controlPoint, newPoint) < radians(0.05f)) {
        insertAt =0;
      }
      //println(newPoint);
      addLimitConeAtIndex(insertAt, newPoint, limitCones.get(insertAt).radius); 

    }

    public void mousePressed(){
      this.wasDragging = false;
      if(isEventRelevant()) {
        if(mouseButton == RIGHT && currentlySelectedConstraintPoint != null) {
          addLimitConeFromMouse(); 
        } if(mouseButton == LEFT) {
          selectControlPoint(new PVector(mouseX, mouseY));
        }
      }
    }

    public void mouseDragged(){
      if(isEventRelevant() && (draggingMinHandle || draggingMaxHandle) && mouseButton == LEFT) {
        DVector onPlane = limitingAxes.getLocalOf(mousePlaneIntersect(attachedTo.localAxes().origin(), attachedTo.localAxes().x().p2, attachedTo.localAxes().z().p2));
        //onPlane.y = 0;
        //onPlane.normalize(); 
        //onPlane.mult(attachedTo.getBoneHeight());
        
        DVector tempMinResult = minAxialAngleLimitHandle; 
        DVector tempMaxResult = maxAxialAngleLimitHandle; 
        if(draggingMinHandle) {
          tempMinResult = onPlane;
        } else if(draggingMaxHandle) {
          tempMaxResult = onPlane;
        }
        if(draggingMinHandle || draggingMaxHandle) wasDragging = true;
        updateAnglesFromLimitHandles(tempMinResult, tempMaxResult);
        //attachedTo.snapToConstraints();
      }
    }
    public void mouseReleased(){      
      draggingMinHandle = false; 
      draggingMaxHandle = false;
    }

    public void keyPressed(){}
    public void keyReleased(){
      
    }

    public void selectControlPoint(PVector mouse) {
      float closestDist = -1;
      int closestIndex = -1;
      
      for(int i =0; i<limitCones.size(); i++) {
        PVector screenCoords = screenXY(limitingAxes.getGlobalOf(limitCones.get(i).controlPoint).toPVec()); 
        PVector screenRadialPoint = screenXY(limitingAxes.getGlobalOf(((pLimitCone)limitCones.get(i)).radialPoint).toPVec());
        ((pLimitCone)limitCones.get(i)).deselect();      
        float dist = min(mouse.dist(screenRadialPoint), mouse.dist(screenCoords)); 
        
        if(dist < 10 && (closestIndex == -1 || (closestDist > dist))) {
          closestIndex = i; 
          closestDist = dist;
        }
      }
      if(closestIndex != -1) {
        //((pLimitCone)limitCones.get(closestIndex)).select(); 
        currentlySelectedConstraintPoint = (pLimitCone)limitCones.get(closestIndex);
        PVector screenCoords = screenXY(limitingAxes.getGlobalOf(limitCones.get(closestIndex).controlPoint).toPVec()); 
        PVector screenRadialPoint = screenXY(limitingAxes.getGlobalOf(((pLimitCone)limitCones.get(closestIndex)).radialPoint).toPVec());
        
        if(screenRadialPoint.dist(mouse) < screenCoords.dist(mouse)) {
          currentlySelectedConstraintPoint.draggingCenter = false;
          currentlySelectedConstraintPoint.draggingRadialHandle = true;
          currentlySelectedConstraintPoint.isSelected = true;
        } else {
        
          currentlySelectedConstraintPoint.select();
        }
        
      } else {
        currentlySelectedConstraintPoint = null; 
        DVector minHandleScreenCoords = screenXY(limitingAxes.getGlobalOf(minAxialAngleLimitHandle)); 
        DVector maxHandleScreenCoords = screenXY(limitingAxes.getGlobalOf(maxAxialAngleLimitHandle));
        if(mouse.dist(minHandleScreenCoords.toPVec()) < 10) { 
          draggingMinHandle = true;   
        } else if (mouse.dist(maxHandleScreenCoords.toPVec()) < 10) {
          draggingMaxHandle = true; 
        }
        

      }
    }

    public Axes limitingAxes() {
      return (Axes)limitingAxes;  
    }

    public void drawMe() {
      //println(this.isVisible);
      if(this.isVisible) {
        //println("--orientationallyConstrained? " + this.orientationallyConstrained); 
        //println("--axyallyConstrained? " + this.axiallyConstrained); 
        if(this.orientationallyConstrained) drawOrientationalLimits();
        if(this.axiallyConstrained) drawAxialLimits();
      }


    }

    public void updateRadialHandles() {
      if(limitCones.size() == 1) {
        DVector center = limitCones.get(0).controlPoint; 
        DVector offset = center.copy();
        offset.x+=1;
        ((pLimitCone)limitCones.get(0)).radialPoint = (new Rot(new Rot(center, offset).getAxis(), limitCones.get(0).radius)).applyTo(center);
        ((pLimitCone)limitCones.get(0)).radialPoint = (new Rot(center, PI/2)).applyTo(((pLimitCone)limitCones.get(0)).radialPoint);
      } else {
        for(int i = 0; i<limitCones.size() - 1; i++ ) {
          DVector center = limitCones.get(i).controlPoint;
          DVector centerNext = limitCones.get(i+1).controlPoint;
          ((pLimitCone)limitCones.get(i)).radialPoint = (new Rot(new Rot(center, centerNext).getAxis(), limitCones.get(i).radius)).applyTo(center);
          ((pLimitCone)limitCones.get(i)).radialPoint = (new Rot(center, PI/2)).applyTo(((pLimitCone)limitCones.get(i)).radialPoint);

        
        } 
        DVector center = limitCones.get(limitCones.size()-1).controlPoint; 
        DVector centerPrev = limitCones.get(limitCones.size()-2).controlPoint;
        //offset.x+=1;
        ((pLimitCone)limitCones.get(limitCones.size()-1)).radialPoint = (new Rot(new Rot(center, centerPrev).getAxis(), limitCones.get(limitCones.size()-1).radius)).applyTo(center);
        ((pLimitCone)limitCones.get(limitCones.size()-1)).radialPoint = (new Rot(limitCones.get(limitCones.size()-1).controlPoint, Math.PI/2d)).applyTo(((pLimitCone)limitCones.get(limitCones.size()-1)).radialPoint);
      }
      
    }
    
    public void drawRadialHandles() {
      
      for(int i=0; i<limitCones.size(); i++) {
    	strokeWeight(14);
      	stroke(200,200,255); 
        point(limitingAxes.getGlobalOf(((pLimitCone)limitCones.get(i)).radialPoint));
    	strokeWeight(10);
    	stroke(0,100,155); 
        point(limitingAxes.getGlobalOf(((pLimitCone)limitCones.get(i)).radialPoint));
      }
      
    }

    public void drawOrientationalLimits() {
      updateRadialHandles();
      drawRadialHandles();
      double granularity = 40;

      if (limitCones.size() > 1) { 
        stroke(255);
        strokeWeight(5);
        for (int i =0; i<limitCones.size()-1; i++) {
          stroke(255);
          point(limitingAxes.getGlobalOf(limitCones.get(i).controlPoint));
          point(limitingAxes.getGlobalOf(limitCones.get(i+1).controlPoint));
          strokeWeight(2);
          drawGreatArc(limitCones.get(i).controlPoint, limitCones.get(i+1).controlPoint, (Axes)this.limitingAxes, 20, false); 

          hint(DISABLE_DEPTH_MASK);
          strokeWeight(2); 
          stroke(255);
          drawMinorArc(limitCones.get(i).tangentCircleRadiusNext, attachedTo.getBoneHeight(), limitCones.get(i).tangentCircleCenterNext1,
              limitCones.get(i).controlPoint, limitCones.get(i+1).controlPoint, 40f, (Axes)this.limitingAxes, this, false);
          drawMinorArc(limitCones.get(i).tangentCircleRadiusNext, attachedTo.getBoneHeight(), limitCones.get(i).tangentCircleCenterNext2,
              limitCones.get(i+1).controlPoint, limitCones.get(i).controlPoint, 40f, (Axes)this.limitingAxes, this, false);


          DVector segmentNormal = limitCones.get(i).controlPoint.cross(limitCones.get(i+1).controlPoint);
          DVector lastPoint = null;

          double nextActualRadius = limitCones.get(i+1).radius;
          Rot endCap = new Rot(segmentNormal, nextActualRadius + radians(0.05f));
          DVector beginPoint = endCap.applyTo(limitCones.get(i+1).controlPoint.copy());
          lastPoint = beginPoint; 
          Rot endCapBorder = new Rot(limitCones.get(i+1).controlPoint.copy(), TAU/(granularity));

          for (int j = 0; j<(granularity+1); j++) {
            if (limitCones.get(i+1).isSelected) { 
              stroke(0, 255, 0); 
              strokeWeight(4);
            } else { 
              stroke(255); 
              strokeWeight(2);
            }

            boolean[] inBounds = {true};
            pointInLimits(limitingAxes.getGlobalOf(beginPoint), inBounds);

            if (!inBounds[0]) { 
              if (limitCones.get(i+1).isSelected) { 
                stroke(0, 255, 0); 
                strokeWeight(4);
              } else { 
                stroke(255); 
                strokeWeight(2);
              }
              line(limitingAxes.getGlobalOf(lastPoint), limitingAxes.getGlobalOf(beginPoint));
              noStroke();
              //strokeWeight(3); 
              fill(0, 0, 0, 50); 
              beginShape();
              vertex(limitingAxes.origin()); 
              vertex(limitingAxes.getGlobalOf(lastPoint));
              vertex(limitingAxes.getGlobalOf(beginPoint));
              endShape();
            } else { 
              point(limitingAxes.getGlobalOf(beginPoint));
            }
            lastPoint = beginPoint.copy();
            beginPoint = endCapBorder.applyTo(beginPoint); 
            //}
          }
          strokeWeight(5);
          point(limitingAxes.getGlobalOf(limitCones.get(i+1).controlPoint));
          hint(ENABLE_DEPTH_MASK);
        }
        hint(DISABLE_DEPTH_MASK);
        strokeWeight(2);
        double thisRadius = limitCones.get(0).radius;

        DVector segmentNormal = limitCones.get(0).controlPoint.cross(limitCones.get(1).controlPoint);

        Rot endCap = new Rot(segmentNormal, thisRadius + radians(0.05f));
        DVector beginPoint = endCap.applyTo(limitCones.get(0).controlPoint.copy());
        DVector lastPoint = beginPoint; 
        Rot endCapBorder = new Rot(limitCones.get(0).controlPoint.copy(), TAU/(granularity));
        for (int j = 0; j<(granularity); j++) {
          if (limitCones.get(0).isSelected) { 
            stroke(0, 255, 0); 
            strokeWeight(4);
          } else { 
            stroke(255); 
            strokeWeight(2);
          }
          boolean[] inBounds = {true};
          pointInLimits(limitingAxes.getGlobalOf(beginPoint), inBounds);
          if (!inBounds[0]) { 
            if (limitCones.get(0).isSelected) { 
              stroke(0, 255, 0); 
              strokeWeight(4);
            } else { 
              stroke(255); 
              strokeWeight(2);
            }
            line(limitingAxes.getGlobalOf(lastPoint), limitingAxes.getGlobalOf(beginPoint));
            noStroke();
            //strokeWeight(3); 
            fill(0, 0, 0, 50); 
            beginShape();
            vertex(limitingAxes.origin()); 
            vertex(limitingAxes.getGlobalOf(lastPoint));
            vertex(limitingAxes.getGlobalOf(beginPoint));
            endShape();
          } else { 
            point(limitingAxes.getGlobalOf(beginPoint));
          }
          lastPoint = beginPoint.copy();
          beginPoint = endCapBorder.applyTo(beginPoint); 
          //}
        }
        hint(ENABLE_DEPTH_MASK);
      } else {
        drawConicLimit(granularity); 
      }

    }

    public void drawConicLimit(double granularity) {

      double thisRadius = limitCones.get(0).radius;
      strokeWeight(5);
      point(limitingAxes.getGlobalOf(limitCones.get(0).controlPoint));
      DVector center = limitCones.get(0).controlPoint.copy(); 
      DVector offSet = DVector.add(center, new DVector(1,0,0)); 
      Rot endCap = new Rot(center.cross(offSet), thisRadius + radians(0.05f));
      DVector beginPoint = endCap.applyTo(center);
      DVector lastPoint = beginPoint; 
      Rot endCapBorder = new Rot(center, TAU/(granularity));
      hint(DISABLE_DEPTH_MASK);
      for (int j = 0; j<(granularity); j++) {
        if (limitCones.get(0).isSelected) { 
          stroke(0, 255, 0); 
          strokeWeight(4);
        } else { 
          stroke(255); 
          strokeWeight(2);
        }

        line(limitingAxes.getGlobalOf(lastPoint), limitingAxes.getGlobalOf(beginPoint));
        noStroke();

        beginShape();
        vertex(limitingAxes.origin()); 
        vertex(limitingAxes.getGlobalOf(lastPoint));
        vertex(limitingAxes.getGlobalOf(beginPoint));
        endShape();
        lastPoint = beginPoint.copy();
        beginPoint = endCapBorder.applyTo(beginPoint);
      }
      hint(ENABLE_DEPTH_MASK);


    }

    public void drawAxialLimits() {
      int granularity = 20;
      updateAxialLimitHandles();
      Rot minToMax = new Rot(minAxialAngleLimitHandle, maxAxialAngleLimitHandle); 
      double angle = minToMax.getAngle(); 
      DVector axis = minToMax.getAxis();
      if(axis.dot(limitingAxes.getLocalOf(attachedTo.localAxes().y().p2)) < 0) { 
        //axis.mult(-1);
        angle = TAU - angle;
      }
      
      double delta = angle / (double)granularity;
      DVector tracer = minAxialAngleLimitHandle.copy();

      fill(150,150,0,100);
      beginShape();
      Rot traceRot = new Rot(limitingAxes.getLocalOf(attachedTo.localAxes().y().p2), delta);
      vertex(limitingAxes.origin());
      vertex(limitingAxes.getGlobalOf(tracer));
      for(int i=0 ; i<granularity; i++) {
        tracer = traceRot.applyTo(tracer);
        vertex(limitingAxes.getGlobalOf(tracer));
      }
      endShape(CLOSE);
      
      stroke(0,0,255);
      strokeWeight(3);
      Ray zRay = attachedTo.localAxes().z().getRayScaledTo(attachedTo.getBoneHeight());
      line(zRay.p1, zRay.p2);

      strokeWeight(10);
      stroke(255,255,0); 
      point(limitingAxes.getGlobalOf(minAxialAngleLimitHandle));
      stroke(0, 255,255); 
      point(limitingAxes.getGlobalOf(maxAxialAngleLimitHandle));
      
      

    }

    public boolean axiallyConstrained() {
      return this.axiallyConstrained;  
    }


    public boolean orientationallyConstrained() {
      return this.orientationallyConstrained;  
    }

    public boolean isEventRelevant() {
      if(this.isVisible && !cameraMode && ((pBone)this.attachedTo).isSelected) return true;
      else return false;

    }
  }

  public class pLimitCone extends ewbIK.AbstractLimitCone implements CustomEvents {
    public DVector radialPoint; 
    
    public pLimitCone(DVector location, double rad, Kusudama attachedTo) {
      super(location, rad, attachedTo);
      eventListeningObjects.add(this);

    }

    public void mousePressed() {
      if(this.isSelected && !cameraMode ) {
        
        PVector mouse = new PVector(mouseX, mouseY);
        
        PVector screenControlPoint = screenXY(((Kusudama)parentKusudama).limitingAxes().getGlobalOf(controlPoint).toPVec());
        PVector screenRadialPoint = screenXY(((Kusudama)parentKusudama).limitingAxes().getGlobalOf(radialPoint).toPVec());
        
        dragStartDist = screenControlPoint.dist(screenRadialPoint);
        dragStartRad = this.radius;
        if(mouse.dist(screenControlPoint) < 10) {
          draggingCenter = true;
          draggingRadialHandle = false;
        } else if(mouse.dist(screenRadialPoint) < 10) {
          draggingRadialHandle = true;
          draggingCenter = false;
        }
      }
    }
    
    double dragStartDist; 
    double dragStartRad; 

    public void mouseDragged() {

      if(this.isSelected && !cameraMode && (draggingCenter || draggingRadialHandle)) {
        Ray boneCamRay = new Ray(parentKusudama.attachedTo().getBase(), new DVector(cameraPosition));
        boneCamRay.mag(parentKusudama.attachedTo().getBoneHeight()/2f);
        DVector throughPoint = boneCamRay.p2;

        DVector planeIntersect = cameraPlaneIntersectThrough(new DVector(mouseX, mouseY), throughPoint); 
        DVector newPosition = ((Kusudama)parentKusudama).limitingAxes().getLocalOf(planeIntersect);
        
        
        if(draggingCenter) {
          controlPoint = newPosition.setMag(parentKusudama.attachedTo().getBoneHeight()); 
          strokeWeight(10);
          stroke(255,255,0);
        } else if(draggingRadialHandle) {
          DVector screenControlPoint = screenXY(((Kusudama)parentKusudama).limitingAxes().getGlobalOf(controlPoint)); 
          double dist = DVector.dist(screenControlPoint, new DVector(mouseX, mouseY));
          
          dist = dist/Math.max(1, dragStartDist);
          radius = Math.min(Math.PI, dragStartRad*dist);          
          ((Kusudama)parentKusudama).wasDragging = true; 
        }
        parentKusudama.updateTangentRadii();
      }
    }

    public void mouseReleased() {
      if(draggingCenter || draggingRadialHandle) ((Kusudama)parentKusudama).wasDragging = true; 
        draggingCenter = false; 
        draggingRadialHandle = false;
    }

    public void keyPressed() {

    }

    public void keyReleased() {

    }

    public void select() {
      ((Kusudama)this.parentKusudama).currentlySelectedConstraintPoint = this;

      draggingRadialHandle = false;
      draggingCenter = true;
      this.isSelected = true;
      currentlySelected_limit_radius.setValue((float)degrees((float)radius));

    }

    public void deselect() {
      this.isSelected = false; 
      this.draggingCenter = false; 
      this.draggingRadialHandle =false; 
    }

  }
  