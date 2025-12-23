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

class PackBitsMemLoader()(implicit p: Parameters) extends Module 
    with MemoryOpConstants {

    val io = IO(new Bundle {
        val l2helperUser = new L2MemHelperBundle

        val src_info = Flipped(Decoupled(new DMAReadInfo))

        val consumer_if = Decoupled(new MemLoaderConsumerBundle)
    })

    val load_info_queue = Module(new Queue(new LoadInfoBundle, 256))

    val buf_info_queue = Module(new Queue(new BufInfoBundle, 16))

    val base_addr_bytes = io.src_info.bits.addr
    val base_len = io.src_info.bits.size
    val base_addr_start_index = io.src_info.bits.addr & 0x1F.U
    val aligned_loadlen =  base_len + base_addr_start_index
    val base_addr_end_index = (base_len + base_addr_start_index) & 0x1F.U
    val base_addr_end_index_inclusive = (base_len + base_addr_start_index - 1.U) & 0x1F.U
    val extra_word = ((aligned_loadlen & 0x1F.U) =/= 0.U).asUInt

    val base_addr_bytes_aligned = (base_addr_bytes >> 5.U) << 5.U
    val words_to_load = (aligned_loadlen >> 5.U) + extra_word
    val words_to_load_minus_one = words_to_load - 1.U


    val print_not_done = RegInit(true.B)

    when (io.src_info.valid && print_not_done) {
        PackBitsAccLogger.logInfo("base_addr_bytes: %x\n", base_addr_bytes)
        PackBitsAccLogger.logInfo("base_len: %x\n", base_len)
        PackBitsAccLogger.logInfo("base_addr_start_index: %x\n", base_addr_start_index)
        PackBitsAccLogger.logInfo("aligned_loadlen: %x\n", aligned_loadlen)
        PackBitsAccLogger.logInfo("base_addr_end_index: %x\n", base_addr_end_index)
        PackBitsAccLogger.logInfo("base_addr_end_index_inclusive: %x\n", base_addr_end_index_inclusive)
        PackBitsAccLogger.logInfo("extra_word: %x\n", extra_word)
        PackBitsAccLogger.logInfo("base_addr_bytes_aligned: %x\n", base_addr_bytes_aligned)
        PackBitsAccLogger.logInfo("words_to_load: %x\n", words_to_load)
        PackBitsAccLogger.logInfo("words_to_load_minus_one: %x\n", words_to_load_minus_one)
        print_not_done := false.B
    }

    when (io.src_info.fire) {
        print_not_done := true.B
        PackBitsAccLogger.logInfo("COMPLETED INPUT LOAD FOR DECOMPRESSION\n")
    }

    val addrinc = RegInit(0.U(64.W))
    load_info_queue.io.enq.bits.start_byte := Mux(addrinc === 0.U, base_addr_start_index, 0.U)
    load_info_queue.io.enq.bits.end_byte := Mux(addrinc === words_to_load_minus_one, base_addr_end_index_inclusive, 31.U)


    val request_fire = DecoupledHelper(
        io.l2helperUser.req.ready,
        io.src_info.valid,
        buf_info_queue.io.enq.ready,
        load_info_queue.io.enq.ready
    )

    io.l2helperUser.req.bits.cmd := M_XRD
    io.l2helperUser.req.bits.size := log2Ceil(32).U
    io.l2helperUser.req.bits.data := 0.U
    io.l2helperUser.req.bits.addr := (base_addr_bytes_aligned) + (addrinc << 5)
    io.l2helperUser.req.valid := request_fire.fire(io.l2helperUser.req.ready)


    load_info_queue.io.enq.valid := request_fire.fire(load_info_queue.io.enq.ready)

    when (request_fire.fire && (addrinc === words_to_load_minus_one)) {
        addrinc := 0.U
    } .elsewhen (request_fire.fire) {
        addrinc := addrinc + 1.U
    }

    io.src_info.ready := request_fire.fire(io.src_info.valid, addrinc === words_to_load_minus_one)

    buf_info_queue.io.enq.valid := request_fire.fire(buf_info_queue.io.enq.ready, addrinc === 0.U)


    buf_info_queue.io.enq.bits.len_bytes := base_len

    val mem_resp_queue = Module(new Queue(new MemLoaderConsumerBundle, 16))
    io.consumer_if <> mem_resp_queue.io.deq

    // response
    val len_already_consumed = RegInit(0.U(64.W))

    val resp_fire = DecoupledHelper(
        io.l2helperUser.resp.valid,
        load_info_queue.io.deq.valid,
        mem_resp_queue.io.enq.ready,
        buf_info_queue.io.deq.valid
    )

    load_info_queue.io.deq.ready := resp_fire.fire(load_info_queue.io.deq.valid)

    buf_info_queue.io.deq.ready := resp_fire.fire(buf_info_queue.io.deq.valid) && (len_already_consumed === buf_info_queue.io.deq.bits.len_bytes)

    val align_shamt = (load_info_queue.io.deq.bits.start_byte << 3)
    val memresp_bits_shifted = io.l2helperUser.resp.bits.data >> align_shamt

    io.l2helperUser.resp.ready := resp_fire.fire(io.l2helperUser.resp.valid)

    mem_resp_queue.io.enq.bits.data := memresp_bits_shifted
    mem_resp_queue.io.enq.bits.last := (len_already_consumed === buf_info_queue.io.deq.bits.len_bytes)
    mem_resp_queue.io.enq.valid := resp_fire.fire(mem_resp_queue.io.enq.ready)

    when (resp_fire.fire) {
        when (len_already_consumed === buf_info_queue.io.deq.bits.len_bytes) {
            len_already_consumed := 0.U
        } .otherwise {
            len_already_consumed := len_already_consumed + (256 / 8).U
        }
    }

}