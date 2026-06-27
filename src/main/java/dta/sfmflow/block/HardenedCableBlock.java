package dta.sfmflow.block;

/**
 * Blast-resistant network cable block that participates in BFS scans [3].
 */
public class HardenedCableBlock extends CableBlock {

    /**
     * Initializes a HardenedCableBlock instance [3].
     *
     * @param properties block behavior properties [3]
     */
    public HardenedCableBlock(Properties properties) {
        super(properties);
    }
}