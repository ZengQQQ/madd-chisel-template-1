package prev_prefetcher

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
