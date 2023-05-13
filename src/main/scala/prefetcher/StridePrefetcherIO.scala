package prefetcher

import chisel3._
import chisel3.util._

// TODO: update this module to implement stride prefetcher's IO.
class StridePrefetcherIO(addressWidth: Int, pcWidth: Int) extens Bundle{

    val io = IO(new Bundle{

        val PC = Input(UInt(pcWidth.W))
        val address = Input(UInt(addressWidth.W))

        prefetch_valid = Output(Bool())
        prefetch_address = Output(UInt(adressWidth.W))

    })
 
class Entry(addressWidth:Int,pcWidth:Int) extends Bundle{
    val io = IO(new Bundle{
        val PC = Input(UInt(pcWidth.W))
        val prev_address = Input(UInt(addressWidth.W))
        val prev_stride = Input(UInt(addressWidth.W))

        val prefetch_address = Output(UInt(addressWidth.W))
    })
}

}