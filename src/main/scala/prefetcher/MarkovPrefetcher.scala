package prefetcher

import chisel3._
import chisel3.util._

class MarkovPrefetcher extends Module{
    val io=IO(new Bundle{
        val address=Input(UInt(32.W))
        val prefetch_address=Output(UInt(32.W))
        val prefetch_hit=Output(Bool())
    })
//创建一个存储链表用于记录内存的地址访问情况
val markovTable = Mem(16, new MarkovNode)
val prev_address = RegInit(0.U(32.W))
val prev_index = RegInit(0.U(4.W))

//使用Vec来映射链表中的条目索引
val address_index = VecInit(Seq.fill(16)(0.U(32.W)))
val index = RegInit(0.U(4.W))      //index用于记录实际内存的地址 号

val nextIndex = Wire(UInt(4.W)) // 新增一个 Wire 来存储更新后的索引

//判断地址是否存在于链表中
val addressExist = WireDefault(false.B)
val now_index = Wire(UInt(4.W))    //now_index用于记录当前节点的索引
  for (i <- 0 until 16) {
    when(address_index(i) === io.address) {
      addressExist := true.B
      now_index := i.U
    }
  }

when(!addressExist){   //若地址不存在于链表中，则将地址添加到链表中
  address_index(index) := io.address
  //更新链表中的条目
    markovTable(index).index := 0.U
    markovTable(index).transitions.zipWithIndex.foreach { case (transition, i) =>
        transition.address := 0.U
        transition.count := 0.U
    }
    markovTable(index).total_transitions := 0.U
    // markovTable.write(index, tempRecord)
  now_index := index  //添加进入之后，当前节点的索引为实际的地址号
}

nextIndex := Mux(!addressExist && index === 15.U, 0.U, index + 1.U) // 更新索引


val newIndex = Mux(markovTable(prev_index).index === 15.U, 0.U, markovTable(prev_index).index + 1.U)
val newTotalTransitions = markovTable(prev_index).total_transitions + 1.U 
//更新链表中的条目 ,add transition
val existingIndex = WireDefault(0.U)
val transitions_addressExist = WireDefault(false.B)
// 判断 address 是否存在于 transitions 中
for (i <- 0 until 16) {
when(markovTable(prev_index).transitions(i).address === io.address) {
    existingIndex := i.U
    transitions_addressExist := true.B
    }
}
when(prev_address =/= 0.U){ //当前一个地址不为零时
    when(!transitions_addressExist){ //若当前地址不存在于前一个节点的链表中，则将当前地址添加到链表中

        markovTable(prev_index).transitions(markovTable(prev_index).index).address := io.address //将新的节点添加到链表中
        markovTable(prev_index).transitions(markovTable(prev_index).index).count := 1.U
        //更新转移表中的值
        markovTable(prev_index).index := newIndex
        markovTable(prev_index).total_transitions := markovTable(prev_index).total_transitions + 1.U

    }.otherwise{ //若当前地址在链表中的转移表已经包含address，则将当前地址的转移表记录跳转次数加一

        markovTable(prev_index).transitions(existingIndex).count := markovTable(prev_index).transitions(existingIndex).count + 1.U
        markovTable(prev_index).total_transitions := markovTable(prev_index).total_transitions + 1.U
    }  
}

when(markovTable(now_index).total_transitions > 1.U){ //若当前地址的跳转次数大于1，则进行预取
    //find the most frequent transition
    val max = markovTable(now_index).transitions(0).count
    val prefetch_address = markovTable(now_index).transitions(0).address

    for(i <- 1 until 16){ //无法确认每个地址在链表中的位置，所以需要遍历整个链表
        when(markovTable(now_index).transitions(i).count > max){
            max := markovTable(now_index).transitions(i).count
            prefetch_address := markovTable(now_index).transitions(i).address
        }
    }
    io.prefetch_address := prefetch_address
    io.prefetch_hit := true.B
}.otherwise{
        io.prefetch_hit := false.B
        io.prefetch_address := 0.U
    }


    prev_address := io.address
    prev_index := now_index
    index := nextIndex
}
