package prefetcher

import chisel3.iotesters._
import chisel3._
import scala.collection.mutable.ListBuffer

class MarkovPrefetcherTester(dut: MarkovPrefetcher) extends PeekPokeTester(dut) {
  def simulateMemoryAccesses(addresses: List[Int]): Unit = {
    var totalAccesses = 0
    var correctPrefetches = 0
    var totalPrefetches = 0
    
    for (i <- 0 until addresses.length - 1) {
      val currentAddress = addresses(i)
      val nextAddress = addresses(i + 1)

      totalAccesses += 1
      poke(dut.io.address, currentAddress.U)
      step(1)

      if (peek(dut.io.prefetch_hit) == 1) {
        totalPrefetches += 1
        println("Prefetch !")
        if (peek(dut.io.prefetch_address) == BigInt(nextAddress)) {//比较上一次的提取地址是否和当前的地址相同
          correctPrefetches += 1
          println("Prefetch correct")
        }
      } else {
        println("No Prefetch ")
      }
}

    val accuracy = correctPrefetches.toFloat / totalPrefetches.toFloat //返回准确率
    val coverage = totalPrefetches.toFloat / totalAccesses.toFloat //返回覆盖率
    println(s"accuracy: $accuracy")
    println(s"coverage: $coverage")
  }

  def sequentialPatternTest(){
    val memoryAccesses1 = generateSequentialPattern(10)
    simulateMemoryAccesses(memoryAccesses1)
  }

  def stridedPatternTest(){
    val memoryAccesses2 = generateStridedPattern(0, 2, 10)
    simulateMemoryAccesses(memoryAccesses2)
  }

  def interleavedPatternTest(){
    val memoryAccesses3 = generateInterleavedPattern(10)
    simulateMemoryAccesses(memoryAccesses3)
  }

  def randomPatternTest(){
    val memoryAccesses4 = generateRandomPattern(10)
    simulateMemoryAccesses(memoryAccesses4)
  }

  def generateSequentialPattern(length: Int): List[Int] = {
      (0 until length).toList
    }

    def generateStridedPattern(start: Int, stride: Int, length: Int): List[Int] = {
      (start until (start + stride * length) by stride).toList
    }

    def generateInterleavedPattern(length: Int): List[Int] = {
      val pattern = new ListBuffer[Int]
      for (j <- 0 until length / 2) {
        pattern += 2 * j
        pattern += 2 * j + 1
      }
      pattern.toList
    }

    def generateRandomPattern(length: Int): List[Int] = {
      val random = new scala.util.Random
      List.fill(length)(random.nextInt(length))
    }

    def generateMarkdownTable(results: Map[String, Float]): String = {
      val header = "| 访问模式 | 预测准确性 |\n| -------- | ---------- |\n"
      val rows = results.map { case (key, value) => s"| $key | $value |" }
      header + rows.mkString("\n")
    }
    // val results = scala.collection.mutable.Map[String, Float]()

    println("Sequential access pattern:")
    sequentialPatternTest()

    println("\nStrided access pattern:")
    stridedPatternTest()

    println("\nInterleaved access pattern:")
    interleavedPatternTest()

    println("\nRandom access pattern:")
    randomPatternTest()
    
}



object MarkovPrefetcherTester extends App {
  chisel3.iotesters.Driver(() => new MarkovPrefetcher) { dut =>
    new MarkovPrefetcherTester(dut)
  }
}
