package packbitsacc

import chisel3._
import chisel3.util._
import chisel3.{Printable}
import freechips.rocketchip.tile._
import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket.{TLBConfig, HellaCacheArbiter}
import freechips.rocketchip.util.DecoupledHelper
import freechips.rocketchip.rocket.constants.MemoryOpConstants
import freechips.rocketchip.tilelink._

class PackBitsDecompressor(opcodes: OpcodeSet)(implicit p: Parameters) extends LazyRoCC(opcodes = opcodes, nPTWPorts = 2) {
    override lazy val module = new PackBitsDecompressorImpl(this)

    val l2_reader = LazyModule(new L2MemHelper("[packbits_reader]", numOutstandingReqs = 8))
    tlNode := TLWidthWidget(32) := l2_reader.masterNode

    val l2_writer = LazyModule(new L2MemHelper("[packbits_writer]", numOutstandingReqs = 8))
    tlNode := TLWidthWidget(32) := l2_writer.masterNode
}

class PackBitsDecompressorImpl(outer: PackBitsDecompressor)(implicit p: Parameters) extends LazyRoCCModuleImp(outer) with MemoryOpConstants {
    
    // accepts & parses incoming RoCC commands
    val cmd_router = Module(new PackBitsCommandRouter)
    cmd_router.io.rocc_in <> io.cmd
    // io.resp <> cmd_router.io.rocc_out // we have no RoCC Response as output for our accelerator

    io.mem.req.valid := false.B
    io.mem.s1_kill := false.B
    io.mem.s2_kill := false.B
    io.mem.keep_clock_enabled := true.B

    // DO NOT TOUCH ABOVE THIS LINE !!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    val memloader = Module(new PackBitsMemLoader)
    outer.l2_reader.module.io.userif <> memloader.io.l2helperUser
    memloader.io.src_info <> cmd_router.io.src_info

    val packbits_decomp_module = Module(new PackBitsDecompressModule)
    memloader.io.consumer_if <> packbits_decomp_module.io.data_stream_in
    // packbits_decomp_module.io.out.ready := false.B // TESTING: tie off for now

    val memwriter = Module(new PackBitsMemWriter)
    outer.l2_writer.module.io.userif <> memwriter.io.l2helperUser
    memwriter.io.dst_info <> cmd_router.io.dst_info
    packbits_decomp_module.io.out <> memwriter.io.consumer_if
    packbits_decomp_module.io.done <> memwriter.io.pack_bits_decompress_done


    // DO NOT TOUCH BELOW THIS LINE UNLESS YOU NEED MORE MEMORY
    // INTERFACES !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    // L2 I/F 0: boilerplate, do not touch
    outer.l2_reader.module.io.sfence <> cmd_router.io.sfence_out
    outer.l2_reader.module.io.status.valid := cmd_router.io.dmem_status_out.valid
    outer.l2_reader.module.io.status.bits := cmd_router.io.dmem_status_out.bits.status
    io.ptw(0) <> outer.l2_reader.module.io.ptw

    // L2 I/F 1: boilerplate, do not touch
    outer.l2_writer.module.io.sfence <> cmd_router.io.sfence_out
    outer.l2_writer.module.io.status.valid := cmd_router.io.dmem_status_out.valid
    outer.l2_writer.module.io.status.bits := cmd_router.io.dmem_status_out.bits.status
    io.ptw(1) <> outer.l2_writer.module.io.ptw

    io.busy := false.B
}