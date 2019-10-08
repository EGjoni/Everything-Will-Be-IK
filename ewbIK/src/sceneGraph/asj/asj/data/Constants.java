package asj.data;
/**
 *These are mostly just constants potentially useful for saving and loading JSOn files. 
 */
public interface Constants {
 

  // platform IDs for StringFuncs.platform
  static final int OTHER   = 0;
  static final int WINDOWS = 1;
  static final int MACOSX  = 2;
  static final int LINUX   = 3;

  static final String[] platformNames = {
    "other", "windows", "macosx", "linux"
  };
  static final String WHITESPACE = " \t\n\r\f\u00A0";

  static final int GROUP           = 0;  
  static final int POINT           = 2;   // primitive
  static final int POINTS          = 3;   // vertices
  static final int LINE            = 4;   // primitive
  static final int TRIANGLE        = 8;   // primitive
  static final int TRIANGLES       = 9;   // vertices
  static final int TRIANGLE_STRIP  = 10;  // vertices
  static final int TRIANGLE_FAN    = 11;  // vertices
  static final int QUAD            = 16;  // primitive
  static final int QUADS           = 17;  // vertices
  static final int QUAD_STRIP      = 18;  // vertices
  static final int POLYGON         = 20;  // in the end, probably cannot
  static final int PATH            = 21;  // separate these two
  static final int RECT            = 30;  // primitive
  static final int ELLIPSE         = 31;  // primitive
  static final int ARC             = 32;  // primitive
  static final int SPHERE          = 40;  // primitive
  static final int BOX             = 41;  // primitive

  static final int OPEN = 1;
  static final int CLOSE = 2;

  static final int CORNER   = 0;
  static final int CORNERS  = 1;
  static final int RADIUS   = 2;
  static final int CENTER   = 3;  
  static final int DIAMETER = 3;
  static final int CHORD  = 2;
  static final int PIE    = 3;
  static final int BASELINE = 0;
  static final int TOP = 101;
  static final int BOTTOM = 102;
  static final int NORMAL     = 1;

  public static final int CLAMP = 0;
  public static final int REPEAT = 1;

  static final int MODEL = 4;

}
