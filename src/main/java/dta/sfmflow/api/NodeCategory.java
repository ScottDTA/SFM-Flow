package dta.sfmflow.api;

/**
 * Public API enumeration defining the logical categories of flowchart workspace
 * nodes [3]. Used on both client and server to filter menus, configure
 * sub-panels, and route execution paths.
 */
public enum NodeCategory {
	/**
	 * Node types that initiate a flowchart logic path, usually listening for
	 * specific triggers or events [3].
	 */
	TRIGGER,

	/**
	 * Node types that pull or query data/items from external blocks or systems [3].
	 */
	INPUT,

	/**
	 * Node types that write, push, or deposit data/items into external blocks or
	 * systems [3].
	 */
	OUTPUT,

	/**
	 * Node types that perform logical evaluations, comparisons, or control flow
	 * branches [3].
	 */
	LOGIC,

	/**
	 * Node types managing variables, counts, registry paths, or local storage
	 * buffers [3].
	 */
	VARIABLE,

	/**
	 * Node types handling utility tasks like camouflage, custom signage, or bulk
	 * management [3].
	 */
	UTILITY
}