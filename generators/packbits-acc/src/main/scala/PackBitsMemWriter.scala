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


class PackBitsMemWriter()(implicit p: Parameters) extends Module 
    with MemoryOpConstants {

    val io = IO(new Bundle{
        val l2helperUser = new L2MemHelperBundle

        val dst_info = Flipped(Decoupled(new DMAWriteDstInfo))

        val consumer_if = Flipped(Decoupled(new MemLoaderConsumerBundle))

        val pack_bits_decompress_done = Input(Bool())
    })

    val incoming_writes_Q = Module(new Queue(new MemLoaderConsumerBundle, 4))
    incoming_writes_Q.io.enq <> io.consumer_if

    val dst_info_Q = Module(new Queue(new DMAWriteDstInfo, 4))
    dst_info_Q.io.enq <> io.dst_info


    val compress_dest_last_fire = RegNext(dst_info_Q.io.deq.fire)
    val compress_dest_last_valid = RegNext(dst_info_Q.io.deq.valid)
    val compress_dest_printhelp = dst_info_Q.io.deq.valid && (compress_dest_last_fire || (!compress_dest_last_valid))

    when (compress_dest_printhelp) {
        PackBitsAccLogger.logInfo("[config-memwriter] got dest info addr: 0x%x\n",
        dst_info_Q.io.deq.bits.addr)
    }

    val len_already_consumed = RegInit(0.U(64.W))

    val mem_write_fire = DecoupledHelper(
        io.l2helperUser.req.ready,
        dst_info_Q.io.deq.valid,
        incoming_writes_Q.io.deq.valid
    )

    dst_info_Q.io.deq.ready := mem_write_fire.fire(dst_info_Q.io.deq.valid)
    incoming_writes_Q.io.deq.ready := mem_write_fire.fire(incoming_writes_Q.io.deq.valid)

    io.l2helperUser.req.bits.size := log2Ceil(32).U // bytes!
    io.l2helperUser.req.bits.addr := dst_info_Q.io.deq.bits.addr
    io.l2helperUser.req.bits.data := incoming_writes_Q.io.deq.bits.data
    io.l2helperUser.req.bits.cmd := M_XWR
    
    io.l2helperUser.req.valid := mem_write_fire.fire(io.l2helperUser.req.ready)    
    
    io.l2helperUser.resp.ready := true.B
}