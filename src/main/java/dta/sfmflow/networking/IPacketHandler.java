package dta.sfmflow.networking;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Common side-safe interface defining packet payload processing callbacks [3].
 * Separates concrete client implementations from common code [3].
 *
 * @param <T> the packet payload type [3]
 */
public interface IPacketHandler<T> {
	/**
	 * Executes the handler callback on the designated payload [3].
	 *
	 * @param payload the received packet payload [3]
	 * @param context the packet execution context [3]
	 */
	void handle(T payload, IPayloadContext context);
}