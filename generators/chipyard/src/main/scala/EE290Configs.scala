package chipyard

import freechips.rocketchip.diplomacy.AddressSet
import saturn.common._
import atlas.config._
import freechips.rocketchip.resources.BigIntHexContext
import freechips.rocketchip.subsystem._
import freechips.rocketchip.diplomacy.RegionType
import org.chipsalliance.cde.config.Config
import testchipip.soc.{OBUS, WithMbusScratchpad}


class EE290SimConfig extends Config(
  new WithAtlasTile() ++
  new tacit.WithTraceSinkDMA(1) ++
  new tacit.WithTraceSinkAlways(0) ++
  new chipyard.config.WithTraceArbiterMonitor ++
  new chipyard.WithTacitEncoder ++
  new saturn.shuttle.WithShuttleVectorUnit(256, 128, VectorParams.mxParams) ++
  new chipyard.config.WithSystemBusWidth(256) ++
  new shuttle.common.WithShuttleTileBeatBytes(16) ++
  new shuttle.common.WithNShuttleCores(1) ++
  new chipyard.config.WithBroadcastManager ++
  new freechips.rocketchip.subsystem.WithCacheBlockBytes(64) ++
  new EE290BaseConfig
)

class EE290BaseConfig extends Config(
  new chipyard.config.WithSystemBusWidth(bitWidth = 256) ++
  new freechips.rocketchip.subsystem.WithExtMemSize(x"10_0000_0000") ++
  new freechips.rocketchip.subsystem.WithCacheBlockBytes(32) ++
  new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++
  new freechips.rocketchip.subsystem.WithEdgeDataBits(64) ++

  new chipyard.config.WithPeripheryBusFrequency(500.0) ++
  new chipyard.config.WithMemoryBusFrequency(500.0) ++
  new chipyard.config.WithControlBusFrequency(500.0) ++
  new chipyard.config.WithSystemBusFrequency(500.0) ++
  new chipyard.config.WithFrontBusFrequency(500.0) ++
  new chipyard.config.WithOffchipBusFrequency(500.0) ++
  new chipyard.harness.WithHarnessBinderClockFreqMHz(500.0) ++
  new testchipip.boot.WithNoCustomBootPin ++
  new chipyard.config.AbstractConfig)
