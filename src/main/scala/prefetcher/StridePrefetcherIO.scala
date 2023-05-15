package prefetcher

import chisel3._
import chisel3.util._

// TODO: update this module to implement stride prefetcher's IO.

class Entry(val pcWidth:Int,val addressWidth:Int) extends Bundle{
 
  val PC = UInt(pcWidth.W)
  val prev_address =UInt(addressWidth.W)
  val prev_stride = SInt(addressWidth.W)

  val prefetch_address = UInt(addressWidth.W)
}



class LookupTableIO(capacity: Int, pc_width: Int, address_width: Int) extends Bundle {
  val PC = Input(UInt(pc_width.W)) // 输入PC
  val address = Input(UInt(address_width.W)) // 输入地址
  //val entry = Output(new Entry(pc_width,address_width)) // 输出条目
  val entry = IO(Vec(capacity, new Entry(pc_width,address_width))) // 输出条目

  val valid = Output(Bool()) // 输出是否找到条目
}


// LookupTable 类定义
class LookupTable(capacity: Int, pc_width:Int,address_width:Int) extends Module {
//  val io = IO(new Bundle {
//  val PC = Input(UInt(pc_width.W)) // 输入PC
//  val address = Input(UInt(address_width.W)) // 输入地址
//  val entry = Output(new Entry(pc_width,address_width)) // 输出条目
//  val valid = Output(Bool()) // 输出是否找到条目
//  })
    val io = IO(new LookupTableIO(capacity, pc_width, address_width))


 // 使用一个Vec来存储条目，Vec是一个可变长度的向量类型
val entries = RegInit(VecInit((Seq.fill(capacity)(Wire((new Entry(pc_width,address_width)))))))

io.entry := entries

 // 查找给定PC的条目，使用MuxCase来实现多路选择器
 val found = Wire(Bool())
 found := false.B
 io.entry := 0.U.asTypeOf(new Entry(pc_width,address_width))
 io.valid := false.B
 for (i <- 0 until capacity) {
 when (entries(i).PC === io.PC) {
 found := true.B
 io.entry := entries(i)
 io.valid := true.B
 }
 }

 // 插入新条目，使用ShiftRegister来实现移位寄存器
 when (!found) {
 // 如果表已满，先删除最旧的条目
 entries := ShiftRegister(entries, capacity - 1)

 // 插入新条目
 val new_entry = Wire(new Entry(pc_width,address_width))
 new_entry.PC := io.PC
 new_entry.prev_address := io.address
 new_entry.prev_stride := 0.S
 entries(capacity - 1) := new_entry
 }


}
