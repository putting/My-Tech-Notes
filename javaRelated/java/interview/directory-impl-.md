# Problem: Model a Directory Structure

## Basic OO modelling BUT there must be a bunch of further questions around hierarchies and Lists
Bit old school from 2011, but contains general idea. Should be using generics and NOT Vector.
(http://www.jguru.com/article/core/files-and-directories-in-java.html)

## TODO: Further Questions
- Recursive finding of files
- Return only leaf files
- Modelling in db

### A Directory which can contain subdirs and files.
```java
import java.util.*;

public class MyDir {
  private String path;
  private String name;
   // All files in this directory:
  private Vector files = new Vector(); Should reall be List<MyFile>
   // All directories in this directory:
  private Vector dirs = new Vector();  // List<MyDir>

  public MyDir(String path, String name) {
    this.path = path;
    this.name = name;
  }  
  
  public String getName() { return name; }  

  public String getPath() { return path; }  
  
  public void addFile(MyFile f) { files.addElement(f); }  

  public void addDir(MyDir d) { dirs.addElement(d); }  
  
  public Vector getFiles() { return files; }  
  
  public Vector getDirs() { return dirs; }  
  
}

```

### MyFile models files
```java
package hansen.playground;
	
public class MyFile {
  private String path;
  private String name;

  /*
  * Set path and file name.
  * Example: For the file "MyFile.class" in 
  * "classes\hansen\playground" we have:
  *   file = "MyFile.class" and
  *   path = "classes\hansen\playground"
  */
  public MyFile(String path, String name) {
    this.path = path;
    this.name = name;
  }  
  
  /*
  * Get the name of the file
  */
  public String getName() { return name; }  

  /*
  * Get the path of the file
  */
  public String getPath() { return path; }  
}
```

### Testing and recursing
```java
public class MyFileStructure{ 
  private String dirname;
  private MyDir mdir;
  protected String result;

public String getResult() {
    return result;
  }

. . . 
  
  /*
  * Display the name of the file on System.out   
  */
  protected void outFile(MyFile f, int level) {
    String name = f.getName();
    result += repeat(" ",2*level) + "File: " + name + "\n";
  }  
  
  /*
  * Display the name of the directory on System.out
  */
  protected void outDir(MyDir d, int level) {
    String name = d.getName();
    result += repeat(" ",2*level) + "Dir: " + name + "\n";
  }  
  
  /*
  * Close display of the directory 
  */
  protected void outEndDir() {
  }  
  
  public void list() {
    if (mdir == null) {
      System.out.println("Not a valid directory");
      return;
    }  
    result = "";
    outDir(mdir, 0);
    list(mdir, 0);
    outEndDir();
  }
  
  private void list(MyDir m, int level) {
    level++;
    Vector md = m.getDirs();
    for (int i = 0; i < md.size(); i++) {
      MyDir d = (MyDir)md.elementAt(i);
      outDir(d, level);
      list(d, level); // recursive call
      outEndDir();
    }  
    Vector mf = m.getFiles();
    for (int i = 0; i < mf.size(); i++) {
      MyFile f = (MyFile)mf.elementAt(i);
      outFile(f,level); 
    }  
  }  
 
}
```
