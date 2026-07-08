package dta.sfmflow.api.capability;

/**
 * Public API callback interface to create a thread-safe, immutable deep copy of an active capability [3].
 * Used by standard capabilities and third-party integrations to support off-thread planning [3].
 */
@FunctionalInterface
public interface ICapabilitySnapshotter<T> {
	/**
	 * Creates an immutable snapshot from a live capability handler on the main server thread [3].
	 *
	 * @param handler the live capability handler instance [3]
	 * @return the immutable snapshot object [3]
	 */
	Object createSnapshot(T handler);
}