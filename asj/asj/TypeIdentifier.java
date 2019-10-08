package asj;
public class TypeIdentifier {
	public Object key; 
	public Object value; 

	/**
	 * A hack to get around Java type erasure. The TypeIdentifier 
	 * will store two Objects. These should be some mixture of other classes 
	 * or type identifiers. And the logic of what to do with them in each case
	 * is left to whatever methods accept the TypeIdentifier (at the moment just hashmapFromJSON)   
	 */
	public TypeIdentifier(Object key, Object value) {
		this.key = key;
		this.value = value; 
	}
}
