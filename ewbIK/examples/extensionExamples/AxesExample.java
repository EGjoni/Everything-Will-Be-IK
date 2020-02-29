/*

Copyright (c) 2015 Eron Gjoni

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and 
associated documentation files (the "Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 

 */

package extensionExamples;
import sceneGraph.*;
import sceneGraph.math.SGVec_3d;
import sceneGraph.math.Vec3d;

/***
 * This class is a reference implementation showing how to extend AbstractAxes. 
 * You can use it as-is if you don't mind working with the Vector representations 
 * used by this library. But if you are dropping this library into an existing framework, 
 * it is recommended that you extend AbstractAxes to behave as a wrapper 
 * that takes your framework's Vector instances as input, and converts them to this 
 * libraries native implementation before processing, then back into your preferred implementation 
 * before returning. 
 * 
 * If you can think of a better way to handle this that increases extensibility without 
 * sacrificing speed, please let me know or contribute a better solution. 
 *  
 * @author Eron Gjoni
 */
public class AxesExample extends AbstractAxes {

	public AxesExample(ExampleVector origin, 
			ExampleVector inX, 
			ExampleVector inY, 
			ExampleVector inZ, 
			boolean forceOrthoNormality,
			AbstractAxes parent) {
		
		super(
				toSGVec(origin),
				toSGVec(inX), 
				toSGVec(inY),
				toSGVec(inZ),
				forceOrthoNormality,
				parent
				);
	}
	
	public AxesExample(SGVec_3d origin, 
			SGVec_3d inX, 
			SGVec_3d inY, 
			SGVec_3d inZ) {
		this(origin, inX, inY, inZ, true, null);
	}
	
	
	public AxesExample() {
		super(
				new SGVec_3d(0,0,0),
				new SGVec_3d(1,0,0), 
				new SGVec_3d(0,1,0),
				new SGVec_3d(0,0,1),
				true,
				null
				);
	}
	
	public AxesExample(SGVec_3d origin, SGVec_3d inX, SGVec_3d inY, SGVec_3d inZ, boolean forceOrthoNormality, AbstractAxes parent) {
		super(origin, inX, inY, inZ, forceOrthoNormality, parent);
	}
	@Override
	protected AbstractAxes instantiate(
			SGVec_3d origin, 
			SGVec_3d gXHeading,
			SGVec_3d gYHeading, 
			SGVec_3d gZHeading, 
			boolean forceOrthoNormality,
			AbstractAxes parent) {
		return new AxesExample(origin, gXHeading, gYHeading, gZHeading, forceOrthoNormality, parent);
	}

	/**conversion functions. Replace these with functions that convert to and from your 
	* framework's native vector and ray representations.
	*/
	//////////////////////////////////////////////////////////////////////////////////////////////
	public static ExampleVector toExampleVector(SGVec_3d sv) {
		return new ExampleVector(sv.x, sv.y, sv.z);
	}
	
	public static void toExampleVector(SGVec_3d sv, ExampleVector storeIn) {
		storeIn.x = sv.x;
		storeIn.y = sv.y;
		storeIn.z = sv.z;
	}

	public static ExampleRay toExampleRay(sgRay sr) {
		return new ExampleRay(
					toExampleVector(sr.p1()),
					toExampleVector(sr.p2())
				);
	}
	
	public static void toExampleRay(sgRay sr, ExampleRay storeIn) {
		storeIn.p1 = toExampleVector(sr.p1());
		storeIn.p2 = toExampleVector(sr.p2());
	}
	
	public static SGVec_3d toSGVec(ExampleVector ev) {
		return new SGVec_3d(ev.x, ev.y, ev.z);
	}
	
	public static sgRay toSgRay(ExampleRay er) {
		return new sgRay(
					toSGVec(er.p1), 
					toSGVec(er.p2)
				);
	}
	
	//////////////////// END OF CONVERSION FUNCTIONS
	
	
	
	///WRAPPER FUNCTIONS. Basically just find + replace these with the appropriate class names and conversion functions above and you should be good to go. 

		
	public ExampleRay x(){
		return toExampleRay(
				super.x_()
			);
	}
	public ExampleRay y(){ 
		return toExampleRay(
				super.y_()
			);
	}
	public ExampleRay z(){ 
		return toExampleRay(
				super.z_()
				);
	}
	public ExampleRay x_norm(){ 
		return toExampleRay(
				super.x_norm_()
				);
	}
	
	public ExampleRay y_norm(){
		return toExampleRay(
				super.y_norm_()
				);
	}
	public ExampleRay z_norm(){ 
		return toExampleRay(
				super.z_norm_()
				);
	}
	
	public ExampleRay x_raw(){ 
		return toExampleRay(
				super.x_raw_()
			);
	}
	public ExampleRay y_raw(){ 
		return toExampleRay(
				super.y_raw_()
				);
	}
	public ExampleRay z_raw(){ 
		return toExampleRay(
				super.z_raw_()
				);
	}		
	
	public ExampleVector orthonormal_Z(){
		return toExampleVector(
				super.orthonormal_Z_()
			);
	}
	
	public ExampleVector getGlobalOf(ExampleVector local_input){
		return toExampleVector(
				super.getGlobalOf(
						toSGVec(local_input))
			);
	}
	public ExampleVector getOrthoNormalizedGlobalOf(ExampleVector local_input){
		return toExampleVector(
				super.getOrthoNormalizedGlobalOf(
						toSGVec(local_input)
						)
			);
	}
	
	public ExampleVector setToGlobalOf(ExampleVector local_input){
		return toExampleVector(
				super.setToGlobalOf(
						toSGVec(local_input)
						)
			);
	}
	
	public void setToGlobalOf(ExampleVector local_input, ExampleVector global_output){
			toExampleVector(
				super.setToGlobalOf(
						toSGVec(local_input)
						),
				global_output
			);		
	}
	
	public void setToRawGlobalOf(ExampleVector local_input, ExampleVector global_output) {
		SGVec_3d tempVec = new SGVec_3d(); 
		super.setToRawGlobalOf(
				toSGVec(local_input), 
				tempVec
				);
		toExampleVector(
				tempVec,
				global_output
			);
	}
	public void setToOrthoNormalizedGlobalOf(ExampleVector local_input, ExampleVector global_output){
		SGVec_3d tempVec = new SGVec_3d(); 
		super.setToOrthoNormalizedGlobalOf(
				toSGVec(local_input), 
				tempVec
				);
		toExampleVector(
				tempVec,
				global_output
			);		
	}
	public void setToOrthoNormalizedGlobalOf(ExampleRay local_input, ExampleRay global_output){
		sgRay tempRay = new sgRay(); 
		super.setToOrthoNormalizedGlobalOf(
				toSgRay(local_input), 
				tempRay
				);
		toExampleRay(
				tempRay,
				global_output
			);		
	}
	public void setToRawGlobalOf(ExampleRay local_input, ExampleRay global_output){
		sgRay tempRay = new sgRay(); 
		super.setToRawGlobalOf(
				toSgRay(local_input), 
				tempRay
				);
		toExampleRay(
				tempRay,
				global_output
			);				
	}
	public void setToGlobalOf(ExampleRay local_input, ExampleRay global_output){
		sgRay tempRay = new sgRay(); 
		super.setToGlobalOf(
				toSgRay(local_input), 
				tempRay
				);
		toExampleRay(
				tempRay,
				global_output
			);	
		
	}
	public ExampleRay getGlobalOf(ExampleRay local_input){ 
		return toExampleRay(
				super.getGlobalOf(
						toSgRay(local_input)
					)
			);
	}
	
	public ExampleRay getLocalOf(ExampleRay global_input){ 
		return toExampleRay(
				super.getLocalOf(
						toSgRay(global_input)
					)
			);
	}	
	
	public ExampleRay getRawGlobalOf(ExampleRay local_input){ 
		return toExampleRay(
				super.getRawGlobalOf(
						toSgRay(local_input)
					)
			);
	}
	public ExampleRay getRawLocalOf(ExampleRay global_input){ 
		return toExampleRay(
				super.getRawLocalOf(
						toSgRay(global_input)
					)
			);
	}
	public ExampleRay getOrthoNormalizedLocalOf(ExampleRay global_input){ 
		return toExampleRay(
				super.getOrthoNormalizedLocalOf(
						toSgRay(global_input)
					)
			);
		}
	public ExampleVector getRawLocalOf(ExampleVector global_input){
		return toExampleVector(getRawLocalOf(
				super.getOrthoNormalizedGlobalOf(
						toSGVec(global_input)
						)
				)
			);		
	}
	
	public ExampleVector getRawGlobalOf(ExampleVector local_input){
		return toExampleVector(getRawLocalOf(
				super.getRawGlobalOf(
						toSGVec(local_input)
						)
				)
			);		
	}
	public void translateByGlobal(ExampleVector translate){
		super.translateByGlobal(
				toSGVec(translate)
			);
	}
	public void translateTo(ExampleVector translate, boolean slip){
		super.translateTo(
				toSGVec(translate),
				false
			);
		
	}
	public void translateTo(ExampleVector translate){
		super.translateTo(
				toSGVec(translate)
			);		
	}
	public ExampleVector getLocalOf(ExampleVector global_input){
		return toExampleVector(
				super.getLocalOf(
						toSGVec(global_input)
						)
			);
	}
	public ExampleVector setToLocalOf(ExampleVector  global_input){	
		toExampleVector(
				super.setToLocalOf(
						toSGVec(global_input)
						),				
					global_input
			);
		return global_input;
	}
	public void setToLocalOf(ExampleVector global_input, ExampleVector local_output){
		SGVec_3d tempVec = new SGVec_3d(); 
		super.setToLocalOf(
				toSGVec(global_input), 
				tempVec
				);
		toExampleVector(
				tempVec,
				local_output
			);				
	}
	public void setToLocalOf(ExampleRay global_input, ExampleRay local_output){
		sgRay tempRay = new sgRay(); 
		super.setToLocalOf(
				toSgRay(global_input), 
				tempRay
				);
			toExampleRay(
				tempRay,
				local_output
			);		
	}
	
	public void setToRawLocalOf(ExampleRay global_input, ExampleRay local_output){
		sgRay tempRay = new sgRay(); 
		super.setToRawLocalOf(
				toSgRay(global_input), 
				tempRay
				);
		toExampleRay(
				tempRay,
				local_output
			);	
	}
	
	public void setToOrthonormalLocalOf(ExampleRay global_input, ExampleRay local_output){
		sgRay tempRay = new sgRay(); 
		super.setToOrthonormalLocalOf(
				toSgRay(global_input), 
				tempRay
				);
			toExampleRay(
				tempRay,
				local_output
			);	
	}	
	
	
	public ExampleVector   getOrthoNormalizedLocalOf(ExampleVector global_input){
		return toExampleVector(
				super.getOrthoNormalizedLocalOf(
						toSGVec(global_input)
						)
			);
	}
	
	
	public ExampleVector  getOrientationalLocalOf(ExampleVector input_global){
		return toExampleVector(
				super.getOrientationalLocalOf(
						toSGVec(input_global)
						)
			);
		
	}
	public ExampleVector getOrientationalGlobalOf(ExampleVector input_local){
		return toExampleVector(
				super.getOrientationalGlobalOf(
						toSGVec(input_local)
						)
			);
		
	}
	
	public void setToRawLocalOf(ExampleVector in, ExampleVector out){
		SGVec_3d tempVec = new SGVec_3d(); 
		super.setToRawLocalOf(
				toSGVec(in), 
				tempVec
				);
		toExampleVector(
				tempVec,
				out
			);
	}
	
		
	public void setToOrthoNormalLocalOf(ExampleVector input_global, ExampleVector output_local_normalized){
		SGVec_3d tempVec = new SGVec_3d(); 
		super.setToOrthoNormalLocalOf(
				toSGVec(input_global), 
				tempVec
				);
		toExampleVector(
				tempVec,
				output_local_normalized
			);
		
	}
	public void setToOrientationalLocalOf(ExampleVector input_global, ExampleVector output_local_orthonormal_chiral){
		SGVec_3d tempVec = new SGVec_3d(); 
		super.setToOrientationalLocalOf(
				toSGVec(input_global), 
				tempVec
				);
		toExampleVector(
				tempVec,
				output_local_orthonormal_chiral
			);
		
	}
	public void setToOrientationalGlobalOf(ExampleVector input_local, ExampleVector output_global_orthonormal_chiral){
		SGVec_3d tempVec = new SGVec_3d(); 
		super.setToOrientationalGlobalOf(
				toSGVec(input_local), 
				tempVec
				);
		toExampleVector(
				tempVec,
				output_global_orthonormal_chiral
			);
		
	}
	
	
	

	////////////////////////?End of wrapper functions 

}
