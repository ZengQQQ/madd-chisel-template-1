package prev_prefetcher

import chisel3._
import chisel3.util._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}

// TODO: update this module to implement stride prefetching.
 class StridePrefetcher(pc_width: Int,address_width: Int) extends Module {

 val io = IO(new Bundle {
    // 输入端口
    val PC = Input(UInt(pc_width.W))
    val address = Input(UInt(address_width.W))
    // 输出端口
    val prefetch_address = Output(UInt(address_width.W))
    val prefetch_valid = Output(Bool())
  })
val capacity = 16
 // 实例化一个LookupTable模块，假设容量为16
 val table = Module(new LookupTable(capacity,pc_width,address_width))

 // 连接输入到LookupTable模块的输入端口
 table.io.PC := io.PC
 table.io.address := io.address

 // 处理当前访问的地址和程序计数器，计算步长，并根据步长是否与上一次相同来决定是否生成预取地址并预取数据

 // 定义一个寄存器来存储上一次访问的地址和步长，初始值为0
 val last_address = RegInit(0.U(address_width.W))
 val last_stride = RegInit(0.S(address_width.W))

 // 计算当前步长，注意要转换成有符号整数才能做减法运算
 val current_stride = (io.address.asSInt() - last_address.asSInt())


 // 定义一个信号来表示当前步长是否与上一次相同，使用===来比较
 val same_stride = Wire(Bool())
 same_stride := current_stride === last_stride

 // 定义一个信号来表示是否需要预取，使用&&来做逻辑与运算
 val need_prefetch = Wire(Bool())
 need_prefetch := table.io.valid && same_stride

 // 定义一个信号来表示预取地址，使用+来做加法运算
 val prefetch_address = Wire(UInt(address_width.W))
 prefetch_address := io.address + current_stride.asUInt()

  // 连接输出到StridePrefetcher模块的输出端口
 io.prefetch_address := prefetch_address
 io.prefetch_valid := need_prefetch

 // 更新寄存器的值，使用:=来做赋值操作
 last_address := io.address
 last_stride := current_stride

 // 定义一个寄存器来记录当前更新的条目的索引，初始值为0
 val index = RegInit(0.U(log2Ceil(capacity).W))

 // 更新LookupTable模块中的条目，使用when来做条件判断
 when (table.io.valid) {
 table.io.entry.in(index).prev_address := io.address
 table.io.entry.in(index).prev_stride := current_stride.asSInt()
 // 更新索引寄存器的值，如果达到最大容量就归零，否则加一
 index := Mux(index === (capacity - 1).U, 0.U, index + 1.U)
 }
}