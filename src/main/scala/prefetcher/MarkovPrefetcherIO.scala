package prefetcher

import chisel3._
import chisel3.util._

class MarkovNode extends Bundle {
  // val address = (0.U(32.W))
  val transitions = (Vec(16, new transition)) // 16 possible transitions 其中存储的是跳转地址
  val total_transitions = (UInt(32.W))
  val index = UInt(4.W)              // 用于记录当前节点的索引 
}


class transition extends Bundle {
  val address = (UInt(32.W))
  val count = (UInt(32.W))
}

// val existingIndex = WireDefault(0.U)
// val addressExist = WireDefault(false.B)

// // 判断 address 是否存在于 transitions 中
// for (i <- 0 until markovTable(prev_address).transitions.length) {
//   when(markovTable(prev_address).transitions(i).address === io.address) {
//     existingIndex := i.U
//     addressExist := true.B
//   }
// }
