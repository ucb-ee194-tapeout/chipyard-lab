package chipyard

import org.chipsalliance.cde.config.{Config}
import freechips.rocketchip.prci.{AsynchronousCrossing}
import freechips.rocketchip.subsystem.{InCluster}

class PackBitsConfig extends Config(
    new packbitsacc.WithPackBitsDecompressor ++
    new chipyard.config.WithExtMemIdBits(8) ++
    new chipyard.config.WithSystemBusWidth(256) ++
    new freechips.rocketchip.subsystem.WithNBanks(8) ++
    new freechips.rocketchip.subsystem.WithInclusiveCache(nWays = 16, capacityKB = 2048) ++
    new freechips.rocketchip.subsystem.WithNMemoryChannels(4) ++
    new freechips.rocketchip.rocket.WithNHugeCores(1) ++
    new chipyard.config.AbstractConfig
)