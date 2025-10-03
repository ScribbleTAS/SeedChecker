package fail.scribble.seedchecker.util;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Locks certain paths from being called during a modify file event.<br>
 * The lock lasts for 1000 ms and each path has it's own timer.
 * 
 * @author Scribble
 *
 */
public class PathLock {

	/**
	 * A list of locks
	 */
	private Map<Path, Long> locks = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Locks the path and captures the current time
	 * @param path The path to lock
	 */
	public void scheduleAndLock(Path path) {
		locks.put(path, System.currentTimeMillis());
	}

	/**
	 * Checks if the path is locked and should be skipped
	 * @param path The path to check for
	 * @return If the path is locked
	 */
	public boolean isLocked(Path path) {
		if (locks.containsKey(path)) {
			long startTime = locks.get(path);
			if (System.currentTimeMillis() - startTime > 1000) {
				locks.remove(path);
				return false;
			} else {
				return true;
			}

		}
		return false;
	}

	@Override
	public String toString() {
		return locks.toString();
	}
}