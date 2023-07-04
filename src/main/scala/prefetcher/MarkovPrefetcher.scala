package prefetcher
import chisel3._
import chisel3.util._

class MarkovPrefetcher extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(32.W))
    val prefetch_address = Output(UInt(32.W))
    val prefetch_hit = Output(Bool())
  })

  // def add_markovNode(node:MarkovNode): Unit = {
  //   node.index := 0.U(4.W)
  //   node.transitions.zipWithIndex.foreach { case (transition, i) =>
  //     transition.address := 0.U(32.W)
  //     transition.count := 0.U(32.W)
  //   }
  //   node.total_transitions := 0.U(32.W)
  // }

  def add_new_transition(address: UInt): Transition = {
    val newTransition = Wire(new Transition)
    newTransition.address := address
    newTransition.count := 1.U(32.W)
    newTransition
  }

  // def update_markovTable(prev_node:MarkovNode,existingIndex:UInt,transitions_addressExist:Bool,address:UInt): MarkovNode = {
  //   when(!transitions_addressExist) { // 若当前地址不存在于前一个节点的链表中，则将当前地址添加到链表中
  //     prev_node.transitions(prev_node.index) := add_new_transition(address) // 将新的节点添加到链表中
  //     // 更新转移表中的值
  //     prev_node.index := Mux(prev_node.index === 15.U(4.W), 0.U(4.W), (prev_node.index + 1.U(4.W)))
  //     prev_node.total_transitions := prev_node.total_transitions + 1.U(32.W)
  //   }.otherwise{ // 若当前地址在链表中的转移表已经包含address，则将当前地址的转移表记录跳转次数加一
  //     prev_node.transitions(existingIndex).count := prev_node.transitions(existingIndex).count + 1.U(32.W)
  //     prev_node.total_transitions := prev_node.total_transitions + 1.U(32.W)
  //   }
  //   prev_node
  // }


  // 创建存储链表用于记录内存的地址访问情况
  val markovTable = SyncReadMem(16, new MarkovNode)

  val prev_address = RegInit(0.U(32.W))
  val prev_index = RegInit(0.U(4.W))
  val index = RegInit(0.U(4.W)) // index用于记录实际内存的地址号

  // val now_node = RegInit(WireDefault(new MarkovNode))
  // val prev_node = RegInit(WireDefault(new MarkovNode))

  // 使用Vec来映射链表中的条目索引
  val address_index = RegInit(VecInit(Seq.fill(16)(0.U(32.W))))
  val nextIndex = Wire(UInt(4.W)) // 新增一个 Wire 来存储更新后的索引


  // 判断地址是否存在于链表中
  val addressExist = address_index.contains(io.address)
  val now_index_temp = MuxLookup(io.address, 0.U(4.W), (address_index.zipWithIndex.map { case (addr, index) =>
  (addr === io.address) -> index.U(4.W)
}))

  val now_index = Mux(addressExist, now_index_temp, index) // 若地址存在于链表中，则当前节点的索引为实际的地址号
  nextIndex := Mux(!addressExist && index === 15.U, 0.U, index + 1.U) // 更新索引

  val now_node = markovTable.read(now_index)

  when(!addressExist) { // 若地址不存在于链表中，则将地址添加到链表中
      address_index(index) := io.address

     now_node.index := 0.U(4.W)
    now_node.transitions.zipWithIndex.foreach { case (transition, i) =>
      transition.address := 0.U(32.W)
      transition.count := 0.U(32.W)
    }
    now_node.total_transitions := 0.U(32.W)


      io.prefetch_hit := false.B        //然后就不用进行预取。
      io.prefetch_address := 0.U(32.W)
  }.otherwise{    //如果存在直接进行预取
    val maxCountValue = Wire(UInt(32.W))
    val maxCountIndex = Wire(UInt(4.W))
    val maxAddress = Wire(UInt(32.W))

    val maxCount = now_node.transitions.map(_.count).reduce((a, b) => Mux(a > b, a, b))
    val maxIndex = PriorityEncoder(now_node.transitions.map(t => t.count === maxCount))

    maxCountValue := maxCount
    maxCountIndex := maxIndex
    maxAddress := now_node.transitions(maxIndex).address

    io.prefetch_address := maxAddress
    io.prefetch_hit := true.B
  }


  val prev_node = markovTable.read(prev_index)
  when(prev_address =/= 0.U){
    // 更新链表中的条目, add transition
    val existingIndex = WireDefault(0.U(4.W))
    val transitions_addressExist = WireDefault(false.B)
    // 判断 address 是否存在于 transitions 中
    for (i <- 0 until 16) {
      when(prev_node.transitions(i).address === io.address) {  //并且前一个地址不为零时
        existingIndex := i.U(4.W)
        transitions_addressExist := true.B
      }
    }
    //update_markovTable(prev_node,existingIndex,transitions_addressExist,io.address)
        when(!transitions_addressExist) { // 若当前地址不存在于前一个节点的链表中，则将当前地址添加到链表中
      prev_node.transitions(prev_node.index) := add_new_transition(io.address) // 将新的节点添加到链表中
      // 更新转移表中的值
      prev_node.index := Mux(prev_node.index === 15.U(4.W), 0.U(4.W), (prev_node.index + 1.U(4.W)))
      prev_node.total_transitions := prev_node.total_transitions + 1.U(32.W)
    }.otherwise{ // 若当前地址在链表中的转移表已经包含address，则将当前地址的转移表记录跳转次数加一
      prev_node.transitions(existingIndex).count := prev_node.transitions(existingIndex).count + 1.U(32.W)
      prev_node.total_transitions := prev_node.total_transitions + 1.U(32.W)
    }
  }


  markovTable.write(now_index, now_node)
  markovTable.write(prev_index, prev_node)

  prev_address := io.address
  prev_index := now_index
  index := nextIndex

}

