package data;

public interface Saveable {
	public JSONObject getSaveJSON();
	public void loadFromJSONObject(JSONObject j);
	public void notifyOfSaveIntent(); 
	public void notifyOfSaveCompletion();
	public boolean isLoading();
	public void setLoading(boolean loading);
	/**
	 * called on all loaded objects once the full load sequence has been completed
	 */
	default void notifyOfLoadCompletion() {}
	public void makeSaveable();
	
	default String getIdentityHash() {
		String result = "";
		result += System.identityHashCode(this);
		String className = this.getClass().getSimpleName(); 
		result+="-"+className;
		return result; 
	}
}
