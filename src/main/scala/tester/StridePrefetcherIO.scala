package tester
import chisel3._

// Define table entry structure
class TableEntry extends Bundle {
    val pc = UInt(32.W)
    val prev_address = UInt(32.W)
    val prev_stride = SInt(32.W)
  }