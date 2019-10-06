package asj.data;
/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 * Most of this code is taken from the PApplet class of the processing 
 * library. It is used exclusively for its excellent JSON processing 
 * functionality. If you are porting Everything Will be IK, 
 * you should not need to port this file. Just use whatever
 * JSON library is available in the language of your choice. 
 * 
 * 
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.regex.*;
import java.util.zip.*;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;




public class StringFuncs implements Constants {

	
		

	  /**
	   * @webref input:files
	   * @param input String to parse as a JSONObject
	   * @see StringFuncs#loadJSONObject(String)
	   * @see StringFuncs#saveJSONObject(JSONObject, String)
	   */
	  public JSONObject parseJSONObject(String input) {
	    return new JSONObject(new StringReader(input));
	  }


	  /**
	   * @webref input:files
	   * @param filename name of a file in the data folder or a URL
	   * @see JSONObject
	   * @see JSONArray
	   * @see StringFuncs#loadJSONArray(String)
	   * @see StringFuncs#saveJSONObject(JSONObject, String)
	   * @see StringFuncs#saveJSONArray(JSONArray, String)
	   */
	  public JSONObject loadJSONObject(String filename) {
	    // can't pass of createReader() to the constructor b/c of resource leak
	    BufferedReader reader = createReader(filename);
	    JSONObject outgoing = new JSONObject(reader);
	    try {
	      reader.close();
	    } catch (IOException e) {  // not sure what would cause this
	      e.printStackTrace();
	    }
	    return outgoing;
	  }


	  /**
	   * @nowebref
	   */
	  static public JSONObject loadJSONObject(File file) {
	    // can't pass of createReader() to the constructor b/c of resource leak
	    BufferedReader reader = createReader(file);
	    JSONObject outgoing = new JSONObject(reader);
	    try {
	      reader.close();
	    } catch (IOException e) {  // not sure what would cause this
	      e.printStackTrace();
	    }
	    return outgoing;
	  }


	  /**
	   * @webref output:files
	   * @param json the JSONObject to save
	   * @param filename the name of the file to save to
	   * @see JSONObject
	   * @see JSONArray
	   * @see StringFuncs#loadJSONObject(String)
	   * @see StringFuncs#loadJSONArray(String)
	   * @see StringFuncs#saveJSONArray(JSONArray, String)
	   */
	  public static boolean saveJSONObject(JSONObject json, String filename) {
	    return saveJSONObject(json, filename, null);
	  }


	  /**
	   * @param options "compact" and "indent=N", replace N with the number of spaces
	   */
	  public static boolean saveJSONObject(JSONObject json, String filename, String options) {
	    return json.save(saveFile(filename), options);
	  }

	/**
	   * @webref input:files
	   * @param input String to parse as a JSONArray
	   * @see JSONObject
	   * @see StringFuncs#loadJSONObject(String)
	   * @see StringFuncs#saveJSONObject(JSONObject, String)
	   */
	  public JSONArray parseJSONArray(String input) {
	    return new JSONArray(new StringReader(input));
	  }


	  /**
	   * @webref input:files
	   * @param filename name of a file in the data folder or a URL
	   * @see JSONArray
	   * @see StringFuncs#loadJSONObject(String)
	   * @see StringFuncs#saveJSONObject(JSONObject, String)
	   * @see StringFuncs#saveJSONArray(JSONArray, String)
	   */
	  public JSONArray loadJSONArray(String filename) {
	    // can't pass of createReader() to the constructor b/c of resource leak
	    BufferedReader reader = createReader(filename);
	    JSONArray outgoing = new JSONArray(reader);
	    try {
	      reader.close();
	    } catch (IOException e) {  // not sure what would cause this
	      e.printStackTrace();
	    }
	    return outgoing;
	  }


	  static public JSONArray loadJSONArray(File file) {
	    // can't pass of createReader() to the constructor b/c of resource leak
	    BufferedReader reader = createReader(file);
	    JSONArray outgoing = new JSONArray(reader);
	    try {
	      reader.close();
	    } catch (IOException e) {  // not sure what would cause this
	      e.printStackTrace();
	    }
	    return outgoing;
	  }


	  /**
	   * @webref output:files
	   * @param json the JSONArray to save
	   * @param filename the name of the file to save to
	   * @see JSONObject
	   * @see JSONArray
	   * @see StringFuncs#loadJSONObject(String)
	   * @see StringFuncs#loadJSONArray(String)
	   * @see StringFuncs#saveJSONObject(JSONObject, String)
	   */
	  public boolean saveJSONArray(JSONArray json, String filename) {
	    return saveJSONArray(json, filename, null);
	  }

	  /**
	   * @param options "compact" and "indent=N", replace N with the number of spaces
	   */
	  public boolean saveJSONArray(JSONArray json, String filename, String options) {
	    return json.save(saveFile(filename), options);
	  }



	//  /**
	//   * @webref input:files
	//   * @see Table
	//   * @see StringFuncs#loadTable(String)
	//   * @see StringFuncs#saveTable(Table, String)
	//   */
	//  public Table xcreateTable() {
//	    return new Table();
	//  }




	  
	  
	  

	  /**
	   * ( begin auto-generated from createReader.xml )
	   *
	   * Creates a <b>BufferedReader</b> object that can be used to read files
	   * line-by-line as individual <b>String</b> objects. This is the complement
	   * to the <b>createWriter()</b> function.
	   * <br/> <br/>
	   * Starting with Processing release 0134, all files loaded and saved by the
	   * Processing API use UTF-8 encoding. In previous releases, the default
	   * encoding for your platform was used, which causes problems when files
	   * are moved to other platforms.
	   *
	   * ( end auto-generated )
	   * @webref input:files
	   * @param filename name of the file to be opened
	   * @see BufferedReader
	   * @see StringFuncs#createWriter(String)
	   * @see PrintWriter
	   */
	  public BufferedReader createReader(String filename) {
	    InputStream is = createInput(filename);
	    if (is == null) {
	      System.err.println("The file \"" + filename + "\" " +
	                       "is missing or inaccessible, make sure " +
	                       "the URL is valid or that the file has been " +
	                       "added to your sketch and is readable.");
	      return null;
	    }
	    return createReader(is);
	  }


	  /**
	   * @nowebref
	   */
	  static public BufferedReader createReader(File file) {
	    try {
	      InputStream is = new FileInputStream(file);
	      if (file.getName().toLowerCase().endsWith(".gz")) {
	        is = new GZIPInputStream(is);
	      }
	      return createReader(is);

	    } catch (IOException e) {
	      // Re-wrap rather than forcing novices to learn about exceptions
	      throw new RuntimeException(e);
	    }
	  }


	  /**
	   * @nowebref
	   * I want to read lines from a stream. If I have to type the
	   * following lines any more I'm gonna send Sun my medical bills.
	   */
	  static public BufferedReader createReader(InputStream input) {
	    InputStreamReader isr =
	      new InputStreamReader(input, StandardCharsets.UTF_8);

	    BufferedReader reader = new BufferedReader(isr);
	    // consume the Unicode BOM (byte order marker) if present
	    try {
	      reader.mark(1);
	      int c = reader.read();
	      // if not the BOM, back up to the beginning again
	      if (c != '\uFEFF') {
	        reader.reset();
	      }
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    return reader;
	  }


	  /**
	   * ( begin auto-generated from createWriter.xml )
	   *
	   * Creates a new file in the sketch folder, and a <b>PrintWriter</b> object
	   * to write to it. For the file to be made correctly, it should be flushed
	   * and must be closed with its <b>flush()</b> and <b>close()</b> methods
	   * (see above example).
	   * <br/> <br/>
	   * Starting with Processing release 0134, all files loaded and saved by the
	   * Processing API use UTF-8 encoding. In previous releases, the default
	   * encoding for your platform was used, which causes problems when files
	   * are moved to other platforms.
	   *
	   * ( end auto-generated )
	   *
	   * @webref output:files
	   * @param filename name of the file to be created
	   * @see PrintWriter
	   * @see StringFuncs#createReader
	   * @see BufferedReader
	   */
	  public PrintWriter createWriter(String filename) {
	    return createWriter(saveFile(filename));
	  }


	  /**
	   * @nowebref
	   * I want to print lines to a file. I have RSI from typing these
	   * eight lines of code so many times.
	   */
	  static public PrintWriter createWriter(File file) {
	    if (file == null) {
	      throw new RuntimeException("File passed to createWriter() was null");
	    }
	    try {
	      createPath(file);  // make sure in-between folders exist
	      OutputStream output = new FileOutputStream(file);
	      if (file.getName().toLowerCase().endsWith(".gz")) {
	        output = new GZIPOutputStream(output);
	      }
	      return createWriter(output);

	    } catch (Exception e) {
	      throw new RuntimeException("Couldn't create a writer for " +
	                                 file.getAbsolutePath(), e);
	    }
	  }

	  /**
	   * @nowebref
	   * I want to print lines to a file. Why am I always explaining myself?
	   * It's the JavaSoft API engineers who need to explain themselves.
	   */
	  static public PrintWriter createWriter(OutputStream output) {
	    BufferedOutputStream bos = new BufferedOutputStream(output, 8192);
	    OutputStreamWriter osw =
	      new OutputStreamWriter(bos, StandardCharsets.UTF_8);
	    return new PrintWriter(osw);
	  }



	  //////////////////////////////////////////////////////////////

	  // FILE INPUT


	  /**
	   * ( begin auto-generated from createInput.xml )
	   *
	   * This is a function for advanced programmers to open a Java InputStream.
	   * It's useful if you want to use the facilities provided by StringFuncs to
	   * easily open files from the data folder or from a URL, but want an
	   * InputStream object so that you can use other parts of Java to take more
	   * control of how the stream is read.<br />
	   * <br />
	   * The filename passed in can be:<br />
	   * - A URL, for instance <b>openStream("http://processing.org/")</b><br />
	   * - A file in the sketch's <b>data</b> folder<br />
	   * - The full path to a file to be opened locally (when running as an
	   * application)<br />
	   * <br />
	   * If the requested item doesn't exist, null is returned. If not online,
	   * this will also check to see if the user is asking for a file whose name
	   * isn't properly capitalized. If capitalization is different, an error
	   * will be printed to the console. This helps prevent issues that appear
	   * when a sketch is exported to the web, where case sensitivity matters, as
	   * opposed to running from inside the Processing Development Environment on
	   * Windows or Mac OS, where case sensitivity is preserved but ignored.<br />
	   * <br />
	   * If the file ends with <b>.gz</b>, the stream will automatically be gzip
	   * decompressed. If you don't want the automatic decompression, use the
	   * related function <b>createInputRaw()</b>.
	   * <br />
	   * In earlier releases, this function was called <b>openStream()</b>.<br />
	   * <br />
	   *
	   * ( end auto-generated )
	   *
	   * <h3>Advanced</h3>
	   * Simplified method to open a Java InputStream.
	   * <p>
	   * This method is useful if you want to use the facilities provided
	   * by StringFuncs to easily open things from the data folder or from a URL,
	   * but want an InputStream object so that you can use other Java
	   * methods to take more control of how the stream is read.
	   * <p>
	   * If the requested item doesn't exist, null is returned.
	   * (Prior to 0096, die() would be called, killing the applet)
	   * <p>
	   * For 0096+, the "data" folder is exported intact with subfolders,
	   * and openStream() properly handles subdirectories from the data folder
	   * <p>
	   * If not online, this will also check to see if the user is asking
	   * for a file whose name isn't properly capitalized. This helps prevent
	   * issues when a sketch is exported to the web, where case sensitivity
	   * matters, as opposed to Windows and the Mac OS default where
	   * case sensitivity is preserved but ignored.
	   * <p>
	   * It is strongly recommended that libraries use this method to open
	   * data files, so that the loading sequence is handled in the same way
	   * as functions like loadBytes(), loadImage(), etc.
	   * <p>
	   * The filename passed in can be:
	   * <UL>
	   * <LI>A URL, for instance openStream("http://processing.org/");
	   * <LI>A file in the sketch's data folder
	   * <LI>Another file to be opened locally (when running as an application)
	   * </UL>
	   *
	   * @webref input:files
	   * @param filename the name of the file to use as input
	   * @see StringFuncs#createOutput(String)
	   * @see StringFuncs#selectOutput(String,String)
	   * @see StringFuncs#selectInput(String,String)
	   *
	   */
	  public InputStream createInput(String filename) {
	    InputStream input = createInputRaw(filename);
	    if (input != null) {
	      // if it's gzip-encoded, automatically decode
	      final String lower = filename.toLowerCase();
	      if (lower.endsWith(".gz") || lower.endsWith(".svgz")) {
	        try {
	          // buffered has to go *around* the GZ, otherwise 25x slower
	          return new BufferedInputStream(new GZIPInputStream(input));

	        } catch (IOException e) {
	          //printStackTrace(e);
	        }
	      } else {
	        return new BufferedInputStream(input);
	      }
	    }
	    return null;
	  }


	  /**
	   * Call openStream() without automatic gzip decompression.
	   */
	  public InputStream createInputRaw(String filename) {
	    if (filename == null) return null;

	 

	    if (filename.length() == 0) {
	      // an error will be called by the parent function
	      //System.err.println("The filename passed to openStream() was empty.");
	      return null;
	    }

	    // First check whether this looks like a URL
	    if (filename.contains(":")) {  // at least smells like URL
	      try {
	        URL url = new URL(filename);
	        URLConnection conn = url.openConnection();

	        if (conn instanceof HttpURLConnection) {
	          HttpURLConnection httpConn = (HttpURLConnection) conn;
	          // Will not handle a protocol change (see below)
	          httpConn.setInstanceFollowRedirects(true);
	          int response = httpConn.getResponseCode();
	          // Default won't follow HTTP -> HTTPS redirects for security reasons
	          // http://stackoverflow.com/a/1884427
	          if (response >= 300 && response < 400) {
	            String newLocation = httpConn.getHeaderField("Location");
	            return createInputRaw(newLocation);
	          }
	          return conn.getInputStream();
	        } else if (conn instanceof JarURLConnection) {
	          return url.openStream();
	        }
	      } catch (MalformedURLException mfue) {
	        // not a url, that's fine

	      } catch (FileNotFoundException fnfe) {
	        // Added in 0119 b/c Java 1.5 throws FNFE when URL not available.
	        // http://dev.processing.org/bugs/show_bug.cgi?id=403

	      } catch (IOException e) {
	        // changed for 0117, shouldn't be throwing exception
	       //printStackTrace(e);
	        //System.err.println("Error downloading from URL " + filename);
	        return null;
	        //throw new RuntimeException("Error downloading from URL " + filename);
	      }
	    }

	    InputStream stream = null;

	    // Moved this earlier than the getResourceAsStream() checks, because
	    // calling getResourceAsStream() on a directory lists its contents.
	    // http://dev.processing.org/bugs/show_bug.cgi?id=716
	    try {
	      // First see if it's in a data folder. This may fail by throwing
	      // a SecurityException. If so, this whole block will be skipped.
	      File file = new File(dataPath(filename));
	      if (!file.exists()) {
	        // next see if it's just in the sketch folder
	        file = sketchFile(filename);
	      }

	      if (file.isDirectory()) {
	        return null;
	      }
	      if (file.exists()) {
	        try {
	          // handle case sensitivity check
	          String filePath = file.getCanonicalPath();
	          String filenameActual = new File(filePath).getName();
	          // make sure there isn't a subfolder prepended to the name
	          String filenameShort = new File(filename).getName();
	          // if the actual filename is the same, but capitalized
	          // differently, warn the user.
	          //if (filenameActual.equalsIgnoreCase(filenameShort) &&
	          //!filenameActual.equals(filenameShort)) {
	          if (!filenameActual.equals(filenameShort)) {
	            throw new RuntimeException("This file is named " +
	                                       filenameActual + " not " +
	                                       filename + ". Rename the file " +
	                                       "or change your code.");
	          }
	        } catch (IOException e) { }
	      }

	      // if this file is ok, may as well just load it
	      stream = new FileInputStream(file);
	      if (stream != null) return stream;

	      // have to break these out because a general Exception might
	      // catch the RuntimeException being thrown above
	    } catch (IOException ioe) {
	    } catch (SecurityException se) { }

	    // Using getClassLoader() prevents java from converting dots
	    // to slashes or requiring a slash at the beginning.
	    // (a slash as a prefix means that it'll load from the root of
	    // the jar, rather than trying to dig into the package location)
	    ClassLoader cl = getClass().getClassLoader();

	    // by default, data files are exported to the root path of the jar.
	    // (not the data folder) so check there first.
	    stream = cl.getResourceAsStream("data/" + filename);
	    if (stream != null) {
	      String cn = stream.getClass().getName();
	      // this is an irritation of sun's java plug-in, which will return
	      // a non-null stream for an object that doesn't exist. like all good
	      // things, this is probably introduced in java 1.5. awesome!
	      // http://dev.processing.org/bugs/show_bug.cgi?id=359
	      if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
	        return stream;
	      }
	    }

	    // When used with an online script, also need to check without the
	    // data folder, in case it's not in a subfolder called 'data'.
	    // http://dev.processing.org/bugs/show_bug.cgi?id=389
	    stream = cl.getResourceAsStream(filename);
	    if (stream != null) {
	      String cn = stream.getClass().getName();
	      if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
	        return stream;
	      }
	    }

	    try {
	      // attempt to load from a local file, used when running as
	      // an application, or as a signed applet
	      try {  // first try to catch any security exceptions
	        try {
	          stream = new FileInputStream(dataPath(filename));
	          if (stream != null) return stream;
	        } catch (IOException e2) { }

	        try {
	          stream = new FileInputStream(sketchPath(filename));
	          if (stream != null) return stream;
	        } catch (Exception e) { }  // ignored

	        try {
	          stream = new FileInputStream(filename);
	          if (stream != null) return stream;
	        } catch (IOException e1) { }

	      } catch (SecurityException se) { }  // online, whups

	    } catch (Exception e) {
	      //printStackTrace(e);
	    }

	    return null;
	  }
	  /**
	   * Prepend the sketch folder path to the filename (or path) that is
	   * passed in. External libraries should use this function to save to
	   * the sketch folder.
	   * <p/>
	   * Note that when running as an applet inside a web browser,
	   * the sketchPath will be set to null, because security restrictions
	   * prevent applets from accessing that information.
	   * <p/>
	   * This will also cause an error if the sketch is not inited properly,
	   * meaning that init() was never called on the StringFuncs when hosted
	   * my some other main() or by other code. For proper use of init(),
	   * see the examples in the main description text for StringFuncs.
	   */
	  public static String sketchPath(String where) {
	    //if (sketchPath() == null) {
	      return where;
	    //}
	    // isAbsolute() could throw an access exception, but so will writing
	    // to the local disk using the sketch path, so this is safe here.
	    // for 0120, added a try/catch anyways.
	   /** try {
	      if (new File(where).isAbsolute()) return where;
	    } catch (Exception e) { }

	    return sketchPath() + File.separator + where;*/
	  }


	  /**
	   * @nowebref
	   */
	  static public InputStream createInput(File file) {
	    if (file == null) {
	      throw new IllegalArgumentException("File passed to createInput() was null");
	    }
	    if (!file.exists()) {
	      System.err.println(file + " does not exist, createInput() will return null");
	      return null;
	    }
	    try {
	      InputStream input = new FileInputStream(file);
	      final String lower = file.getName().toLowerCase();
	      if (lower.endsWith(".gz") || lower.endsWith(".svgz")) {
	        return new BufferedInputStream(new GZIPInputStream(input));
	      }
	      return new BufferedInputStream(input);

	    } catch (IOException e) {
	      System.err.println("Could not createInput() for " + file);
	      e.printStackTrace();
	      return null;
	    }
	  }


	  /**
	   * ( begin auto-generated from loadBytes.xml )
	   *
	   * Reads the contents of a file or url and places it in a byte array. If a
	   * file is specified, it must be located in the sketch's "data"
	   * directory/folder.<br />
	   * <br />
	   * The filename parameter can also be a URL to a file found online. For
	   * security reasons, a Processing sketch found online can only download
	   * files from the same server from which it came. Getting around this
	   * restriction requires a <a
	   * href="http://wiki.processing.org/w/Sign_an_Applet">signed applet</a>.
	   *
	   * ( end auto-generated )
	   * @webref input:files
	   * @param filename name of a file in the data folder or a URL.
	   * @see StringFuncs#loadStrings(String)
	   * @see StringFuncs#saveStrings(String, String[])
	   * @see StringFuncs#saveBytes(String, byte[])
	   *
	   */
	  public byte[] loadBytes(String filename) {
	    String lower = filename.toLowerCase();
	    // If it's not a .gz file, then we might be able to uncompress it into
	    // a fixed-size buffer, which should help speed because we won't have to
	    // reallocate and resize the target array each time it gets full.
	    if (!lower.endsWith(".gz")) {
	      // If this looks like a URL, try to load it that way. Use the fact that
	      // URL connections may have a content length header to size the array.
	      if (filename.contains(":")) {  // at least smells like URL
	        InputStream input = null;
	        try {
	          URL url = new URL(filename);
	          URLConnection conn = url.openConnection();
	          int length = -1;

	          if (conn instanceof HttpURLConnection) {
	            HttpURLConnection httpConn = (HttpURLConnection) conn;
	            // Will not handle a protocol change (see below)
	            httpConn.setInstanceFollowRedirects(true);
	            int response = httpConn.getResponseCode();
	            // Default won't follow HTTP -> HTTPS redirects for security reasons
	            // http://stackoverflow.com/a/1884427
	            if (response >= 300 && response < 400) {
	              String newLocation = httpConn.getHeaderField("Location");
	              return loadBytes(newLocation);
	            }
	            length = conn.getContentLength();
	            input = conn.getInputStream();
	          } else if (conn instanceof JarURLConnection) {
	            length = conn.getContentLength();
	            input = url.openStream();
	          }

	          if (input != null) {
	            byte[] buffer = null;
	            if (length != -1) {
	              buffer = new byte[length];
	              int count;
	              int offset = 0;
	              while ((count = input.read(buffer, offset, length - offset)) > 0) {
	                offset += count;
	              }
	            } else {
	              buffer = loadBytes(input);
	            }
	            input.close();
	            return buffer;
	          }
	        } catch (MalformedURLException mfue) {
	          // not a url, that's fine

	        } catch (FileNotFoundException fnfe) {
	          // Java 1.5+ throws FNFE when URL not available
	          // http://dev.processing.org/bugs/show_bug.cgi?id=403

	        } catch (IOException e) {
	          //printStackTrace(e);
	          return null;

	        } finally {
	          if (input != null) {
	            try {
	              input.close();
	            } catch (IOException e) {
	              // just deal
	            }
	          }
	        }
	      }
	    }

	    InputStream is = createInput(filename);
	    if (is != null) {
	      byte[] outgoing = loadBytes(is);
	      try {
	        is.close();
	      } catch (IOException e) {
	        //printStackTrace(e);  // shouldn't happen
	      }
	      return outgoing;
	    }

	    System.err.println("The file \"" + filename + "\" " +
	                       "is missing or inaccessible, make sure " +
	                       "the URL is valid or that the file has been " +
	                       "added to your sketch and is readable.");
	    return null;
	  }


	  /**
	   * @nowebref
	   */
	  static public byte[] loadBytes(InputStream input) {
	    try {
	      ByteArrayOutputStream out = new ByteArrayOutputStream();
	      byte[] buffer = new byte[4096];

	      int bytesRead = input.read(buffer);
	      while (bytesRead != -1) {
	        out.write(buffer, 0, bytesRead);
	        bytesRead = input.read(buffer);
	      }
	      out.flush();
	      return out.toByteArray();

	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    return null;
	  }


	  /**
	   * @nowebref
	   */
	  static public byte[] loadBytes(File file) {
	    if (!file.exists()) {
	      System.err.println(file + " does not exist, loadBytes() will return null");
	      return null;
	    }

	    try {
	      InputStream input;
	      int length;

	      if (file.getName().toLowerCase().endsWith(".gz")) {
	        RandomAccessFile raf = new RandomAccessFile(file, "r");
	        raf.seek(raf.length() - 4);
	        int b4 = raf.read();
	        int b3 = raf.read();
	        int b2 = raf.read();
	        int b1 = raf.read();
	        length = (b1 << 24) | (b2 << 16) + (b3 << 8) + b4;
	        raf.close();

	        // buffered has to go *around* the GZ, otherwise 25x slower
	        input = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)));

	      } else {
	        long len = file.length();
	        // http://stackoverflow.com/a/3039805
	        int maxArraySize = Integer.MAX_VALUE - 5;
	        if (len > maxArraySize) {
	          System.err.println("Cannot use loadBytes() on a file larger than " + maxArraySize);
	          return null;
	        }
	        length = (int) len;
	        input = new BufferedInputStream(new FileInputStream(file));
	      }
	      byte[] buffer = new byte[length];
	      int count;
	      int offset = 0;
	      // count will come back 0 when complete (or -1 if somehow going long?)
	      while ((count = input.read(buffer, offset, length - offset)) > 0) {
	        offset += count;
	      }
	      input.close();
	      return buffer;

	    } catch (IOException e) {
	      e.printStackTrace();
	      return null;
	    }
	  }


	  /**
	   * @nowebref
	   */
	  static public String[] loadStrings(File file) {
	    if (!file.exists()) {
	      System.err.println(file + " does not exist, loadStrings() will return null");
	      return null;
	    }

	    InputStream is = createInput(file);
	    if (is != null) {
	      String[] outgoing = loadStrings(is);
	      try {
	        is.close();
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
	      return outgoing;
	    }
	    return null;
	  }


	  /**
	   * ( begin auto-generated from loadStrings.xml )
	   *
	   * Reads the contents of a file or url and creates a String array of its
	   * individual lines. If a file is specified, it must be located in the
	   * sketch's "data" directory/folder.<br />
	   * <br />
	   * The filename parameter can also be a URL to a file found online. For
	   * security reasons, a Processing sketch found online can only download
	   * files from the same server from which it came. Getting around this
	   * restriction requires a <a
	   * href="http://wiki.processing.org/w/Sign_an_Applet">signed applet</a>.
	   * <br />
	   * If the file is not available or an error occurs, <b>null</b> will be
	   * returned and an error message will be printed to the console. The error
	   * message does not halt the program, however the null value may cause a
	   * NullPointerException if your code does not check whether the value
	   * returned is null.
	   * <br/> <br/>
	   * Starting with Processing release 0134, all files loaded and saved by the
	   * Processing API use UTF-8 encoding. In previous releases, the default
	   * encoding for your platform was used, which causes problems when files
	   * are moved to other platforms.
	   *
	   * ( end auto-generated )
	   *
	   * <h3>Advanced</h3>
	   * Load data from a file and shove it into a String array.
	   * <p>
	   * Exceptions are handled internally, when an error, occurs, an
	   * exception is printed to the console and 'null' is returned,
	   * but the program continues running. This is a tradeoff between
	   * 1) showing the user that there was a problem but 2) not requiring
	   * that all i/o code is contained in try/catch blocks, for the sake
	   * of new users (or people who are just trying to get things done
	   * in a "scripting" fashion. If you want to handle exceptions,
	   * use Java methods for I/O.
	   *
	   * @webref input:files
	   * @param filename name of the file or url to load
	   * @see StringFuncs#loadBytes(String)
	   * @see StringFuncs#saveStrings(String, String[])
	   * @see StringFuncs#saveBytes(String, byte[])
	   */
	  public String[] loadStrings(String filename) {
	    InputStream is = createInput(filename);
	    if (is != null) {
	      String[] strArr = loadStrings(is);
	      try {
	        is.close();
	      } catch (IOException e) {
	       //printStackTrace(e);
	      }
	      return strArr;
	    }

	    System.err.println("The file \"" + filename + "\" " +
	                       "is missing or inaccessible, make sure " +
	                       "the URL is valid or that the file has been " +
	                       "added to your sketch and is readable.");
	    return null;
	  }

	  /**
	   * @nowebref
	   */
	  static public String[] loadStrings(InputStream input) {
	    try {
	      BufferedReader reader =
	        new BufferedReader(new InputStreamReader(input, "UTF-8"));
	      return loadStrings(reader);
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    return null;
	  }


	  static public String[] loadStrings(BufferedReader reader) {
	    try {
	      String lines[] = new String[100];
	      int lineCount = 0;
	      String line = null;
	      while ((line = reader.readLine()) != null) {
	        if (lineCount == lines.length) {
	          String temp[] = new String[lineCount << 1];
	          System.arraycopy(lines, 0, temp, 0, lineCount);
	          lines = temp;
	        }
	        lines[lineCount++] = line;
	      }
	      reader.close();

	      if (lineCount == lines.length) {
	        return lines;
	      }

	      // resize array to appropriate amount for these lines
	      String output[] = new String[lineCount];
	      System.arraycopy(lines, 0, output, 0, lineCount);
	      return output;

	    } catch (IOException e) {
	      e.printStackTrace();
	      //throw new RuntimeException("Error inside loadStrings()");
	    }
	    return null;
	  }



	  //////////////////////////////////////////////////////////////

	  // FILE OUTPUT


	  /**
	   * ( begin auto-generated from createOutput.xml )
	   *
	   * Similar to <b>createInput()</b>, this creates a Java <b>OutputStream</b>
	   * for a given filename or path. The file will be created in the sketch
	   * folder, or in the same folder as an exported application.
	   * <br /><br />
	   * If the path does not exist, intermediate folders will be created. If an
	   * exception occurs, it will be printed to the console, and <b>null</b>
	   * will be returned.
	   * <br /><br />
	   * This function is a convenience over the Java approach that requires you
	   * to 1) create a FileOutputStream object, 2) determine the exact file
	   * location, and 3) handle exceptions. Exceptions are handled internally by
	   * the function, which is more appropriate for "sketch" projects.
	   * <br /><br />
	   * If the output filename ends with <b>.gz</b>, the output will be
	   * automatically GZIP compressed as it is written.
	   *
	   * ( end auto-generated )
	   * @webref output:files
	   * @param filename name of the file to open
	   * @see StringFuncs#createInput(String)
	   * @see StringFuncs#selectOutput(String,String)
	   */
	  public OutputStream createOutput(String filename) {
	    return createOutput(saveFile(filename));
	  }

	  /**
	   * @nowebref
	   */
	  static public OutputStream createOutput(File file) {
	    try {
	      createPath(file);  // make sure the path exists
	      OutputStream output = new FileOutputStream(file);
	      if (file.getName().toLowerCase().endsWith(".gz")) {
	        return new BufferedOutputStream(new GZIPOutputStream(output));
	      }
	      return new BufferedOutputStream(output);

	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    return null;
	  }


	  /**
	   * ( begin auto-generated from saveStream.xml )
	   *
	   * Save the contents of a stream to a file in the sketch folder. This is
	   * basically <b>saveBytes(blah, loadBytes())</b>, but done more efficiently
	   * (and with less confusing syntax).<br />
	   * <br />
	   * When using the <b>targetFile</b> parameter, it writes to a <b>File</b>
	   * object for greater control over the file location. (Note that unlike
	   * some other functions, this will not automatically compress or uncompress
	   * gzip files.)
	   *
	   * ( end auto-generated )
	   *
	   * @webref output:files
	   * @param target name of the file to write to
	   * @param source location to read from (a filename, path, or URL)
	   * @see StringFuncs#createOutput(String)
	   */
	  public boolean saveStream(String target, String source) {
	    return saveStream(saveFile(target), source);
	  }

	  /**
	   * Identical to the other saveStream(), but writes to a File
	   * object, for greater control over the file location.
	   * <p/>
	   * Note that unlike other api methods, this will not automatically
	   * compress or uncompress gzip files.
	   */
	  public boolean saveStream(File target, String source) {
	    return saveStream(target, createInputRaw(source));
	  }

	  /**
	   * @nowebref
	   */
	  public boolean saveStream(String target, InputStream source) {
	    return saveStream(saveFile(target), source);
	  }

	  /**
	   * @nowebref
	   */
	  static public boolean saveStream(File target, InputStream source) {
	    File tempFile = null;
	    try {
	      // make sure that this path actually exists before writing
	      createPath(target);
	      tempFile = createTempFile(target);
	      FileOutputStream targetStream = new FileOutputStream(tempFile);

	      saveStream(targetStream, source);
	      targetStream.close();
	      targetStream = null;

	      if (target.exists()) {
	        if (!target.delete()) {
	          System.err.println("Could not replace " +
	                             target.getAbsolutePath() + ".");
	        }
	      }
	      if (!tempFile.renameTo(target)) {
	        System.err.println("Could not rename temporary file " +
	                           tempFile.getAbsolutePath());
	        return false;
	      }
	      return true;

	    } catch (IOException e) {
	      if (tempFile != null) {
	        tempFile.delete();
	      }
	      e.printStackTrace();
	      return false;
	    }
	  }

	  /**
	   * @nowebref
	   */
	  static public void saveStream(OutputStream target,
	                                InputStream source) throws IOException {
	    BufferedInputStream bis = new BufferedInputStream(source, 16384);
	    BufferedOutputStream bos = new BufferedOutputStream(target);

	    byte[] buffer = new byte[8192];
	    int bytesRead;
	    while ((bytesRead = bis.read(buffer)) != -1) {
	      bos.write(buffer, 0, bytesRead);
	    }

	    bos.flush();
	  }


	  /**
	   * ( begin auto-generated from saveBytes.xml )
	   *
	   * Opposite of <b>loadBytes()</b>, will write an entire array of bytes to a
	   * file. The data is saved in binary format. This file is saved to the
	   * sketch's folder, which is opened by selecting "Show sketch folder" from
	   * the "Sketch" menu.<br />
	   * <br />
	   * It is not possible to use saveXxxxx() functions inside a web browser
	   * unless the sketch is <a
	   * href="http://wiki.processing.org/w/Sign_an_Applet">signed applet</A>. To
	   * save a file back to a server, see the <a
	   * href="http://wiki.processing.org/w/Saving_files_to_a_web-server">save to
	   * web</A> code snippet on the Processing Wiki.
	   *
	   * ( end auto-generated )
	   *
	   * @webref output:files
	   * @param filename name of the file to write to
	   * @param data array of bytes to be written
	   * @see StringFuncs#loadStrings(String)
	   * @see StringFuncs#loadBytes(String)
	   * @see StringFuncs#saveStrings(String, String[])
	   */
	  public void saveBytes(String filename, byte[] data) {
	    saveBytes(saveFile(filename), data);
	  }


	  /**
	   * Creates a temporary file based on the name/extension of another file
	   * and in the same parent directory. Ensures that the same extension is used
	   * (i.e. so that .gz files are gzip compressed on output) and that it's done
	   * from the same directory so that renaming the file later won't cross file
	   * system boundaries.
	   */
	  static private File createTempFile(File file) throws IOException {
	    File parentDir = file.getParentFile();
	    if (!parentDir.exists()) {
	      parentDir.mkdirs();
	    }
	    String name = file.getName();
	    String prefix;
	    String suffix = null;
	    int dot = name.lastIndexOf('.');
	    if (dot == -1) {
	      prefix = name;
	    } else {
	      // preserve the extension so that .gz works properly
	      prefix = name.substring(0, dot);
	      suffix = name.substring(dot);
	    }
	    // Prefix must be three characters
	    if (prefix.length() < 3) {
	      prefix += "processing";
	    }
	    return File.createTempFile(prefix, suffix, parentDir);
	  }


	  /**
	   * @nowebref
	   * Saves bytes to a specific File location specified by the user.
	   */
	  static public void saveBytes(File file, byte[] data) {
	    File tempFile = null;
	    try {
	      tempFile = createTempFile(file);

	      OutputStream output = createOutput(tempFile);
	      saveBytes(output, data);
	      output.close();
	      output = null;

	      if (file.exists()) {
	        if (!file.delete()) {
	          System.err.println("Could not replace " + file.getAbsolutePath());
	        }
	      }

	      if (!tempFile.renameTo(file)) {
	        System.err.println("Could not rename temporary file " +
	                           tempFile.getAbsolutePath());
	      }

	    } catch (IOException e) {
	      System.err.println("error saving bytes to " + file);
	      if (tempFile != null) {
	        tempFile.delete();
	      }
	      e.printStackTrace();
	    }
	  }


	  /**
	   * @nowebref
	   * Spews a buffer of bytes to an OutputStream.
	   */
	  static public void saveBytes(OutputStream output, byte[] data) {
	    try {
	      output.write(data);
	      output.flush();

	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	  }


	  //

	  /**
	   * ( begin auto-generated from saveStrings.xml )
	   *
	   * Writes an array of strings to a file, one line per string. This file is
	   * saved to the sketch's folder, which is opened by selecting "Show sketch
	   * folder" from the "Sketch" menu.<br />
	   * <br />
	   * It is not possible to use saveXxxxx() functions inside a web browser
	   * unless the sketch is <a
	   * href="http://wiki.processing.org/w/Sign_an_Applet">signed applet</A>. To
	   * save a file back to a server, see the <a
	   * href="http://wiki.processing.org/w/Saving_files_to_a_web-server">save to
	   * web</A> code snippet on the Processing Wiki.<br/>
	   * <br/ >
	   * Starting with Processing 1.0, all files loaded and saved by the
	   * Processing API use UTF-8 encoding. In previous releases, the default
	   * encoding for your platform was used, which causes problems when files
	   * are moved to other platforms.
	   *
	   * ( end auto-generated )
	   * @webref output:files
	   * @param filename filename for output
	   * @param data string array to be written
	   * @see StringFuncs#loadStrings(String)
	   * @see StringFuncs#loadBytes(String)
	   * @see StringFuncs#saveBytes(String, byte[])
	   */
	  public void saveStrings(String filename, String data[]) {
	    saveStrings(saveFile(filename), data);
	  }


	  /**
	   * @nowebref
	   */
	  static public void saveStrings(File file, String data[]) {
	    saveStrings(createOutput(file), data);
	  }


	  /**
	   * @nowebref
	   */
	  static public void saveStrings(OutputStream output, String[] data) {
	    PrintWriter writer = createWriter(output);
	    for (int i = 0; i < data.length; i++) {
	      writer.println(data[i]);
	    }
	    writer.flush();
	    writer.close();
	  }


	  //////////////////////////////////////////////////////////////


	  static protected String calcSketchPath() {
	    // try to get the user folder. if running under java web start,
	    // this may cause a security exception if the code is not signed.
	    // http://processing.org/discourse/yabb_beta/YaBB.cgi?board=Integrate;action=display;num=1159386274
	    String folder = null;
	    try {
	      folder = System.getProperty("user.dir");

	      URL jarURL =
	          StringFuncs.class.getProtectionDomain().getCodeSource().getLocation();
	      // Decode URL
	      String jarPath = jarURL.toURI().getSchemeSpecificPart();

	      // Workaround for bug in Java for OS X from Oracle (7u51)
	      // https://github.com/processing/processing/issues/2181
	     /* if (platform == MACOSX) {
	        if (jarPath.contains("Contents/Java/")) {
	          String appPath = jarPath.substring(0, jarPath.indexOf(".app") + 4);
	          File containingFolder = new File(appPath).getParentFile();
	          folder = containingFolder.getAbsolutePath();
	        }
	      } else {
	        // Working directory may not be set properly, try some options
	        // https://github.com/processing/processing/issues/2195
	        if (jarPath.contains("/lib/")) {
	          // Windows or Linux, back up a directory to get the executable
	          folder = new File(jarPath, "../..").getCanonicalPath();
	        }
	      }*/
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	    return folder;
	  }



	  public File sketchFile(String where) {
	    return new File(sketchPath(where));
	  }


	  /**
	   * Returns a path inside the applet folder to save to. Like sketchPath(),
	   * but creates any in-between folders so that things save properly.
	   * <p/>
	   * All saveXxxx() functions use the path to the sketch folder, rather than
	   * its data folder. Once exported, the data folder will be found inside the
	   * jar file of the exported application or applet. In this case, it's not
	   * possible to save data into the jar file, because it will often be running
	   * from a server, or marked in-use if running from a local file system.
	   * With this in mind, saving to the data path doesn't make sense anyway.
	   * If you know you're running locally, and want to save to the data folder,
	   * use <TT>saveXxxx("data/blah.dat")</TT>.
	   */
	  public static String savePath(String where) {
	    if (where == null) return null;
	    String filename = sketchPath(where);
	    createPath(filename);
	    return filename;
	  }


	  /**
	   * Identical to savePath(), but returns a File object.
	   */
	  public static File saveFile(String where) {
	    return new File(savePath(where));
	  }


	  static File desktopFolder;



	  /**
	   * <b>This function almost certainly does not do the thing you want it to.</b>
	   * The data path is handled differently on each platform, and should not be
	   * considered a location to write files. It should also not be assumed that
	   * this location can be read from or listed. This function is used internally
	   * as a possible location for reading files. It's still "public" as a
	   * holdover from earlier code.
	   * <p>
	   * Libraries should use createInput() to get an InputStream or createOutput()
	   * to get an OutputStream. sketchPath() can be used to get a location
	   * relative to the sketch. Again, <b>do not</b> use this to get relative
	   * locations of files. You'll be disappointed when your app runs on different
	   * platforms.
	   */
	  public String dataPath(String where) {
	    return dataFile(where).getAbsolutePath();
	  }


	  /**
	   * Return a full path to an item in the data folder as a File object.
	   * See the dataPath() method for more information.
	   */
	  public File dataFile(String where) {
	    // isAbsolute() could throw an access exception, but so will writing
	    // to the local disk using the sketch path, so this is safe here.
	    File why = new File(where);
	    if (why.isAbsolute()) return why;

	    URL jarURL = getClass().getProtectionDomain().getCodeSource().getLocation();
	    // Decode URL
	    String jarPath;
	    try {
	      jarPath = jarURL.toURI().getPath();
	    } catch (URISyntaxException e) {
	      e.printStackTrace();
	      return null;
	    }
	    if (jarPath.contains("Contents/Java/")) {
	      File containingFolder = new File(jarPath).getParentFile();
	      File dataFolder = new File(containingFolder, "data");
	      return new File(dataFolder, where);
	    }
	    // Windows, Linux, or when not using a Mac OS X .app file
	    File workingDirItem =
	      new File( File.separator + "data" + File.separator + where);
//	    if (workingDirItem.exists()) {
	    return workingDirItem;
//	    }
//	    // In some cases, the current working directory won't be set properly.
	  }


	  /**
	   * On Windows and Linux, this is simply the data folder. On Mac OS X, this is
	   * the path to the data folder buried inside Contents/Java
	   */
	//  public File inputFile(String where) {
	//  }


	//  public String inputPath(String where) {
	//  }


	  /**
	   * Takes a path and creates any in-between folders if they don't
	   * already exist. Useful when trying to save to a subfolder that
	   * may not actually exist.
	   */
	  static public void createPath(String path) {
	    createPath(new File(path));
	  }


	  static public void createPath(File file) {
	    try {
	      String parent = file.getParent();
	      if (parent != null) {
	        File unit = new File(parent);
	        if (!unit.exists()) unit.mkdirs();
	      }
	    } catch (SecurityException se) {
	      System.err.println("You don't have permissions to create " +
	                         file.getAbsolutePath());
	    }
	  }


	  static public String getExtension(String filename) {
	    String extension;

	    String lower = filename.toLowerCase();
	    int dot = filename.lastIndexOf('.');
	    if (dot == -1) {
	      return "";  // no extension found
	    }
	    extension = lower.substring(dot + 1);

	    // check for, and strip any parameters on the url, i.e.
	    // filename.jpg?blah=blah&something=that
	    int question = extension.indexOf('?');
	    if (question != -1) {
	      extension = extension.substring(0, question);
	    }

	    return extension;
	  }


	  //////////////////////////////////////////////////////////////

	  // URL ENCODING


	  static public String urlEncode(String str) {
	    try {
	      return URLEncoder.encode(str, "UTF-8");
	    } catch (UnsupportedEncodingException e) {  // oh c'mon
	      return null;
	    }
	  }


	  // DO NOT use for file paths, URLDecoder can't handle RFC2396
	  // "The recommended way to manage the encoding and decoding of
	  // URLs is to use URI, and to convert between these two classes
	  // using toURI() and URI.toURL()."
	  // https://docs.oracle.com/javase/8/docs/api/java/net/URL.html
	  static public String urlDecode(String str) {
	    try {
	      return URLDecoder.decode(str, "UTF-8");
	    } catch (UnsupportedEncodingException e) {  // safe per the JDK source
	      return null;
	    }
	  }



	  //////////////////////////////////////////////////////////////

	  // SORT


	  /**
	   * ( begin auto-generated from sort.xml )
	   *
	   * Sorts an array of numbers from smallest to largest and puts an array of
	   * words in alphabetical order. The original array is not modified, a
	   * re-ordered array is returned. The <b>count</b> parameter states the
	   * number of elements to sort. For example if there are 12 elements in an
	   * array and if count is the value 5, only the first five elements on the
	   * array will be sorted. <!--As of release 0126, the alphabetical ordering
	   * is case insensitive.-->
	   *
	   * ( end auto-generated )
	   * @webref data:array_functions
	   * @param list array to sort
	   * @see StringFuncs#reverse(boolean[])
	   */
	  static public byte[] sort(byte list[]) {
	    return sort(list, list.length);
	  }

	  /**
	        * @param count number of elements to sort, starting from 0
	   */
	  static public byte[] sort(byte[] list, int count) {
	    byte[] outgoing = new byte[list.length];
	    System.arraycopy(list, 0, outgoing, 0, list.length);
	    Arrays.sort(outgoing, 0, count);
	    return outgoing;
	  }

	  static public char[] sort(char list[]) {
	    return sort(list, list.length);
	  }

	  static public char[] sort(char[] list, int count) {
	    char[] outgoing = new char[list.length];
	    System.arraycopy(list, 0, outgoing, 0, list.length);
	    Arrays.sort(outgoing, 0, count);
	    return outgoing;
	  }

	  static public int[] sort(int list[]) {
	    return sort(list, list.length);
	  }

	  static public int[] sort(int[] list, int count) {
	    int[] outgoing = new int[list.length];
	    System.arraycopy(list, 0, outgoing, 0, list.length);
	    Arrays.sort(outgoing, 0, count);
	    return outgoing;
	  }

	  static public float[] sort(float list[]) {
	    return sort(list, list.length);
	  }

	  static public float[] sort(float[] list, int count) {
	    float[] outgoing = new float[list.length];
	    System.arraycopy(list, 0, outgoing, 0, list.length);
	    Arrays.sort(outgoing, 0, count);
	    return outgoing;
	  }

	  static public String[] sort(String list[]) {
	    return sort(list, list.length);
	  }

	  static public String[] sort(String[] list, int count) {
	    String[] outgoing = new String[list.length];
	    System.arraycopy(list, 0, outgoing, 0, list.length);
	    Arrays.sort(outgoing, 0, count);
	    return outgoing;
	  }

	  static public String[] split(String value, char delim) {
		    // do this so that the exception occurs inside the user's
		    // program, rather than appearing to be a bug inside split()
		    if (value == null) return null;
		    //return split(what, String.valueOf(delim));  // huh

		    char chars[] = value.toCharArray();
		    int splitCount = 0; //1;
		    for (int i = 0; i < chars.length; i++) {
		      if (chars[i] == delim) splitCount++;
		    }
		    // make sure that there is something in the input string
		    //if (chars.length > 0) {
		      // if the last char is a delimeter, get rid of it..
		      //if (chars[chars.length-1] == delim) splitCount--;
		      // on second thought, i don't agree with this, will disable
		    //}
		    if (splitCount == 0) {
		      String splits[] = new String[1];
		      splits[0] = value;
		      return splits;
		    }
		    //int pieceCount = splitCount + 1;
		    String splits[] = new String[splitCount + 1];
		    int splitIndex = 0;
		    int startIndex = 0;
		    for (int i = 0; i < chars.length; i++) {
		      if (chars[i] == delim) {
		        splits[splitIndex++] =
		          new String(chars, startIndex, i-startIndex);
		        startIndex = i + 1;
		      }
		    }
		    //if (startIndex != chars.length) {
		      splits[splitIndex] =
		        new String(chars, startIndex, chars.length-startIndex);
		    //}
		    return splits;
		  }


		  static public String[] split(String value, String delim) {
		    List<String> items = new ArrayList<>();
		    int index;
		    int offset = 0;
		    while ((index = value.indexOf(delim, offset)) != -1) {
		      items.add(value.substring(offset, index));
		      offset = index + delim.length();
		    }
		    items.add(value.substring(offset));
		    String[] outgoing = new String[items.size()];
		    items.toArray(outgoing);
		    return outgoing;
		  }


		  static protected LinkedHashMap<String, Pattern> matchPatterns;

		  static Pattern matchPattern(String regexp) {
		    Pattern p = null;
		    if (matchPatterns == null) {
		      matchPatterns = new LinkedHashMap<String, Pattern>(16, 0.75f, true) {
		        @Override
		        protected boolean removeEldestEntry(Map.Entry<String, Pattern> eldest) {
		          // Limit the number of match patterns at 10 most recently used
		          return size() == 10;
		        }
		      };
		    } else {
		      p = matchPatterns.get(regexp);
		    }
		    if (p == null) {
		      p = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
		      matchPatterns.put(regexp, p);
		    }
		    return p;
		  }


		  /**
		   * ( begin auto-generated from match.xml )
		   *
		   * The match() function is used to apply a regular expression to a piece of
		   * text, and return matching groups (elements found inside parentheses) as
		   * a String array. No match will return null. If no groups are specified in
		   * the regexp, but the sequence matches, an array of length one (with the
		   * matched text as the first element of the array) will be returned.<br />
		   * <br />
		   * To use the function, first check to see if the result is null. If the
		   * result is null, then the sequence did not match. If the sequence did
		   * match, an array is returned.
		   * If there are groups (specified by sets of parentheses) in the regexp,
		   * then the contents of each will be returned in the array.
		   * Element [0] of a regexp match returns the entire matching string, and
		   * the match groups start at element [1] (the first group is [1], the
		   * second [2], and so on).<br />
		   * <br />
		   * The syntax can be found in the reference for Java's <a
		   * href="http://download.oracle.com/javase/6/docs/api/">Pattern</a> class.
		   * For regular expression syntax, read the <a
		   * href="http://download.oracle.com/javase/tutorial/essential/regex/">Java
		   * Tutorial</a> on the topic.
		   *
		   * ( end auto-generated )
		   * @webref data:string_functions
		   * @param str the String to be searched
		   * @param regexp the regexp to be used for matching
		   * @see StringFuncs#matchAll(String, String)
		   * @see StringFuncs#split(String, String)
		   * @see StringFuncs#splitTokens(String, String)
		   * @see StringFuncs#join(String[], String)
		   * @see StringFuncs#trim(String)
		   */
		  static public String[] match(String str, String regexp) {
		    Pattern p = matchPattern(regexp);
		    Matcher m = p.matcher(str);
		    if (m.find()) {
		      int count = m.groupCount() + 1;
		      String[] groups = new String[count];
		      for (int i = 0; i < count; i++) {
		        groups[i] = m.group(i);
		      }
		      return groups;
		    }
		    return null;
		  }


		  /**
		   * ( begin auto-generated from matchAll.xml )
		   *
		   * This function is used to apply a regular expression to a piece of text,
		   * and return a list of matching groups (elements found inside parentheses)
		   * as a two-dimensional String array. No matches will return null. If no
		   * groups are specified in the regexp, but the sequence matches, a two
		   * dimensional array is still returned, but the second dimension is only of
		   * length one.<br />
		   * <br />
		   * To use the function, first check to see if the result is null. If the
		   * result is null, then the sequence did not match at all. If the sequence
		   * did match, a 2D array is returned. If there are groups (specified by
		   * sets of parentheses) in the regexp, then the contents of each will be
		   * returned in the array.
		   * Assuming, a loop with counter variable i, element [i][0] of a regexp
		   * match returns the entire matching string, and the match groups start at
		   * element [i][1] (the first group is [i][1], the second [i][2], and so
		   * on).<br />
		   * <br />
		   * The syntax can be found in the reference for Java's <a
		   * href="http://download.oracle.com/javase/6/docs/api/">Pattern</a> class.
		   * For regular expression syntax, read the <a
		   * href="http://download.oracle.com/javase/tutorial/essential/regex/">Java
		   * Tutorial</a> on the topic.
		   *
		   * ( end auto-generated )
		   * @webref data:string_functions
		   * @param str the String to be searched
		   * @param regexp the regexp to be used for matching
		   * @see StringFuncs#match(String, String)
		   * @see StringFuncs#split(String, String)
		   * @see StringFuncs#splitTokens(String, String)
		   * @see StringFuncs#join(String[], String)
		   * @see StringFuncs#trim(String)
		   */
		  static public String[][] matchAll(String str, String regexp) {
		    Pattern p = matchPattern(regexp);
		    Matcher m = p.matcher(str);
		    List<String[]> results = new ArrayList<>();
		    int count = m.groupCount() + 1;
		    while (m.find()) {
		      String[] groups = new String[count];
		      for (int i = 0; i < count; i++) {
		        groups[i] = m.group(i);
		      }
		      results.add(groups);
		    }
		    if (results.isEmpty()) {
		      return null;
		    }
		    String[][] matches = new String[results.size()][count];
		    for (int i = 0; i < matches.length; i++) {
		      matches[i] = results.get(i);
		    }
		    return matches;
		  }



		  //////////////////////////////////////////////////////////////

		  // CASTING FUNCTIONS, INSERTED BY PREPROC


		  /**
		   * Convert a char to a boolean. 'T', 't', and '1' will become the
		   * boolean value true, while 'F', 'f', or '0' will become false.
		   */
		  /*
		  static final public boolean parseBoolean(char what) {
		    return ((what == 't') || (what == 'T') || (what == '1'));
		  }
		  */

		  /**
		   * <p>Convert an integer to a boolean. Because of how Java handles upgrading
		   * numbers, this will also cover byte and char (as they will upgrade to
		   * an int without any sort of explicit cast).</p>
		   * <p>The preprocessor will convert boolean(what) to parseBoolean(what).</p>
		   * @return false if 0, true if any other number
		   */
		  static final public boolean parseBoolean(int what) {
		    return (what != 0);
		  }

		  /*
		  // removed because this makes no useful sense
		  static final public boolean parseBoolean(float what) {
		    return (what != 0);
		  }
		  */

		  /**
		   * Convert the string "true" or "false" to a boolean.
		   * @return true if 'what' is "true" or "TRUE", false otherwise
		   */
		  static final public boolean parseBoolean(String what) {
		    return Boolean.parseBoolean(what);
		  }

		  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

		  /*
		  // removed, no need to introduce strange syntax from other languages
		  static final public boolean[] parseBoolean(char what[]) {
		    boolean outgoing[] = new boolean[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] =
		        ((what[i] == 't') || (what[i] == 'T') || (what[i] == '1'));
		    }
		    return outgoing;
		  }
		  */

		  /**
		   * Convert a byte array to a boolean array. Each element will be
		   * evaluated identical to the integer case, where a byte equal
		   * to zero will return false, and any other value will return true.
		   * @return array of boolean elements
		   */
		  /*
		  static final public boolean[] parseBoolean(byte what[]) {
		    boolean outgoing[] = new boolean[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = (what[i] != 0);
		    }
		    return outgoing;
		  }
		  */

		  /**
		   * Convert an int array to a boolean array. An int equal
		   * to zero will return false, and any other value will return true.
		   * @return array of boolean elements
		   */
		  static final public boolean[] parseBoolean(int what[]) {
		    boolean outgoing[] = new boolean[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = (what[i] != 0);
		    }
		    return outgoing;
		  }

		  /*
		  // removed, not necessary... if necessary, convert to int array first
		  static final public boolean[] parseBoolean(float what[]) {
		    boolean outgoing[] = new boolean[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = (what[i] != 0);
		    }
		    return outgoing;
		  }
		  */

		  static final public boolean[] parseBoolean(String what[]) {
		    boolean outgoing[] = new boolean[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = Boolean.parseBoolean(what[i]);
		    }
		    return outgoing;
		  }

		  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

		  static final public byte parseByte(boolean what) {
		    return what ? (byte)1 : 0;
		  }

		  static final public byte parseByte(char what) {
		    return (byte) what;
		  }

		  static final public byte parseByte(int what) {
		    return (byte) what;
		  }

		  static final public byte parseByte(float what) {
		    return (byte) what;
		  }

		  /*
		  // nixed, no precedent
		  static final public byte[] parseByte(String what) {  // note: array[]
		    return what.getBytes();
		  }
		  */

		  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

		  static final public byte[] parseByte(boolean what[]) {
		    byte outgoing[] = new byte[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = what[i] ? (byte)1 : 0;
		    }
		    return outgoing;
		  }

		  static final public byte[] parseByte(char what[]) {
		    byte outgoing[] = new byte[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = (byte) what[i];
		    }
		    return outgoing;
		  }

		  static final public byte[] parseByte(int what[]) {
		    byte outgoing[] = new byte[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = (byte) what[i];
		    }
		    return outgoing;
		  }

		  static final public byte[] parseByte(float what[]) {
		    byte outgoing[] = new byte[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = (byte) what[i];
		    }
		    return outgoing;
		  }

		  /*
		  static final public byte[][] parseByte(String what[]) {  // note: array[][]
		    byte outgoing[][] = new byte[what.length][];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = what[i].getBytes();
		    }
		    return outgoing;
		  }
		  */

		  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

		  /*
		  static final public char parseChar(boolean what) {  // 0/1 or T/F ?
		    return what ? 't' : 'f';
		  }
		  */

		  static final public char parseChar(byte what) {
		    return (char) (what & 0xff);
		  }

		  static final public char parseChar(int what) {
		    return (char) what;
		  }

		  /*
		  static final public char parseChar(float what) {  // nonsensical
		    return (char) what;
		  }

		  static final public char[] parseChar(String what) {  // note: array[]
		    return what.toCharArray();
		  }
		  */

		  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

		  /*
		  static final public char[] parseChar(boolean what[]) {  // 0/1 or T/F ?
		    char outgoing[] = new char[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = what[i] ? 't' : 'f';
		    }
		    return outgoing;
		  }
		  */

		  static final public char[] parseChar(byte what[]) {
		    char outgoing[] = new char[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = (char) (what[i] & 0xff);
		    }
		    return outgoing;
		  }

		  static final public char[] parseChar(int what[]) {
		    char outgoing[] = new char[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = (char) what[i];
		    }
		    return outgoing;
		  }

		  /*
		  static final public char[] parseChar(float what[]) {  // nonsensical
		    char outgoing[] = new char[what.length];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = (char) what[i];
		    }
		    return outgoing;
		  }

		  static final public char[][] parseChar(String what[]) {  // note: array[][]
		    char outgoing[][] = new char[what.length][];
		    for (int i = 0; i < what.length; i++) {
		      outgoing[i] = what[i].toCharArray();
		    }
		    return outgoing;
		  }
		  */

		  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

		  static final public int parseInt(boolean what) {
		    return what ? 1 : 0;
		  }

		  /**
		   * Note that parseInt() will un-sign a signed byte value.
		   */
		  static final public int parseInt(byte what) {
		    return what & 0xff;
		  }

		  /**
		   * Note that parseInt('5') is unlike String in the sense that it
		   * won't return 5, but the ascii value. This is because ((int) someChar)
		   * returns the ascii value, and parseInt() is just longhand for the cast.
		   */
		  static final public int parseInt(char what) {
		    return what;
		  }

		  /**
		   * Same as floor(), or an (int) cast.
		   */
		  static final public int parseInt(float what) {
		    return (int) what;
		  }

		  /**
		   * Parse a String into an int value. Returns 0 if the value is bad.
		   */
		  static final public int parseInt(String what) {
		    return parseInt(what, 0);
		  }

		  /**
		   * Parse a String to an int, and provide an alternate value that
		   * should be used when the number is invalid.
		   */
		  static final public int parseInt(String what, int otherwise) {
		    try {
		      int offset = what.indexOf('.');
		      if (offset == -1) {
		        return Integer.parseInt(what);
		      } else {
		        return Integer.parseInt(what.substring(0, offset));
		      }
		    } catch (NumberFormatException e) { }
		    return otherwise;
		  }

		  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

		  static final public int[] parseInt(boolean what[]) {
		    int list[] = new int[what.length];
		    for (int i = 0; i < what.length; i++) {
		      list[i] = what[i] ? 1 : 0;
		    }
		    return list;
		  }

		  static final public int[] parseInt(byte what[]) {  // note this unsigns
		    int list[] = new int[what.length];
		    for (int i = 0; i < what.length; i++) {
		      list[i] = (what[i] & 0xff);
		    }
		    return list;
		  }

		  static final public int[] parseInt(char what[]) {
		    int list[] = new int[what.length];
		    for (int i = 0; i < what.length; i++) {
		      list[i] = what[i];
		    }
		    return list;
		  }

		  static public int[] parseInt(float what[]) {
		    int inties[] = new int[what.length];
		    for (int i = 0; i < what.length; i++) {
		      inties[i] = (int)what[i];
		    }
		    return inties;
		  }

		  /**
		   * Make an array of int elements from an array of String objects.
		   * If the String can't be parsed as a number, it will be set to zero.
		   *
		   * String s[] = { "1", "300", "44" };
		   * int numbers[] = parseInt(s);
		   *
		   * numbers will contain { 1, 300, 44 }
		   */
		  static public int[] parseInt(String what[]) {
		    return parseInt(what, 0);
		  }

		  /**
		   * Make an array of int elements from an array of String objects.
		   * If the String can't be parsed as a number, its entry in the
		   * array will be set to the value of the "missing" parameter.
		   *
		   * String s[] = { "1", "300", "apple", "44" };
		   * int numbers[] = parseInt(s, 9999);
		   *
		   * numbers will contain { 1, 300, 9999, 44 }
		   */
		  static public int[] parseInt(String what[], int missing) {
		    int output[] = new int[what.length];
		    for (int i = 0; i < what.length; i++) {
		      try {
		        output[i] = Integer.parseInt(what[i]);
		      } catch (NumberFormatException e) {
		        output[i] = missing;
		      }
		    }
		    return output;
		  }

		  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

		  /*
		  static final public float parseFloat(boolean what) {
		    return what ? 1 : 0;
		  }
		  */

		  /**
		   * Convert an int to a float value. Also handles bytes because of
		   * Java's rules for upgrading values.
		   */
		  static final public float parseFloat(int what) {  // also handles byte
		    return what;
		  }

		  static final public float parseFloat(String what) {
		    return parseFloat(what, Float.NaN);
		  }

		  static final public float parseFloat(String what, float otherwise) {
		    try {
		      return Float.parseFloat(what);
		    } catch (NumberFormatException e) { }

		    return otherwise;
		  }

		  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

		  /*
		  static final public float[] parseFloat(boolean what[]) {
		    float floaties[] = new float[what.length];
		    for (int i = 0; i < what.length; i++) {
		      floaties[i] = what[i] ? 1 : 0;
		    }
		    return floaties;
		  }

		  static final public float[] parseFloat(char what[]) {
		    float floaties[] = new float[what.length];
		    for (int i = 0; i < what.length; i++) {
		      floaties[i] = (char) what[i];
		    }
		    return floaties;
		  }
		  */

		  static final public float[] parseFloat(byte what[]) {
		    float floaties[] = new float[what.length];
		    for (int i = 0; i < what.length; i++) {
		      floaties[i] = what[i];
		    }
		    return floaties;
		  }

		  static final public float[] parseFloat(int what[]) {
		    float floaties[] = new float[what.length];
		    for (int i = 0; i < what.length; i++) {
		      floaties[i] = what[i];
		    }
		    return floaties;
		  }

		  static final public float[] parseFloat(String what[]) {
		    return parseFloat(what, Float.NaN);
		  }

		  static final public float[] parseFloat(String what[], float missing) {
		    float output[] = new float[what.length];
		    for (int i = 0; i < what.length; i++) {
		      try {
		        output[i] = Float.parseFloat(what[i]);
		      } catch (NumberFormatException e) {
		        output[i] = missing;
		      }
		    }
		    return output;
		  }

		  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

		  static final public String str(boolean x) {
		    return String.valueOf(x);
		  }

		  static final public String str(byte x) {
		    return String.valueOf(x);
		  }

		  static final public String str(char x) {
		    return String.valueOf(x);
		  }

		  static final public String str(int x) {
		    return String.valueOf(x);
		  }

		  static final public String str(float x) {
		    return String.valueOf(x);
		  }

		  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

		  static final public String[] str(boolean x[]) {
		    String s[] = new String[x.length];
		    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x[i]);
		    return s;
		  }

		  static final public String[] str(byte x[]) {
		    String s[] = new String[x.length];
		    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x[i]);
		    return s;
		  }

		  static final public String[] str(char x[]) {
		    String s[] = new String[x.length];
		    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x[i]);
		    return s;
		  }

		  static final public String[] str(int x[]) {
		    String s[] = new String[x.length];
		    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x[i]);
		    return s;
		  }

		  static final public String[] str(float x[]) {
		    String s[] = new String[x.length];
		    for (int i = 0; i < x.length; i++) s[i] = String.valueOf(x[i]);
		    return s;
		  }


		  //////////////////////////////////////////////////////////////

		  // INT NUMBER FORMATTING

		  static public String nf(float num) {
		    int inum = (int) num;
		    if (num == inum) {
		      return str(inum);
		    }
		    return str(num);
		  }

		  static public String[] nf(float[] nums) {
		    String[] outgoing = new String[nums.length];
		    for (int i = 0; i < nums.length; i++) {
		      outgoing[i] = nf(nums[i]);
		    }
		    return outgoing;
		  }

		  /**
		   * Integer number formatter.
		   */

		  static private NumberFormat int_nf;
		  static private int int_nf_digits;
		  static private boolean int_nf_commas;

		  /**
		   * ( begin auto-generated from nf.xml )
		   *
		   * Utility function for formatting numbers into strings. There are two
		   * versions, one for formatting floats and one for formatting ints. The
		   * values for the <b>digits</b>, <b>left</b>, and <b>right</b> parameters
		   * should always be positive integers.<br /><br />As shown in the above
		   * example, <b>nf()</b> is used to add zeros to the left and/or right of a
		   * number. This is typically for aligning a list of numbers. To
		   * <em>remove</em> digits from a floating-point number, use the
		   * <b>int()</b>, <b>ceil()</b>, <b>floor()</b>, or <b>round()</b>
		   * functions.
		   *
		   * ( end auto-generated )
		   * @webref data:string_functions
		   * @param nums the numbers to format
		   * @param digits number of digits to pad with zero
		   * @see StringFuncs#nfs(float, int, int)
		   * @see StringFuncs#nfp(float, int, int)
		   * @see StringFuncs#nfc(float, int)
		   * @see <a href="https://processing.org/reference/intconvert_.html">int(float)</a>
		   */

		  static public String[] nf(int nums[], int digits) {
		    String formatted[] = new String[nums.length];
		    for (int i = 0; i < formatted.length; i++) {
		      formatted[i] = nf(nums[i], digits);
		    }
		    return formatted;
		  }

		  /**
		   * @param num the number to format
		   */
		  static public String nf(int num, int digits) {
		    if ((int_nf != null) &&
		        (int_nf_digits == digits) &&
		        !int_nf_commas) {
		      return int_nf.format(num);
		    }

		    int_nf = NumberFormat.getInstance();
		    int_nf.setGroupingUsed(false); // no commas
		    int_nf_commas = false;
		    int_nf.setMinimumIntegerDigits(digits);
		    int_nf_digits = digits;
		    return int_nf.format(num);
		  }

		  /**
		   * ( begin auto-generated from nfc.xml )
		   *
		   * Utility function for formatting numbers into strings and placing
		   * appropriate commas to mark units of 1000. There are two versions, one
		   * for formatting ints and one for formatting an array of ints. The value
		   * for the <b>digits</b> parameter should always be a positive integer.
		   * <br/><br/>
		   * For a non-US locale, this will insert periods instead of commas, or
		   * whatever is apprioriate for that region.
		   *
		   * ( end auto-generated )
		   * @webref data:string_functions
		   * @param nums the numbers to format
		   * @see StringFuncs#nf(float, int, int)
		   * @see StringFuncs#nfp(float, int, int)
		   * @see StringFuncs#nfs(float, int, int)
		   */
		  static public String[] nfc(int nums[]) {
		    String formatted[] = new String[nums.length];
		    for (int i = 0; i < formatted.length; i++) {
		      formatted[i] = nfc(nums[i]);
		    }
		    return formatted;
		  }


		  /**
		   * @param num the number to format
		   */
		  static public String nfc(int num) {
		    if ((int_nf != null) &&
		        (int_nf_digits == 0) &&
		        int_nf_commas) {
		      return int_nf.format(num);
		    }

		    int_nf = NumberFormat.getInstance();
		    int_nf.setGroupingUsed(true);
		    int_nf_commas = true;
		    int_nf.setMinimumIntegerDigits(0);
		    int_nf_digits = 0;
		    return int_nf.format(num);
		  }


		  /**
		   * number format signed (or space)
		   * Formats a number but leaves a blank space in the front
		   * when it's positive so that it can be properly aligned with
		   * numbers that have a negative sign in front of them.
		   */

		  /**
		   * ( begin auto-generated from nfs.xml )
		   *
		   * Utility function for formatting numbers into strings. Similar to
		   * <b>nf()</b> but leaves a blank space in front of positive numbers so
		   * they align with negative numbers in spite of the minus symbol. There are
		   * two versions, one for formatting floats and one for formatting ints. The
		   * values for the <b>digits</b>, <b>left</b>, and <b>right</b> parameters
		   * should always be positive integers.
		   *
		   * ( end auto-generated )
		  * @webref data:string_functions
		  * @param num the number to format
		  * @param digits number of digits to pad with zeroes
		  * @see StringFuncs#nf(float, int, int)
		  * @see StringFuncs#nfp(float, int, int)
		  * @see StringFuncs#nfc(float, int)
		  */
		  static public String nfs(int num, int digits) {
		    return (num < 0) ? nf(num, digits) : (' ' + nf(num, digits));
		  }

		  /**
		   * @param nums the numbers to format
		   */
		  static public String[] nfs(int nums[], int digits) {
		    String formatted[] = new String[nums.length];
		    for (int i = 0; i < formatted.length; i++) {
		      formatted[i] = nfs(nums[i], digits);
		    }
		    return formatted;
		  }

		  //

		  /**
		   * number format positive (or plus)
		   * Formats a number, always placing a - or + sign
		   * in the front when it's negative or positive.
		   */
		 /**
		   * ( begin auto-generated from nfp.xml )
		   *
		   * Utility function for formatting numbers into strings. Similar to
		   * <b>nf()</b> but puts a "+" in front of positive numbers and a "-" in
		   * front of negative numbers. There are two versions, one for formatting
		   * floats and one for formatting ints. The values for the <b>digits</b>,
		   * <b>left</b>, and <b>right</b> parameters should always be positive integers.
		   *
		   * ( end auto-generated )
		  * @webref data:string_functions
		  * @param num the number to format
		  * @param digits number of digits to pad with zeroes
		  * @see StringFuncs#nf(float, int, int)
		  * @see StringFuncs#nfs(float, int, int)
		  * @see StringFuncs#nfc(float, int)
		  */
		  static public String nfp(int num, int digits) {
		    return (num < 0) ? nf(num, digits) : ('+' + nf(num, digits));
		  }
		  /**
		   * @param nums the numbers to format
		   */
		  static public String[] nfp(int nums[], int digits) {
		    String formatted[] = new String[nums.length];
		    for (int i = 0; i < formatted.length; i++) {
		      formatted[i] = nfp(nums[i], digits);
		    }
		    return formatted;
		  }



		  //////////////////////////////////////////////////////////////

		  // FLOAT NUMBER FORMATTING

		  static private NumberFormat float_nf;
		  static private int float_nf_left, float_nf_right;
		  static private boolean float_nf_commas;

		  /**
		   * @param left number of digits to the left of the decimal point
		   * @param right number of digits to the right of the decimal point
		   */
		  static public String[] nf(float nums[], int left, int right) {
		    String formatted[] = new String[nums.length];
		    for (int i = 0; i < formatted.length; i++) {
		      formatted[i] = nf(nums[i], left, right);
		    }
		    return formatted;
		  }

		  static public String nf(float num, int left, int right) {
		    if ((float_nf != null) &&
		        (float_nf_left == left) &&
		        (float_nf_right == right) &&
		        !float_nf_commas) {
		      return float_nf.format(num);
		    }

		    float_nf = NumberFormat.getInstance();
		    float_nf.setGroupingUsed(false);
		    float_nf_commas = false;

		    if (left != 0) float_nf.setMinimumIntegerDigits(left);
		    if (right != 0) {
		      float_nf.setMinimumFractionDigits(right);
		      float_nf.setMaximumFractionDigits(right);
		    }
		    float_nf_left = left;
		    float_nf_right = right;
		    return float_nf.format(num);
		  }

		  /**
		   * @param right number of digits to the right of the decimal point
		  */
		  static public String[] nfc(float nums[], int right) {
		    String formatted[] = new String[nums.length];
		    for (int i = 0; i < formatted.length; i++) {
		      formatted[i] = nfc(nums[i], right);
		    }
		    return formatted;
		  }

		  static public String nfc(float num, int right) {
		    if ((float_nf != null) &&
		        (float_nf_left == 0) &&
		        (float_nf_right == right) &&
		        float_nf_commas) {
		      return float_nf.format(num);
		    }

		    float_nf = NumberFormat.getInstance();
		    float_nf.setGroupingUsed(true);
		    float_nf_commas = true;

		    if (right != 0) {
		      float_nf.setMinimumFractionDigits(right);
		      float_nf.setMaximumFractionDigits(right);
		    }
		    float_nf_left = 0;
		    float_nf_right = right;
		    return float_nf.format(num);
		  }


		 /**
		  * @param left the number of digits to the left of the decimal point
		  * @param right the number of digits to the right of the decimal point
		  */
		  static public String[] nfs(float nums[], int left, int right) {
		    String formatted[] = new String[nums.length];
		    for (int i = 0; i < formatted.length; i++) {
		      formatted[i] = nfs(nums[i], left, right);
		    }
		    return formatted;
		  }

		  static public String nfs(float num, int left, int right) {
		    return (num < 0) ? nf(num, left, right) :  (' ' + nf(num, left, right));
		  }

		 /**
		  * @param left the number of digits to the left of the decimal point
		  * @param right the number of digits to the right of the decimal point
		  */
		  static public String[] nfp(float nums[], int left, int right) {
		    String formatted[] = new String[nums.length];
		    for (int i = 0; i < formatted.length; i++) {
		      formatted[i] = nfp(nums[i], left, right);
		    }
		    return formatted;
		  }

		  static public String nfp(float num, int left, int right) {
		    return (num < 0) ? nf(num, left, right) :  ('+' + nf(num, left, right));
		  }



		  //////////////////////////////////////////////////////////////

		  // HEX/BINARY CONVERSION


		  /**
		   * ( begin auto-generated from hex.xml )
		   *
		   * Converts a byte, char, int, or color to a String containing the
		   * equivalent hexadecimal notation. For example color(0, 102, 153) will
		   * convert to the String "FF006699". This function can help make your geeky
		   * debugging sessions much happier.
		   * <br/> <br/>
		   * Note that the maximum number of digits is 8, because an int value can
		   * only represent up to 32 bits. Specifying more than eight digits will
		   * simply shorten the string to eight anyway.
		   *
		   * ( end auto-generated )
		   * @webref data:conversion
		   * @param value the value to convert
		   * @see StringFuncs#unhex(String)
		   * @see StringFuncs#binary(byte)
		   * @see StringFuncs#unbinary(String)
		   */
		  static final public String hex(byte value) {
		    return hex(value, 2);
		  }

		  static final public String hex(char value) {
		    return hex(value, 4);
		  }

		  static final public String hex(int value) {
		    return hex(value, 8);
		  }
		/**
		 * @param digits the number of digits (maximum 8)
		 */
		  static final public String hex(int value, int digits) {
		    String stuff = Integer.toHexString(value).toUpperCase();
		    if (digits > 8) {
		      digits = 8;
		    }

		    int length = stuff.length();
		    if (length > digits) {
		      return stuff.substring(length - digits);

		    } else if (length < digits) {
		      return "00000000".substring(8 - (digits-length)) + stuff;
		    }
		    return stuff;
		  }

		 /**
		   * ( begin auto-generated from unhex.xml )
		   *
		   * Converts a String representation of a hexadecimal number to its
		   * equivalent integer value.
		   *
		   * ( end auto-generated )
		   *
		   * @webref data:conversion
		   * @param value String to convert to an integer
		   * @see StringFuncs#hex(int, int)
		   * @see StringFuncs#binary(byte)
		   * @see StringFuncs#unbinary(String)
		   */
		  static final public int unhex(String value) {
		    // has to parse as a Long so that it'll work for numbers bigger than 2^31
		    return (int) (Long.parseLong(value, 16));
		  }

		  //

		  /**
		   * Returns a String that contains the binary value of a byte.
		   * The returned value will always have 8 digits.
		   */
		  static final public String binary(byte value) {
		    return binary(value, 8);
		  }

		  /**
		   * Returns a String that contains the binary value of a char.
		   * The returned value will always have 16 digits because chars
		   * are two bytes long.
		   */
		  static final public String binary(char value) {
		    return binary(value, 16);
		  }

		  /**
		   * Returns a String that contains the binary value of an int. The length
		   * depends on the size of the number itself. If you want a specific number
		   * of digits use binary(int what, int digits) to specify how many.
		   */
		  static final public String binary(int value) {
		    return binary(value, 32);
		  }

		  /*
		   * Returns a String that contains the binary value of an int.
		   * The digits parameter determines how many digits will be used.
		   */

		 /**
		   * ( begin auto-generated from binary.xml )
		   *
		   * Converts a byte, char, int, or color to a String containing the
		   * equivalent binary notation. For example color(0, 102, 153, 255) will
		   * convert to the String "11111111000000000110011010011001". This function
		   * can help make your geeky debugging sessions much happier.
		   * <br/> <br/>
		   * Note that the maximum number of digits is 32, because an int value can
		   * only represent up to 32 bits. Specifying more than 32 digits will simply
		   * shorten the string to 32 anyway.
		   *
		   * ( end auto-generated )
		  * @webref data:conversion
		  * @param value value to convert
		  * @param digits number of digits to return
		  * @see StringFuncs#unbinary(String)
		  * @see StringFuncs#hex(int,int)
		  * @see StringFuncs#unhex(String)
		  */
		  static final public String binary(int value, int digits) {
		    String stuff = Integer.toBinaryString(value);
		    if (digits > 32) {
		      digits = 32;
		    }

		    int length = stuff.length();
		    if (length > digits) {
		      return stuff.substring(length - digits);

		    } else if (length < digits) {
		      int offset = 32 - (digits-length);
		      return "00000000000000000000000000000000".substring(offset) + stuff;
		    }
		    return stuff;
		  }


		 /**
		   * ( begin auto-generated from unbinary.xml )
		   *
		   * Converts a String representation of a binary number to its equivalent
		   * integer value. For example, unbinary("00001000") will return 8.
		   *
		   * ( end auto-generated )
		   * @webref data:conversion
		   * @param value String to convert to an integer
		   * @see StringFuncs#binary(byte)
		   * @see StringFuncs#hex(int,int)
		   * @see StringFuncs#unhex(String)
		   */
		  static final public int unbinary(String value) {
		    return Integer.parseInt(value, 2);
		  }


		  static public void arrayCopy(Object src, int srcPosition,
                  Object dst, int dstPosition,
                  int length) {
System.arraycopy(src, srcPosition, dst, dstPosition, length);
}

/**
* Convenience method for arraycopy().
* Identical to <CODE>arraycopy(src, 0, dst, 0, length);</CODE>
*/
static public void arrayCopy(Object src, Object dst, int length) {
System.arraycopy(src, 0, dst, 0, length);
}

/**
* Shortcut to copy the entire contents of
* the source into the destination array.
* Identical to <CODE>arraycopy(src, 0, dst, 0, src.length);</CODE>
*/
static public void arrayCopy(Object src, Object dst) {
System.arraycopy(src, 0, dst, 0, Array.getLength(src));
}

/**
* Use arrayCopy() instead.
*/
@Deprecated
static public void arraycopy(Object src, int srcPosition,
                  Object dst, int dstPosition,
                  int length) {
System.arraycopy(src, srcPosition, dst, dstPosition, length);
}

/**
* Use arrayCopy() instead.
*/
@Deprecated
static public void arraycopy(Object src, Object dst, int length) {
System.arraycopy(src, 0, dst, 0, length);
}

/**
* Use arrayCopy() instead.
*/
@Deprecated
static public void arraycopy(Object src, Object dst) {
System.arraycopy(src, 0, dst, 0, Array.getLength(src));
}
static public boolean[] subset(boolean[] list, int start) {
    return subset(list, start, list.length - start);
  }


 /**
   * ( begin auto-generated from subset.xml )
   *
   * Extracts an array of elements from an existing array. The <b>array</b>
   * parameter defines the array from which the elements will be copied and
   * the <b>offset</b> and <b>length</b> parameters determine which elements
   * to extract. If no <b>length</b> is given, elements will be extracted
   * from the <b>offset</b> to the end of the array. When specifying the
   * <b>offset</b> remember the first array element is 0. This function does
   * not change the source array.
   * <br/> <br/>
   * When using an array of objects, the data returned from the function must
   * be cast to the object array's data type. For example: <em>SomeClass[]
   * items = (SomeClass[]) subset(originalArray, 0, 4)</em>.
   *
   * ( end auto-generated )
  * @webref data:array_functions
  * @param list array to extract from
  * @param start position to begin
  * @param count number of values to extract
  * @see StringFuncs#splice(boolean[], boolean, int)
  */
  static public boolean[] subset(boolean[] list, int start, int count) {
    boolean[] output = new boolean[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public byte[] subset(byte[] list, int start) {
    return subset(list, start, list.length - start);
  }


  static public byte[] subset(byte[] list, int start, int count) {
    byte[] output = new byte[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public char[] subset(char[] list, int start) {
    return subset(list, start, list.length - start);
  }


  static public char[] subset(char[] list, int start, int count) {
    char[] output = new char[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public int[] subset(int[] list, int start) {
    return subset(list, start, list.length - start);
  }


  static public int[] subset(int[] list, int start, int count) {
    int[] output = new int[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public long[] subset(long[] list, int start) {
    return subset(list, start, list.length - start);
  }


  static public long[] subset(long[] list, int start, int count) {
    long[] output = new long[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public float[] subset(float[] list, int start) {
    return subset(list, start, list.length - start);
  }


  static public float[] subset(float[] list, int start, int count) {
    float[] output = new float[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public double[] subset(double[] list, int start) {
    return subset(list, start, list.length - start);
  }


  static public double[] subset(double[] list, int start, int count) {
    double[] output = new double[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public String[] subset(String[] list, int start) {
    return subset(list, start, list.length - start);
  }


  static public String[] subset(String[] list, int start, int count) {
    String[] output = new String[count];
    System.arraycopy(list, start, output, 0, count);
    return output;
  }


  static public Object subset(Object list, int start) {
    int length = Array.getLength(list);
    return subset(list, start, length - start);
  }


  static public Object subset(Object list, int start, int count) {
    Class<?> type = list.getClass().getComponentType();
    Object outgoing = Array.newInstance(type, count);
    System.arraycopy(list, start, outgoing, 0, count);
    return outgoing;
  }


 /**
   * ( begin auto-generated from concat.xml )
   *
   * Concatenates two arrays. For example, concatenating the array { 1, 2, 3
   * } and the array { 4, 5, 6 } yields { 1, 2, 3, 4, 5, 6 }. Both parameters
   * must be arrays of the same datatype.
   * <br/> <br/>
   * When using an array of objects, the data returned from the function must
   * be cast to the object array's data type. For example: <em>SomeClass[]
   * items = (SomeClass[]) concat(array1, array2)</em>.
   *
   * ( end auto-generated )
  * @webref data:array_functions
  * @param a first array to concatenate
  * @param b second array to concatenate
  * @see StringFuncs#splice(boolean[], boolean, int)
  * @see StringFuncs#arrayCopy(Object, int, Object, int, int)
  */
  static public boolean[] concat(boolean a[], boolean b[]) {
    boolean c[] = new boolean[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  static public byte[] concat(byte a[], byte b[]) {
    byte c[] = new byte[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  static public char[] concat(char a[], char b[]) {
    char c[] = new char[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  static public int[] concat(int a[], int b[]) {
    int c[] = new int[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  static public float[] concat(float a[], float b[]) {
    float c[] = new float[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  static public String[] concat(String a[], String b[]) {
    String c[] = new String[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  static public Object concat(Object a, Object b) {
    Class<?> type = a.getClass().getComponentType();
    int alength = Array.getLength(a);
    int blength = Array.getLength(b);
    Object outgoing = Array.newInstance(type, alength + blength);
    System.arraycopy(a, 0, outgoing, 0, alength);
    System.arraycopy(b, 0, outgoing, alength, blength);
    return outgoing;
  }

  //


 /**
   * ( begin auto-generated from reverse.xml )
   *
   * Reverses the order of an array.
   *
   * ( end auto-generated )
  * @webref data:array_functions
  * @param list booleans[], bytes[], chars[], ints[], floats[], or Strings[]
  * @see StringFuncs#sort(String[], int)
  */
  static public boolean[] reverse(boolean list[]) {
    boolean outgoing[] = new boolean[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }

  static public byte[] reverse(byte list[]) {
    byte outgoing[] = new byte[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }

  static public char[] reverse(char list[]) {
    char outgoing[] = new char[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }

  static public int[] reverse(int list[]) {
    int outgoing[] = new int[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }

  static public float[] reverse(float list[]) {
    float outgoing[] = new float[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }

  static public String[] reverse(String list[]) {
    String outgoing[] = new String[list.length];
    int length1 = list.length - 1;
    for (int i = 0; i < list.length; i++) {
      outgoing[i] = list[length1 - i];
    }
    return outgoing;
  }

  static public Object reverse(Object list) {
    Class<?> type = list.getClass().getComponentType();
    int length = Array.getLength(list);
    Object outgoing = Array.newInstance(type, length);
    for (int i = 0; i < length; i++) {
      Array.set(outgoing, i, Array.get(list, (length - 1) - i));
    }
    return outgoing;
  }



  //////////////////////////////////////////////////////////////

  // STRINGS


  /**
   * ( begin auto-generated from trim.xml )
   *
   * Removes whitespace characters from the beginning and end of a String. In
   * addition to standard whitespace characters such as space, carriage
   * return, and tab, this function also removes the Unicode "nbsp" character.
   *
   * ( end auto-generated )
   * @webref data:string_functions
   * @param str any string
   * @see StringFuncs#split(String, String)
   * @see StringFuncs#join(String[], char)
   */
  static public String trim(String str) {
    if (str == null) {
      return null;
    }
    return str.replace('\u00A0', ' ').trim();
  }


 /**
  * @param array a String array
  */
  static public String[] trim(String[] array) {
    if (array == null) {
      return null;
    }
    String[] outgoing = new String[array.length];
    for (int i = 0; i < array.length; i++) {
      if (array[i] != null) {
        outgoing[i] = trim(array[i]);
      }
    }
    return outgoing;
  }


  /**
   * ( begin auto-generated from join.xml )
   *
   * Combines an array of Strings into one String, each separated by the
   * character(s) used for the <b>separator</b> parameter. To join arrays of
   * ints or floats, it's necessary to first convert them to strings using
   * <b>nf()</b> or <b>nfs()</b>.
   *
   * ( end auto-generated )
   * @webref data:string_functions
   * @param list array of Strings
   * @param separator char or String to be placed between each item
   * @see StringFuncs#split(String, String)
   * @see StringFuncs#trim(String)
   * @see StringFuncs#nf(float, int, int)
   * @see StringFuncs#nfs(float, int, int)
   */
  static public String join(String[] list, char separator) {
    return join(list, String.valueOf(separator));
  }


  static public String join(String[] list, String separator) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < list.length; i++) {
      if (i != 0) sb.append(separator);
      sb.append(list[i]);
    }
    return sb.toString();
  }


  static public String[] splitTokens(String value) {
    return splitTokens(value, WHITESPACE);
  }


  /**
   * ( begin auto-generated from splitTokens.xml )
   *
   * The splitTokens() function splits a String at one or many character
   * "tokens." The <b>tokens</b> parameter specifies the character or
   * characters to be used as a boundary.
   * <br/> <br/>
   * If no <b>tokens</b> character is specified, any whitespace character is
   * used to split. Whitespace characters include tab (\\t), line feed (\\n),
   * carriage return (\\r), form feed (\\f), and space. To convert a String
   * to an array of integers or floats, use the datatype conversion functions
   * <b>int()</b> and <b>float()</b> to convert the array of Strings.
   *
   * ( end auto-generated )
   * @webref data:string_functions
   * @param value the String to be split
   * @param delim list of individual characters that will be used as separators
   * @see StringFuncs#split(String, String)
   * @see StringFuncs#join(String[], String)
   * @see StringFuncs#trim(String)
   */
  static public String[] splitTokens(String value, String delim) {
    StringTokenizer toker = new StringTokenizer(value, delim);
    String pieces[] = new String[toker.countTokens()];

    int index = 0;
    while (toker.hasMoreTokens()) {
      pieces[index++] = toker.nextToken();
    }
    return pieces;
  }

  
  static public boolean[] expand(boolean list[]) {
	    return expand(list, list.length > 0 ? list.length << 1 : 1);
	  }

	  /**
	   * @param newSize new size for the array
	   */
	  static public boolean[] expand(boolean list[], int newSize) {
	    boolean temp[] = new boolean[newSize];
	    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
	    return temp;
	  }

	  static public byte[] expand(byte list[]) {
	    return expand(list, list.length > 0 ? list.length << 1 : 1);
	  }

	  static public byte[] expand(byte list[], int newSize) {
	    byte temp[] = new byte[newSize];
	    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
	    return temp;
	  }

	  static public char[] expand(char list[]) {
	    return expand(list, list.length > 0 ? list.length << 1 : 1);
	  }

	  static public char[] expand(char list[], int newSize) {
	    char temp[] = new char[newSize];
	    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
	    return temp;
	  }

	  static public int[] expand(int list[]) {
	    return expand(list, list.length > 0 ? list.length << 1 : 1);
	  }

	  static public int[] expand(int list[], int newSize) {
	    int temp[] = new int[newSize];
	    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
	    return temp;
	  }

	  static public long[] expand(long list[]) {
	    return expand(list, list.length > 0 ? list.length << 1 : 1);
	  }

	  static public long[] expand(long list[], int newSize) {
	    long temp[] = new long[newSize];
	    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
	    return temp;
	  }

	  static public float[] expand(float list[]) {
	    return expand(list, list.length > 0 ? list.length << 1 : 1);
	  }

	  static public float[] expand(float list[], int newSize) {
	    float temp[] = new float[newSize];
	    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
	    return temp;
	  }

	  static public double[] expand(double list[]) {
	    return expand(list, list.length > 0 ? list.length << 1 : 1);
	  }

	  static public double[] expand(double list[], int newSize) {
	    double temp[] = new double[newSize];
	    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
	    return temp;
	  }

	  static public String[] expand(String list[]) {
	    return expand(list, list.length > 0 ? list.length << 1 : 1);
	  }

	  static public String[] expand(String list[], int newSize) {
	    String temp[] = new String[newSize];
	    // in case the new size is smaller than list.length
	    System.arraycopy(list, 0, temp, 0, Math.min(newSize, list.length));
	    return temp;
	  }

	 /**
	  * @nowebref
	  */
	  static public Object expand(Object array) {
	    int len = Array.getLength(array);
	    return expand(array, len > 0 ? len << 1 : 1);
	  }

	  static public Object expand(Object list, int newSize) {
	    Class<?> type = list.getClass().getComponentType();
	    Object temp = Array.newInstance(type, newSize);
	    System.arraycopy(list, 0, temp, 0,
	                     Math.min(Array.getLength(list), newSize));
	    return temp;
	  }

	  // contract() has been removed in revision 0124, use subset() instead.
	  // (expand() is also functionally equivalent)

	  /**
	   * ( begin auto-generated from append.xml )
	   *
	   * Expands an array by one element and adds data to the new position. The
	   * datatype of the <b>element</b> parameter must be the same as the
	   * datatype of the array.
	   * <br/> <br/>
	   * When using an array of objects, the data returned from the function must
	   * be cast to the object array's data type. For example: <em>SomeClass[]
	   * items = (SomeClass[]) append(originalArray, element)</em>.
	   *
	   * ( end auto-generated )
	   *
	   * @webref data:array_functions
	   * @param array array to append
	   * @param value new data for the array
	   * @see StringFuncs#shorten(boolean[])
	   * @see StringFuncs#expand(boolean[])
	   */
	  static public byte[] append(byte array[], byte value) {
	    array = expand(array, array.length + 1);
	    array[array.length-1] = value;
	    return array;
	  }

	  static public char[] append(char array[], char value) {
	    array = expand(array, array.length + 1);
	    array[array.length-1] = value;
	    return array;
	  }

	  static public int[] append(int array[], int value) {
	    array = expand(array, array.length + 1);
	    array[array.length-1] = value;
	    return array;
	  }

	  static public float[] append(float array[], float value) {
	    array = expand(array, array.length + 1);
	    array[array.length-1] = value;
	    return array;
	  }

	  static public String[] append(String array[], String value) {
	    array = expand(array, array.length + 1);
	    array[array.length-1] = value;
	    return array;
	  }

	  static public Object append(Object array, Object value) {
	    int length = Array.getLength(array);
	    array = expand(array, length + 1);
	    Array.set(array, length, value);
	    return array;
	  }


	 /**
	   * ( begin auto-generated from shorten.xml )
	   *
	   * Decreases an array by one element and returns the shortened array.
	   * <br/> <br/>
	   * When using an array of objects, the data returned from the function must
	   * be cast to the object array's data type. For example: <em>SomeClass[]
	   * items = (SomeClass[]) shorten(originalArray)</em>.
	   *
	   * ( end auto-generated )
	   *
	   * @webref data:array_functions
	   * @param list array to shorten
	   * @see StringFuncs#append(byte[], byte)
	   * @see StringFuncs#expand(boolean[])
	   */
	  static public boolean[] shorten(boolean list[]) {
	    return subset(list, 0, list.length-1);
	  }

	  static public byte[] shorten(byte list[]) {
	    return subset(list, 0, list.length-1);
	  }

	  static public char[] shorten(char list[]) {
	    return subset(list, 0, list.length-1);
	  }

	  static public int[] shorten(int list[]) {
	    return subset(list, 0, list.length-1);
	  }

	  static public float[] shorten(float list[]) {
	    return subset(list, 0, list.length-1);
	  }

	  static public String[] shorten(String list[]) {
	    return subset(list, 0, list.length-1);
	  }

	  static public Object shorten(Object list) {
	    int length = Array.getLength(list);
	    return subset(list, 0, length - 1);
	  }


	  /**
	   * ( begin auto-generated from splice.xml )
	   *
	   * Inserts a value or array of values into an existing array. The first two
	   * parameters must be of the same datatype. The <b>array</b> parameter
	   * defines the array which will be modified and the second parameter
	   * defines the data which will be inserted.
	   * <br/> <br/>
	   * When using an array of objects, the data returned from the function must
	   * be cast to the object array's data type. For example: <em>SomeClass[]
	   * items = (SomeClass[]) splice(array1, array2, index)</em>.
	   *
	   * ( end auto-generated )
	   * @webref data:array_functions
	   * @param list array to splice into
	   * @param value value to be spliced in
	   * @param index position in the array from which to insert data
	   * @see StringFuncs#concat(boolean[], boolean[])
	   * @see StringFuncs#subset(boolean[], int, int)
	   */
	  static final public boolean[] splice(boolean list[],
	                                       boolean value, int index) {
	    boolean outgoing[] = new boolean[list.length + 1];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    outgoing[index] = value;
	    System.arraycopy(list, index, outgoing, index + 1,
	                     list.length - index);
	    return outgoing;
	  }

	  static final public boolean[] splice(boolean list[],
	                                       boolean value[], int index) {
	    boolean outgoing[] = new boolean[list.length + value.length];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    System.arraycopy(value, 0, outgoing, index, value.length);
	    System.arraycopy(list, index, outgoing, index + value.length,
	                     list.length - index);
	    return outgoing;
	  }

	  static final public byte[] splice(byte list[],
	                                    byte value, int index) {
	    byte outgoing[] = new byte[list.length + 1];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    outgoing[index] = value;
	    System.arraycopy(list, index, outgoing, index + 1,
	                     list.length - index);
	    return outgoing;
	  }

	  static final public byte[] splice(byte list[],
	                                    byte value[], int index) {
	    byte outgoing[] = new byte[list.length + value.length];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    System.arraycopy(value, 0, outgoing, index, value.length);
	    System.arraycopy(list, index, outgoing, index + value.length,
	                     list.length - index);
	    return outgoing;
	  }


	  static final public char[] splice(char list[],
	                                    char value, int index) {
	    char outgoing[] = new char[list.length + 1];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    outgoing[index] = value;
	    System.arraycopy(list, index, outgoing, index + 1,
	                     list.length - index);
	    return outgoing;
	  }

	  static final public char[] splice(char list[],
	                                    char value[], int index) {
	    char outgoing[] = new char[list.length + value.length];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    System.arraycopy(value, 0, outgoing, index, value.length);
	    System.arraycopy(list, index, outgoing, index + value.length,
	                     list.length - index);
	    return outgoing;
	  }

	  static final public int[] splice(int list[],
	                                   int value, int index) {
	    int outgoing[] = new int[list.length + 1];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    outgoing[index] = value;
	    System.arraycopy(list, index, outgoing, index + 1,
	                     list.length - index);
	    return outgoing;
	  }

	  static final public int[] splice(int list[],
	                                   int value[], int index) {
	    int outgoing[] = new int[list.length + value.length];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    System.arraycopy(value, 0, outgoing, index, value.length);
	    System.arraycopy(list, index, outgoing, index + value.length,
	                     list.length - index);
	    return outgoing;
	  }

	  static final public float[] splice(float list[],
	                                     float value, int index) {
	    float outgoing[] = new float[list.length + 1];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    outgoing[index] = value;
	    System.arraycopy(list, index, outgoing, index + 1,
	                     list.length - index);
	    return outgoing;
	  }

	  static final public float[] splice(float list[],
	                                     float value[], int index) {
	    float outgoing[] = new float[list.length + value.length];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    System.arraycopy(value, 0, outgoing, index, value.length);
	    System.arraycopy(list, index, outgoing, index + value.length,
	                     list.length - index);
	    return outgoing;
	  }

	  static final public String[] splice(String list[],
	                                      String value, int index) {
	    String outgoing[] = new String[list.length + 1];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    outgoing[index] = value;
	    System.arraycopy(list, index, outgoing, index + 1,
	                     list.length - index);
	    return outgoing;
	  }

	  static final public String[] splice(String list[],
	                                      String value[], int index) {
	    String outgoing[] = new String[list.length + value.length];
	    System.arraycopy(list, 0, outgoing, 0, index);
	    System.arraycopy(value, 0, outgoing, index, value.length);
	    System.arraycopy(list, index, outgoing, index + value.length,
	                     list.length - index);
	    return outgoing;
	  }

	  static final public Object splice(Object list, Object value, int index) {
	    Class<?> type = list.getClass().getComponentType();
	    Object outgoing = null;
	    int length = Array.getLength(list);

	    // check whether item being spliced in is an array
	    if (value.getClass().getName().charAt(0) == '[') {
	      int vlength = Array.getLength(value);
	      outgoing = Array.newInstance(type, length + vlength);
	      System.arraycopy(list, 0, outgoing, 0, index);
	      System.arraycopy(value, 0, outgoing, index, vlength);
	      System.arraycopy(list, index, outgoing, index + vlength, length - index);

	    } else {
	      outgoing = Array.newInstance(type, length + 1);
	      System.arraycopy(list, 0, outgoing, 0, index);
	      Array.set(outgoing, index, value);
	      System.arraycopy(list, index, outgoing, index + 1, length - index);
	    }
	    return outgoing;
	  }
	  Random internalRandom;
	  
	  /**
	   *
	   */
	  public final float random(float high) {
	    // avoid an infinite loop when 0 or NaN are passed in
	    if (high == 0 || high != high) {
	      return 0;
	    }

	    if (internalRandom == null) {
	      internalRandom = new Random();
	    }

	    // for some reason (rounding error?) Math.random() * 3
	    // can sometimes return '3' (once in ~30 million tries)
	    // so a check was added to avoid the inclusion of 'howbig'
	    float value = 0;
	    do {
	      value = internalRandom.nextFloat() * high;
	    } while (value == high);
	    return value;
	  }
	
  }
