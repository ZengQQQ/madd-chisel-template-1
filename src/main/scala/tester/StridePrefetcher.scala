package tester
import chisel3._
import chisel3.util._

class StridePrefetcher extends Module {
  val io = IO(new Bundle {
    val pc = Input(UInt(32.W))
    val address = Input(UInt(32.W))
    val prefetch_address = Output(UInt(32.W))
    val prefetch_valid = Output(Bool())
  })

  // Define table entry structure
  class TableEntry extends Bundle {
    val pc = UInt(32.W)
    val prev_address = UInt(32.W)
    val prev_stride = SInt(32.W)
  }

  val tableSize = 16
  val table = Mem(tableSize, new TableEntry())
  val tableIndex = RegInit(0.U(log2Ceil(tableSize).W))

  // Initialize table entries
  for (i <- 0 until tableSize) {
    table(i).pc := 0.U
    table(i).prev_address := 0.U
    table(i).prev_stride := 0.S
  }

  val stride = io.address.asSInt - table(tableIndex).prev_address.asSInt

  // Find a match in the table
  val matchIndex = WireDefault(0.U)
  for (i <- 0 until tableSize) {
    when(table(i).pc === io.pc) {
      matchIndex := i.U
    }
  }

 when(matchIndex =/= 0.U) {
  // Match found
  when(stride === table(matchIndex).prev_stride) {
    io.prefetch_address := (io.address.asSInt + stride).asUInt
    io.prefetch_valid := true.B
    // Update the previous stride and address
    table(matchIndex).prev_stride := stride
    table(matchIndex).prev_address := io.address
  }.otherwise {
    io.prefetch_address := 0.U
    io.prefetch_valid := false.B
  }
}.otherwise {
  // No match found, create a new entry
  io.prefetch_address := 0.U
  io.prefetch_valid := false.B

  val newEntry = Wire(new TableEntry())
  newEntry.pc := io.pc
  newEntry.prev_address := io.address
  newEntry.prev_stride := stride

  table(tableIndex) := newEntry

  // Update table index using FIFO replacement strategy
  tableIndex := Mux(tableIndex === (tableSize - 1).U, 0.U, tableIndex + 1.U)
}
}