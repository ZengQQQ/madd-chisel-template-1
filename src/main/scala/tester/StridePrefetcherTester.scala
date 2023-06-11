package tester
import chisel3.iotesters.PeekPokeTester

class StridePrefetcherTester(dut: StridePrefetcher) extends PeekPokeTester(dut) {
  // Continuous memory access trace
  val continuousTrace = Seq(0, 4, 8, 12, 16, 20, 24)
  // Non-continuous memory access trace
  val nonContinuousTrace = Seq(0, 8, 16, 24, 32, 40, 48)

  def runContinuousAccessTest(): Unit = {
    println("连续访问测试")
    // Test continuous access trace
    for (addressIndex <- 0 until (continuousTrace.size - 1)) {
      val address = continuousTrace(addressIndex)
      val nextAddress = continuousTrace(addressIndex + 1)

      poke(dut.io.pc, 0)
      poke(dut.io.address, address)
      step(1)

      val prefetchAddress = peek(dut.io.prefetch_address)
      expect(dut.io.prefetch_valid, true, "预取有效")

      if (prefetchAddress == nextAddress) {
        println(s"预取正确: $prefetchAddress")
        // Perform additional assertions or actions
      } else {
        println(s"预取地址错误: $prefetchAddress (期望: $nextAddress)")
        // Handle the failure case
      }
    }
  }

  def runNonContinuousAccessTest(): Unit = {
    println("非连续访问测试")
    // Test non-continuous access trace
    for (addressIndex <- 0 until (nonContinuousTrace.size - 1)) {
      val address = nonContinuousTrace(addressIndex)
      val nextAddress = nonContinuousTrace(addressIndex + 1)

      poke(dut.io.pc, 0)
      poke(dut.io.address, address)
      step(1)

      val prefetchAddress = peek(dut.io.prefetch_address)
      val prefetchValid = peek(dut.io.prefetch_valid)

      if (prefetchValid == 0) {
        println("预取无效")
      } else {
        if (prefetchAddress == nextAddress) {
          println(s"预取成功，预取地址: $prefetchAddress")
          // Perform additional assertions or actions
        } else {
          println(s"预取成功，但预取地址不正确: $prefetchAddress (期望: $nextAddress)")
          // Handle the failure case
        }
      }
    }
  }

  // Reset
  reset()
  runContinuousAccessTest()

  // Reset
  reset()
  runNonContinuousAccessTest()
}

object StridePrefetcherMain extends App {
  chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new StridePrefetcher) {
    dut => new StridePrefetcherTester(dut)
  }
}
