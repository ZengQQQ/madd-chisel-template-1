package prefetcher
import chisel3._
import chisel3.iotesters.PeekPokeTester

class StridePrefetcherTester(dut: StridePrefetcher) extends PeekPokeTester(dut) {

  // Test a sequential access sequence: 0, 4, 8, 12, 16, ...
  // Assume the PC is always 0 for simplicity
  def test_sequential_access() {
    // Define a variable to store the current address
    var address = 0

    // Define a variable to store the expected prefetch address
    var prefetch_address = 0

    // Initialize counters
    var totalAccesses = 0
    var correctPrefetches = 0
    var totalPrefetches = 0
    println(s"test sequential access")
    // Loop for 10 times
    for (i <- 0 until 10) {
      // Poke the PC and the address
      poke(dut.io.pc, 0)
      poke(dut.io.address, address)

      
      // If it is not the first access, expect a prefetch
      if (i > 0) {
        expect(dut.io.prefetch_valid, true.B)
        expect(dut.io.prefetch_address, prefetch_address)
        totalPrefetches += 1
      } else {
        // Otherwise, expect no prefetch
        expect(dut.io.prefetch_valid, false.B)
      }

    

      // Update the address and the prefetch address by adding 4
      address += 4
      prefetch_address += 4

      // Update counters
      totalAccesses += 1
 
      println(s"peek(dut.io.prefetch_address) = ${peek(dut.io.prefetch_address)}")
      if (i > 0 && peek(dut.io.prefetch_valid) == 1 && peek(dut.io.prefetch_address) == prefetch_address) {
        correctPrefetches += 1
      }
      // Advance one cycle
      step(1)
    }

    // Calculate accuracy and coverage
    val accuracy = correctPrefetches.toDouble / totalPrefetches.toDouble
    val coverage = totalPrefetches.toDouble / totalAccesses.toDouble

    println(s"Sequential Access Test:")
    println(s"  Total Accesses: $totalAccesses")
    println(s"  Total Prefetches: $totalPrefetches")
    println(s"  Correct Prefetches: $correctPrefetches")
    println(s"  Accuracy: $accuracy")
    println(s"  Coverage: $coverage")
  }
  // Test a non-sequential access sequence: 0, 8, 16, 24, 32, ...
// Assume the PC is always 0 for simplicity
def test_non_sequential_access() {
  // Define a variable to store the current address
  var address = 0

  // Define a variable to store the expected prefetch address
  var prefetch_address = 0

  // Initialize counters
  var totalAccesses = 0
  var correctPrefetches = 0
  var totalPrefetches = 0
  println(s"test  non_sequential access")
  // Loop for 10 times
  for (i <- 0 until 10) {
    // Poke the PC and the address
    poke(dut.io.pc, 0)
    poke(dut.io.address, address)

    // If it is not the first access, expect a prefetch
    if (i > 0) {
      expect(dut.io.prefetch_valid, true.B)
      expect(dut.io.prefetch_address, prefetch_address)
      totalPrefetches += 1
    } else {
      // Otherwise, expect no prefetch
      expect(dut.io.prefetch_valid, false.B)
    }

    // Update the address and the prefetch address by adding 8
    address += 8
    prefetch_address += 8

    // Update counters
    totalAccesses += 1

    // Check if the prefetch was correct
    if (i > 0 && peek(dut.io.prefetch_valid) == 1 && peek(dut.io.prefetch_address) == prefetch_address) {
      correctPrefetches += 1
    }

    // Advance one cycle
    step(1)
  }

  // Calculate accuracy and coverage
  val accuracy = correctPrefetches.toDouble / totalPrefetches.toDouble
  val coverage = totalPrefetches.toDouble / totalAccesses.toDouble

  println(s"Non-sequential Access Test:")
  println(s"  Total Accesses: $totalAccesses")
  println(s"  Total Prefetches: $totalPrefetches")
  println(s"  Correct Prefetches: $correctPrefetches")
  println(s"  Accuracy: $accuracy")
  println(s"  Coverage: $coverage")
}

  // Call the test function

  test_sequential_access()
  
  test_non_sequential_access()
}

object StridePrefetcherTester extends App {
  chisel3.iotesters.Driver(() => new StridePrefetcher(32, 32)) { dut =>
    new StridePrefetcherTester(dut)
  }
}
