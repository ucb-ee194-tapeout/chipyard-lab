package packbitsacc

import chisel3._
import chisel3.util._
import chisel3.{Printable}
import freechips.rocketchip.tile._
import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket.{TLBConfig}
import freechips.rocketchip.util.DecoupledHelper
import freechips.rocketchip.rocket.constants.MemoryOpConstants
import freechips.rocketchip.util.DontTouch


class PackBitsDecompressModule()(implicit p: Parameters) extends Module {

    val io = IO(new Bundle {
        val data_stream_in = Flipped(Decoupled(new MemLoaderConsumerBundle))
        val out = Decoupled(new MemLoaderConsumerBundle)
        val done = Output(Bool())
    })

    /* YOUR CODE HERE */
   
}