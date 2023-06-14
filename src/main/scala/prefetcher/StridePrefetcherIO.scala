package prefetcher
import chisel3._
import chisel3.util._



// Define table entry structure
class TableEntry(val pcWidth:Int ,val address_width:Int) extends Bundle {
    val pc = UInt(pcWidth.W)
    val prev_address = UInt(address_width.W)
    val prev_stride = SInt(address_width.W)
  }
