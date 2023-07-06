package prefetcher

import chisel3._
import chisel3.util._


class MarkovPrefetcher extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(32.W))
    val prefetchAddress = Output(UInt(32.W))
    val prefetchHit = Output(Bool())
  })

  // Create a synchronous read memory to store the linked list for recording memory access pattern
  val markovTable = SyncReadMem(16, new MarkovNode)

  val prev_address = RegInit(0.U(32.W))
  when(prev_address =/= 0.U){
    // Update the entry in the linked list (add transition)
    markovTable(prev_address).transitions(io.address) := markovTable(prev_address).transitions(io.address) + 1.U
    markovTable(prev_address).total_transitions := markovTable(prev_address).total_transitions + 1.U
  }

  when(markovTable(io.address).total_transitions > 1.U) {
    // Find the most frequent transition
    val maxAddress = markovTable(io.address).transitions.indexWhere((x: UInt) => x === markovTable(io.address).transitions.reduceLeft((x: UInt, y: UInt) => x.max(y)))

    io.prefetchAddress  := maxAddress
    io.prefetchHit := true.B
  }.otherwise {
    io.prefetchHit := false.B
    io.prefetchAddress  := 0.U
  }

  prev_address := io.address
}


