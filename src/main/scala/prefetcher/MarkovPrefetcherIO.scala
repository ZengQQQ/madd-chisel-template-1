package prefetcher

import chisel3._
import chisel3.util._

class MarkovNode extends Bundle {
  val transitions = Vec(16, UInt(32.W)) // 16 possible transitions
  val total_transitions = UInt(32.W)
}