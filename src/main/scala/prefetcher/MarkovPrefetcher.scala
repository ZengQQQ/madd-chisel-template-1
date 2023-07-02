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

//使用Vec来映射链表中的条目索引
val address_index = RegInit(VecInit(Seq.fill(16)(0.U(32.W))))
val index = RegInit(0.U(4.W))      //index用于记录当前最新节点的索引

// val now_index := RegInit(0.U(4.W))  //now_index用于记录当前节点的索引,address的索引，不知道是否存在，需要计算
// val prev_index := address_index.indexWhere(_ === prev_address) //prev_index用于记录前一个节点的索引

val addressExist = Wire(Bool())
addressExist := address_index.contains(io.address)

//判断地址是否存在于链表中
when(!addressExist){   //若地址不存在于链表中，则将地址添加到链表中
    address_index(index) := io.address
    //更新链表中的条目
    markovTable(index).transitions.foreach { transition =>
      transition.address := 0.U
      transition.count := 0.U
    }
    markovTable(index).total_transitions := 0.U
    markovTable(index).index := 0.U
}

when(prev_address =/= 0.U){ //当前一个地址不为零时
    //更新链表中的条目 ,add transition
    val prev_index = address_index.indexWhere(_ === prev_address) //prev_index用于记录前一个节点在markovTable中的索引
    //val existingIndex = markovTable(prev_index).transitions.indexWhere(_.address === io.address)



    when(existingIndex === -1){ //若当前地址不存在于前一个节点的链表中，则将当前地址添加到链表中
        markovTable(prev_index).transitions(markovTable(prev_index).index).address := io.address
        markovTable(prev_index).transitions(markovTable(prev_index).index).count := 1.U
        //更新索引
        markovTable(prev_index).index := Mux(markovTable(prev_index).index === 15.U, 0.U, markovTable(prev_index).index + 1.U)

    }.otherwise{ //若当前地址存在于链表中，则将当前地址的跳转次数加一
        markovTable(prev_index).transitions(existingIndex).count := markovTable(prev_index).transitions(existingIndex).count + 1.U
    } 
    markovTable(prev_index).total_transitions := markovTable(prev_address).total_transitions + 1.U
}

val now_index = address_index.indexWhere(_ === io.address) //now_index用于记录当前节点的索引

when(markovTable(now_index).transitions.total_transitions > 1.U){ //若当前地址的跳转次数大于1，则进行预取
    //find the most frequent transition
    val max = regInit(0.U(32.W))
    max := 0.U
    val prefetch_address = RegInit(0.U(32.W))
    for(i <- 0 until markovTable(address).index-1){
        when(markovTable(now_index ).transitions(i).count > max){
            max := markovTable(now_index).transitions(i).count
            prefetch_address := markovTable(now_index).transitions(i).address
        }
    }
    when(max =/= 0.U){ //若预取地址不为零，则进行预取
    io.prefetch_address := prefetch_address
    io.prefetch_hit := true.B
    }.otherwise{
        io.prefetch_hit := false.B
        io.prefetch_address := 0.U
        }
}
//更新索引，使用FIFO替换策略
    index := Mux(index === 15.U, 0.U, index + 1.U)
    prev_address := io.address
        
}
