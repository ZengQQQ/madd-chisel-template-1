这是strideprefetcher 模块的代码


````scala

package prefetcher

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
 table.io.entry(index).prev_address := io.address
 table.io.entry(index).prev_stride := current_stride.asSInt()
 // 更新索引寄存器的值，如果达到最大容量就归零，否则加一
 index := Mux(index === (capacity - 1).U, 0.U, index + 1.U)
 }
}


````

strideprefetcherIO代码
````scala
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


````

strideprefetcherTester代码块
````scala
package prefetcher

import chisel3._
import chisel3.iotesters.PeekPokeTester
import chisel3.util._

// TODO: update this module to implement unit testing for stride prefetching.
class StridePrefetcherTester(dut: StridePrefetcher)
 extends PeekPokeTester(dut) {

  // Test a sequential access sequence: 0, 4, 8, 12, 16, ...
  // Assume the PC is always 0 for simplicity
  def test_sequential_access() {
    // Define a variable to store the current address
    var address = 0

    // Define a variable to store the expected prefetch address
    var prefetch_address = 0

    // Loop for 10 times
    for (i <- 0 until 10) {
      // Poke the PC and the address
      poke(dut.io.PC, 0)
      poke(dut.io.address, address)

      // If it is not the first or second access, expect a prefetch
      if (i > 1) {
        expect(dut.io.prefetch_valid, true.B)
        expect(dut.io.prefetch_address, prefetch_address)
      } else {
        // Otherwise, expect no prefetch
        expect(dut.io.prefetch_valid, false.B)
      }

      // Advance one cycle
      step(1)

      // Update the address and the prefetch address by adding 4
      address += 4
      prefetch_address += 4
    }
  }

   // Test a non-sequential access sequence: 0, 8, 16, 24, 32, ...
   // Assume the PC is always 0 for simplicity
   def test_non_sequential_access() {
     // Define a variable to store the current address
     var address = 0

     // Define a variable to store the expected prefetch address
     var prefetch_address = 0

     // Loop for 10 times
     for (i <- 0 until 10) {
       // Poke the PC and the address
       poke(dut.io.PC, 0)
       poke(dut.io.address, address)

       // If it is not the first or second access, expect a prefetch
       if (i > 1) {
         expect(dut.io.prefetch_valid, true.B)
         expect(dut.io.prefetch_address, prefetch_address)
       } else {
         // Otherwise, expect no prefetch
         expect(dut.io.prefetch_valid, false.B)
       }

       // Advance one cycle
       step(1)

       // Update the address and the prefetch address by adding 8
       address += 8
       prefetch_address += 8
     }
   }

   // Call the test functions
   test_sequential_access()
   test_non_sequential_access()
}

object StridePrefetcherTester extends App {
 chisel3.iotesters.Driver(() => new StridePrefetcher(3,2)) { dut =>
 new StridePrefetcherTester(dut)
 }
}

````