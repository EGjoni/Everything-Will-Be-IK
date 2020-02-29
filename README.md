# Everything-WIll-Be-IK
'Everything Will Be IK' is a robust Inverse Kinematics library. 
</br></br>

See here for a demo: https://youtu.be/y_o34kOx_FA

It relies on a novel (I'm still writing an explainer) highly stable generalization of CCD. 
</br></br>
<b>Features:</b>
<ul>
<li>Position AND orientation targets (6-DOF).</li>
<li>Highly stable.</li>
<li>Multiple end-effector support</li>
<li>Intermediary effector support.</li>
<li>Dampening (stiffness control).</li>
<li>Target weight/priority (per target, per degree of freedom).</li>
<li>Highly versatile 3-DOF constraints with arbitrarily shaped orientation regions.</li>
<li>"Soft" constraint support, allowing joints to meet the target in the least uncomfortable way.</li>
</ul>
</br>

The code is quite fast and suitable for realtime use in most graphics applications. A fully constrained humanoid torso effectored at the hips, hands and head (simultaneously trying to reach all four corresponding targets in position and orientation) will solve in well under a millisecond (roughly 0.2 milliseconds on an 8 year old mid-level consumer grade CPU). But further optimizations are likely still possible with data-structure tweaks.
</br>
</br>
Please let me know if you find bugs you can't fix. Please commit back changes for any bugs you do fix. 
</br>
</br>
<br></br>

<b>DISCLAIMER: This code was intended primarily for graphics applications and has not been thoroughly tested for use in robotics. Until this disclaimer disappears (or you have independently verified it is suitable for your purposes) please do not use this code to command any servos that can put out enough torque to cause damage to people, property, or other components in your build.</b>







