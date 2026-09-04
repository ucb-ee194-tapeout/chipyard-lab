package packbitsacc

import chisel3._
import chisel3.util._
import chisel3.util._
import chisel3.{Printable}
import org.chipsalliance.cde.config._

class DMAReadInfo extends Bundle {
    val addr = UInt(64.W)
    val size = UInt(64.W)
}

class DMAWriteDstInfo extends Bundle {
    val addr = UInt(64.W)
}

class MemLoaderConsumerBundle extends Bundle {
    val data = UInt(256.W)
    val last = Bool()
}

class LoadInfoBundle extends Bundle {
  val start_byte = UInt(5.W)
  val end_byte = UInt(5.W)
}

class BufInfoBundle extends Bundle {
  val len_bytes = UInt(64.W)
}