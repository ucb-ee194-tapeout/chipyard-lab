package packbitsacc

import chisel3._
import chisel3.util._
import chisel3.util._
import chisel3.{Printable}
import freechips.rocketchip.tile._
import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket.{TLBConfig, HellaCacheArbiter}
import freechips.rocketchip.util.DecoupledHelper
import freechips.rocketchip.rocket.constants.MemoryOpConstants
import freechips.rocketchip.tilelink._

case object PackBitsAccPrintfEnable extends Field[Boolean](false)

class WithPackBitsDecompressor extends Config((site, here, up) => {
    case PackBitsAccPrintfEnable => false
    case PackBitsAccTLB => Some(TLBConfig(nSets = 4, nWays = 4, nSectors = 1, nSuperpageEntries = 1))
    case BuildRoCC => up(BuildRoCC) ++ Seq((p: Parameters) => {
        val packbitsacc = LazyModule.apply(new PackBitsDecompressor(OpcodeSet.custom0)(p))
        packbitsacc
    })
})