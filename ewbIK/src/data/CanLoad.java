package data;
import data.JSONObject;

/**
 * this interface defines objects which can self populate from JSONObjects but don't 
 * need to register themselves with the savestate tracker. 
 * @author rufsketch1
 */
public interface CanLoad {
	
	/**
	 * 
	 * @param j
	 * @return should return an instance of itself for chaining
	 */
	public CanLoad populateSelfFromJSON(JSONObject j);
	public JSONObject toJSONObject();

}