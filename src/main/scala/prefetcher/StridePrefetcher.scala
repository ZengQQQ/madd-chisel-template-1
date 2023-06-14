package prefetcher
import chisel3._
import chisel3.util._



// TODO: update this module to implement stride prefetching.
class StridePrefetcher(val pcWidth:Int ,val address_width:Int) extends Module {
  val io = IO(new Bundle {
    val pc = Input(UInt(pcWidth.W))
    val address = Input(UInt(address_width.W))
    val prefetch_address = Output(UInt(address_width.W))
    val prefetch_valid = Output(Bool())
  })

  val tableSize = 16
  val table = Mem(tableSize, new TableEntry(pcWidth,address_width))
  val tableIndex = RegInit(0.U(log2Ceil(tableSize).W))

  val stride = io.address.asSInt - table(tableIndex).prev_address.asSInt

  // Update table entry or create a new entry
  when(table(tableIndex).prev_stride === stride) {
    // Match found
    io.prefetch_address := io.address + stride.asUInt
    io.prefetch_valid := true.B

    table(tableIndex).prev_address := io.address
    table(tableIndex).prev_stride := stride
  }.otherwise {
    // No match found, create a new entry
    io.prefetch_address := table(tableIndex).prev_address
    io.prefetch_valid := false.B

    // val newEntry = Wire(new TableEntry(pcWidth,address_width))
    // newEntry.pc := io.pc
    // newEntry.prev_address := io.address
    // newEntry.prev_stride := stride    
    //table(tableIndex) := newEntry
    //tableIndex := Mux(tableIndex === (tableSize.asUInt - 1.U), 0.U, tableIndex + 1.U)
    table(tableIndex).prev_address := io.address
    table(tableIndex).prev_stride := stride

    
  }
}
