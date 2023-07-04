package prefetcher

import chisel3._
import chisel3.util._

class MarkovNode extends Bundle {
  val transitions = Vec(16, new Transition) // 16 possible transitions 其中存储的是跳转地址
  var total_transitions = UInt(32.W)
  var index = UInt(4.W)         // 用于记录当前节点的索引 
}


class Transition extends Bundle {
  var address = UInt(32.W)
  var count = UInt(32.W)
}
