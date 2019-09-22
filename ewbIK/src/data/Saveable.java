package data;

public interface Saveable {
	public JSONObject getSaveJSON(SaveManager saveManager);
	public void loadFromJSONObject(JSONObject j, LoadManager l);
	public void notifyOfSaveIntent(SaveManager saveManager); 
	public void notifyOfSaveCompletion(SaveManager saveManager);
	public boolean isLoading();
	public void setLoading(boolean loading);
	/**
	 * called on all loaded objects once the full load sequence has been completed
	 */
	default void notifyOfLoadCompletion() {}
	public void makeSaveable(SaveManager saveManager);
	
	default String getIdentityHash() {
		String result = "";
		result += System.identityHashCode(this);
		String className = this.getClass().getSimpleName(); 
		result+="-"+className;
		return result; 
	}
}
