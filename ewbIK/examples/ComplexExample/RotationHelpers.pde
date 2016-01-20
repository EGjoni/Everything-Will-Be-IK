  public class Axes extends pseudoScene.AbstractAxes implements AxisDependancy { //<>//

    public Axes(Ray inX, Ray inY, Ray inZ) {
      super(inX, inY, inZ);   
    }

    public Axes(Ray inX, Ray inY, Ray inZ, Axes parent) {
      super(inX, inY, inZ, parent);
    }


    public Axes(DVector origin, DVector inX, DVector inY, DVector inZ) {
      super(origin, inX, inY, inZ);
    }

    public Axes(DVector origin, DVector inX, DVector inY, DVector inZ,  Axes parent) { //<>//
      super(origin, inX, inY, inZ, parent);
    }

    @Override
    public Axes instantiate(Ray x, Ray y, Ray z) {
      // TODO Auto-generated method stub
      return new Axes(x, y, z);
    }

    @Override
    public Axes instantiate(DVector origin, DVector x, DVector y, DVector z) {
      // TODO Auto-generated method stub
      return new Axes(origin, x, y, z);
    }

    @Override
    public Axes instantiate(Ray x, Ray y, Ray z, AbstractAxes par) {
      // TODO Auto-generated method stub
      return new Axes(x, y, z, (Axes)par);
    }  

    public void drawMe(double scalar){/*
    this.updateGlobal();
    stroke(255,0,0);
    line(globalCoords.ly.getRayScaledTo(scalar));
    stroke(0,255,0);
    line(globalCoords.lx.getRayScaledTo(scalar));
    stroke(0,0,255);
    line(globalCoords.lz.getRayScaledTo(scalar));*/
    }

    public void drawMe() {
      //this.drawMe(10);  
    }      

    /*public void addToSaveState(SaveState ss) {
       if(ss.axes.get(this) == null) {
         AxesSaveObject aso = new AxesSaveObject(this); 
         ss.axes.put(this, aso);
       }
  }*/

  }
  


public DVector axisRotation(DVector point, DVector axis, double angle) {
  //println("rotting");
  Rot rotation = new Rot(axis, angle);
  point = rotation.applyTo(point);
  return point;
}

public DVector[] axisRotation(DVector[] points, DVector axis, double angle) {
  
  Rot rotation = new Rot(axis, angle);
  for(DVector point : points) {
    point = rotation.applyTo(point);
  }
  return points;
}



public void rayRotation(DVector point, Ray r, double angle) {
  
  DVector axis = DVector.sub(r.p2, r.p1);
  point.sub(r.p1); 
  point = axisRotation(point, axis, angle);
  point.add(r.p1);
  
}

public void rayRotation(Ray point, Ray axis, double angle) {
  DVector result = point.heading().copy();   
  result = axisRotation(result, axis.heading(), angle); 
  point.heading(result);
  
}

public void rayRotation(DVector[] points, Ray r, double angle) {

  DVector axis = DVector.sub(r.p2, r.p1);
  for(DVector point : points) {
    point.sub(r.p1); 
  }
  points = axisRotation(points, axis, angle);
  for(DVector point : points) {
    point.add(r.p1); 
  }
  
}