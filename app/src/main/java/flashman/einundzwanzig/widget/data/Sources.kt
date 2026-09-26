package flashman.einundzwanzig.widget.data

/** Data sources in priority order - identical to the iOS widget v11. */
object Sources {
    val height = listOf(
        "https://mempool.space/api/blocks/tip/height",
        "https://blockstream.info/api/blocks/tip/height",
        "https://mempool.flashman.ch/api/blocks/tip/height",
    )
    val fees = listOf(
        "https://mempool.space/api/v1/fees/recommended",
        "https://blockstream.info/api/fee-estimates",
        "https://mempool.flashman.ch/api/v1/fees/recommended",
    )
    val price = listOf(
        "https://mempool.space/api/v1/prices",
        "https://blockchain.info/ticker",
        "https://mempool.flashman.ch/api/v1/prices",
    )
    val hashrate = listOf(
        "https://mempool.space/api/v1/mining/hashrate/1m",
        "https://mempool.flashman.ch/api/v1/mining/hashrate/1m",
    )
    val difficulty = listOf(
        "https://mempool.space/api/v1/difficulty-adjustment",
        "https://mempool.flashman.ch/api/v1/difficulty-adjustment",
    )
}
