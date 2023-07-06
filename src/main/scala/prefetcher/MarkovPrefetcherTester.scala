package prefetcher

import chisel3.iotesters._
import chisel3._
import scala.collection.mutable.ListBuffer
import scala.util.Random
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
      
      if(peek(dut.io.prefetchHit) == BigInt(1)) {//判断是否预取
        totalPrefetches += 1
        if (peek(dut.io.prefetchAddress) == BigInt(nextAddress)) {//比较上一次的提取地址是否和当前的地址相同
          correctPrefetches += 1
          println("Prefetch correct")
        }
      } else {
        println("No Prefetch ")
      }
      step(1)
}
    var accuracy = 0.0f
    if(totalPrefetches == 0){
      accuracy  = 10
    }else{
      accuracy = correctPrefetches.toFloat / totalPrefetches.toFloat //返回准确率
    }
    val coverage = totalPrefetches.toFloat / totalAccesses.toFloat //返回覆盖率
    println(s"accuracy: $accuracy")
    println(s"coverage: $coverage")
  }

  def sequentialPatternTest(){
    val memoryAccesses1 = generateSequentialPattern(16)
    println(memoryAccesses1.toString())
    simulateMemoryAccesses(memoryAccesses1)
  }

  def stridedPatternTest(){
    val memoryAccesses2 = generateStridedPattern(0, 2, 16)
    println(memoryAccesses2.toString())
    simulateMemoryAccesses(memoryAccesses2)
  }

  def interleavedPatternTest(){
    val memoryAccesses3 = generateInterleavedPattern(16)
    println(memoryAccesses3.toString())
    simulateMemoryAccesses(memoryAccesses3)
  }

  def randomPatternTest(){
    val memoryAccesses4 = generateRandomPattern(16)
    println(memoryAccesses4.toString())
    simulateMemoryAccesses(memoryAccesses4)
  }

    def generateSequentialPattern(length: Int): List[Int] = {
      (0 until length).toList
    }

    def generateStridedPattern(start: Int, stride: Int, length: Int): List[Int] = {
      (0 until length).map(i => start + i * stride).toList
    }

    def generateInterleavedPattern(length: Int): List[Int] = {
    List.tabulate(length)(i => Seq(1, 2, 3, 5, 7, 1, 2, 3, 5, 7, 1, 11, 2, 3, 1, 11)(i % 16))
  }


    def generateRandomPattern(length: Int): List[Int] = {
      val random = new Random()
      (0 until length).map(_ => random.nextInt(length)).toList
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
  def runTests(): Unit = {
    chisel3.iotesters.Driver(() => new MarkovPrefetcher) { dut =>
      new MarkovPrefetcherTester(dut)
    }
  }

  runTests()
}
