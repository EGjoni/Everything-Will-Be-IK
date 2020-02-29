# Agnostic Savior (Java)


<h4>What is it?</h4>
<p>Agnostic Savior is a simple scheme for saving and loading Object Oriented data in a programming language agnostic JSON format.</p>  

<h4>What is it Useful For?</h4>
<p>
Porting Mostly. For example, say you want to port some java program or library into another language. Ideally, you'd like to test to make sure that results 
of the Java program are the same as the results of your port, or vice-versa. You could of course do this reductively, making sure that every part behaves as intended, 
thereby presumably ensuring that the whole does as well, but when the whole doesn't regardless, it might be helpful to check a complete save state in the original 
against a loaded version of the complete state in your port.</p> 
<p>Beyond that, it's probably useful if you want to save and load stuff from and into your Java program, but don't want to spend a lot of time figuring out a structure 
for a file format. (You could, of course, also use the Java Serializable class. This library is for situations where you'd prefer not to).</p>
 
<h4>I'm Sold. How do I use it?</h4> 
<p>
To every class you want to save, implement the `Saveable` interface offered by this library. 
To each class implementing the `Saveable` interface, add the following method </p>
  
```
@Override
makeSaveable(Savemanager s) { 
  someObjectThisDependsOn.makeSaveable(s); 
  someOtherObjectThisDependsOn.makeSaveable(s);
  anotherObjectThisDependsOn.makeSaveable(s); 
  s.addToSaveState(this); 
}
```

