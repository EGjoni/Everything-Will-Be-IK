# Everything-WIll-Be-IK
'Everything Will Be IK' is a robust Inverse Kinematics library For Processing. 
</br></br>

It is based on a generalization of CCD (highly stable), which incorporates elements of FABRIK(highly versatile). 
</br></br>
<b>Features:</b></br>
-Highly stable.</br>
-Multiple end-effector support</br>
-intermediary effector support.</br>
-dampening (stiffness control)</br>
-1-DOF constraints.</br>
-2-DOF constraints. </br>
-3-DOF constraints.</br>
</br>
</br>
The code isn't optimized, but the algorithm is fast enough that it shouldn't matter too much for most applications. 
</br>
</br>
Please e-mail me if you find bugs you can't fix. Please commit back changes for any bugs you do fix. 
The library is new, so, you might need to work around the occasional null pointer exception.
Please refer to the included examples when in doubt.
</br>
</br>
Coming soon: Better documentation, algorithm outline, and generalized Java library. 

<br></br>

<b>Installation Instructions:</b></br>
-Download a .zip from this repository. 
-Locate your processing sketchbook folder. (You can find this from within the Processing IDE by clicking File -> Preferences). </br>
-Navigate to that sketchbook directory.<br>
-Extract the ewbIK folder into the 'libraries' folder within your sketchbook.<br>
-The final directory layout should look something like<br>
..sketchbook/<br>
&nbsp;&nbsp;&nbsp;&nbsp;┣ libraries/<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;┣ ewbIK</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;┣ examples/<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;┣ library/</br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;┣ reference/</br>

<b>DISCLAIMER: This code was intended primarily for graphics applications and has not been thoroughly tested for use in robotics. Until this disclaimer disappears (or you have independently verified it is suitable for your purposes) please do not use this code to command any servos that can put out enough torque to cause damage to people, property, or other components in your build.</b>







