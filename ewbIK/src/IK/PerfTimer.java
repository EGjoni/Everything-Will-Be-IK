package IK;

import java.util.Stack;

public final class PerfTimer {
	
	static Stack<Long> startTimes = new Stack<>();
	static long startTime = 0;
	
		public static void start() {
			startTimes.push(System.nanoTime());
		}
	
	public static void printTimeSinceStart(String prepend) {
		long newDelta = System.nanoTime() - startTimes.pop();
		System.out.println(prepend+": " + ( newDelta));
	}

}
