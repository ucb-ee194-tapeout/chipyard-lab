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

class PackBitsCommandRouter()(implicit p: Parameters) extends Module {

    val FUNCT_SFENCE = 0.U
    val FUNCT_SRC_INFO = 1.U
    val FUNCT_DST_INFO = 2.U

    val io = IO(new Bundle {
        val rocc_in = Flipped(Decoupled(new RoCCCommand))

        // val rocc_out = Decoupled(new RoCCResponse) // our accelerator has no rocc resp, typically this is used for polling the status of an instruction in flight in the accelerator

        val sfence_out = Output(Bool())
        val dmem_status_out = Valid(new RoCCCommand)

        val src_info = Decoupled(new DMAReadInfo)

        val dst_info = Decoupled(new DMAWriteDstInfo)

    })

    val track_dispatched_src_infos = RegInit(0.U(64.W))
    when (io.rocc_in.fire) {
        when (io.rocc_in.bits.inst.funct === FUNCT_SRC_INFO) {
        val next_track_dispatched_src_infos = track_dispatched_src_infos + 1.U
        track_dispatched_src_infos := next_track_dispatched_src_infos
        PackBitsAccLogger.logInfo("dispatched src info commands: current 0x%x, next 0x%x\n",
            track_dispatched_src_infos,
            next_track_dispatched_src_infos)
        }
    }

    when (io.rocc_in.fire) {
        PackBitsAccLogger.logInfo("gotcmd funct %x, rd %x, rs1val %x, rs2val %x\n", 
        io.rocc_in.bits.inst.funct,
        io.rocc_in.bits.inst.rd,
        io.rocc_in.bits.rs1,
        io.rocc_in.bits.rs2)
    }

    io.dmem_status_out.bits <> io.rocc_in.bits
    io.dmem_status_out.valid := io.rocc_in.fire

    val current_funct = io.rocc_in.bits.inst.funct

    val sfence_fire = DecoupledHelper(
        io.rocc_in.valid,
        current_funct === FUNCT_SFENCE
    )
    io.sfence_out := sfence_fire.fire


    val src_info_queue = Module(new Queue(new DMAReadInfo, 4))
    io.src_info <> src_info_queue.io.deq

    val src_info_fire = DecoupledHelper(
        // YOUR CODE HERE
        // YOUR CODE HERE
        // YOUR CODE HERE - Hint: How do we differentiate between different RoCC instructions? - See lines 16-18!
    )

    src_info_queue.io.enq.bits.addr := /* YOUR CODE HERE - Which RoCC field gives the address to read from?*/
    src_info_queue.io.enq.bits.size := /* YOUR CODE HERE - Which RoCC field gives the size of the data to read?*/
    src_info_queue.io.enq.valid := src_info_fire.fire(src_info_queue.io.enq.ready)


    val dst_info_queue = Module(new Queue(new DMAWriteDstInfo, 4))
    io.dst_info <> dst_info_queue.io.deq

    val dst_info_fire = DecoupledHelper(
        // YOUR CODE HERE - Should be the same as line 69
        // YOUR CODE HERE - Should be almost the same as line 70
        // YOUR CODE HERE - Should be almost the same as line 71
    )
    dst_info_queue.io.enq.bits.addr := /* YOUR CODE HERE - Which RoCC field gives the address to write to?*/
    dst_info_queue.io.enq.valid := dst_info_fire.fire(dst_info_queue.io.enq.ready)


    io.rocc_in.ready := sfence_fire.fire(io.rocc_in.valid) || src_info_fire.fire(io.rocc_in.valid) || dst_info_fire.fire(io.rocc_in.valid)
}